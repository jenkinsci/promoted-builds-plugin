/*
 * The MIT License
 * 
 * Copyright (c) 2013 IKEDA Yasuyuki
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.promoted_builds.conditions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;

import jenkins.model.Jenkins;
import hudson.Extension;
import hudson.Util;
import hudson.matrix.MatrixConfiguration;
import hudson.matrix.MatrixRun;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Fingerprint;
import hudson.model.InvisibleAction;
import hudson.model.ItemGroup;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Cause.UpstreamCause;
import hudson.model.CauseAction;
import hudson.model.Descriptor;
import hudson.model.Queue.QueueDecisionHandler;
import hudson.model.Queue.Task;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotionBadge;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.PromotionConditionDescriptor;
import hudson.plugins.promoted_builds.PromotionProcess;

/**
 * Promote when all triggered projects succeeded.
 * 
 * This is useful when there are projects that is triggered conditionally.
 */
public class TriggeredPassCondition extends PromotionCondition {
    private String excludeProjectNames;
    /**
     * Downstream projects to exclude from conditions.
     * 
     * @return the excludeProjectNames
     */
    public String getExcludeProjectNames() {
        return excludeProjectNames;
    }
    
    /**
     * @param itemGroup
     * @return excluded projects
     */
    private List<AbstractProject<?, ?>> getExcludeProjectList(ItemGroup<?> itemGroup) {
        if (StringUtils.isBlank(getExcludeProjectNames())) {
            return Collections.emptyList();
        }
        List<AbstractProject<?, ?>> excludeProjectList = new ArrayList<AbstractProject<?, ?>>();
        for (String excludeProjectName: StringUtils.split(getExcludeProjectNames(), ',')) {
            if (StringUtils.isBlank(excludeProjectName)) {
                continue;
            }
            
            excludeProjectName = StringUtils.trim(excludeProjectName);
            AbstractProject<?,?> p = (AbstractProject<?, ?>)itemGroup.getItem(excludeProjectName);
            if (p == null) {
                continue;
            }
            excludeProjectList.add(p);
        }
        return excludeProjectList;
    }
    
    private boolean evenIfUnstable;
    /**
     * Promotes even when the triggered projects are finished with UNSTABLE.
     * 
     * @return the evenIfUnstable
     */
    public boolean isEvenIfUnstable() {
        return evenIfUnstable;
    }
    
    private boolean onlyDirectTriggered;
    /**
     * Promotes only when the exact triggered builds succeeded.
     * 
     * If false, promotes even when builds of triggered projects
     * using artifacts of the triggering build succeed.
     * 
     * @return the onlyDirectTriggered
     */
    public boolean isOnlyDirectTriggered() {
        return onlyDirectTriggered;
    }
    
    /**
     * @param excludeProjectNames
     * @param evenIfUnstable
     * @param onlyDirectTriggered
     */
    @DataBoundConstructor
    public TriggeredPassCondition(String excludeProjectNames, boolean evenIfUnstable, boolean onlyDirectTriggered) {
        this.excludeProjectNames = excludeProjectNames;
        this.evenIfUnstable = evenIfUnstable;
        this.onlyDirectTriggered = onlyDirectTriggered;
    }
    
    /**
     * Retrieve triggered projects. For using from jelly files.
     * 
     * @return triggered projects
     */
    public List<AbstractProject<?,?>> getTriggeredProjectList() {
        return getTriggeredProjectList(Stapler.getCurrentRequest().findAncestorObject(AbstractBuild.class));
    }
    
    /**
     * Retrieve triggered projects.
     * 
     * @param build owner of the promotion containing this condition.
     * @return triggered projects
     */
    public List<AbstractProject<?,?>> getTriggeredProjectList(AbstractBuild<?,?> build) {
        TriggerRecordAction action = build.getAction(TriggerRecordAction.class);
        if (action == null) {
            return Collections.emptyList();
        }
        
        List<AbstractProject<?,?>> triggeredProjectList = new ArrayList<AbstractProject<?,?>>();
        List<AbstractProject<?,?>> excludeProjectList = getExcludeProjectList(build.getProject().getParent());
        
        for (String triggeredProjectName: action.getTriggeredProjectList()) {
            AbstractProject<?,?> p = Jenkins.getInstance().getItemByFullName(triggeredProjectName, AbstractProject.class);
            if (p == null) {
                // the project may be removed.
                continue;
            }
            if (excludeProjectList.contains(p)) {
                continue;
            }
            
            triggeredProjectList.add(p);
        }
        
        return triggeredProjectList;
    }
    
