package hudson.plugins.promoted_builds;

import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.DependencyGraph;
import hudson.model.Run;
import hudson.model.Descriptor;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;

import java.util.List;
import java.util.ArrayList;

/**
 * A dummy {@link AbstractProject} instance so that
 * {@link BuildStepDescriptor#isApplicable(AbstractProject)} can be used
 * to check if the {@link BuildStep} can be used as a post-promotion task.
 *
 * @author Kohsuke Kawaguchi
 */
public final class PostPromotionTask extends AbstractProject {
    private PostPromotionTask() {
        super(null, null);
    }

    public FilePath getWorkspace() {
        throw new UnsupportedOperationException();
    }

    protected Class getBuildClass() {
        throw new UnsupportedOperationException();
    }

    public boolean isFingerprintConfigured() {
        throw new UnsupportedOperationException();
    }

    protected void buildDependencyGraph(DependencyGraph graph) {
        throw new UnsupportedOperationException();
    }

    protected void removeRun(Run run) {
        throw new UnsupportedOperationException();
    }

    private static final PostPromotionTask INSTANCE = new PostPromotionTask();

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
                if(bsd.isApplicable(PostPromotionTask.INSTANCE))
                    list.add(d);
            }
        }
    }
}
