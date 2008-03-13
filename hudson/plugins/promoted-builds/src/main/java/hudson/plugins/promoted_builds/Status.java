package hudson.plugins.promoted_builds;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.util.Iterators;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.List;
import java.io.IOException;

/**
 * Promotion status of a build wrt a specific {@link PromotionProcess}.
 *
 * @author Kohsuke Kawaguchi
 * @see PromotedBuildAction#statuses
 */
public final class Status {
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

    public Status(PromotionProcess process, Collection<? extends PromotionBadge> badges) {
        this.name = process.getName();
        this.badges = badges.toArray(new PromotionBadge[badges.size()]);
    }

    public String getName() {
        return name;
    }

    /**
     * Gets the parent {@link Status} that owns this object.
     */
    public PromotedBuildAction getParent() {
        return parent;
    }

    /**
     * Gets the {@link PromotionProcess} that this object deals with.
     */
    public PromotionProcess getProcess() {
        JobPropertyImpl jp = parent.getProject().getProperty(JobPropertyImpl.class);
        if(jp==null)    return null;
        return jp.getItem(name);
    }

    /**
     * Gets the build that was qualified for a promotion.
     */
    public AbstractBuild<?,?> getTarget() {
        return getParent().owner;
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

    /**
     * Returns true if the promotion for this is pending in the queue,
     * waiting to be executed.
     */
    public boolean isInQueue() {
        PromotionProcess p = getProcess();
        return p!=null && p.isInQueue(getTarget());
    }

    /**
     * Gets the badges indicating how did a build qualify for a promotion.
     */
    public List<PromotionBadge> getBadges() {
        return Arrays.asList(badges);
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

//
// web bound methods
//

    /**
     * Gets the last successful {@link Promotion}.
     */
    public Promotion getLastSuccessful() {
        PromotionProcess p = getProcess();
        for( Integer n : Iterators.reverse(promotionAttempts) ) {
            Promotion b = p.getBuildByNumber(n);
            if(b!=null && b.getResult()== Result.SUCCESS)
                return b;
        }
        return null;
    }

    /**
     * Gets the last successful {@link Promotion}.
     */
    public Promotion getLastFailed() {
        PromotionProcess p = getProcess();
        for( Integer n : Iterators.reverse(promotionAttempts) ) {
            Promotion b = p.getBuildByNumber(n);
            if(b!=null && b.getResult()!=Result.SUCCESS)
                return b;
        }
        return null;
    }

    /**
     * Schedules a new build.
     */
    public void doBuild(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        if(!Hudson.adminCheck(req,rsp))
            return;
        getProcess().scheduleBuild(getTarget());
        // TODO: we need better visual feed back so that the user knows that the build happened.
        rsp.forwardToPreviousPage(req);
    }
}
