package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.InvisibleAction;


/**
 * Remembers what build it's promoting. Attached to {@link Promotion}.
 *
 * @author Kohsuke Kawaguchi
 */
public class PromotionTargetAction extends InvisibleAction {
    private final String jobName;
    private final int number;
    private final AbstractProject<?, ?> project;

    public PromotionTargetAction(AbstractBuild<?,?> build) {
        jobName = build.getParent().getFullName();
        number = build.getNumber();
        project = build.getProject();
    }

    public AbstractBuild<?,?> resolve() {
        return project.getBuildByNumber(number);
    }


}
