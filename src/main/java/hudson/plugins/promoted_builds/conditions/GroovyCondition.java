package hudson.plugins.promoted_builds.conditions;

import groovy.lang.Binding;
import hudson.EnvVars;
import hudson.PluginManager;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.promoted_builds.PromotionBadge;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.PromotionProcess;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.scriptsecurity.sandbox.RejectedAccessException;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript;
import org.jenkinsci.plugins.scriptsecurity.scripts.UnapprovedClasspathException;
import org.jenkinsci.plugins.scriptsecurity.scripts.UnapprovedUsageException;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Allow specification of Groovy scripts to qualify builds. Script evaluation is done using the
 * <a href="https://wiki.jenkins-ci.org/display/JENKINS/Script+Security+Plugin">Script Security plugin</a>
 */
public class GroovyCondition extends PromotionCondition {
    private static final Logger LOGGER = Logger.getLogger(GroovyCondition.class.getName());

    @CheckForNull
    private final String unmetQualificationLabel;
    @CheckForNull
    private final String metQualificationLabel;
    private final SecureGroovyScript script;

    @DataBoundConstructor
    public GroovyCondition(final SecureGroovyScript script, final String unmetQualificationLabel, final String metQualificationLabel) {
        this.unmetQualificationLabel = Util.fixEmptyAndTrim(unmetQualificationLabel);
        this.metQualificationLabel = Util.fixEmptyAndTrim(metQualificationLabel);
        this.script = script.configuringWithNonKeyItem();
    }

    // exposed for config.jelly
    public SecureGroovyScript getScript() {
        return script;
    }

    // exposed for config.jelly
    public String getUnmetQualificationLabel() {
        return unmetQualificationLabel;
    }

    // exposed for config.jelly
    public String getMetQualificationLabel() {
        return metQualificationLabel;
    }

    // exposed for promotion status page
    public String getDisplayLabel() {
        return unmetQualificationLabel == null ? Messages.GroovyCondition_UnmetQualificationLabel() : unmetQualificationLabel;
    }

    @Override
    public PromotionBadge isMet(final PromotionProcess promotionProcess, final AbstractBuild<?, ?> build) {
        // TODO switch to Jenkins.getActiveInstance() once bumped to 1.590
        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins == null) {
            // Jenkins not started or shut down
            LOGGER.log(Level.WARNING, "Missing Jenkins instance");
            return null;
        }
        final PluginManager pluginManager = jenkins.getPluginManager();
        if (pluginManager == null) {
            LOGGER.log(Level.WARNING, "Unable to retrieve PluginManager");
            return null;
        }
        final ClassLoader classLoader = pluginManager.uberClassLoader;
        final Binding binding = new Binding();
        binding.setVariable("promotionProcess", promotionProcess);
        binding.setVariable("build", build);
        binding.setVariable("jenkins", jenkins);
        Object result = null;
        try {
            result = script.evaluate(classLoader, binding);
        } catch (final RejectedAccessException e) {
            LOGGER.log(Level.WARNING, "Sandbox exception", e);
            return null;
        } catch (final UnapprovedUsageException e) {
            LOGGER.log(Level.WARNING, "Unapproved script", e);
            return null;
        } catch (final UnapprovedClasspathException e) {
            LOGGER.log(Level.WARNING, "Unapproved classpath", e);
            return null;
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Evaluation error", e);
            return null;
        }
        final String displayLabel = metQualificationLabel == null ? Messages.GroovyCondition_MetQualificationLabel() : metQualificationLabel;
        if (Boolean.TRUE.equals(result)) {
            return new Badge(displayLabel, Collections.<String,String>emptyMap());
        } else if (result instanceof Map && !((Map) result).isEmpty()) {
            final Map<String, String> variables = new HashMap<String, String>(((Map) result).size());
            for (final Map.Entry entry : ((Map<Object, Object>) result).entrySet()) {
                final Object key = entry.getKey();
                final Object value = entry.getValue();
                if (key == null) {
                    continue;
                }
                variables.put(key.toString(), value == null ? "" : value.toString());
            }
            return new Badge(displayLabel, variables);
        }
        return null;
    }

    public static final class Badge extends PromotionBadge {
        private final String displayLabel;
        private final Map<String, String> variables;

        public Badge(final String displayLabel, final Map<String, String> variables) {
            this.displayLabel = displayLabel;
            this.variables = variables;
        }

        // exposed for Jelly
        public String getDisplayLabel() {
            return displayLabel;
        }

        @Override
        public void buildEnvVars(final Run<?, ?> build, final EnvVars env, TaskListener listener) {
            for (final Map.Entry<String, String> entry :
                    variables.entrySet()) {
                env.put(entry.getKey(), entry.getValue());
            }
        }
    }

}
