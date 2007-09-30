package hudson.plugins.promoted_builds;

import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Project;
import hudson.tasks.Publisher;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
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
    private final List<PromotionCriteria> criteria = new ArrayList<PromotionCriteria>();

    private PublisherImpl(StaplerRequest req, JSONObject json) throws FormException {
        for( JSONObject c : (List<JSONObject>)JSONArray.fromObject(json.get("criteria")) )
            criteria.add(new PromotionCriteria(req,c));
    }

    /**
     * Gets the list of promotion criteria defined for this project.
     *
     * @return
     *      non-null and non-empty. Read-only.
     */
    public List<PromotionCriteria> getCriteria() {
        return criteria;
    }

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

        public PublisherImpl newInstance(StaplerRequest req, JSONObject json) throws FormException {
            return new PublisherImpl(req,json);
        }

        // exposed for Jelly
        public List<PromotionConditionDescriptor> getApplicableConditions(AbstractProject<?,?> p) {
            return PromotionConditions.getApplicableTriggers(p);
        }

        public static final DescriptorImpl INSTANCE = new DescriptorImpl();
    }
}
