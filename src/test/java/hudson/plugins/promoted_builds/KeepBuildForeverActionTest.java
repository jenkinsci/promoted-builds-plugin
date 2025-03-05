package hudson.plugins.promoted_builds;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Result;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.Fingerprinter;
import hudson.tasks.Recorder;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static hudson.plugins.promoted_builds.util.ItemListenerHelper.fireItemListeners;
import static org.junit.jupiter.api.Assertions.*;

@WithJenkins
class KeepBuildForeverActionTest {

    @Test
    void testCanMarkBuildKeepForever(JenkinsRule j) throws Exception {
        FreeStyleProject upJob = createProject(j, "up");
        upJob.getBuildersList().add(successfulBuilder());
        FreeStyleProject downJob = createProject(j, "down");
        downJob.getBuildersList().add(successfulBuilder());

        PromotionProcess promotionJob = createDownstreamSuccessPromotion(upJob, downJob);
        promotionJob.getBuildSteps().add(new KeepBuildForeverAction());

        // fire ItemListeners, this includes ArtifactArchiver,Migrator to make this test compatible with jenkins 1.575+
        fireItemListeners();

        FreeStyleBuild upBuild = j.assertBuildStatusSuccess(upJob.scheduleBuild2(0).get());
        assertFalse(upBuild.isKeepLog());

        j.assertBuildStatusSuccess(downJob.scheduleBuild2(0).get());
        waitForBuild(promotionJob, 1);
        assertTrue(upBuild.isKeepLog());
    }

    @Test
    void testDoesNotMarkBuildIfPromotionNotGoodEnough(JenkinsRule j) throws Exception {
        FreeStyleProject upJob = createProject(j, "up");
        upJob.getBuildersList().add(successfulBuilder());
        FreeStyleProject downJob = createProject(j, "down");
        downJob.getBuildersList().add(successfulBuilder());

        PromotionProcess promotionJob = createDownstreamSuccessPromotion(upJob, downJob);
        promotionJob.getBuildSteps().add(new FixedResultBuilder(Result.FAILURE));
        promotionJob.getBuildSteps().add(new KeepBuildForeverAction());

        // fire ItemListeners, this includes ArtifactArchiver,Migrator to make this test compatible with jenkins 1.575+
        fireItemListeners();

        FreeStyleBuild upBuild = j.assertBuildStatusSuccess(upJob.scheduleBuild2(0).get());
        assertFalse(upBuild.isKeepLog());

        j.assertBuildStatusSuccess(downJob.scheduleBuild2(0).get());
        waitForBuild(promotionJob, 1);
        assertFalse(upBuild.isKeepLog());
    }

    @Test
    void testDoesNotCareAboutResultOfOriginalBuild(JenkinsRule j) throws Exception {
        FreeStyleProject upJob = createProject(j, "up");
        upJob.getBuildersList().add(new FixedResultBuilder(Result.FAILURE));
        FreeStyleProject downJob = createProject(j, "down");
        downJob.getBuildersList().add(successfulBuilder());

        PromotionProcess promotionJob = createDownstreamSuccessPromotion(upJob, downJob);
        promotionJob.getBuildSteps().add(new KeepBuildForeverAction());

        // fire ItemListeners, this includes ArtifactArchiver,Migrator to make this test compatible with jenkins 1.575+
        fireItemListeners();

        FreeStyleBuild upBuild = j.assertBuildStatus(Result.FAILURE, upJob.scheduleBuild2(0).get());
        assertFalse(upBuild.isKeepLog());

        j.assertBuildStatusSuccess(downJob.scheduleBuild2(0).get());
        waitForBuild(promotionJob, 1);
        assertTrue(upBuild.isKeepLog());
    }

    @Test
    void testDoesNotMarkBuildIfBuildNotPromotion(JenkinsRule j) throws Exception {
        FreeStyleProject job = createProject(j, "job");
        job.getBuildersList().add(successfulBuilder());
        job.getPublishersList().add(new KeepBuildForeverAction());

        // fire ItemListeners, this includes ArtifactArchiver,Migrator to make this test compatible with jenkins 1.575+
        fireItemListeners();

        FreeStyleBuild build = j.assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0).get());
        assertFalse(build.isKeepLog());
    }

    private void waitForBuild(final Job job, final int buildNumber) throws Exception {
        waitFor(() -> (job.getBuildByNumber(buildNumber) != null) && !job.getBuildByNumber(buildNumber).isBuilding(), 2000);
    }

    private void waitFor(final WaitCondition condition, long timeout) throws Exception {
        Thread waiter = new Thread(() -> {
            try {
                while (!condition.isMet()) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException ie) { }
        });
        waiter.start();
        waiter.join(timeout);
        if (waiter.isAlive()) {
            waiter.interrupt();
        }
        if (!condition.isMet())
            fail("Condition not met");
    }

    private PromotionProcess createDownstreamSuccessPromotion(FreeStyleProject upStream, FreeStyleProject downStream)
            throws Descriptor.FormException, IOException {
        JobPropertyImpl promotionProperty = new JobPropertyImpl(upStream);
        upStream.addProperty(promotionProperty);
        PromotionProcess promotionJob = promotionProperty.addProcess("promotion");
        promotionJob.conditions.add(new DownstreamPassCondition(downStream.getName()));
        return promotionJob;
    }

    private FreeStyleProject createProject(JenkinsRule j, String name) throws Exception {
        FreeStyleProject project = j.createFreeStyleProject(name);
        project.getPublishersList().replaceBy(createFingerprinters());
        return project;
    }

    private List<Recorder> createFingerprinters() {
        Recorder r1 = new ArtifactArchiver("*", null, false);
        Recorder r2 = new Fingerprinter("", true);
        return Arrays.asList(r1, r2);
    }

    private FixedResultBuilder successfulBuilder() {
        return new FixedResultBuilder(Result.SUCCESS);
    }

    public interface WaitCondition {
        boolean isMet();
    }

    public static class FixedResultBuilder extends TestBuilder {
        private final Result buildResult;
        FixedResultBuilder(Result buildResult) {
            this.buildResult = buildResult;
        }
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            FilePath workspace = build.getWorkspace();
            if (workspace == null) {
                throw new AbortException("Workspace is null in " + FixedResultBuilder.class.getName());
            }
            workspace.child("my.file").write("Hello world!", "UTF-8");
            build.setResult(buildResult);
            return true;
        }
    }

}
