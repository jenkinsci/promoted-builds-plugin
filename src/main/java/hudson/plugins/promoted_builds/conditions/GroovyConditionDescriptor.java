package hudson.plugins.promoted_builds.conditions;

import hudson.Extension;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.model.AbstractProject;
import hudson.model.Job;
import hudson.model.TaskListener;
import hudson.plugins.promoted_builds.PromotionConditionDescriptor;
import javafx.concurrent.Task;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Declared outside GroovyCondition as it depends on the optional 'script-security' dependency
 */
@Extension(optional = true)
public class GroovyConditionDescriptor extends PromotionConditionDescriptor {

    private static final Logger LOGGER = Logger.getLogger(GroovyConditionDescriptor.class.getName());

    public GroovyConditionDescriptor() {
        super(GroovyCondition.class);
    }

   @Override
   public boolean isApplicable(@Nonnull Job<?,?> item, TaskListener listener){
        if(item instanceof AbstractProject){
            return isApplicable((AbstractProject)item, TaskListener.NULL);
        }else{
            final Jenkins jenkins = Jenkins.getInstance();
            if (jenkins == null) {
                // Jenkins not started or shut down
                return false;
            }
            final PluginManager pluginManager = jenkins.getPluginManager();
            final PluginWrapper plugin = pluginManager.getPlugin("script-security");
            return plugin != null && plugin.isActive();
        }

   }

   public boolean isApplicable(AbstractProject<?, ?> item) {
        return false;
   }


    @Override
    public String getDisplayName() {
        return Messages.GroovyCondition_DisplayName();
    }

}
