package hudson.plugins.promoted_builds;

import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.tasks.BuildStep;
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
public final class JobPropertyImpl extends JobProperty<AbstractProject<?,?>> {
    private final List<PromotionConfig> configs = new ArrayList<PromotionConfig>();

    private JobPropertyImpl(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
        for( JSONObject c : (List<JSONObject>) JSONArray.fromObject(json.get("config")) )
            configs.add(new PromotionConfig(req,c));
    }

    /**
     * Gets the list of promotion criteria defined for this project.
     *
     * @return
     *      non-null and non-empty. Read-only.
     */
    public List<PromotionConfig> getConfigs() {
        return configs;
    }

    /**
     * Finds a config by name.
     */
    public PromotionConfig getConfig(String name) {
        for (PromotionConfig c : configs) {
            if(c.getName().equals(name))
                return c;
        }
        return null;
    }

    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.INSTANCE;
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

        // exposed for Jelly
        public List<Descriptor<? extends BuildStep>> getApplicableBuildSteps(AbstractProject<?,?> p) {
            return PostPromotionTask.getAll();
        }

        public static final DescriptorImpl INSTANCE = new DescriptorImpl();
    }
}
