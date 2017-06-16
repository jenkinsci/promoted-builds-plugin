package hudson.plugins.promoted_builds.conditions.inheritance;

import hudson.model.Cause;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.plugins.project_inheritance.projects.InheritanceBuild;
import hudson.plugins.project_inheritance.projects.InheritanceProject.IMode;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotedBuildAction;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.conditions.FixedResultBuilder;
import hudson.plugins.promoted_builds.conditions.SelfPromotionCondition;
import hudson.plugins.promoted_builds.inheritance.helpers.InheritanceProjectRule;
import hudson.plugins.promoted_builds.inheritance.helpers.InheritanceProjectsPair;

import static hudson.plugins.promoted_builds.util.ItemListenerHelper.fireItemListeners;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;


/**
 * @author Jacek Tomaka
 */
public class SelfPromotionInheritanceTest  {
    @Rule 
    public InheritanceProjectRule j = new InheritanceProjectRule();
    @Test
    public void testBasic() throws Exception {
        InheritanceProjectsPair inheritanceProjectPair = j.createInheritanceProjectDerivedWithBase();

        // promote if the downstream passes
        JobPropertyImpl promotion = new JobPropertyImpl(inheritanceProjectPair.getBase());
        inheritanceProjectPair.getBase().addProperty(promotion);

        PromotionProcess promo1 = promotion.addProcess("promo1");
        promo1.conditions.add(new SelfPromotionCondition(false));

        PromotionProcess promo2 = promotion.addProcess("promo2");
        promo2.conditions.add(new SelfPromotionCondition(false));

       
        InheritanceBuild b = j.assertBuildStatusSuccess(inheritanceProjectPair.getDerived().scheduleBuild2(0));

        // internally, the promotion is still an asynchronous process. It just happens
        // right away after the build is complete.
        j.waitUntilNoActivity();

        // rebind
        promotion = inheritanceProjectPair.getDerived().getProperty(JobPropertyImpl.class, 
                /*Forcing inheritance as temporary hack for inheritance plugin 1.53 
                because that version of the plugin uses inheritance only for certain predefined cases: 
                -specific methods on the call stack
                -url paths.
                This has been changed as pull request https://github.com/i-m-c/jenkins-inheritance-plugin/pull/40
               */
                IMode.INHERIT_FORCED);
        promo1 = promotion.getItem("promo1");
        promo2 = promotion.getItem("promo2");

        
        // verify that both promotions happened
        Promotion pb = promo1.getBuilds().get(0);
        assertSame(pb.getTarget(),b);

        pb = promo2.getBuilds().get(0);
        assertSame(pb.getTarget(),b);

        PromotedBuildAction badge = (PromotedBuildAction) b.getBadgeActions().get(0);
        assertTrue(badge.contains(promo1));
        assertTrue(badge.contains(promo2));
    }
    @Test
    public void testUnstable() throws Exception {
        InheritanceProjectsPair inheritanceProjectPair = j.createInheritanceProjectDerivedWithBase();

        // promote if the downstream passes
        JobPropertyImpl promotion = new JobPropertyImpl(inheritanceProjectPair.getBase());
        inheritanceProjectPair.getBase().addProperty(promotion);

        PromotionProcess promo1 = promotion.addProcess("promo1");
        promo1.conditions.add(new SelfPromotionCondition(false));

        PromotionProcess promo2 = promotion.addProcess("promo2");
        promo2.conditions.add(new SelfPromotionCondition(true));

       

        inheritanceProjectPair.getDerived().getBuildersList().add(unstableBuilder());
        InheritanceBuild b = j.assertBuildStatus(Result.UNSTABLE, inheritanceProjectPair.getDerived().scheduleBuild2(0).get());
        j.waitUntilNoActivity();
        // rebind
        promotion = inheritanceProjectPair.getDerived().getProperty(JobPropertyImpl.class, 
                /*Forcing inheritance as temporary hack for inheritance plugin 1.53 
                because that version of the plugin uses inheritance only for certain predefined cases: 
                -specific methods on the call stack
                -url paths.
                This has been changed as pull request https://github.com/i-m-c/jenkins-inheritance-plugin/pull/40
               */
                IMode.INHERIT_FORCED);
        promo1 = promotion.getItem("promo1");
        promo2 = promotion.getItem("promo2");
        // internally, the promotion is still an asynchronous process. It just happens
        // right away after the build is complete.
        

        // verify that only one promotions happened
        assertTrue(promo1.getBuilds().isEmpty());

        Promotion pb = promo2.getBuilds().get(0);
        assertSame(pb.getTarget(),b);

        PromotedBuildAction badge = (PromotedBuildAction) b.getBadgeActions().get(0);
        assertFalse(badge.contains(promo1));
        assertTrue(badge.contains(promo2));
    }

