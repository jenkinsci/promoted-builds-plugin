package hudson.plugins.promoted_builds;

import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.Job;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * This doesn't really publish anything, but we want the configuration to show
 * up in the post-build action because the promotion is a post-build activity.
 *
 * <p>
 * The primary role of this class is to remember the promotion criteria.
 *
 * @author Kohsuke Kawaguchi
 */
public class JobPropertyImpl extends JobProperty<AbstractProject<?,?>> {
    private final List<PromotionCriterion> criteria = new ArrayList<PromotionCriterion>();

    private JobPropertyImpl(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
        for( JSONObject c : (List<JSONObject>) JSONArray.fromObject(json.get("criteria")) )
            criteria.add(new PromotionCriterion(req,c));
    }

    /**
     * Gets the list of promotion criteria defined for this project.
     *
     * @return
     *      non-null and non-empty. Read-only.
     */
    public List<PromotionCriterion> getCriteria() {
        return criteria;
    }

    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.INSTANCE;
    }

    public void onCompleted(AbstractBuild<?,?> build, TaskListener listener) {
    }

    public static final class DescriptorImpl extends JobPropertyDescriptor {
        private DescriptorImpl() {
            super(JobPropertyImpl.class);
        }

        public String getDisplayName() {
            return "Promote Builds When...";
        }

        public boolean isApplicable(Class<? extends Job> jobType) {
            return AbstractProject.class.isAssignableFrom(jobType);
        }

        public JobPropertyImpl newInstance(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
            return new JobPropertyImpl(req,json);
        }

        // exposed for Jelly
        public List<PromotionConditionDescriptor> getApplicableConditions(AbstractProject<?,?> p) {
            return PromotionConditions.getApplicableTriggers(p);
        }

        public static final DescriptorImpl INSTANCE = new DescriptorImpl();
    }
}
