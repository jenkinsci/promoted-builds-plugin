package hudson.plugins.promoted_builds;

import hudson.model.AbstractProject;
import hudson.model.Descriptor;

/**
 * {@link Descriptor} for {@link PromotionCondition}.
 *
 * @author Kohsuke Kawaguchi
 * @see PromotionCondition#all()
 * @see PromotionCondition#getApplicableTriggers(AbstractProject) 
 */
public abstract class PromotionConditionDescriptor extends Descriptor<PromotionCondition> {
    protected PromotionConditionDescriptor(Class<? extends PromotionCondition> clazz) {
        super(clazz);
    }

    protected PromotionConditionDescriptor() {
        super();
    }

    /**
     * Returns true if this condition is applicable to the given project.
     *
     * @return
     *      true to allow user to configure this promotion condition for the given project.
     */
    public abstract boolean isApplicable(AbstractProject<?,?> item);
}
