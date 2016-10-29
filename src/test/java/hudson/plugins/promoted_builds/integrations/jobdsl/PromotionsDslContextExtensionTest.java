package hudson.plugins.promoted_builds.integrations.jobdsl;

import com.google.common.io.Files;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Result;
import hudson.model.TopLevelItem;
import hudson.model.queue.QueueTaskFuture;

import java.io.File;
import java.nio.charset.Charset;

import javaposse.jobdsl.plugin.RemovedJobAction;
import javaposse.jobdsl.plugin.ExecuteDslScripts;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.w3c.dom.*;

public class PromotionsDslContextExtensionTest extends HudsonTestCase {

    @Test
    public void testShouldGenerateTheDefindedJob() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/example-dsl.groovy"));
        FreeStyleProject seedJob = createFreeStyleProject();
        seedJob.getBuildersList().add(
                new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When        
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        assertBuildStatusSuccess(scheduleBuild2);
    }
    

    @Test
    public void testShouldGenerateTheDefindedComplexJob() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/complex-example-dsl.groovy"));
        FreeStyleProject seedJob = createFreeStyleProject();
        seedJob.getBuildersList().add(
                new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When        
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        assertBuildStatusSuccess(scheduleBuild2);
    }

    @Test
    public void testShouldGenerateTheCopyArtifactsJob() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/copyartifacts-example-dsl.groovy"));
        FreeStyleProject seedJob = createFreeStyleProject();
        seedJob.getBuildersList().add(
                new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then (unstable b/c we aren't including the CopyArtifacts dependency)
        assertBuildStatus(Result.UNSTABLE, scheduleBuild2.get());

        TopLevelItem item = jenkins.getItem("copy-artifacts-test");
        File config = new File(item.getRootDir(), "promotions/Development/config.xml");
        String content = Files.toString(config, Charset.forName("UTF-8"));
        assert content.contains("<selector class='hudson.plugins.copyartifact.SpecificBuildSelector'>");
    }

    @Test
    public void testAutomaticallyGeneratedDsl() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/automatically-generated-dsl.groovy"));
        FreeStyleProject seedJob = createFreeStyleProject();
        seedJob.getBuildersList().add(
            new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        assertBuildStatusSuccess(scheduleBuild2);
    }

    @Test
    public void testConfigureBlock() throws Exception {
        // Given
        String dsl = FileUtils.readFileToString(new File("src/test/resources/configure-block-dsl.groovy"));
        FreeStyleProject seedJob = createFreeStyleProject();
        seedJob.getBuildersList().add(
            new ExecuteDslScripts(new ExecuteDslScripts.ScriptLocation(Boolean.TRUE.toString(), null, dsl), false, RemovedJobAction.DELETE));
        // When
        QueueTaskFuture<FreeStyleBuild> scheduleBuild2 = seedJob.scheduleBuild2(0);
        // Then
        assertBuildStatusSuccess(scheduleBuild2);

        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        final XPath xPath = XPathFactory.newInstance().newXPath();

        final Item createdJob = jenkins.getItemByFullName("configure-block-test");
        final File promotionConfig = new File(createdJob.getRootDir(), "promotions/PromotionName/config.xml");
        final String xml = Files.toString(promotionConfig, Charset.forName("UTF-8"));

        final Document document = documentBuilder.parse(promotionConfig);

        final String expression = "/*/buildSteps[1]/foo.bar.CustomAction[1]/customAttribute[1]/text()";
        final Object evaluate = xPath.compile(expression).evaluate(document, XPathConstants.STRING);

        assertEquals("Should contain customization but is \n" + xml, "customValue", evaluate);

        // does not run because of version inconsistencies in classpath: java.lang.NoSuchMethodError: org.hamcrest.Matcher.describeMismatch(Ljava/lang/Object;Lorg/hamcrest/Description;)V
//        Assert.assertThat(document, new BaseMatcher<Document>() {
//            @Override
//            public boolean matches(Object item) {
//                return false;
//            }
//
//            @Override
//            public void describeTo(Description description) {
//                description.appendText("Contain the expected XML customization");
//            }
//        });
    }
}
