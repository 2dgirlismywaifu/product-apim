/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.am.scenario.tests.api.secure.oauth2;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.ApiException;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.impl.RestAPIPublisherImpl;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APICreationRequestBean;
import org.wso2.am.integration.test.utils.bean.APILifeCycleAction;
import org.wso2.am.integration.test.utils.bean.APIResourceBean;
import org.wso2.am.scenario.test.common.HttpClient;
import org.wso2.am.scenario.test.common.ScenarioDataProvider;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.am.scenario.test.common.ScenarioTestConstants;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class SecureUsingAuth2NegativeTestCases extends ScenarioTestBase {

    private static final Log log = LogFactory.getLog(SecureUsingAuth2NegativeTestCases.class);
    private final String API_DEVELOPER_USERNAME = "3.1.1-developer";
    private final String TEST_API_1_NAME = "PhoneVerifyAPI-2";
    private final String TEST_API_1_CONTEXT = "phone";
    private final String TEST_API_1_VERSION = "1.0.0";
    private final String TEST_APPLICATION_NAME_2 = "TestApp2";
    private final String INVALID_TOKEN = "Bear !23sqsAe%2@4&~";
    private final String REVOKE_TOKEN = "revoke";
    private final String CUSTOM_AUTH_HEADER = "foo";
    private String accessToken;
    private String applicationOneAccessToken;
    private String consumerKey;
    private String consumerSecret;


    private final String TEST_API_1_CONTEXT_TENANT = "t/wso2.com/phone";
    private final String TEST_APPLICATION_NAME = "TestApp1";
    private String tierCollection = "Gold,Bronze,Unlimited";
    private static String apiId;
    private static String applicationId;
    private static String applicationId2;

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin@wso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String API_CREATOR_PUBLISHER_USERNAME = "micheal";
    private static final String API_CREATOR_PUBLISHER_PW = "Micheal#123";
    private static final String API_SUBSCRIBER_USERNAME = "andrew";
    private static final String API_SUBSCRIBER_PW = "Andrew#123";

    private final String API_END_POINT_POSTFIX_URL = "jaxrs_basic/services/customers/customerservice/";

    @Factory(dataProvider = "userModeDataProvider")
    public SecureUsingAuth2NegativeTestCases(TestUserMode userMode) {
        this.userMode = userMode;
    }

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                    ADMIN_USERNAME, ADMIN_PW);
            createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, ADMIN_USERNAME, ADMIN_PW);
        }

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // create user in wso2.com tenant
            addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_USERNAME, ADMIN_PW);
            if (isActivated(ScenarioTestConstants.TENANT_WSO2)) {
                //Add and activate wso2.com tenant
                createUserWithPublisherAndCreatorRole(API_CREATOR_PUBLISHER_USERNAME, API_CREATOR_PUBLISHER_PW,
                        TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
                createUserWithSubscriberRole(API_SUBSCRIBER_USERNAME, API_SUBSCRIBER_PW, TENANT_ADMIN_USERNAME,
                        TENANT_ADMIN_PW);
            }
        }
        super.init(userMode);
        String apiEndPointUrl = backEndServerUrl.getWebAppURLHttp() + API_END_POINT_POSTFIX_URL;
        APICreationRequestBean apiCreationRequestBean = new APICreationRequestBean(TEST_API_1_NAME, "/" + TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                API_CREATOR_PUBLISHER_USERNAME, new URL(apiEndPointUrl));

        apiCreationRequestBean.setTiersCollection(tierCollection);
        ArrayList<APIResourceBean> resourceBeanArrayList = new ArrayList<>();

        resourceBeanArrayList.add(new APIResourceBean(APIMIntegrationConstants.HTTP_VERB_GET,
                APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION, APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED, "/customers/{id}"));

        resourceBeanArrayList.add(new APIResourceBean(APIMIntegrationConstants.HTTP_VERB_POST,
                APIMIntegrationConstants.RESOURCE_AUTH_TYPE_APPLICATION_USER, APIMIntegrationConstants.RESOURCE_TIER.UNLIMITED, "/customers/name/"));

        apiCreationRequestBean.setResourceBeanList(resourceBeanArrayList);
        APIDTO apiDto = restAPIPublisher.addAPI(apiCreationRequestBean);
        apiId = apiDto.getId();
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);

        HttpResponse applicationResponse = restAPIStore.createApplication(TEST_APPLICATION_NAME,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);

        applicationId = applicationResponse.getData();
        //Subscribe the API to the Application
        restAPIStore.createSubscription(apiId, applicationId, APIMIntegrationConstants.API_TIER.UNLIMITED);
        accessToken = generateAppKeys();
    }

    @Test(description = "3.1.1.8")
    public void testOAuth2AuthorizationWithoutToken() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        String gatewayHttpsUrl = null;
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                    "/customers/123");
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT_TENANT, TEST_API_1_VERSION,
                    "/customers/123");
        }

        if (log.isDebugEnabled()) {
            log.debug("Gateway HTTPS URL : " + gatewayHttpsUrl);
        }
        HttpResponse apiResponse = HttpClient.doGet(gatewayHttpsUrl, requestHeaders);
        assertEquals(apiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation. \n API response : " + apiResponse.getData());
    }

    @Test(description = "3.1.1.9", dependsOnMethods = "testOAuth2AuthorizationWithoutToken")
    public void testOAuth2AuthorizationWithInValidToken() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer alkjsbdjabwiubdaj");
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        String gatewayHttpsUrl = null;
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                    "/customers/123");
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT_TENANT, TEST_API_1_VERSION,
                    "/customers/123");
        }

        if (log.isDebugEnabled()) {
            log.debug("Gateway HTTPS URL : " + gatewayHttpsUrl);
        }
        HttpResponse apiResponse = HttpClient.doGet(gatewayHttpsUrl, requestHeaders);
        assertEquals(apiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation. \n API response : " + apiResponse.getData());
    }

    @Test(description = "3.1.1.10", dependsOnMethods = "testOAuth2AuthorizationWithInValidToken")
    public void testResourceInvokedByExpiredToken() throws Exception {

        HttpResponse applicationResponse = restAPIStore.createApplication(TEST_APPLICATION_NAME_2,
                "Test Application", APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED,
                ApplicationDTO.TokenTypeEnum.OAUTH);

        applicationId2 = applicationResponse.getData();
        //Subscribe the API to the Application
        restAPIStore.createSubscription(apiId, applicationId2, APIMIntegrationConstants.API_TIER.UNLIMITED);
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId2, "1", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        applicationOneAccessToken = accessToken;
        accessToken = applicationKeyDTO.getToken().getAccessToken();
        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();
        Thread.sleep(2000);
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        String gatewayHttpsUrl = null;
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                    "/customers/123");
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT_TENANT, TEST_API_1_VERSION,
                    "/customers/123");
        }

        if (log.isDebugEnabled()) {
            log.debug("Gateway HTTPS URL : " + gatewayHttpsUrl);
        }
        HttpResponse apiResponse = HttpClient.doGet(gatewayHttpsUrl, requestHeaders);
        assertEquals(apiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation. \n API response : " + apiResponse.getData());
    }

    @Test(description = "3.1.1.11", dependsOnMethods = "testResourceInvokedByExpiredToken")
    public void testResourceInvokedByRevokedToken() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        URL tokenEndpointURL = new URL(gatewayHttpsURL + "/" + REVOKE_TOKEN);

        String requestBody = "token=" + accessToken;
        HttpResponse response = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody, tokenEndpointURL);
        assertEquals(response.getResponseCode(), Response.Status.OK.getStatusCode(),
                "Response code mismatched when revoke toke. \n API response : " +
                        response.getData());

        requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, "Bearer " + accessToken);
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        String gatewayHttpsUrl = null;
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                    "/customers/123");
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT_TENANT, TEST_API_1_VERSION,
                    "/customers/123");
        }

        if (log.isDebugEnabled()) {
            log.debug("Gateway HTTPS URL : " + gatewayHttpsUrl);
        }
        HttpResponse apiResponse = HttpClient.doGet(gatewayHttpsUrl, requestHeaders);
        assertEquals(apiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation. \n API response : " + apiResponse.getData());
    }

    // Disabling since resource level application security was removed since we have scopes.
    @Test(description = "3.1.1.12", dependsOnMethods = "testResourceInvokedByRevokedToken", enabled = false)
    public void testSecurityTypeAsApplicationResourceInvokedByTokenWithPasswordGrantType() throws Exception {
        String requestBody = null;
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            requestBody = "grant_type=password&username=andrew&password=Andrew#123&scope=PRODUCTION";
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            requestBody = "grant_type=password&username=andrew@wso2.com&password=Andrew#123&scope=PRODUCTION";
        }
        URL tokenEndpointURL = new URL(gatewayHttpsURL + "/token");
        HttpResponse firstResponse = restAPIStore.generateUserAccessKey(consumerKey, consumerSecret, requestBody,
                tokenEndpointURL);
        JSONObject firstAccessTokenGenerationResponse = new JSONObject(firstResponse.getData());
        //get an access token for the first time
        String accessTokenWithPasswordGrantType = firstAccessTokenGenerationResponse.getString("access_token");

        Map<String, String> requestHeaders = new HashMap();
        requestHeaders.put("Authorization", "Bearer " + accessTokenWithPasswordGrantType);
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
        String gatewayHttpsUrl = null;
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                    "/customers/123");
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT_TENANT, TEST_API_1_VERSION,
                    "/customers/123");
        }

        HttpResponse apiResponse = HttpClient.doGet(gatewayHttpsUrl, requestHeaders);
        assertEquals(apiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation. \n API response : " + apiResponse.getData());
    }

    // Disabling since resource level application security was removed since we have scopes.
    @Test(description = "3.1.1.13", dependsOnMethods = "testResourceInvokedByRevokedToken", enabled = false)
    public void testSecurityTypeAsApplicationResourceUserInvokedByTokenWithClientCredentials() throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Authorization", "Bearer " + accessToken);
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        String gatewayHttpsUrl = null;
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                    "/customers/name");
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT_TENANT, TEST_API_1_VERSION,
                    "/customers/name");
        }
        HashMap<String, String> requestHeadersPost = new HashMap<>();
        requestHeadersPost.put("accept", "text/plain");
        requestHeadersPost.put("Content-Type", "text/plain");
        HttpResponse apiResponse = HttpClient.doPost(new URL(gatewayHttpsUrl), "id=25", requestHeadersPost);
        assertEquals(apiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation. \n API response : " + apiResponse.getData());
    }

    @Test(description = "3.1.1.15", dependsOnMethods = "testResourceInvokedByRevokedToken")
    public void testResourceApplicationInvokeByCustomAuthorization() throws Exception {
        Map<String, String> requestHeaders = new HashMap();
        changeCustomAuthorizationHeaderInAPI(CUSTOM_AUTH_HEADER, apiId);
        restAPIPublisher.changeAPILifeCycleStatus(apiId, APILifeCycleAction.PUBLISH.getAction(), null);
        requestHeaders.put("Authorization", "Bearer " + applicationOneAccessToken);
        requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");

        String gatewayHttpsUrl = null;
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                    "/customers/123");
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT_TENANT, TEST_API_1_VERSION,
                    "/customers/123");
        }
        HttpResponse apiResponse = HttpClient.doGet(gatewayHttpsUrl, requestHeaders);
        assertEquals(apiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation. \n API response : " + apiResponse.getData());
        changeCustomAuthorizationHeaderInAPI("Authorization", apiId);
    }

    @Test(description = "3.1.1.17", dataProvider = "IncorrectFormattedAuthorizationHeadersDataProvider",
            dataProviderClass = ScenarioDataProvider.class, dependsOnMethods = "testResourceApplicationInvokeByCustomAuthorization")
    public void testOAuth2AuthorizationWithIncorrectFormattedHeader(String tokenPrefix, String tokenValue)
            throws Exception {
        Map<String, String> requestHeaders = new HashMap<>();

        if (tokenValue.isEmpty()) {
            requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, tokenPrefix);
        }
        if (tokenValue.equals("tokenVal")) {
            requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, tokenPrefix + accessToken);
        }
        if (tokenValue.equals("tokenDuplicated")) {
            requestHeaders.put(APIMIntegrationConstants.AUTHORIZATION_HEADER, tokenPrefix + accessToken + "; " +
                    tokenPrefix + accessToken);
        }

        String gatewayHttpsUrl = null;
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT, TEST_API_1_VERSION,
                    "/customers/123");
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            gatewayHttpsUrl = getHttpsAPIInvocationURL(TEST_API_1_CONTEXT_TENANT, TEST_API_1_VERSION,
                    "/customers/123");
        }

        if (log.isDebugEnabled()) {
            log.debug("Gateway HTTPS URL : " + gatewayHttpsUrl);
        }
        HttpResponse apiResponse = HttpClient.doGet(gatewayHttpsUrl, requestHeaders);
        assertEquals(apiResponse.getResponseCode(), Response.Status.UNAUTHORIZED.getStatusCode(),
                "Response code mismatched when api invocation. \n API response : " + apiResponse.getData());
    }

    @Test(description = "3.1.1.18", dependsOnMethods = "testOAuth2AuthorizationWithIncorrectFormattedHeader")
    public void testModifyCustomAuthHeaderByUserWithCreatorRole() throws Exception {

        String creatorUsername = "john";
        String creatorPassword = "john@123";
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            createUserWithCreatorRole(creatorUsername, creatorPassword,
                    ADMIN_USERNAME, ADMIN_PW);
        }

        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // create user in wso2.com tenant
            addTenantAndActivate(ScenarioTestConstants.TENANT_WSO2, ADMIN_USERNAME, ADMIN_PW);
            if (isActivated(ScenarioTestConstants.TENANT_WSO2)) {
                //Add and activate wso2.com tenant
                createUserWithCreatorRole(creatorUsername, creatorPassword,
                        TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            }
        }

        RestAPIPublisherImpl restAPIPublisherNew = new RestAPIPublisherImpl(creatorUsername, creatorPassword,
                storeContext.getContextTenant().getDomain(), storeURLHttps);

        HttpResponse response = restAPIPublisherNew.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        apidto.setAuthorizationHeader(CUSTOM_AUTH_HEADER);
        try {
            restAPIPublisherNew.updateAPI(apidto, apiId);
            fail("API Creator role was able to update the authorization header");
        } catch (ApiException e) {
            assertTrue(e.getResponseBody().contains("Error while updating the API : " + apiId));
        }
    }


    public String generateAppKeys() throws Exception {
        ArrayList grantTypes = new ArrayList();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore.generateKeys(applicationId, "36000", "",
                ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);
        //get access token
        accessToken = applicationKeyDTO.getToken().getAccessToken();
        consumerKey = applicationKeyDTO.getConsumerKey();
        consumerSecret = applicationKeyDTO.getConsumerSecret();
        return accessToken;
    }

    public org.wso2.am.integration.clients.store.api.v1.dto.APIDTO changeCustomAuthorizationHeaderInAPI(String customAuth, String apiId) throws Exception {
        HttpResponse response = restAPIPublisher.getAPI(apiId);
        Gson g = new Gson();
        APIDTO apidto = g.fromJson(response.getData(), APIDTO.class);
        apidto.setAuthorizationHeader(customAuth);
        APIDTO updatedAPI = restAPIPublisher.updateAPI(apidto, apiId);
        restAPIPublisher.changeAPILifeCycleStatus(updatedAPI.getId(), APILifeCycleAction.PUBLISH.getAction(), null);
        // Waiting until the api is available in store.
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            restAPIStore.isAvailableInDevPortal(updatedAPI.getId(), "carbon.super");
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            restAPIStore.isAvailableInDevPortal(updatedAPI.getId(), "wso2.com");
        }

        log.info("API available in store" + "api_id: " + apiId);
        return restAPIStore.getAPI(apiId);
    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIStore.deleteApplication(applicationId2);
        restAPIPublisher.deleteAPI(apiId);
        if (this.userMode.equals(TestUserMode.SUPER_TENANT_USER)) {
            // deleteUser(API_CREATOR_PUBLISHER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
            // deleteUser(API_SUBSCRIBER_USERNAME, ADMIN_USERNAME, ADMIN_PW);
        }
        if (this.userMode.equals(TestUserMode.TENANT_USER)) {
            // deleteUser(API_CREATOR_PUBLISHER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            // deleteUser(API_SUBSCRIBER_USERNAME, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            // deactivateAndDeleteTenant(ScenarioTestConstants.TENANT_WSO2);
        }
    }

    @DataProvider
    public static Object[][] userModeDataProvider() {
        setup();
        // return the relevant parameters for each test run
        // 1) Super tenant API creator
        // 2) Tenant API creator
        return new Object[][]{
                new Object[]{TestUserMode.SUPER_TENANT_USER},
                new Object[]{TestUserMode.TENANT_USER},
        };
    }
}
