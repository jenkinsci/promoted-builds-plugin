package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.InvisibleAction;
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

    public PromotionTargetAction(AbstractBuild<?,?> build) {
        jobName = build.getParent().getFullName();
        number = build.getNumber();
    }

    @CheckForNull
    public AbstractBuild<?,?> resolve() {
        AbstractProject<?,?> job = Jenkins.get().getItemByFullName(jobName, AbstractProject.class);
        return job.getBuildByNumber(number);
    }

    @CheckForNull
    public AbstractBuild<?,?> resolve(PromotionProcess parent) {
        AbstractBuild<?,?> build = this.resolve();
        if (build !=null){
            return build;
        }
        //In case of project renamed.
        AbstractProject<?,?> j = parent.getOwner();
        if (j==null)    return null;
        return j.getBuildByNumber(number);
    }

    public AbstractBuild<?,?> resolve(Promotion parent) {
        return resolve(parent.getParent());
    }
}
