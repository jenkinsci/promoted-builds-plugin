package hudson.plugins.promoted_builds.integrations.jobdsl;

import groovy.lang.Closure;
import groovy.util.Node;

import java.util.HashMap;
import java.util.Map;

import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.dsl.helpers.BuildParametersContext;

import javaposse.jobdsl.plugin.DslEnvironment;

import javax.annotation.CheckForNull;

import static javaposse.jobdsl.plugin.ContextExtensionPoint.executeInContext;

public class ConditionsContext implements Context {
	private final DslEnvironment dslEnvironment;

	// Self Promotion Condition
	private boolean isSelfPromotion = false;
	
	private boolean evenIfUnstable = false;
	
	// Parametzerized Self Promotion Condition
	private boolean isParameterizedSelfPromotion = false;
	
	private boolean evenIfUnstableParameterized = false;
	
	private String parameterName = null;
	
	private String parameterValue = null;
	
	// Manual Promotion Condition
	private boolean isManual = false;
	
	private String users = null;
	
	final Map<String, Node> params = new HashMap<String, Node>();
	
	// Release Build Condition
	private boolean isReleaseBuild = false;
	
	// Downstream Build Condition
	private boolean isDownstreamPass = false;
	
	private boolean evenIfUnstableDownstream = false;
	
	private String jobs = null;
	
	// Upstream Build Condition
	private boolean isUpstreamPromotion = false;
	
	private String promotionNames = null;

	public ConditionsContext(DslEnvironment dslEnvironment) {
		this.dslEnvironment = dslEnvironment;
	}

	public void selfPromotion(@CheckForNull Boolean evenIfUnstable) {
		isSelfPromotion = true;
		if (evenIfUnstable != null && evenIfUnstable) {
			this.evenIfUnstable = true;
		}
	}

	public void parameterizedSelfPromotion(@CheckForNull Boolean evenIfUnstable, String parameterName, String parameterValue) {
		isParameterizedSelfPromotion = true;
		if (evenIfUnstable != null && evenIfUnstable) {
			this.isParameterizedSelfPromotion = true;
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

	public void downstream(@CheckForNull Boolean evenIfUnstable, String jobs) {
		isDownstreamPass = true;
		if (evenIfUnstable != null && evenIfUnstable) {
			this.evenIfUnstableDownstream = true;
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

}
