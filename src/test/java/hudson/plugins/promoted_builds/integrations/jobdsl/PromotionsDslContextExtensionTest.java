package hudson.plugins.promoted_builds.integrations.jobdsl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.model.queue.QueueTaskFuture;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Pattern;

import javaposse.jobdsl.plugin.RemovedJobAction;
import javaposse.jobdsl.plugin.ExecuteDslScripts;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class PromotionsDslContextExtensionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule(); 

    private ExecuteDslScripts createScript(String dsl) {
        ExecuteDslScripts script = new ExecuteDslScripts();
        script.setScriptText(dsl);
        script.setRemovedJobAction(RemovedJobAction.DELETE);
        script.setIgnoreMissingFiles(false);
        return script;
    }

    @Test
    public void testShouldGenerateTheDefindedJob() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/example-dsl.groovy"));
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(createScript(dsl));
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
        seedJob.getBuildersList().add(createScript(dsl));
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
        seedJob.getBuildersList().add(createScript(dsl));
        // When
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then (unstable b/c we aren't including the CopyArtifacts dependency)
        j.assertBuildStatus(Result.UNSTABLE, scheduleBuild2.get());

        TopLevelItem item = j.jenkins.getItem("copy-artifacts-test");
        File config = new File(item.getRootDir(), "promotions/Development/config.xml");
        String content = Files.readString(config.toPath(), StandardCharsets.UTF_8);
        assert content.contains("<selector class=\"hudson.plugins.copyartifact.SpecificBuildSelector\">");
    }
    
    @Test
    public void testShouldGenerateTheJobWithBuildWrappers() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/buildwrapper-example-dsl.groovy"));
        System.out.println(dsl);
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(createScript(dsl));
        // When
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then (unstable b/c we aren't including the Timestamper dependency)
        j.assertBuildStatus(Result.UNSTABLE, scheduleBuild2.get());
        
        TopLevelItem item = j.jenkins.getItem("build-wrapper-test");
        assertNotNull(item);
        File config = new File(item.getRootDir(), "promotions/build-wrapper-promotion/config.xml");
        String content = Files.readString(config.toPath(), StandardCharsets.UTF_8);
       
        assertTrue(Pattern.compile("<buildWrappers>\\s+<hudson\\.plugins\\.timestamper\\.TimestamperBuildWrapper/>\\s+</buildWrappers>")
                .matcher(content).find());
    }

    @Test
    public void testShouldGenerateTheDynamicDslJob() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/dynamic-dsl-example-dsl.groovy"));
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(createScript(dsl));
        // When
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        j.assertBuildStatusSuccess(scheduleBuild2.get());

        TopLevelItem item = j.jenkins.getItem("dynamic-dsl-test");
        File config = new File(item.getRootDir(), "promotions/Development/config.xml");
        String content = Files.toString(config, Charset.forName("UTF-8"));
        assert content.contains("<javaposse.jobdsl.plugin.ExecuteDslScripts>");
    }

}
