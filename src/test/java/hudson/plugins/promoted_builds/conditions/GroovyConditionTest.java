package hudson.plugins.promoted_builds.conditions;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotionProcess;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class GroovyConditionTest {
    @Rule
    public JenkinsRule j =  new JenkinsRule();

    @Test
    public void testBooleanScript() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        final JobPropertyImpl property = new JobPropertyImpl(p);

        final PromotionProcess promotionProcessTrue = property.addProcess("truePromotion");
        promotionProcessTrue.conditions.add(new GroovyCondition(new SecureGroovyScript("true", false, null), "", ""));

        final PromotionProcess promotionProcessFalse = property.addProcess("falsePromotion");
        promotionProcessFalse.conditions.add(new GroovyCondition(new SecureGroovyScript("false", false, null), "", ""));

        p = j.configRoundtrip(p);

        final FreeStyleBuild build = j.buildAndAssertSuccess(p);

        Assert.assertNotNull("Promotion was expected", promotionProcessTrue.isMet(build));
        Assert.assertNull("Promotion was not expected", promotionProcessFalse.isMet(build));
    }

    @Test
    public void testMapScript() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        final JobPropertyImpl property = new JobPropertyImpl(p);

        final PromotionProcess promotionProcessEmptyMap = property.addProcess("emptyMap");
        promotionProcessEmptyMap.conditions.add(new GroovyCondition(new SecureGroovyScript("[:]", false, null), "", ""));

        final PromotionProcess promotionProcessNonEmptyMap = property.addProcess("nonEmptyMap");
        promotionProcessNonEmptyMap.conditions.add(new GroovyCondition(new SecureGroovyScript("[foo: 'bar']", false, null), "", ""));

        p = j.configRoundtrip(p);

        final FreeStyleBuild build = j.buildAndAssertSuccess(p);

        Assert.assertNull("Promotion was not expected", promotionProcessEmptyMap.isMet(build));
        Assert.assertNotNull("Promotion was expected", promotionProcessNonEmptyMap.isMet(build));
    }

    @Test
    public void testBinding() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        final JobPropertyImpl property = new JobPropertyImpl(p);

        final PromotionProcess promotionProcess = property.addProcess("testPromotion");
        promotionProcess.conditions.add(
            new GroovyCondition(
                new SecureGroovyScript(
                    "promotionProcess instanceof hudson.plugins.promoted_builds.PromotionProcess && " +
                        "build instanceof hudson.model.AbstractBuild && " +
                        "jenkins instanceof jenkins.model.Jenkins",
                    false,
                    null
                ),
            "",
            ""
        ));

        p = j.configRoundtrip(p);

        final FreeStyleBuild build = j.buildAndAssertSuccess(p);

        Assert.assertNotNull("Promotion was expected", promotionProcess.isMet(build));
    }
}
