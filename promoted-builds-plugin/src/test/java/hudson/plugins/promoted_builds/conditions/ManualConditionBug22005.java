package hudson.plugins.promoted_builds.conditions;

import hudson.ExtensionList;
import hudson.model.FreeStyleBuild;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.ParameterDefinition;
import hudson.model.StringParameterDefinition;
import hudson.plugins.promoted_builds.Status;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotedBuildAction;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.conditions.ManualCondition.ManualApproval;

import java.io.IOException;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import static com.gargoylesoftware.htmlunit.html.HtmlFormUtil.submit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

@Issue("JENKINS-22005")
public class ManualConditionBug22005 {

    @Rule
    public JenkinsRule j = new JenkinsRule();

	private PromotionProcess createPromotionProcess(JobPropertyImpl parent, String name) throws IOException{
        PromotionProcess prom0 = parent.addProcess(name);
        ManualCondition prom0ManualCondition=new ManualCondition();
        prom0ManualCondition.getParameterDefinitions().add(new StringParameterDefinition("param1", prom0.getName()+"_value_1", "Parameter 1"));
        prom0ManualCondition.getParameterDefinitions().add(new StringParameterDefinition("param2", prom0.getName()+"_value_2", "Parameter 2"));
        prom0.conditions.add(prom0ManualCondition);
        return prom0;
	}

	@Test
	public void testPromotionProcess() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        
        ExtensionList<Descriptor> list = j.jenkins.getExtensionList(Descriptor.class);
        list.add(new JobPropertyImpl.DescriptorImpl(JobPropertyImpl.class));
        JobPropertyImpl base =  new JobPropertyImpl(p);
        p.addProperty(base);
        PromotionProcess prom0=createPromotionProcess(base, "PROM0");
        ManualCondition prom0Condition=prom0.conditions.get(ManualCondition.class);
        PromotionProcess prom1=createPromotionProcess(base, "PROM1");
        ManualCondition prom1Condition=prom1.conditions.get(ManualCondition.class);
        PromotionProcess prom2=createPromotionProcess(base, "PROM2");
        ManualCondition prom2Condition=prom2.conditions.get(ManualCondition.class);
        
        FreeStyleBuild b1 = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        Promotion p0b1=j.assertBuildStatusSuccess(prom0Condition.approve(b1, prom0));
        assertEquals(2,p0b1.getParameterValues().size());
        assertEquals(2,p0b1.getParameterDefinitionsWithValue().size());
        
        Promotion p1b1=j.assertBuildStatusSuccess(prom1Condition.approve(b1, prom1));
        assertEquals(2,p1b1.getParameterValues().size());
        assertEquals(2,p1b1.getParameterDefinitionsWithValue().size());
        
        Promotion p2b1=j.assertBuildStatusSuccess(prom2Condition.approve(b1, prom2));
        assertEquals(2,p2b1.getParameterValues().size());
        assertEquals(2,p2b1.getParameterDefinitionsWithValue().size());
        
        List<ManualApproval> approvals=b1.getActions(ManualApproval.class);
        assertEquals(3, approvals.size());
        
        PromotedBuildAction promBuildAction=b1.getAction(PromotedBuildAction.class);
        List<Status> statuses=promBuildAction.getPromotions();
        assertEquals(3, statuses.size());
        
        for (Status status:statuses){
        	Promotion lastBuild=status.getLast();
        	List<ParameterDefinition> lastBuildParameters=lastBuild.getParameterDefinitionsWithValue();
        	assertEquals(2, lastBuildParameters.size());
        }
	}

    @Test
    public void testPromotionProcessViaWebClient() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        
        ExtensionList<Descriptor> list = j.jenkins.getExtensionList(Descriptor.class);
        list.add(new JobPropertyImpl.DescriptorImpl(JobPropertyImpl.class));
        JobPropertyImpl base =  new JobPropertyImpl(p);
        p.addProperty(base);
        createPromotionProcess(base, "PROM0");
        createPromotionProcess(base, "PROM1");
        createPromotionProcess(base, "PROM2");
        
        
        FreeStyleBuild b1 = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        assertNull(b1.getAction(ManualApproval.class));
        HtmlPage page=j.createWebClient().getPage(b1, "promotion");
        //Approve Promotion
        List<HtmlForm> forms=ManualConditionTest.getFormsByName(page, "approve");
        assertFalse(forms.isEmpty());
        assertEquals(3, forms.size());
        for (HtmlForm form:forms){
        	submit(form);
        }
        
        //reload promotions page
        page=j.createWebClient().getPage(b1, "promotion");
        forms=ManualConditionTest.getFormsByName(page,"build");
        for (HtmlForm form:forms){
        	List<HtmlElement> parameters=ManualConditionTest.getFormParameters(form);
        	assertEquals(2, parameters.size());
        }
    }
}
