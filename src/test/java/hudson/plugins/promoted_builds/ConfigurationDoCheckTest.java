package hudson.plugins.promoted_builds;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import hudson.tasks.JavadocArchiver;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import static com.gargoylesoftware.htmlunit.html.HtmlFormUtil.submit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Seiji Sogabe
 */
public class ConfigurationDoCheckTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Issue("JENKINS-7972")
    @Test
    public void testCheckProcessNameRequired() throws Exception {
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

        HtmlPage page = (HtmlPage) submit(client.getPage(p, "configure").getFormByName("config"));
        assertTrue(page.asText().contains("No name is specified"));

    }

    @Issue("JENKINS-7972")
    @Test
    public void testCheckInvalidProcessName() throws Exception {
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

        HtmlPage page = (HtmlPage) submit(client.getPage(p, "configure").getFormByName("config"));
        assertTrue(page.asText().contains("unsafe character"));

    }
}
