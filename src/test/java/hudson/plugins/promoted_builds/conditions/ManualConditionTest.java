package hudson.plugins.promoted_builds.conditions;

import hudson.ExtensionList;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleBuild;
import hudson.model.Item;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.Result;
import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.model.StringParameterDefinition;
import hudson.model.User;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotedBuildAction;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.PromotionProcess;
import hudson.plugins.promoted_builds.Status;
import hudson.plugins.promoted_builds.conditions.ManualCondition.ManualApproval;
import hudson.security.ACL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.acegisecurity.context.SecurityContext;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.MockAuthorizationStrategy;

import static com.gargoylesoftware.htmlunit.html.HtmlFormUtil.submit;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
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

    @Test
    @Issue("SECURITY-190")
    public void testManualPromotionPermissions() throws Exception {
        enableSecurity(j);
        FreeStyleProject p = j.createFreeStyleProject();
        PromotionProcess pp = addPromotionProcess(p, "foo");
        ManualCondition cond = new ManualCondition();
        pp.conditions.add(cond);

        {
            // No approvers specified and user does not have Promotion/Promote
            FreeStyleBuild b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
            SecurityContext previous = ACL.impersonate(User.get("non-promoter").impersonate());
            cond.approve(b, pp, Collections.EMPTY_LIST);
            ACL.impersonate(previous.getAuthentication());
            ManualApproval approval = b.getAction(ManualApproval.class);
            assertThat("If no users are specified, then users without Promotion/Promote permissions should not be able to approve the promotion",
                    approval, nullValue());
        }

        {
            // No approvers specified and user does have Promotion/Promote
            FreeStyleBuild b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
            SecurityContext previous = ACL.impersonate(User.get("promoter").impersonate());
            j.assertBuildStatusSuccess(cond.approve(b, pp, Collections.EMPTY_LIST));
            ACL.impersonate(previous.getAuthentication());
            ManualApproval approval = b.getAction(ManualApproval.class);
            assertThat("If no users are specified, then users with Promotion/Promote permissions should be able to approve the promotion",
                    approval, notNullValue());
        }

        {
            // Approvers specified, user is approver, but does not have Promotion/Promote
            cond.setUsers("non-promoter");
            FreeStyleBuild b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
            SecurityContext previous = ACL.impersonate(User.get("non-promoter").impersonate());
            j.assertBuildStatusSuccess(cond.approve(b, pp, Collections.EMPTY_LIST));
            ACL.impersonate(previous.getAuthentication());
            ManualApproval approval = b.getAction(ManualApproval.class);
            assertThat("If users are specified, then users in that list should be able to approve even without Promotion/Promote permissions",
                    approval, notNullValue());
        }

        {
            // Approvers specified, user is not approver, but does have Promotion/Promote
            cond.setUsers("non-promoter");
            FreeStyleBuild b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
            SecurityContext previous = ACL.impersonate(User.get("promoter").impersonate());
            cond.approve(b, pp, Collections.EMPTY_LIST);
            ACL.impersonate(previous.getAuthentication());
            ManualApproval approval = b.getAction(ManualApproval.class);
            assertThat("If users are specified, then users not in the list should not be able to approve regardless of their permissions",
                    approval, nullValue());
        }
    }

    @Test
    public void testManualPromotionPermissionsViaWebClient() throws Exception {
        enableSecurity(j);
        FreeStyleProject p = j.createFreeStyleProject();
        PromotionProcess pp = addPromotionProcess(p, "foo");
        WebClient wc = j.createWebClient();

        FreeStyleBuild b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        ManualCondition cond = new ManualCondition();
        pp.conditions.add(cond);
        j.assertBuildStatusSuccess(cond.approve(b, pp, Collections.EMPTY_LIST));
        assertThat(b.getAction(ManualApproval.class), notNullValue());

        {
            // Re-execute promotion as user without Promotion/Promote when no users are specified
            wc.login("non-promoter", "non-promoter");
            // The build is not scheduled but we `return;` from the method in this case, which is why we use goTo with "" for the MIME type.
            wc.goTo(String.format("job/%s/%d/promotion/%s/build?json={}", p.getName(), b.getNumber(), pp.getName()), "");
            assertThat(pp.getBuildByNumber(2), nullValue());
        }

        {
            // Re-execute promotion as user with Promotion/Promote when no users are specified
            wc.login("promoter", "promoter");
            try {
                wc.getPage(b, String.format("promotion/%s/build?json={}", pp.getName()));
            } catch (FailingHttpStatusCodeException e) {
                assertThat(e.getStatusCode(), equalTo(404)); // Redirect after the build is broken.
            }
            assertThat(pp.getBuildByNumber(2).getResult(), equalTo(Result.SUCCESS));
        }

        {
            // Re-execute promotion as specified user without Promotion/Promote
            cond.setUsers("non-promoter");
            wc.login("non-promoter", "non-promoter");
            try {
                wc.getPage(b, String.format("promotion/%s/build?json={}", pp.getName()));
            } catch (FailingHttpStatusCodeException e) {
                assertThat(e.getStatusCode(), equalTo(404)); // Redirect after the build is broken.
            }
            assertThat(pp.getBuildByNumber(3).getResult(), equalTo(Result.SUCCESS));
        }

        {
            // Re-execute promotion as unspecified user with Promotion/Promote
            cond.setUsers("non-promoter");
            wc.login("promoter", "promoter");
            // The build is not scheduled but we `return;` from the method in this case, which is why we use goTo with "" for the MIME type.
            try {
                wc.goTo(String.format("job/%s/%d/promotion/%s/build?json={}", p.getName(), b.getNumber(), pp.getName()), "");
            } catch (FailingHttpStatusCodeException e) {
                assertThat(e.getStatusCode(), equalTo(404)); // Redirect after the build is broken.
            }
            assertThat(pp.getBuildByNumber(4), nullValue());
        }
    }

    private PromotionProcess addPromotionProcess(AbstractProject<?,?> owner, String name) throws Exception {
        ExtensionList<Descriptor> list = j.jenkins.getExtensionList(Descriptor.class);
        list.add(new JobPropertyImpl.DescriptorImpl(JobPropertyImpl.class));
        JobPropertyImpl base = new JobPropertyImpl(owner);
        owner.addProperty(base);
        return base.addProcess(name);
    }

    private static void enableSecurity(JenkinsRule j) {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        MockAuthorizationStrategy mas = new MockAuthorizationStrategy();
        mas.grant(Item.BUILD, Item.READ, Jenkins.READ)
                .everywhere()
                .to("non-promoter", "promoter");
        mas.grant(Promotion.PROMOTE)
                .everywhere()
                .to("promoter");
        j.jenkins.setAuthorizationStrategy(mas);
    }
}
