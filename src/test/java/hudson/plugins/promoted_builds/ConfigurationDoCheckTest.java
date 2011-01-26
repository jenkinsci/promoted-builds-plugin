package hudson.plugins.promoted_builds;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.FreeStyleProject;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import hudson.tasks.JavadocArchiver;
import org.jvnet.hudson.test.Bug;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * @author Seiji Sogabe
 */
public class ConfigurationDoCheckTest extends HudsonTestCase {

    @Bug(7972)
    public void testCheckProcessNameRequired() throws Exception {
        FreeStyleProject down = createFreeStyleProject();

        FreeStyleProject p = createFreeStyleProject();
        JobPropertyImpl pp = new JobPropertyImpl(p);
        p.addProperty(pp);

        PromotionProcess proc = pp.addProcess("");
        assertEquals(1,pp.getItems().size());
        proc.conditions.add(new DownstreamPassCondition(down.getName()));
        proc.getBuildSteps().add(new JavadocArchiver("somedir",true));
        proc.icon = "star-blue";

        WebClient client = new WebClient();
        client.setThrowExceptionOnFailingStatusCode(false);
        HtmlPage page = submit(client.getPage(p, "configure").getFormByName("config"));
        assertTrue(page.asText().contains("No name is specified"));

    }

    @Bug(7972)
    public void testCheckInvalidProcessName() throws Exception {
        FreeStyleProject down = createFreeStyleProject();

        FreeStyleProject p = createFreeStyleProject();
        JobPropertyImpl pp = new JobPropertyImpl(p);
        p.addProperty(pp);

        PromotionProcess proc = pp.addProcess("test/");
        assertEquals(1,pp.getItems().size());
        proc.conditions.add(new DownstreamPassCondition(down.getName()));
        proc.getBuildSteps().add(new JavadocArchiver("somedir",true));
        proc.icon = "star-blue";

        WebClient client = new WebClient();
        client.setThrowExceptionOnFailingStatusCode(false);
        HtmlPage page = submit(client.getPage(p, "configure").getFormByName("config"));
        assertTrue(page.asText().contains("unsafe character"));

    }
}
