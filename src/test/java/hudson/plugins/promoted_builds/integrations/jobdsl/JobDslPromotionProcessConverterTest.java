package hudson.plugins.promoted_builds.integrations.jobdsl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;

import groovy.util.Node;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.conditions.SelfPromotionCondition;
import hudson.util.XStream2;

public class JobDslPromotionProcessConverterTest  {

    private static final XStream XSTREAM = new XStream2();
    
    @Test
    public void testShouldGenerateValidXml() throws Exception {
        // Given
        JobDslPromotionProcess pp = new JobDslPromotionProcess();
        //Conditions
        List<PromotionCondition> conditions = new ArrayList<PromotionCondition>();
        conditions.add(new SelfPromotionCondition(true));
        //BuildSteps
        List<Node> buildSteps = new ArrayList<Node>();
        Node node = new Node(null, "hudson.tasks.Shell");
        Node subNode = new Node(node, "command");
        buildSteps.add(node);
        subNode.setValue("echo hello;");
        Node node2 = new Node(null, "hudson.plugins.parameterizedtrigger.TriggerBuilder");
        Node subNode2 = new Node(node2, "configs");
        Node subsubNode2 = new Node(subNode2, "hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig");
        Node subsubsubNode2a = new Node(subsubNode2, "projects");
        subsubsubNode2a.setValue("anoter-project");
        Node subsubsubNode2b = new Node(subsubNode2, "condition");
        subsubsubNode2b.setValue("ALWAYS");
        buildSteps.add(node2);
        pp.setBuildSteps(buildSteps);
        pp.setConditions(conditions);
        // When
        XSTREAM.registerConverter(new JobDslPromotionProcessConverter(XSTREAM.getMapper(), XSTREAM.getReflectionProvider()));
        String xml = XSTREAM.toXML(pp);
        // Then
        assertNotNull(xml);
        System.out.println(xml);
        assertTrue(StringUtils.contains(xml, "hudson.plugins.promoted__builds.PromotionProcess"));
    }
}
