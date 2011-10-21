/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package hudson.plugins.promoted_builds.conditions;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.plugins.promoted_builds.PromotionBadge;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.PromotionConditionDescriptor;
import hudson.plugins.promoted_builds.PromotionProcess;
import java.util.Map;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * {@link PromotionCondition} that promotes a build as soon as it's done if a
 * given parameter has the specified value.
 *
 * @author Grant Limberg <glimberg@gmail.com>
 */
public class ParameterizedSelfPromotionCondition extends SelfPromotionCondition {
    private final String parameterName;
    private final String parameterValue;

    @DataBoundConstructor
    public ParameterizedSelfPromotionCondition(boolean evenIfUnstable, String parameterName, String parameterValue) {
        super(evenIfUnstable);
        this.parameterName = parameterName;
        this.parameterValue = parameterValue;
    }

    public String getParameterName()
    {
        return parameterName;
    }

    public String getParameterValue()
    {
        return parameterValue;
    }

    @Override
    public PromotionBadge isMet(PromotionProcess promotionProcess, AbstractBuild<?, ?> build) {
        if(super.isMet(promotionProcess, build) != null) {

            Result r = build.getResult();
            Map<String, String> vars = build.getBuildVariables();
            if(vars.containsKey(parameterName) &&
               ((String)vars.get(parameterName)).equals(parameterValue)) {
                System.out.println("Matched parameters!");
                return new ParameterizedSelfPromotionBadge();
            }
        }
        return null;
    }


    @Extension
    public static final class DescriptorImpl extends PromotionConditionDescriptor {
        public boolean isApplicable(AbstractProject<?,?> item) {
            return true;
        }

        public String getDisplayName() {
            return Messages.ParameterizedSelfPromotionCondition_DisplayName();
        }
    }
}
