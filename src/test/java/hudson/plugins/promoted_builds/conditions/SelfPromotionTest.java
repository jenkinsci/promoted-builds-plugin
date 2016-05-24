package hudson.plugins.promoted_builds.conditions;

import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotedBuildAction;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.PromotionProcess;
import org.jvnet.hudson.test.Bug;
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
        promo1.conditions.add(new SelfPromotionCondition(false));

        PromotionProcess promo2 = promotion.addProcess("promo2");
        promo2.conditions.add(new SelfPromotionCondition(false));

        // ensure that the data survives the roundtrip
        configRoundtrip(p);

        // rebind
        promotion = p.getProperty(JobPropertyImpl.class);
        promo1 = promotion.getItem("promo1");
        promo2 = promotion.getItem("promo2");

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

    public void testUnstable() throws Exception {
        FreeStyleProject p = createFreeStyleProject();

        // promote if the downstream passes
        JobPropertyImpl promotion = new JobPropertyImpl(p);
        p.addProperty(promotion);

        PromotionProcess promo1 = promotion.addProcess("promo1");
        promo1.conditions.add(new SelfPromotionCondition(false));

        PromotionProcess promo2 = promotion.addProcess("promo2");
        promo2.conditions.add(new SelfPromotionCondition(true));

        // ensure that the data survives the roundtrip
        configRoundtrip(p);

        // rebind
        promotion = p.getProperty(JobPropertyImpl.class);
        promo1 = promotion.getItem("promo1");
        promo2 = promotion.getItem("promo2");

        p.getBuildersList().add(unstableBuilder());
        FreeStyleBuild b = assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0).get());
        // internally, the promotion is still an asynchronous process. It just happens
        // right away after the build is complete.
        Thread.sleep(1000);

        // verify that only one promotions happened
        assertTrue(promo1.getBuilds().isEmpty());

        Promotion pb = promo2.getBuilds().get(0);
        assertSame(pb.getTarget(),b);

        PromotedBuildAction badge = (PromotedBuildAction) b.getBadgeActions().get(0);
        assertFalse(badge.contains(promo1));
        assertTrue(badge.contains(promo2));
    }


    public void testFailure() throws Exception {
        FreeStyleProject p = createFreeStyleProject();

        // promote if the downstream passes
        JobPropertyImpl promotion = new JobPropertyImpl(p);
        p.addProperty(promotion);

        PromotionProcess promo1 = promotion.addProcess("promo1");
        promo1.conditions.add(new SelfPromotionCondition(false));

        PromotionProcess promo2 = promotion.addProcess("promo2");
        promo2.conditions.add(new SelfPromotionCondition(true));

        // ensure that the data survives the roundtrip
        configRoundtrip(p);

        // rebind
        promotion = p.getProperty(JobPropertyImpl.class);
        promo1 = promotion.getItem("promo1");
        promo2 = promotion.getItem("promo2");

        p.getBuildersList().add(failureBuilder());
        FreeStyleBuild b = assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());

        // internally, the promotion is still an asynchronous process. It just happens
        // right away after the build is complete.
        Thread.sleep(1000);

        // verify that neither promotions happened
        assertTrue("promo1 did not occur", promo1.getBuilds().isEmpty());
        assertTrue("promo2 did not occur", promo2.getBuilds().isEmpty());

        PromotedBuildAction badge = (PromotedBuildAction) b.getBadgeActions().get(0);
        assertFalse(badge.contains(promo1));
        assertFalse(badge.contains(promo2));
    }

    @Bug(22679)
    // @Bug(34826) // Can be reproduced in Jenkins 2.3 +
    public void testPromotionEnvironmentShouldIncludeTargetParameters() throws Exception {
        String paramName = "param";

        FreeStyleProject p = createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(new StringParameterDefinition(paramName, "")));

        // promote if the downstream passes
        JobPropertyImpl promotion = new JobPropertyImpl(p);
        p.addProperty(promotion);

        PromotionProcess promo1 = promotion.addProcess("promo1");
        promo1.conditions.add(new SelfPromotionCondition(false));

        // ensure that the data survives the roundtrip
        configRoundtrip(p);

        // rebind
        promotion = p.getProperty(JobPropertyImpl.class);
        promo1 = promotion.getItem("promo1");

        String paramValue = "someString";
        FreeStyleBuild b = assertBuildStatusSuccess(p.scheduleBuild2(0, new Cause.UserCause(),
                new ParametersAction(new StringParameterValue(paramName, paramValue))));
        // internally, the promotion is still an asynchronous process. It just happens
        // right away after the build is complete.
        Thread.sleep(1000);

        // verify that the promotion's environment contains the parameter from the target build.
        Promotion pb = promo1.getBuildByNumber(1);
        assertEquals(paramValue, pb.getEnvironment(TaskListener.NULL).get(paramName, null));
    }

    private FixedResultBuilder successfulBuilder() {
        return new FixedResultBuilder(Result.SUCCESS);
    }

    private FixedResultBuilder failureBuilder() {
        return new FixedResultBuilder(Result.FAILURE);
    }

    private FixedResultBuilder unstableBuilder() {
        return new FixedResultBuilder(Result.UNSTABLE);
    }

}
