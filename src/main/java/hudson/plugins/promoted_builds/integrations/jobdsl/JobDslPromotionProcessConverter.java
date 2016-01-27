package hudson.plugins.promoted_builds.integrations.jobdsl;

import java.util.Collection;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.AbstractReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
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
                for (Node node : promotionProcess.getBuildSteps()) {
                    writer.startNode(node.name().toString());
                    if (node.value() instanceof Collection) {
                        for (Object subNode : (Collection) node.value()) {
                            convertNode((Node) subNode, writer);
                        }
                    } else {
                        writer.setValue(node.value().toString());
                    }
                    writer.endNode();
                }
                writer.endNode();
            }
        }
    }

    private void convertNode(Node node, HierarchicalStreamWriter writer) {
        writer.startNode(node.name().toString());
        if (node.value() instanceof Collection) {
            for (Object subNode : (Collection) node.value()) {
                convertNode((Node) subNode, writer);
            }
        } else {
            writer.setValue(node.value().toString());
        }
        writer.endNode();
    }

    private String obtainClassOwnership() {
        if (this.classOwnership != null) {
            return this.classOwnership;
        }
        if (pm == null) {
            Jenkins j = Jenkins.getInstance();
            if (j != null) {
                pm = j.getPluginManager();
            }
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
