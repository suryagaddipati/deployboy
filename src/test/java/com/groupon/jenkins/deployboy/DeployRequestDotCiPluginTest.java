package com.groupon.jenkins.deployboy;

import com.groupon.jenkins.dynamic.build.DynamicBuild;
import com.groupon.jenkins.dynamic.build.cause.BuildCause;
import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DeployRequestDotCiPluginTest {

    private DynamicBuild build;

    @Before
    public void setupBuild(){

        build = Mockito.mock(DynamicBuild.class);
        BuildCause buildCause = Mockito.mock(BuildCause.class);
        Mockito.when(build.getCause()).thenReturn(buildCause);
        Mockito.when(buildCause.getPusher()).thenReturn("sherman");
    }

    @Test
    public void should_add_pusher_to_payload(){
        String payload = new DeployRequestDotCiPlugin().getPayload(build, "{}");
        JSONObject payloadJson = JSONObject.fromObject(payload);
        Assert.assertTrue(payloadJson.containsKey("requested_by"));
        Assert.assertEquals("sherman",payloadJson.getString("requested_by"));

    }
    @Test
    public void should_add_pusher_to_payload_if_payload_is_null(){
        String payload = new DeployRequestDotCiPlugin().getPayload(build,null);
        JSONObject payloadJson = JSONObject.fromObject(payload);
        Assert.assertTrue(payloadJson.containsKey("requested_by"));
        Assert.assertEquals("sherman",payloadJson.getString("requested_by"));

    }

}