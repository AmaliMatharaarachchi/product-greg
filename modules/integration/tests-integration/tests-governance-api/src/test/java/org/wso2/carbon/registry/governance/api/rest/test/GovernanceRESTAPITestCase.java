package org.wso2.carbon.registry.governance.api.rest.test;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.greg.integration.common.utils.GREGIntegrationBaseTest;

import java.util.HashMap;
import java.util.Map;

public class GovernanceRESTAPITestCase extends GREGIntegrationBaseTest {

    private String type = "restservices";
    private String serverUrl;
    private String createdAssetUUID;

    private static final String PAYLOAD =
            "{\n" + "   \"name\": \"rest\",\n" + "   \"type\": \"restservice\",\n" + "   \"uritemplate\":[\n"
                    + "      {\n" + "      \"urlPattern\":\"/api\",\n" + "      \"authType\": \"Basic AUth\",\n"
                    + "      \"httpVerb\" : \"http\"\n" + "      },\n" + "      {\n"
                    + "      \"urlPattern\":\"/auth/api\",\n" + "      \"authType\": \"oauth\",\n"
                    + "      \"httpVerb\" : \"https\"\n" + "      }\n" + "   ],\n" + "   \"context\": \"contest\",\n"
                    + "   \"endpoints\": null,\n" + "   \"description\": \"description\",\n"
                    + "   \"version\": \"1.2.3\",\n" + "   \"security_authenticationType\": \"None\",\n"
                    + "   \"contacts\": null\n" + "}";

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init(TestUserMode.SUPER_TENANT_ADMIN);
        serverUrl = automationContext.getContextUrls().getWebAppURLHttps() + "/governance/" + type;
    }

    @Test(groups = { "wso2.greg" }, description = "test rest api POST")
    public void testRestApiPOST() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Basic YWRtaW46YWRtaW4=");
        requestHeaders.put("Content-Type", "application/json");
        HttpResponse response = HTTPSClientUtils.doPost(serverUrl, requestHeaders, PAYLOAD);
        String createdAssetUrl = ((HashMap) response.getHeaders()).get("Location").toString();
        String[] splitParams = createdAssetUrl.split("/");
        createdAssetUUID = splitParams[splitParams.length - 1];
        Assert.assertEquals(response.getResponseCode(), 201);
        Assert.assertNotNull(createdAssetUUID);
    }

    @Test(groups = {
            "wso2.greg" }, description = "test rest api GET", dependsOnMethods = "testRestApiPOST")
    public void testRestApiGet()
            throws Exception {
        Thread.sleep(30000);
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Basic YWRtaW46YWRtaW4=");
        requestHeaders.put("Content-Type", "application/json");
        HttpResponse response = HTTPSClientUtils.doGet(serverUrl + "/" + createdAssetUUID, requestHeaders);
        Assert.assertTrue(response.getData().contains(createdAssetUUID));
        Assert.assertTrue(response.getData().contains("httpVerb"));
        Assert.assertTrue(response.getData().contains("https"));

        Assert.assertTrue(response.getData().contains("urlPattern"));
        Assert.assertTrue(response.getData().contains("/auth/api"));

        Assert.assertTrue(response.getData().contains("authType"));
        Assert.assertTrue(response.getData().contains("httpVerb"));

        Assert.assertTrue(response.getData().contains("http"));
        Assert.assertEquals(response.getResponseCode(), 200);
    }

}
