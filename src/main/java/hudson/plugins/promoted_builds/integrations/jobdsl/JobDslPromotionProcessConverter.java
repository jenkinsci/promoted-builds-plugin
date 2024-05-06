package hudson.plugins.promoted_builds.integrations.jobdsl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.CheckForNull;

import org.apache.commons.lang.ObjectUtils;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import groovy.util.Node;
import hudson.PluginManager;
import hudson.PluginWrapper;
import jenkins.model.Jenkins;

/**
 * XStream Converter for the PromotionProcess for the Job DSL Plugin
 * 
 * @author Dennis Schulte
 */
public class JobDslPromotionProcessConverter extends ReflectionConverter {

    public JobDslPromotionProcessConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
        super(mapper, reflectionProvider);
    }

    private String classOwnership;

    private PluginManager pm;

    @Override
    public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
        return type.equals(JobDslPromotionProcess.class);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        JobDslPromotionProcess promotionProcess = (JobDslPromotionProcess) source;
        // attributes
        String plugin = obtainClassOwnership();
        if (plugin != null) {
            writer.addAttribute("plugin", plugin);
        }
        // nodes
        if (promotionProcess != null) {
            if (promotionProcess.getName() != null) {
                writer.startNode("name");
                writer.setValue(promotionProcess.getName());
                writer.endNode();
            }
            if (promotionProcess.getIcon() != null) {
                writer.startNode("icon");
                writer.setValue(promotionProcess.getIcon());
                writer.endNode();
            }
            if (promotionProcess.getAssignedLabel() != null) {
                String assignedLabel = promotionProcess.getAssignedLabel();
                if (assignedLabel != null) {
                    writer.startNode("assignedLabel");
                    writer.setValue(assignedLabel);
                    writer.endNode();
                }
            }
            if (promotionProcess.getConditions() != null) {
                writer.startNode("conditions");
                context.convertAnother(promotionProcess.getConditions());
                writer.endNode();
            }
            if (promotionProcess.getBuildSteps() != null) {
                writer.startNode("buildSteps");
                writeNodes(writer, promotionProcess.getBuildSteps());
                writer.endNode();
            }
            if (promotionProcess.getBuildWrappers() != null) {
                writer.startNode("buildWrappers");
                writeNodes(writer, promotionProcess.getBuildWrappers());
                writer.endNode();
            }
        }
    }

    private void writeNodes(HierarchicalStreamWriter writer, List<Node> nodes) {
        for (Node node : nodes) {
            writeNode(node, writer);
        }
    }

    private void writeNode(Node node, HierarchicalStreamWriter writer) {
        writer.startNode(node.name().toString());
        writeNodeAttributes(node, writer);
        if (node.value() instanceof Collection) {
            for (Object subNode : (Collection) node.value()) {
                if (subNode instanceof Node) {
                    writeNode((Node) subNode, writer);
                } else {
                    writer.setValue(subNode.toString());
                }
            }
        } else {
            writer.setValue(node.value().toString());
        }
        writer.endNode();
    }

    private void writeNodeAttributes(Node node, HierarchicalStreamWriter writer) {
        Map<?,?> attributes = node.attributes();
        if (attributes != null) {
            for (Map.Entry<?,?> entry : attributes.entrySet()) {
                String key = ObjectUtils.toString(entry.getKey());
                String value = ObjectUtils.toString(entry.getValue());
                writer.addAttribute(key, value);
            }
        }
    }

    @CheckForNull
    private String obtainClassOwnership() {
        if (this.classOwnership != null) {
            return this.classOwnership;
        }
        if (pm == null) {
            Jenkins j = Jenkins.getInstanceOrNull();
            pm = j != null ? j.getPluginManager() : null;
        }
        if (pm == null) {
            return null;
        }
        // TODO: possibly recursively scan super class to discover dependencies
        PluginWrapper p = pm.whichPlugin(hudson.plugins.promoted_builds.PromotionProcess.class);
        this.classOwnership = p != null ? p.getShortName() + '@' + trimVersion(p.getVersion()) : null;
        return this.classOwnership;
    }

    static String trimVersion(String version) {
        // TODO seems like there should be some trick with VersionNumber to do
        // this
        return version.replaceFirst(" .+$", "");
    }
}
