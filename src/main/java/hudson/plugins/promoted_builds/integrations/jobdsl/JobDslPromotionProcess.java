package hudson.plugins.promoted_builds.integrations.jobdsl;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import groovy.util.Node;
import hudson.plugins.promoted_builds.PromotionCondition;

/**
 * Special holder for the PromotionProcess generated by the Job DSL Plugin
 * 
 * @author Dennis Schulte
 */
@XStreamAlias("hudson.plugins.promoted_builds.PromotionProcess")
public final class JobDslPromotionProcess {

    private String name;
    /**
     * The icon that represents this promotion process. This is the name of
     * the SVG icon that can be found in ${rootURL}/plugin/promoted-builds/icons/,
     * e.g. <code>"star-gold"</code>.
     */
    private String icon;

    /**
     * The label that promotion process can be run on.
     */
    private String assignedLabel;

    /**
     * {@link PromotionCondition}s. All have to be met for a build to be promoted.
     */
    private List<PromotionCondition> conditions = new ArrayList<PromotionCondition>();

    private List<Node> buildSteps = new ArrayList<Node>();
    
    private List<Node> buildWrappers = new ArrayList<Node>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getAssignedLabel() {
        return assignedLabel;
    }

    public void setAssignedLabel(String assignedLabel) {
        this.assignedLabel = assignedLabel;
    }

    public List<PromotionCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<PromotionCondition> conditions) {
        this.conditions = conditions;
    }

    public List<Node> getBuildSteps() {
        return buildSteps;
    }

    public void setBuildSteps(List<Node> buildSteps) {
        this.buildSteps = buildSteps;
    }
    
    public void setBuildWrappers(List<Node> buildWrappers) {
        this.buildWrappers = buildWrappers;
    }

    public List<Node> getBuildWrappers() {
        return buildWrappers;
    }

}
