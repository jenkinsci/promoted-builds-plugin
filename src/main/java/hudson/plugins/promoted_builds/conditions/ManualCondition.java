package hudson.plugins.promoted_builds.conditions;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.InvisibleAction;
import hudson.model.User;
import hudson.plugins.promoted_builds.PromotedBuildAction;
import hudson.plugins.promoted_builds.PromotionBadge;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.PromotionConditionDescriptor;
import hudson.plugins.promoted_builds.PromotionProcess;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * {@link PromotionCondition} that requires manual promotion.
 *
 * @author Kohsuke Kawaguchi
 * @author Peter Hayes
 */
public class ManualCondition extends PromotionCondition {
    private String users;

    public ManualCondition(String users) {
        this.users = users;
    }

    public String getUsers() {
        return users;
    }

    public Set<String> getUsersAsSet() {
        if (users == null || users.equals(""))
            return Collections.emptySet();

        Set<String> set = new HashSet<String>();
        for (String user : users.split(",")) {
            user = user.trim();

            if (user.trim().length() > 0)
                set.add(user);
        }
        
        return set;
    }

    @Override
    public PromotionBadge isMet(PromotionProcess promotionProcess, AbstractBuild<?,?> build) {
        List<ManualApproval> approvals = build.getActions(ManualApproval.class);

        for (ManualApproval approval : approvals) {
            if (approval.name.equals(promotionProcess.getName()))
                return new Badge(approval.authenticationName);
        }

        return null;
    }

    /**
     * Verifies that the currently logged in user (or anonymous) has permission
     * to approve the promotion and that the promotion has not already been
     * approved.
     */
    public boolean canApprove(PromotionProcess promotionProcess, AbstractBuild<?,?> build) {
        // Current user must be in users list or users list is empty
        Set<String> users = getUsersAsSet();
        if (!users.isEmpty()) {
            if (!users.contains(Hudson.getAuthentication().getName())) {
                return false;
            }
        }
        
        List<ManualApproval> approvals = build.getActions(ManualApproval.class);

        // For now, only allow approvals if this wasn't already approved
        for (ManualApproval approval : approvals) {
            if (approval.name.equals(promotionProcess.getName()))
                return false;
        }

        return true;
    }

    /**
     * Web method to handle the approval action submitted by the user.
     */
    public void doApprove(StaplerRequest req, StaplerResponse rsp, @QueryParameter("job") String jobName,
            @QueryParameter("buildNumber") int buildNumber, @QueryParameter("promotion") String promotionName) throws IOException {
        AbstractProject<?,?> job = Hudson.getInstance().getItemByFullName(jobName, AbstractProject.class);

        // Get the specific build from the job by number
        AbstractBuild<?,?> build = job.getBuildByNumber(buildNumber);

        PromotedBuildAction pba = build.getAction(PromotedBuildAction.class);

        PromotionProcess promotionProcess = pba.getPromotionProcess(promotionName);

        if (canApprove(promotionProcess, build)) {
            // add approval to build
            build.addAction(new ManualApproval(promotionName));
            build.save();

            // check for promotion
            promotionProcess.considerPromotion(build);
        }

        rsp.sendRedirect2("../../../..");
    }

    /*
     * Used to annotate the build to indicate that it was manually approved.  This
     * is then looked for in the isMet method.
     */
    public static final class ManualApproval extends InvisibleAction {
        public String name;
        public String authenticationName;

        public ManualApproval(String name) {
            this.name = name;
            authenticationName = Hudson.getAuthentication().getName();
        }
    }

    public static final class Badge extends PromotionBadge {
        public String authenticationName;

        public Badge(String authenticationName) {
            this.authenticationName = authenticationName;
        }

        public String getUserName() {
            if (authenticationName == null)
                return "N/A";

            User u = User.get(authenticationName, false);
            return u != null ? u.getDisplayName() : authenticationName;
        }
    }

    @Extension
    public static final class DescriptorImpl extends PromotionConditionDescriptor {
        public boolean isApplicable(AbstractProject<?,?> item) {
            return true;
        }

        public String getDisplayName() {
            return "Only when manually approved";
        }

        public ManualCondition newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new ManualCondition(formData.getString("users"));
        }

        public String getHelpFile() {
            return "/plugin/promoted-builds/conditions/manual.html";
        }
    }
}

