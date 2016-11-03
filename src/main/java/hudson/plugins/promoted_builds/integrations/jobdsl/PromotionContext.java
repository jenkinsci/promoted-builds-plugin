package hudson.plugins.promoted_builds.integrations.jobdsl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Closure;
import groovy.lang.MetaClass;
import groovy.util.Node;
import groovy.util.NodeBuilder;
import groovy.util.NodeList;
import javaposse.jobdsl.dsl.DslContext;
import javaposse.jobdsl.dsl.Item;
import javaposse.jobdsl.dsl.JobManagement;
import javaposse.jobdsl.dsl.helpers.step.StepContext;
import javaposse.jobdsl.plugin.DslEnvironment;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.MethodClosure;

import java.util.ArrayList;
import java.util.List;

import static javaposse.jobdsl.plugin.ContextExtensionPoint.executeInContext;

/**
 * Implementation notes: in order to get advanced integration with Job DSL which is Groovy-powered, we make use of
 * some helper classes such as the {@link MethodClosure} adapter used for methods accepting Closure arguments. This may
 * result in some unusual OOP constructs.
 */
public class PromotionContext extends Item {

    // never persist the MetaClass
    private transient MetaClass metaClass;

    protected final DslEnvironment dslEnvironment;

    private List<Node> actions = new ArrayList<Node>();

    /**
     *
     * @param name
     * @deprecated Use {@link PromotionsContext#promotion(String, Closure)} for consistency
     *  with Job DSL core API such as {@link javaposse.jobdsl.dsl.DslFactory#freeStyleJob(String, Closure)}.
     */
    @Deprecated
    public void name(final String name) {
        setName(name);
    }

    public void icon(final String icon) {
        configure(new MethodClosure(new Object() {
            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically called by MethodClosure")
            public void call(Node promotion) {
                ((Node) ((NodeList) promotion.get("icon")).get(0)).setValue(icon);
            }
        }, "call"));
    }

    /**
     *
     * @param restrict
     * @deprecated Use {@link #label(String)} for consistency with Job DSL core API {@link javaposse.jobdsl.dsl.Job#label(String)}
     */
    @Deprecated
    public void restrict(final String restrict) {
        jobManagement.logDeprecationWarning("Use label(String) for consistency with Job DSL core API");
        label(restrict);
    }

    public void label(final String labelExpression) {
        configure(new MethodClosure(new Object() {
            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically called by MethodClosure")
            public void call(Node promotion) {
                Node node = (Node) new NodeBuilder().invokeMethod("assignedLabel", labelExpression);
                promotion.append(node);
            }
        }, "call"));
    }

    public PromotionContext(JobManagement jobManagement, DslEnvironment dslEnvironment) {
        super(jobManagement);
        this.metaClass = InvokerHelper.getMetaClass(this.getClass());
        this.dslEnvironment = dslEnvironment;
    }

    /**
     * Configure the top level &lt;conditions&gt; element by adding the provided nodes as children.
     */
    public void conditions(@DslContext(ConditionsContext.class) Closure<?> conditionClosure) {
        // delegate to ConditionsContext
        final ConditionsContext conditionContext = dslEnvironment.createContext(ConditionsContext.class);
        executeInContext(conditionClosure, conditionContext);
        configure(new MethodClosure(new Object() {
            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically called by MethodClosure")
            public void call(Node promotion) {
                for (final Node condition : conditionContext.conditionNodes) {
                    ((Node) ((NodeList) promotion.get("conditions")).get(0)).append(condition);
                }
            }
        }, "call"));
    }

    public void actions(@DslContext(StepContext.class) Closure<?> actionsClosure) {
        // delegate to StepContext
        final StepContext stepContext = dslEnvironment.createContext(StepContext.class);
        executeInContext(actionsClosure, stepContext);
        actions.addAll(stepContext.getStepNodes());
        configure(new MethodClosure(new Object() {
            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically called by MethodClosure")
            public void call(Node promotion) {
                for (Node stepNode: stepContext.getStepNodes()) {
                    ((Node) ((NodeList) promotion.get("buildSteps")).get(0)).append(stepNode);
                }
            }
        }, "call"));
    }

    @Override
    public Object getProperty(String property) {
        return getMetaClass().getProperty(this, property);
    }

    @Override
    public void setProperty(String property, Object newValue) {
        getMetaClass().setProperty(this, property, newValue);
    }

    @Override
    public Object invokeMethod(String name, Object args) {
        return getMetaClass().invokeMethod(this, name, args);
    }

    @Override
    public MetaClass getMetaClass() {
        if (metaClass == null) {
            metaClass = InvokerHelper.getMetaClass(getClass());
        }
        return metaClass;
    }

    @Override
    public void setMetaClass(MetaClass metaClass) {
        this.metaClass = metaClass;
    }
}
