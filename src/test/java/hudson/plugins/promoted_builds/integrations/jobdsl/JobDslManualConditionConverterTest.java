package hudson.plugins.promoted_builds.integrations.jobdsl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;

import hudson.util.XStream2;

public class JobDslManualConditionConverterTest {
    
    private static final XStream XSTREAM = new XStream2();
    
    @Test
    public void testShouldGenerateValidXml() throws Exception {
        //Given
        JobDslManualCondition mc = new JobDslManualCondition();
        mc.setUsers("testusers");
        //When
        XSTREAM.registerConverter(new ManualConditionConverter(XSTREAM.getMapper(), XSTREAM.getReflectionProvider()));
        String xml =  XSTREAM.toXML(mc);
        //Then
        assertNotNull(xml);
        System.out.println(xml);
        assertTrue(StringUtils.contains(xml, "hudson.plugins.promoted__builds.conditions.ManualCondition"));
    }
}
