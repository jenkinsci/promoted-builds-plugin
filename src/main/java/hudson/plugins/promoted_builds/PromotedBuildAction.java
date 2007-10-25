package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;
import hudson.model.Action;

/**
 * {@link Action} for {@link AbstractBuild} indicating that it's promoted.
 *
 * @author Kohsuke Kawaguchi
 */
public final class PromotedBuildAction implements Action {
    /**
     * List of promotion criteria names that this build qualified.
     */
    private volatile String[] promotions;

    public PromotedBuildAction(PromotionCriterion criterion) {
        promotions = new String[]{criterion.getName()};
    }

    /**
     * Checks if the given criterion is already promoted.
     */
    public synchronized boolean contains(PromotionCriterion criterion) {
        for (String p : promotions)
            if(p.equals(criterion.getName()))
                return true;
        return false;
    }
    /**
     * Called when the build passes another promotion criterion.
     */
    public synchronized boolean add(PromotionCriterion criterion) {
        String n = criterion.getName();
        for (String p : promotions)
            if(n.equals(p))
                return false; // noop

        String[] r = new String[promotions.length+1];
        System.arraycopy(promotions,0,r,0,promotions.length);
        r[promotions.length] = n;

        // atomically replace it
        this.promotions = r;
        return true;
    }

    public String getIconFileName() {
        // TODO: fix this
        return "health-80plus.gif";
    }

    public String getDisplayName() {
        return "Promotion Status";
    }

    public String getUrlName() {
        return "promoted";
    }
}
