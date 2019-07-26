package hudson.plugins.promoted_builds;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Job;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;

/**
 * Show promotion statuses of the project.
 * @see LastBuildPromotionStatusColumn
 * @since 2.22
 */
public class PromotionStatusColumn extends ListViewColumn {

    @DataBoundConstructor
    public PromotionStatusColumn() {
        super();
    }

    public PromotedProjectAction getAction(Job job) {
        PromotedProjectAction action = job.getAction(PromotedProjectAction.class);
        return action;
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {

        public DescriptorImpl() {
        }

        @Override
        public String getDisplayName() {
            return Messages.PromotionStatusColumn_DisplayName();
        }

        public boolean shownByDefault() {
            return false;
        }
    }

}
