package hudson.plugins.promoted_builds.integrations.jobdsl;

import com.google.common.io.Files;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.model.queue.QueueTaskFuture;
import javaposse.jobdsl.plugin.ExecuteDslScripts;
import javaposse.jobdsl.plugin.RemovedJobAction;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.nio.charset.Charset;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@For(PromotionsExtensionPoint.class)
public class PromotionsDslContextExtensionTest {

    private final XmlHelper xmlHelper = new XmlHelper();

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
        assert content.contains("<selector class='hudson.plugins.copyartifact.SpecificBuildSelector'>");
    }

    @Test
    @Issue("JENKINS-39342")
    public void testAutomaticallyGeneratedDsl() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/automatically-generated-dsl.groovy"));
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(
            new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        j.assertBuildStatusSuccess(scheduleBuild2);
    }

    @Test
    @Issue("JENKINS-34982")
    public void testConfigureBlock() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/configure-block-dsl.groovy"));
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(
            new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        j.assertBuildStatusSuccess(scheduleBuild2);

        final Item createdJob = j.jenkins.getItemByFullName("configure-block-test");
        final File promotionConfig = new File(createdJob.getRootDir(), "promotions/PromotionName/config.xml");

        assertTrue("Configuration file must exist", promotionConfig.exists());
        assertThat(promotionConfig, xmlHelper.newFileXPathMatcher("/*/buildSteps[1]/foo.bar.CustomAction[1]/customAttribute[1]/text()", "customValue"));
        assertThat(promotionConfig, xmlHelper.newFileXPathMatcher("/*/buildSteps[1]/foo.bar.CustomAction[1]/@foo", "bar"));
    }

}
