/*
 * The MIT License
 *
 * Copyright (c) 2103, Peter Hayes
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
package hudson.plugins.promoted_builds.parameters;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.Run;
import hudson.model.SimpleParameterDefinition;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotedBuildAction;
import hudson.plugins.promoted_builds.PromotedProjectAction;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.util.ItemPathResolver;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import hudson.util.RunList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

/**
 * Defines a parameter that allows the user to select a promoted build
 * from a drop down list.
 * <p>
 * Remarks on addressing: 
 * Starting from TODO, the field also supports folders and the relative addressing (JENKINS-25011). 
 * See {@link ItemPathResolver#getByPath(java.lang.String, hudson.model.Item, java.lang.Class)} 
 * for the documentation.
 * @author Pete Hayes
 */
public class PromotedBuildParameterDefinition extends SimpleParameterDefinition {
    
    /**
     * Absolute path to the item starting from the root element.
     * See format clarification in the {@link PromotedBuildParameterDefinition}.
     */
    private final String projectName;
    private final String promotionProcessName;

    @DataBoundConstructor
    public PromotedBuildParameterDefinition(String name, String jobName, String process, String description) {
        super(name, description);
        this.projectName = jobName;
        this.promotionProcessName = process;
    }

    @Override
    public PromotedBuildParameterValue createValue(StaplerRequest req, JSONObject jo) {
        PromotedBuildParameterValue value = req.bindJSON(PromotedBuildParameterValue.class, jo);
        value.setPromotionProcessName(promotionProcessName);
        value.setDescription(getDescription());
        return value;
    }

    public PromotedBuildParameterValue createValue(String value) {
        PromotedBuildParameterValue p = new PromotedBuildParameterValue(getName(), value, getDescription());
        p.setPromotionProcessName(promotionProcessName);
        return p;
    }

    @Override
    public PromotedBuildParameterValue getDefaultParameterValue() {
        final List builds = getBuilds();

        if (builds.isEmpty()) {
            return null;
        }

        return createValue(((Run) builds.get(0)).getExternalizableId());
    }

    @Override
    public ParameterDefinition copyWithDefaultValue(ParameterValue defaultValue) {
        if (defaultValue instanceof PromotedBuildParameterValue) {
            PromotedBuildParameterValue value = (PromotedBuildParameterValue) defaultValue;
            return new PromotedBuildParameterDefinition(getName(), value.getRunId(), value.getPromotionProcessName(), getDescription());
        } else {
            return this;
        }
    }

    /**
     * Absolute path to the item starting from the root element.
     */
    @Exported
    public String getJobName() {
        return projectName;
    }

    @Exported
    public String getProcess() {
        return promotionProcessName;
    }

    /**
     * Gets a list of promoted builds for the project.
     * @return List of {@link AbstractBuild}s, which have been promoted
     * @deprecated This method retrieves the base item for relative addressing from 
     * the {@link StaplerRequest}. The relative addressing may be malfunctional if
     * you use this method outside {@link StaplerRequest}s. 
     * Use {@link #getBuilds(hudson.model.Item)} instead
     */
    @Nonnull
    @Deprecated
    public List getBuilds() {
        // Try to get ancestor from the object, otherwise pass null and disable the relative addressing
        final StaplerRequest currentRequest = Stapler.getCurrentRequest();
        final Item item = currentRequest != null ? currentRequest.findAncestorObject(Item.class) : null;
        return getRuns(item);
    }
    
    /**
     * Gets a list of promoted builds for the project.
     * @param base Base item for the relative addressing
     * @return List of {@link AbstractBuild}s, which have been promoted.
     *         May return an empty list if {@link Jenkins} instance is not ready
     * @since 2.22
     */
    @Nonnull
    public List<Run<?,?>> getRuns(@CheckForNull Item base) {
        final List<Run<?,?>> runs = new ArrayList<Run<?,?>>();
        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            return runs;
        }

