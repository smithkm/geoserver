/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.io.File;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.TemporaryResourceStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.springframework.ldap.test.LdapTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Basic class for LDAP related tests.
 * 
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 *
 */
public abstract class LDAPBaseTest {
    protected LDAPSecurityProvider securityProvider;
    
    protected Authentication authentication;
    protected Authentication authenticationOther;
    protected LDAPBaseSecurityServiceConfig config;
    
    public static final String ldapServerUrl = LDAPTestUtils.LDAP_SERVER_URL;
    public static final String basePath = LDAPTestUtils.LDAP_BASE_PATH;
    
    @Rule
    public TemporaryResourceStore storeRule = TemporaryResourceStore.temp();
    
    @Before
    public void setUp() throws Exception {
    
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(storeRule.getStore());
        GeoServerSecurityManager securityManager = new GeoServerSecurityManager(
                new GeoServerDataDirectory(resourceLoader));
        securityProvider = new LDAPSecurityProvider(securityManager);
        
        createConfig();
        config.setServerURL(ldapServerUrl + "/" + basePath);
        config.setGroupSearchBase("ou=Groups");
        config.setGroupSearchFilter("member=cn={1}");
        config.setUseTLS(false);
        
        authentication = new UsernamePasswordAuthenticationToken("admin",
                "admin");
        authenticationOther = new UsernamePasswordAuthenticationToken("other",
                "other");
    }

    protected abstract void createConfig();
    
    @After
    public void tearDown() throws Exception {
        LdapTestUtils
                .destroyApacheDirectoryServer(LdapTestUtils.DEFAULT_PRINCIPAL,
                        LdapTestUtils.DEFAULT_PASSWORD);
        if(SecurityContextHolder.getContext() != null) {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }
}
