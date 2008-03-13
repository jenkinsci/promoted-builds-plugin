package hudson.plugins.promoted_builds;

import hudson.Plugin;
import hudson.tasks.BuildStep;
import hudson.model.Jobs;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import hudson.plugins.promoted_builds.tasks.RedeployBatchTaskPublisher;

/**
 * @author Kohsuke Kawaguchi
 * @plugin
 */
public class PluginImpl extends Plugin {
    @Override
    public void start() throws Exception {
        Jobs.PROPERTIES.add(JobPropertyImpl.DescriptorImpl.INSTANCE);
        BuildStep.PUBLISHERS.addRecorder(RedeployBatchTaskPublisher.DESCRIPTOR);
        // force the evaluation of the object, which registers its handler
        noop(DownstreamPassCondition.DescriptorImpl.INSTANCE);

        // DownstreamPassCondition contains RunListener which triggers promotion conditions
    }

    private void noop(Object o) {
    }
}
