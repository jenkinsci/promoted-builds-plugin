package hudson.plugins.promoted_builds.integrations.jobdsl;

import groovy.lang.Closure;
import groovy.lang.MetaClass;
import groovy.util.Node;
import groovy.util.XmlParser;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import hudson.plugins.promoted_builds.conditions.ParameterizedSelfPromotionCondition;
import hudson.plugins.promoted_builds.conditions.SelfPromotionCondition;
import hudson.plugins.promoted_builds.conditions.UpstreamPromotionCondition;
import javaposse.jobdsl.dsl.*;
import javaposse.jobdsl.dsl.helpers.BuildParametersContext;
import javaposse.jobdsl.plugin.DslEnvironment;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static javaposse.jobdsl.plugin.ContextExtensionPoint.executeInContext;

@ContextType("hudson.plugins.promoted_builds.PromotionCondition")
public class ConditionsContext extends AbstractExtensibleContext {

	/**
	 * Used as a migration path toward Automatically Generated DSL usage
	 */
	public class ParametersContext implements Context {
		Map<String, Node> buildParameterNodes;

		public void parameters(@DslContext(BuildParametersContext.class) Closure<?> closure) {
			final BuildParametersContext context = dslEnvironment.createContext(BuildParametersContext.class);
			executeInContext(closure, context);
			this.buildParameterNodes = context.getBuildParameterNodes();
		}
	}

	private static final String DEPRECATION_MODEL_SERIALIZATION_WARNING = "Automatically Generated DSL should be used";

	// never persist the MetaClass
	private transient MetaClass metaClass;

	/** Used for migration purpose to retain compatibility with previous implementation. */
	private transient XmlParser xmlParser;

	protected final DslEnvironment dslEnvironment;

	final List<Node> conditionNodes = new ArrayList<>();

	public ConditionsContext(JobManagement jobManagement, Item item, DslEnvironment dslEnvironment) {
		super(jobManagement, item);
		this.dslEnvironment = dslEnvironment;
		this.metaClass = InvokerHelper.getMetaClass(this.getClass());
	}

	/**
	 * @param evenIfUnstable
	 * @deprecated Use Automatically Generated DSL for syntax consistency
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	@Deprecated
	public void selfPromotion(Boolean evenIfUnstable) throws ParserConfigurationException, SAXException, IOException {
		jobManagement.logDeprecationWarning(DEPRECATION_MODEL_SERIALIZATION_WARNING);
		addCondition(new SelfPromotionCondition(evenIfUnstable));
	}

	/**
	 * @param evenIfUnstable
	 * @param parameterName
	 * @param parameterValue
	 * @deprecated Use Automatically Generated DSL for syntax consistency
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	@Deprecated
	public void parameterizedSelfPromotion(Boolean evenIfUnstable, String parameterName, String parameterValue) throws ParserConfigurationException, SAXException, IOException {
		jobManagement.logDeprecationWarning(DEPRECATION_MODEL_SERIALIZATION_WARNING);
		addCondition(new ParameterizedSelfPromotionCondition(evenIfUnstable, parameterName, parameterValue));
	}

	/**
	 * @param users
	 * @deprecated Use Automatically Generated DSL for syntax consistency
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @see #manual(String, Closure)
	 */
	@Deprecated
	public void manual(String users) throws ParserConfigurationException, SAXException, IOException {
		jobManagement.logDeprecationWarning(DEPRECATION_MODEL_SERIALIZATION_WARNING);
		doManual(users, null);
	}

	/**
	 * @param users
	 * @param parametersClosure
	 * @deprecated Use Automatically Generated DSL for syntax consistency
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	@Deprecated
	public void manual(String users, @DslContext(ParametersContext.class) Closure<?> parametersClosure) throws ParserConfigurationException, SAXException, IOException {
		jobManagement.logDeprecationWarning(DEPRECATION_MODEL_SERIALIZATION_WARNING);
		doManual(users, parametersClosure);
	}

	private void doManual(String users, @DslContext(ParametersContext.class) Closure<?> parametersClosure) throws ParserConfigurationException, SAXException, IOException {
		JobDslManualCondition condition = new JobDslManualCondition();
		condition.setUsers(users);
		if (parametersClosure != null) {
			final ParametersContext context = new ParametersContext();
			executeInContext(parametersClosure, context);
			condition.setParameterDefinitionNodes(context.buildParameterNodes.values());
		}
		addCondition(condition);
	}

	/**
	 * @deprecated Use Automatically Generated DSL for syntax consistency
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	@Deprecated
	public void releaseBuild() throws ParserConfigurationException, SAXException, IOException {
		jobManagement.logDeprecationWarning(DEPRECATION_MODEL_SERIALIZATION_WARNING);
		addCondition(new ReleasePromotionCondition());
	}

	/**
	 * @param parametersClosure
	 * @deprecated This method doesn't achieve anything (was wrongly exposed at a higher context). See
	 * 	{@link ParametersContext#parameters(Closure)}.
	 */
	@Deprecated
	public void parameters(Closure<?> parametersClosure) {
		jobManagement.logDeprecationWarning("This method doesn't achieve anything");
	}

	/**
	 * @param evenIfUnstable
	 * @param jobs
	 * @deprecated Use Automatically Generated DSL for syntax consistency
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	@Deprecated
	public void downstream(Boolean evenIfUnstable, String jobs) throws ParserConfigurationException, SAXException, IOException {
		jobManagement.logDeprecationWarning(DEPRECATION_MODEL_SERIALIZATION_WARNING);
		addCondition(new DownstreamPassCondition(jobs, evenIfUnstable));
	}

	/**
	 * @param promotionNames
	 * @deprecated Use Automatically Generated DSL for syntax consistency
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	@Deprecated
	public void upstream(String promotionNames) throws ParserConfigurationException, SAXException, IOException {
		jobManagement.logDeprecationWarning(DEPRECATION_MODEL_SERIALIZATION_WARNING);
		addCondition(new UpstreamPromotionCondition(promotionNames));
	}

	/**
	 * Helper method to migrate from domain object serialization to direct XML generation from Automatically Generated
	 * DSL
	 *
	 * @param condition
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	protected void addCondition(PromotionCondition condition) throws ParserConfigurationException, SAXException, IOException {
		if (xmlParser == null) {
			xmlParser = new XmlParser();
		}
		addExtensionNode(xmlParser.parseText(PromotionsExtensionPoint.XSTREAM.toXML(condition)));
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
