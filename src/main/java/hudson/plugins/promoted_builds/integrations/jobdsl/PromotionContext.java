package hudson.plugins.promoted_builds.integrations.jobdsl;

import groovy.lang.Closure;
import groovy.util.Node;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import hudson.plugins.promoted_builds.conditions.ParameterizedSelfPromotionCondition;
import hudson.plugins.promoted_builds.conditions.SelfPromotionCondition;
import hudson.plugins.promoted_builds.conditions.UpstreamPromotionCondition;

import java.util.ArrayList;
import java.util.List;

import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.dsl.helpers.step.StepContext;

import javaposse.jobdsl.plugin.DslEnvironment;

import static javaposse.jobdsl.plugin.ContextExtensionPoint.executeInContext;

class PromotionContext implements Context {
    private final DslEnvironment dslEnvironment;

    private List<PromotionCondition> conditions = new ArrayList<PromotionCondition>();
    
    private List<Node> actions = new ArrayList<Node>();

    private String icon;

    private String restrict;

    private String name;

    public void name(String name) {
        this.name = name;
    }

    public void icon(String icon) {
        this.icon = icon;
    }

    public void restrict(String restrict) {
        this.restrict = restrict;
    }

    public PromotionContext(DslEnvironment dslEnvironment) {
        this.dslEnvironment = dslEnvironment;
    }

    public void conditions(Closure<?> conditionClosure) {
        // delegate to ConditionsContext
        ConditionsContext conditionContext = new ConditionsContext(dslEnvironment);
        executeInContext(conditionClosure, conditionContext);
        if (conditionContext.isSelfPromotion()) {
            conditions.add(new SelfPromotionCondition(conditionContext.isEvenIfUnstable()));
        }
        if (conditionContext.isParameterizedSelfPromotion()) {
            conditions.add(new ParameterizedSelfPromotionCondition(conditionContext.isEvenIfUnstableParameterized(), conditionContext
                    .getParameterName(), conditionContext.getParameterValue()));
        }
        if (conditionContext.isManual()) {
            JobDslManualCondition condition = new JobDslManualCondition();
            condition.setUsers(conditionContext.getUsers());
            if (conditionContext.getParams() != null) {
                condition.setParameterDefinitionNodes(conditionContext.getParams().values());
            }
            conditions.add(condition);
        }
        if (conditionContext.isReleaseBuild()) {
            conditions.add(new ReleasePromotionCondition());
        }
        if (conditionContext.isDownstreamPass()) {
            conditions.add(new DownstreamPassCondition(conditionContext.getJobs(), conditionContext.isEvenIfUnstableDownstream()));
        }
        if (conditionContext.isUpstreamPromotion()) {
            conditions.add(new UpstreamPromotionCondition(conditionContext.getPromotionNames()));
        }
    }

    public void actions(Closure<?> actionsClosure) {
        // delegate to StepContext
        StepContext stepContext = dslEnvironment.createContext(StepContext.class);
        executeInContext(actionsClosure, stepContext);
        actions.addAll(stepContext.getStepNodes());
    }

    public List<PromotionCondition> getConditions() {
        return conditions;
    }

    public List<Node> getActions() {
        return actions;
    }

    public String getIcon() {
        return icon;
    }

    public String getRestrict() {
        return restrict;
    }

    public String getName() {
        return name;
    }

    
}
