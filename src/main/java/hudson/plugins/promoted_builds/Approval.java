package hudson.plugins.promoted_builds;

import hudson.model.Hudson;

/**
 * Tracks if a promotion has been approved and by whom
 *
 * @author Peter Hayes
 */
public class Approval {
    
    /**
     * Matches with {@link PromotionProcess#name}.
     */
    public final String name;

    public final String authenticationName;

    public Approval(String name) {
        this.name = name;

        this.authenticationName = Hudson.getAuthentication().getName();
    }
}
