package hudson.plugins.promoted_builds;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterValue;
import hudson.plugins.promoted_builds.conditions.ManualCondition;
import hudson.plugins.promoted_builds.conditions.ManualCondition.ManualApproval;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;

import static org.junit.Assert.assertSame;

/**
 * @author Kohsuke Kawaguchi
 */
public class PromotionTargetActionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    /**
     * When a project is created, built, and renamed, then the old build is created,
     * that results in NPE.
     */
    @Test
    public void test1() throws Exception {
        FreeStyleProject up = j.createFreeStyleProject("up");
        up.setCustomWorkspace(j.createTmpDir().getPath());

        // promote if the downstream passes
        JobPropertyImpl promotion = new JobPropertyImpl(up);
        up.addProperty(promotion);
        PromotionProcess proc = promotion.addProcess("promo");
        proc.conditions.add(new ManualCondition());

        FreeStyleBuild b = j.assertBuildStatusSuccess(up.scheduleBuild2(0));

        b.addAction(new ManualApproval(proc.getName(), Collections.<ParameterValue>emptyList()));
        b.save();

        // check for promotion
        Promotion p = j.assertBuildStatusSuccess(proc.considerPromotion2(b));

        up.renameTo("up2");

        assertSame(b,p.getTarget());
    }
}
