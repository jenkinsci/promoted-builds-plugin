package hudson.plugins.promoted_builds;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Item;
import hudson.model.Job;
import hudson.plugins.promoted_builds.util.JenkinsHelper;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.util.List;

/**
 * {@link Trigger} that starts a build when a promotion happens.
 *
 * @author Kohsuke Kawaguchi
 */
public class PromotionTrigger extends Trigger<AbstractProject> {
    private final String jobName;
    private final String process;

    @DataBoundConstructor
    public PromotionTrigger(String jobName, String process) {
        this.jobName = jobName;
        this.process = process;
    }

    public String getJobName() {
        return jobName;
    }

    public String getProcess() {
        return process;
    }

    public boolean appliesTo(PromotionProcess proc) {
        return proc.getName().equals(process) && proc.getParent().getOwner().getFullName().equals(jobName);
    }

    public void consider(Promotion p) {
        if (appliesTo(p.getParent()) && job != null) {
            job.scheduleBuild2(job.getQuietPeriod());
        }
    }

    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {
        @Override
        public boolean isApplicable(Item item) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Build when another project is promoted";
        }

        /**
         * Checks the job name.
         * @param project Current project
         * @param value Value to be validated
         * @return Validation result
         */
        public FormValidation doCheckJobName(@AncestorInPath Item project, @QueryParameter String value ) {
            if (!project.hasPermission(Item.CONFIGURE) && project.hasPermission(Item.EXTENDED_READ)) {
                return FormValidation.ok();
            }
            project.checkPermission(Item.CONFIGURE);

            if (StringUtils.isNotBlank(value)) {
                AbstractProject p = JenkinsHelper.getInstance().getItem(value,project,AbstractProject.class);
                if(p==null) {
                    AbstractProject nearest = AbstractProject.findNearest(value, project.getParent());
                    return FormValidation.error( nearest != null 
                            ? hudson.tasks.Messages.BuildTrigger_NoSuchProject(value, nearest.getRelativeNameFrom(project))
                            : Messages.Shared_noSuchProject(value));
                }
            }

            return FormValidation.ok();
        }

        public AutoCompletionCandidates doAutoCompleteJobName(@QueryParameter String value) {
            AutoCompletionCandidates candidates = new AutoCompletionCandidates();
            List<AbstractProject> jobs = JenkinsHelper.getInstance().getItems(AbstractProject.class);
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
         * @param defaultJob Base job
         * @param jobName Current name of the job specified in the form
         * @return List of possible project names. May be empty
         */
        public ListBoxModel doFillProcessItems(@AncestorInPath Item defaultJob, @QueryParameter("jobName") String jobName) {
            if (!defaultJob.hasPermission(Item.CONFIGURE) && defaultJob.hasPermission(Item.EXTENDED_READ)) {
                return new ListBoxModel();
            }
            defaultJob.checkPermission(Item.CONFIGURE);

            AbstractProject<?,?> j = null;
            if (jobName!=null)
                j = JenkinsHelper.getInstance().getItem(jobName,defaultJob,AbstractProject.class);

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
