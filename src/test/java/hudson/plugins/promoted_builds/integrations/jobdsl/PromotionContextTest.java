package hudson.plugins.promoted_builds.integrations.jobdsl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Closure;
import javaposse.jobdsl.dsl.Item;
import javaposse.jobdsl.dsl.JobManagement;
import javaposse.jobdsl.dsl.MemoryJobManagement;
import javaposse.jobdsl.dsl.helpers.step.StepContext;
import javaposse.jobdsl.dsl.jobs.FreeStyleJob;
import javaposse.jobdsl.plugin.DslEnvironmentImpl;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.For;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@For(PromotionContext.class)
public class PromotionContextTest {

    private final XmlHelper xmlHelper = new XmlHelper();

    private JobManagement jobManagement;
    private Item item;

    @Before
    public void setUp() throws Exception {
        jobManagement = new MemoryJobManagement();
        item = new FreeStyleJob(jobManagement);
    }

    /**
     * When mocking with {@link MemoryJobManagement}, extensions are not available so we cannot test automatically generated DSL usage
     *
     * @throws Exception
     */
    @Test
    public void shouldGenerateValidXml() throws Exception {

        final PromotionContext promotionContext = new PromotionContext(jobManagement, item, new DslEnvironmentImpl(jobManagement, item));

        final Object thisObject = new Object();
        final Object owner = new Object();

        promotionContext.label("!master && linux");
        promotionContext.icon("yellow-unicorn");
        promotionContext.conditions(new Closure(owner, thisObject) {
            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked")
            public void doCall() {
                assertThat("Wrong conditions() closure delegate", getDelegate(), new IsInstanceOf(ConditionsContext.class));
                // When using {@link MemoryJobManagement}, extensions are not available so we cannot test automatically generated DSL usage
            }
        });
        promotionContext.actions(new Closure(owner, thisObject) {
            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked")
            public void doCall() {
                assertThat("Wrong actions() closure delegate", getDelegate(), new IsInstanceOf(StepContext.class));
            }
        });

        final String xml = promotionContext.getXml();

        assertNotNull(xml);
        assertThat(xml, xmlHelper.newStringXPathMatcher("name(/*)", "hudson.plugins.promoted__builds.PromotionProcess"));
        assertThat(xml, xmlHelper.newStringXPathMatcher("/*/assignedLabel[1]/text()", "!master && linux"));
        assertThat(xml, xmlHelper.newStringXPathMatcher("/*/icon/text()", "yellow-unicorn"));

//        // Given
//        JobDslPromotionProcess pp = new JobDslPromotionProcess();
//        //Conditions
//        List<Node> conditions = new ArrayList<Node>();
//        conditions.add(XSTREAM.toXML(new SelfPromotionCondition(true)));
//        //BuildSteps
//        List<Node> buildSteps = new ArrayList<Node>();
//        Node node = new Node(null, "hudson.tasks.Shell");
//        Node subNode = new Node(node, "command");
//        buildSteps.add(node);
//        subNode.setValue("echo hello;");
//        Node node2 = new Node(null, "hudson.plugins.parameterizedtrigger.TriggerBuilder");
//        Node subNode2 = new Node(node2, "configs");
//        Node subsubNode2 = new Node(subNode2, "hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig");
//        Node subsubsubNode2a = new Node(subsubNode2, "projects");
//        subsubsubNode2a.setValue("anoter-project");
//        Node subsubsubNode2b = new Node(subsubNode2, "condition");
//        subsubsubNode2b.setValue("ALWAYS");
//        buildSteps.add(node2);
//        pp.setBuildSteps(buildSteps);
//        pp.setConditions(conditions);
//        // When
//        XSTREAM.registerConverter(new JobDslPromotionProcessConverter(XSTREAM.getMapper(), XSTREAM.getReflectionProvider()));
//        String xml = XSTREAM.toXML(pp);
//        // Then
//        assertNotNull(xml);
//        System.out.println(xml);
//        assertTrue(StringUtils.contains(xml, "hudson.plugins.promoted__builds.PromotionProcess"));
    }
}
