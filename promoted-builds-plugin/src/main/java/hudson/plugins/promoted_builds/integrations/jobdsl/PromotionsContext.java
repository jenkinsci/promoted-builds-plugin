package hudson.plugins.promoted_builds.integrations.jobdsl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;

import groovy.lang.Closure;
import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.plugin.DslEnvironment;

import static javaposse.jobdsl.plugin.ContextExtensionPoint.executeInContext;

public class PromotionsContext implements Context {
    private final DslEnvironment dslEnvironment;

    Set<String> names = new HashSet<String>();
    
    Map<String,PromotionContext> promotionContexts = new HashMap<String,PromotionContext>();

    public PromotionsContext(DslEnvironment dslEnvironment) {
        this.dslEnvironment = dslEnvironment;
    }

    //TODO: What does this Javadoc mean? Just marked as pre
    /**
     * See the examples below.
     * 
     * PromotionNodes:
     * <pre>
     * 1. &lt;string&gt;dev&lt;/string&gt;
     * 2. &lt;string&gt;test&lt;/string&gt;
     * 
     * AND
     * 
     * Sub PromotionNode for every promotion:
     * 1. &lt;project&gt;
     * &lt;name&gt;dev&lt;/name&gt;
     * .
     * .
     * .
     * &lt;/project&gt;
     * 2. &lt;project&gt;
     * &lt;name&gt;test&lt;/name&gt;
     * .
     * .
     * .
     * &lt;/project&gt;
     * </pre>
     * @param promotionClosure Input closure
     */
    public void promotion(Closure<?> promotionClosure) {
        PromotionContext promotionContext = new PromotionContext(dslEnvironment);
        executeInContext(promotionClosure, promotionContext);
        Preconditions.checkNotNull(promotionContext.getName(), "promotion name cannot be null");
        Preconditions.checkArgument(promotionContext.getName().length() > 0);
        names.add(promotionContext.getName());
        promotionContexts.put(promotionContext.getName(),promotionContext);
        
    }

}
