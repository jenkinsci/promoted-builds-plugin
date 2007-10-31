package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildBadgeAction;
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
     * List of promotion criteria names that this build qualified.
     */
    private final CopyOnWriteList<PromotionBadgeList> promotions = new CopyOnWriteList<PromotionBadgeList>();

    public PromotedBuildAction(AbstractBuild<?,?> owner) {
        assert owner!=null;
        this.owner = owner;
    }

    public PromotedBuildAction(AbstractBuild<?,?> owner, PromotionBadgeList badgeList) {
        this(owner);
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
    public boolean contains(PromotionProcess process) {
        for (PromotionBadgeList p : promotions)
            if(p.isFor(process))
                return true;
        return false;
    }

    /**
     * Checks if the given criterion is already promoted.
     */
    public boolean contains(String name) {
        for (PromotionBadgeList p : promotions)
            if(p.name.equals(name))
                return true;
        return false;
    }

    /**
     * Called when the build passes another promotion criterion.
     */
    public synchronized boolean add(PromotionBadgeList badgeList) throws IOException {
        for (PromotionBadgeList p : promotions)
            if(p.name.equals(badgeList.name))
                return false; // already promoted. noop

        this.promotions.add(badgeList);
        badgeList.parent = this;
        owner.save();
        return true;
    }

    /**
     * Gets the read-only view of all the promotions that this build achieved.
     */
    public List<PromotionBadgeList> getPromotions() {
        return promotions.getView();
    }

    /**
     * Finds the {@link PromotionBadgeList} that has matching {@link PromotionBadgeList#name} value.
     * Or null if not found.
     */
    public PromotionBadgeList getPromotion(String name) {
        for (PromotionBadgeList p : promotions)
            if(p.name.equals(name))
                return p;
        return null;
    }

    public boolean hasPromotion() {
        return !promotions.isEmpty();
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
        for (PromotionBadgeList p : promotions)
            p.parent = this;
        return this;
    }

//
// web methods
//
    /**
     * Binds {@link PromotionBadgeList} to URL hierarchy by its name.
     */
    public PromotionBadgeList getDynamic(String name) {
        return getPromotion(name);
    }

    /**
     * Force a promotion.
     */
    public void doForcePromotion(StaplerRequest req, StaplerResponse rsp, @QueryParameter("name") String name) throws IOException {
//        if(!req.getMethod().equals("POST")) {// require post,
//            rsp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
//            return;
//        }

        JobPropertyImpl pp = getProject().getProperty(JobPropertyImpl.class);
        if(pp==null)
            throw new IllegalStateException("This project doesn't have any promotion criteria set");

        PromotionProcess c = pp.getItem(name);
        if(c==null)
            throw new IllegalStateException("This project doesn't have the promotion criterion called "+name);
        add(new PromotionBadgeList(c,Collections.singleton(new ManualPromotionBadge())));

        rsp.sendRedirect2(".");
    }
}
