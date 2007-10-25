package hudson.plugins.promoted_builds;

import java.util.AbstractList;
import java.util.List;

/**
 * List of {@link PromotionBadge}s indicating how a promotion happened.
 * 
 * @author Kohsuke Kawaguchi
 */
public final class PromotionBadgeList extends AbstractList<PromotionBadge> {
    /**
     * Matches with {@link PromotionCriterion#name}.
     */
    public final String criterion;

    private final PromotionBadge[] badges;

    public PromotionBadgeList(PromotionCriterion criterion, List<PromotionBadge> badges) {
        this.criterion = criterion.getName();
        this.badges = badges.toArray(new PromotionBadge[badges.size()]);
    }

    public boolean isFor(PromotionCriterion criterion) {
        return criterion.getName().equals(this.criterion);
    }

    public PromotionBadge get(int index) {
        return badges[index];
    }

    public int size() {
        return badges.length;
    }
}
