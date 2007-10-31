package hudson.plugins.promoted_builds;

import hudson.Util;
import hudson.model.AbstractBuild;

import java.util.AbstractList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.ArrayList;

/**
 * List of {@link PromotionBadge}s indicating how a promotion happened.
 *
 * @author Kohsuke Kawaguchi
 * @see PromotedBuildAction#promotions
 */
public final class PromotionBadgeList extends AbstractList<PromotionBadge> {
    /**
     * Matches with {@link PromotionProcess#name}.
     */
    public final String name;

    private final PromotionBadge[] badges;

    /**
     * When did the build qualify for a promotion?
     */
    public final Calendar timestamp = new GregorianCalendar();

    /**
     * If the build is successfully promoted, the build number of {@link Promotion}
     * that represents that record.
     *
     * -1 to indicate that the promotion was not successful yet. 
     */
    private int promotion = -1;

    /**
     * Bulid numbers of {@link Promotion}s that are attempted.
     * If {@link Promotion} fails, this field can have multiple values.
     * Sorted in the ascending order.
     */
    private List<Integer> promotionAttempts = new ArrayList<Integer>();

    /*package*/ transient PromotedBuildAction parent;

    public PromotionBadgeList(PromotionProcess process, Collection<? extends PromotionBadge> badges) {
        this.name = process.getName();
        this.badges = badges.toArray(new PromotionBadge[badges.size()]);
    }

    public String getName() {
        return name;
    }

    /**
     * Gets the parent {@link PromotionBadgeList} that owns this object. 
     */
    public PromotedBuildAction getParent() {
        return parent;
    }

    /**
     * Gets the string that says how long since this promotion had happened.
     *
     * @return
     *      string like "3 minutes" "1 day" etc.
     */
    public String getTimestampString() {
        long duration = new GregorianCalendar().getTimeInMillis()-timestamp.getTimeInMillis();
        return Util.getTimeSpanString(duration);
    }

    /**
     * Gets the string that says how long did it toook for this build to be promoted.
     */
    public String getDelayString(AbstractBuild<?,?> owner) {
        long duration = timestamp.getTimeInMillis() - owner.getTimestamp().getTimeInMillis() - owner.getDuration();
        return Util.getTimeSpanString(duration);
    }

    public boolean isFor(PromotionProcess process) {
        return process.getName().equals(this.name);
    }

    /**
     * Returns the {@link Promotion} object that represents the successful promotion.
     *
     * @return
     *      null if the promotion has never been successful, or if it was but
     *      the record is already lost.
     */
    public Promotion getSuccessfulPromotion(JobPropertyImpl jp) {
        if(promotion>=0) {
            PromotionProcess p = jp.getItem(name);
            if(p!=null)
                return p.getBuildByNumber(promotion);
        }
        return null;
    }

    /**
     * Returns true if the promotion was successfully completed.
     */
    public boolean isPromotionSuccessful() {
        return promotion>=0;
    }

    /**
     * Returns true if at least one {@link Promotion} activity is attempted.
     * False if none is executed yet (this includes the case where it's in the queue.)
     */
    public boolean isPromotionAttempted() {
        return !promotionAttempts.isEmpty();
    }
    
    public PromotionBadge get(int index) {
        return badges[index];
    }

    public int size() {
        return badges.length;
    }

    /**
     * Called when a new promotion attempts for this build starts.
     */
    /*package*/ void addPromotionAttempt(Promotion p) {
        promotionAttempts.add(p.getNumber());
    }

    /**
     * Called when a promotion succeeds.
     */
    /*package*/ void onSuccessfulPromotion(Promotion p) {
        promotion = p.getNumber();
    }
}
