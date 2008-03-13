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
     * @return
     *      non-null if the promotion condition is met. This object is then recorded so that
     *      we know how a build was promoted.
     *      Null if otherwise, meaning it shouldn't be promoted.
     */
    public abstract PromotionBadge isMet(AbstractBuild<?,?> build);

    public abstract PromotionConditionDescriptor getDescriptor();
}
