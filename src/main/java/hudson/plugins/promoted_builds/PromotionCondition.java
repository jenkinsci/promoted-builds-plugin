package hudson.plugins.promoted_builds;

import hudson.ExtensionPoint;
import hudson.DescriptorExtensionList;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCM;
import hudson.model.AbstractBuild;
import hudson.model.Describable;
import hudson.model.Hudson;
import hudson.model.AbstractProject;

import java.util.List;
import java.util.ArrayList;

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

    public PromotionConditionDescriptor getDescriptor() {
        return (PromotionConditionDescriptor)Hudson.getInstance().getDescriptor(getClass());
    }

    /**
     * Returns all the registered {@link PromotionConditionDescriptor}s.
     */
    public static DescriptorExtensionList<PromotionCondition,PromotionConditionDescriptor> all() {
        return Hudson.getInstance().getDescriptorList(PromotionCondition.class);
    }

    /**
     * Returns a subset of {@link PromotionConditionDescriptor}s that applys to the given project.
     */
    public static List<PromotionConditionDescriptor> getApplicableTriggers(AbstractProject<?,?> p) {
        List<PromotionConditionDescriptor> r = new ArrayList<PromotionConditionDescriptor>();
        for (PromotionConditionDescriptor t : all()) {
            if(t.isApplicable(p))
                r.add(t);
        }
        return r;
    }
}
