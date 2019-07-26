package hudson.plugins.promoted_builds;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.Describable;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import jenkins.model.Jenkins;

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
     * @deprecated
     */
    @CheckForNull
    public PromotionBadge isMet(Run<?,?> build) {
        return null;
    }

    /**
     * Checks if the promotion criteria is met.
     *
     * @param promotionProcess
     *      The promotion process being evaluated for qualification
     * @param build
     *      The build for which the promotion is considered.
     * @return
     *      non-null if the promotion condition is met. This object is then recorded so that
     *      we know how a build was promoted.
     *      Null if otherwise, meaning it shouldn't be promoted.
     */
    public PromotionBadge isMet(PromotionProcess promotionProcess, Run<?,?> build) {
        // just call the deprecated version to support legacy conditions
        return isMet(build);
    }

    public PromotionConditionDescriptor getDescriptor() {
        return (PromotionConditionDescriptor)Jenkins.get().getDescriptor(getClass());
    }

    /**
     * Returns all the registered {@link PromotionConditionDescriptor}s.
     */
    public static DescriptorExtensionList<PromotionCondition,PromotionConditionDescriptor> all() {
        return Jenkins.get().<PromotionCondition,PromotionConditionDescriptor>getDescriptorList(PromotionCondition.class);
    }

    /**
     * Returns a subset of {@link PromotionConditionDescriptor}s that applys to the given project.
     */
    public static List<PromotionConditionDescriptor> getApplicableTriggers(Job<?,?> p) {
        List<PromotionConditionDescriptor> r = new ArrayList<PromotionConditionDescriptor>();
        for (PromotionConditionDescriptor t : all()) {
            if(t.isApplicable(p))
                r.add(t);
        }
        return r;
    }
}
