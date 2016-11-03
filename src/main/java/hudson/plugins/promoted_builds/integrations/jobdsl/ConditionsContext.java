package hudson.plugins.promoted_builds.integrations.jobdsl;

import com.thoughtworks.xstream.XStreamException;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.MetaClass;
import groovy.util.Node;
import groovy.util.NodeBuilder;
import groovy.util.NodeList;
import groovy.util.XmlParser;
import hudson.model.Items;
import hudson.plugins.promoted_builds.PromotionCondition;
import hudson.plugins.promoted_builds.conditions.DownstreamPassCondition;
import hudson.plugins.promoted_builds.conditions.ManualCondition;
import hudson.plugins.promoted_builds.conditions.ParameterizedSelfPromotionCondition;
import hudson.plugins.promoted_builds.conditions.SelfPromotionCondition;
import hudson.plugins.promoted_builds.conditions.UpstreamPromotionCondition;
import javaposse.jobdsl.dsl.AbstractExtensibleContext;
import javaposse.jobdsl.dsl.Context;
import javaposse.jobdsl.dsl.ContextType;
import javaposse.jobdsl.dsl.DslContext;
import javaposse.jobdsl.dsl.JobManagement;
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

/**
 * Context for defining {@link PromotionCondition promotion conditions}.
 */
@ContextType("hudson.plugins.promoted_builds.PromotionCondition")
public class ConditionsContext extends AbstractExtensibleContext {

	/**
	 * @see #manual(String, Closure)
	 */
	public class ManualPromotionContext implements Context {
		Map<String, Node> buildParameterNodes;

		public void parameters(@DslContext(BuildParametersContext.class) @DelegatesTo(BuildParametersContext.class) Closure<?> closure) {
			final BuildParametersContext context = dslEnvironment.createContext(BuildParametersContext.class);
			executeInContext(closure, context);
			this.buildParameterNodes = context.getBuildParameterNodes();
		}
	}

	/**
	 * Used to integrate this Java class with Groovy meta protocol
	 *
	 * @see groovy.lang.GroovyObjectSupport#metaClass
	 */
	// never persist the MetaClass
	private transient MetaClass metaClass;

	/**
	 * Used to generate the required memory node structure from an XML representation.
	 */
	private transient XmlParser xmlParser;

	protected final DslEnvironment dslEnvironment;

	final List<Node> conditionNodes = new ArrayList<>();

	/**
	 * @param jobManagement Never {@code null}
	 * @param dslEnvironment Never {@code null}
	 */
	public ConditionsContext(JobManagement jobManagement, DslEnvironment dslEnvironment) {
		super(jobManagement, null);
		this.dslEnvironment = dslEnvironment;
		// see groovy.lang.GroovyObjectSupport
		this.metaClass = InvokerHelper.getMetaClass(this.getClass());
	}

	/**
	 * @param evenIfUnstable {@code true} if unstable builds also triggers promotion
	 * @see SelfPromotionCondition
	 * @throws ParserConfigurationException Issue with parser configuration
	 * @throws SAXException Any SAX exception, possibly wrapping another exception.
	 * @throws IOException An IO exception from the parser, possibly from a byte stream or character stream supplied by the application.
	 */
	public void selfPromotion(boolean evenIfUnstable) throws ParserConfigurationException, SAXException, IOException {
		addCondition(new SelfPromotionCondition(evenIfUnstable));
	}

	/**
	 * @param evenIfUnstable {@code true} if unstable builds also triggers promotion
	 * @param parameterName Never {@code null}
	 * @param parameterValue Never {@code null}
	 * @see ParameterizedSelfPromotionCondition
	 * @throws ParserConfigurationException Issue with parser configuration
	 * @throws SAXException Any SAX exception, possibly wrapping another exception.
	 * @throws IOException An IO exception from the parser, possibly from a byte stream or character stream supplied by the application.
	 */
	public void parameterizedSelfPromotion(boolean evenIfUnstable, String parameterName, String parameterValue) throws ParserConfigurationException, SAXException, IOException {
		addCondition(new ParameterizedSelfPromotionCondition(evenIfUnstable, parameterName, parameterValue));
	}

	/**
	 * @param users Comma-separated list of users/groups allowed to trigger the promotion
	 * @see ManualCondition
	 * @throws ParserConfigurationException Issue with parser configuration
	 * @throws SAXException Any SAX exception, possibly wrapping another exception.
	 * @throws IOException An IO exception from the parser, possibly from a byte stream or character stream supplied by the application.
	 */
	public void manual(String users) throws ParserConfigurationException, SAXException, IOException {
		manual(users, null);
	}

	/**
	 * @param users Comma-separated list of users/groups allowed to trigger the promotion
	 * @param manualPromotionClosure Extra options, see {@link ManualPromotionContext}. Can be {@code null}.
	 * @see ManualCondition
	 * @throws ParserConfigurationException Issue with parser configuration
	 * @throws SAXException Any SAX exception, possibly wrapping another exception.
	 * @throws IOException An IO exception from the parser, possibly from a byte stream or character stream supplied by the application.
	 */
	public void manual(String users, @DslContext(ManualPromotionContext.class) @DelegatesTo(ManualPromotionContext.class) Closure<?> manualPromotionClosure) throws ParserConfigurationException, SAXException, IOException {
		final ManualCondition condition = new ManualCondition();
		condition.setUsers(users);
		final Node conditionNode = toNode(condition);
		if (manualPromotionClosure != null) {
			// parameters closure specified, need to execute it and integrate its result with our instance
			final ManualPromotionContext context = new ManualPromotionContext();
			executeInContext(manualPromotionClosure, context);
			final Node parameterDefinitionsNode = (Node) ((NodeList) conditionNode.get("parameterDefinitions")).get(0);
			for (final Node node: context.buildParameterNodes.values()) {
				parameterDefinitionsNode.append(node);
			}
			addExtensionNode(conditionNode);
		} else {
			addCondition(condition);
		}
	}

