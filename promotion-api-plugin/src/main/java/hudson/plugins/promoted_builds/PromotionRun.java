package hudson.plugins.promoted_builds;

// TODO: implementation for Pipeline
// TODO: add generics?

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractBuild;
import hudson.model.Items;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Run;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.PermissionScope;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @see Promotion
 */
public interface PromotionRun { // always a run?

    // Run which we try to promote
    @Nonnull
    Run<?,?> getPromotedRun();

    // Execution which does the promotion
    @Nonnull
    Run<?,?> getPromotionRun();

    //TODO: Move implementation to a default method?
    List<ParameterValue> getParameterValues();


    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void addAliases() {
        // The class was promoted to upstream interface, but we still need to convert the data correctly
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.promoted_builds.Promotion.PromotionParametersAction", PromotionParametersAction.class);
    }

    /**
     * Action, which stores promotion parameters.
     * This class allows defining custom parameters filtering logic, which is
     * important for versions after the SECURITY-170 fix.
     * @since TODO
     */
    @Restricted(NoExternalUse.class)
    public static class PromotionParametersAction extends ParametersAction {

        private List<ParameterValue> unfilteredParameters;

        private PromotionParametersAction(List<ParameterValue> params) {
            // Pass the parameters upstairs
            super(params);
            unfilteredParameters = params;
        }

        @Override
        public List<ParameterValue> getParameters() {
            return Collections.unmodifiableList(filter(unfilteredParameters));
        }

        private List<ParameterValue> filter(List<ParameterValue> params) {
            // buildToBePromoted::getParameters() invokes the secured method, hence all
            // parameters from the promoted build are safe.
            return params;
        }

        public static PromotionParametersAction buildFor(
                @Nonnull AbstractBuild<?, ?> buildToBePromoted,
                @CheckForNull List<ParameterValue> promotionParams) {
            if (promotionParams == null) {
                promotionParams = new ArrayList<ParameterValue>();
            }

            List<ParameterValue> params = new ArrayList<ParameterValue>();

            //Add the target build parameters first, if the same parameter is not being provided by the promotion build
            List<ParametersAction> parameters = buildToBePromoted.getActions(ParametersAction.class);
            for (ParametersAction paramAction : parameters) {
                for (ParameterValue pvalue : paramAction.getParameters()) {
                    if (!promotionParams.contains(pvalue)) {
                        params.add(pvalue);
                    }
                }
            }

            //Add all the promotion build parameters
            params.addAll(promotionParams);

            // Create list of actions to pass to scheduled build
            return new PromotionParametersAction(params);
        }
    }
}
