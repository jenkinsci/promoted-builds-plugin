package hudson.plugins.promoted_builds;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.PrintStream;

public class KeepBuildForeverAction extends Notifier {
    
    @DataBoundConstructor
    public KeepBuildForeverAction() { }
    
    private static final Result PROMOTION_RESULT_MUST_BE_AT_LEAST = Result.UNSTABLE;
    
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        PrintStream console = listener.getLogger();
        // only applicable to promotions, so should be impossible not to be one, but check anyway
        if (!(build instanceof Promotion)) {
            console.println(Messages.KeepBuildForEverAction_console_notPromotion());
            build.setResult(Result.FAILURE);
            return false;
        }
        if ((build.getResult() != null) && build.getResult().isWorseThan(PROMOTION_RESULT_MUST_BE_AT_LEAST)) {
            console.println(Messages.KeepBuildForEverAction_console_promotionNotGoodEnough(build.getResult()));
            return true;
        }
        AbstractBuild promoted = ((Promotion) build).getTarget();
        console.println(Messages.KeepBuildForEverAction_console_keepingBuild());
        promoted.keepLog();
        return true;
    }

    @Extension
    public static class KeepBuildForeverDescriptor extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return jobType == PromotionProcess.class;
        }

        @Override
        public String getDisplayName() {
            return Messages.KeepBuildForEverAction_descriptor_displayName();
        }
    }
    
}
