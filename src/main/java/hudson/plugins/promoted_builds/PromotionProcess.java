package hudson.plugins.promoted_builds;


import antlr.ANTLRException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.BulkChange;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.ParameterValue;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Cause;
import hudson.model.Cause.UserCause;
import hudson.model.DependencyGraph;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Failure;
import hudson.model.FreeStyleProject;
import hudson.model.ItemGroup;
import hudson.model.JDK;
import hudson.model.Job;
import hudson.model.Label;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PermalinkProjectAction.Permalink;
import hudson.model.Queue.Item;
import hudson.model.Run;
import hudson.model.Saveable;
import hudson.model.StringParameterValue;
import hudson.model.labels.LabelAtom;
import hudson.model.labels.LabelExpression;
import hudson.plugins.promoted_builds.conditions.ManualCondition.ManualApproval;
import hudson.plugins.promoted_builds.util.JenkinsHelper;
import hudson.security.ACL;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrappers;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.util.TimeDuration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

public abstract class PromotionProcess extends Job<PromotionProcess,Promotion> implements Saveable, Describable<PromotionProcess>, BuildableItemWithBuildWrappers {
    public final DescribableList<PromotionCondition,PromotionConditionDescriptor> conditions =
            new DescribableList<PromotionCondition, PromotionConditionDescriptor>(this);


    @CheckForNull
    public PromotionCondition getPromotionCondition(String promotionClassName) {
        for (PromotionCondition condition : conditions) {
            if (condition.getClass().getName().equals(promotionClassName)) {
                return condition;
            }
        }

        return null;
    }



    @Nonnull
    public List<PromotionBadge> getMetQualifications(AbstractBuild<?,?> build) {
        List<PromotionBadge> badges = new ArrayList<PromotionBadge>();
        for (PromotionCondition cond : conditions) {
            PromotionBadge b = cond.isMet(this, build);

            if (b != null)
                badges.add(b);
        }
        return badges;
    }


    /**
     * Checks if all the conditions to promote a build is met.
     *
     * @param build Build to be checked
     * @return
     *      {@code null} if promotion conditions are not met.
     *      otherwise returns a list of badges that record how the promotion happened.
     */
    @CheckForNull
    public Status isMet(AbstractBuild<?,?> build) {
        List<PromotionBadge> badges = new ArrayList<PromotionBadge>();
        for (PromotionCondition cond : conditions) {
            PromotionBadge b = cond.isMet(this, build);
            if(b==null)
                return null;
            badges.add(b);
        }
        return new Status(this,badges);
    }

}
