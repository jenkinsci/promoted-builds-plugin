package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.util.CopyOnWriteList;

import java.util.List;

/**
 * {@link Action} for {@link AbstractBuild} indicating that it's promoted.
 *
 * @author Kohsuke Kawaguchi
 */
public final class PromotedBuildAction implements Action {
    public final AbstractBuild<?,?> owner;

    /**
     * List of promotion criteria names that this build qualified.
     */
    private final CopyOnWriteList<PromotionBadgeList> promotions = new CopyOnWriteList<PromotionBadgeList>();

    public PromotedBuildAction(AbstractBuild<?,?> owner, PromotionBadgeList badgeList) {
        assert owner!=null;
        this.owner = owner;
        promotions.add(badgeList);
    }

    /**
     * Gets the owning project.
     */
    public AbstractProject<?,?> getProject() {
        return owner.getProject();
    }

    /**
     * Checks if the given criterion is already promoted.
     */
    public boolean contains(PromotionCriterion criterion) {
        for (PromotionBadgeList p : promotions)
            if(p.isFor(criterion))
                return true;
        return false;
    }
    /**
     * Called when the build passes another promotion criterion.
     */
    public synchronized boolean add(PromotionBadgeList badgeList) {
        for (PromotionBadgeList p : promotions)
            if(p.criterion.equals(badgeList.criterion))
                return false; // already promoted. noop

        this.promotions.add(badgeList);
        return true;
    }

    /**
     * Gets the read-only view of all the promotions that this build achieved.
     */
    public List<PromotionBadgeList> getPromotions() {
        return promotions.getView();
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
