package hudson.plugins.promoted_builds.conditions;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.promoted_builds.PromotionBadge;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.PromotionConditionDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * {@link PromotionCondition} that requires manual promotion.
 *
 * @author Kohsuke Kawaguchi
 */
public class ManualCondition extends PromotionCondition {

    @Override
    public PromotionBadge isMet(AbstractBuild<?,?> build) {
        return null;
    }

    public PromotionConditionDescriptor getDescriptor() {
        return DescriptorImpl.INSTANCE;
    }

    public static final class DescriptorImpl extends PromotionConditionDescriptor {
        public DescriptorImpl() {
            super(ManualCondition.class);
        }

        public boolean isApplicable(AbstractProject<?,?> item) {
            return true;
        }

        public String getDisplayName() {
            return "Only when manually approved";
        }

        public ManualCondition newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new ManualCondition();
        }

        public String getHelpFile() {
            return "/plugin/promoted-builds/conditions/manual.html";
        }

        public static final DescriptorImpl INSTANCE = new DescriptorImpl();
    }
}

