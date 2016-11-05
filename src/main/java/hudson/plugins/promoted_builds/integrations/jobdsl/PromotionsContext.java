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

public class PromotionsContext implements Context {
    protected final JobManagement jobManagement;
    protected final DslEnvironment dslEnvironment;

    protected List<PromotionContext> promotionContexts = new ArrayList<>();

    public PromotionsContext(final JobManagement jobManagement, final DslEnvironment dslEnvironment) {
        this.jobManagement = jobManagement;
        this.dslEnvironment = dslEnvironment;
    }

    public void promotion(@DslContext(PromotionContext.class) @DelegatesTo(PromotionContext.class) Closure<?> promotionClosure) {
        promotion(null, promotionClosure);
    }

    public void promotion(String name, @DslContext(PromotionContext.class) @DelegatesTo(PromotionContext.class) Closure promotionClosure) {
        PromotionContext promotionContext = dslEnvironment.createContext(PromotionContext.class);
        promotionContext.name = name;
        executeInContext(promotionClosure, promotionContext);
        Preconditions.checkNotNullOrEmpty(promotionContext.getName(), "promotion name cannot be null");
        promotionContexts.add(promotionContext);
    }
}
