package hudson.plugins.promoted_builds.conditions;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.PromotionConditionDescriptor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * @author Kohsuke Kawaguchi
 */
public class DownstreamPassCondition extends PromotionCondition {
    public boolean isMet(AbstractBuild<?, ?> build) {
        // TODO
        throw new UnsupportedOperationException();
    }

    public PromotionConditionDescriptor getDescriptor() {
        return DescriptorImpl.INSTANCE;
    }

    public static final class DescriptorImpl extends PromotionConditionDescriptor {
        public DescriptorImpl() {
            super(DownstreamPassCondition.class);
        }

        public boolean isApplicable(AbstractProject<?,?> item) {
            return true;
        }

        public String getDisplayName() {
            return "When the following downstream projects build successfully";
        }

        public PromotionCondition newInstance(StaplerRequest req) throws FormException {
            return new DownstreamPassCondition();
        }

        private static final DescriptorImpl INSTANCE = new DescriptorImpl();
    }
}
