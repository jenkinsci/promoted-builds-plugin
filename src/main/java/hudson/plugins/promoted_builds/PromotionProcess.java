package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStep;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

/**
 * Records a promotion process.
 *
 * @author Kohsuke Kawaguchi
 */
public class PromotionProcess extends AbstractBuild<PromotionProcessJob,PromotionProcess> {
    public PromotionProcess(PromotionProcessJob job) throws IOException {
        super(job);
    }

    public PromotionProcess(PromotionProcessJob job, Calendar timestamp) {
        super(job, timestamp);
    }

    public PromotionProcess(PromotionProcessJob project, File buildDir) throws IOException {
        super(project, buildDir);
    }

    public void run() {
        run(new RunnerImpl());
    }

    protected class RunnerImpl extends AbstractRunner {
        protected Result doRun(BuildListener listener) throws Exception {
            if(!preBuild(listener,project.config.buildSteps))
                return Result.FAILURE;

            if(!build(listener,project.config.buildSteps))
                return Result.FAILURE;

            return null;
        }

        protected void post2(BuildListener listener) throws Exception {
            // no-op
        }

        private boolean build(BuildListener listener, BuildStep[] steps) throws IOException, InterruptedException {
            for( BuildStep bs : steps )
                if(!bs.perform(PromotionProcess.this, launcher, listener))
                    return false;
            return true;
        }

        private boolean preBuild(BuildListener listener,BuildStep[] steps) {
            for( BuildStep bs : steps )
                if(!bs.prebuild(PromotionProcess.this,listener))
                    return false;
            return true;
        }
    }
}
