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

import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlInput;
import org.htmlunit.html.HtmlPage;
import hudson.model.StringParameterValue;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import org.acegisecurity.context.SecurityContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.JenkinsRule.WebClient;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Kohsuke Kawaguchi
 */
@WithJenkins
public class ManualConditionTest {

	public static List<HtmlForm> getFormsByName(HtmlPage page, String name){
        List<HtmlForm> forms= new ArrayList<>();
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
    void testManualPromotionProcess(JenkinsRule j) throws Exception {
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

    /**
     * Verify that the plugin is tolerant against SECURITY-170 in Manual conditions
     */
    @Issue("SECURITY-170")
    @Test
    void testManualPromotionProcessWithInvalidParam(JenkinsRule j) throws Exception {
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
        assertNotNull(pb.getEnvironment(TaskListener.NULL).get("INVALID_PARAM", null),
                "INVALID_PARAM should not be injected into the environment");
    }

    @Test
    void testManualPromotionProcessViaWebClient(JenkinsRule j) throws Exception {
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
        assertEquals(1, forms.size());

        HtmlForm form=forms.get(0);
        List<HtmlElement> parameters=getFormParameters(form);
        assertEquals(parameters.size(), condition.getParameterDefinitions().size());
        for(HtmlElement param:parameters){
            HtmlInput v = (HtmlInput) param.getElementsByAttribute("input", "name", "value").get(0);
            v.setValue(v.getValue() + "1");
        }
        j.submit(forms.get(0));

        ManualApproval approval=b1.getAction(ManualApproval.class);
        assertNotNull(approval);
        SortedMap<Integer, Promotion> builds=foo.getBuildsAsMap();
        assertNotNull(builds);
        assertEquals(1, builds.size());

        //Re-Execute approved promotion
        page=j.createWebClient().getPage(b1, "promotion");
        forms=getFormsByName(page,"build");
        assertFalse(forms.isEmpty());
        assertEquals(1, forms.size());
        form=forms.get(0);
        parameters=getFormParameters(form);
        assertEquals(parameters.size(), condition.getParameterDefinitions().size());

        for(HtmlElement param:parameters){
            HtmlInput v = (HtmlInput) param.getElementsByAttribute("input", "name", "value").get(0);
            v.setValue(v.getValue() + "2");
        }
        j.submit(form);

        builds=foo.getBuildsAsMap();
        assertEquals(2, builds.size());
        List<ManualApproval> actions=b1.getActions(ManualApproval.class);
        assertEquals(1, actions.size());

        PromotedBuildAction buildActions=b1.getAction(PromotedBuildAction.class);
        int buildIndex=1;
        String valueSufix="1";
	    List<Promotion> promotions = new ArrayList<>(builds.values());

        Collections.reverse(promotions);
        for (Promotion build:promotions){
        	List<ParameterDefinition> values=build.getParameterDefinitionsWithValue();
                assertEquals(values.size(), condition.getParameterDefinitions().size());
        	for (ParameterDefinition v:values){
                assertInstanceOf(StringParameterDefinition.class, v);
        		String pvalue=((StringParameterDefinition)v).getDefaultValue();
        		assertTrue(pvalue.endsWith(valueSufix));
        	}
        	buildIndex++;
        	valueSufix+=buildIndex;
        }

        for (Status status:buildActions.getPromotions()){
        	assertNotNull(status.getLast());
        	List<ParameterDefinition> values=status.getLast().getParameterDefinitionsWithValue();
                assertEquals(values.size(), condition.getParameterDefinitions().size());
        }

    }

    @Test
    @Issue("SECURITY-190")
    void testManualPromotionPermissions(JenkinsRule j) throws Exception {
        enableSecurity(j);
        FreeStyleProject p = j.createFreeStyleProject();
        PromotionProcess pp = addPromotionProcess(j, p, "foo");
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

    // TODO figure out a good way to test this with SECURITY-2293
    @Test
    @Disabled
    void testManualPromotionPermissionsViaWebClient(JenkinsRule j) throws Exception {
        enableSecurity(j);
        FreeStyleProject p = j.createFreeStyleProject();
        PromotionProcess pp = addPromotionProcess(j, p, "foo");
        WebClient wc = j.createWebClient();

        FreeStyleBuild b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        ManualCondition cond = new ManualCondition();
        pp.conditions.add(cond);
        j.assertBuildStatusSuccess(cond.approve(b, pp, Collections.EMPTY_LIST));
        assertThat(b.getAction(ManualApproval.class), notNullValue());

        {
            // Re-execute promotion as user without Promotion/Promote when no users are specified
            wc.login("non-promoter", "non-promoter");
            // Status#doBuild does a bare `return;` without scheduling the build in this case, which is why we use goTo with "" for the MIME type.
            wc.goTo(String.format("job/%s/%d/promotion/%s/build?json={}", p.getName(), b.getNumber(), pp.getName()), "");
            assertThat(pp.getBuildByNumber(2), nullValue());
        }

        {
            // Re-execute promotion as user with Promotion/Promote when no users are specified
            wc.login("promoter", "promoter");
            try {
                wc.getPage(b, String.format("promotion/%s/build?json={}", pp.getName()));
                fail();
            } catch (FailingHttpStatusCodeException e) {
                assertThat(e.getStatusCode(), equalTo(404)); // Redirect after the build is broken.
            }
            assertThat(waitForBuildByNumber(pp, 2).getResult(), equalTo(Result.SUCCESS));
        }

        {
            // Re-execute promotion as specified user without Promotion/Promote
            cond.setUsers("non-promoter");
            wc.login("non-promoter", "non-promoter");
            try {
                wc.getPage(b, String.format("promotion/%s/build?json={}", pp.getName()));
                fail();
            } catch (FailingHttpStatusCodeException e) {
                assertThat(e.getStatusCode(), equalTo(404)); // Redirect after the build is broken.
            }
            assertThat(waitForBuildByNumber(pp, 3).getResult(), equalTo(Result.SUCCESS));
        }

        {
            // Re-execute promotion as unspecified user with Promotion/Promote
            cond.setUsers("non-promoter");
            wc.login("promoter", "promoter");
            // Status#doBuild does a bare `return;` without scheduling the build in this case, which is why we use goTo with "" for the MIME type.
            wc.goTo(String.format("job/%s/%d/promotion/%s/build?json={}", p.getName(), b.getNumber(), pp.getName()), "");
            assertThat(pp.getBuildByNumber(4), nullValue());
        }
    }

    private Promotion waitForBuildByNumber(PromotionProcess pp, int n) throws InterruptedException {
        for(int i = 0; i < 100; i++){
            Promotion promotion = pp.getBuildByNumber(n);
            if(promotion != null && promotion.getResult() != null){
                return promotion;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Timeout to retrieve the buildByNumber: " + n);
    }

    private PromotionProcess addPromotionProcess(JenkinsRule j, AbstractProject<?,?> owner, String name) throws Exception {
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
