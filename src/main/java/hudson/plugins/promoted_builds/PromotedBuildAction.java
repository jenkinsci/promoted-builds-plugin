package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildBadgeAction;
import hudson.model.Cause.UserCause;
import hudson.plugins.promoted_builds.conditions.ManualCondition;
import hudson.util.CopyOnWriteList;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link Action} for {@link AbstractBuild} indicating that it's promoted.
 *
 * @author Kohsuke Kawaguchi
 */
public final class PromotedBuildAction implements BuildBadgeAction {
    public final AbstractBuild<?,?> owner;

    /**
     * Per-process status.
     */
    private final CopyOnWriteList<Status> statuses = new CopyOnWriteList<Status>();

    /**
     * Per-process approval status
     */
    private final CopyOnWriteList<Approval> approvals = new CopyOnWriteList<Approval>();

    public PromotedBuildAction(AbstractBuild<?,?> owner) {
        assert owner!=null;
        this.owner = owner;
    }

    public PromotedBuildAction(AbstractBuild<?,?> owner, Status firstStatus) {
        this(owner);
        statuses.add(firstStatus);
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
    public boolean contains(PromotionProcess process) {
        for (Status s : statuses)
            if(s.isFor(process))
                return true;
        return false;
    }

    /**
     * Checks if the given criterion is already promoted.
     */
    public boolean contains(String name) {
        for (Status s : statuses)
            if(s.name.equals(name))
                return true;
        return false;
    }

    /**
     * Called when the build is qualified.
     */
    public synchronized boolean add(Status status) throws IOException {
        for (Status s : statuses)
            if(s.name.equals(status.name))
                return false; // already qualified. noop.

        this.statuses.add(status);
        status.parent = this;
        owner.save();
        return true;
    }

    /**
     * Gets the read-only view of all the promotions that this build achieved.
     */
    public List<Status> getPromotions() {
        return statuses.getView();
    }

    /**
     * Gets the read-only approval status that has a matching {@link Approval#name} value.
     * Or null if not found.
     */
    public Approval getApproval(String name) {
        for (Approval approval : approvals) {
            if (approval.name.equals(name)) {
                return approval;
            }
        }
        return null;
    }

    /**
     * Decide if the promotion with a matching {@link Promotion#name} can be approved.
     * This is decided by verifying the logged in user has permission, that the
     * promotion requires an approval, it has not already been promoted and
     * it has not already been approved.
     */
    public boolean canApprove(String name) {
        List<PromotionProcess> pps = getPendingPromotions();

        for (PromotionProcess pp : pps) {
            if (pp.getName().equals(name)) {
                // require manual approval?
                return pp.requiresApproval() &&
                        this.getProject().hasPermission(Promotion.PROMOTE) &&
                        getApproval(name) == null;
            }
        }
        return false;
    }

    /**
     * Finds the {@link Status} that has matching {@link Status#name} value.
     * Or null if not found.
     */
    public Status getPromotion(String name) {
        for (Status s : statuses)
            if(s.name.equals(name))
                return s;
        return null;
    }

    public boolean hasPromotion() {
        return !statuses.isEmpty();
    }

    public boolean canPromote() {
        return this.getProject().hasPermission(Promotion.PROMOTE);
    }

    /**
     * Gets list of {@link PromotionProcess}s that are not yet attained.
     * @return can be empty but never null.
     */
    public List<PromotionProcess> getPendingPromotions() {
        JobPropertyImpl pp = getProject().getProperty(JobPropertyImpl.class);
        if(pp==null)        return Collections.emptyList();

        List<PromotionProcess> r = new ArrayList<PromotionProcess>();
        for (PromotionProcess p : pp.getActiveItems()) {
            if(!contains(p))    r.add(p);
        }

        return r;
    }

    public String getIconFileName() {
        return "star.gif";
    }

    public String getDisplayName() {
        return "Promotion Status";
    }

    public String getUrlName() {
        return "promotion";
    }

    private Object readResolve() {
        // resurrect the parent pointer when read from disk
        for (Status s : statuses)
            s.parent = this;
        return this;
    }

//
// web methods
//
    /**
     * Binds {@link Status} to URL hierarchy by its name.
     */
    public Status getDynamic(String name, StaplerRequest req, StaplerResponse rsp) {
        return getPromotion(name);
    }

    /**
     * Approve a promotion.
     */
    public void doApprovePromotion(StaplerRequest req, StaplerResponse rsp, @QueryParameter("name") String name) throws IOException {
        if(!this.getProject().hasPermission(Promotion.PROMOTE))
            return;

        JobPropertyImpl pp = getProject().getProperty(JobPropertyImpl.class);
        if(pp==null)
            throw new IllegalStateException("This project doesn't have any promotion criteria set");

        PromotionProcess p = pp.getItem(name);
        if(p==null)
            throw new IllegalStateException("This project doesn't have the promotion criterion called "+name);


        // add approval to build and save
        if (canApprove(name)) {
            approvals.add(new Approval(name));
            owner.save();

            // consider promotion now that we've been approved
            p.considerPromotion(owner);
        }

        rsp.sendRedirect2(".");
    }
}
