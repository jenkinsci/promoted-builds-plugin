package hudson.plugins.promoted_builds.integrations.jobdsl;

import com.thoughtworks.xstream.XStream;
import hudson.util.XStream2;
import org.junit.Test;
import org.jvnet.hudson.test.For;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@For({JobDslManualCondition.class, ManualConditionConverter.class})
public class JobDslManualConditionConverterTest {

    private static final XStream XSTREAM = new XStream2();

    private XmlHelper xmlHelper = new XmlHelper();

    @Test
    public void testShouldGenerateValidXml() throws Exception {
        //Given
        JobDslManualCondition mc = new JobDslManualCondition();
        mc.setUsers("testusers");
        //When
        XSTREAM.registerConverter(new ManualConditionConverter(XSTREAM.getMapper(), XSTREAM.getReflectionProvider()));
        String xml =  XSTREAM.toXML(mc);
        //Then
        assertThat(xml, allOf(
            notNullValue(),
            xmlHelper.newStringXPathMatcher("count(//hudson.plugins.promoted__builds.conditions.ManualCondition)", "1")
        ));
    }
}
