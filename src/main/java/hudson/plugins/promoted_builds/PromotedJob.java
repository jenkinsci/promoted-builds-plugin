package hudson.plugins.promoted_builds;

import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.model.JobDescriptor;
import hudson.model.RunMap;
import hudson.model.RunMap.Constructor;

import java.io.File;
import java.io.IOException;
import java.util.SortedMap;
import java.util.logging.Logger;

/**
 * {@link Job} used for remembering key builds of another {@link Job}
 * (AKA promoted builds.)
 *
 */
public class PromotedJob extends Job<PromotedJob, PromotedBuild> {

    /**
     * All the promoted builds keyed by their build number.
     */
    private transient /*almost final*/ RunMap<PromotedBuild> builds = new RunMap<PromotedBuild>();

    public PromotedJob(Hudson parent, String name) {
        super(parent, name);
    }


    protected void onLoad(Hudson root, String name) throws IOException {
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

    public Descriptor<Job<PromotedJob, PromotedBuild>> getDescriptor() {
        return DESCRIPTOR;
    }

    static final JobDescriptor<PromotedJob, PromotedBuild> DESCRIPTOR = new JobDescriptor<PromotedJob, PromotedBuild>(PromotedJob.class) {
        public String getDisplayName() {
            return "Managing promotion of another job";
        }

        public PromotedJob newInstance(String name) {
            return new PromotedJob(Hudson.getInstance(),name);
        }
    };

    static {
        Job.XSTREAM.alias("promotedJob",PromotedJob.class);
    }

    private static final Logger logger = Logger.getLogger(PromotedJob.class.getName());
}
