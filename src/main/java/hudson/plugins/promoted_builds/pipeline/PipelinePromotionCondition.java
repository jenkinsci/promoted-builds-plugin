package hudson.plugins.promoted_builds.pipeline;

import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.promoted_builds.PromotionBadge;
import hudson.plugins.promoted_builds.util.JenkinsHelper;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.List;

public abstract class PipelinePromotionCondition implements ExtensionPoint, Describable<PipelinePromotionCondition> {

    //for those promotions that don't satisfy the desired Promotion Conditions
    @CheckForNull
    public PromotionBadge isMet(Run<?,?> run, TaskListener listener){
        return null;
    }

    //The method is used by the Conditions to label those who satisfy the presented Promotion Conditions.
    public PromotionBadge isMet(PipelinePromotionProcess promotionProcess, Run<?,?> run, TaskListener listener){
        return isMet(run,listener);
    }

    //Loads the descriptor with the URL
    public PipelinePromotionConditionDescriptor getDescriptor() {
        return (PipelinePromotionConditionDescriptor) JenkinsHelper.getInstance().getDescriptor(getClass());
    }

    //Holds a set of Descriptors
    public static DescriptorExtensionList<PipelinePromotionCondition, PipelinePromotionConditionDescriptor> all() {
        return JenkinsHelper.getInstance().<PipelinePromotionCondition, PipelinePromotionConditionDescriptor>getDescriptorList(PipelinePromotionCondition.class);
    }

    /**
     * Returns a subset of {@link PipelinePromotionConditionDescriptor}s that applies to the given project.
     */
    //Required Promotion Conditions to be satisfied for Promotion is being defined here.
    public static List<PipelinePromotionConditionDescriptor> getApplicableTriggers(Job<?,?> p) {
        List<PipelinePromotionConditionDescriptor> r = new ArrayList<PipelinePromotionConditionDescriptor>();
        for (PipelinePromotionConditionDescriptor t : all()) {
            if(t.isApplicable(p))
                r.add(t);
        }
        return r;
    }

}