    @Test
    public void testFailure() throws Exception {
        InheritanceProjectsPair inheritanceProjectPair = j.createInheritanceProjectDerivedWithBase();

        // promote if the downstream passes
        JobPropertyImpl promotion = new JobPropertyImpl(inheritanceProjectPair.getBase());
        inheritanceProjectPair.getBase().addProperty(promotion);

        PromotionProcess promo1 = promotion.addProcess("promo1");
        promo1.conditions.add(new SelfPromotionCondition(false));

        PromotionProcess promo2 = promotion.addProcess("promo2");
        promo2.conditions.add(new SelfPromotionCondition(true));

        inheritanceProjectPair.getDerived().getBuildersList().add(failureBuilder());
        InheritanceBuild b = j.assertBuildStatus(Result.FAILURE, inheritanceProjectPair.getDerived().scheduleBuild2(0).get());
        
        // internally, the promotion is still an asynchronous process. It just happens
        // right away after the build is complete.
        j.waitUntilNoActivity();
        
        // rebind
        promotion = inheritanceProjectPair.getDerived().getProperty(JobPropertyImpl.class, 
              /*Forcing inheritance as temporary hack for inheritance plugin 1.53 
                because that version of the plugin uses inheritance only for certain predefined cases: 
                -specific methods on the call stack
                -url paths.
                This has been changed as pull request https://github.com/i-m-c/jenkins-inheritance-plugin/pull/40
               */
                IMode.INHERIT_FORCED); 
        promo1 = promotion.getItem("promo1");
        promo2 = promotion.getItem("promo2");


        // verify that neither promotions happened
        assertTrue("promo1 did not occur", promo1.getBuilds().isEmpty());
        assertTrue("promo2 did not occur", promo2.getBuilds().isEmpty());

        PromotedBuildAction badge = (PromotedBuildAction) b.getBadgeActions().get(0);
        assertFalse(badge.contains(promo1));
        assertFalse(badge.contains(promo2));
    }

    @Test
    @Issue("JENKINS-22679")
    public void testPromotionEnvironmentShouldIncludeTargetParameters() throws Exception {
        String paramName = "param";

        InheritanceProjectsPair inheritanceProjectPair = j.createInheritanceProjectDerivedWithBase();

        // promote if the downstream passes
        JobPropertyImpl promotion = new JobPropertyImpl(inheritanceProjectPair.getBase());
        inheritanceProjectPair.getBase().addProperty(promotion);

        // TODO review this property asignment after https://issues.jenkins-ci.org/browse/JENKINS-34831 is fixed
        inheritanceProjectPair.getBase().addProperty(new ParametersDefinitionProperty(new StringParameterDefinition(paramName, "")));
        inheritanceProjectPair.getDerived().addProperty(new ParametersDefinitionProperty(new StringParameterDefinition(paramName, "")));
        PromotionProcess promo1 = promotion.addProcess("promo1");
        promo1.conditions.add(new SelfPromotionCondition(false));

        // fire ItemListeners, this includes ArtifactArchiver,Migrator to make this test compatible with jenkins 1.575+
        fireItemListeners();

        String paramValue = "someString";
        j.assertBuildStatusSuccess(inheritanceProjectPair.getDerived().scheduleBuild2(0, new Cause.UserCause(),
                new ParametersAction(new StringParameterValue(paramName, paramValue))));
        // internally, the promotion is still an asynchronous process. It just happens
        // right away after the build is complete.
        j.waitUntilNoActivity();

        // rebind
        promotion = inheritanceProjectPair.getDerived().getProperty(JobPropertyImpl.class, 
              /*Forcing inheritance as temporary hack for inheritance plugin 1.53 
                because that version of the plugin uses inheritance only for certain predefined cases: 
                -specific methods on the call stack
                -url paths.
                This has been changed as pull request https://github.com/i-m-c/jenkins-inheritance-plugin/pull/40
               */
                IMode.INHERIT_FORCED); 
        promo1 = promotion.getItem("promo1");
        
        // verify that the promotion's environment contains the parameter from the target build.
        Promotion pb = promo1.getBuildByNumber(1);
        assertEquals(paramValue, pb.getEnvironment(TaskListener.NULL).get(paramName, null));
    }

    private FixedResultBuilder failureBuilder() {
        return new FixedResultBuilder(Result.FAILURE);
    }

    private FixedResultBuilder unstableBuilder() {
        return new FixedResultBuilder(Result.UNSTABLE);
    }

}
