package hudson.plugins.promoted_builds.conditions;

import hudson.CopyOnWrite;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Fingerprint;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotionBadge;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.PromotionConditionDescriptor;
import hudson.plugins.promoted_builds.PromotionProcess;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link PromotionCondition} that tests if certain downstream projects have passed.
 * 
 * @author Kohsuke Kawaguchi
 */
public class DownstreamPassCondition extends PromotionCondition {
    /**
     * List of downstream jobs that are used as the promotion criteria.
     * 
     * Every job has to have at least one successful build for us to promote a build.
     */
    private final String jobs;

    public DownstreamPassCondition(String jobs) {
        this.jobs = jobs;
    }

    public String getJobs() {
        return jobs;
    }

    @Override
    public PromotionBadge isMet(AbstractBuild<?,?> build) {
        Badge badge = new Badge();

        OUTER:
        for (AbstractProject<?,?> j : getJobList()) {
            for( AbstractBuild<?,?> b : build.getDownstreamBuilds(j) ) {
                if(b.getResult()== Result.SUCCESS) {
                    badge.add(b);
                    continue OUTER;
                }
            }

            // none of the builds of this job passed.
            return null;
        }
        
        return badge;
    }

    public PromotionConditionDescriptor getDescriptor() {
        return DescriptorImpl.INSTANCE;
    }

    /**
     * List of downstream jobs that we need to monitor.
     *
     * @return never null.
     */
    public List<AbstractProject<?,?>> getJobList() {
        List<AbstractProject<?,?>> r = new ArrayList<AbstractProject<?,?>>();
        for (String name : Util.tokenize(jobs,",")) {
            AbstractProject job = Hudson.getInstance().getItemByFullName(name.trim(),AbstractProject.class);
            if(job!=null)   r.add(job);
        }
        return r;
    }

    /**
     * Short-cut for {@code getJobList().contains(job)}.
     */
    public boolean contains(AbstractProject<?,?> job) {
        if(!jobs.contains(job.getFullName()))    return false;   // quick rejection test

        for (String name : Util.tokenize(jobs,",")) {
            if(name.trim().equals(job.getFullName()))
                return true;
        }
        return false;
    }

    public static final class Badge extends PromotionBadge {
        /**
         * Downstream builds that certified this build. Should be considered read-only.
         */
        public final List<Fingerprint.BuildPtr> builds = new ArrayList<Fingerprint.BuildPtr>();

        void add(AbstractBuild<?,?> b) {
           builds.add(new Fingerprint.BuildPtr(b));
        }
    }

    public static final class DescriptorImpl extends PromotionConditionDescriptor {
        public DescriptorImpl() {
            super(DownstreamPassCondition.class);
            new RunListenerImpl().register();
        }

        public boolean isApplicable(AbstractProject<?,?> item) {
            return true;
        }

        public String getDisplayName() {
            return "When the following downstream projects build successfully";
        }

        public String getHelpFile() {
            return "/plugin/promoted-builds/conditions/downstream.html";
        }
        
        public PromotionCondition newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new DownstreamPassCondition(formData.getString("jobs"));
        }

        public static final DescriptorImpl INSTANCE = new DescriptorImpl();
    }

    /**
     * {@link RunListener} to pick up completions of downstream builds.
     *
     * <p>
     * This is a single instance that receives all the events everywhere in the system.
     * @author Kohsuke Kawaguchi
     */
    private static final class RunListenerImpl extends RunListener<AbstractBuild<?,?>> {
        public RunListenerImpl() {
            super((Class)AbstractBuild.class);
        }

        @Override
        public void onCompleted(AbstractBuild<?,?> build, TaskListener listener) {
            // this is not terribly efficient,
            for(AbstractProject<?,?> j : Hudson.getInstance().getAllItems(AbstractProject.class)) {
                JobPropertyImpl jp = j.getProperty(JobPropertyImpl.class);
                if(jp!=null) {
                    for (PromotionProcess p : jp.getItems()) {
                        boolean considerPromotion = false;
                        for (PromotionCondition cond : p.conditions) {
                            if (cond instanceof DownstreamPassCondition) {
                                DownstreamPassCondition dpcond = (DownstreamPassCondition) cond;
                                if(dpcond.contains(build.getParent())) {
                                    considerPromotion = true;
                                    break;
                                }
                            }
                        }
                        if(considerPromotion) {
                            try {
                                AbstractBuild<?,?> u = build.getUpstreamRelationshipBuild(j);
                                if(u==null) {
                                    // no upstream build. perhaps a configuration problem?
                                    if(build.getResult()==Result.SUCCESS)
                                        listener.getLogger().println("WARNING: "+j.getFullDisplayName()+" appears to use this job as a promotion criteria, but no fingerprint is recorded.");
                                } else
                                if(p.considerPromotion(u))
                                    listener.getLogger().println("Promoted "+u);
                            } catch (IOException e) {
                                e.printStackTrace(listener.error("Failed to promote a build"));
                            }
                        }
                    }
                }
            }
        }

        /**
         * List of downstream jobs that we are interested in.
         */
        @CopyOnWrite
        private static volatile Set<AbstractProject> DOWNSTREAM_JOBS = Collections.emptySet();

        /**
         * Called whenever some {@link JobPropertyImpl} changes to update {@link #DOWNSTREAM_JOBS}.
         */
        public static void rebuildCache() {
            Set<AbstractProject> downstreams = new HashSet<AbstractProject>();


            DOWNSTREAM_JOBS = downstreams;
        }
    }
}
