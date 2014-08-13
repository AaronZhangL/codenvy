/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2014] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.cdec.im;

import static org.testng.Assert.assertEquals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.codenvy.cdec.Daemon;
import com.codenvy.cdec.client.restlet.RestletClient;
import com.codenvy.cdec.server.InstallationManagerService;

/**
 * @author Dmytro Nochevnov
 */
public class TestInstallationManagerApplication {    
    protected static final Logger LOG = LoggerFactory.getLogger(TestInstallationManagerApplication.class);

    InstallationManagerService    managerSeviceProxy;
    
    @BeforeMethod
    public void setUp() throws Exception {          
        Daemon.start();
        
        managerSeviceProxy = RestletClient.getServiceProxy(InstallationManagerService.class);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        Daemon.stop();
    }

    @Test
    public void testGetNewVersions() throws Exception {
        String expectedNewVersionList = "{}";
        
        String newVersionList = managerSeviceProxy.doGetNewVersions();
        assertEquals(newVersionList, expectedNewVersionList);
    }
    

}
