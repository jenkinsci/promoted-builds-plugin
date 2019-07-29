package hudson.plugins.promoted_builds;

import hudson.model.InvisibleAction;
import hudson.model.Job;
import hudson.model.Run;
import jenkins.model.Jenkins;

import javax.annotation.CheckForNull;

/**
 * Remembers what build it's promoting. Attached to {@link Promotion}.
 *
 * @author Kohsuke Kawaguchi
 */
public class PromotionTargetAction extends InvisibleAction {
    private final String jobName;
    private final int number;

    public PromotionTargetAction(Run<?,?> build) {
        jobName = build.getParent().getFullName();
        number = build.getNumber();
    }

    @CheckForNull
    public Run<?,?> resolve() {
        Job<?,?> job = Jenkins.get().getItemByFullName(jobName, Job.class);
        if (job == null) {
            return null;
        }
        return job.getBuildByNumber(number);
    }

    @CheckForNull
    public Run<?,?> resolve(PromotionProcess parent) {
        Run<?,?> run = this.resolve();
        if (run !=null){
            return run;
        }
        //In case of project renamed.
        Job<?,?> j = parent.getOwner();
        if (j==null)    return null;
        return j.getBuildByNumber(number);
    }

    public Run<?,?> resolve(Promotion parent) {
        return resolve(parent.getParent());
    }
}
