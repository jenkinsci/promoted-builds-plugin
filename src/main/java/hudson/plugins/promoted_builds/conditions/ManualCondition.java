package hudson.plugins.promoted_builds.conditions;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.promoted_builds.Approval;
import hudson.plugins.promoted_builds.ManualPromotionBadge;
import hudson.plugins.promoted_builds.PromotedBuildAction;
import hudson.plugins.promoted_builds.PromotionBadge;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.PromotionConditionDescriptor;
import hudson.plugins.promoted_builds.PromotionProcess;
import java.util.List;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * {@link PromotionCondition} that requires manual promotion.
 *
 * @author Kohsuke Kawaguchi
 * @author Peter Hayes
 */
public class ManualCondition extends PromotionCondition {

    @Override
    public PromotionBadge isMet(PromotionProcess p, AbstractBuild<?,?> build) {
        PromotedBuildAction pba = build.getAction(PromotedBuildAction.class);

        if (pba == null)
            return null;

        Approval approval = pba.getApproval(p.getName());

        if (approval != null) {
            return new ManualPromotionBadge(approval);
        }

        return null;
    }

    @Extension
    public static final class DescriptorImpl extends PromotionConditionDescriptor {
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
    }
}