	/**
	 * Add the <a href="https://wiki.jenkins-ci.org/display/JENKINS/Release+Plugin">release plugin</a> promotion
	 * condition
	 *
	 * @throws ParserConfigurationException Issue with parser configuration
	 * @throws SAXException Any SAX exception, possibly wrapping another exception.
	 * @throws IOException An IO exception from the parser, possibly from a byte stream or character stream supplied by the application.
	 */
	public void releaseBuild() throws ParserConfigurationException, SAXException, IOException {
		jobManagement.requirePlugin("release");
		addExtensionNode((Node) new NodeBuilder().invokeMethod("hudson.plugins.release.promotion.ReleasePromotionCondition", null));
	}

	/**
	 * @param parametersClosure Ignored
	 * @deprecated This method doesn't achieve anything (was wrongly exposed at a higher context). See
	 * 	{@link ManualPromotionContext#parameters(Closure)}.
	 */
	@Deprecated
	public void parameters(Closure<?> parametersClosure) {
		// As of job-dsl:1.53, it will automatically add " is deprecated" to the end of the message so adding a "This
		// method doesn't achieve anything" message would not make much sense -&gt; "This method doesn't achieve
		// anything is deprecated"
		jobManagement.logDeprecationWarning();
	}

	/**
	 * @param evenIfUnstable {@code true} if unstable builds also triggers promotion
	 * @param jobs Downstream jobs to monitor for success
	 * @see DownstreamPassCondition
	 * @throws ParserConfigurationException Issue with parser configuration
	 * @throws SAXException Any SAX exception, possibly wrapping another exception.
	 * @throws IOException An IO exception from the parser, possibly from a byte stream or character stream supplied by the application.
	 */
	public void downstream(boolean evenIfUnstable, String jobs) throws ParserConfigurationException, SAXException, IOException {
		addCondition(new DownstreamPassCondition(jobs, evenIfUnstable));
	}

	/**
	 * @param promotionNames Comma-separated list of promotions
	 * @see UpstreamPromotionCondition
	 * @throws ParserConfigurationException Issue with parser configuration
	 * @throws SAXException Any SAX exception, possibly wrapping another exception.
	 * @throws IOException An IO exception from the parser, possibly from a byte stream or character stream supplied by the application.
	 */
	public void upstream(String promotionNames) throws ParserConfigurationException, SAXException, IOException {
		addCondition(new UpstreamPromotionCondition(promotionNames));
	}

	/**
	 * Add a condition to be generated.
	 * @param condition Condition to include in resulting promotion. Never {@code null}.
	 * @throws ParserConfigurationException Issue with parser configuration
	 * @throws SAXException Any SAX exception, possibly wrapping another exception.
	 * @throws IOException An IO exception from the parser, possibly from a byte stream or character stream supplied by the application.
	 */
	protected void addCondition(PromotionCondition condition) throws ParserConfigurationException, SAXException, IOException {
		final Node node = toNode(condition);
		addExtensionNode(node);
	}

	/**
	 * Convert an object into a {@link Node}.
	 * @param object Never {@code null}
	 * @return Converted object, mainly for use with {@link #addExtensionNode(Node)}. Never {@code null}
	 * @throws ParserConfigurationException Issue with parser configuration
	 * @throws SAXException Any SAX exception, possibly wrapping another exception.
	 * @throws IOException An IO exception from the parser, possibly from a byte stream or character stream supplied by the application.
	 * @throws XStreamException if the object cannot be serialized
	 */
	protected Node toNode(Object object) throws ParserConfigurationException, SAXException, IOException, XStreamException {
		if (xmlParser == null) {
			xmlParser = new XmlParser();
		}
		return xmlParser.parseText(Items.XSTREAM2.toXML(object));
	}

	@Override
	protected void addExtensionNode(Node node) {
		conditionNodes.add(node);
	}

	/*
	 * @see groovy.lang.GroovyObjectSupport#getProperty(String)
	 */
	@Override
	public Object getProperty(String property) {
		return getMetaClass().getProperty(this, property);
	}

	/*
	 * @see groovy.lang.GroovyObjectSupport#setProperty(String, Object)
	 */
	@Override
	public void setProperty(String property, Object newValue) {
		getMetaClass().setProperty(this, property, newValue);
	}

	/*
	 * @see groovy.lang.GroovyObjectSupport#invokeMethod(String, Object)
	 */
	@Override
	public Object invokeMethod(String name, Object args) {
		return getMetaClass().invokeMethod(this, name, args);
	}

	/*
	 * @see groovy.lang.GroovyObjectSupport#getMetaClass()
	 */
	@Override
	public MetaClass getMetaClass() {
		if (metaClass == null) {
			metaClass = InvokerHelper.getMetaClass(getClass());
		}
		return metaClass;
	}

	/*
	 * @see groovy.lang.GroovyObjectSupport#setMetaClass(MetaClass)
	 */
	@Override
	public void setMetaClass(MetaClass metaClass) {
		this.metaClass = metaClass;
	}
}
