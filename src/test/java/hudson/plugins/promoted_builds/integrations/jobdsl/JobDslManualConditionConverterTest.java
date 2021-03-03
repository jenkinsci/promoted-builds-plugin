package hudson.plugins.promoted_builds.integrations.jobdsl;

import org.junit.Test;

import com.thoughtworks.xstream.XStream;

import hudson.util.XStream2;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

public class JobDslManualConditionConverterTest {
    
    private static final XStream XSTREAM = new XStream2();
    
    @Test
    public void testShouldGenerateValidXml() throws Exception {
        //Given
        JobDslManualCondition mc = new JobDslManualCondition();
        mc.setUsers("testusers");
        //When
        XSTREAM.registerConverter(new ManualConditionConverter(XSTREAM.getMapper(), XSTREAM.getReflectionProvider()));
        XSTREAM.processAnnotations(new Class<?>[] {JobDslManualCondition.class});
        String xml =  XSTREAM.toXML(mc);
        //Then
        assertThat(xml,
                   allOf(notNullValue(),
                         containsString("hudson.plugins.promoted__builds.conditions.ManualCondition")));
    }
}
