package hudson.plugins.promoted_builds.integrations.jobdsl;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.queue.QueueTaskFuture;

import java.io.File;

import javaposse.jobdsl.plugin.RemovedJobAction;
import javaposse.jobdsl.plugin.ExecuteDslScripts;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

public class PromotionsDslContextExtensionTest extends HudsonTestCase {

    @Test
    public void testShouldGenerateTheDefindedJob() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/example-dsl.groovy"));
        FreeStyleProject seedJob = createFreeStyleProject();
        seedJob.getBuildersList().add(
                new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When        
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        assertBuildStatusSuccess(scheduleBuild2);
    }
    

    @Test
    public void testShouldGenerateTheDefindedComplexJob() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/complex-example-dsl.groovy"));
        FreeStyleProject seedJob = createFreeStyleProject();
        seedJob.getBuildersList().add(
                new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When        
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        assertBuildStatusSuccess(scheduleBuild2);
    }
}
