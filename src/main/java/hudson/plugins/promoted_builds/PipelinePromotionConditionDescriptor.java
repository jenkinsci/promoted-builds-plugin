package hudson.plugins.promoted_builds;

import hudson.model.Descriptor;
import hudson.model.Job;

public abstract class PipelinePromotionConditionDescriptor extends Descriptor<PipelinePromotionCondition> {

    protected PipelinePromotionConditionDescriptor(Class<? extends PipelinePromotionCondition> clazz) {
        super(clazz);
    }

    protected PipelinePromotionConditionDescriptor() {
        super();
    }

    /**
     * Returns true if this condition is applicable to the given project.
     *
     * @return true to allow user to configure this promotion condition for the given project.
     */
    public abstract boolean isApplicable(Job<?, ?> item);
}