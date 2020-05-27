package hudson.plugins.promoted_builds.conditions;

import hudson.Extension;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.model.AbstractProject;
import hudson.plugins.promoted_builds.PromotionConditionDescriptor;
import jenkins.model.Jenkins;

import java.util.logging.Logger;

/**
 * Declared outside GroovyCondition as it depends on the optional 'script-security' dependency
 */
@Extension(optional = true)
public final class GroovyConditionDescriptor extends PromotionConditionDescriptor {

    private static final Logger LOGGER = Logger.getLogger(GroovyConditionDescriptor.class.getName());

    public GroovyConditionDescriptor() {
        super(GroovyCondition.class);
    }

    @Override
    public boolean isApplicable(final AbstractProject<?, ?> item) {
        final PluginManager pluginManager = Jenkins.get().getPluginManager();
        final PluginWrapper plugin = pluginManager.getPlugin("script-security");
        return plugin != null && plugin.isActive();
    }

    @Override
    public String getDisplayName() {
        return Messages.GroovyCondition_DisplayName();
    }

}
