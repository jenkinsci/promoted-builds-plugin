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
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

/**
 * Defines a parameter that allows the user to select a promoted build
 * from a drop down list.
 *
 * @author Pete Hayes
 */
public class PromotedBuildParameterDefinition extends SimpleParameterDefinition {
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
        List builds = getBuilds();

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

    @Exported
    public String getJobName() {
        return projectName;
    }

    @Exported
    public String getProcess() {
        return promotionProcessName;
    }

    public List getBuilds() {
        List builds = new ArrayList();

        AbstractProject job = (AbstractProject) Jenkins.getInstance().getItem(projectName);

        PromotedProjectAction promotedProjectAction  = job.getAction(PromotedProjectAction.class);
        if (promotedProjectAction == null) {
            return builds;
        }

        for (Iterator iter = job.getBuilds().iterator(); iter.hasNext(); )  {
            Run run = (Run) iter.next();

            List buildActions = run.getActions(PromotedBuildAction.class);
            for (int i = 0; i < buildActions.size(); i++) {
                PromotedBuildAction buildAction = (PromotedBuildAction) buildActions.get(i);

                if (buildAction.contains(promotionProcessName)) {
                    builds.add(run);
                    break;
                }
            }
        }

        return builds;
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
            project.checkPermission(Item.CONFIGURE);

            if (StringUtils.isNotBlank(value)) {
                AbstractProject p = Jenkins.getInstance().getItem(value,project,AbstractProject.class);
                if(p==null)
                    return FormValidation.error(hudson.tasks.Messages.BuildTrigger_NoSuchProject(value,
                            AbstractProject.findNearest(value, project.getParent()).getRelativeNameFrom(project)));

            }

            return FormValidation.ok();
        }

        public AutoCompletionCandidates doAutoCompleteJobName(@QueryParameter String value) {
            AutoCompletionCandidates candidates = new AutoCompletionCandidates();
            List<AbstractProject> jobs = Jenkins.getInstance().getItems(AbstractProject.class);
            for (AbstractProject job: jobs) {
                if (job.getFullName().startsWith(value)) {
                    if (job.hasPermission(Item.READ)) {
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
            defaultJob.checkPermission(Item.CONFIGURE);

            AbstractProject<?,?> j = null;
            if (jobName!=null)
                j = Jenkins.getInstance().getItem(jobName,defaultJob,AbstractProject.class);

            ListBoxModel r = new ListBoxModel();
            if (j!=null) {
                JobPropertyImpl pp = j.getProperty(JobPropertyImpl.class);
                if (pp!=null) {
                    for (PromotionProcess proc : pp.getActiveItems())
                        r.add(new Option(proc.getDisplayName(),proc.getName()));
                }
            }
            return r;
        }
    }

}
