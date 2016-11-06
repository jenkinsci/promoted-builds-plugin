package hudson.plugins.promoted_builds.integrations.jobdsl;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.dsl.DslContext;
import javaposse.jobdsl.dsl.JobManagement;
import javaposse.jobdsl.dsl.Preconditions;
import javaposse.jobdsl.plugin.DslEnvironment;

import java.util.ArrayList;
import java.util.List;

import static javaposse.jobdsl.plugin.ContextExtensionPoint.executeInContext;

/**
 * Context for defining promotions
 */
public class PromotionsContext implements Context {
    protected final JobManagement jobManagement;
    protected final DslEnvironment dslEnvironment;
    protected final List<PromotionContext> promotionContexts = new ArrayList<>();

    public PromotionsContext(final JobManagement jobManagement, final DslEnvironment dslEnvironment) {
        this.jobManagement = jobManagement;
        this.dslEnvironment = dslEnvironment;
    }

    /**
     * @param promotionClosure
     * @deprecated Use {@link #promotion(String, Closure)} for consistency with Job DSL core API such as {@link javaposse.jobdsl.dsl.DslFactory#freeStyleJob(String, Closure)}
     */
    @Deprecated
    public void promotion(@DslContext(PromotionContext.class) @DelegatesTo(PromotionContext.class) Closure<?> promotionClosure) {
        jobManagement.logDeprecationWarning();
        promotion(null, promotionClosure);
    }

    /**
     * Declare a new promotion
     *
     * @param name                Promotion name
     * @param promotionClosure    Configuration closure
     */
    public void promotion(String name, @DslContext(PromotionContext.class) @DelegatesTo(PromotionContext.class) Closure promotionClosure) {
        PromotionContext promotionContext = dslEnvironment.createContext(PromotionContext.class);
        promotionContext.name = name;
        executeInContext(promotionClosure, promotionContext);
        Preconditions.checkNotNullOrEmpty(promotionContext.name, "promotion name cannot be null");
        promotionContexts.add(promotionContext);
    }
}
