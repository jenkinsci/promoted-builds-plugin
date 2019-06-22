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
 */
public interface PromotionRun { // always a run?

    // Run which we try to promote
    @Nonnull
    Run<?,?> getPromotedRun();

    // Execution which does the promotion
    @Nonnull
    Run<?,?> getPromotionRun();

    //TODO: Move implementation to a default method?
    List<ParameterValue> getParameterValues();
}
