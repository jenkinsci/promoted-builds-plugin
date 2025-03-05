package hudson.plugins.promoted_builds.conditions;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotionProcess;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@WithJenkins
class GroovyConditionTest {

    @Test
    void testBooleanScript(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        final JobPropertyImpl property = new JobPropertyImpl(p);

        final PromotionProcess promotionProcessTrue = property.addProcess("truePromotion");
        promotionProcessTrue.conditions.add(new GroovyCondition(new SecureGroovyScript("true", false, null), "", ""));

        final PromotionProcess promotionProcessFalse = property.addProcess("falsePromotion");
        promotionProcessFalse.conditions.add(new GroovyCondition(new SecureGroovyScript("false", false, null), "", ""));

        p = j.configRoundtrip(p);

        final FreeStyleBuild build = j.buildAndAssertSuccess(p);

        assertNotNull(promotionProcessTrue.isMet(build), "Promotion was expected");
        assertNull(promotionProcessFalse.isMet(build), "Promotion was not expected");
    }

    @Test
    void testMapScript(JenkinsRule j) throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        final JobPropertyImpl property = new JobPropertyImpl(p);

        final PromotionProcess promotionProcessEmptyMap = property.addProcess("emptyMap");
        promotionProcessEmptyMap.conditions.add(new GroovyCondition(new SecureGroovyScript("[:]", false, null), "", ""));

        final PromotionProcess promotionProcessNonEmptyMap = property.addProcess("nonEmptyMap");
        promotionProcessNonEmptyMap.conditions.add(new GroovyCondition(new SecureGroovyScript("[foo: 'bar']", false, null), "", ""));

        p = j.configRoundtrip(p);

        final FreeStyleBuild build = j.buildAndAssertSuccess(p);

        assertNull(promotionProcessEmptyMap.isMet(build), "Promotion was not expected");
        assertNotNull(promotionProcessNonEmptyMap.isMet(build), "Promotion was expected");
    }

    @Test
    void testBinding(JenkinsRule j) throws Exception {
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

        assertNotNull(promotionProcess.isMet(build), "Promotion was expected");
    }
}
