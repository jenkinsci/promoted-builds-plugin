package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Cause.LegacyCodeCause;
import hudson.model.DependencyGraph;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.JDK;
import hudson.model.Label;
import hudson.model.Queue.Item;
import hudson.model.Saveable;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A dummy {@link AbstractProject} to carry out promotion operations.
 *
 * @author Kohsuke Kawaguchi
 */
public final class PromotionProcess extends AbstractProject<PromotionProcess,Promotion> implements Saveable {

    /**
     * {@link PromotionCondition}s. All have to be met for a build to be promoted.
     */
    public final DescribableList<PromotionCondition,PromotionConditionDescriptor> conditions =
            new DescribableList<PromotionCondition, PromotionConditionDescriptor>(this);
    /**
     * The icon that represents this promotion process. This is the name of
     * the GIF icon that can be found in ${rootURL}/plugin/promoted-builds/icons/16x16/
     * and ${rootURL}/plugin/promoted-builds/icons/32x32/, e.g. <code>"star-gold"</code>.
     */
    public String icon;

    private List<BuildStep> buildSteps = new ArrayList<BuildStep>();

    /*package*/ PromotionProcess(JobPropertyImpl property, String name) {
        super(property, name);
    }

    /*package*/ void configure(StaplerRequest req, JSONObject c) throws Descriptor.FormException, IOException {
        // apply configuration
        conditions.rebuild(req,c, PromotionCondition.all());

        buildSteps = (List)Descriptor.newInstancesFromHeteroList(
                req, c, "buildStep", (List) PromotionProcess.getAll());
        icon = c.getString("icon");
        save();
    }

    /**
     * Returns the root project value.
     *
     * @return the root project value.
     */
    @Override
    public AbstractProject getRootProject() {
    	return getParent().getOwner().getRootProject();
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

    public DescribableList<Publisher, Descriptor<Publisher>> getPublishersList() {
        // TODO: extract from the buildsSteps field? Or should I separate builders and publishers?
        return new DescribableList<Publisher,Descriptor<Publisher>>(this);
    }

    protected Class<Promotion> getBuildClass() {
        return Promotion.class;
    }

    public List<BuildStep> getBuildSteps() {
        return buildSteps;
    }

    @Override public Label getAssignedLabel() {
        // Really would like to run on the exact node that the promoted build ran on,
        // not just the same label.. but at least this works if job is tied to one node:
        return getOwner().getAssignedLabel();
    }

    @Override public JDK getJDK() {
        return getOwner().getJDK();
    }

    /**
     * Get the icon name, without the extension. It will always return a non null
     * and non empty string, as <code>"star-gold"</code> is used for compatibility
     * for older promotions configurations.
     * 
     * @return the icon name
     */
    public String getIcon() {
    	return getIcon(icon);
    }

    /**
     * Handle compatibility with pre-1.8 configs.
     * 
     * @param sIcon
     *      the name of the icon used by this promotion; if null or empty,
     *      we return the gold icon for compatibility with previous releases
     * @return the icon file name for this promotion
     */
    private static String getIcon(String sIcon) {
    	if ((sIcon == null) || sIcon.equals(""))
            return "star-gold";
    	else
            return sIcon;
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

        promote(build,new LegacyCodeCause(),qualification); // TODO: define promotion cause

        return true;
    }

    /**
     * Promote the given build by using the given qualification.
     *
     * @param cause
     *      Why the build is promoted?
     */
    public void promote(AbstractBuild<?,?> build, Cause cause, Status qualification) throws IOException {
        PromotedBuildAction a = build.getAction(PromotedBuildAction.class);
        // build is qualified for a promotion.
        if(a!=null) {
            a.add(qualification);
        } else {
            build.addAction(new PromotedBuildAction(build,qualification));
            build.save();
        }

        // schedule promotion activity.
        scheduleBuild(build,cause);
    }

    /**
     * @deprecated
     *      You need to be using {@link #scheduleBuild(AbstractBuild)}
     */
    public boolean scheduleBuild() {
        return super.scheduleBuild();
    }

    public boolean scheduleBuild(AbstractBuild<?,?> build) {
        return scheduleBuild(build,new LegacyCodeCause());
    }

    public boolean scheduleBuild(AbstractBuild<?,?> build, Cause cause) {
        assert build.getProject()==getOwner();

        // remember what build we are promoting
        return super.scheduleBuild(0,cause,new PromotionTargetAction(build));
    }

    public boolean isInQueue(AbstractBuild<?,?> build) {
        for (Item item : Hudson.getInstance().getQueue().getItems(this))
            if (item.getAction(PromotionTargetAction.class).resolve()==build)
                return true;
        return false;
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
        addTo(Builder.all(), list);
        addTo(Publisher.all(), list);
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
