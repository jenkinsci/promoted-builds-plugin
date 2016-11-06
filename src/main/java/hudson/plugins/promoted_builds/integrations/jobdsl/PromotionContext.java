package hudson.plugins.promoted_builds.integrations.jobdsl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.util.Node;
import groovy.util.NodeBuilder;
import groovy.util.NodeList;
import groovy.util.XmlNodePrinter;
import groovy.util.XmlParser;
import hudson.plugins.promoted_builds.PromotionProcess;
import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.dsl.DslContext;
import javaposse.jobdsl.dsl.JobManagement;
import javaposse.jobdsl.dsl.helpers.step.StepContext;
import javaposse.jobdsl.plugin.DslEnvironment;
import org.codehaus.groovy.runtime.GroovyCategorySupport;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static javaposse.jobdsl.plugin.ContextExtensionPoint.executeInContext;

/**
 * Context for defining a {@link hudson.plugins.promoted_builds.PromotionProcess}
 */
public class PromotionContext implements Context {

    protected final JobManagement jobManagement;
    protected final DslEnvironment dslEnvironment;

    protected String name;

    private final List<Closure> configureBlocks = new ArrayList<>();

    public PromotionContext(final JobManagement jobManagement, final DslEnvironment dslEnvironment) {
        this.jobManagement = jobManagement;
        this.dslEnvironment = dslEnvironment;
    }

    /**
     *
     * @param name
     * @deprecated Use {@link PromotionsContext#promotion(String, Closure)} for consistency
     *  with Job DSL core API such as {@link javaposse.jobdsl.dsl.DslFactory#freeStyleJob(String, Closure)}.
     */
    @Deprecated
    public void name(final String name) {
        jobManagement.logDeprecationWarning();
        this.name = name;
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
        configure(new Closure(this) {
            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked when the closure gets called")
            protected void doCall(Node promotion) {
                for (Node stepNode: stepContext.getStepNodes()) {
                    ((Node) ((NodeList) promotion.get("buildSteps")).get(0)).append(stepNode);
                }
            }
        });
    }

    /**
     * Enqueue a {@link Node} manipulation operation
     * @param closure Operation to enqueue. Will be given a single {@link Node} argument representing the root of the
     *                configuration.
     */
    public void configure(final Closure<?> closure) {
        configureBlocks.add(closure);
    }

    /**
     * Retrieve an XML string representation of {@link #getNode()}
     * @return XML for the current promotion definition. Unlikely to be useful for DSL script authors.
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public String getXml() throws ParserConfigurationException, SAXException, IOException {
        Writer xmlOutput = new StringWriter();
        XmlNodePrinter xmlNodePrinter = new XmlNodePrinter(new PrintWriter(xmlOutput), "    ");
        xmlNodePrinter.setPreserveWhitespace(true);
        xmlNodePrinter.setExpandEmptyElements(true);
        xmlNodePrinter.setQuote("'"); // Use single quote for attributes
        xmlNodePrinter.print(getNode());

        return xmlOutput.toString();
    }

    /**
     * Trigger processing of the {@link #configure(Closure) queued operations} against a
     * {@link #getNodeTemplate() template}
     * @return  {@link Node Node tree} for the current promotion definition. Unlikely to be useful for DSL script
     *          authors (instance is transient).
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public Node getNode() throws IOException, SAXException, ParserConfigurationException {
        Node node = getNodeTemplate();
        // we don't make use of Job DSL ContextHelper because it is not part of the public API
        executeConfigureBlocks(node, configureBlocks);
        return node;
    }

    protected Node getNodeTemplate() throws ParserConfigurationException, SAXException, IOException {
        try (final InputStream resourceAsStream = getClass().getResourceAsStream(getClass().getSimpleName() + "-template.xml")) {
            return new XmlParser().parse(resourceAsStream);
        }
    }

    /**
     * Duplication of Job DSL <a
     * href="https://github.com/jenkinsci/job-dsl-plugin/blob/job-dsl-1.52/job-dsl-core/src/main/groovy/javaposse/jobdsl/dsl/ContextHelper.groovy#L20"
     * >ContextHelper</a> to avoid use of non-public API. Code was modified to account for Java compilation.
     *
     * @param node
     * @param configureBlock
     */
    private static void executeConfigureBlock(final Node node, final Closure configureBlock) {
        if (configureBlock != null) {
            configureBlock.setDelegate(new MissingPropertyToStringDelegate(node));

            GroovyCategorySupport.use(NodeEnhancement.class, new Closure(null) {
                @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked when the closure gets called")
                protected void doCall() {
                    configureBlock.call(node);
                }
            });
        }
    }

    /**
     * Duplication of Job DSL <a
     * href="https://github.com/jenkinsci/job-dsl-plugin/blob/job-dsl-1.52/job-dsl-core/src/main/groovy/javaposse/jobdsl/dsl/ContextHelper.groovy#L30"
     * >ContextHelper</a> to avoid use of non-public API. Code was modified to account for Java compilation.
     *
     * @param node
     * @param configureBlocks
     */
    private static void executeConfigureBlocks(Node node, List<Closure> configureBlocks) {
        for (Closure it: configureBlocks) {
            executeConfigureBlock(node, it);
        }
    }
}
