package hudson.plugins.promoted_builds;

import antlr.ANTLRException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.BulkChange;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.ParameterValue;
import hudson.model.Cause;
import hudson.model.Cause.UserCause;
import hudson.model.DependencyGraph;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Failure;
import hudson.model.FreeStyleProject;
import hudson.model.ItemGroup;
import hudson.model.JDK;
import hudson.model.Job;
import hudson.model.Label;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.PermalinkProjectAction.Permalink;
import hudson.model.Queue.Item;
import hudson.model.Run;
import hudson.model.Saveable;
import hudson.model.StringParameterValue;
import hudson.model.labels.LabelAtom;
import hudson.model.labels.LabelExpression;
import hudson.plugins.promoted_builds.conditions.ManualCondition.ManualApproval;
import hudson.plugins.promoted_builds.util.JenkinsHelper;
import hudson.security.ACL;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.util.TimeDuration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * A dummy {@link AbstractProject} to carry out promotion operations.
 *
 * @author Kohsuke Kawaguchi
 */
public final class PromotionProcess extends AbstractProject<PromotionProcess,Promotion> implements Saveable, Describable<PromotionProcess> {

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

    /**
     * The label that promotion process can be run on.
     */
    public String assignedLabel;
    /**
     * Tells if this promotion should be hidden.
     */
    public String isVisible;

    private List<BuildStep> buildSteps = new ArrayList<BuildStep>();

    /*package*/ PromotionProcess(JobPropertyImpl property, String name) {
        super(property, name);
    }

    /*package*/ PromotionProcess(ItemGroup parent, String name) {
        super(parent, name);
    }

    /**
     * Creates unconnected {@link PromotionProcess} instance from the JSON configuration.
     * This is mostly only useful for capturing its configuration in XML format.
     * @param req Request
     * @param o JSON object with source data
     * @throws FormException form submission issue, includes form validation
     * @throws IOException {@link PromotionProcess} creation issue
     * @return Parsed promotion process
     */
    public static PromotionProcess fromJson(StaplerRequest req, JSONObject o) throws FormException, IOException {
        String name = o.getString("name");
        try {
            Jenkins.checkGoodName(name);
        } catch (Failure f) {
            throw new Descriptor.FormException(f.getMessage(), name);
        }
        PromotionProcess p = new PromotionProcess(null,name);
        BulkChange bc = new BulkChange(p);
        try {
            p.configure(req, o); // apply configuration. prevent it from trying to save to disk while we do this
        } finally {
            bc.abort();
        }
        return p;
    }

    @Override
    public void doSetName(String name) {
        super.doSetName(name);
    }

