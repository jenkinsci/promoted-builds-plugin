package hudson.plugins.promoted_builds;

import hudson.Util;
import hudson.model.AbstractBuild;

import java.util.AbstractList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;

/**
 * List of {@link PromotionBadge}s indicating how a promotion happened.
 * 
 * @author Kohsuke Kawaguchi
 */
public final class PromotionBadgeList extends AbstractList<PromotionBadge> {
    /**
     * Matches with {@link PromotionConfig#name}.
     */
    public final String criterion;

    private final PromotionBadge[] badges;

    /**
     * When did the promotion happen?
     */
    public final Calendar timestamp = new GregorianCalendar();

    public PromotionBadgeList(PromotionConfig config, Collection<? extends PromotionBadge> badges) {
        this.criterion = config.getName();
        this.badges = badges.toArray(new PromotionBadge[badges.size()]);
    }

    public String getName() {
        return criterion;
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

    public boolean isFor(PromotionConfig config) {
        return config.getName().equals(this.criterion);
    }

    public PromotionBadge get(int index) {
        return badges[index];
    }

    public int size() {
        return badges.length;
    }
}
