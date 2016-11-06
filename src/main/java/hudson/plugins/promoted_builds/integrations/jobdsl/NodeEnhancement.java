package hudson.plugins.promoted_builds.integrations.jobdsl;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import groovy.lang.Closure;
import groovy.util.Node;
import groovy.util.NodeBuilder;
import groovy.util.NodeList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Duplication of Job DSL <a
 * href="https://github.com/jenkinsci/job-dsl-plugin/blob/job-dsl-1.52/job-dsl-core/src/main/groovy/javaposse/jobdsl/dsl/NodeEnhancement.groovy"
 * >NodeEnhancement</a> to avoid coupling to non-public API. Code was modified to account for Java
 * compilation.
 *
 * <p>
 * Add div and leftShift operators to Node.
 * * div - Will return the first child that matches name, and if it doesn't exists, it creates
 * * leftShift - Take node (or configure block to create) and appends as child, as opposed to plus which appends as a
 *               peer
 * </p>
 */
public class NodeEnhancement {
    private static final Logger LOGGER = Logger.getLogger(NodeEnhancement.class.getName());

    public static Node div(Node current, Node orphan) {
        final Node clonedOrphan = cloneNode(orphan);
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(String.format("Looking for child node %s", clonedOrphan));
        }
        final String childName = String.valueOf(clonedOrphan.name());
        List<Node> children = new ArrayList<>(Collections2.filter(current.children(), new Predicate() {
            @Override
            public boolean apply(@Nullable Object child) {
                return child instanceof Node && ((Node) child).name().equals(childName) &&
                    ((Node) child).attributes().entrySet().containsAll(clonedOrphan.attributes().entrySet());
            }
        }));
        if (children.size() == 0) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format("Creating node for %s", childName));
            }
            // Create node using just name
            current.append(clonedOrphan);
            return clonedOrphan;
        } else {
            // Return first childName, that's the contract for div
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format("Using first found childName for %s", childName));
            }
            final Node found = children.get(0);

            // Copy over value and attribute from orphan if it has one.
            if (clonedOrphan.value() != null) {
                found.setValue(clonedOrphan.value());
            }
            for (final Map.Entry entry : ((Map<Object, Object>) clonedOrphan.attributes()).entrySet()) {
                found.attributes().put(entry.getKey(), entry.getValue());
            }

            return found;
        }
    }

    public static Node div(Node current, final String childName) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(String.format("Looking for childName %s %s", childName, LOGGER.getLevel()));
        }

        List<Node> children = new ArrayList<>(Collections2.filter(current.children(), new Predicate() {
            @Override
            public boolean apply(@Nullable Object child) {
                return child instanceof Node && ((Node) child).name().equals(childName);
            }
        }));
        if (children.size() == 0) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format("Creating node for %s", childName));
            }
            // Create node using just name
            return current.appendNode(childName);
        } else {
            // Return first childName, that's the contract for div
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format("Using first found childName for %s", childName));
            }
            return children.get(0);
        }
    }

    private static List<Node> buildChildren(Object c) {
        NodeBuilder b = new NodeBuilder();
        Node newNode = (Node) b.invokeMethod("dummyNode", c);
        return newNode.children();
    }

    public static Node leftShift(Node current, boolean boolValue) {
        return leftShift(current, boolValue ? "true" : "false");
    }

    public static Node leftShift(Node current, String appendChildName) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(String.format("Setting value of %s for %s", appendChildName, current.name()));
        }
        current.setValue(appendChildName);
        return current;
    }

    public static Node leftShift(Node current, Node child) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(String.format("Appending node %s to %s", child, current));
        }
        current.append(cloneNode(child));
        return current;
    }

    public static Node leftShift(final Node current, Closure configureBlock) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(String.format("Appending block from %s", configureBlock));
        }
        configureBlock.setResolveStrategy(Closure.DELEGATE_FIRST);
        List<Node> newChildren = buildChildren(configureBlock);
        for (Node it : newChildren) {
            current.append(it);
        }
        return current;
    }

    /**
     * Creates a new Node with the same name, no parent, shallow cloned attributes
     * and if the value is a NodeList, a (deep) clone of those nodes.
     *
     * @return the clone
     */
    // can be replaced by Node#clone() when using a Groovy release that contains fixes for GROOVY-5682 and GROOVY-7044
    private static Node cloneNode(Node node) {
        Object newValue = node.value();
        if (newValue instanceof List) {
            newValue = cloneNodeList((List) newValue);
        }
        Map attributes = !node.attributes().isEmpty() ? new HashMap(node.attributes()) : new HashMap<>();
        return new Node(null, node.name(), attributes, newValue);
    }

    /**
     * Creates a new NodeList containing the same elements as the
     * original (but cloned in the case of Nodes).
     *
     * @return the clone
     */
    private static List cloneNodeList(List nodeList) {
        List result = nodeList instanceof NodeList ? new NodeList(nodeList.size()) : new ArrayList(nodeList.size());
        for (Object next: nodeList) {
            if (next instanceof Node) {
                result.add(cloneNode(((Node) next)));
            } else {
                result.add(next);
            }
        }
        return result;
    }
}
