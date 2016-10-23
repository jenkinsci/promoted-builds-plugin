package hudson.plugins.promoted_builds.integrations.jobdsl;

import javaposse.jobdsl.dsl.AbstractContext;
import javaposse.jobdsl.dsl.DslContext;
import javaposse.jobdsl.dsl.Item;
import javaposse.jobdsl.dsl.JobManagement;
import com.google.common.base.Preconditions;
import javaposse.jobdsl.plugin.DslEnvironment;

import java.util.*;

import groovy.lang.Closure;

import static javaposse.jobdsl.plugin.ContextExtensionPoint.executeInContext;

public class PromotionsContext extends AbstractContext {
    protected final Item item;
    protected final DslEnvironment dslEnvironment;

    protected List<PromotionContext> promotionContexts = new ArrayList<PromotionContext>();

    public PromotionsContext(JobManagement jobManagement, Item item, DslEnvironment dslEnvironment) {
        super(jobManagement);
        this.item = item;
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
    public void promotion(@DslContext(PromotionContext.class) Closure<?> promotionClosure) {
        promotion(null, promotionClosure);
    }

    public void promotion(String name, @DslContext(PromotionContext.class) Closure promotionClosure) {
        PromotionContext promotionContext = new PromotionContext(jobManagement, item, dslEnvironment);
        promotionContext.setName(name);
        executeInContext(promotionClosure, promotionContext);
        Preconditions.checkNotNull(promotionContext.getName(), "promotion name cannot be null");
        Preconditions.checkArgument(promotionContext.getName().length() > 0);
        promotionContexts.add(promotionContext);
    }
}
