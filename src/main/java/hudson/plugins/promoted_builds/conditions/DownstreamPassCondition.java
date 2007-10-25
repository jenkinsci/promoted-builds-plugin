package hudson.plugins.promoted_builds.conditions;

import hudson.CopyOnWrite;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.PromotionConditionDescriptor;
import hudson.plugins.promoted_builds.PromotionCriterion;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
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
    public boolean isMet(AbstractBuild<?,?> build) {
        for (AbstractProject<?,?> j : getJobList()) {
            boolean passed = false;
            for( AbstractBuild<?,?> b : build.getDownstreamBuilds(j) ) {
                if(b.getResult()== Result.SUCCESS) {
                    passed = true;
                    break;
                }
            }

            if(!passed) // none of the builds of this job passed.
                return false;
        }
        return true;
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
                JobPropertyImpl p = j.getProperty(JobPropertyImpl.class);
                if(p!=null) {
                    for (PromotionCriterion c : p.getCriteria()) {
                        boolean considerPromotion = false;
                        for (PromotionCondition cond : c.getConditions()) {
                            if (cond instanceof DownstreamPassCondition) {
                                DownstreamPassCondition dpcond = (DownstreamPassCondition) cond;
                                if(dpcond.contains(build.getParent()))
                                    considerPromotion = true;
                            }
                        }
                        if(considerPromotion) {
                            try {
                                AbstractBuild<?,?> u = build.getUpstreamRelationshipBuild(j);
                                if(u!=null && c.considerPromotion(u))
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