    /**
     * Checks if all triggered projects succeeded.
     * 
     * @param promotionProcess
     * @param triggeringBuild
     * @return badge. null if not the condition is met.
     * @see hudson.plugins.promoted_builds.PromotionCondition#isMet(hudson.plugins.promoted_builds.PromotionProcess, hudson.model.AbstractBuild)
     */
    @Override
    public PromotionBadge isMet(PromotionProcess promotionProcess, AbstractBuild<?, ?> triggeringBuild) {
        TriggerRecordAction action = triggeringBuild.getAction(TriggerRecordAction.class);
        if (action == null) {
            // no triggering project is recorded.
            return null;
        }
        
        Badge badge = new Badge();
        
        OUTER:
        for (AbstractProject<?,?> p: getTriggeredProjectList(triggeringBuild)) {
            for (AbstractBuild<?,?> triggeredBuild : triggeringBuild.getDownstreamBuilds(p)) {
                // test builds using artifacts of the triggering build.
                if (isTriggeredPass(triggeringBuild, triggeredBuild)) {
                    badge.add(triggeredBuild);
                    continue OUTER;
                }
            }
            
            return null;
        }
        return (badge.getBuilds().size() > 0)?badge:null;
    }
    
    private boolean isTriggeredPass(AbstractBuild<?, ?> triggeringBuild, AbstractBuild<?, ?> triggeredBuild) {
        if (isOnlyDirectTriggered()) {
            UpstreamCause cause = triggeredBuild.getCause(UpstreamCause.class);
            if (
                    cause == null
                    || !triggeringBuild.getProject().getFullName().equals(cause.getUpstreamProject())
                    || triggeredBuild.getNumber() != cause.getUpstreamBuild()
            ) {
                // This build is not exactly the triggered one.
                return false;
            }
        }
        Result r = triggeredBuild.getResult();
        return (!isEvenIfUnstable() && Result.SUCCESS.isWorseOrEqualTo(r))
            || (isEvenIfUnstable() && Result.UNSTABLE.isWorseOrEqualTo(r));
    }
    
    
    /**
     * {@link Descriptor} for {@link TriggeredPassCondition}
     */
    @Extension
    public static final class DescriptorImpl extends PromotionConditionDescriptor {
        /**
         * @param item
         * @return
         * @see hudson.plugins.promoted_builds.PromotionConditionDescriptor#isApplicable(hudson.model.AbstractProject)
         */
        @Override
        public boolean isApplicable(AbstractProject<?, ?> item) {
            return true;
        }
        
        /**
         * @return
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName() {
            return Messages.TriggeredPassCondition_DisplayName();
        }
        
        public AutoCompletionCandidates doAutoCompleteExcludeProjectNames(
                @QueryParameter String value,
                @AncestorInPath AbstractProject<?,?> project
        ) {
            @SuppressWarnings("rawtypes")
            List<AbstractProject> downstreams = project.getDownstreamProjects();
            
            AutoCompletionCandidates candidates = new AutoCompletionCandidates();
            
            for (AbstractProject<?,?> downstream: downstreams) {
                String name = downstream.getRelativeNameFrom(project);
                if(name.startsWith(value)) {
                    candidates.add(name);
                }
            }
            return candidates;
        }
    }
    
    /**
     * Badge for {@link TriggeredPassCondition}
     */
    public static class Badge extends PromotionBadge {
        private List<Fingerprint.BuildPtr> builds = new ArrayList<Fingerprint.BuildPtr>();
        
        /**
         * Builds satisfied the condition.
         * 
         * @return the builds
         */
        public List<Fingerprint.BuildPtr> getBuilds() {
            return builds;
        }
        
        /**
         * Add a build.
         * 
         * @param b
         */
        public void add(AbstractBuild<?,?> b) {
           builds.add(new Fingerprint.BuildPtr(b));
        }
    }
    
    /**
     * Action to recored triggered projects.
     */
    public static class TriggerRecordAction extends InvisibleAction {
        private List<String> triggeredProjectList = new ArrayList<String>();
        /**
         * The list of the name of triggered projects.
         * 
         * @return the triggeredProjectList
         */
        public List<String> getTriggeredProjectList() {
            return triggeredProjectList;
        }
        
