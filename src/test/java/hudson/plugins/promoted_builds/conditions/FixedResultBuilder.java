package hudson.plugins.promoted_builds.conditions;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: stephenc
 * Date: 09/08/2011
 * Time: 11:15
 * To change this template use File | Settings | File Templates.
 */
public class FixedResultBuilder extends Builder {

    private Result buildResult;

    @DataBoundConstructor
    public FixedResultBuilder(Result buildResult) {
        this.buildResult = buildResult;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public Result getBuildResult() {
        return buildResult;
    }

    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        build.getWorkspace().child("my.file").write("Hello world!", "UTF-8");
        build.setResult(buildResult);
        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return null;
        }
    }
}
