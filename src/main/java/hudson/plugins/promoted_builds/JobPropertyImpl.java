package hudson.plugins.promoted_builds;

import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Failure;
import hudson.model.Hudson;
import hudson.model.ItemGroup;
import hudson.model.ItemGroupMixIn;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
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
import java.util.logging.Level;
import java.util.logging.Logger;

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

    /**
     * Programmatic construction.
     */
    public JobPropertyImpl(AbstractProject<?,?> owner) throws Descriptor.FormException, IOException {
        this.owner = owner;
        init();
    }

    private JobPropertyImpl(StaplerRequest req, JSONObject json) throws Descriptor.FormException, IOException {
        // a hack to get the owning AbstractProject.
        // this is needed here so that we can load items
        List<Ancestor> ancs = req.getAncestors();
        owner = (AbstractProject)ancs.get(ancs.size()-1).getObject();

        // newer version of Hudson put "promotions". This code makes it work with or without them.
        if(json.has("promotions"))
            json = json.getJSONObject("promotions");

        for( Object o : JSONArray.fromObject(json.get("activeItems")) ) {
            JSONObject c = (JSONObject)o;
            String name = c.getString("name");
            try {
                Hudson.checkGoodName(name);
            } catch (Failure f) {
                throw new Descriptor.FormException(f.getMessage(), name);
            }
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
        init();
    }

    private void init() throws IOException {
        // load inactive processes
        File[] subdirs = getRootDir().listFiles(new FileFilter() {
            public boolean accept(File child) {
                return child.isDirectory() && !isActiveProcessNameIgnoreCase(child.getName());
            }
        });
        if(subdirs!=null) {
            for (File subdir : subdirs) {
                try {
                    PromotionProcess p = (PromotionProcess) Items.load(this, subdir);
                    processes.add(p);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to load promotion process in "+subdir,e);
                }
            }
        }

        buildActiveProcess();
    }

    /**
     * Adds a new promotion process of the given name.
     */
    public PromotionProcess addProcess(String name) throws IOException {
        PromotionProcess p = new PromotionProcess(this, name);
        activeProcessNames.add(name);
        processes.add(p);
        buildActiveProcess();
        return p;
    }

    @Override
    protected void setOwner(AbstractProject<?,?> owner) {
        super.setOwner(owner);

        // readResolve is too early because we don't have our parent set yet,
        // so use this as the initialization opportunity.
        // CopyListener is also using setOwner to re-init after copying config from another job.
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
            boolean active = isActiveProcessNameIgnoreCase(p.getName());
            p.makeDisabled(!active);
            if(active)
                activeProcesses.add(p);

            // ensure that the name casing matches what's given in the activeProcessName
            // this is because in case insensitive file system, we may end up resolving
            // to a directory name that differs only in their case.
            p.renameTo(getActiveProcessName(p.getName()));
        }
    }

    /**
     * Return the string in the case as specified in {@link #activeProcessNames}.
     */
    private String getActiveProcessName(String s) {
        for (String n : activeProcessNames) {
            if (n.equalsIgnoreCase(s))
                return n;
        }
        return s;   // huh?
    }

    private boolean isActiveProcessNameIgnoreCase(String s) {
        for (String n : activeProcessNames)
            if (n.equalsIgnoreCase(s))
                return true;
        return false;
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

    public void save() throws IOException {
        // there's nothing to save, actually
    }

    public void onDeleted(PromotionProcess process) {
        // TODO delete the persisted directory?
    }

    public void onRenamed(PromotionProcess item, String oldName, String newName) throws IOException {
        // TODO should delete the persisted directory?
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

    File getRootDirFor(String name) {
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

    @Deprecated
    public Action getJobAction(AbstractProject<?,?> job) {
        return new PromotedProjectAction(job,this);
    }

    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {
        public String getDisplayName() {
            return "Promote Builds When...";
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return AbstractProject.class.isAssignableFrom(jobType);
        }

        @Override
        public JobPropertyImpl newInstance(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
            try {
                if(json.has("promotions"))
                    return new JobPropertyImpl(req, json);
                return null;
            } catch (IOException e) {
                throw new FormException("Failed to create",e,null); // TODO:hmm
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(JobPropertyImpl.class.getName());
}
