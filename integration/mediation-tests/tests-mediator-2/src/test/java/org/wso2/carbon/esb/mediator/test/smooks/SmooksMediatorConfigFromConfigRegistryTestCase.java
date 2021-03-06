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
package org.wso2.carbon.esb.mediator.test.smooks;

import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.annotations.ExecutionEnvironment;
import org.wso2.carbon.automation.engine.annotations.SetEnvironment;
import org.wso2.esb.integration.common.clients.registry.ResourceAdminServiceClient;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import org.wso2.esb.integration.common.utils.servers.MultiMessageReceiver;

import java.io.File;
import java.net.URL;
import java.util.List;
import javax.activation.DataHandler;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class SmooksMediatorConfigFromConfigRegistryTestCase extends ESBIntegrationTest {
    private ResourceAdminServiceClient resourceAdminServiceStub;
    private final int PORT = 8201;
    private boolean isProxyDeployed = false;
    /**
     * MSG_COUNT is depend on the edi.tct file
     */
    private final int MSG_COUNT = 5;
    private final String COMMON_FILE_LOCATION = File.separator + "artifacts" + File.separator + "ESB" + File.separator + "synapseconfig" + File.separator + "vfsTransport" + File.separator;

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {
        super.init();
        resourceAdminServiceStub = new ResourceAdminServiceClient(contextUrls.getBackEndUrl(), context.getContextTenant().getContextUser().getUserName()
, context.getContextTenant().getContextUser().getPassword());
        uploadResourcesToConfigRegistry();
        addVFSProxy();
    }

    @SetEnvironment(executionEnvironments = {ExecutionEnvironment.STANDALONE
})
    @Test(groups = {"wso2.esb", "local only"}, description = "Smooks configuration refer form configuration registry",
            enabled = false)
    public void testSmookConfigFromConfigRegistry() throws Exception {
        MultiMessageReceiver multiMessageReceiver = new MultiMessageReceiver(PORT);

        multiMessageReceiver.startServer();
        try {
            File afile = new File(getClass().getResource(COMMON_FILE_LOCATION + File.separator + "edi.txt").getPath());
            File bfile = new File(getClass().getResource(COMMON_FILE_LOCATION).getPath() + "test" + File.separator + "in" + File.separator + "edi.txt");
            ;
            FileUtils.copyFile(afile, bfile);
            new File(getClass().getResource(COMMON_FILE_LOCATION).getPath() + "test" + File.separator + "out" + File.separator).mkdir();
            Thread.sleep(30000);
        } catch (Exception e) {
        }
        List<String> response = null;
        while (multiMessageReceiver.getMessageQueueSize() < MSG_COUNT) {
            System.out.println("Waiting for fill up the list");
            Thread.sleep(1000);
        }
        response = multiMessageReceiver.getIncomingMessages();
        multiMessageReceiver.stopServer();
        String totalResponse = "";
        for (String temp : response) {
            totalResponse += temp;
        }
        assertNotNull(response, "Response is null");
        assertEquals(response.size(), MSG_COUNT, "Message count is mis matching");
        assertTrue(totalResponse.contains("IBM"), "IBM is not in the response");
        assertTrue(totalResponse.contains("MSFT"), "MSFT is not in the response");
        assertTrue(totalResponse.contains("SUN"), "SUN is not in the response");

        deleteProxyService("smooksMediatorAtConfigRegTestProxy");
    }

    @AfterClass(alwaysRun = true)
    public void restoreServerConfiguration() throws Exception {
        try {
            if(isProxyDeployed)
            {
                deleteProxyService("smooksMediatorAtConfigRegTestProxy");
            }
            resourceAdminServiceStub.deleteResource("/_system/config/smooks");

        } finally {
            super.cleanup();
            Thread.sleep(3000);
            resourceAdminServiceStub = null;
        }

    }

    private void addVFSProxy()
            throws Exception {

        addProxyService(AXIOMUtil.stringToOM("<proxy xmlns=\"http://ws.apache.org/ns/synapse\" "
                                             + "name=\"smooksMediatorAtConfigRegTestProxy\" "
                                             + "transports=\"vfs\">\n" +
                                             "        <parameter name=\"transport.vfs.ContentType\">text/plain</parameter>\n" +
                                             "        <!--CHANGE-->\n" +
                                             "        <parameter name=\"transport.vfs.FileURI\">file://" + getClass().getResource(COMMON_FILE_LOCATION).getPath() + "test" + File.separator + "in" + File.separator + "</parameter>\n" +
                                             "        <parameter name=\"transport.vfs.ContentType\">text/plain</parameter>\n" +
                                             "        <parameter name=\"transport.vfs.FileNamePattern\">.*\\.txt</parameter>\n" +
                                             "        <parameter name=\"transport.PollInterval\">5</parameter>\n" +
                                             "        <!--CHANGE-->\n" +
                                             "        <parameter name=\"transport.vfs.MoveAfterProcess\">file://" + getClass().getResource(COMMON_FILE_LOCATION).getPath() + "test" + File.separator + "out" + File.separator + "</parameter>\n" +
                                             "        <!--CHANGE-->\n" +
                                             "        <parameter name=\"transport.vfs.MoveAfterFailure\">file://" + getClass().getResource(COMMON_FILE_LOCATION).getPath() + "test" + File.separator + "out" + File.separator + "</parameter>\n" +
                                             "        <parameter name=\"transport.vfs.ActionAfterProcess\">MOVE</parameter>\n" +
                                             "        <parameter name=\"transport.vfs.ActionAfterFailure\">MOVE</parameter>\n" +
                                             "        <parameter name=\"Operation\">urn:placeOrder</parameter>\n" +
                                             "        <target>\n" +
                                             "            <inSequence>\n" +
                                             "                <smooks config-key=\"conf:/smooks/smooks-config.xml\">\n" +
                                             "                    <input type=\"text\"/>\n" +
                                             "                    <output type=\"xml\"/>\n" +
                                             "                </smooks>\n" +
                                             "                <xslt key=\"smooksXsltTransform\"/>\n" +
                                             "                <log level=\"full\"/>\n" +
                                             "                <!--<property name=\"ContentType\" value=\"text/xml\" scope=\"axis2-client\"/>-->\n" +
                                             "                <!--<property name=\"messageType\" value=\"text/xml\" scope=\"axis2\"/>-->\n" +
                                             "                <iterate expression=\"//m0:placeOrder/m0:order\" preservePayload=\"true\" attachPath=\"//m0:placeOrder\" xmlns:m0=\"http://services.samples\">\n" +
                                             "                    <target>\n" +
                                             "                        <sequence>\n" +
                                             "                            <header name=\"Action\" value=\"urn:placeOrder\"/>\n" +
                                             "                            <property action=\"set\" name=\"OUT_ONLY\" value=\"true\"/>\n" +
                                             "                            <send>\n" +
                                             "                                <endpoint>\n" +
                                             "                                    <address format=\"soap11\"\n" +
                                             "                                             uri=\"http://localhost:" + PORT + "\"/>\n" +
                                             "                                </endpoint>\n" +
                                             "                            </send>\n" +
                                             "                        </sequence>\n" +
                                             "                    </target>\n" +
                                             "                </iterate>\n" +
                                             "            </inSequence>\n" +
                                             "            <outSequence/>\n" +
                                             "        </target>\n" +
                                             "        <publishWSDL uri=\"file:samples/service-bus/resources/smooks/PlaceStockOrder.wsdl\"/>\n" +
                                             "    </proxy>\n"));
        isProxyDeployed = true;
    }

    private void uploadResourcesToConfigRegistry() throws Exception {
        resourceAdminServiceStub.deleteResource("/_system/config/smooks");
        resourceAdminServiceStub.addCollection("/_system/config/", "smooks", "",
                                               "Contains smooks config files");
        resourceAdminServiceStub.addResource(
                "/_system/config/smooks/smooks-config.xml", "application/xml", "xml files",
                new DataHandler(new URL("file:///" + getClass().getResource(
                        "/artifacts/ESB/synapseconfig/vfsTransport/smooks-config.xml").getPath())));
    }

}

