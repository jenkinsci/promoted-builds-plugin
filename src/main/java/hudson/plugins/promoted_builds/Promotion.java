package hudson.plugins.promoted_builds;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.slaves.WorkspaceList;
import hudson.slaves.WorkspaceList.Lease;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepCompatibilityLayer;
import hudson.tasks.BuildTrigger;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

/**
 * Records a promotion process.
 *
 * @author Kohsuke Kawaguchi
 */
public class Promotion extends AbstractBuild<PromotionProcess,Promotion> {
    /**
     * The build number of the project that this promotion promoted.
     * @see #getTarget()
     */
    private int targetBuildNumber;

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
     */
    public AbstractBuild<?,?> getTarget() {
        return getParent().getOwner().getBuildByNumber(targetBuildNumber);
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
        String rootUrl = Hudson.getInstance().getRootUrl();
        if(rootUrl!=null)
            e.put("PROMOTED_URL",rootUrl+getTarget().getUrl());
        return e;
    }

    public void run() {
        run(new RunnerImpl());
    }

    protected class RunnerImpl extends AbstractRunner {
        private AbstractBuild<?,?> getTarget() {
            PromotionTargetAction pta = getAction(PromotionTargetAction.class);
            return pta.resolve();
        }

        @Override
        protected Lease decideWorkspace(Node n, WorkspaceList wsl) throws InterruptedException, IOException {
            return wsl.acquire(n.getWorkspaceFor((TopLevelItem)getTarget().getProject()),true);
        }

        protected Result doRun(BuildListener listener) throws Exception {
            AbstractBuild<?, ?> target = getTarget();
            targetBuildNumber = target.getNumber();

            listener.getLogger().println("Promoting "+target);

            getStatus().addPromotionAttempt(Promotion.this);

            // start with SUCCESS, unless someone makes it a failure
            setResult(Result.SUCCESS);

            if(!preBuild(listener,project.getBuildSteps()))
                return Result.FAILURE;

            if(!build(listener,project.getBuildSteps()))
                return Result.FAILURE;

            return null;
        }

        protected void post2(BuildListener listener) throws Exception {
            if(getResult()== Result.SUCCESS)
                getStatus().onSuccessfulPromotion(Promotion.this);
            // persist the updated build record
            getTarget().save();
        }

        private boolean build(BuildListener listener, List<BuildStep> steps) throws IOException, InterruptedException {
            for( BuildStep bs : steps ) {
                if ( bs instanceof BuildTrigger) {
                    BuildTrigger bt = (BuildTrigger)bs;
                    for(AbstractProject p : bt.getChildProjects()) {
                        listener.getLogger().println("  scheduling build for " + p.getDisplayName());
                        p.scheduleBuild();
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
            boolean allOk = true;
            for( BuildStep bs : steps ) {
                if(bs instanceof BuildStepCompatibilityLayer && ! overridesPerform(bs.getClass())) {
                    listener.getLogger().println(bs + " doesn't support Promotion");
                    allOk = false;
                } else if(!bs.prebuild(Promotion.this,listener)) {
                    listener.getLogger().println("failed pre build " + bs + " " + getResult());
                    return false;
                }
            }
            return allOk;
        }
        
        private boolean overridesPerform(Class<? extends BuildStep> bsc) {
           try {
                Class<?> declarer = bsc.getMethod("perform", AbstractBuild.class, Launcher.class, BuildListener.class).getDeclaringClass();
                return ! declarer.equals(BuildStepCompatibilityLayer.class);
            } catch (NoSuchMethodException noSuchMethodException) {
                return false;
            } catch (SecurityException securityException) {
                throw new RuntimeException(securityException);
            }
        }
    }

    //public static final PermissionGroup PERMISSIONS = new PermissionGroup(Promotion.class, Messages._Promotion_Permissions_Title());
    //public static final Permission PROMOTE = new Permission(PERMISSIONS, "Promote", Messages._Promotion_PromotePermission_Description(), Hudson.ADMINISTER);
    public static final PermissionGroup PERMISSIONS = new PermissionGroup(Promotion.class, null);
    public static final Permission PROMOTE = new Permission(PERMISSIONS, "Promote", null, Hudson.ADMINISTER);
}
