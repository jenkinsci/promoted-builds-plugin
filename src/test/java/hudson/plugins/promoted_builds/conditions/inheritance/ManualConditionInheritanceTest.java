package hudson.plugins.promoted_builds.conditions.inheritance;

import hudson.ExtensionList;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.Descriptor;
import hudson.model.StringParameterDefinition;
import hudson.plugins.project_inheritance.projects.InheritanceBuild;
import hudson.plugins.project_inheritance.projects.InheritanceProject.IMode;
import hudson.plugins.promoted_builds.JobPropertyImpl;
import hudson.plugins.promoted_builds.PromotedBuildAction;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.PromotionProcess;

import hudson.plugins.promoted_builds.conditions.ManualCondition;
import hudson.plugins.promoted_builds.conditions.ManualCondition.ManualApproval;
import hudson.plugins.promoted_builds.inheritance.helpers.InheritanceProjectRule;
import hudson.plugins.promoted_builds.inheritance.helpers.InheritanceProjectsPair;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

import org.htmlunit.html.HtmlInput;
import org.junit.Rule;
import org.junit.Test;

import jenkins.model.Jenkins;

import org.htmlunit.html.HtmlElement;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;

/**
 * @author Jacek Tomaka
 */
public class ManualConditionInheritanceTest {
    @Rule 
    public InheritanceProjectRule j = new InheritanceProjectRule();
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
        InheritanceProjectsPair inheritanceProjectsPair = j.createInheritanceProjectDerivedWithBase();

        ExtensionList<Descriptor> list=Jenkins.get().getExtensionList(Descriptor.class);
        list.add(new JobPropertyImpl.DescriptorImpl(JobPropertyImpl.class));
        JobPropertyImpl base =  new JobPropertyImpl(inheritanceProjectsPair.getBase());
        inheritanceProjectsPair.getBase().addProperty(base);
        PromotionProcess foo = base.addProcess("foo");

        ManualCondition condition=new ManualCondition();
        condition.getParameterDefinitions().add(new StringParameterDefinition("bogus_string_param_1", "bogus_value_1", "Bog parameter"));
        condition.getParameterDefinitions().add(new StringParameterDefinition("bogus_string_param_2", "bogus_value_2", "Bog parameter"));
        foo.conditions.add(condition);

        InheritanceBuild b1 = j.assertBuildStatusSuccess(inheritanceProjectsPair.getDerived().scheduleBuild2(0));

        // promote a build

        List<ParameterValue> paramValues = condition.createDefaultValues();
        //try to add duplicate values
        paramValues.addAll(condition.createDefaultValues());
        //We cannot assume that the process will contain builds because the process added to base project is different to the one in derived. 
        JobPropertyImpl jobProperty = inheritanceProjectsPair.getDerived().getProperty(JobPropertyImpl.class, 
              /*Forcing inheritance as temporary hack for inheritance plugin 1.53 
                because that version of the plugin uses inheritance only for certain predefined cases: 
                -specific methods on the call stack
                -url paths.
                This has been changed as pull request https://github.com/i-m-c/jenkins-inheritance-plugin/pull/40
               */
                IMode.INHERIT_FORCED); 
        assertNotNull("derived jobProperty is null", jobProperty);
        PromotionProcess fooDerived = jobProperty.getItem("foo");
        
        j.assertBuildStatusSuccess(condition.approve(b1, fooDerived, paramValues));
        ManualApproval manualApproval=b1.getAction(ManualApproval.class);
        assertNotNull(manualApproval);

        PromotedBuildAction statuses=b1.getAction(PromotedBuildAction.class);
        assertNotNull(statuses);
        assertNotNull(statuses.getPromotions());
        assertFalse(statuses.getPromotions().isEmpty());
    }


    @Test
    public void testManualPromotionProcessViaWebClient() throws Exception {
        InheritanceProjectsPair inheritanceProjectsPair = j.createInheritanceProjectDerivedWithBase();

        ExtensionList<Descriptor> list=Jenkins.get().getExtensionList(Descriptor.class);
        list.add(new JobPropertyImpl.DescriptorImpl(JobPropertyImpl.class));
        JobPropertyImpl base =  new JobPropertyImpl(inheritanceProjectsPair.getBase());
        inheritanceProjectsPair.getDerived().addProperty(base);
        PromotionProcess foo = base.addProcess("foo");
        ManualCondition condition=new ManualCondition();
        condition.getParameterDefinitions().add(new StringParameterDefinition("bogus_string_param_1", "bogus_value_1", "Bog parameter"));
        condition.getParameterDefinitions().add(new StringParameterDefinition("bogus_string_param_2", "bogus_value_2", "Bog parameter"));
        foo.conditions.add(condition);

        InheritanceBuild b1 = j.assertBuildStatusSuccess(inheritanceProjectsPair.getDerived().scheduleBuild2(0));
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
        j.waitUntilNoActivity();
         //We cannot assume that the process will contain builds because the process added to base project is different to the one in derived. 
        final JobPropertyImpl jobProperty = inheritanceProjectsPair.getDerived().getProperty(JobPropertyImpl.class, 
              /*Forcing inheritance as temporary hack for inheritance plugin 1.53 
                because that version of the plugin uses inheritance only for certain predefined cases: 
                -specific methods on the call stack
                -url paths.
                This has been changed as pull request https://github.com/i-m-c/jenkins-inheritance-plugin/pull/40
               */
                IMode.INHERIT_FORCED); 
        assertNotNull("derived jobProperty is null", jobProperty);
        final PromotionProcess fooDerived = jobProperty.getItem("foo");
        ManualApproval approval=b1.getAction(ManualApproval.class);
        assertNotNull(approval);
        SortedMap<Integer, Promotion> builds=fooDerived.getBuildsAsMap();
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
        j.waitUntilNoActivity();
        final JobPropertyImpl jobProperty2 = inheritanceProjectsPair.getDerived().getProperty(JobPropertyImpl.class, 
                /*Forcing inheritance as temporary hack for inheritance plugin 1.53 
                  because that version of the plugin uses inheritance only for certain predefined cases: 
                  -specific methods on the call stack
                  -url paths.
                  This has been changed as pull request https://github.com/i-m-c/jenkins-inheritance-plugin/pull/40
                 */
                  IMode.INHERIT_FORCED); 
        assertNotNull("derived jobProperty is null", jobProperty2);
        final PromotionProcess fooDerived2 = jobProperty2.getItem("foo");
        
        builds=fooDerived2.getBuildsAsMap();
        assertEquals(2, builds.size());
        List<ManualApproval> actions=b1.getActions(ManualApproval.class);
        assertEquals(1, actions.size());

        PromotedBuildAction buildActions=b1.getAction(PromotedBuildAction.class);
        int buildIndex=1;
        String valueSufix="1";
        List<Promotion> promotions=new ArrayList<Promotion>();
        promotions.addAll(builds.values());

        Collections.reverse(promotions);
        for (Promotion build:promotions){
            List<ParameterDefinition> values=build.getParameterDefinitionsWithValue();
            assertEquals(values.size(), condition.getParameterDefinitions().size());
            for (ParameterDefinition v:values){
                assertTrue(v instanceof StringParameterDefinition);
                String pvalue=((StringParameterDefinition)v).getDefaultValue();
                assertTrue(pvalue.endsWith(valueSufix));
            }
            buildIndex++;
            valueSufix+=buildIndex;
        }

    }
}
