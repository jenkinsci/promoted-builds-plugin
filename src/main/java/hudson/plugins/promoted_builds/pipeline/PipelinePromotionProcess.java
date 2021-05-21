package hudson.plugins.promoted_builds.pipeline;

import hudson.model.*;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.PromotionBadge;
import hudson.plugins.promoted_builds.Status;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static org.jenkinsci.plugins.tokenmacro.impl.XmlFileMacro.LOGGER;

public class PipelinePromotionProcess {

    @Nonnull
    private final List<ParameterValue> parameters;      // [tried to consume the parameters from PromotionCondition][]

    PipelinePromotionProcess(List<ParameterValue> parameters) {

        this.parameters = parameters;
    }

    public List<PipelinePromotionProcess> justCheck = new ArrayList<>(parameters); //[copy the parameters to another List]


    public String icon;

    public String getIcon() {

        return getIcon(icon);
    }

    @Nonnull
    private static String getIcon(@CheckForNull String sIcon) {
        if ((sIcon == null) || sIcon.equals(""))
            return "star-gold";
        else
            return sIcon;
    }

    /**


    @Override                                              // Origin
    public Job getRootProject() {

        return getParent().getOwner().getRootProject();
    }
    // QUES: What About this JobPropertyImpl
    @Override
    public JobPropertyImpl getParent() {
        return (JobPropertyImpl)super.getParent();
    }

     */
    //(getParent) does not register
    public Job<?,?> getOwner() {
        return getParent().getOwner();
    }



    @Override public boolean supportsMakeDisabled() {
        return true;
    }



    //My approach
    /**
    @CheckForNull
    public Status isMet(Run<?,?> run) {
        List<PromotionBadge> badges = new ArrayList<PromotionBadge>();
        for (PipelinePromotionProcess cond : justCheck) {           //checks the new parameter from "JustCheck"
            PromotionBadge b = cond.isMet(this, run);
            if(b == null)
                return null;
            badges.add(b);
        }
        return new Status(this,badges);
    }
     */



    @CheckForNull
    public Future<Promotion> considerPromotion2(Run<?,?> run, List<ParameterValue> params, TaskListener listener) throws IOException {


        PipelinePromotedBuildAction a = run.getAction(PipelinePromotedBuildAction.class);

        // if it's already promoted, no need to do anything.
        if(a!=null && a.contains(this))
            return null;
        // getName() --- is from AbstractItem so does not compile!!
        LOGGER.fine("Considering the promotion of "+run+" via "+ getName() +" with parameters");
        Status qualification = isMet(run);
        if(qualification==null)
            return null; // not this time

        LOGGER.fine("Promotion condition of "+run+" is met: "+qualification);
        Future<Promotion> f = promote2(run, new UserCause(), qualification, params); // TODO: define promotion cause
        if (f==null)
            LOGGER.warning(run+" qualifies for a promotion but the queueing failed.");
        return f;
    }





    public Future<Promotion> promote2(Run<?,?> run, Cause cause, Status qualification, List<ParameterValue> params, TaskListener listener) throws IOException {
        PipelinePromotedBuildAction a = run.getAction(PipelinePromotedBuildAction.class);
        // build is qualified for a promotion.
        if(a!=null) {
            a.add(qualification);
        } else {
            run.addAction(new PipelinePromotedBuildAction(run,qualification));
            run.save();
        }

        // schedule promotion activity.
        return scheduleBuild2(run,cause, params);
    }
    //QUES: Should I leave it till here only?

    /**
     * @deprecated
     *      You need to be using {@link #scheduleBuild(AbstractBuild)}
     */


    public boolean scheduleBuild(@Nonnull AbstractBuild<?,?> build) {
        return scheduleBuild(build,new UserCause());
    }


    //QUES: Left as is!!
    @CheckForNull
    public Future<Promotion> scheduleBuild2(@Nonnull Run<?,?> run,
                                            Cause cause, @CheckForNull List<ParameterValue> params) {
        List<Action> actions = new ArrayList<Action>();
        actions.add(Promotion.PromotionParametersAction.buildFor(run, params));
        actions.add(new PipelineTargetAction(run));

        // remember what build we are promoting
        return super.scheduleBuild2(0, cause, actions.toArray(new Action[actions.size()]));
    }

    //QUES: After this it's all StaplerRequests
    //Now go for (Status,PromotedBuildAction)







}