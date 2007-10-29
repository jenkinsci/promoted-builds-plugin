package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.tasks.BuildStep;
import hudson.util.DescribableList;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Build promotion configuration.
 *
 * Criteria for a build to be promoted, as well as what to do when a build is promoted.
 *
 * @author Kohsuke Kawaguchi
 */
public final class PromotionConfig implements DescribableList.Owner {
    private final String name;

    /**
     * {@link PromotionCondition}s. All have to be met for a build to be promoted.
     */
    public final DescribableList<PromotionCondition,PromotionConditionDescriptor> conditions =
            new DescribableList<PromotionCondition, PromotionConditionDescriptor>(this);

    /**
     * {@link BuildStep}s to be executed when a build is promoted.
     */
    public final DescribableList<? extends BuildStep,?> buildSteps = new DescribableList(this);

    /*package*/ PromotionConfig(StaplerRequest req, JSONObject c) throws FormException {
        this.name = c.getString("name");
        conditions.rebuild(req,c,PromotionConditions.CONDITIONS,"condition");
        buildSteps.rebuild(req,c,(List)PostPromotionTask.getAll(),"buildStep");
    }

    /**
     * Checks if all the conditions to promote a build is met.
     *
     * @return
     *      null if promotion conditions are not met.
     *      otherwise returns a list of badges that record how the promotion happened.
     */
    public PromotionBadgeList isMet(AbstractBuild<?,?> build) {
        List<PromotionBadge> badges = new ArrayList<PromotionBadge>();
        for (PromotionCondition cond : conditions) {
            PromotionBadge b = cond.isMet(build);
            if(b==null)
                return null;
            badges.add(b);
        }
        return new PromotionBadgeList(this,badges);
    }

    /**
     * Checks if the build is promotable, and if so, promote it.
     *
     * @return
     *      true if the build was promoted.
     */
    public boolean considerPromotion(AbstractBuild<?,?> build) throws IOException {
        PromotedBuildAction a = build.getAction(PromotedBuildAction.class);

        // if it's already promoted, no need to do anything.
        if(a!=null && a.contains(this))
            return false;

        PromotionBadgeList badges = isMet(build);
        if(badges==null)
            return false; // not this time

        // promote it
        if(a!=null) {
            a.add(badges);
        } else {
            build.addAction(new PromotedBuildAction(build,badges));
            build.save();
        }

        return true;
    }

    /**
     * Gets the human readable name set by the user. 
     */
    public String getName() {
        return name;
    }

    /**
     * @deprecated
     *      Save is not supported on this level.
     */
    public void save() throws IOException {
        // TODO?
    }
}
