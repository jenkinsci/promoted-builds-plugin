package hudson.plugins.promoted_builds;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Failure;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.ItemGroupMixIn;
import hudson.model.Items;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.listeners.ItemListener;
import hudson.util.IOUtils;
import javax.annotation.CheckForNull;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Promotion processes defined for a project.
 *
 * <p>
 * TODO: a possible performance problem as every time the owner job is reconfigured,
 * all the promotion processes get reloaded from the disk.
 * </p>
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
    /**
     * Programmatic construction.
     * @param owner owner job
     */
    public JobPropertyImpl(AbstractProject<?,?> owner) throws Descriptor.FormException, IOException {
        this.owner = owner;
        init();
    }
    /**
     * Programmatic construction.
     * @param other Property to be copied
     * @param owner owner job
     */
    public JobPropertyImpl(JobPropertyImpl other, AbstractProject<?,?> owner) throws Descriptor.FormException, IOException {
        this.owner = owner;
        this.activeProcessNames.addAll(other.activeProcessNames);
        loadAllProcesses(other.getRootDir()); 
    }

    /**
     * Programmatic construction.
     */
    @Restricted(NoExternalUse.class)
    public JobPropertyImpl(Set<String> activeProcessNames) {
        this.activeProcessNames.addAll(activeProcessNames);
    }

    private JobPropertyImpl(StaplerRequest req, JSONObject json) throws Descriptor.FormException, IOException {
        // a hack to get the owning AbstractProject.
        // this is needed here so that we can load items
        List<Ancestor> ancs = req.getAncestors();
        final Object ancestor = ancs.get(ancs.size()-1).getObject();
        if (ancestor instanceof AbstractProject) {
            owner = (AbstractProject)ancestor;
        } else if (ancestor == null) {
            throw new Descriptor.FormException("Cannot retrieve the ancestor item in the request",
            "owner");
        } else {
            throw new Descriptor.FormException("Cannot create Promoted Builds Job Property for " + ancestor.getClass()
            + ". Currently the plugin supports instances of AbstractProject only."
            + ". Other job types are not supported, submit a bug to the plugin, which provides the job type"
            + ". If you use Multi-Branch Project plugin, see https://issues.jenkins-ci.org/browse/JENKINS-32237",
            "owner");
        }
            
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
            safeAddToProcessesList(p);
        }
        init();
    }
    
    private void loadAllProcesses(File rootDir) throws IOException {
        File[] subdirs = rootDir.listFiles(new FileFilter() {
            public boolean accept(File child) {
                return child.isDirectory();
            }
        });

        loadProcesses(subdirs);
    }
    private void init() throws IOException {
        // load inactive processes
        File[] subdirs = getRootDir().listFiles(new FileFilter() {
            public boolean accept(File child) {
                return child.isDirectory() && !isActiveProcessNameIgnoreCase(child.getName());
            }
        });
        loadProcesses(subdirs);
    }
    private void loadProcesses(File[] subdirs) throws IOException {
        if(subdirs!=null) {
            for (File subdir : subdirs) {
                try {
                    PromotionProcess p = (PromotionProcess) Items.load(this, subdir);
                    safeAddToProcessesList(p);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to load promotion process in "+subdir,e);
                }
            }
        }

        buildActiveProcess();
    }

    /**
     * Adds a new promotion process of the given name.
     * @param name Name of the process to be created
     * @return Created process
     * @throws IOException Execution error
     */
    public synchronized PromotionProcess addProcess(String name) throws IOException {
        PromotionProcess p = new PromotionProcess(this, name);
        activeProcessNames.add(name);
        safeAddToProcessesList(p);
        buildActiveProcess();
        p.onCreatedFromScratch();
        return p;
    }

    private synchronized void safeAddToProcessesList(PromotionProcess p) {
        int index = 0;
        boolean found = false;
        for (ListIterator<PromotionProcess> i = processes.listIterator(); i.hasNext();) {
            PromotionProcess process = i.next();
            if (p.getName().equalsIgnoreCase(process.getName())) {
                found = true;
                try {
                    i.set(p);
                    break;
                } catch (UnsupportedOperationException e) {
                    // shouldn't end up here but Java Runtime Spec allows for this case
                    // we don't care about ConcurrentModificationException because we are done
                    // with the iterator once we find the first element.
                    processes.set(index, p);
                    break;
                }
            }
            index++;
        }
        if (!found) {
            processes.add(p);
        }
    }

    @Override
    protected void setOwner(AbstractProject<?,?> owner) {
        super.setOwner(owner);

        // readResolve is too early because we don't have our parent set yet,
        // so use this as the initialization opportunity.
        // CopyListener is also using setOwner to re-init after copying config from another job.
        synchronized (this) {
            processes = new ArrayList<PromotionProcess>(ItemGroupMixIn.<String, PromotionProcess>loadChildren(
                    this, getRootDir(), ItemGroupMixIn.KEYED_BY_NAME).values());
            try {
                buildActiveProcess();
            } catch (IOException e) {
                throw new Error(e);
            }
        }
    }

    /**
     * Builds {@link #activeProcesses}.
     * @throws IOException Execution error
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
            String processName = p.getName();
            String activeProcessName = getActiveProcessName(processName);
            if (!activeProcessName.equals(processName)){
                p.renameTo(activeProcessName);
            }
        }
    }

    /**
     * Return the string in the case as specified in {@link #activeProcessNames}.
     */
    private synchronized String getActiveProcessName(String s) {
        for (String n : activeProcessNames) {
            if (n.equalsIgnoreCase(s))
                return n;
        }
        return s;   // huh?
    }

    private synchronized boolean isActiveProcessNameIgnoreCase(String s) {
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
    public synchronized List<PromotionProcess> getItems() {
        return processes;
    }

    /**
     * Gets the list of active promotion processes.
     */
    public List<PromotionProcess> getActiveItems() {
        return activeProcesses;
    }

    /** @see ItemGroupMixIn#createProjectFromXML */
    public PromotionProcess createProcessFromXml(final String name, InputStream xml) throws IOException {
        owner.checkPermission(Item.CONFIGURE); // CREATE is ItemGroup-scoped and owner is not an ItemGroup
        Jenkins.getInstance().getProjectNamingStrategy().checkName(name);
        if (getItem(name) != null) {
            throw new IllegalArgumentException(owner.getDisplayName() + " already contains an item '" + name + "'");
        }
        File configXml = Items.getConfigFile(getRootDirFor(name)).getFile();
        File dir = configXml.getParentFile();
        if (!dir.mkdirs()) {
            throw new IOException("Cannot create directories for "+dir);
        }
        try {
            IOUtils.copy(xml, configXml);
            PromotionProcess result = Items.whileUpdatingByXml(new MasterToSlaveCallable<PromotionProcess, IOException>() {
                @Override public PromotionProcess call() throws IOException {
                    setOwner(owner);
                    return getItem(name);
                }
            });
            if (result == null) {
                throw new IOException("failed to load from " + configXml);
            }
            ItemListener.fireOnCreated(result);
            return result;
        } catch (IOException e) {
            Util.deleteRecursive(dir);
            throw e;
        }
    }

    /**
     * Gets {@link AbstractProject} that contains us.
     * @return Owner project
     */
    public AbstractProject<?,?> getOwner() {
        return owner;
    }

    /**
     * Finds a {@link PromotionProcess} by name.
     * @param name Name of the process
     * @return {@link PromotionProcess} if it can be found.
     */
    @CheckForNull
    public synchronized PromotionProcess getItem(String name) {
        if (processes == null) {
            return null;
        }
        for (PromotionProcess c : processes) {
            if( StringUtils.equals( c.getName(),name))
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
        setOwner(owner);
        ItemListener.fireOnDeleted(process);
    }

    public void onRenamed(PromotionProcess item, String oldName, String newName) throws IOException {
        setOwner(owner);
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

        public DescriptorImpl() {
            super();
        }

        public DescriptorImpl(Class<? extends JobProperty<?>> clazz) {
            super(clazz);
        }

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