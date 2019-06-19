package hudson.plugins.promoted_builds.pipeline;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.util.JenkinsHelper;

public class PipelineTargetAction {

    private final String jobName;
    private final int number;

    public PipelineTargetAction(Run<?,?> run) {
        jobName = run.getParent().getFullName();
        number = run.getNumber();
    }

    @CheckForNull
    public Run<?,?> resolve() {
        Job<?,?> j = JenkinsHelper.getInstance().getItemByFullName(jobName, Job.class);
        if (j==null)    return null;
        return j.getBuildByNumber(number);
    }

    @CheckForNull
    public Run<?,?> resolve(PipelinePromotionProcess parent) {
        Run<?,?> run = this.resolve();
        if (run != null){
            return run;
        }
        //In case of project renamed.
        Job<?,?> j = parent.getOwner();
        if (j==null)    return null;
        return j.getBuildByNumber(number);
    }

    // For this "Promotion" should also be refactored as the "return" does not compile correctly.
    /*
    public Run<?,?> resolve(Promotion parent) {
        return resolve(parent.getParent());
    }
    */



}
