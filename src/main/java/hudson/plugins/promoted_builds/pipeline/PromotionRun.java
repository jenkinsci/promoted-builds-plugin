package hudson.plugins.promoted_builds.pipeline;

// TODO: implementation for Pipeline
// TODO: add generics?

import hudson.model.ParameterValue;
import hudson.model.Run;
import hudson.plugins.promoted_builds.Promotion;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @see Promotion
 *<p>
 * Interface to support pipeline builds' promotion
 */
public interface PromotionRun { // always a run?


    /**
     *
     * @return the run which we try to promote
     */
    @Nonnull
    Run<?,?> getPromotedRun();

    /**
     *
     * @return method which does the {@link Promotion}
     */
    @Nonnull
    Run<?,?> getPromotionRun();

    /**
     *
     * @return the parameter values from future DSL commands.
     */
    List<ParameterValue> getParameterValues();
}
