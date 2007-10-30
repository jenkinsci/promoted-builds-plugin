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
public class Promotion extends AbstractBuild<PromotionProcess, Promotion> {
    public Promotion(PromotionProcess job) throws IOException {
        super(job);
    }

    public Promotion(PromotionProcess job, Calendar timestamp) {
        super(job, timestamp);
    }

    public Promotion(PromotionProcess project, File buildDir) throws IOException {
        super(project, buildDir);
    }

    public void run() {
        run(new RunnerImpl());
    }

    protected class RunnerImpl extends AbstractRunner {
        protected Result doRun(BuildListener listener) throws Exception {
            if(!preBuild(listener,project.getBuildSteps()))
                return Result.FAILURE;

            if(!build(listener,project.getBuildSteps()))
                return Result.FAILURE;

            return null;
        }

        protected void post2(BuildListener listener) throws Exception {
            // no-op
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
