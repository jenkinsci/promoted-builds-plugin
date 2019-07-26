package hudson.plugins.promoted_builds;

// TODO: implementation for Pipeline
// TODO: add generics?

import hudson.model.ParameterValue;
import hudson.model.Run;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @see Promotion
 */
public interface PromotionRun {

    // Run which we try to promote
    @Nonnull
    Run<?,?> getPromotedRun();

    // Execution which does the promotion
    @Nonnull
    Run<?,?> getPromotionRun();

    //TODO: Move implementation to a default method?
    List<ParameterValue> getParameterValues();
}