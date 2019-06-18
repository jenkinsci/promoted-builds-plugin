package hudson.plugins.promoted_builds;

import com.sun.mail.imap.ACL;
import hudson.model.*;
import hudson.plugins.promoted_builds.conditions.PipelineSelfPromotionCondition;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static org.jenkinsci.plugins.tokenmacro.impl.XmlFileMacro.LOGGER;

public class PipelinePromotionProcess {

    @Nonnull
    private final List<ParameterValue> parameters;

    PipelinePromotionProcess(List<ParameterValue> parameters) {

        this.parameters = parameters;
    }

    public List<PipelinePromotionProcess> justCheck = new ArrayList<>(parameters);


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
    //Probably won't work

    @Override
    public Job getRootProject() {

        return getParent().getOwner().getRootProject();
    }
    // QUES: What About this JobPropertyImpl
    @Override
    public JobPropertyImpl getParent() {
        return (JobPropertyImpl)super.getParent();
    }

    /**
     * Gets the owner {@link AbstractProject} that configured {@link JobPropertyImpl} as
     * a job property.
     * @return Current owner project
     */
    //(getParent) does not register
    public Job<?,?> getOwner() {
        return getParent().getOwner();
    }


    /**
     * JENKINS-27716: Since 1.585, the promotion must explicitly indicate that
     * it can be disabled. Otherwise, promotions which trigger automatically
     * upon build completion will execute, even if they're archived.
     */
    @Override public boolean supportsMakeDisabled() {
        return true;
    }




    /**
     * Checks if all the conditions to promote a build is met.
     *
     * @param build Build to be checked
     * @return
     *      {@code null} if promotion conditions are not met.
     *      otherwise returns a list of badges that record how the promotion happened.
     */
    @CheckForNull
    public Status isMet(Run<?,?> run) {
        List<PromotionBadge> badges = new ArrayList<PromotionBadge>();
        for (PipelinePromotionProcess cond : justCheck) {
            PromotionBadge b = cond.isMet(this, run);
            if(b == null)
                return null;
            badges.add(b);
        }
        return new Status(this,badges);
    }
    // QUES: Now Goto Status?
    /**
     * Checks if the build is promotable, and if so, promote it.
     *
     * @param build Build to be promoted
     * @return
     *      {@code null} if the build was not promoted, otherwise Future that kicks in when the build is completed.
     * @throws IOException
     */


    @CheckForNull
    public Future<Promotion> considerPromotion2(Run<?,?> run, List<ParameterValue> params, TaskListener listener) throws IOException {

       // QUES: Now Also GOTO PromotedBuildAction
        PipelinePromotedBuildAction a = run.getAction(PipelinePromotedBuildAction.class);

        // if it's already promoted, no need to do anything.
        if(a!=null && a.contains(this))
            return null;

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


    /**
     * Promote the given build by using the given qualification.
     *
     * @param build Build to promote
     * @param cause Why the build is promoted?
     * @param qualification Initial promotion status
     * @return Future to track the completion of the promotion.
     * @throws IOException Promotion failure
     */
    public Future<Promotion> promote2(Run<?,?> run, Cause cause, Status qualification) throws IOException {
        return promote2(run, cause, qualification, null);
    }

    /**
     * Promote the given build by using the given qualification.
     *
     * @param build Build to promote
     * @param cause Why the build is promoted?
     * @param qualification Initial promotion status
     * @param params Promotion parameters
     * @return Future to track the completion of the promotion.
     * @throws IOException Promotion failure
     */
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
    // When are the real "Star" badges assigned maybe we should work till there?
    /**
     * @deprecated
     *      You need to be using {@link #scheduleBuild(AbstractBuild)}
     */


    public boolean scheduleBuild(@Nonnull AbstractBuild<?,?> build) {
        return scheduleBuild(build,new UserCause());
    }

    /**
     * @param build Target build
     * @param cause Promotion cause
     * @return {@code true} if scheduling is successful
     * @deprecated
     *      Use {@link #scheduleBuild2(AbstractBuild, Cause)}
     */

    /**
     * Schedules the promotion.
     * @param build Target build
     * @param cause Promotion cause
     * @param params Parameters to be passed
     * @return Future result or {@code null} if the promotion cannot be scheduled
     */

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