    /*package*/ void configure(StaplerRequest req, JSONObject c) throws Descriptor.FormException, IOException {
        // apply configuration
        conditions.rebuild(req,c.optJSONObject("conditions"), PromotionCondition.all());

        buildSteps = (List)Descriptor.newInstancesFromHeteroList(
                req, c, "buildStep", (List) PromotionProcess.getAll());
        icon = c.getString("icon");
        if (c.optBoolean("hasAssignedLabel")) {
            assignedLabel = Util.fixEmptyAndTrim(c.optString("assignedLabelString"));
        } else {
            assignedLabel = null;
        }
        isVisible = c.getString("isVisible");
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
     * @return Current owner project
     */
    public AbstractProject<?,?> getOwner() {
        return getParent().getOwner();
    }

    @Override public ACL getACL() {
        return getOwner().getACL();
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
     * Get the promotion condition by referencing it fully qualified class name
     * @param promotionClassName Class name of {@link Promotion}
     * @return Promotion condition if exists
     */
    @CheckForNull
    public PromotionCondition getPromotionCondition(String promotionClassName) {
        for (PromotionCondition condition : conditions) {
            if (condition.getClass().getName().equals(promotionClassName)) {
                return condition;
            }
        }

        return null;
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

    /**
     * Gets the textual representation of the assigned label as it was entered by the user.
     * @return Assigned label string
     */
    @Override
    public String getAssignedLabelString() {
        if (assignedLabel == null) return null;
        try {
            LabelExpression.parseExpression(assignedLabel);
            return assignedLabel;
        } catch (ANTLRException e) {
            // must be old label or host name that includes whitespace or other unsafe chars
            return LabelAtom.escape(assignedLabel);
        }
    }

    @Override public Label getAssignedLabel() {
        // Really would like to run on the exact node that the promoted build ran on,
        // not just the same label.. but at least this works if job is tied to one node:
        if (assignedLabel == null) return getOwner().getAssignedLabel();

        return JenkinsHelper.getInstance().getLabel(assignedLabel);
    }

    @Override public JDK getJDK() {
        return getOwner().getJDK();
    }

    /**
     * Gets the customWorkspace of the owner project.
     *
     * Support for FreeStyleProject only.
     * @return customWorkspace
     */
    @CheckForNull
    public String getCustomWorkspace() {
        AbstractProject<?, ?> p = getOwner();
        if (p instanceof FreeStyleProject)
            return ((FreeStyleProject) p).getCustomWorkspace();
        return null;
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

    public String getIsVisible(){
    	return isVisible;
    }
    
    public boolean isVisible(){
    	if (isVisible == null) return true;
    	
    	AbstractProject<?, ?> job = getOwner();
    	
    	if (job == null) return true;
    	
    	String expandedIsVisible = isVisible;
    	EnvVars environment = getDefaultParameterValuesAsEnvVars(job);
    	if (environment != null){
    		expandedIsVisible = environment.expand(expandedIsVisible);
    	}
   	
    	if (expandedIsVisible == null){
    		return true;
    	}
    	if (expandedIsVisible.toLowerCase().equals("false")){
    		return false;
    	}
    	return true;
    }
    private static EnvVars getDefaultParameterValuesAsEnvVars(AbstractProject owner) {
    	EnvVars envVars = null;
		ParametersDefinitionProperty parametersDefinitionProperty = (ParametersDefinitionProperty)owner.getProperty(ParametersDefinitionProperty.class);
		if (parametersDefinitionProperty!=null){
			envVars = new EnvVars();
			for (ParameterDefinition parameterDefinition: parametersDefinitionProperty.getParameterDefinitions()){
				ParameterValue defaultParameterValue = parameterDefinition.getDefaultParameterValue();
				if (defaultParameterValue!=null){
					if (defaultParameterValue instanceof StringParameterValue){
						envVars.put(parameterDefinition.getName(), ((StringParameterValue)defaultParameterValue).value);
					}
				}
			}
			EnvVars.resolve(envVars);
		}
		
		return envVars;
    }
    /**
     * Handle compatibility with pre-1.8 configs.
     * 
     * @param sIcon
     *      the name of the icon used by this promotion; if null or empty,
     *      we return the gold icon for compatibility with previous releases
     * @return the icon file name for this promotion
     */
    @Nonnull
    private static String getIcon(@CheckForNull String sIcon) {
        if ((sIcon == null) || sIcon.equals(""))
            return "star-gold";
        else
            return sIcon;
    }

    /**
     * Get the badges of conditions that were passed for this promotion for the build
     * @param build The build to be checked
     * @return List of generated promotion badges
     */
    @Nonnull
    public List<PromotionBadge> getMetQualifications(AbstractBuild<?,?> build) {
        List<PromotionBadge> badges = new ArrayList<PromotionBadge>();
        for (PromotionCondition cond : conditions) {
            PromotionBadge b = cond.isMet(this, build);

            if (b != null)
                badges.add(b);
        }
        return badges;
    }

    /**
     * Get the conditions that have not been met for this promotion for the build
     * @param build Build to be checked
     * @return List of unmet promotion conditions
     */
    @Nonnull
    public List<PromotionCondition> getUnmetConditions(AbstractBuild<?,?> build) {
        List<PromotionCondition> unmetConditions = new ArrayList<PromotionCondition>();

        for (PromotionCondition cond : conditions) {
            if (cond.isMet(this, build) == null)
                unmetConditions.add(cond);
        }

        return unmetConditions;
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
    public Status isMet(AbstractBuild<?,?> build) {
        List<PromotionBadge> badges = new ArrayList<PromotionBadge>();
        for (PromotionCondition cond : conditions) {
            PromotionBadge b = cond.isMet(this, build);
            if(b==null)
                return null;
            badges.add(b);
        }
        return new Status(this,badges);
    }

    /**
     * @deprecated
     *      Use {@link #considerPromotion2(AbstractBuild)}
     */
    @Deprecated
    public boolean considerPromotion(AbstractBuild<?,?> build) throws IOException {
        return considerPromotion2(build)!=null;
    }

    /**
     * Checks if the build is promotable, and if so, promote it.
     *
     * @param build Build to be promoted
     * @return
     *      {@code null} if the build was not promoted, otherwise Future that kicks in when the build is completed.
     * @throws IOException 
     */
    @CheckForNull
    public Future<Promotion> considerPromotion2(AbstractBuild<?, ?> build) throws IOException {
		LOGGER.fine("Considering the promotion of "+build+" via "+getName()+" without parmeters");
		// If the build has manual approvals, use the parameters from it
		List<ParameterValue> params = new ArrayList<ParameterValue>();
		List<ManualApproval> approvals = build.getActions(ManualApproval.class);
		for (ManualApproval approval : approvals) {
			if (approval.name.equals(getName())) {
				LOGGER.fine("Getting parameters from existing manual promotion");
				params = approval.badge.getParameterValues();
				LOGGER.finer("Using paramters: "+params.toString());
			}
		}

		return considerPromotion2(build, params);
	}
	
    @CheckForNull
    public Future<Promotion> considerPromotion2(AbstractBuild<?,?> build, List<ParameterValue> params) throws IOException {
        if (!isActive())
            return null;    // not active

        PromotedBuildAction a = build.getAction(PromotedBuildAction.class);

        // if it's already promoted, no need to do anything.
        if(a!=null && a.contains(this))
            return null;

        LOGGER.fine("Considering the promotion of "+build+" via "+getName()+" with parameters");
        Status qualification = isMet(build);
        if(qualification==null)
            return null; // not this time

        LOGGER.fine("Promotion condition of "+build+" is met: "+qualification);
        Future<Promotion> f = promote2(build, new UserCause(), qualification, params); // TODO: define promotion cause
        if (f==null)
            LOGGER.warning(build+" qualifies for a promotion but the queueing failed.");
        return f;
    }

    public void promote(AbstractBuild<?,?> build, Cause cause, PromotionBadge... badges) throws IOException {
        promote2(build,cause,new Status(this,Arrays.asList(badges)));
    }

    /**
     * @deprecated
     *      Use {@link #promote2(AbstractBuild, Cause, Status)}
     */
    public void promote(AbstractBuild<?,?> build, Cause cause, Status qualification) throws IOException {
        promote2(build,cause,qualification);
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
    public Future<Promotion> promote2(AbstractBuild<?,?> build, Cause cause, Status qualification) throws IOException {
    	return promote2(build, cause, qualification, null);
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
    public Future<Promotion> promote2(AbstractBuild<?,?> build, Cause cause, Status qualification, List<ParameterValue> params) throws IOException {
        PromotedBuildAction a = build.getAction(PromotedBuildAction.class);
        // build is qualified for a promotion.
        if(a!=null) {
            a.add(qualification);
        } else {
            build.addAction(new PromotedBuildAction(build,qualification));
            build.save();
        }

        // schedule promotion activity.
        return scheduleBuild2(build,cause, params);
    }

    /**
     * @deprecated
     *      You need to be using {@link #scheduleBuild(AbstractBuild)}
     */
    @Deprecated
    public boolean scheduleBuild() {
        return super.scheduleBuild();
    }

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
    @Deprecated
    public boolean scheduleBuild(@Nonnull AbstractBuild<?,?> build, @Nonnull Cause cause) {
        return scheduleBuild2(build,cause)!=null;
    }

    /**
     * Schedules the promotion.
     * @param build Target build
     * @param cause Promotion cause
     * @param params Parameters to be passed
     * @return Future result or {@code null} if the promotion cannot be scheduled
     */
    @CheckForNull
    public Future<Promotion> scheduleBuild2(@Nonnull AbstractBuild<?,?> build, 
            Cause cause, @CheckForNull List<ParameterValue> params) {
        List<Action> actions = new ArrayList<Action>();
        actions.add(Promotion.PromotionParametersAction.buildFor(build, params));
        actions.add(new PromotionTargetAction(build));

        // remember what build we are promoting
        return super.scheduleBuild2(0, cause, actions.toArray(new Action[actions.size()]));
    }


    @Override
    public void doBuild(StaplerRequest req, StaplerResponse rsp, @QueryParameter TimeDuration delay) throws IOException, ServletException {
        throw HttpResponses.error(404, "Promotion processes may not be built directly");
    }

    public Future<Promotion> scheduleBuild2(@Nonnull AbstractBuild<?,?> build, @Nonnull Cause cause) {
        return scheduleBuild2(build, cause, null);
    }

    public boolean isInQueue(@Nonnull AbstractBuild<?,?> build) {
        for (Item item : JenkinsHelper.getInstance().getQueue().getItems(this))
            if (item.getAction(PromotionTargetAction.class).resolve(this)==build)
                return true;
        return false;
    }

    //
    // these are dummy implementations to implement abstract methods.
    // need to think about what the implications are.
    //
    @Override
    public boolean isFingerprintConfigured() {
        return false;
    }

    @Override
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

    public Permalink asPermalink() {
        return new Permalink() {
            @Override
            public String getDisplayName() {
                return Messages.PromotionProcess_PermalinkDisplayName(PromotionProcess.this.getDisplayName());
            }

            @Override
            public String getId() {
                return PromotionProcess.this.getName();
            }

            @Override
            public Run<?, ?> resolve(Job<?, ?> job) {
                String id = getId();
                for( Run<?,?> build : job.getBuilds() ) {
                    PromotedBuildAction a = build.getAction(PromotedBuildAction.class);
                    if(a!=null && a.contains(id))
                        return build;
                }
                return null;
            }
        };
    }

    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)JenkinsHelper.getInstance().getDescriptorOrDie(getClass());
    }

    @Override
    public String getShortUrl() {
        // Must be overridden since JobPropertyImpl.getUrlChildPrefix is "" not "process" as you might expect (also see e50f0f5 in 1.519)
        return "process/" + Util.rawEncode(getName()) + '/';
    }

    public boolean isActive() {
        return !isDisabled();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<PromotionProcess> {
        @Override
        public String getDisplayName() {
            return "Promotion Process";
        }

        public FormValidation doCheckLabelString(@QueryParameter String value) {
            if (Util.fixEmpty(value)==null)
                return FormValidation.ok(); // nothing typed yet
            try {
                Label.parseExpression(value);
            } catch (ANTLRException e) {
                return FormValidation.error(e,
                        Messages.JobPropertyImpl_LabelString_InvalidBooleanExpression(e.getMessage()));
            }
            // TODO: if there's an atom in the expression that is empty, report it
            if (JenkinsHelper.getInstance().getLabel(value).isEmpty())
                return FormValidation.warning(Messages.JobPropertyImpl_LabelString_NoMatch());
            return FormValidation.ok();
        }

        public AutoCompletionCandidates doAutoCompleteAssignedLabelString(@QueryParameter String value) {
            AutoCompletionCandidates c = new AutoCompletionCandidates();
            Set<Label> labels = JenkinsHelper.getInstance().getLabels();
            List<String> queries = new AutoCompleteSeeder(value).getSeeds();

            for (String term : queries) {
                for (Label l : labels) {
                    if (l.getName().startsWith(term)) {
                        c.add(l.getName());
                    }
                }
            }
            return c;
        }

        /**
         * Utility class for taking the current input value and computing a list
         * of potential terms to match against the list of defined labels.
         */
        static class AutoCompleteSeeder {

            private String source;

            AutoCompleteSeeder(String source) {
                this.source = source;
            }

            List<String> getSeeds() {
                ArrayList<String> terms = new ArrayList();
                boolean trailingQuote = source.endsWith("\"");
                boolean leadingQuote = source.startsWith("\"");
                boolean trailingSpace = source.endsWith(" ");

                if (trailingQuote || (trailingSpace && !leadingQuote)) {
                    terms.add("");
                } else {
                    if (leadingQuote) {
                        int quote = source.lastIndexOf('"');
                        if (quote == 0) {
                            terms.add(source.substring(1));
                        } else {
                            terms.add("");
                        }
                    } else {
                        int space = source.lastIndexOf(' ');
                        if (space > -1) {
                            terms.add(source.substring(space + 1));
                        } else {
                            terms.add(source);
                        }
                    }
                }

                return terms;
            }
        }

        // exposed for Jelly
        public List<PromotionConditionDescriptor> getApplicableConditions(AbstractProject<?,?> p) {
            return p==null ? PromotionCondition.all() : PromotionCondition.getApplicableTriggers(p);
        }

        public List<PromotionConditionDescriptor> getApplicableConditions(Object context) {
            return PromotionCondition.all();
        }

        // exposed for Jelly
        public List<Descriptor<? extends BuildStep>> getApplicableBuildSteps() {
            return PromotionProcess.getAll();
        }

        @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "exposed for Jelly")
        public final Class<PromotionProcess> promotionProcessType = PromotionProcess.class;

        public FormValidation doCheckName(@QueryParameter String name) {
            name = Util.fixEmptyAndTrim(name);
            if (name == null) {
                return FormValidation.error(Messages.JobPropertyImpl_ValidateRequired());
            }

            try {
                Jenkins.checkGoodName(name);
            } catch (Failure f) {
                return FormValidation.error(f.getMessage());
            }

            return FormValidation.ok();
        }
    }

    private static final Logger LOGGER = Logger.getLogger(PromotionProcess.class.getName());

    public Future<Promotion> considerPromotion2(AbstractBuild<?, ?> build, ManualApproval approval) throws IOException {
        return considerPromotion2(build, approval.badge.getParameterValues());
    }

}
