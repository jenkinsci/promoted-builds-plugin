package hudson.plugins.promoted_builds;

import hudson.model.User;

/**
 * Indicates that the promotion happened manually.
 *
 * @author Kohsuke Kawaguchi
 * @author Peter Hayes
 */
public final class ManualPromotionBadge extends PromotionBadge {
    private String authenticationName;

    public ManualPromotionBadge(Approval approval) {
        authenticationName = approval.authenticationName;
    }

    public String getUserName() {
        if (authenticationName == null)
            return "N/A";
        
        User u = User.get(authenticationName, false);
        return u != null ? u.getDisplayName() : authenticationName;
    }
}
