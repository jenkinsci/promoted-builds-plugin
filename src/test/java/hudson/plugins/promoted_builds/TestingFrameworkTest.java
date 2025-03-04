package hudson.plugins.promoted_builds;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This is not a regular unit test: it validates the testing framework by making sure we can rely on it
 */
class TestingFrameworkTest {

    @Test
    @Issue("JENKINS-39362")
    void testNegativeMatcher() {
        assertThrows(
                AssertionError.class,
                () -> assertThat(null, new BaseMatcher<>() {
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
                })
        );
    }

    @Test
    void testPositiveMatcher() {
        assertDoesNotThrow(
                () -> assertThat(null, new BaseMatcher<>() {
	                @Override
	                public void describeTo(Description description) {
		                description.appendText("Always true matcher");
	                }

	                @Override
	                public boolean matches(Object item) {
		                return true;
	                }
                })
        );
    }

}
