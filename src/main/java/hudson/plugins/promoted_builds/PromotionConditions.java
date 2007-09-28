package hudson.plugins.promoted_builds;

import hudson.model.AbstractProject;
import hudson.model.Descriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * All registered {@link PromotionConditionDescriptor}s in the system.
 *
 * @author Kohsuke Kawaguchi
 */
public class PromotionConditions {
    public static final List<PromotionConditionDescriptor> CONDITIONS = Descriptor.toList();

    /**
     * Returns a subset of {@link PromotionConditionDescriptor}s that applys to the given project.
     */
    public static List<PromotionConditionDescriptor> getApplicableTriggers(AbstractProject<?,?> p) {
        List<PromotionConditionDescriptor> r = new ArrayList<PromotionConditionDescriptor>();
        for (PromotionConditionDescriptor t : CONDITIONS) {
            if(t.isApplicable(p))
                r.add(t);
        }
        return r;
    }
}
