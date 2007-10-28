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
    public synchronized boolean add(PromotionBadgeList badgeList) throws IOException {
        for (PromotionBadgeList p : promotions)
            if(p.criterion.equals(badgeList.criterion))
                return false; // already promoted. noop

        this.promotions.add(badgeList);
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
     * Gets list of {@link PromotionCriterion}s that are not yet attained.
     * @return can be empty but never null.
     */
    public List<PromotionCriterion> getPendingPromotions() {
        JobPropertyImpl pp = getProject().getProperty(JobPropertyImpl.class);
        if(pp==null)        return Collections.emptyList();

        List<PromotionCriterion> r = new ArrayList<PromotionCriterion>();
        for (PromotionCriterion c : pp.getCriteria()) {
            if(!contains(c))    r.add(c);
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
        return "promoted";
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

        PromotionCriterion c = pp.getCriterion(name);
        if(c==null)
            throw new IllegalStateException("This project doesn't have the promotion criterion called "+name);
        promotions.add(new PromotionBadgeList(c,Collections.singleton(new ManualPromotionBadge())));
        owner.save();

        rsp.sendRedirect2(".");
    }
}
