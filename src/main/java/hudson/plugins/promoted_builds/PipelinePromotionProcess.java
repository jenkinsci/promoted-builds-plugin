package hudson.plugins.promoted_builds;

import hudson.model.Job;

import java.util.List;

public class PipelinePromotionProcess {
    public List<PipelinePromotionConditionDescriptor> getApplicableConditions(Job<?,?> p) {
        return p==null ? PipelinePromotionCondition.all() : PipelinePromotionCondition.getApplicableTriggers(p);
    }
}
