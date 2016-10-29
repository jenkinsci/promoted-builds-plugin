package hudson.plugins.promoted_builds;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.Issue;

import static org.junit.Assert.assertThat;

/**
 * This is not a regular unit test: it validates the testing framework by making sure we can rely on it
 */
public class TestingFrameworkTest {

    /**
     * Allow special processing of AssertionError
     */
    private static class TestMonitoringRule implements TestRule {

        boolean expectFailure = false;

        @Override
        public Statement apply(final Statement base, final org.junit.runner.Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    boolean testSuccess = false;
                    try {
                        base.evaluate();
                        testSuccess = true;
                    } catch (AssertionError e) {
                        if (!expectFailure) {
                            throw e;
                        }
                        // expected failure, discard assertion error
                    }
                    if (expectFailure && testSuccess) {
                        // if we get here, that means we are not processing a failing test (the purpose of this class)
                        throw new AssertionError("This must test the processing of failed Matcher, not the well-being of system under tests");
                    }
                }
            };
        }
    }

    @Rule
    public TestMonitoringRule rule = new TestMonitoringRule();

    @Test
    @Issue("JENKINS-39362")
    public void testNegativeMatcher() throws Throwable {
        rule.expectFailure = true;
        assertThat(null, new BaseMatcher<Object>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Always false matcher");
            }
            @Override
            public boolean matches(Object item) {
                // returning false indicates the test failure
                // JENKINS-39362 is caused by errors during the processing of that failure
                return false;
            }
        });
    }

    @Test
    public void testPositiveMatcher() throws Throwable {
        assertThat(null, new BaseMatcher<Object>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Always true matcher");
            }
            @Override
            public boolean matches(Object item) {
                return true;
            }
        });
    }

}