        /**
         * Add a triggered project.
         * 
         * @param triggeredProject
         */
        public void addTriggeredProject(AbstractProject<?, ?> triggeredProject) {
            triggeredProjectList.add(triggeredProject.getFullName());
        }
    }
    
    /**
     * DecisionHandler to watch triggering builds.
     * 
     * Never decides whether tasks are scheduled. Used only for listening purpose.
     */
    @Extension(ordinal=-Double.MAX_VALUE) // This must be evaluated at the last.
    public static class TriggerRecordingQueueDecisionHandler extends QueueDecisionHandler {
        @Override
        public boolean shouldSchedule(Task task, List<Action> actions) {
            onQueued(task, actions);
            return true;
        }
        
        private void onQueued(Task task, List<Action> actions) {
            if (!(task instanceof AbstractProject)) {
                return;
            }
            
            AbstractProject<?,?> p = (AbstractProject<?,?>)task;
            if (p instanceof MatrixConfiguration) {
                // MatrixConfiguration is always triggered by MatrixProject.
                return;
            }
            for (CauseAction action: Util.filter(actions, CauseAction.class)) {
                for (UpstreamCause cause: Util.filter(action.getCauses(), UpstreamCause.class)) {
                    AbstractProject<?,?> upstreamProject = Jenkins.getInstance().getItemByFullName(cause.getUpstreamProject(), AbstractProject.class);
                    if (upstreamProject == null) {
                        // the upstream project may be removed.
                        return;
                    }
                    AbstractBuild<?,?> upstreamBuild = upstreamProject.getBuildByNumber(cause.getUpstreamBuild());
                    if (hasTriggeredPassCondition(upstreamProject)) {
                        addTriggered(upstreamBuild, p);
                    }
                }
            }
        }
        
        private synchronized void addTriggered(AbstractBuild<?, ?> upstreamBuild, AbstractProject<?, ?> triggeredProject) {
            TriggerRecordAction action = upstreamBuild.getAction(TriggerRecordAction.class);
            if (action == null) {
                action = new TriggerRecordAction();
                upstreamBuild.addAction(action);
            }
            action.addTriggeredProject(triggeredProject);
        }
        
        private boolean hasTriggeredPassCondition(AbstractProject<?, ?> project) {
            JobPropertyImpl prop = project.getProperty(JobPropertyImpl.class);
            if (prop == null) {
                return false;
            }
            
            for (PromotionProcess pp : prop.getItems()) {
                for (PromotionCondition cond : pp.conditions) {
                    if (cond instanceof TriggeredPassCondition) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
    
    /**
     * Listen builds completed to trigger consideration whether {@link TriggeredPassCondition} is met.
     */
    @Extension
    public static class TriggeredPassListener extends RunListener<AbstractBuild<?,?>> {
        /**
         * A build is completed. Trigger the check of {@link PromotionProcess} if the project is condigured with {@link TriggeredPassCondition}.
         * 
         * @param build
         * @param listener
         * @see hudson.model.listeners.RunListener#onCompleted(hudson.model.Run, hudson.model.TaskListener)
         */
        @Override
        public void onCompleted(AbstractBuild<?, ?> build, TaskListener listener) {
            if (build instanceof MatrixRun) {
                // Should evaluated only when MatrixBuild is completed.
                return;
            }
            for (@SuppressWarnings("rawtypes") Entry<AbstractProject, Integer> upstream: build.getUpstreamBuilds().entrySet()) {
                AbstractProject<?,?> project = upstream.getKey();
                AbstractBuild<?,?> upstreamBuild = project.getBuildByNumber(upstream.getValue());
                JobPropertyImpl prop = project.getProperty(JobPropertyImpl.class);
                if (prop == null) {
                    continue;
                }
                
                for (PromotionProcess pp : prop.getItems()) {
                    if (hasTriggeredPassCondition(pp)) {
                        try {
                            pp.considerPromotion2(upstreamBuild);
                        } catch(IOException e) {
                            e.printStackTrace(listener.getLogger());
                        }
                    }
                }
            }
        }
        
        private boolean hasTriggeredPassCondition(PromotionProcess pp) {
            for (PromotionCondition cond : pp.conditions) {
                if (cond instanceof TriggeredPassCondition) {
                    return true;
                }
            }
            return false;
        }
    }
}
