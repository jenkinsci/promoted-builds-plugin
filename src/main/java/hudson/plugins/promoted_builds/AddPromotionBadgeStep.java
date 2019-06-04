package hudson.plugins.promoted_builds;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;

//TODO: Add a Pipeline step so that we can spot-check the logic
// SimpleBuildStep  for quick win, but it will require workspace
// Use Pipeline: Step API for advanced thing
public class AddPromotionBadgeStep extends SimpleBuildStep {

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        // Somehow define badge
        // Take env vars
        // Append it to a the run action
    }

    @Extension
    @Symbol("addPromotionBadge")
    public static class DescriptorImpl extends ..Descriptor {

    }
}
