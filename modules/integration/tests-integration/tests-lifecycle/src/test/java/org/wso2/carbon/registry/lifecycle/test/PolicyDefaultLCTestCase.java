/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.registry.lifecycle.test;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.context.TestUserMode;
import org.wso2.carbon.governance.custom.lifecycles.checklist.stub.CustomLifecyclesChecklistAdminServiceExceptionException;
import org.wso2.carbon.governance.custom.lifecycles.checklist.stub.beans.xsd.LifecycleBean;
import org.wso2.carbon.governance.lcm.stub.LifeCycleManagementServiceExceptionException;
import org.wso2.carbon.registry.activities.stub.RegistryExceptionException;
import org.wso2.carbon.registry.activities.stub.beans.xsd.ActivityBean;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.lifecycle.test.utils.LifeCycleUtils;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;
import org.wso2.greg.integration.common.clients.ActivityAdminServiceClient;
import org.wso2.greg.integration.common.clients.LifeCycleAdminServiceClient;
import org.wso2.greg.integration.common.utils.GREGIntegrationBaseTest;
import org.wso2.greg.integration.common.utils.RegistryProviderUtil;

import java.rmi.RemoteException;
import java.util.Calendar;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class PolicyDefaultLCTestCase extends GREGIntegrationBaseTest{

    private WSRegistryServiceClient wsRegistry;
    private LifeCycleAdminServiceClient lifeCycleAdminServiceClient;
    private ActivityAdminServiceClient activityAdminServiceClient;

    private final String ASPECT_NAME = "ServiceLifeCycle";
    private final String ACTION_PROMOTE = "Promote";
    private String policyPathDev;
    private String sessionCookie;
    private String userName1WithoutDomain;


    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init(TestUserMode.SUPER_TENANT_USER);
        sessionCookie = getSessionCookie();

        lifeCycleAdminServiceClient = new LifeCycleAdminServiceClient(backendURL, sessionCookie);
        activityAdminServiceClient = new ActivityAdminServiceClient(backendURL, sessionCookie);

        RegistryProviderUtil registryProviderUtil = new RegistryProviderUtil();
        wsRegistry = registryProviderUtil.getWSRegistry(automationContext);
        Registry governance = registryProviderUtil.getGovernanceRegistry(wsRegistry, automationContext);

        String policyName = "LifeCycleTestPolicy.xml";
        policyPathDev = "/_system/governance" + LifeCycleUtils.addPolicy(policyName, governance);
        Thread.sleep(1000);

        String userName = automationContext.getContextTenant().getContextUser().getUserName();
        userName1WithoutDomain = userName.substring(0, userName.indexOf('@'));

    }

    /**
     * @throws org.wso2.carbon.registry.core.exceptions.RegistryException
     *
     * @throws org.wso2.carbon.governance.custom.lifecycles.checklist.stub.CustomLifecyclesChecklistAdminServiceExceptionException
     *
     * @throws java.rmi.RemoteException
     * @throws InterruptedException
     * @throws org.wso2.carbon.registry.activities.stub.RegistryExceptionException
     *
     */
    @Test(groups = "wso2.greg", description = "Add lifecycle to a Policy")
    public void addLifecycle()
            throws RegistryException, CustomLifecyclesChecklistAdminServiceExceptionException,
                   RemoteException, InterruptedException, RegistryExceptionException {
        lifeCycleAdminServiceClient.addAspect(policyPathDev, ASPECT_NAME);
        Thread.sleep(500);
        LifecycleBean lifeCycle = lifeCycleAdminServiceClient.getLifecycleBean(policyPathDev);
        Resource service = wsRegistry.get(policyPathDev);
        assertNotNull(service, "Service Not found on registry path " + policyPathDev);
        assertEquals(service.getPath(), policyPathDev, "Service path changed after adding life cycle. " + policyPathDev);

        assertEquals(LifeCycleUtils.getLifeCycleState(lifeCycle), "Development", "LifeCycle State Mismatched");

        //Activity search
        Thread.sleep(1000 * 10);
    }

    //    Extracting out the activity search related testing
    //    https://wso2.org/jira/browse/REGISTRY-1178

    /**
     * @throws org.wso2.carbon.registry.core.exceptions.RegistryException
     *
     * @throws org.wso2.carbon.governance.custom.lifecycles.checklist.stub.CustomLifecyclesChecklistAdminServiceExceptionException
     *
     * @throws java.rmi.RemoteException
     * @throws InterruptedException
     * @throws org.wso2.carbon.registry.activities.stub.RegistryExceptionException
     *
     */
    @Test(groups = "wso2.greg", description = "Get the activity list", dependsOnMethods = "addLifecycle")
    public void testLifecycleAddActivities()
            throws RegistryException, CustomLifecyclesChecklistAdminServiceExceptionException,
                   RemoteException, InterruptedException, RegistryExceptionException {
        ActivityBean activityObj = activityAdminServiceClient.getActivities(sessionCookie, userName1WithoutDomain
                , policyPathDev, null
                , "", ActivityAdminServiceClient.FILTER_ASSOCIATE_ASPECT, 1);

        assertNotNull(activityObj, "Activity object null for Associate Aspect");
        assertNotNull(activityObj.getActivity().length > 0, "Activity list object null for Associate Aspect");
        assertTrue((activityObj.getActivity().length > 0), "Activity list object null");
        // activity string format is
        //true|admin|admin| has updated the resource |/_system/governance/trunk/policies/LifeCycleTestPolicy.xml|/_system/governance/trunk/policies/LifeCycleTestPolicy.xml|  0m ago.
        boolean lcAddActivityFound = false;
        for (String activity : activityObj.getActivity()) {
            if (activity.contains(userName1WithoutDomain) && activity.contains(policyPathDev) &&
                activity.contains("associated the aspect ServiceLifeCycle")) {
                lcAddActivityFound = true;
            }
        }
        assertTrue(lcAddActivityFound, "LC add activity not found");
    }

    /**
     * @throws org.wso2.carbon.governance.custom.lifecycles.checklist.stub.CustomLifecyclesChecklistAdminServiceExceptionException
     *
     * @throws java.rmi.RemoteException
     * @throws InterruptedException
     * @throws org.wso2.carbon.registry.core.exceptions.RegistryException
     *
     * @throws org.wso2.carbon.registry.activities.stub.RegistryExceptionException
     *
     */
    @Test(groups = "wso2.greg", dependsOnMethods = "testLifecycleAddActivities", description = "Promote to Testing")
    public void promoteToTesting()
            throws CustomLifecyclesChecklistAdminServiceExceptionException, RemoteException,
                   InterruptedException, RegistryException, RegistryExceptionException {

        lifeCycleAdminServiceClient.invokeAspect(policyPathDev, ASPECT_NAME,
                                                 ACTION_PROMOTE, null);
        Thread.sleep(2000);

        LifecycleBean lifeCycle = lifeCycleAdminServiceClient.getLifecycleBean(policyPathDev);
        Resource service = wsRegistry.get(policyPathDev);
        assertNotNull(service, "Service Not found on registry path " + policyPathDev);

        assertEquals(LifeCycleUtils.getLifeCycleState(lifeCycle), "Testing", "LifeCycle State Mismatched");
        assertEquals(wsRegistry.get(policyPathDev).getPath(), policyPathDev,
                     "Resource not exist on trunk. Preserve original not working fine");

        //activity search for trunk
        Thread.sleep(1000 * 10);
        ActivityBean activityObjTrunk = activityAdminServiceClient.getActivities(sessionCookie, userName1WithoutDomain
                , policyPathDev, LifeCycleUtils.formatDate(Calendar.getInstance().getTime())
                , "", ActivityAdminServiceClient.FILTER_RESOURCE_UPDATE, 1);
        assertNotNull(activityObjTrunk, "Activity object null in trunk");
        assertNotNull(activityObjTrunk.getActivity(), "Activity list object null");
        assertTrue((activityObjTrunk.getActivity().length > 0), "Activity list object null");
        String activity = activityObjTrunk.getActivity()[0];
        assertTrue(activity.contains(userName1WithoutDomain), "Activity not found. User name not found on last activity. " + activity);
        assertTrue(activity.contains("has updated the resource"),
                   "Activity not found. has updated not contain in last activity. " + activity);
        assertTrue(activity.contains("0m ago"), "Activity not found. current time not found on last activity. " + activity);

    }

    /**
     * @throws org.wso2.carbon.governance.custom.lifecycles.checklist.stub.CustomLifecyclesChecklistAdminServiceExceptionException
     *
     * @throws java.rmi.RemoteException
     * @throws InterruptedException
     * @throws org.wso2.carbon.registry.core.exceptions.RegistryException
     *
     * @throws org.wso2.carbon.registry.activities.stub.RegistryExceptionException
     *
     */
    @Test(groups = "wso2.greg", dependsOnMethods = "promoteToTesting", description = "Promote to Testing")
    public void promoteToProduction()
            throws CustomLifecyclesChecklistAdminServiceExceptionException, RemoteException,
                   InterruptedException, RegistryException, RegistryExceptionException {

        lifeCycleAdminServiceClient.invokeAspect(policyPathDev, ASPECT_NAME,
                                                 ACTION_PROMOTE, null);
        Thread.sleep(2000);

        Resource service = wsRegistry.get(policyPathDev);
        assertNotNull(service, "Service Not found on registry path " + policyPathDev);

        assertEquals(wsRegistry.get(policyPathDev).getPath(), policyPathDev,
                     "Resource not exist on trunk. Preserve original not working fine");

        //activity search for trunk
        Thread.sleep(1000 * 10);
        ActivityBean activityObjTrunk = activityAdminServiceClient.getActivities(sessionCookie, userName1WithoutDomain
                , policyPathDev, LifeCycleUtils.formatDate(Calendar.getInstance().getTime())
                , "", ActivityAdminServiceClient.FILTER_RESOURCE_UPDATE, 1);
        assertNotNull(activityObjTrunk, "Activity object null in trunk");
        assertNotNull(activityObjTrunk.getActivity(), "Activity list object null");
        assertTrue((activityObjTrunk.getActivity().length > 0), "Activity list object null");
        String activity = activityObjTrunk.getActivity()[0];
        assertTrue(activity.contains(userName1WithoutDomain), "Activity not found. User name not found on last activity. " + activity);
        assertTrue(activity.contains("has updated the resource"),
                   "Activity not found. has updated not contain in last activity. " + activity);
        assertTrue(activity.contains("0m ago"), "Activity not found. current time not found on last activity. " + activity);

    }

    /**
     * @throws RegistryException
     * @throws LifeCycleManagementServiceExceptionException
     *
     * @throws RemoteException
     */
    @AfterClass
    public void cleanup()
            throws RegistryException, LifeCycleManagementServiceExceptionException,
                   RemoteException {

        if (policyPathDev != null) {
            wsRegistry.delete(policyPathDev);
        }

    }
}
