package org.onap.integration.simulators.nfsimulator.vesclient.integration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.gson.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Main.class, TestConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class VariablesReplacement {

    @Autowired
    private VesSimulatorService vesSimulatorService;

    private String currentVesSimulatorIp;

    @Before
    public void setUp() throws Exception {
        currentVesSimulatorIp = TestUtils.getCurrentIpAddress();
    }

    @Test
    public void whenTriggeredSimulatorShouldReplaceStringKeyword() {
        String startUrl = prepareRequestUrl();
        String body = "{\n" + "\"templateName\": \"cmNotification.json\",\n" + "\"patch\":{},\n" + "\"variables\":{\n"
                              + "\"dN\": \"NRNB=5, NRCEL=1234\",\n" + "\"attributeList\":{\n"
                              + "\"threshXHighQ\": \"50\",\n" + "\"threshXHighP\": \"52\"\n" + "}\n" + "},\n"
                              + "\"simulatorParams\": {\n" + "\"vesServerUrl\": \"https://" + currentVesSimulatorIp
                              + ":9443/ves-simulator/eventListener/v5\",\n" + "\"repeatInterval\": 1,\n"
                              + "\"repeatCount\": 1\n" + "}\n" + "}";
        ArgumentCaptor<JsonObject> parameterCaptor = ArgumentCaptor.forClass(JsonObject.class);

        given().contentType("application/json").body(body).when().post(startUrl).then().statusCode(200)
                .body("message", equalTo("Request started"));

        Mockito.verify(vesSimulatorService, Mockito.timeout(3000)).sendEventToDmaapV5(parameterCaptor.capture());

        assertAttributeList(parameterCaptor);
        assertDn(parameterCaptor);
    }

    private void assertDn(ArgumentCaptor<JsonObject> parameterCaptor) {
        String dn = parameterCaptor.getValue().getAsJsonObject("event").getAsJsonObject("otherFields")
                            .getAsJsonArray("jsonObjects").get(0)
                            .getAsJsonObject().getAsJsonArray("objectInstances")
                            .get(0).getAsJsonObject().getAsJsonObject("objectInstance")
                            .getAsJsonObject("cm3gppNotifyFields").getAsJsonPrimitive("dN").getAsString();
        assertThat(dn).isEqualTo("NRNB=5, NRCEL=1234");
    }

    private void assertAttributeList(ArgumentCaptor<JsonObject> parameterCaptor) {
        JsonObject attributeList = parameterCaptor.getValue().getAsJsonObject("event").getAsJsonObject("otherFields")
                                           .getAsJsonArray("jsonObjects").get(0).getAsJsonObject()
                                           .getAsJsonArray("objectInstances").get(0).getAsJsonObject()
                                           .getAsJsonObject("objectInstance").getAsJsonObject("cm3gppNotifyFields")
                                           .getAsJsonObject("attributeList");
        assertThat(attributeList.get("threshXHighQ").getAsString()).isEqualTo("50");
        assertThat(attributeList.get("threshXHighP").getAsString()).isEqualTo("52");
    }

    private String prepareRequestUrl() {
        return "http://0.0.0.0:5000/simulator/start";
    }

}
