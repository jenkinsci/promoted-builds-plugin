package hudson.plugins.promoted_builds;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.console.ConsoleLogFilter;
import hudson.console.HyperlinkNote;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.StreamBuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause.UserCause;
import hudson.model.Cause.UserIdCause;
import hudson.model.Environment;
import hudson.model.Node;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParameterValue;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.model.Run;
import hudson.model.User;
import hudson.plugins.promoted_builds.conditions.ManualCondition;
import hudson.plugins.promoted_builds.util.JenkinsHelper;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.PermissionScope;
import hudson.slaves.WorkspaceList;
import hudson.slaves.WorkspaceList.Lease;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildTrigger;

import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.export.Exported;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Records a promotion process.
 *
 * @author Kohsuke Kawaguchi
 */
public class Promotion extends AbstractBuild<PromotionProcess,Promotion> {

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
    @Exported
    public AbstractBuild<?,?> getTarget() {
        PromotionTargetAction pta = getAction(PromotionTargetAction.class);
        return pta == null ? null : pta.resolve(this);
    }

    @Override public AbstractBuild<?,?> getRootBuild() {
        return getTarget().getRootBuild();
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
        String rootUrl = JenkinsHelper.getInstance().getRootUrl();
        AbstractBuild<?, ?> target = getTarget();
        if(rootUrl!=null)
            e.put("PROMOTED_URL",rootUrl+target.getUrl());
        e.put("PROMOTED_JOB_NAME", target.getParent().getName());
        e.put("PROMOTED_JOB_FULL_NAME", target.getParent().getFullName());
        e.put("PROMOTED_NUMBER", Integer.toString(target.getNumber()));
        e.put("PROMOTED_ID", target.getId());
        GlobalBuildPromotedBuilds globalBuildPromotedBuilds = GlobalBuildPromotedBuilds.get();
        String dateFormat = globalBuildPromotedBuilds.getDateFormat();
        String timeZone = globalBuildPromotedBuilds.getTimeZone();
        SimpleDateFormat format = null;
        TimeZone tz = null;

        if (dateFormat != null && !StringUtils.isBlank(dateFormat)) {
            try {
                format = new SimpleDateFormat(dateFormat);
            } catch (IllegalArgumentException e1) {
                LOGGER.log(Level.WARNING, String.format("An illegal date format was introduced: %s. Default ISO 8601 yyyy-MM-dd'T'HH:mmZ will be used", dateFormat), e1);
                format =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
            }
        } else {
            // Per ISO 8601
            format =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
        }

        if (timeZone !=null && !StringUtils.isBlank(timeZone)) {
            try {
                tz = TimeZone.getTimeZone(timeZone);
            } catch (IllegalArgumentException e2) {
                LOGGER.log(Level.WARNING, String.format("An illegal time zone was introduced: %s. Default GMT time zone will be used", timeZone), e2);
                tz = TimeZone.getTimeZone("GMT");
            }
        } else {
            tz = TimeZone.getTimeZone("GMT");
        }

        format.setTimeZone(tz);
        e.put("PROMOTED_TIMESTAMP", format.format(new Date()));
        e.put("PROMOTED_DISPLAY_NAME", target.getDisplayName());
        e.put("PROMOTED_USER_NAME", getUserName());
        e.put("PROMOTED_USER_ID", getUserId());
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
    
    
    /**
     * Get a user name of the person, who triggered the promotion.
     * The method tries various sources like {@link UserIdCause} or {@link ManualCondition.Badge}.
     * @return user's name who triggered the promotion, or 'anonymous' if the search fails
     */
    @Nonnull
    public String getUserName() {
        // Deprecated, but we still want to support it in order to maintain the compatiibility
        final UserCause userCause = getCause(UserCause.class);
        final String nameFromUserCause = userCause != null ? userCause.getUserName() : null;
        if (nameFromUserCause != null) {
            return nameFromUserCause;
        }

        // Modern UserIdCause
        final UserIdCause userIdCause = getCause(UserIdCause.class);
        final String nameFromUserIdCause = userIdCause != null ? userIdCause.getUserName() : null;
        if (nameFromUserIdCause != null) {
            return nameFromUserIdCause;
        }

        //fallback to badge lookup for compatibility 
        for (PromotionBadge badget : getStatus().getBadges()) {
            if (badget instanceof ManualCondition.Badge) {
                final String nameFromBadge = ((ManualCondition.Badge) badget).getUserName();
                if (!nameFromBadge.equals(ManualCondition.MISSING_USER_ID_DISPLAY_STRING)) {
                    return nameFromBadge;
                }
            }
        }
        return Jenkins.ANONYMOUS.getName();
    }
    

    /**
     * Gets ID of the {@link User}, who triggered the promotion.
     * The method tries various sources like {@link UserIdCause} or {@link ManualCondition.Badge}.
     * @return ID of the user who triggered the promotion.
     *         If the search fails, returns ID of {@link User#getUnknown()}.
     * @since 2.22 
     */
    @Nonnull
    public String getUserId() {
        // Deprecated, but we still want to support it in order to maintain the compatiibility
        // We try to convert the cause to the user ID by using a search by the full name, not reliable
        final UserCause userCause = getCause(UserCause.class);
        final String nameFromUserCause = userCause != null ? userCause.getUserName(): null;
        final User user = nameFromUserCause != null ? User.get(nameFromUserCause, false, null) : null;
        if (user != null) {
            return user.getId();
        }

        // Modern UserIdCause
        final UserIdCause userIdCause = getCause(UserIdCause.class);
        final String idFromUserIdCause = userIdCause != null ? userIdCause.getUserId(): null;
        if (idFromUserIdCause != null) {
            return idFromUserIdCause;
        }

        //fallback to badge lookup for compatibility 
        for (PromotionBadge badget : getStatus().getBadges()) {
            if (badget instanceof ManualCondition.Badge) {
                final String idFromBadge = ((ManualCondition.Badge) badget).getUserId();
                if (!idFromBadge.equals(ManualCondition.MISSING_USER_ID_DISPLAY_STRING)) {
                    return idFromBadge;
                }
            }
        }
        return User.getUnknown().getId();
    }

    public List<ParameterValue> getParameterValues(){
      List<ParameterValue> values=new ArrayList<ParameterValue>(); 
      ParametersAction parametersAction=getParametersActions(this);
      if (parametersAction!=null){
        ManualCondition manualCondition=(ManualCondition) getProject().getPromotionCondition(ManualCondition.class.getName());
        if (manualCondition!=null){
          for (ParameterValue pvalue:parametersAction.getParameters()){
            if (manualCondition.getParameterDefinition(pvalue.getName())!=null){
              values.add(pvalue);
            }
          }
        }
        return values;
      }
      
      //fallback to badge lookup for compatibility 
      for (PromotionBadge badget:getStatus().getBadges()){
        if (badget instanceof ManualCondition.Badge){
          return ((ManualCondition.Badge) badget).getParameterValues();
        }
      }
      return Collections.emptyList();
    }
    
    /**
     * Gets parameter definitions from the {@link ManualCondition}.
     * @return List of parameter definitions to be presented.
     *         May be empty if there is no {@link ManualCondition}.
     */
    @Nonnull
    public List<ParameterDefinition> getParameterDefinitionsWithValue(){
    	List<ParameterDefinition> definitions=new ArrayList<ParameterDefinition>();
    	ManualCondition manualCondition=(ManualCondition) getProject().getPromotionCondition(ManualCondition.class.getName());
    	if (manualCondition == null) {
            return definitions;
        }
        
        for (ParameterValue pvalue:getParameterValues()){
    		ParameterDefinition pdef=manualCondition.getParameterDefinition(pvalue.getName());
                if (pdef == null) {
                    // We cannot do anything with such missing definitions.
                    // May happen only in the case of the wrong form submission
                    continue;
                }
    		definitions.add(pdef.copyWithDefaultValue(pvalue));
    	}
    	return definitions;
    }

    public void doRebuild(StaplerRequest req, StaplerResponse rsp) throws IOException {
        throw HttpResponses.error(404, "Promotions may not be rebuilt directly");
    }

    public void run() {
        if (getTarget() != null) {
            getStatus().addPromotionAttempt(this);
        }
        run(new RunnerImpl(this));
    }

    protected class RunnerImpl extends AbstractRunner {
        final Promotion promotionRun;
        
        RunnerImpl(final Promotion promotionRun) {
            this.promotionRun = promotionRun;
        }
        
        @Override
        protected Lease decideWorkspace(Node n, WorkspaceList wsl) throws InterruptedException, IOException {
            if (getTarget() == null) {
                throw new IOException("No Promotion target, cannot retrieve workspace");
            }
            String customWorkspace = Promotion.this.getProject().getCustomWorkspace();
            if (customWorkspace != null) {
                final FilePath rootPath = n.getRootPath();
                if (rootPath == null) {
                    throw new IOException("Cannot retrieve the root path of the node " + n);
                }
                // we allow custom workspaces to be concurrently used between jobs.
                return Lease.createDummyLease(
                        rootPath.child(getEnvironment(listener).expand(customWorkspace)));
            }
            
            TopLevelItem item = (TopLevelItem)getTarget().getProject();
            FilePath workspace = n.getWorkspaceFor(item);
            if (workspace == null) {
                throw new IOException("Cannot retrieve workspace for " + item + " on the node " + n);
            }
            return wsl.allocate(workspace, promotionRun);
        }

        protected Result doRun(BuildListener listener) throws Exception {
            AbstractBuild<?, ?> target = getTarget();

            OutputStream logger = listener.getLogger();
            AbstractProject rootProject = project.getRootProject();
            // Global log filters
            for (ConsoleLogFilter filter : ConsoleLogFilter.all()) {
                logger = filter.decorateLogger(target, logger);
            }

            // Project specific log filters
            if (rootProject instanceof BuildableItemWithBuildWrappers) {
                BuildableItemWithBuildWrappers biwbw = (BuildableItemWithBuildWrappers) rootProject;
                for (BuildWrapper bw : biwbw.getBuildWrappersList()) {
                    logger = bw.decorateLogger(target, logger);
                }
            }
            listener = new StreamBuildListener(logger);

            listener.getLogger().println(
                Messages.Promotion_RunnerImpl_Promoting(
                    HyperlinkNote.encodeTo('/' + target.getUrl(), target.getFullDisplayName())
                )
            );

            // start with SUCCESS, unless someone makes it a failure
            setResult(Result.SUCCESS);

            if(!preBuild(listener,project.getBuildSteps()))
                return Result.FAILURE;
            
            try {
            	List<ParameterValue> params=getParameterValues();
                
            	if (params!=null){
        	    	for(ParameterValue value : params) {
        	    		BuildWrapper wrapper=value.createBuildWrapper(Promotion.this);
        	    		if (wrapper!=null){
        	    			Environment e = wrapper.setUp(Promotion.this, launcher, listener);
                			if(e==null)
                                return Result.FAILURE;
                			buildEnvironments.add(e);
        	    		}
        	    	}
            	}
	
	            if(!build(listener,project.getBuildSteps(),target))
	                return Result.FAILURE;
	
	            return null;
            } finally {
            	boolean failed = false;
            	
            	for(int i = buildEnvironments.size()-1; i >= 0; i--) {
            		if (!buildEnvironments.get(i).tearDown(Promotion.this,listener)) {
                        failed=true;
                    }
            	}
            	
            	if(failed)
            		return Result.FAILURE;
            }
        }

        protected void post2(BuildListener listener) throws Exception {
            if (getTarget() == null) {
                listener.error("No Promotion target, cannot save target or update status");
                return;
            }

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
                for (AbstractProject<?,?> p : JenkinsHelper.getInstance().getAllItems(AbstractProject.class)) {
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
    public int hashCode() {
        return this.getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Promotion other = (Promotion) obj;
        return this.getId().equals(other.getId());
    }
    
    
    
    /**
     * Factory method for creating {@link ParametersAction}
     * @param parameters
     * @return
     */
    public static ParametersAction createParametersAction(List<ParameterValue> parameters){
    	return new ParametersAction(parameters);
    }
    public static ParametersAction getParametersActions(Promotion build){
    	return build.getAction(ParametersAction.class);
    }

    /**
     * Combine the target build parameters with the promotion build parameters
     * @param actions
     * @param build
     * @param promotionParams
     * @deprecated Use {@link PromotionParametersAction} with constructor instead.
     */
    @Deprecated
    public static void buildParametersAction(@Nonnull List<Action> actions,
            @Nonnull AbstractBuild<?, ?> build,
            @CheckForNull List<ParameterValue> promotionParams) {
        // Create list of actions to pass to scheduled build
        actions.add(PromotionParametersAction.buildFor(build, promotionParams));
    }

    private static final Logger LOGGER = Logger.getLogger(Promotion.class.getName());
    
    /**
     * Action, which stores promotion parameters.
     * This class allows defining custom parameters filtering logic, which is
     * important for versions after the SECURITY-170 fix.
     * @since TODO
     */
    @Restricted(NoExternalUse.class)
    public static class PromotionParametersAction extends ParametersAction {
        
        private List<ParameterValue> unfilteredParameters;
        
        private PromotionParametersAction(List<ParameterValue> params) {
            // Pass the parameters upstairs
            super(params);   
            unfilteredParameters = params;
        }

        @Override
        public List<ParameterValue> getParameters() {
            return Collections.unmodifiableList(filter(unfilteredParameters));
        }
        
        private List<ParameterValue> filter(List<ParameterValue> params) {
            // buildToBePromoted::getParameters() invokes the secured method, hence all
            // parameters from the promoted build are safe.
            return params;
        }
        
        public static PromotionParametersAction buildFor(
                @Nonnull AbstractBuild<?, ?> buildToBePromoted,
                @CheckForNull List<ParameterValue> promotionParams) {
            if (promotionParams == null) {
                promotionParams = new ArrayList<ParameterValue>();
            }

            List<ParameterValue> params = new ArrayList<ParameterValue>();

            //Add the target build parameters first, if the same parameter is not being provided by the promotion build
            List<ParametersAction> parameters = buildToBePromoted.getActions(ParametersAction.class);
            for (ParametersAction paramAction : parameters) {
                for (ParameterValue pvalue : paramAction.getParameters()) {
                    if (!promotionParams.contains(pvalue)) {
                        params.add(pvalue);
                    }
                }
            }

            //Add all the promotion build parameters
            params.addAll(promotionParams);

            // Create list of actions to pass to scheduled build
            return new PromotionParametersAction(params);
        }
    }
}
