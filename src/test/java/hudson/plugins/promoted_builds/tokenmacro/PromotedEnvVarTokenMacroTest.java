/*
 * The MIT License
 *
 * Copyright (c) 2015 CloudBees, Inc..
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.promoted_builds.tokenmacro;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.conditions.ManualCondition;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.acegisecurity.context.SecurityContextHolder;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockFolder;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link PromotedEnvVarTokenMacro}.
 * @author Oleg Nenashev
 */
@WithJenkins
class PromotedEnvVarTokenMacroTest {

    @Test
    void testEnvironmentVariableExpansion(JenkinsRule r) throws Exception {
        // Assemble
        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());
        User u = User.get("foo");
        u.setFullName("Foobar");

        SecurityContextHolder.getContext().setAuthentication(u.impersonate());

        MockFolder parent = r.createFolder("Folder");
        FreeStyleProject project = parent.createProject(FreeStyleProject.class, "Project");

        JobPropertyImpl promotionProperty = new JobPropertyImpl(project);
        PromotionProcess promotionProcess = promotionProperty.addProcess("promo");
        promotionProcess.conditions.clear();
        ManualCondition manualCondition = new ManualCondition();
        manualCondition.getParameterDefinitions().add(new StringParameterDefinition("PROMOTION_PARAM", "defaultValue"));
        promotionProcess.conditions.add(manualCondition);
        Action approvalAction = new ManualCondition.ManualApproval(promotionProcess.getName(),
		        new LinkedList<>());
        TokenMacroExpressionRecorder recorder = new TokenMacroExpressionRecorder("${PROMOTION_ENV,var=\"PROMOTION_PARAM\"}");
        promotionProcess.getBuildSteps().add(recorder);

        // Act & promote
        FreeStyleBuild build = project.scheduleBuild2(0).get();
        build.addAction(approvalAction);
        build.save();
        Promotion promotion = promotionProcess.considerPromotion2(build,
		        List.of(new StringParameterValue("PROMOTION_PARAM", "FOO"))).get();

        // Check results
        EnvVars env = promotion.getEnvironment(TaskListener.NULL);
        assertEquals("FOO", env.get("PROMOTION_PARAM"), "The PROMOTION_PARAM variable has not been injected");
        assertEquals("FOO", recorder.getCaptured(), "The promotion variable value has not been resolved by the PROMOTION_PARAM macro");
    }

    private static class TokenMacroExpressionRecorder extends Recorder {

        private final String expression;
        private transient String captured;

        @DataBoundConstructor
        public TokenMacroExpressionRecorder(String expression) {

            this.expression = expression;
        }

        public String getCaptured() {
            return captured;
        }

        public String getExpression() {
            return expression;
        }

        @Override
        public BuildStepMonitor getRequiredMonitorService() {
            return BuildStepMonitor.NONE;
        }

        @Override
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            if (build instanceof Promotion) {
                // TODO: It seems to be a bug in the test suite
                AbstractBuild<?, ?> target = ((Promotion)build).getTargetBuildOrFail();
                return performWithParentBuild(build, listener);
            }
            return false;
        }

        private boolean performWithParentBuild(AbstractBuild<?, ?> build, BuildListener listener)
                throws InterruptedException, IOException {
            try {
                captured = TokenMacro.expand(build, listener, expression);
            } catch (MacroEvaluationException ex) {
                throw new IOException(ex);
            }
            return true;
        }

        @TestExtension("testEnvironmentVariableExpansion")
        public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

            @Override
            public boolean isApplicable(Class<? extends AbstractProject> jobType) {
                return true;
            }

            @NonNull
            @Override
            public String getDisplayName() {
                return "Perform Token Macro expression using the parent build";
            }
        }
    }
}
