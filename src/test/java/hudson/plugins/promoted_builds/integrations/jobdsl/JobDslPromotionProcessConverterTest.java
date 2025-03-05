package hudson.plugins.promoted_builds.integrations.jobdsl;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.XStream;

import groovy.util.Node;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.conditions.SelfPromotionCondition;
import hudson.util.XStream2;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;

class JobDslPromotionProcessConverterTest {

    private static final XStream XSTREAM = new XStream2();

    @Test
    void testShouldGenerateValidXml() {
        // Given
        JobDslPromotionProcess pp = new JobDslPromotionProcess();
        //Conditions
        List<PromotionCondition> conditions = new ArrayList<>();
        conditions.add(new SelfPromotionCondition(true));
        //BuildSteps
        List<Node> buildSteps = new ArrayList<>();
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
        List<Node> buildWrappers = new ArrayList<>();
        Node bwNode = new Node(null, "hudson.test.buildwrapper.ExampleWrapper");
        buildWrappers.add(bwNode);
        pp.setBuildWrappers(buildWrappers);
        // When
        XSTREAM.registerConverter(new JobDslPromotionProcessConverter(XSTREAM.getMapper(), XSTREAM.getReflectionProvider()));
        XSTREAM.processAnnotations(new Class<?>[] {JobDslPromotionProcess.class});
        String xml = XSTREAM.toXML(pp);
        // Then
        assertThat(xml,
                   allOf(notNullValue(),
                         containsString("hudson.plugins.promoted__builds.PromotionProcess"),
                         matchesPattern("(?s).*<buildWrappers>\\s+<hudson\\.test\\.buildwrapper\\.ExampleWrapper/>\\s+</buildWrappers>.*")));
    }
}