        // JENKINS-25011: also look for jobs in folders.
        final AbstractProject<?,?> job = ItemPathResolver.getByPath(projectName, base, AbstractProject.class);
        if (job == null) {
            return runs;
        }

        PromotedProjectAction promotedProjectAction  = job.getAction(PromotedProjectAction.class);
        if (promotedProjectAction == null) {
            return runs;
        }

        for (Run<?,?> run : job.getBuilds()) {
            List<PromotedBuildAction> actions = run.getActions(PromotedBuildAction.class);
            for (PromotedBuildAction buildAction : actions) {
                if (buildAction.contains(promotionProcessName)) {
                    runs.add(run);
                    break;
                }
            }
        }
        
        return runs;
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {

        @Override
        public String getDisplayName() {
            return "Promoted Build Parameter";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/promoted-builds/parameter/promotion.html";
        }

        @Override
        public ParameterDefinition newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(PromotedBuildParameterDefinition.class, formData);
        }
        
        /**
         * Checks the job name.
         */
        public FormValidation doCheckJobName(@AncestorInPath Item project, @QueryParameter String value ) {
            final Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null) {
                return FormValidation.error("Jenkins instance is not ready");
            }
            if (!project.hasPermission(Item.CONFIGURE) && project.hasPermission(Item.EXTENDED_READ)) {
                return FormValidation.ok();
            }
            
            project.checkPermission(Item.CONFIGURE);

            if (StringUtils.isNotBlank(value)) {
                // JENKINS-25011: also look for jobs in folders.
                final AbstractProject p = ItemPathResolver.getByPath(value, project, AbstractProject.class);
                if (p==null) {
                    // suggest full name so that getBuilds() can find item.
                    AbstractProject nearest = AbstractProject.findNearest(value, project.getParent());
                    return FormValidation.error( nearest != null
                            ? hudson.tasks.Messages.BuildTrigger_NoSuchProject(value, nearest.getFullName())
                            : hudson.plugins.promoted_builds.Messages.Shared_noSuchProject(value));
                }

            }

            return FormValidation.ok();
        }

        @Restricted(NoExternalUse.class)
        public AutoCompletionCandidates doAutoCompleteJobName(@AncestorInPath @CheckForNull Item project, 
                @QueryParameter String value) {
            final AutoCompletionCandidates candidates = new AutoCompletionCandidates();
            final Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null || project == null || !project.hasPermission(Item.CONFIGURE)) {
                return candidates;
            }
          
            // JENKINS-25011: look for jobs in all folders.
            //TODO: remove prefixes
            for (AbstractProject job: jenkins.getAllItems(AbstractProject.class)) {
                if (job.getFullName().contains(value)) {
                    if (job.hasPermission(Item.READ)) {
                        // suggest full name so that getBuilds() can find item.
                        candidates.add(job.getFullName());
                    }
                }
            }
            return candidates;
        }

        /**
         * Fills in the available promotion processes.
         */
        public ListBoxModel doFillProcessItems(@AncestorInPath Job defaultJob, @QueryParameter("jobName") String jobName) {
            final ListBoxModel r = new ListBoxModel();
            final Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null) {
                return r;
            }
            if (!defaultJob.hasPermission(Item.CONFIGURE) && defaultJob.hasPermission(Item.EXTENDED_READ)) {
                return r;
            }
            defaultJob.checkPermission(Item.CONFIGURE);

            AbstractProject<?,?> j = null;
            if (jobName != null) {
                j = ItemPathResolver.getByPath(jobName, defaultJob, AbstractProject.class);
            }
           
            if (j!=null) {
                JobPropertyImpl pp = j.getProperty(JobPropertyImpl.class);
                if (pp!=null) {
                    for (PromotionProcess proc : pp.getActiveItems()) {
                        // Note: why not list all items instead of active ones?
                        // this would allow to configure the job even
                        // if a promotion hasn't happened (yet).
                        r.add(new Option(proc.getDisplayName(),proc.getName()));
                    }
                }
            }
            return r;
        }
    }    
}
