package hudson.plugins.promoted_builds;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.Nonnull;

/**
 * Captures the information about how/when the promotion criteria is satisfied.
 *
 * <p>
 * This information is used by humans to make sense out of what happened.
 *
 * @author Kohsuke Kawaguchi
 */
@ExportedBean
public abstract class PromotionBadge {

    /**
     * Called by {@link Status} to allow promotion badges to contribute environment variables.
     *
     * @param run
     *      The calling run. Never null
     * @param env
     *      Environment variables should be added to this map.
     */
    public void buildEnvVars(@Nonnull Run<?,?> run, EnvVars env,TaskListener listener) {
        // Default implementation when method is not overridden i.e a classical build
        if(run instanceof AbstractBuild){
            buildEnvVars((AbstractBuild<?,?>)run,env);
        }
    }

    /**

      @deprecated Use {@link #buildEnvVars(Run, EnvVars, TaskListener)}

     */

    @Deprecated
    public void buildEnvVars(AbstractBuild<?,?> build, EnvVars env) {
        // by default don't contribute any variables
    }
}
