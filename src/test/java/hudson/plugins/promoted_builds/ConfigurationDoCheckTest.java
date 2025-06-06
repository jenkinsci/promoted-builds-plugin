package hudson.plugins.promoted_builds;

import org.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import hudson.tasks.JavadocArchiver;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Seiji Sogabe
 */
@WithJenkins
class ConfigurationDoCheckTest {

    @Issue("JENKINS-7972")
    @Test
    void testCheckProcessNameRequired(JenkinsRule j) throws Exception {
        FreeStyleProject down = j.createFreeStyleProject();

        FreeStyleProject p = j.createFreeStyleProject();
        JobPropertyImpl pp = new JobPropertyImpl(p);
        p.addProperty(pp);

        PromotionProcess proc = pp.addProcess("");
        assertEquals(1,pp.getItems().size());
        proc.conditions.add(new DownstreamPassCondition(down.getName()));
        proc.getBuildSteps().add(new JavadocArchiver("somedir",true));
        proc.icon = "star-blue";

        JenkinsRule.WebClient client = j.createWebClient();
        client.getOptions().setThrowExceptionOnFailingStatusCode(false);

        HtmlPage page = j.submit(client.getPage(p, "configure").getFormByName("config"));
        assertTrue(page.getVisibleText().contains("No name is specified"));

    }

    @Issue("JENKINS-7972")
    @Test
    void testCheckInvalidProcessName(JenkinsRule j) throws Exception {
        FreeStyleProject down = j.createFreeStyleProject();

        FreeStyleProject p = j.createFreeStyleProject();
        JobPropertyImpl pp = new JobPropertyImpl(p);
        p.addProperty(pp);

        PromotionProcess proc = pp.addProcess("test/");
        assertEquals(1,pp.getItems().size());
        proc.conditions.add(new DownstreamPassCondition(down.getName()));
        proc.getBuildSteps().add(new JavadocArchiver("somedir",true));
        proc.icon = "star-blue";

        JenkinsRule.WebClient client = j.createWebClient();
        client.getOptions().setThrowExceptionOnFailingStatusCode(false);

        HtmlPage page = j.submit(client.getPage(p, "configure").getFormByName("config"));
        assertTrue(page.getVisibleText().contains("unsafe character"));

    }
}
