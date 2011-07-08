package hudson.plugins.promoted_builds.conditions;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.promoted_builds.PromotedBuildAction;
import hudson.plugins.promoted_builds.PromotionBadge;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.PromotionConditionDescriptor;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.Status;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link PromotionCondition} that tests if 1 or more upstream promotions have
 * occurred.
 * 
 * @author Peter Hayes
 */
public class UpstreamPromotionCondition extends PromotionCondition {
    /**
     * List of upstream promotions that are used as the promotion criteria.
     */
    private final String requiredPromotionNames;

    public UpstreamPromotionCondition(String requiredPromotionNames) {
        this.requiredPromotionNames = requiredPromotionNames;
    }

    public String getRequiredPromotionNames() {
        return requiredPromotionNames;
    }

    public Set<String> getRequiredPromotionNamesAsSet() {
        if (requiredPromotionNames == null) {
            return Collections.emptySet();
        }

        return new HashSet<String>(Arrays.asList(requiredPromotionNames.split(",")));
    }
    
    @Override
    public PromotionBadge isMet(PromotionProcess promotionProcess, AbstractBuild<?,?> build) {
        Badge badge = new Badge();

        Set<String> requiredPromotions = getRequiredPromotionNamesAsSet();
        if (requiredPromotions.isEmpty()) {
            return badge;
        }

        PromotedBuildAction pba = build.getAction(PromotedBuildAction.class);

        if (pba == null) {
            return null;
        }

        for (Status status : pba.getPromotions()) {
            if (status.isPromotionSuccessful()) {
                requiredPromotions.remove(status.getName());
                badge.add(status.getName());

                // short circuit for loop if 
                if (requiredPromotions.isEmpty()) break;
            }
        }

        return requiredPromotions.isEmpty() ? badge : null;
    }

    public static final class Badge extends PromotionBadge {
        public final List<String> promotions = new ArrayList<String>(3);

        public void add(String promotion) {
            promotions.add(promotion);
        }
    }

    @Extension
    public static final class DescriptorImpl extends PromotionConditionDescriptor {
        public boolean isApplicable(AbstractProject<?,?> item) {
            return true;
        }

        public String getDisplayName() {
            return Messages.UpstreamPromotionCondition_DisplayName();
        }

        public PromotionCondition newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new UpstreamPromotionCondition(
                    formData.getString("promotions"));
        }
    }
}
