package hudson.plugins.promoted_builds;

import org.htmlunit.html.HtmlImage;
import org.htmlunit.html.HtmlPage;
import hudson.model.Cause.UserCause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.IOException2;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Kohsuke Kawaguchi
 */
public class PromotedBuildActionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testDeletedPromotionProcess() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        JobPropertyImpl base = new JobPropertyImpl(p);
        p.addProperty(base);
        PromotionProcess foo = base.addProcess("foo");

        // promote a build
        FreeStyleBuild b1 = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        foo.promote(b1,new UserCause(),new ManualPromotionBadge());

        // now delete the promotion process
        p.removeProperty(base);
        p.addProperty(base=new JobPropertyImpl(p));
        assertTrue(base.getActiveItems().isEmpty());

        // make sure that the page renders OK without any error
        HtmlPage page = j.createWebClient().getPage(p);
        List<?> candidates = page.getByXPath("//IMG");
        for (Object candidate : candidates) {
            if (!(candidate instanceof HtmlImage)) {
                continue;
            } 
            HtmlImage img = (HtmlImage)candidate;
            try {
                assertEquals("Failed to load " + img.getSrcAttribute(),
                        200,
                        img.getWebResponse(true).getStatusCode());
            } catch (IOException e) {
                throw new AssertionError("Failed to load " + img.getSrcAttribute());
            }
        }
    }
}
