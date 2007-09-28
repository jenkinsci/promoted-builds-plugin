package hudson.plugins.promoted_builds;

import hudson.ExtensionPoint;
import hudson.model.AbstractBuild;
import hudson.model.Describable;

/**
 * Extension point for defining a promotion criteria.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class PromotionCondition implements ExtensionPoint, Describable<PromotionCondition> {
    /**
     * Checks if the promotion criteria is met.
     *
     * @param build
     *      The build for which the promotion is considered.
     * @return true if so.
     */
    public abstract boolean isMet(AbstractBuild<?,?> build);

    public abstract PromotionConditionDescriptor getDescriptor();
}
