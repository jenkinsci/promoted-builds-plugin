package hudson.plugins.promoted_builds.integrations.jobdsl;

import com.google.common.io.Files;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;

public class XmlHelper {

    // DocumentBuilderFactory is not thread-safe
    private ThreadLocal<DocumentBuilderFactory> documentBuilderFactory = new ThreadLocal<DocumentBuilderFactory>() {
        @Override
        protected DocumentBuilderFactory initialValue() {
            return DocumentBuilderFactory.newInstance();
        }
    };

    // XPathFactory is not thread-safe
    private ThreadLocal<XPathFactory> xPathFactory = new ThreadLocal<XPathFactory>() {
        @Override
        protected XPathFactory initialValue() {
            return XPathFactory.newInstance();
        }
    };

    public abstract class BaseXPathMatcher<T>  extends BaseMatcher<T> {
        protected final String xPathExpression;
        protected final String expectedValue;

        protected Object lastEval;

        public BaseXPathMatcher(String xPathExpression, String expectedValue) {
            this.xPathExpression = xPathExpression;
            this.expectedValue = expectedValue;
        }

        @Override
        public boolean matches(Object item) {
            try {
                final Document document = parse(item);

                final XPath xPath = xPathFactory.get().newXPath();
                final XPathExpression xPathExpression1 = xPath.compile(xPathExpression);

                lastEval = xPathExpression1.evaluate(document, XPathConstants.STRING);

                return expectedValue.equals(lastEval);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        protected abstract Document parse(Object item) throws ParserConfigurationException, SAXException, IOException;

        @Override
        public void describeTo(Description description) {
            description.appendText("Result of ").appendValue(xPathExpression).appendText(" XPath evaluation equals to ").appendValue(expectedValue);
        }
    }

    public class FileXPathMatcher extends BaseXPathMatcher<File> {

        private FileXPathMatcher(String xPathExpression, String expectedValue) {
            super(xPathExpression, expectedValue);
        }

        @Override
        protected Document parse(Object item) throws ParserConfigurationException, SAXException, IOException {
            final File file = (File) item;

            final DocumentBuilder documentBuilder = documentBuilderFactory.get().newDocumentBuilder();
            return documentBuilder.parse(file);
        }

        @Override
        public void describeMismatch(Object item, Description description) {
            try {
                description.appendText("Parsed XML (").appendValue(item).appendText(") evaluates XPath to ").appendValue(lastEval).appendText(" with XML ").appendValue(Files.toString((File) item, Charset.forName("UTF-8")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public class StringXPathMatcher extends BaseXPathMatcher<String> {

        public StringXPathMatcher(String xPathExpression, String expectedValue) {
            super(xPathExpression, expectedValue);
        }

        @Override
        protected Document parse(Object item) throws ParserConfigurationException, SAXException, IOException {
            final DocumentBuilder documentBuilder = documentBuilderFactory.get().newDocumentBuilder();
            return documentBuilder.parse(new InputSource(new StringReader((String) item)));
        }

        @Override
        public void describeMismatch(Object item, Description description) {
            description.appendText("was evaluated to ").appendValue(lastEval).appendText(" with XML ").appendValue(item);
        }
    }

    /**
     * @param xPathExpression Never null. Will be evaluated as {@link XPathConstants#STRING}
     * @param expectedValue   Never null. Will be compared ({@link #equals(Object)}) to the result of the XPath expression evaluation
     * @return Matcher to validate XML file using XPath
     */
    public Matcher<File> newFileXPathMatcher(String xPathExpression, String expectedValue) {
        return new FileXPathMatcher(xPathExpression, expectedValue);
    }

    /**
     * @param xPathExpression Never null. Will be evaluated as {@link XPathConstants#STRING}
     * @param expectedValue   Never null. Will be compared ({@link #equals(Object)}) to the result of the XPath expression evaluation
     * @return Matcher to validate XML string using XPath
     */
    public Matcher<String> newStringXPathMatcher(String xPathExpression, String expectedValue) {
        return new StringXPathMatcher(xPathExpression, expectedValue);
    }
}