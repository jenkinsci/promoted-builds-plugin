package hudson.plugins.promoted_builds;

import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.DependencyGraph;
import hudson.model.Descriptor;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * A dummy {@link AbstractProject} to carry out promotion operations.
 *
 * @author Kohsuke Kawaguchi
 */
public final class PromotionProcessJob extends AbstractProject<PromotionProcessJob,PromotionProcess> {
    public final JobPropertyImpl property;
    public final PromotionConfig config;

    /*package*/ PromotionProcessJob(JobPropertyImpl property, PromotionConfig config) {
        super(null, property.getOwner().getFullName()+" promotion ("+config.getName()+")");
        this.property = property;
        this.config = config;
    }

    /**
     * Gets the owner {@link AbstractProject} thta configured {@link #property}
     */
    public AbstractProject<?,?> getOwner() {
        return property.getOwner();
    }

    public FilePath getWorkspace() {
        return getOwner().getWorkspace();
    }

    protected Class<PromotionProcess> getBuildClass() {
        return PromotionProcess.class;
    }

//
// these are dummy implementations to implement abstract methods.
// need to think about what the implications are.
//
    public boolean isFingerprintConfigured() {
        throw new UnsupportedOperationException();
    }

    protected void buildDependencyGraph(DependencyGraph graph) {
        throw new UnsupportedOperationException();
    }

    public static List<Descriptor<? extends BuildStep>> getAll() {
        List<Descriptor<? extends BuildStep>> list = new ArrayList<Descriptor<? extends BuildStep>>();
        addTo(BuildStep.BUILDERS, list);
        addTo(BuildStep.PUBLISHERS, list);
        return list;
    }

    private static void addTo(List<? extends Descriptor<? extends BuildStep>> source, List<Descriptor<? extends BuildStep>> list) {
        for (Descriptor<? extends BuildStep> d : source) {
            if (d instanceof BuildStepDescriptor) {
                BuildStepDescriptor bsd = (BuildStepDescriptor) d;
                if(bsd.isApplicable(PromotionProcessJob.class))
                    list.add(d);
            }
        }
    }
}
