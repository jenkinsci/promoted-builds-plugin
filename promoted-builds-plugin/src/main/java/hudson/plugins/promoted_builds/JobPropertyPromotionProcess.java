package hudson.plugins.promoted_builds;

import hudson.BulkChange;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Failure;
import hudson.model.FreeStyleProject;
import hudson.model.ItemGroup;
import hudson.model.Items;
import hudson.model.JDK;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class JobPropertyPromotionProcess extends PromotionProcess {

    private List<BuildStep> buildSteps = new ArrayList<BuildStep>();

    private volatile DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrappers;
    private static final AtomicReferenceFieldUpdater<JobPropertyPromotionProcess,DescribableList> buildWrappersSetter
            = AtomicReferenceFieldUpdater.newUpdater(JobPropertyPromotionProcess.class,DescribableList.class,"buildWrappers");

    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void addAliases() {
        Items.XSTREAM2.addCompatibilityAlias("hudson.plugins.promoted_builds.PromotionProcess", JobPropertyPromotionProcess.class);
    }

    /*package*/ JobPropertyPromotionProcess(ItemGroup parent, String name) {
        super(parent, name);
    }

    @Override
    public DescribableList<PromotionCondition,PromotionConditionDescriptor> getConditions() {
        return conditions;
    }
    /**
     * Creates unconnected {@link PromotionProcess} instance from the JSON configuration.
     * This is mostly only useful for capturing its configuration in XML format.
     * @param req Request
     * @param o JSON object with source data
     * @throws Descriptor.FormException form submission issue, includes form validation
     * @throws IOException {@link PromotionProcess} creation issue
     * @return Parsed promotion process
     */
    public static JobPropertyPromotionProcess fromJson(StaplerRequest req, JSONObject o) throws Descriptor.FormException, IOException {
        String name = o.getString("name");
        try {
            Jenkins.checkGoodName(name);
        } catch (Failure f) {
            throw new Descriptor.FormException(f.getMessage(), name);
        }
        JobPropertyPromotionProcess p = new JobPropertyPromotionProcess(null,name);
        BulkChange bc = new BulkChange(p);
        try {
            p.configure(req, o); // apply configuration. prevent it from trying to save to disk while we do this
        } finally {
            bc.abort();
        }
        return p;
    }

    /*package*/ void configure(StaplerRequest req, JSONObject c) throws Descriptor.FormException, IOException {
        buildSteps = (List)Descriptor.newInstancesFromHeteroList(
                req, c, "buildStep", (List) PromotionProcess.getAll());
        super.configure(req, c);
    }

    @Override
    public JobPropertyImpl getParent() {
        return (JobPropertyImpl)super.getParent();
    }

    @Override
    public DescribableList<BuildWrapper,Descriptor<BuildWrapper>> getBuildWrappersList() {
        if(buildWrappers == null) {
            buildWrappersSetter.compareAndSet(this,null,new DescribableList<BuildWrapper,Descriptor<BuildWrapper>>(this));
        }
        return buildWrappers;
    }

    @Override
    public List<BuildStep> getBuildSteps() {
        return buildSteps;
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

    /**
     * Gets the owner {@link AbstractProject} that configured {@link JobPropertyImpl} as
     * a job property.
     * @return Current owner project
     */
    public AbstractProject<?,?> getOwner() {
        return getParent().getOwner();
    }

    /**
     * JENKINS-27716: Since 1.585, the promotion must explicitly indicate that
     * it can be disabled. Otherwise, promotions which trigger automatically
     * upon build completion will execute, even if they're archived.
     */
    @Override
    public boolean supportsMakeDisabled() {
        return true;
    }

    // TODO(oleg_nenashev): We cannot change it without breaking binary compatibility. In tests we trust
    @Override
    protected Class<JobPropertyPromotionProcess> getBuildClass() {
        return JobPropertyPromotionProcess.class;
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
}
