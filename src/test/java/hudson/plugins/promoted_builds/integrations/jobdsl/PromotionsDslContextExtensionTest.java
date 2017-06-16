package hudson.plugins.promoted_builds.integrations.jobdsl;

import com.google.common.io.Files;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.model.queue.QueueTaskFuture;

import java.io.File;
import java.nio.charset.Charset;

import javaposse.jobdsl.plugin.RemovedJobAction;
import javaposse.jobdsl.plugin.ExecuteDslScripts;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class PromotionsDslContextExtensionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testShouldGenerateTheDefindedJob() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/example-dsl.groovy"));
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(
                new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When        
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        j.assertBuildStatusSuccess(scheduleBuild2);
    }
    

    @Test
    public void testShouldGenerateTheDefindedComplexJob() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/complex-example-dsl.groovy"));
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(
                new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When        
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        j.assertBuildStatusSuccess(scheduleBuild2);
    }

    @Test
    public void testShouldGenerateTheCopyArtifactsJob() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/copyartifacts-example-dsl.groovy"));
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(
                new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then (unstable b/c we aren't including the CopyArtifacts dependency)
        j.assertBuildStatus(Result.UNSTABLE, scheduleBuild2.get());

        TopLevelItem item = j.jenkins.getItem("copy-artifacts-test");
        File config = new File(item.getRootDir(), "promotions/Development/config.xml");
        String content = Files.toString(config, Charset.forName("UTF-8"));
        assert content.contains("<selector class=\"hudson.plugins.copyartifact.SpecificBuildSelector\">");
    }

}
