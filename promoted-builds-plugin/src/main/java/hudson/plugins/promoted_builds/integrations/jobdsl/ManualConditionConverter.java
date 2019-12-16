package hudson.plugins.promoted_builds.integrations.jobdsl;

import groovy.util.Node;

import java.util.Collection;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * XStream Converter for the ManualCondition for the Job DSL Plugin
 * 
 * @author Dennis Schulte
 */
public class ManualConditionConverter extends ReflectionConverter {

    public ManualConditionConverter(Mapper mapper, ReflectionProvider reflectionProvider) {
        super(mapper, reflectionProvider);
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
        return type.equals(JobDslManualCondition.class);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        JobDslManualCondition mc = (JobDslManualCondition) source;
        if (mc.getUsers() != null) {
            writer.startNode("users");
            writer.setValue(mc.getUsers());
            writer.endNode();
        }
        writer.startNode("parameterDefinitions");
        Collection<Node> parameterDefinitionNodes = mc.getParameterDefinitionNodes();
        if(parameterDefinitionNodes != null && !parameterDefinitionNodes.isEmpty()){        
            for (Node node : mc.getParameterDefinitionNodes()) {
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
        }
        writer.endNode();
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

}
