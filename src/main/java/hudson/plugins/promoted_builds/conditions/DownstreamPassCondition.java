package hudson.plugins.promoted_builds.conditions;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import hudson.CopyOnWrite;
import hudson.Extension;
import hudson.Util;
import hudson.console.HyperlinkNote;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Cause.UpstreamCause;
import hudson.model.Fingerprint;
import hudson.model.Fingerprint.BuildPtr;
import hudson.model.Hudson;
import hudson.model.InvisibleAction;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotionBadge;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.PromotionConditionDescriptor;
import hudson.plugins.promoted_builds.PromotionProcess;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
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

    private final boolean evenIfUnstable;

    public DownstreamPassCondition(String jobs) {
        this(jobs, false);
    }

    public DownstreamPassCondition(String jobs, boolean evenIfUnstable) {
        this.jobs = jobs;
        this.evenIfUnstable = evenIfUnstable;
    }

    public String getJobs() {
        return jobs;
    }

    public boolean isEvenIfUnstable() {
        return evenIfUnstable;
    }
    
    @Override
    public PromotionBadge isMet(PromotionProcess promotionProcess, AbstractBuild<?,?> build) {
        Badge badge = new Badge();

        PseudoDownstreamBuilds pdb = build.getAction(PseudoDownstreamBuilds.class);

        OUTER:
        for (AbstractProject<?,?> j : getJobList(build.getProject().getParent())) {
            for( AbstractBuild<?,?> b : build.getDownstreamBuilds(j) ) {
                Result r = b.getResult();
                if ((r == Result.SUCCESS) || (evenIfUnstable && r == Result.UNSTABLE)) {
                    badge.add(b);
                    continue OUTER;
                }
            }

            if (pdb!=null) {// if fingerprint doesn't have any, try the pseudo-downstream
                for (AbstractBuild<?,?> b : pdb.listBuilds(j)) {
                    Result r = b.getResult();
                    if ((r == Result.SUCCESS) || (evenIfUnstable && r == Result.UNSTABLE)) {
                        badge.add(b);
                        continue OUTER;
                    }
                }
            }

            // none of the builds of this job passed.
            return null;
        }
        
        return badge;
    }

    /**
     * List of downstream jobs that we need to monitor.
     *
     * @return never null.
     */
    public List<AbstractProject<?,?>> getJobList(ItemGroup context) {
        List<AbstractProject<?,?>> r = new ArrayList<AbstractProject<?,?>>();
        for (String name : Util.tokenize(jobs,",")) {
            AbstractProject job = Hudson.getInstance().getItem(name.trim(), context, AbstractProject.class);
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

    @Extension
    public static final class DescriptorImpl extends PromotionConditionDescriptor {
        public boolean isApplicable(AbstractProject<?,?> item) {
            return true;
        }

        public String getDisplayName() {
            return Messages.DownstreamPassCondition_DisplayName();
        }

        public PromotionCondition newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return new DownstreamPassCondition(
                    formData.getString("jobs"), formData.getBoolean("evenIfUnstable"));
        }

        public AutoCompletionCandidates doAutoCompleteJobs(@QueryParameter String value, @AncestorInPath AbstractProject project) {
            List<AbstractProject> downstreams = project.getDownstreamProjects();
            List<Item> all = Jenkins.getInstance().getItems(Item.class);
            List<String> candidatesDownstreams = Lists.newArrayList();
            List<String> candidatesOthers = Lists.newArrayList();
            for (Item i : all) {
                if(! i.hasPermission(Item.READ)) continue;
                Set<String> names = Sets.newLinkedHashSet();
                names.add(i.getRelativeNameFrom(project));
                names.add(i.getFullName());
                for(String name : names) {
                    if(name.startsWith(value)) {
                        if(downstreams.contains(i)) {
                            candidatesDownstreams.add(name);
                        }else{
                            candidatesOthers.add(name);
                        }
                    }
                }
            }
            AutoCompletionCandidates candidates = new AutoCompletionCandidates();
            candidates.add(candidatesDownstreams.toArray(new String[0]));
            if(candidatesDownstreams.size() > 0 && candidatesOthers.size() > 0) {
                candidates.add("- - -");
            }
            // Downstream jobs might not be set when user wants to set DownstreamPassCondition.
            // Better to show non-downstream candidates even if they are not downstreams at the moment.
            candidates.add(candidatesOthers.toArray(new String[0]));
            return candidates;
        }
    }

    /**
     * {@link RunListener} to pick up completions of downstream builds.
     *
     * <p>
     * This is a single instance that receives all the events everywhere in the system.
     * @author Kohsuke Kawaguchi
     */
    @Extension
    public static final class RunListenerImpl extends RunListener<AbstractBuild<?,?>> {
        public RunListenerImpl() {
            super((Class)AbstractBuild.class);
        }

        @Override
        public void onCompleted(AbstractBuild<?,?> build, TaskListener listener) {
            // this is not terribly efficient,
            for(AbstractProject<?,?> j : Hudson.getInstance().getAllItems(AbstractProject.class)) {
                boolean warned = false; // used to avoid warning for the same project more than once.

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
                                if (u==null) {
                                    // if the fingerprint doesn't tell us, perhaps the cause would tell us?
                                    for (UpstreamCause uc : Util.filter(build.getCauses(), UpstreamCause.class)) {
                                        if (uc.getUpstreamProject().equals(j.getFullName())) {
                                            u = j.getBuildByNumber(uc.getUpstreamBuild());
                                            if (u!=null) {
                                                // remember that this build is a pseudo-downstream of the discovered build.
                                                PseudoDownstreamBuilds pdb = u.getAction(PseudoDownstreamBuilds.class);
                                                if (pdb==null)
                                                    u.addAction(pdb=new PseudoDownstreamBuilds());
                                                pdb.add(build);
                                                u.save();
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (u==null) {
                                    // no upstream build. perhaps a configuration problem?
                                    if(build.getResult()==Result.SUCCESS && !warned) {
                                        listener.getLogger().println("WARNING: "+j.getFullDisplayName()+" appears to use this job as a promotion criteria, " +
                                            "but no fingerprint is recorded. Fingerprint needs to be enabled on both this job and "+j.getFullDisplayName()+". " +
                                                "See http://hudson.gotdns.com/wiki/display/HUDSON/Fingerprint for more details");
                                        warned = true;
                                    }
                                }

                                if(u!=null && p.considerPromotion2(u)!=null)
                                    listener.getLogger().println("Promoted "+HyperlinkNote.encodeTo('/'+u.getUrl(),u.getFullDisplayName()));
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
            DOWNSTREAM_JOBS = new HashSet<AbstractProject>();
        }
    }

    /**
     * Remembers those downstream jobs that are not related by fingerprint but by the triggering relationship.
     * This is a weaker form of the relationship and less reliable, but often people don't understand
     * the notion of fingerprints, in which case this works.
     */
    public static class PseudoDownstreamBuilds extends InvisibleAction {
        final List<BuildPtr> builds = new ArrayList<BuildPtr>();

        public void add(AbstractBuild<?,?> run) {
            builds.add(new BuildPtr(run));
        }

        public List<AbstractBuild<?,?>> listBuilds(AbstractProject<?, ?> job) {
            List<AbstractBuild<?,?>> list = new ArrayList<AbstractBuild<?,?>>();
            for (BuildPtr b : builds) {
                if (b.is(job)) {
                    Run r = b.getRun();
                    if (r instanceof AbstractBuild)
                        // mainly null check, plus a defensive measure caused by a possible rename.
                        list.add((AbstractBuild)r);
                }
            }
            return list;
        }
    }
}
