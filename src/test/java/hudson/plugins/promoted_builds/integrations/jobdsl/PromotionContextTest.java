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
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.For;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
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
        item = new FreeStyleJob(jobManagement, "foo");
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

        // For making sure the assertions put in inner classes get executed
        final Runnable conditionsChecker = Mockito.mock(Runnable.class);
        final Runnable actionsChecker = Mockito.mock(Runnable.class);

        promotionContext.label("label to be replaced");
        promotionContext.label("!master && linux");
        promotionContext.icon("icon to be replaced");
        promotionContext.icon("yellow-unicorn");
        promotionContext.conditions(new Closure(owner, thisObject) {
            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked")
            public void doCall() {
                conditionsChecker.run();
                assertThat("Wrong conditions() closure delegate", getDelegate(), instanceOf(ConditionsContext.class));
                // When using {@link MemoryJobManagement}, extensions are not available so we cannot test automatically generated DSL usage
            }
        });
        promotionContext.actions(new Closure(owner, thisObject) {
            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked")
            public void doCall() {
                actionsChecker.run();
                assertThat("Wrong actions() closure delegate", getDelegate(), instanceOf(StepContext.class));
            }
        });

        Mockito.verify(conditionsChecker).run();
        Mockito.verify(actionsChecker).run();

        final String xml = promotionContext.getXml();

        assertThat(xml, allOf(
            notNullValue(),
            xmlHelper.newStringXPathMatcher("name(/*)", "hudson.plugins.promoted__builds.PromotionProcess"),
            xmlHelper.newStringXPathMatcher("count(/*/assignedLabel)", "1"),
            xmlHelper.newStringXPathMatcher("/*/assignedLabel[1]/text()", "!master && linux"),
            xmlHelper.newStringXPathMatcher("count(/*/icon)", "1"),
            xmlHelper.newStringXPathMatcher("/*/icon/text()", "yellow-unicorn")
        ));

        Mockito.validateMockitoUsage();
    }

    @Test
    public void testReleaseCondition() throws Throwable {
        promotionContext.conditions(new Closure(new Object()) {
            public void doCall() throws Exception {
                ((ConditionsContext) getDelegate()).releaseBuild();
            }
        });
        assertThat(promotionContext.getXml(), allOf(
            xmlHelper.newStringXPathMatcher("count(/*/conditions[1]/hudson.plugins.release.promotion.ReleasePromotionCondition)", "1"),
            xmlHelper.newStringXPathMatcher("count(/*/conditions[1]/hudson.plugins.promoted__builds.integrations.jobdsl.ReleasePromotionCondition)", "0")
        ));
    }

    @Test
    public void testManualConditionCustomSerializationTrick() throws Throwable {
        final String expectedUsers = "expectedUsers";
        final String expectedParameterName = "PARAMETER_NAME";
        final String expectedParameter2Name = "UNICORNS";
        final String expectedParameter2Description = "ELEPHANTS";
        promotionContext.conditions(new Closure(new Object()) {
            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked")
            public void doCall() throws Exception {
                ((ConditionsContext) getDelegate()).manual(expectedUsers, new Closure(this) {
                    @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked")
                    public void doCall() throws Exception {
                        ((ConditionsContext.ManualPromotionContext) getDelegate()).parameters(new Closure(this) {
                            @SuppressFBWarnings(value = "UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", justification = "Dynamically invoked")
                            public void doCall() throws Exception {
                                ((BuildParametersContext) getDelegate()).stringParam(expectedParameterName);
                                ((BuildParametersContext) getDelegate()).booleanParam(expectedParameter2Name, true, expectedParameter2Description);
                            }
                        });
                    }
                });
            }
        });
        assertThat(promotionContext.getXml(), allOf(
            xmlHelper.newStringXPathMatcher("/*/conditions[1]/hudson.plugins.promoted__builds.conditions.ManualCondition[1]/users[1]/text()", expectedUsers),
            xmlHelper.newStringXPathMatcher("count(/*/conditions[1]/hudson.plugins.promoted__builds.conditions.ManualCondition[1]/parameterDefinitions[1]/*)", "2"),
            xmlHelper.newStringXPathMatcher("/*/conditions[1]/hudson.plugins.promoted__builds.conditions.ManualCondition[1]/parameterDefinitions[1]/hudson.model.StringParameterDefinition[1]/name[1]/text()", expectedParameterName),
            xmlHelper.newStringXPathMatcher("/*/conditions[1]/hudson.plugins.promoted__builds.conditions.ManualCondition[1]/parameterDefinitions[1]/hudson.model.BooleanParameterDefinition[1]/name[1]/text()", expectedParameter2Name),
            xmlHelper.newStringXPathMatcher("/*/conditions[1]/hudson.plugins.promoted__builds.conditions.ManualCondition[1]/parameterDefinitions[1]/hudson.model.BooleanParameterDefinition[1]/description[1]/text()", expectedParameter2Description)
        ));
    }
}
