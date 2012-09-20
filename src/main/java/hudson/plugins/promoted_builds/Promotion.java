package hudson.plugins.promoted_builds;

import hudson.EnvVars;
import hudson.console.HyperlinkNote;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.PermissionScope;
import hudson.slaves.WorkspaceList;
import hudson.slaves.WorkspaceList.Lease;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildTrigger;
import hudson.triggers.Trigger;
import jenkins.model.Jenkins;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;

/**
 * Records a promotion process.
 *
 * @author Kohsuke Kawaguchi
 */
public class Promotion extends AbstractBuild<PromotionProcess,Promotion> 
	implements Comparable<Promotion>{

    public Promotion(PromotionProcess job) throws IOException {
        super(job);
    }

    public Promotion(PromotionProcess job, Calendar timestamp) {
        super(job, timestamp);
    }

    public Promotion(PromotionProcess project, File buildDir) throws IOException {
        super(project, buildDir);
    }

    /**
     * Gets the build that this promotion promoted.
     *
     * @return
     *      null if there's no such object. For example, if the build has already garbage collected.
     */
    public AbstractBuild<?,?> getTarget() {
        PromotionTargetAction pta = getAction(PromotionTargetAction.class);
        return pta.resolve(this);
    }

    public AbstractBuild<?,?> getRootBuild() {
        // TODO: once 1.421 ships, update this to getTarget().getRootBuild() for correctness.
        return getTarget();
    }

    @Override
    public String getUrl() {
        return getTarget().getUrl() + "promotion/" + getParent().getName() + "/promotionBuild/" + getNumber() + "/";
    }

    /**
     * Gets the {@link Status} object that keeps track of what {@link Promotion}s are
     * performed for a build, including this {@link Promotion}.
     */
    public Status getStatus() {
        return getTarget().getAction(PromotedBuildAction.class).getPromotion(getParent().getName());
    }

    @Override
    public EnvVars getEnvironment(TaskListener listener) throws IOException, InterruptedException {
        EnvVars e = super.getEnvironment(listener);

        // Augment environment with target build's information
        String rootUrl = Jenkins.getInstance().getRootUrl();
        AbstractBuild<?, ?> target = getTarget();
        if(rootUrl!=null)
            e.put("PROMOTED_URL",rootUrl+target.getUrl());
        e.put("PROMOTED_JOB_NAME", target.getParent().getName());
        e.put("PROMOTED_NUMBER", Integer.toString(target.getNumber()));
        e.put("PROMOTED_ID", target.getId());
        EnvVars envScm = new EnvVars();
        target.getProject().getScm().buildEnvVars( target, envScm );
        for ( Entry<String, String> entry : envScm.entrySet() )
        {
            e.put( "PROMOTED_" + entry.getKey(), entry.getValue() );
        }

        // Allow the promotion status to contribute to build environment
        getStatus().buildEnvVars(this, e);

        return e;
    }

    public void run() {
        getStatus().addPromotionAttempt(this);
        run(new RunnerImpl(this));
    }

    protected class RunnerImpl extends AbstractRunner {
        final Promotion promotionRun;
        
        RunnerImpl(final Promotion promotionRun) {
            this.promotionRun = promotionRun;
        }
        
        @Override
        protected Lease decideWorkspace(Node n, WorkspaceList wsl) throws InterruptedException, IOException {
            String customWorkspace = getProject().getCustomWorkspace();
            if (customWorkspace != null)
                // we allow custom workspaces to be concurrently used between jobs.
                return Lease.createDummyLease(
                        n.getRootPath().child(getEnvironment(listener).expand(customWorkspace)));
            return wsl.acquire(n.getWorkspaceFor((TopLevelItem)getTarget().getProject()),true);
        }

        protected Result doRun(BuildListener listener) throws Exception {
            AbstractBuild<?, ?> target = getTarget();

            listener.getLogger().println(
                Messages.Promotion_RunnerImpl_Promoting(
                    HyperlinkNote.encodeTo('/' + target.getUrl(), target.getFullDisplayName())
                )
            );

            // start with SUCCESS, unless someone makes it a failure
            setResult(Result.SUCCESS);

            if(!preBuild(listener,project.getBuildSteps()))
                return Result.FAILURE;

            if(!build(listener,project.getBuildSteps(),target))
                return Result.FAILURE;

            return null;
        }

        protected void post2(BuildListener listener) throws Exception {
            if(getResult()== Result.SUCCESS)
                getStatus().onSuccessfulPromotion(Promotion.this);
            // persist the updated build record
            getTarget().save();

            if (getResult() == Result.SUCCESS) {
                // we should evaluate any other pending promotions in case
                // they had a condition on this promotion
                PromotedBuildAction pba = getTarget().getAction(PromotedBuildAction.class);
                for (PromotionProcess pp : pba.getPendingPromotions()) {
                    pp.considerPromotion2(getTarget());
                }

                // tickle PromotionTriggers
                for (AbstractProject<?,?> p : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
                    PromotionTrigger pt = p.getTrigger(PromotionTrigger.class);
                    if (pt!=null)
                        pt.consider(Promotion.this);
                }
            }
        }

        private boolean build(final BuildListener listener,
                              final List<BuildStep> steps,
                              final Run promotedBuild)
            throws IOException, InterruptedException
        {
            for( BuildStep bs : steps ) {
                if ( bs instanceof BuildTrigger) {
                    BuildTrigger bt = (BuildTrigger)bs;
                    for(AbstractProject p : bt.getChildProjects()) {
                        listener.getLogger().println(
                            Messages.Promotion_RunnerImpl_SchedulingBuild(
                                HyperlinkNote.encodeTo('/' + p.getUrl(), p.getDisplayName())
                            )
                        );

                        p.scheduleBuild(0, new PromotionCause(promotionRun, promotedBuild));
                    }
                } else if(!bs.perform(Promotion.this, launcher, listener)) {
                    listener.getLogger().println("failed build " + bs + " " + getResult());
                    return false;
                } else  {
                    listener.getLogger().println("build " + bs + " " + getResult());
                }
            }
            return true;
        }

        private boolean preBuild(BuildListener listener, List<BuildStep> steps) {
            for( BuildStep bs : steps ) {
                if(!bs.prebuild(Promotion.this,listener)) {
                    listener.getLogger().println("failed pre build " + bs + " " + getResult());
                    return false;
                }
            }
            return true;
        }
        
    }

    public static final PermissionGroup PERMISSIONS = new PermissionGroup(Promotion.class, Messages._Promotion_Permissions_Title());
    public static final Permission PROMOTE = new Permission(PERMISSIONS, "Promote", Messages._Promotion_PromotePermission_Description(), Jenkins.ADMINISTER, PermissionScope.RUN);

    @Override
    public int compareTo(Promotion that) {
    	return that.getId().compareTo( this.getId() );
    }
    
}
