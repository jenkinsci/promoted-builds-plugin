package hudson.plugins.promoted_builds.integrations.jobdsl;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotionProcess;
import javaposse.jobdsl.plugin.ExecuteDslScripts;
import javaposse.jobdsl.plugin.RemovedJobAction;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CustomMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.describedAs;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@For(PromotionsExtensionPoint.class)
public class PromotionsDslContextExtensionTest {

    private final XmlHelper xmlHelper = new XmlHelper();

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void shouldGenerateTheDefinedJob() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/example-dsl.groovy"));
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(
                new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When        
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        j.assertBuildStatusSuccess(scheduleBuild2);

        final FreeStyleProject generatedJob = j.jenkins.getItemByFullName("test-job", FreeStyleProject.class);
        assertThat(generatedJob, new CustomMatcher<FreeStyleProject>("Job is Promoted-builds-plugin enabled") {
            @Override
            public boolean matches(Object item) {
                return ((FreeStyleProject) item).getProperty(JobPropertyImpl.class) != null;
            }
        });
        final JobPropertyImpl jobProperty = generatedJob.getProperty(JobPropertyImpl.class);
        final String promotionName = "Development";
        assertThat(jobProperty, new CustomMatcher<JobPropertyImpl>("The requested promotion is defined") {
            @Override
            public boolean matches(Object item) {
                return ((JobPropertyImpl) item).getItem(promotionName) != null;
            }
        });
        assertThat(jobProperty.getItem(promotionName), new CustomMatcher<PromotionProcess>("The requested promotion has a configuration file") {
            @Override
            public boolean matches(Object item) {
                return ((PromotionProcess) item).getConfigFile().getFile().exists();
            }
        });
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

        final FreeStyleProject item = j.jenkins.getItemByFullName("copy-artifacts-test", FreeStyleProject.class);
        final String config = item.getProperty(JobPropertyImpl.class).getItem("Development").getConfigFile().asString();
        assertThat(config, xmlHelper.newStringXPathMatcher("count(//selector[@class = 'hudson.plugins.copyartifact.SpecificBuildSelector'])", "1"));
    }

    @Test
    public void testReleaseCondition() throws Exception {
        final FreeStyleProject seedJob = j.createFreeStyleProject();
        final ExecuteDslScripts dslBuilder = new ExecuteDslScripts();
        dslBuilder.setScriptText("freeStyleJob('foo') { properties { promotions { promotion('Deploy on release') { conditions { releaseBuild() } } } } }");
        seedJob.getBuildersList().add(dslBuilder);

        final QueueTaskFuture<FreeStyleBuild> schedule = seedJob.scheduleBuild2(0);

        // release plugin is marked as required but is not in classpath, so expect JobDSL to mark build as UNSTABLE
        j.assertBuildStatus(Result.UNSTABLE, schedule.get(1, TimeUnit.MINUTES));
        final FreeStyleProject producedJob = j.jenkins.getItemByFullName("foo", FreeStyleProject.class);
        assertThat(producedJob, describedAs("Produced project found", notNullValue()));
        final JobPropertyImpl promotedBuildsProperty = producedJob.getProperty(JobPropertyImpl.class);
        assertThat(promotedBuildsProperty, describedAs("promoted-builds property found", notNullValue()));
        final PromotionProcess promotion = promotedBuildsProperty.getItem("Deploy on release");
        assertThat(promotion, describedAs("PromotionProcess found", notNullValue()));
        final String xml = promotion.getConfigFile().asString();
        assertThat(xml, xmlHelper.newStringXPathMatcher("count(/*/conditions[1]/hudson.plugins.release.promotion.ReleasePromotionCondition)", "1"));
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

        final FreeStyleProject generatedJob = j.jenkins.getItemByFullName("Job-from-automatically-generated-DSL", FreeStyleProject.class);
        final String promotion1 = "Development";
        final String promotion2 = "test";
        final String devPromotionConfig = generatedJob.getProperty(JobPropertyImpl.class).getItem(promotion1).getConfigFile().asString();
        final String testPromotionConfig = generatedJob.getProperty(JobPropertyImpl.class).getItem(promotion2).getConfigFile().asString();

        assertThat(devPromotionConfig, allOf(
            xmlHelper.newStringXPathMatcher("count(/*/buildSteps/hudson.plugins.parameterizedtrigger.TriggerBuilder)", "1"),
            xmlHelper.newStringXPathMatcher("count(/*/buildSteps/hudson.plugins.parameterizedtrigger.TriggerBuilder[1]/descendant::hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig)", "1"),
            xmlHelper.newStringXPathMatcher("/*/assignedLabel[1]/text()", "slave1")
        ));
        assertThat(testPromotionConfig, xmlHelper.newStringXPathMatcher("/*/assignedLabel[1]/text()", "slave2"));

        assertThat(generatedJob.getProperty(JobPropertyImpl.class).getItem(promotion1).getAssignedLabelString(), equalTo("slave1"));
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

        final FreeStyleProject createdJob = j.jenkins.getItemByFullName("configure-block-test", FreeStyleProject.class);
        final String promotionConfig = createdJob.getProperty(JobPropertyImpl.class).getItem("PromotionName").getConfigFile().asString();

        assertThat(promotionConfig, allOf(
            xmlHelper.newStringXPathMatcher("/*/buildSteps[1]/foo.bar.CustomAction[1]/customAttribute[1]/text()", "customValue"),
            xmlHelper.newStringXPathMatcher("/*/buildSteps[1]/foo.bar.CustomAction[1]/@foo", "bar")
        ));
    }

}
