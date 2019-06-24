package hudson.plugins.promoted_builds.conditions;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.*;
import hudson.plugins.promoted_builds.PromotionPermissionHelper;
import hudson.plugins.promoted_builds.PromotionBadge;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.PromotionConditionDescriptor;
import hudson.plugins.promoted_builds.Promotion;
import hudson.plugins.promoted_builds.PromotionProcess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import javax.servlet.ServletException;

import hudson.plugins.promoted_builds.pipeline.PromotionRun;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.acegisecurity.GrantedAuthority;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;


/**
 * {@link PromotionCondition} that requires manual promotion.
 *
 * @author Kohsuke Kawaguchi
 * @author Peter Hayes
 */
public class ManualCondition extends PromotionCondition {
    private String users;
    private List<ParameterDefinition> parameterDefinitions = new ArrayList<ParameterDefinition>();
    
    /**
     * 
     */
    public final static String MISSING_USER_ID_DISPLAY_STRING = "N/A";

    public ManualCondition() {
    }

    /*
     * Restrict the Condition to specific user(s)
     * @since 2.24
     */
    public String getUsers() {
        return users;
    }

    public void setUsers(String users) {
        this.users = users;
    }
    
    public List<ParameterDefinition> getParameterDefinitions() {
        return parameterDefinitions;
    }

    /**
     * Gets the {@link ParameterDefinition} of the given name, if any.
     */
    @CheckForNull
    public ParameterDefinition getParameterDefinition(String name) {
        if (parameterDefinitions == null) {
            return null;
        }

        for (ParameterDefinition pd : parameterDefinitions)
            if (pd.getName().equals(name))
                return pd;
        
        return null;
    }

    public Set<String> getUsersAsSet() {
        if (users == null || users.equals(""))
            return Collections.emptySet();

        Set<String> set = new HashSet<String>();
        for (String user : users.split(",")) {
            user = user.trim();

            if (user.trim().length() > 0)
                set.add(user);
        }
        
        return set;
    }

    @Override
    public PromotionBadge isMet(PromotionProcess promotionProcess, AbstractBuild<?,?> build) {
        List<ManualApproval> approvals = build.getActions(ManualApproval.class);

        for (ManualApproval approval : approvals) {
            if (approval.name.equals(promotionProcess.getName()))
                return approval.badge;
        }

        return null;
    }

    
    /**
     * Verifies that the currently logged in user (or anonymous) has permission
     * to approve the promotion and that the promotion has not already been
     * approved.
     */
    public boolean canApprove(PromotionProcess promotionProcess, AbstractBuild<?,?> build) {
        if (!PromotionPermissionHelper.hasPermission(build.getProject(), this)) {
            return false;
        }
        
        List<ManualApproval> approvals = build.getActions(ManualApproval.class);

        // For now, only allow approvals if this wasn't already approved
        for (ManualApproval approval : approvals) {
            if (approval.name.equals(promotionProcess.getName()))
                return false;
        }

        return true;
    }

    //TODO: updated the access level to public for reuse in another class
    /*
     * Check if user is listed in user list as a specific user
     * @since 2.24 
     */
    public boolean isInUsersList() {
        // Current user must be in users list or users list is empty
        Set<String> usersSet = getUsersAsSet();
        return usersSet.contains(Hudson.getAuthentication().getName());
    }

    /*
     * Check if user is a member of a groups as listed in the user / group field
     */
    public boolean isInGroupList() {
        Set<String> groups = getUsersAsSet();
        GrantedAuthority[] authorities = Hudson.getAuthentication().getAuthorities();
        for (GrantedAuthority authority : authorities) {
            if (groups.contains(authority.getAuthority()))
                return true;
        }
        return false;
    }
    
    @CheckForNull
    public Future<Promotion> approve(AbstractBuild<?,?> build, PromotionProcess promotionProcess, List<ParameterValue> paramValues) throws IOException{
        if (canApprove(promotionProcess, build)) {            
            // add approval to build
            ManualApproval approval=new ManualApproval(promotionProcess.getName(), paramValues);
            build.addAction(approval);
            build.save();

            // check for promotion
            return promotionProcess.considerPromotion2(build, approval);
        }
        return null;
    }
    public List<ParameterValue> createDefaultValues(){
        List<ParameterValue> paramValues = new ArrayList<ParameterValue>();

        if (parameterDefinitions != null && !parameterDefinitions.isEmpty()) {
            for (ParameterDefinition d:parameterDefinitions){
                paramValues.add(d.getDefaultParameterValue());
            }
        }
        return paramValues;
    }
    public Future<Promotion> approve(AbstractBuild<?,?> build, PromotionProcess promotionProcess) throws IOException{
        List<ParameterValue> paramValues = createDefaultValues();
        return approve(build, promotionProcess, paramValues);
    }

