package hudson.plugins.promoted_builds.conditions;

import hudson.ExtensionList;
import hudson.model.FreeStyleBuild;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.StringParameterDefinition;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotedBuildAction;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.Status;
import hudson.plugins.promoted_builds.conditions.ManualCondition.ManualApproval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;

import static com.gargoylesoftware.htmlunit.html.HtmlFormUtil.submit;
import static org.junit.Assert.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class ManualConditionTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

	public static List<HtmlForm> getFormsByName(HtmlPage page, String name){
        List<HtmlForm> forms=new ArrayList<HtmlForm>();
        for (HtmlForm f:page.getForms()){
        	if (name.equals(f.getNameAttribute())){
        		forms.add(f);
        	}
        }
        return forms;
	}
	public static List<HtmlElement> getFormParameters(HtmlForm form){
		return form.getElementsByAttribute("div", "name", "parameter");
	}

	@Test
    public void testManualPromotionProcess() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        
        ExtensionList<Descriptor> list = j.jenkins.getExtensionList(Descriptor.class);
        list.add(new JobPropertyImpl.DescriptorImpl(JobPropertyImpl.class));
        JobPropertyImpl base =  new JobPropertyImpl(p);
        p.addProperty(base);
        PromotionProcess foo = base.addProcess("foo");
        
        ManualCondition condition=new ManualCondition();
        condition.getParameterDefinitions().add(new StringParameterDefinition("bogus_string_param_1", "bogus_value_1", "Bog parameter"));
        condition.getParameterDefinitions().add(new StringParameterDefinition("bogus_string_param_2", "bogus_value_2", "Bog parameter"));
        foo.conditions.add(condition);
        
        FreeStyleBuild b1 = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        
        // promote a build
        
        List<ParameterValue> paramValues = condition.createDefaultValues();
        //try to add duplicate values
        paramValues.addAll(condition.createDefaultValues());
        
        j.assertBuildStatusSuccess(condition.approve(b1, foo, paramValues));
        ManualApproval manualApproval=b1.getAction(ManualApproval.class);
        assertNotNull(manualApproval);
        
        PromotedBuildAction statuses=b1.getAction(PromotedBuildAction.class);
        assertNotNull(statuses);
        assertNotNull(statuses.getPromotions());
        assertFalse(statuses.getPromotions().isEmpty());
    }
    
    @Issue("SECURITY-170")
    @Test
    /**
     * Verify that the plugin is tolerant against SECURITY-170 in Manual conditions
     */
    public void testManualPromotionProcessWithInvalidParam() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        ExtensionList<Descriptor> list = j.jenkins.getExtensionList(Descriptor.class);
        list.add(new JobPropertyImpl.DescriptorImpl(JobPropertyImpl.class));
        JobPropertyImpl base =  new JobPropertyImpl(p);
        p.addProperty(base);
        PromotionProcess foo = base.addProcess("foo");
        
        ManualCondition condition=new ManualCondition();
        condition.getParameterDefinitions().add(new StringParameterDefinition("FOO", "BAR", "Test parameter"));
        foo.conditions.add(condition);
        
        FreeStyleBuild b1 = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        
        // Promote a build. Also add one invalid parameter 
        List<ParameterValue> paramValues = condition.createDefaultValues();
        paramValues.add(new StringParameterValue("INVALID_PARAM", "hacked!"));
        j.assertBuildStatusSuccess(condition.approve(b1, foo, paramValues));
        ManualApproval manualApproval = b1.getAction(ManualApproval.class);
        assertNotNull(manualApproval);
        List<ParameterValue> parameterValues = manualApproval.badge.getParameterValues();
        
        // Verify that the build succeeds && has no INVALID_PARAM
        PromotedBuildAction statuses=b1.getAction(PromotedBuildAction.class);
        assertNotNull(statuses);
        assertNotNull(statuses.getPromotions());
        assertFalse(statuses.getPromotions().isEmpty());
        Promotion pb = base.getItem("foo").getBuildByNumber(1);
        assertNotNull("INVALID_PARAM should not be injected into the environment", 
                pb.getEnvironment(TaskListener.NULL).get("INVALID_PARAM", null));
    }

    @Test
    public void testManualPromotionProcessViaWebClient() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        
        ExtensionList<Descriptor> list = j.jenkins.getExtensionList(Descriptor.class);
        list.add(new JobPropertyImpl.DescriptorImpl(JobPropertyImpl.class));
        JobPropertyImpl base =  new JobPropertyImpl(p);
        p.addProperty(base);
        PromotionProcess foo = base.addProcess("foo");
        ManualCondition condition=new ManualCondition();
        condition.getParameterDefinitions().add(new StringParameterDefinition("bogus_string_param_1", "bogus_value_1", "Bog parameter"));
        condition.getParameterDefinitions().add(new StringParameterDefinition("bogus_string_param_2", "bogus_value_2", "Bog parameter"));
        foo.conditions.add(condition);
        
        FreeStyleBuild b1 = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        assertNull(b1.getAction(ManualApproval.class));
        HtmlPage page=j.createWebClient().getPage(b1, "promotion");
        //Approve Promotion
        List<HtmlForm> forms=getFormsByName(page, "approve");
        assertFalse(forms.isEmpty());
        assertTrue(forms.size()==1);
        
        HtmlForm form=forms.get(0);
        List<HtmlElement> parameters=getFormParameters(form);
        assertTrue(parameters.size()==condition.getParameterDefinitions().size());
        for(HtmlElement param:parameters){
        	HtmlElement v=param.getElementsByAttribute("input", "name", "value").get(0);
        	v.setAttribute("value", v.getAttribute("value")+"1");
        }
        submit(forms.get(0));
        
        ManualApproval approval=b1.getAction(ManualApproval.class);
        assertNotNull(approval);
        SortedMap<Integer, Promotion> builds=foo.getBuildsAsMap();
        assertNotNull(builds);
        assertTrue(builds.size()==1);
        
        //Re-Execute approved promotion
        page=j.createWebClient().getPage(b1, "promotion");
        forms=getFormsByName(page,"build");
        assertFalse(forms.isEmpty());
        assertTrue(forms.size()==1);
        form=forms.get(0);
        parameters=getFormParameters(form);
        assertTrue(parameters.size()==condition.getParameterDefinitions().size());
        
        for(HtmlElement param:parameters){
        	HtmlElement v=param.getElementsByAttribute("input", "name", "value").get(0);
        	v.setAttribute("value", v.getAttribute("value")+"2");
        }
        submit(form);
        
        builds=foo.getBuildsAsMap();
        assertTrue(builds.size()==2);
        List<ManualApproval> actions=b1.getActions(ManualApproval.class);
        assertTrue(actions.size()==1);
        
        PromotedBuildAction buildActions=b1.getAction(PromotedBuildAction.class);
        int buildIndex=1;
        String valueSufix="1";
        List<Promotion> promotions=new ArrayList<Promotion>();
        promotions.addAll(builds.values());
        
        Collections.reverse(promotions);
        for (Promotion build:promotions){
        	List<ParameterDefinition> values=build.getParameterDefinitionsWithValue();
        	assertTrue(values.size()==condition.getParameterDefinitions().size());
        	for (ParameterDefinition v:values){
        		assertTrue(v instanceof StringParameterDefinition);
        		String pvalue=((StringParameterDefinition)v).getDefaultValue();
        		assertTrue(pvalue.endsWith(valueSufix));
        	}
        	buildIndex++;
        	valueSufix+=buildIndex;
        }
        
        for (Status status:buildActions.getPromotions()){
        	assertNotNull(status.getLast()!=null);
        	List<ParameterDefinition> values=status.getLast().getParameterDefinitionsWithValue();
        	assertTrue(values.size()==condition.getParameterDefinitions().size());
        }
        
    }
}
