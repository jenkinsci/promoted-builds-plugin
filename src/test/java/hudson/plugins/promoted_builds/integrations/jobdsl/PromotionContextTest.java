package hudson.plugins.promoted_builds.integrations.jobdsl;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Closure;
import javaposse.jobdsl.dsl.Item;
import javaposse.jobdsl.dsl.JobManagement;
import javaposse.jobdsl.dsl.MemoryJobManagement;
import javaposse.jobdsl.dsl.helpers.BuildParametersContext;
import javaposse.jobdsl.dsl.helpers.step.StepContext;
import javaposse.jobdsl.dsl.jobs.FreeStyleJob;
import javaposse.jobdsl.plugin.DslEnvironmentImpl;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.For;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@For(PromotionContext.class)
public class PromotionContextTest {

    private final XmlHelper xmlHelper = new XmlHelper();

    private JobManagement jobManagement;
    private Item item;
    /** Tested class */
    private PromotionContext promotionContext;

    @Before
    public void setUp() throws Exception {
        jobManagement = new MemoryJobManagement();
        item = new FreeStyleJob(jobManagement);
        promotionContext = new PromotionContext(jobManagement, new DslEnvironmentImpl(jobManagement, item));
    }

    /**
     * When mocking with {@link MemoryJobManagement}, extensions are not available so we cannot test automatically generated DSL usage
     *
     * @throws Exception
     */
    @Test
    public void shouldGenerateValidXml() throws Exception {

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
    }

    /**
     * @see ReleasePromotionCondition
     */
    @Test
    @For(ReleasePromotionCondition.class)
    public void testReleaseConditionNameClashTrick() {
        promotionContext.conditions(new Closure(new Object()) {
            public void doCall() throws Exception {
                ((ConditionsContext) getDelegate()).releaseBuild();
            }
        });
        assertThat(promotionContext.getXml(), xmlHelper.newStringXPathMatcher("count(/*/conditions[1]/hudson.plugins.release.promotion.ReleasePromotionCondition)", "1"));
        assertThat(promotionContext.getXml(), xmlHelper.newStringXPathMatcher("count(/*/conditions[1]/hudson.plugins.promoted__builds.integrations.jobdsl.ReleasePromotionCondition)", "0"));
    }

    @Test
    @For({ManualConditionConverter.class, JobDslManualCondition.class})
    public void testManualConditionCustomSerializationTrick() {
        final String expectedUsers = "expectedUsers";
        final String expectedParameterName = "PARAMETER_NAME";
        promotionContext.conditions(new Closure(new Object()) {
            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked")
            public void doCall() throws Exception {
                ((ConditionsContext) getDelegate()).manual(expectedUsers, new Closure(this) {
                    @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked")
                    public void doCall() throws Exception {
                        ((ConditionsContext.ParametersContext) getDelegate()).parameters(new Closure(this) {
                            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked")
                            public void doCall() throws Exception {
                                ((BuildParametersContext) getDelegate()).stringParam(expectedParameterName);
                            }
                        });
                    }
                });
            }
        });
        assertThat(promotionContext.getXml(), xmlHelper.newStringXPathMatcher("/*/conditions[1]/hudson.plugins.promoted__builds.conditions.ManualCondition[1]/users[1]/text()", expectedUsers));
        assertThat(promotionContext.getXml(), xmlHelper.newStringXPathMatcher("/*/conditions[1]/hudson.plugins.promoted__builds.conditions.ManualCondition[1]/parameterDefinitions[1]/hudson.model.StringParameterDefinition[1]/name[1]/text()", expectedParameterName));
    }
}
