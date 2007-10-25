package hudson.plugins.promoted_builds;

import hudson.Plugin;
import hudson.model.Jobs;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;

/**
 * @author Kohsuke Kawaguchi
 * @plugin
 */
public class PluginImpl extends Plugin {
    @Override
    public void start() throws Exception {
        Jobs.PROPERTIES.add(JobPropertyImpl.DescriptorImpl.INSTANCE);
        // force the evaluation of the object, which registers its handler
        noop(DownstreamPassCondition.DescriptorImpl.INSTANCE);
    }

    private void noop(Object o) {
    }
}
