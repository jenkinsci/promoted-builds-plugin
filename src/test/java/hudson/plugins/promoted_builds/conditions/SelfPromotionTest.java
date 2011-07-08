package hudson.plugins.promoted_builds.conditions;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotedBuildAction;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.PromotionProcess;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * @author Kohsuke Kawaguchi
 */
public class SelfPromotionTest extends HudsonTestCase {
    public void testBasic() throws Exception {
        FreeStyleProject p = createFreeStyleProject();

        // promote if the downstream passes
        JobPropertyImpl promotion = new JobPropertyImpl(p);
        p.addProperty(promotion);

        PromotionProcess promo1 = promotion.addProcess("promo1");
        promo1.conditions.add(new SelfPromotionCondition());

        PromotionProcess promo2 = promotion.addProcess("promo2");
        promo2.conditions.add(new SelfPromotionCondition());

        // ensure that the data survives the roundtrip
        configRoundtrip(p);

        FreeStyleBuild b = assertBuildStatusSuccess(p.scheduleBuild2(0));
        // internally, the promotion is still an asynchronous process. It just happens
        // right away after the build is complete.
        Thread.sleep(1000);

        // verify that both promotions happened
        Promotion pb = promo1.getBuilds().get(0);
        assertSame(pb.getTarget(),b);

        pb = promo2.getBuilds().get(0);
        assertSame(pb.getTarget(),b);

        PromotedBuildAction badge = (PromotedBuildAction) b.getBadgeActions().get(0);
        assertTrue(badge.contains(promo1));
        assertTrue(badge.contains(promo2));
    }
}
