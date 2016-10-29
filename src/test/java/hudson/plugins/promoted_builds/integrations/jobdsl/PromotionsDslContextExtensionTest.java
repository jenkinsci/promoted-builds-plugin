package hudson.plugins.promoted_builds.integrations.jobdsl;

import com.google.common.io.Files;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.model.queue.QueueTaskFuture;
import javaposse.jobdsl.plugin.ExecuteDslScripts;
import javaposse.jobdsl.plugin.RemovedJobAction;
import org.apache.commons.io.FileUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.For;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@For(PromotionsExtensionPoint.class)
public class PromotionsDslContextExtensionTest {

    private class XPathMatcher extends BaseMatcher<File> {
        private final String xPathExpression;
        private final String expectedValue;

        /**
         * @param xPathExpression Never null. Will be evaluated as {@link XPathConstants#STRING}
         * @param expectedValue Never null. Will be compared ({@link #equals(Object)}) to the result of the XPath expression evaluation
         */
        public XPathMatcher(String xPathExpression, String expectedValue) {
            this.xPathExpression = xPathExpression;
            this.expectedValue = expectedValue;
        }

        @Override
        public boolean matches(Object item) {
            try {
                final File file = (File) item;

                final DocumentBuilder documentBuilder = documentBuilderFactory.get().newDocumentBuilder();

                final XPath xPath = xPathFactory.get().newXPath();
                final XPathExpression xPathExpression = xPath.compile(this.xPathExpression);

                final Document document = documentBuilder.parse(file);
                final Object evaluate = xPathExpression.evaluate(document, XPathConstants.STRING);

                return expectedValue.equals(evaluate);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Result of ").appendValue(xPathExpression).appendText(" XPath evaluation equals to ").appendValue(expectedValue);
        }

        @Override
        public void describeMismatch(Object item, Description description) {
            try {
                description.appendText("Parsed XML (").appendValue(item).appendText(") is ").appendValue(Files.toString((File) item, Charset.forName("UTF-8")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Rule
    public JenkinsRule j = new JenkinsRule();

    // DocumentBuilderFactory is not thread-safe
    private ThreadLocal<DocumentBuilderFactory> documentBuilderFactory = new ThreadLocal<DocumentBuilderFactory>() {
        @Override
        protected DocumentBuilderFactory initialValue() {
            return DocumentBuilderFactory.newInstance();
        }
    };

    // XPathFactory is not thread-safe
    private ThreadLocal<XPathFactory> xPathFactory = new ThreadLocal<XPathFactory>() {
        @Override
        protected XPathFactory initialValue() {
            return XPathFactory.newInstance();
        }
    };

    @Test
    public void testShouldGenerateTheDefindedJob() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/example-dsl.groovy"));
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(
                new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When        
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        j.assertBuildStatusSuccess(scheduleBuild2);
    }
    

    @Test
    public void testShouldGenerateTheDefindedComplexJob() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/complex-example-dsl.groovy"));
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(
                new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When        
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        j.assertBuildStatusSuccess(scheduleBuild2);
    }

    @Test
    public void testShouldGenerateTheCopyArtifactsJob() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/copyartifacts-example-dsl.groovy"));
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(
                new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then (unstable b/c we aren't including the CopyArtifacts dependency)
        j.assertBuildStatus(Result.UNSTABLE, scheduleBuild2.get());

        TopLevelItem item = j.jenkins.getItem("copy-artifacts-test");
        File config = new File(item.getRootDir(), "promotions/Development/config.xml");
        String content = Files.toString(config, Charset.forName("UTF-8"));
        assert content.contains("<selector class='hudson.plugins.copyartifact.SpecificBuildSelector'>");
    }

    @Test
    @Issue("JENKINS-39342")
    public void testAutomaticallyGeneratedDsl() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/automatically-generated-dsl.groovy"));
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(
            new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        j.assertBuildStatusSuccess(scheduleBuild2);
    }

    @Test
    @Issue("JENKINS-34982")
    public void testConfigureBlock() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/configure-block-dsl.groovy"));
        FreeStyleProject seedJob = j.createFreeStyleProject();
        seedJob.getBuildersList().add(
            new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        j.assertBuildStatusSuccess(scheduleBuild2);

        final Item createdJob = j.jenkins.getItemByFullName("configure-block-test");
        final File promotionConfig = new File(createdJob.getRootDir(), "promotions/PromotionName/config.xml");

        assertTrue("Configuration file must exist", promotionConfig.exists());
        assertThat(promotionConfig, new XPathMatcher("/*/buildSteps[1]/foo.bar.CustomAction[1]/customAttribute[1]/text()", "customValue"));
        assertThat(promotionConfig, new XPathMatcher("/*/buildSteps[1]/foo.bar.CustomAction[1]/@foo", "bar"));
    }

}
