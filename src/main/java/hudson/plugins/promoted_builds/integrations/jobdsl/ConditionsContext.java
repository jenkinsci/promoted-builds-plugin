package hudson.plugins.promoted_builds.integrations.jobdsl;

import groovy.lang.Closure;
import groovy.lang.MetaClass;
import groovy.util.Node;
import javaposse.jobdsl.dsl.AbstractExtensibleContext;
import javaposse.jobdsl.dsl.ContextType;
import javaposse.jobdsl.dsl.Item;
import javaposse.jobdsl.dsl.JobManagement;
import javaposse.jobdsl.dsl.helpers.BuildParametersContext;
import javaposse.jobdsl.plugin.DslEnvironment;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javaposse.jobdsl.plugin.ContextExtensionPoint.executeInContext;

@ContextType("hudson.plugins.promoted_builds.PromotionCondition")
public class ConditionsContext extends AbstractExtensibleContext {
	// never persist the MetaClass
	private transient MetaClass metaClass;

	protected final DslEnvironment dslEnvironment;

	final List<Node> conditionNodes = new ArrayList<Node>();

	// Self Promotion Condition
	private boolean isSelfPromotion;

	private boolean evenIfUnstable;

	// Parametzerized Self Promotion Condition
	private boolean isParameterizedSelfPromotion;

	private boolean evenIfUnstableParameterized;

	private String parameterName;

	private String parameterValue;

	// Manual Promotion Condition
	private boolean isManual;

	private String users;

	final Map<String, Node> params = new HashMap<String, Node>();

	// Release Build Condition
	private boolean isReleaseBuild;

	// Downstream Build Condition
	private boolean isDownstreamPass;

	private boolean evenIfUnstableDownstream;

	private String jobs;

	// Upstream Build Condition
	private boolean isUpstreamPromotion;

	private String promotionNames;

	public ConditionsContext(JobManagement jobManagement, Item item, DslEnvironment dslEnvironment) {
		super(jobManagement, item);
		this.dslEnvironment = dslEnvironment;
		this.metaClass = InvokerHelper.getMetaClass(this.getClass());
	}

	public void selfPromotion(Boolean evenIfUnstable) {
		isSelfPromotion = true;
		if (evenIfUnstable) {
			this.evenIfUnstable = evenIfUnstable;
		}
	}

	public void parameterizedSelfPromotion(Boolean evenIfUnstable, String parameterName, String parameterValue) {
		isParameterizedSelfPromotion = true;
		if (evenIfUnstable) {
			this.isParameterizedSelfPromotion = evenIfUnstable;
		}
		this.parameterName = parameterName;
		this.parameterValue = parameterValue;
	}

	public void manual(String users) {
		isManual = true;
		this.users = users;
	}

	public void manual(String users, Closure<?> parametersClosure) {
		isManual = true;
		this.users = users;
		parameters(parametersClosure);
	}

	public void releaseBuild() {
		isReleaseBuild = true;
	}

	public void parameters(Closure<?> parametersClosure) {
		// delegate to main BuildParametersContext
		BuildParametersContext parametersContext = dslEnvironment.createContext(BuildParametersContext.class);
		executeInContext(parametersClosure, parametersContext);
		params.putAll(parametersContext.getBuildParameterNodes());
	}

	public void downstream(Boolean evenIfUnstable, String jobs) {
		isDownstreamPass = true;
		if (evenIfUnstable) {
			this.evenIfUnstableDownstream = evenIfUnstable;
		}
		this.jobs = jobs;
	}

	public void upstream(String promotionNames) {
		isUpstreamPromotion = true;
		this.promotionNames = promotionNames;
	}

	public Map<String, Node> getParams() {
		return params;
	}

	public boolean isSelfPromotion() {
		return isSelfPromotion;
	}

	public boolean isEvenIfUnstable() {
		return evenIfUnstable;
	}

	public boolean isParameterizedSelfPromotion() {
		return isParameterizedSelfPromotion;
	}

	public boolean isEvenIfUnstableParameterized() {
		return evenIfUnstableParameterized;
	}

	public String getParameterName() {
		return parameterName;
	}

	public String getParameterValue() {
		return parameterValue;
	}

	public boolean isManual() {
		return isManual;
	}

	public String getUsers() {
		return users;
	}

	public boolean isReleaseBuild() {
		return isReleaseBuild;
	}

	public boolean isDownstreamPass() {
		return isDownstreamPass;
	}

	public boolean isEvenIfUnstableDownstream() {
		return evenIfUnstableDownstream;
	}

	public String getJobs() {
		return jobs;
	}

	public boolean isUpstreamPromotion() {
		return isUpstreamPromotion;
	}

	public String getPromotionNames() {
		return promotionNames;
	}

	@Override
	protected void addExtensionNode(Node node) {
		conditionNodes.add(node);
	}

	@Override
	public Object getProperty(String property) {
		return getMetaClass().getProperty(this, property);
	}

	@Override
	public void setProperty(String property, Object newValue) {
		getMetaClass().setProperty(this, property, newValue);
	}

	@Override
	public Object invokeMethod(String name, Object args) {
		return getMetaClass().invokeMethod(this, name, args);
	}

	@Override
	public MetaClass getMetaClass() {
		if (metaClass == null) {
			metaClass = InvokerHelper.getMetaClass(getClass());
		}
		return metaClass;
	}

	@Override
	public void setMetaClass(MetaClass metaClass) {
		this.metaClass = metaClass;
	}
}
