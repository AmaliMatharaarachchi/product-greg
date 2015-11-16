/*
*Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/
package org.wso2.carbon.registry.es.publisher.crud;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wink.client.ClientResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.*;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.es.utils.ESTestCommonUtils;
import org.wso2.carbon.registry.es.utils.GregESTestBaseTest;
import org.wso2.greg.integration.common.utils.GenericRestClient;

import javax.ws.rs.core.MediaType;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class WadlCRUDByUrlTestCase extends GregESTestBaseTest {
    private static final Log log = LogFactory.getLog(WadlCRUDByUrlTestCase.class);
    private TestUserMode userMode;
    String jSessionId;
    String assetId;
    String assetName;
    String cookieHeader;
    GenericRestClient genericRestClient;
    Map<String, String> headerMap;
    String publisherUrl;
    String resourcePath;
    Map<String, String> assocUUIDMap;

    @Factory(dataProvider = "userModeProvider")
    public WadlCRUDByUrlTestCase(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init(userMode);
        genericRestClient = new GenericRestClient();
        headerMap = new HashMap<>();
        resourcePath = FrameworkPathUtil.getSystemResourceLocation()
                + "artifacts" + File.separator + "GREG" + File.separator;
        publisherUrl = automationContext.getContextUrls()
                .getSecureServiceUrl().replace("services", "publisher/apis");
        setTestEnvironment();
    }

    private void setTestEnvironment() throws JSONException, XPathExpressionException,
            IOException {
        JSONObject objSessionPublisher =
                new JSONObject(authenticate(publisherUrl, genericRestClient,
                        automationContext.getSuperTenant().getTenantAdmin().getUserName(),
                        automationContext.getSuperTenant().getTenantAdmin().getPassword())
                        .getEntity(String.class));
        jSessionId = objSessionPublisher.getJSONObject("data").getString("sessionId");
        cookieHeader = "JSESSIONID=" + jSessionId;
        Assert.assertNotNull(jSessionId, "Invalid JSessionID received");
    }

    @Test(groups = {"wso2.greg", "wso2.greg.es"}, description = "Import WADL in Publisher")
    public void createWadlServiceAsset() throws JSONException, IOException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("type", "wadl");
        String wadlTemplate = readFile(resourcePath + "json" + File.separator + "wadl-sample.json");
        assetName = "SearchSearvice.wadl";
        String dataBody = String.format(wadlTemplate,
                "https://raw.githubusercontent.com/wso2/wso2-qa-artifacts/master/automation-artifacts/greg/wadl/SearchSearvice.wadl",
                assetName,
                "2.0.0");
        ClientResponse response =
                genericRestClient.geneticRestRequestPost(publisherUrl + "/assets",
                        MediaType.APPLICATION_JSON,
                        MediaType.APPLICATION_JSON, dataBody
                        , queryParamMap, headerMap, cookieHeader);
        JSONObject obj = new JSONObject(response.getEntity(String.class));
        Assert.assertTrue((response.getStatusCode() == 201),
                "Wrong status code ,Expected 201 Created ,Received " +
                        response.getStatusCode());
        String resultName = (String)obj.get("overview_name");
        Assert.assertEquals(resultName, assetName);
    }

    @Test(groups = {"wso2.greg", "wso2.greg.es"}, description = "Search WADL in Publisher",
            dependsOnMethods = {"createWadlServiceAsset"})
    public void searchWadlAsset() throws JSONException {
        boolean assetFound = false;
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("q", "\"name" + "\":" + "\"" + assetName + "\"");
        ClientResponse clientResponse = searchAssetByQuery(publisherUrl, genericRestClient, cookieHeader, queryParamMap);
        JSONObject obj = new JSONObject(clientResponse.getEntity(String.class));
        JSONArray jsonArray = obj.getJSONArray("list");
        for (int i = 0; i < jsonArray.length(); i++) {
            String name = (String) jsonArray.getJSONObject(i).get("name");
            if (assetName.equals(name)) {
                assetFound = true;
                assetId = (String) jsonArray.getJSONObject(i).get("id");
                break;
            }
        }
        Assert.assertEquals(assetFound, true);
        Assert.assertNotNull(assetId, "Empty asset resource id available");
    }

    @Test(groups = {"wso2.greg", "wso2.greg.es"}, description = "Get WADL in Publisher",
            dependsOnMethods = {"searchWadlAsset"})
    public void getWadlAsset() throws JSONException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("type", "wadl");
        ClientResponse clientResponse = getAssetById(publisherUrl, genericRestClient, cookieHeader, assetId, queryParamMap);
        Assert.assertTrue((clientResponse.getStatusCode() == 200),
                "Wrong status code ,Expected 200 OK " +
                        clientResponse.getStatusCode());
        JSONObject obj = new JSONObject(clientResponse.getEntity(String.class));
        Assert.assertEquals(obj.get("id"), assetId);
    }

    @Test(groups = {"wso2.greg", "wso2.greg.es"}, description = "Delete WADL in Publisher",
            dependsOnMethods = {"getWadlAsset"})
    public void deleteWadlAsset() throws JSONException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("type", "wadl");
        assocUUIDMap = getAssociationsFromPages(publisherUrl, genericRestClient, cookieHeader, assetId, queryParamMap);
        genericRestClient.geneticRestRequestDelete(publisherUrl + "/assets/" + assetId,
                MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_JSON
                , queryParamMap, headerMap, cookieHeader);
        ClientResponse clientResponse = getAssetById(publisherUrl, genericRestClient, cookieHeader, assetId, queryParamMap);
        Assert.assertTrue((clientResponse.getStatusCode() == 404),
                "Wrong status code ,Expected 404 Not Found " +
                        clientResponse.getStatusCode());
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() throws RegistryException, JSONException {
        Map<String, String> queryParamMap = new HashMap<>();
        queryParamMap.put("type", "wadl");
        deleteAssetById(publisherUrl, genericRestClient, cookieHeader, assetId, queryParamMap);
        deleteAllAssociationsById(publisherUrl, genericRestClient, cookieHeader, assetId, queryParamMap);
        queryParamMap.clear();
        for (String uuid : assocUUIDMap.keySet()) {
            queryParamMap.put("type", getType(assocUUIDMap.get(uuid)));
            deleteAssetById(publisherUrl, genericRestClient, cookieHeader, uuid, queryParamMap);
        }
    }

    @DataProvider
    private static TestUserMode[][] userModeProvider() {
        return new TestUserMode[][]{
                new TestUserMode[]{TestUserMode.SUPER_TENANT_ADMIN}
//                new TestUserMode[]{TestUserMode.TENANT_USER},
        };
    }

    private String getType(String mediaType) {
        String type;
        switch (mediaType) {
            case "application/x-xsd+xml":
                type = "schema";
                break;
            case "application/vnd.wso2-service+xml":
                type = "service";
                break;
            case "application/vnd.wso2-soap-service+xml":
                type = "soapservice";
                break;
            case "application/vnd.wso2-restservice+xml":
                type = "restservice";
                break;
            case "application/policy+xml":
                type = "policy";
                break;
            case "application/vnd.wso2-endpoint+xml":
                type = "endpoint";
                break;
            case "application/vnd.wso2-notes+xml":
                type = "note";
                break;
            case "application/vnd.wso2-server+xml":
                type = "server";
                break;
            case "application/swagger+json":
                type = "swagger";
                break;
            case "application/wadl+xml":
                type = "wadl";
                break;
            case "application/wsdl+xml":
                type = "wsdl";
                break;
            default:
                type = null;
                break;
        }
        return type;
    }
}
