package hudson.plugins.promoted_builds.integrations.jobdsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleBuild;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.conditions.ManualCondition;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.model.queue.QueueTaskFuture;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Pattern;

import javaposse.jobdsl.plugin.RemovedJobAction;
import javaposse.jobdsl.plugin.ExecuteDslScripts;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class PromotionsDslContextExtensionTest {

    private ExecuteDslScripts createScript(String dsl) {
        ExecuteDslScripts script = new ExecuteDslScripts();
        script.setScriptText(dsl);
        script.setRemovedJobAction(RemovedJobAction.DELETE);
        script.setIgnoreMissingFiles(false);
        return script;
    }

    @Test
    void testShouldGenerateTheDefindedJob(JenkinsRule j) throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/example-dsl.groovy"), StandardCharsets.UTF_8);
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(createScript(dsl));
        // When
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        j.assertBuildStatusSuccess(scheduleBuild2);
    }


    @Test
    void testShouldGenerateTheDefindedComplexJob(JenkinsRule j) throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/complex-example-dsl.groovy"), StandardCharsets.UTF_8);
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(createScript(dsl));
        // When
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        j.assertBuildStatusSuccess(scheduleBuild2);
    }

    @Test
    void testShouldGenerateTheCopyArtifactsJob(JenkinsRule j) throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/copyartifacts-example-dsl.groovy"), StandardCharsets.UTF_8);
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
    void testShouldGenerateTheJobWithBuildWrappers(JenkinsRule j) throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/buildwrapper-example-dsl.groovy"), StandardCharsets.UTF_8);
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
    void testShouldNotRewriteUnchangedPromotions(JenkinsRule j) throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/example-dsl.groovy"), StandardCharsets.UTF_8);
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(createScript(dsl));
        j.assertBuildStatusSuccess(seedJob.scheduleBuild2(0));

        FreeStyleProject job = j.jenkins.getItemByFullName("test-job", FreeStyleProject.class);
        PromotionProcess process = job.getProperty(JobPropertyImpl.class).getItem("Development");
        File config = new File(job.getRootDir(), "promotions/Development/config.xml");
        long lastModified = System.currentTimeMillis() - 3_600_000L;
        assertTrue(config.setLastModified(lastModified));

        // When
        j.assertBuildStatusSuccess(seedJob.scheduleBuild2(0));

        // Then
        assertEquals(lastModified, config.lastModified(), "the unchanged promotion was rewritten");
        job = j.jenkins.getItemByFullName("test-job", FreeStyleProject.class);
        assertSame(process, job.getProperty(JobPropertyImpl.class).getItem("Development"), "the job was reloaded");
    }

    @Test
    void testShouldRewriteChangedPromotions(JenkinsRule j) throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/example-dsl.groovy"), StandardCharsets.UTF_8);
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(createScript(dsl));
        j.assertBuildStatusSuccess(seedJob.scheduleBuild2(0));

        // When
        seedJob.getBuildersList().replace(createScript(dsl.replace("manual('tester')", "manual('reviewer')")));
        j.assertBuildStatusSuccess(seedJob.scheduleBuild2(0));

        // Then
        FreeStyleProject job = j.jenkins.getItemByFullName("test-job", FreeStyleProject.class);
        File config = new File(job.getRootDir(), "promotions/Development/config.xml");
        assertTrue(Files.readString(config.toPath(), StandardCharsets.UTF_8).contains("reviewer"));
        ManualCondition condition = job.getProperty(JobPropertyImpl.class).getItem("Development").conditions.get(ManualCondition.class);
        assertEquals("reviewer", condition.getUsers(), "the job wasn't reloaded");
    }

    @Test
    void testShouldGenerateTheDynamicDslJob(JenkinsRule j) throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/dynamic-dsl-example-dsl.groovy"), StandardCharsets.UTF_8);
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(createScript(dsl));
        // When
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        j.assertBuildStatusSuccess(scheduleBuild2.get());

        TopLevelItem item = j.jenkins.getItem("dynamic-dsl-test");
        File config = new File(item.getRootDir(), "promotions/Development/config.xml");
        String content = Files.readString(config.toPath());
        assert content.contains("<javaposse.jobdsl.plugin.ExecuteDslScripts");
    }

}
