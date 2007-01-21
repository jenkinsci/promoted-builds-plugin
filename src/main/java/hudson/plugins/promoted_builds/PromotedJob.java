package hudson.plugins.promoted_builds;

import hudson.model.Items;
import hudson.model.Job;
import hudson.model.RunMap;
import hudson.model.RunMap.Constructor;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Hudson;

import java.io.File;
import java.io.IOException;
import java.util.SortedMap;
import java.util.logging.Logger;

/**
 * {@link Job} used for remembering key builds of another {@link Job}
 * (AKA promoted builds.)
 *
 */
public class PromotedJob extends Job<PromotedJob, PromotedBuild> implements TopLevelItem {

    /**
     * All the promoted builds keyed by their build number.
     */
    private transient /*almost final*/ RunMap<PromotedBuild> builds = new RunMap<PromotedBuild>();

    public PromotedJob(String name) {
        super(Hudson.getInstance(),name);
    }

    @Override
    public Hudson getParent() {
        return (Hudson)super.getParent();
    }

    @Override
    public void onLoad(ItemGroup<? extends Item> parent, String name) throws IOException {
        this.builds = new RunMap<PromotedBuild>();
        this.builds.load(this,new Constructor<PromotedBuild>() {
            public PromotedBuild create(File dir) throws IOException {
                return new PromotedBuild(PromotedJob.this,dir);
            }
        });
    }

    public boolean isBuildable() {
        // I guess it doesn't make sense to map a "build" to the promotion,
        // as you rarely want to promote the last CI build.
        return false;
    }

    protected SortedMap<Integer, PromotedBuild> _getRuns() {
        return builds.getView();
    }

    protected void removeRun(PromotedBuild run) {
        builds.remove(run);
    }

    public TopLevelItemDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    static final TopLevelItemDescriptor DESCRIPTOR = new TopLevelItemDescriptor(PromotedJob.class) {
        public String getDisplayName() {
            return "Managing promotion of another job";
        }

        public PromotedJob newInstance(String name) {
            return new PromotedJob(name);
        }
    };

    static {
        Items.XSTREAM.alias("promotedJob",PromotedJob.class);
    }

    private static final Logger logger = Logger.getLogger(PromotedJob.class.getName());
}
