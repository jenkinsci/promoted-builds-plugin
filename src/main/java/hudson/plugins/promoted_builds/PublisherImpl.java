package hudson.plugins.promoted_builds;

import hudson.Launcher;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.model.AbstractProject;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.List;

/**
 * This doesn't really publish anything, bu we want the configuration to show
 * up in the post-build action because the promotion is a post-build activity.
 *
 * <p>
 * The primary role of this class is to remember the promotion criteria.
 *
 * @author Kohsuke Kawaguchi
 */
public class PublisherImpl extends Publisher {
    public boolean prebuild(Build build, BuildListener listener) {
        return true;
    }

    public boolean perform(Build<?,?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return true;
    }

    public Action getProjectAction(Project project) {
        return null;
    }

    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.INSTANCE;
    }

    public static final class DescriptorImpl extends Descriptor<Publisher> {
        private DescriptorImpl() {
            super(PublisherImpl.class);
        }

        public String getDisplayName() {
            return "Promote Builds When...";
        }

        public Publisher newInstance(StaplerRequest req) throws FormException {
            // TODO
            throw new UnsupportedOperationException();
        }

        // exposed for Jelly
        public List<PromotionConditionDescriptor> getApplicableConditions(AbstractProject<?,?> p) {
            return PromotionConditions.getApplicableTriggers(p);
        }

        public static final DescriptorImpl INSTANCE = new DescriptorImpl();
    }
}
