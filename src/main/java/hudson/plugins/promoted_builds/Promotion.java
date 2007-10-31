package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStep;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

/**
 * Records a promotion process.
 *
 * @author Kohsuke Kawaguchi
 */
public class Promotion extends AbstractBuild<PromotionProcess,Promotion> {
    /**
     * The build number of the project that this promotion promoted.
     * @see #getTarget()
     */
    private int targetBuildNumber;

    public Promotion(PromotionProcess job) throws IOException {
        super(job);
    }

    public Promotion(PromotionProcess job, Calendar timestamp) {
        super(job, timestamp);
    }

    public Promotion(PromotionProcess project, File buildDir) throws IOException {
        super(project, buildDir);
    }

    /**
     * Gets the build that this promotion promoted.
     */
    public AbstractBuild<?,?> getTarget() {
        return getParent().getOwner().getBuildByNumber(targetBuildNumber);
    }

    /**
     * Gets the {@link Status} object that keeps track of what {@link Promotion}s are
     * performed for a build, including this {@link Promotion}.
     */
    public Status getStatus() {
        return getTarget().getAction(PromotedBuildAction.class).getPromotion(getParent().getName());
    }

    public void run() {
        run(new RunnerImpl());
    }

    protected class RunnerImpl extends AbstractRunner {
        protected Result doRun(BuildListener listener) throws Exception {
            AbstractBuild<?,?> target;

            // which build are we trying to promote?
            if(project.queue==null) {
                listener.getLogger().println("Nothing to promote here. Aborting");
                return Result.ABORTED;
            }

            // perform the update of queue and Status atomically,
            // so that no one gets into a situation that they think they got dropped from the queue
            // without having a build being performed.
            synchronized (project.queue) {
                if(project.queue.isEmpty()) {
                    listener.getLogger().println("Nothing to promote here. Aborting");
                    return Result.ABORTED;
                }
                target = project.queue.remove(0);
                targetBuildNumber = target.getNumber();

                // if there's more in the queue schedule another one.
                if(!project.queue.isEmpty())
                    project.scheduleBuild(); // the call to a deprecated method is intentional here.
            }

            listener.getLogger().println("Promoting "+target);

            getStatus().addPromotionAttempt(Promotion.this);

            // start with SUCCESS, unless someone makes it a failure
            setResult(Result.SUCCESS);

            if(!preBuild(listener,project.getBuildSteps()))
                return Result.FAILURE;

            if(!build(listener,project.getBuildSteps()))
                return Result.FAILURE;

            return null;
        }

        protected void post2(BuildListener listener) throws Exception {
            if(getResult()== Result.SUCCESS)
                getStatus().onSuccessfulPromotion(Promotion.this);
            // persist the updated build record
            getTarget().save();
        }

        private boolean build(BuildListener listener, List<BuildStep> steps) throws IOException, InterruptedException {
            for( BuildStep bs : steps )
                if(!bs.perform(Promotion.this, launcher, listener))
                    return false;
            return true;
        }

        private boolean preBuild(BuildListener listener, List<BuildStep> steps) {
            for( BuildStep bs : steps )
                if(!bs.prebuild(Promotion.this,listener))
                    return false;
            return true;
        }
    }
}
