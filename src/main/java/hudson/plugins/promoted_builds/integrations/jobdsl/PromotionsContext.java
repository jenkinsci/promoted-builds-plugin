package hudson.plugins.promoted_builds.integrations.jobdsl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;

import groovy.lang.Closure;
import javaposse.jobdsl.dsl.Context;

public class PromotionsContext implements Context {

    Set<String> names = new HashSet<String>();
    
    Map<String,PromotionContext> promotionContexts = new HashMap<String,PromotionContext>();

    /**
     * PromotionNodes:
     * 1. <string>dev</string>
     * 2. <string>test</string>
     * 
     * AND
     * 
     * Sub PromotionNode for every promotion
     * 1. <project>
     * <name>dev</name>
     * .
     * .
     * .
     * </project>
     * 2. <project>
     * <name>test</name>
     * .
     * .
     * .
     * </project>
     * 
     * @param promotionClosure
     * @return
     */
    public void promotion(Closure<?> promotionClosure) {
        PromotionContext promotionContext = new PromotionContext();
        executeInContext(promotionClosure, promotionContext);
        Preconditions.checkNotNull(promotionContext.getName(), "promotion name cannot be null");
        Preconditions.checkArgument(promotionContext.getName().length() > 0);
        names.add(promotionContext.getName());
        promotionContexts.put(promotionContext.getName(),promotionContext);
        
    }

    private static void executeInContext(Closure<?> configClosure, Object context) {
        configClosure.setDelegate(context);
        configClosure.setResolveStrategy(Closure.DELEGATE_FIRST);
        configClosure.call();
    }

}
