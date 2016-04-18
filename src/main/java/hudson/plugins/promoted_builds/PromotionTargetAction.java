package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.InvisibleAction;
import hudson.plugins.promoted_builds.util.JenkinsHelper;
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
        AbstractProject<?,?> j = JenkinsHelper.getInstance().getItemByFullName(jobName, AbstractProject.class);
        if (j==null)    return null;
        return j.getBuildByNumber(number);
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