    /**
     * Web method to handle the approval action submitted by the user.
     */
    public void doApprove(StaplerRequest req, StaplerResponse rsp,
            @AncestorInPath PromotionProcess promotionProcess,
            @AncestorInPath AbstractBuild<?,?> build) throws IOException, ServletException {

    JSONObject formData = req.getSubmittedForm();

        if (canApprove(promotionProcess, build)) {
            List<ParameterValue> paramValues = new ArrayList<ParameterValue>();

            if (parameterDefinitions != null && !parameterDefinitions.isEmpty()) {
                JSONArray a = JSONArray.fromObject(formData.get("parameter"));

                for (Object o : a) {
                    JSONObject jo = (JSONObject) o;
                    String name = jo.getString("name");

                    ParameterDefinition d = getParameterDefinition(name);
                    if (d==null)
                        throw new IllegalArgumentException("No such parameter definition: " + name);
                    
                    paramValues.add(d.createValue(req, jo));
                }
            }
            approve(build, promotionProcess, paramValues);
        }

        rsp.sendRedirect2("../../../..");
    }

    /*
     * Used to annotate the build to indicate that it was manually approved.  This
     * is then looked for in the isMet method.
     */
    public static final class ManualApproval extends InvisibleAction {
        public String name;
        public Badge badge;

        public ManualApproval(String name, List<ParameterValue> values) {
            this.name = name;
            badge = new Badge(values);
        }
    }

    public static final class Badge extends PromotionBadge {
        public String authenticationName;
        private final List<ParameterValue> values;

        public Badge(List<ParameterValue> values) {
            this.authenticationName = Hudson.getAuthentication().getName();
            this.values = values;
        }

        /**
         * Gets name of the user, who has promoted the build.
         * @return User name or {@link #MISSING_USER_ID_DISPLAY_STRING} if the user cannot be determined
         */
        @Exported
        @Nonnull
        public String getUserName() {
            if (authenticationName == null)
                return MISSING_USER_ID_DISPLAY_STRING;

            User u = User.get(authenticationName, false, Collections.emptyMap());
            return u != null ? u.getDisplayName() : authenticationName;
        }

        /**
         * Gets ID of the user, who has promoted the build.
         * @return User id or {@link #MISSING_USER_ID_DISPLAY_STRING} if the user cannot be determined
         */
        @Exported
        public String getUserId() {
            if (authenticationName == null) {
                return MISSING_USER_ID_DISPLAY_STRING;
            }

            return authenticationName;
        }

        @Exported
        public List<ParameterValue> getParameterValues() {
            return values != null ? values : Collections.<ParameterValue>emptyList();
        }

        //TODO, TBD: Refactor API to PromotionRun ?
        @Override
        public void buildEnvVars(Run<?, ?> run, EnvVars env, TaskListener listener) {
            // TODO: Refactor to support Pipeline Promotion types
            if (!(run instanceof PromotionRun)) {
                throw new IllegalStateException ("Wrong build type. Expected a PromotionRun, but got "+run.getClass());
            }

            PromotionRun promotion = (PromotionRun)run;
            List<ParameterValue> params = ((PromotionRun) run).getParameterValues();
            if (params != null) {
                for (ParameterValue value : params) {
                    value.buildEnvironment(promotion.getPromotionRun(), env);
                }
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends PromotionConditionDescriptor {
        public boolean isApplicable(@Nonnull Job<?,?> item, @Nonnull TaskListener listener) {
            if(item instanceof AbstractProject){
                return isApplicable((AbstractProject)item);
            }
            return true;
        }

        @Deprecated
        public boolean isApplicable(AbstractProject<?,?> item) {
            return true;
        }

        public String getDisplayName() {
            return Messages.ManualCondition_DisplayName();
        }

        @Override
        public ManualCondition newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            ManualCondition instance = new ManualCondition();
            instance.users = formData.getString("users");
            instance.parameterDefinitions = Descriptor.newInstancesFromHeteroList(req, formData, "parameters", ParameterDefinition.all());
            return instance;
        }
    }
}

