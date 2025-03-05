package hudson.plugins.promoted_builds;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.promoted_builds.conditions.ManualCondition;
import hudson.plugins.promoted_builds.conditions.ManualCondition.ManualApproval;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.File;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Kohsuke Kawaguchi
 */
@WithJenkins
class PromotionTargetActionTest {

    @TempDir
    private File tmp;

    /**
     * When a project is created, built, and renamed, then the old build is created,
     * that results in NPE.
     */
    @Test
    void test1(JenkinsRule j) throws Exception {
        FreeStyleProject up = j.createFreeStyleProject("up");
        up.setCustomWorkspace(tmp.getPath());

        // promote if the downstream passes
        JobPropertyImpl promotion = new JobPropertyImpl(up);
        up.addProperty(promotion);
        PromotionProcess proc = promotion.addProcess("promo");
        proc.conditions.add(new ManualCondition());

        FreeStyleBuild b = j.assertBuildStatusSuccess(up.scheduleBuild2(0));

        b.addAction(new ManualApproval(proc.getName(), Collections.emptyList()));
        b.save();

        // check for promotion
        Promotion p = j.assertBuildStatusSuccess(proc.considerPromotion2(b));

        up.renameTo("up2");

        assertSame(b,p.getTargetBuildOrFail());
    }
}
