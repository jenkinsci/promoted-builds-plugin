package hudson.plugins.promoted_builds;

import com.gargoylesoftware.htmlunit.html.HtmlImage;
import hudson.model.Cause.UserCause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.IOException2;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.IOException;
import java.util.List;

/**
 * @author Kohsuke Kawaguchi
 */
public class PromotedBuildActionTest extends HudsonTestCase {
    public void testDeletedPromotionProcess() throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        JobPropertyImpl base = new JobPropertyImpl(p);
        p.addProperty(base);
        PromotionProcess foo = base.addProcess("foo");

        // promote a build
        FreeStyleBuild b1 = assertBuildStatusSuccess(p.scheduleBuild2(0));
        foo.promote(b1,new UserCause(),new ManualPromotionBadge());

        // now delete the promotion process
        p.removeProperty(base);
        p.addProperty(base=new JobPropertyImpl(p));
        assertTrue(base.getActiveItems().isEmpty());

        // make sure that the page renders OK without any error
        for (HtmlImage img : createWebClient().getPage(p).<HtmlImage>selectNodes("//IMG")) {
            try {
                img.getHeight();
            } catch (IOException e) {
                throw new IOException2("Failed to load "+img.getSrcAttribute(),e);
            }
        }
    }
}
