package hudson.plugins.promoted_builds;

import hudson.EnvVars;
import hudson.console.HyperlinkNote;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause.UserCause;
import hudson.model.Environment;
import hudson.model.Node;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParameterValue;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.model.Run;
import hudson.plugins.promoted_builds.conditions.ManualCondition;
import hudson.security.Permission;
import hudson.security.PermissionGroup;
import hudson.security.PermissionScope;
import hudson.slaves.WorkspaceList;
import hudson.slaves.WorkspaceList.Lease;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildTrigger;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.export.Exported;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

/**
 * Records a promotion process.
 *
 * @author Kohsuke Kawaguchi
 */
public class Promotion extends AbstractBuild<PromotionProcess,Promotion> 
	implements Comparable<Promotion>{
	
	private String icon;

	public Promotion(PromotionProcess job) throws IOException {
		super(job);
		this.icon = job.icon;
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
        return pta.resolve(this);
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

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
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
        e.put("PROMOTED_JOB_FULL_NAME", target.getParent().getFullName());
        e.put("PROMOTED_NUMBER", Integer.toString(target.getNumber()));
        e.put("PROMOTED_ID", target.getId());
        e.put("PROMOTED_DISPLAY_NAME", target.getDisplayName());
        e.put("PROMOTED_USER_NAME", getUserName());
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
     * 
     * @return user's name who triggered the promotion, or 'anonymous'
     */
    public String getUserName(){
    	UserCause userClause=getCause(UserCause.class);
    	if (userClause!=null && userClause.getUserName()!=null){
    		return userClause.getUserName();
    	}
    	
    	//fallback to badge lookup for compatibility 
    	for (PromotionBadge badget:getStatus().getBadges()){
    		if (badget instanceof ManualCondition.Badge){
    			return ((ManualCondition.Badge) badget).getUserName();
    		}
    	}
    	return "anonymous";
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
    
    public List<ParameterDefinition> getParameterDefinitionsWithValue(){
    	List<ParameterDefinition> definitions=new ArrayList<ParameterDefinition>();
    	ManualCondition manualCondition=(ManualCondition) getProject().getPromotionCondition(ManualCondition.class.getName());
    	for (ParameterValue pvalue:getParameterValues()){
    		ParameterDefinition pdef=manualCondition.getParameterDefinition(pvalue.getName());
    		definitions.add(pdef.copyWithDefaultValue(pvalue));
    	}
    	return definitions;
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
            String customWorkspace = Promotion.this.getProject().getCustomWorkspace();
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
     */
	public static void buildParametersAction(List<Action> actions, AbstractBuild<?, ?> build, List<ParameterValue> promotionParams) {
        if (promotionParams == null) {
            promotionParams = new ArrayList<ParameterValue>();
        }

		List<ParameterValue> params=new ArrayList<ParameterValue>();
		
		//Add the target build parameters first, if the same parameter is not being provided bu the promotion build
        List<ParametersAction> parameters = build.getActions(ParametersAction.class);
        for(ParametersAction paramAction:parameters){
        	for (ParameterValue pvalue:paramAction.getParameters()){
        		if (!promotionParams.contains(pvalue)){
        			params.add(pvalue);
        		}
        	}
        }
        
        //Add all the promotion build parameters
        params.addAll(promotionParams);
        
        // Create list of actions to pass to scheduled build
        actions.add(new ParametersAction(params));
	}
}
