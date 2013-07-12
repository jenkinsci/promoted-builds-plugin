package hudson.plugins.promoted_builds;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import org.kohsuke.stapler.export.ExportedBean;

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
     * @param build
     *      The calling build. Never null.
     * @param env
     *      Environment variables should be added to this map.
     */
    public void buildEnvVars(AbstractBuild<?,?> build, EnvVars env) {
        // by default don't contribute any variables
    }
}
