package hudson.plugins.promoted_builds;

import hudson.Launcher;
import hudson.model.*;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.DontKeepBuildForeverAction;
import hudson.plugins.promoted_builds.KeepBuildForeverAction;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.Fingerprinter;
import hudson.tasks.Recorder;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.TestBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DontKeepBuildForeverActionTest extends HudsonTestCase {
    
    public void testCanMarkBuildKeepForever() throws Exception {
        FreeStyleProject upJob = createProject("up");
        upJob.getBuildersList().add(successfulBuilder());
        FreeStyleProject downJob = createProject("down");
        downJob.getBuildersList().add(successfulBuilder());

        PromotionProcess promotionJob = createDownstreamSuccessPromotion(upJob, downJob);
        promotionJob.getBuildSteps().add(new KeepBuildForeverAction());
        promotionJob.getBuildSteps().add(new DontKeepBuildForeverAction());

        FreeStyleBuild upBuild = assertBuildStatusSuccess(upJob.scheduleBuild2(0).get());
        assertFalse(upBuild.isKeepLog());

        assertBuildStatusSuccess(downJob.scheduleBuild2(0).get());
        waitForBuild(promotionJob, 1);
        assertFalse(upBuild.isKeepLog());
    }
    
    public void testDoesNotMarkBuildIfPromotionNotGoodEnough() throws Exception {
        FreeStyleProject upJob = createProject("up");
        upJob.getBuildersList().add(successfulBuilder());
        FreeStyleProject downJob = createProject("down");
        downJob.getBuildersList().add(successfulBuilder());

        PromotionProcess promotionJob = createDownstreamSuccessPromotion(upJob, downJob);
        promotionJob.getBuildSteps().add(new FixedResultBuilder(Result.FAILURE));
        promotionJob.getBuildSteps().add(new KeepBuildForeverAction());
        promotionJob.getBuildSteps().add(new DontKeepBuildForeverAction());

        FreeStyleBuild upBuild = assertBuildStatusSuccess(upJob.scheduleBuild2(0).get());
        assertFalse(upBuild.isKeepLog());
        
        assertBuildStatusSuccess(downJob.scheduleBuild2(0).get());
        waitForBuild(promotionJob, 1);
        assertFalse(upBuild.isKeepLog());
    }

    public void testDoesNotCareAboutResultOfOriginalBuild() throws Exception {
        FreeStyleProject upJob = createProject("up");
        upJob.getBuildersList().add(new FixedResultBuilder(Result.FAILURE));
        FreeStyleProject downJob = createProject("down");
        downJob.getBuildersList().add(successfulBuilder());

        PromotionProcess promotionJob = createDownstreamSuccessPromotion(upJob, downJob);
        promotionJob.getBuildSteps().add(new KeepBuildForeverAction());
        promotionJob.getBuildSteps().add(new DontKeepBuildForeverAction());

        FreeStyleBuild upBuild = assertBuildStatus(Result.FAILURE, upJob.scheduleBuild2(0).get());
        assertFalse(upBuild.isKeepLog());
        
        assertBuildStatusSuccess(downJob.scheduleBuild2(0).get());
        waitForBuild(promotionJob, 1);
        assertFalse(upBuild.isKeepLog());
    }

    public void testDoesNotMarkBuildIfBuildNotPromotion() throws Exception {
        FreeStyleProject job = createProject("job");
        job.getBuildersList().add(successfulBuilder());
        job.getPublishersList().add(new KeepBuildForeverAction());
        job.getPublishersList().add(new DontKeepBuildForeverAction());

        FreeStyleBuild build = assertBuildStatus(Result.FAILURE, job.scheduleBuild2(0).get());
        assertFalse(build.isKeepLog());
    }

    private void waitForBuild(final Job job, final int buildNumber) throws Exception {
        waitFor(new WaitCondition() {
            public boolean isMet() {
                return (job.getBuildByNumber(buildNumber) != null) && !job.getBuildByNumber(buildNumber).isBuilding();
            }
        }, 2000);
    }

    private void waitFor(final WaitCondition condition, long timeout) throws Exception {
        Thread waiter = new Thread() {
            public void run() {
                try {
                    while (!condition.isMet()) {
                        Thread.sleep(100);
                    }
                } catch (InterruptedException ie) { }
            }
        };
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
    
    private FreeStyleProject createProject(String name) throws Exception {
        FreeStyleProject project = createFreeStyleProject(name);
        project.getPublishersList().replaceBy(createFingerprinters());
        return project;
    }
    
    private List<Recorder> createFingerprinters() {
        return Arrays.asList(
            new ArtifactArchiver("*", null, false),
            new Fingerprinter("", true)
        );
    }

    private FixedResultBuilder successfulBuilder() {
        return new FixedResultBuilder(Result.SUCCESS);
    }
    
    public interface WaitCondition {
        boolean isMet();
    }
    
    public static class FixedResultBuilder extends TestBuilder {
        private Result buildResult;
        FixedResultBuilder(Result buildResult) {
            this.buildResult = buildResult;
        }
        public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                throws InterruptedException, IOException {
            build.getWorkspace().child("my.file").write("Hello world!", "UTF-8");
            build.setResult(buildResult);
            return true;
        }
    }
    
}
