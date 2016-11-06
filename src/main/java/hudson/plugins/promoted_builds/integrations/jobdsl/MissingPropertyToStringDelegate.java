package hudson.plugins.promoted_builds.integrations.jobdsl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.util.Node;
import groovy.util.NodeBuilder;
import groovy.util.XmlNodePrinter;
import org.codehaus.groovy.runtime.InvokerHelper;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Duplication of Job DSL <a
 * href="https://github.com/jenkinsci/job-dsl-plugin/blob/job-dsl-1.52/job-dsl-core/src/main/groovy/javaposse/jobdsl/dsl/MissingPropertyToStringDelegate.groovy"
 * >MissingPropertyToStringDelegate</a> to avoid coupling to non-public API. Code was modified to
 * account for Java compilation.
 *
 * <p>
 * Works like NodeBuilder, but in the context of a parent node. Used as the delegate for configure block closures.
 * </p>
 */
public class MissingPropertyToStringDelegate extends GroovyObjectSupport {
    private static final Logger LOGGER = Logger.getLogger(MissingPropertyToStringDelegate.class.getName());
    private Node root;

    public MissingPropertyToStringDelegate(Node root) {
        this.root = root;
    }
    /**
     * Make string for div() to do lookup.
     */
    protected String propertyMissing(String propertyName) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(String.format("Missing %s", propertyName));
        }
        return propertyName;
    }

    protected String toXml(Node n) throws Exception {
        try (StringWriter writer = new StringWriter()) {
            try (final PrintWriter printWriter = new PrintWriter(writer)) {
                new XmlNodePrinter(printWriter).print(n);
                return writer.toString();
            }
        }
    }

    protected Node methodMissing(String methodName, Object args) throws Exception {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(String.format("Method missing for %s %s", methodName, args));
        }

        InvokerHelper.invokeMethod(args, "each", new Closure(null) {
            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked when the closure gets called")
            protected void doCall(final Object it) {
                if (it instanceof Closure) {
                    // Node Builder will make a better delegate than ourselves
                    ((Closure) it).setResolveStrategy(Closure.DELEGATE_FIRST);
                }
            }
        });
        NodeBuilder b = new NodeBuilder();
        Node newNode = (Node) b.invokeMethod(methodName, args);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(String.format("Missing %s created %s", methodName, toXml(newNode)));
        }
        return newNode;
    }
}
