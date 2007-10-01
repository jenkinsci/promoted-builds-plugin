package hudson.plugins.promoted_builds;

import hudson.model.Descriptor.FormException;
import hudson.model.AbstractBuild;
import hudson.util.DescribableList;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

/**
 * Criteria for a build to be promoted.
 *
 * @author Kohsuke Kawaguchi
 */
public final class PromotionCriterion implements DescribableList.Owner {
    private final String name;

    /**
     * {@link PromotionCondition}s.
     */
    private final DescribableList<PromotionCondition,PromotionConditionDescriptor> conditions =
            new DescribableList<PromotionCondition, PromotionConditionDescriptor>(this);

    /*package*/ PromotionCriterion(StaplerRequest req, JSONObject c) throws FormException {
        this.name = c.getString("name");
        conditions.rebuild(req,c,PromotionConditions.CONDITIONS,"condition");
    }

    /**
     * Checks if all the conditions to promote a build is met.
     */
    public boolean isMet(AbstractBuild<?,?> build) {
        for (PromotionCondition cond : conditions)
            if(!cond.isMet(build))
                return false;
        return true;
    }


    /**
     * Gets the human readable name set by the user. 
     */
    public String getName() {
        return name;
    }

    /**
     * @deprecated
     *      Save is not supported on this level.
     */
    public void save() throws IOException {
        // TODO?
    }

    /**
     * {@link PromotionCondition}s that constitute this criteria.
     */
    public DescribableList<PromotionCondition, PromotionConditionDescriptor> getConditions() {
        return conditions;
    }
}
