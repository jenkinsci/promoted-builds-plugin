package hudson.plugins.promoted_builds;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.DependencyGraph;
import hudson.model.Descriptor;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

/**
 * A dummy {@link AbstractProject} to carry out promotion operations.
 *
 * @author Kohsuke Kawaguchi
 */
public final class PromotionProcess extends AbstractProject<PromotionProcess,Promotion> implements DescribableList.Owner {

    /**
     * {@link PromotionCondition}s. All have to be met for a build to be promoted.
     */
    public final DescribableList<PromotionCondition,PromotionConditionDescriptor> conditions =
            new DescribableList<PromotionCondition, PromotionConditionDescriptor>(this);

    private List<BuildStep> buildSteps;

    /**
     * Queues of builds to be promoted.
     */
    /*package*/ transient volatile List<AbstractBuild<?,?>> queue;

    /*package*/ PromotionProcess(JobPropertyImpl property, String name) {
        super(property, name);
    }

    /*package*/ void configure(StaplerRequest req, JSONObject c) throws Descriptor.FormException, IOException {
        // apply configuration
        conditions.rebuild(req,c, PromotionConditions.CONDITIONS,"condition");

        buildSteps = (List)Descriptor.newInstancesFromHeteroList(
                req, c, "buildStep", (List) PromotionProcess.getAll());
        save();
    }

    @Override
    public JobPropertyImpl getParent() {
        return (JobPropertyImpl)super.getParent();
    }

    /**
     * Gets the owner {@link AbstractProject} that configured {@link JobPropertyImpl} as
     * a job property.
     */
    public AbstractProject<?,?> getOwner() {
        return getParent().getOwner();
    }

    public FilePath getWorkspace() {
        return getOwner().getWorkspace();
    }

    public DescribableList<Publisher, Descriptor<Publisher>> getPublishersList() {
        // TODO: extract from the buildsSteps field? Or should I separate builders and publishers?
        return new DescribableList<Publisher,Descriptor<Publisher>>(this);
    }

    protected Class<Promotion> getBuildClass() {
        return Promotion.class;
    }

    public List<BuildStep> getBuildSteps() {
        return Collections.unmodifiableList(buildSteps);
    }

    /**
     * Checks if all the conditions to promote a build is met.
     *
     * @return
     *      null if promotion conditions are not met.
     *      otherwise returns a list of badges that record how the promotion happened.
     */
    public Status isMet(AbstractBuild<?,?> build) {
        List<PromotionBadge> badges = new ArrayList<PromotionBadge>();
        for (PromotionCondition cond : conditions) {
            PromotionBadge b = cond.isMet(build);
            if(b==null)
                return null;
            badges.add(b);
        }
        return new Status(this,badges);
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

        Status qualification = isMet(build);
        if(qualification==null)
            return false; // not this time

        promote(build,qualification);

        return true;
    }

    /**
     * Promote the given build by using the given qualification.
     */
    public void promote(AbstractBuild<?,?> build, Status qualification) throws IOException {
        PromotedBuildAction a = build.getAction(PromotedBuildAction.class);
        // build is qualified for a promotion.
        if(a!=null) {
            a.add(qualification);
        } else {
            build.addAction(new PromotedBuildAction(build,qualification));
            build.save();
        }

        // schedule promotion activity.
        scheduleBuild(build);
    }

    /**
     * @deprecated
     *      You need to be using {@link #scheduleBuild(AbstractBuild)}
     */
    public boolean scheduleBuild() {
        return super.scheduleBuild();
    }

    public boolean scheduleBuild(AbstractBuild<?,?> build) {
        assert build.getProject()==getOwner();

        if(queue ==null)
            queue = Collections.synchronizedList(new LinkedList<AbstractBuild<?,?>>());
        queue.add(build);

        return super.scheduleBuild();
    }

    public boolean isInQueue(AbstractBuild<?,?> build) {
        return isInQueue() && queue!=null && queue.contains(build);
    }

//
// these are dummy implementations to implement abstract methods.
// need to think about what the implications are.
//
    public boolean isFingerprintConfigured() {
        throw new UnsupportedOperationException();
    }

    protected void buildDependencyGraph(DependencyGraph graph) {
        throw new UnsupportedOperationException();
    }

    public static List<Descriptor<? extends BuildStep>> getAll() {
        List<Descriptor<? extends BuildStep>> list = new ArrayList<Descriptor<? extends BuildStep>>();
        addTo(BuildStep.BUILDERS, list);
        addTo(BuildStep.PUBLISHERS, list);
        return list;
    }

    private static void addTo(List<? extends Descriptor<? extends BuildStep>> source, List<Descriptor<? extends BuildStep>> list) {
        for (Descriptor<? extends BuildStep> d : source) {
            if (d instanceof BuildStepDescriptor) {
                BuildStepDescriptor bsd = (BuildStepDescriptor) d;
                if(bsd.isApplicable(PromotionProcess.class))
                    list.add(d);
            }
        }
    }
}
