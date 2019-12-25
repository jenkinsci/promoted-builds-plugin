package hudson.plugins.promoted_builds;

import hudson.util.DescribableList;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Contains a generic executable promotion definition.
 * Provides equivalent of {@code PromotionProcess} API in the Promoted Builds plugin
 * @since 4.0
 */
public interface PromotionTask {

    /**
     * Gets all conditions associated with the task
     * @return List of conditions
     */
    @Nonnull
    DescribableList<PromotionCondition,PromotionConditionDescriptor> getConditions();

    /**
     * Get the promotion condition by referencing it fully qualified class name
     * @param promotionClassName Class name of the condition
     * @return Promotion condition if exists
     */
    @CheckForNull
    default PromotionCondition getPromotionCondition(String promotionClassName) {
        for (PromotionCondition condition : getConditions()) {
            if (condition.getClass().getName().equals(promotionClassName)) {
                return condition;
            }
        }

        return null;
    }
}
