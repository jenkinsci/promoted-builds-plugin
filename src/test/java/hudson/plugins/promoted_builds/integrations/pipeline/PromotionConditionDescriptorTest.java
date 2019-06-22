package hudson.plugins.promoted_builds.integrations.pipeline;

import hudson.Functions;
import hudson.model.*;
import hudson.tasks.BatchFile;
import hudson.tasks.Builder;
import hudson.tasks.Shell;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;


public class PromotionConditionDescriptorTest {
    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule
    public JenkinsRule j = new JenkinsRule();
    @Rule public LoggerRule logging = new LoggerRule();

    @Test
    public void free() throws Exception {
        final String message = "Testing done for pipeline";

        FreeStyleProject p = j.createFreeStyleProject("p");
        FreeStyleBuild build = j.buildAndAssertSuccess(p);
        Builder step = Functions.isWindows() ? new BatchFile(message) : new Shell(message);
        p.getBuildersList().add(step);
        j.assertLogContains(message,build);

    }

    @Test
    public void pipe() throws Exception {

        final WorkflowJob jo = j.jenkins.createProject(WorkflowJob.class, "jo");
        jo.setDefinition(new CpsFlowDefinition("node { echo 'This is pipeline inside the promoted-builds' }", true));

        WorkflowRun build = j.buildAndAssertSuccess(jo);

        j.assertLogContains("This is pipeline inside the promoted-builds",build);
    }

}