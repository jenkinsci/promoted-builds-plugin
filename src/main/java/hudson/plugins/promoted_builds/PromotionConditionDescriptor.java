package hudson.plugins.promoted_builds;

import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * {@link Descriptor} for {@link PromotionCondition}.
 *
 * @author Kohsuke Kawaguchi
 * @see PromotionConditions#CONDITIONS
 */
public abstract class PromotionConditionDescriptor extends Descriptor<PromotionCondition> {
    protected PromotionConditionDescriptor(Class<? extends PromotionCondition> clazz) {
        super(clazz);
    }

    /**
     * @deprecated
     *      This method is not used. Use {@link #newInstance(JSONObject)}  instead.
     */
    public final PromotionCondition newInstance(StaplerRequest req) throws FormException {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a configured instance from the submitted form.
     *
     * @param formData
     *      JSON object that contains the submitted form.
     *      See http://hudson.gotdns.com/wiki/display/HUDSON/Structured+Form+Submission
     */
    public abstract PromotionCondition newInstance(JSONObject formData) throws FormException;

    /**
     * Returns true if this condition is applicable to the given project.
     *
     * @return
     *      true to allow user to configure this promotion condition for the given project.
     */
    public abstract boolean isApplicable(AbstractProject<?,?> item);
}
