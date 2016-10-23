package hudson.plugins.promoted_builds.integrations.jobdsl;

import groovy.lang.Closure;
import groovy.lang.MetaClass;
import groovy.util.Node;
import groovy.util.NodeBuilder;
import groovy.util.NodeList;
import groovy.util.XmlParser;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import hudson.plugins.promoted_builds.conditions.ParameterizedSelfPromotionCondition;
import hudson.plugins.promoted_builds.conditions.SelfPromotionCondition;
import hudson.plugins.promoted_builds.conditions.UpstreamPromotionCondition;
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

class PromotionContext extends Item {

    private static class ConfigureConditions {
        public static final String METHOD_NAME = "configureConditions";
        private final List<Node> conditions;
        public ConfigureConditions(List<Node> conditions) {
            this.conditions = conditions;
        }
        public void configureConditions(Node promotion) {
            for (final Node condition : conditions) {
                ((Node) ((NodeList) promotion.get("conditions")).get(0)).append(condition);
            }
        }
    }

    /**
     * @deprecated Use automatically generated DSL for complete coverage of available {@link PromotionCondition}
     * implementations
     */
    @Deprecated
    private static class ConfigureLegacyConditions {
        public static final String METHOD_NAME = "configureConditions";
        private final ConditionsContext conditionContext;
        public ConfigureLegacyConditions(final ConditionsContext conditionsContext) {
            this.conditionContext = conditionsContext;
        }
        public void configureConditions(Node promotion) throws Exception {
            final XmlParser xmlParser = new XmlParser();
            Node container = ((Node) ((NodeList) promotion.get("conditions")).get(0));
            if (conditionContext.isSelfPromotion()) {
                container.append(xmlParser.parseText(PromotionsExtensionPoint.XSTREAM.toXML(new SelfPromotionCondition(conditionContext.isEvenIfUnstable()))));
            }
            if (conditionContext.isParameterizedSelfPromotion()) {
                container.append(xmlParser.parseText(PromotionsExtensionPoint.XSTREAM.toXML(new ParameterizedSelfPromotionCondition(conditionContext.isEvenIfUnstableParameterized(), conditionContext
                    .getParameterName(), conditionContext.getParameterValue()))));
            }
            if (conditionContext.isManual()) {
                JobDslManualCondition condition = new JobDslManualCondition();
                condition.setUsers(conditionContext.getUsers());
                if (conditionContext.getParams() != null) {
                    condition.setParameterDefinitionNodes(conditionContext.getParams().values());
                }
                container.append(xmlParser.parseText(PromotionsExtensionPoint.XSTREAM.toXML(condition)));
            }
            if (conditionContext.isReleaseBuild()) {
                container.append(xmlParser.parseText(PromotionsExtensionPoint.XSTREAM.toXML(new ReleasePromotionCondition())));
            }
            if (conditionContext.isDownstreamPass()) {
                container.append(xmlParser.parseText(PromotionsExtensionPoint.XSTREAM.toXML(new DownstreamPassCondition(conditionContext.getJobs(), conditionContext.isEvenIfUnstableDownstream()))));
            }
            if (conditionContext.isUpstreamPromotion()) {
                container.append(xmlParser.parseText(PromotionsExtensionPoint.XSTREAM.toXML(new UpstreamPromotionCondition(conditionContext.getPromotionNames()))));
            }
        }
    }

    // never persist the MetaClass
    private transient MetaClass metaClass;

    /** the owning project */
    protected final Item parentItem;
    protected final DslEnvironment dslEnvironment;

    private List<Node> actions = new ArrayList<Node>();

    /**
     *
     * @param name
     * @deprecated Use {@link #setName(String)}
     */
    @Deprecated
    public void name(final String name) {
        setName(name);
    }

    public void icon(final String icon) {
        configure(new MethodClosure(new Object() {
            public void conf(Node promotion) {
                ((Node) ((NodeList) promotion.get("icon")).get(0)).setValue(icon);
            }
        }, "conf"));
    }

    /**
     *
     * @param restrict
     * @deprecated Use {@link #label(String)}
     */
    @Deprecated
    public void restrict(final String restrict) {
        label(restrict);
    }

    public void label(final String labelExpression) {
        configure(new MethodClosure(new Object() {
            public void conf(Node promotion) {
                Node node = (Node) new NodeBuilder().invokeMethod("assignedLabel", labelExpression);
                promotion.append(node);
            }
        }, "conf"));
    }

    public PromotionContext(JobManagement jobManagement, Item item, DslEnvironment dslEnvironment) {
        super(jobManagement);
        this.metaClass = InvokerHelper.getMetaClass(this.getClass());
        this.parentItem = item;
        this.dslEnvironment = dslEnvironment;
    }

    public void conditions(@DslContext(ConditionsContext.class) Closure<?> conditionClosure) {
        // delegate to ConditionsContext
        ConditionsContext conditionContext = new ConditionsContext(jobManagement, parentItem, dslEnvironment);
        executeInContext(conditionClosure, conditionContext);
        configure(new MethodClosure(new ConfigureConditions(conditionContext.conditionNodes), ConfigureConditions.METHOD_NAME));
        configure(new MethodClosure(new ConfigureLegacyConditions(conditionContext), ConfigureLegacyConditions.METHOD_NAME));
    }

    public void actions(@DslContext(StepContext.class) Closure<?> actionsClosure) {
        // delegate to StepContext
        final StepContext stepContext = dslEnvironment.createContext(StepContext.class);
        executeInContext(actionsClosure, stepContext);
        actions.addAll(stepContext.getStepNodes());
        configure(new MethodClosure(new Object(){
            public void doWork(Node promotion) {
                for (Node stepNode: stepContext.getStepNodes()) {
                    ((Node) ((NodeList) promotion.get("buildSteps")).get(0)).append(stepNode);
                }
            }
        }, "doWork"));
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
