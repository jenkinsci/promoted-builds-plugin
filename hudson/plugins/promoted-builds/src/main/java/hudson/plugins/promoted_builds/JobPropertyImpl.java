package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;
import hudson.model.ItemGroupMixIn;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.tasks.BuildStep;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Promotion processes defined for a project.
 *
 * <p>
 * TODO: a possible performance problem as every time the owner job is reconfigured,
 * all the promotion processes get reloaded from the disk.
 *
 * @author Kohsuke Kawaguchi
 */
public final class JobPropertyImpl extends JobProperty<AbstractProject<?,?>> implements ItemGroup<PromotionProcess> {
    /**
     * These are loaded from the disk in a different way.
     */
    private transient /*final*/ List<PromotionProcess> processes = new ArrayList<PromotionProcess>();

    /**
     * Subset of {@link #processes} that only contains {@link #activeProcessNames processes that are active}.
     * This is really just a cache and not an independent variable.
     */
    private transient /*final*/ List<PromotionProcess> activeProcesses;

    /**
     * These {@link PromotionProcess}es are active.
     */
    private final Set<String> activeProcessNames = new HashSet<String>();

//    /**
//     * Names of the processes that are configured.
//     * Used to construct {@link #processes}.
//     */
//    private final List<String> names = new ArrayList<String>();
    
    private JobPropertyImpl(StaplerRequest req, JSONObject json) throws Descriptor.FormException, IOException {
        // a hack to get the owning AbstractProject.
        // this is needed here so that we can load items
        List<Ancestor> ancs = req.getAncestors();
        owner = (AbstractProject)ancs.get(ancs.size()-1).getObject();

        for( JSONObject c : (List<JSONObject>) JSONArray.fromObject(json.get("config")) ) {
            String name = c.getString("name");
            activeProcessNames.add(name);
            PromotionProcess p;
            try {
                p = (PromotionProcess) Items.load(this, getRootDirFor(name));
            } catch (IOException e) {
                // failed to load
                p = new PromotionProcess(this,name);
            }

            // apply configuration
            p.configure(req,c);
            processes.add(p);
        }

        // load inactive processes
        File[] subdirs = getRootDir().listFiles(new FileFilter() {
            public boolean accept(File child) {
                return child.isDirectory() && !activeProcessNames.contains(child.getName());
            }
        });
        if(subdirs!=null) {
            for (File subdir : subdirs) {
                PromotionProcess p = (PromotionProcess) Items.load(this, subdir);
                processes.add(p);
            }
        }

        buildActiveProcess();
    }

    protected void setOwner(AbstractProject<?,?> owner) {
        super.setOwner(owner);

        // readResolve is too early because we don't have our parent set yet,
        // so use this as the initialization opportunity.
        processes = new ArrayList<PromotionProcess>(ItemGroupMixIn.<String,PromotionProcess>loadChildren(
            this,getRootDir(),ItemGroupMixIn.KEYED_BY_NAME).values());
        try {
            buildActiveProcess();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    /**
     * Builds {@link #activeProcesses}.
     */
    private void buildActiveProcess() throws IOException {
        activeProcesses = new ArrayList<PromotionProcess>();
        for (PromotionProcess p : processes) {
            boolean active = activeProcessNames.contains(p.getName());
            p.makeDisabled(!active);
            if(active)
                activeProcesses.add(p);
        }
    }

    /**
     * Gets the list of promotion processes defined for this project,
     * including ones that are no longer actively used and only
     * for archival purpose.
     *
     * @return
     *      non-null and non-empty. Read-only.
     */
    public List<PromotionProcess> getItems() {
        return processes;
    }

    /**
     * Gets the list of active promotion processes.
     */
    public List<PromotionProcess> getActiveItems() {
        return activeProcesses;
    }

    /**
     * Gets {@link AbstractProject} that contains us.
     */
    public AbstractProject<?,?> getOwner() {
        return owner;
    }

    /**
     * Finds a config by name.
     */
    public PromotionProcess getItem(String name) {
        for (PromotionProcess c : processes) {
            if(c.getName().equals(name))
                return c;
        }
        return null;
    }

    public File getRootDir() {
        return new File(getOwner().getRootDir(),"promotions");
    }

    public String getUrl() {
        return getOwner().getUrl()+"promotion/";
    }

    public String getFullName() {
        return getOwner().getFullName()+"/promotion";
    }

    public String getFullDisplayName() {
        return getOwner().getFullDisplayName()+" \u00BB promotion";
    }

    public String getUrlChildPrefix() {
        return "";
    }

    public File getRootDirFor(PromotionProcess child) {
        return getRootDirFor(child.getName());
    }

    private File getRootDirFor(String name) {
        return new File(getRootDir(), name);
    }

    public String getDisplayName() {
        return "promotion";
    }

    @Override
    public boolean prebuild(AbstractBuild<?,?> build, BuildListener listener) {
        build.addAction(new PromotedBuildAction(build));
        return true;
    }

    @Override
    public Action getJobAction(AbstractProject<?,?> job) {
        return new PromotedProjectAction(job,this);
    }

    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.INSTANCE;
    }

    public static final class DescriptorImpl extends JobPropertyDescriptor {
        private DescriptorImpl() {
            super(JobPropertyImpl.class);
        }

        public String getDisplayName() {
            return "Promote Builds When...";
        }

        public boolean isApplicable(Class<? extends Job> jobType) {
            return AbstractProject.class.isAssignableFrom(jobType);
        }

        public JobPropertyImpl newInstance(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
            try {
                if(json.has("promotions"))
                    return new JobPropertyImpl(req, json);
                return null;
            } catch (IOException e) {
                throw new FormException("Failed to create",e,null); // TODO:hmm
            }
        }

        // exposed for Jelly
        public List<PromotionConditionDescriptor> getApplicableConditions(AbstractProject<?,?> p) {
            return PromotionConditions.getApplicableTriggers(p);
        }

        // exposed for Jelly
        public List<Descriptor<? extends BuildStep>> getApplicableBuildSteps(AbstractProject<?,?> p) {
            return PromotionProcess.getAll();
        }

        // exposed for Jelly
        public final Class<PromotionProcess> promotionProcessType = PromotionProcess.class;

        public static final DescriptorImpl INSTANCE = new DescriptorImpl();
    }
}
