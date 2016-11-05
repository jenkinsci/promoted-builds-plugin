package hudson.plugins.promoted_builds.integrations.jobdsl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.MetaClass;
import groovy.util.Node;
import groovy.util.NodeBuilder;
import groovy.util.NodeList;
import hudson.plugins.promoted_builds.PromotionProcess;
import javaposse.jobdsl.dsl.DslContext;
import javaposse.jobdsl.dsl.Item;
import javaposse.jobdsl.dsl.JobManagement;
import javaposse.jobdsl.dsl.helpers.step.StepContext;
import javaposse.jobdsl.plugin.DslEnvironment;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.ArrayList;
import java.util.List;

import static javaposse.jobdsl.plugin.ContextExtensionPoint.executeInContext;

/**
 * Context for defining a {@link hudson.plugins.promoted_builds.PromotionProcess}
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

    /**
     *
     * @param icon
     * @see PromotionProcess#getIcon()
     */
    public void icon(final String icon) {
        configure(new Closure(this) {
            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked when the closure gets called")
            protected void doCall(Node promotion) {
                ((Node) ((NodeList) promotion.get("icon")).get(0)).setValue(icon);
            }
        });
    }

    /**
     *
     * @param restrict
     * @deprecated Use {@link #label(String)} for consistency with Job DSL core API {@link javaposse.jobdsl.dsl.Job#label(String)}
     */
    @Deprecated
    public void restrict(final String restrict) {
        // no String argument because JobDSL expects it to be the subject of the deprecation instead of an actual description
        jobManagement.logDeprecationWarning();
        label(restrict);
    }

    /**
     * Label which specifies which nodes the promotion can run on.
     * @param labelExpression
     * @see PromotionProcess#getAssignedLabel()
     */
    public void label(final String labelExpression) {
        configure(new Closure(this) {
            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked when the closure gets called")
            protected void doCall(Node promotion) {
                final NodeList assignedLabels = (NodeList) promotion.get("assignedLabel");
                if (assignedLabels.size() == 0) {
                    final Node node = (Node) new NodeBuilder().invokeMethod("assignedLabel", labelExpression);
                    promotion.append(node);
                } else {
                    ((Node) assignedLabels.get(0)).setValue(labelExpression);
                }
            }
        });
    }

    public PromotionContext(JobManagement jobManagement, DslEnvironment dslEnvironment) {
        super(jobManagement);
        this.metaClass = InvokerHelper.getMetaClass(this.getClass());
        this.dslEnvironment = dslEnvironment;
    }

    /**
     * Configure the top level &lt;conditions&gt; element by adding the provided nodes as children.
     * @param conditionClosure    Can be {@code null}
     * @see PromotionProcess#conditions
     */
    public void conditions(@DslContext(ConditionsContext.class) @DelegatesTo(ConditionsContext.class) Closure<?> conditionClosure) {
        // delegate to ConditionsContext
        final ConditionsContext conditionContext = dslEnvironment.createContext(ConditionsContext.class);
        executeInContext(conditionClosure, conditionContext);
        configure(new Closure(this) {
            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked when the closure gets called")
            protected void doCall(Node promotion) {
                for (final Node condition : conditionContext.conditionNodes) {
                    ((Node) ((NodeList) promotion.get("conditions")).get(0)).append(condition);
                }
            }
        });
    }

    /**
     * Configure the build steps of the promotion process
     * @param actionsClosure Can be {@code null}
     * @see PromotionProcess#getBuildSteps()
     */
    public void actions(@DslContext(StepContext.class) @DelegatesTo(StepContext.class) Closure<?> actionsClosure) {
        // delegate to StepContext
        final StepContext stepContext = dslEnvironment.createContext(StepContext.class);
        executeInContext(actionsClosure, stepContext);
        actions.addAll(stepContext.getStepNodes());
        configure(new Closure(this) {
            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked when the closure gets called")
            protected void doCall(Node promotion) {
                for (Node stepNode: stepContext.getStepNodes()) {
                    ((Node) ((NodeList) promotion.get("buildSteps")).get(0)).append(stepNode);
                }
            }
        });
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
