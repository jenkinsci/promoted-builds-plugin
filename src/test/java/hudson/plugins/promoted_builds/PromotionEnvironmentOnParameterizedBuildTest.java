package hudson.plugins.promoted_builds;

import hudson.model.AbstractBuild;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.model.TaskListener;
import hudson.plugins.promoted_builds.conditions.SelfPromotionCondition;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;

public class PromotionEnvironmentOnParameterizedBuildTest {

	private static final String PARAM_NAME = "paramOne";
	private static final String PARAM_VALUE = "13.13";
	private static final String PROMOTION_PROCESS_NAME = "PromoProcess";

	@Rule public JenkinsRule jenkins = new JenkinsRule();

	private FreeStyleProject project;

	@Test
	public void promotionEnvironmentShouldContainParametersPassedToParentBuild() throws Exception {
		givenParameterizedProjectToBePromoted();
		givenPromotionProcessForProject();

		FreeStyleBuild build = jenkins.assertBuildStatusSuccess(project.scheduleBuild2(0));

		Promotion promotion = thenPromotionIsComplete();
		assertThat(promotion.getEnvironment(TaskListener.NULL).get(PARAM_NAME), is(equalTo(PARAM_VALUE)));
		assertThat(promotion.getTarget(), is(sameInstance((AbstractBuild) build)));
	}

	private void givenParameterizedProjectToBePromoted() throws IOException {
		project = jenkins.createFreeStyleProject();
		StringParameterDefinition paramOne = new StringParameterDefinition(PARAM_NAME, PARAM_VALUE);
		project.addProperty(new ParametersDefinitionProperty(paramOne));
	}

	private void givenPromotionProcessForProject() throws Descriptor.FormException, IOException {
		JobPropertyImpl promotionJobProperty = new JobPropertyImpl(project);
		PromotionProcess promotionProcess = promotionJobProperty.addProcess(PROMOTION_PROCESS_NAME);
		promotionProcess.conditions.add(new SelfPromotionCondition(false));
		project.addProperty(promotionJobProperty);
		project.save();
	}

	private Promotion thenPromotionIsComplete() {
		JobPropertyImpl promotionProperty = project.getProperty(JobPropertyImpl.class);
		PromotionProcess promotionProcess = promotionProperty.getItem(PROMOTION_PROCESS_NAME);
		assertThat(promotionProcess.getBuilds(), hasSize(1));
		return promotionProcess.getFirstBuild();
	}
}