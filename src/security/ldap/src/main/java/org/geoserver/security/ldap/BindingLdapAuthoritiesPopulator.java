/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.Name;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.AuthenticatedLdapEntryContextCallback;
import org.springframework.ldap.core.AuthenticationErrorCallback;
import org.springframework.ldap.core.ContextExecutor;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.LdapEntryIdentification;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.util.Assert;

/**
 * BindingLdapAuthoritiesPopulator: modified DefaultLdapAuthoritiesPopulator
 * that binds the user before extracting roles.
 * 
 * Needed for Windows ActiveDirectory support and maybe other LDAP servers
 * requiring binding before searches.
 * 
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 * 
 */
public class BindingLdapAuthoritiesPopulator implements
        LdapAuthoritiesPopulator {

    

    // ~ Static fields/initializers
    // =====================================================================================

    private static final Log logger = LogFactory
            .getLog(BindingLdapAuthoritiesPopulator.class);

    // ~ Instance fields
    // ================================================================================================

    /**
     * A default role which will be assigned to all authenticated users if set
     */
    private GrantedAuthority defaultRole;

    private final SpringSecurityLdapTemplate ldapTemplate;

    /**
     * Controls used to determine whether group searches should be performed
     * over the full sub-tree from the base DN. Modified by searchSubTree
     * property
     */
    private final SearchControls searchControls = new SearchControls();

    /**
     * The ID of the attribute which contains the role name for a group
     */
    private String groupRoleAttribute = "cn";

    /**
     * The base DN from which the search for group membership should be
     * performed
     */
    private String groupSearchBase;

    /**
     * The pattern to be used for the user search. {0} is the user's DN
     */
    private String groupSearchFilter = "(member={0})";
    private String rolePrefix = "ROLE_";
    private boolean convertToUpperCase = true;

    // ~ Constructors
    // ===================================================================================================

    /**
     * Constructor for group search scenarios. <tt>userRoleAttributes</tt> may
     * still be set as a property.
     * 
     * @param contextSource
     *            supplies the contexts used to search for user roles.
     * @param groupSearchBase
     *            if this is an empty string the search will be performed from
     *            the root DN of the context factory. If null, no search will be
     *            performed.
     */
    public BindingLdapAuthoritiesPopulator(ContextSource contextSource,
            String groupSearchBase) {
        Assert.notNull(contextSource, "contextSource must not be null");

        // use a binding LdapTemplate, that doesn't make searches without
        // authentication
        ldapTemplate = new BindingLdapTemplate(contextSource);
        ldapTemplate.setSearchControls(searchControls);
        this.groupSearchBase = groupSearchBase;

        if (groupSearchBase == null) {
            logger.info("groupSearchBase is null. No group search will be performed.");
        } else if (groupSearchBase.length() == 0) {
            logger.info("groupSearchBase is empty. Searches will be performed from the context source base");
        }
    }

    // ~ Methods
    // ========================================================================================================

    /**
     * This method should be overridden if required to obtain any additional
     * roles for the given user (on top of those obtained from the standard
     * search implemented by this class).
     * 
     * @param user
     *            the context representing the user who's roles are required
     * @return the extra roles which will be merged with those returned by the
     *         group search
     */

    protected Set<GrantedAuthority> getAdditionalRoles(DirContext ctx,
            DirContextOperations user, String username) {
        return null;
    }

    /**
     * Obtains the authorities for the user who's directory entry is represented
     * by the supplied LdapUserDetails object.
     * 
     * @param user
     *            the user who's authorities are required (or user:password to
     *            be used to bind to ldap server prior to the search
     *            operations).
     * 
     * @return the set of roles granted to the user.
     */
    public final Collection<GrantedAuthority> getGrantedAuthorities(
            final DirContextOperations user, String username) {
        final String userDn = user.getNameInNamespace();

        if (logger.isDebugEnabled()) {
            logger.debug("Getting authorities for user " + userDn);
        }
        if (logger.isDebugEnabled() && username.startsWith("robert.sallee")) {
            logger.debug("We are here: ", new RuntimeException());
        }
        final List<GrantedAuthority> result = new ArrayList<GrantedAuthority>();

        // username is in the form username:password -> authenticate before
        // search
        if (username.indexOf(":") != -1) {
            if (logger.isDebugEnabled()) {
                logger.debug("Password included with username, authenticating before finding roles");
            }
            String[] userAndPassword = username.split(":");
            final String userName = userAndPassword[0];
            String password = userAndPassword[1];
            // authenticate and execute role extraction in the authenticated
            // context
            ldapTemplate.authenticate(DistinguishedName.EMPTY_PATH, userDn,
                    password, new AuthenticatedLdapEntryContextCallback() {

                        @Override
                        public void executeWithContext(DirContext ctx,
                                LdapEntryIdentification ldapEntryIdentification) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("User "+userDn+" authenticated, now finding roles with context");
                            }
                            getAllRoles(user, userDn, result, userName, ctx);
                            if (logger.isDebugEnabled()) {
                                logger.debug("Got roles, callback complete");
                            }
                        }
                    }, new AuthenticationErrorCallback(){

                        @Override
                        public void execute(Exception e) {
                            logger.warn("Error during authentication", e);
                        }
                        
                    });
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Password not included with username for user "+userDn+" Looking up roles without context");
            }
            getAllRoles(user, userDn, result, username, null);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Roles found: "+ StringUtils.join(result.iterator(), ", "));
        }
        return result;
    }

    public Set<GrantedAuthority> getGroupMembershipRoles(final DirContext ctx,
            String userDn, String username) {
        if (getGroupSearchBase() == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Search base is null.  Will not search for groups.");
            }
            return new HashSet<GrantedAuthority>();
        }

        Set<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();

        if (logger.isDebugEnabled()) {
            logger.debug("Searching for roles for user '" + username
                    + "', DN = " + "'" + userDn + "', with filter "
                    + groupSearchFilter + " in search base '"
                    + getGroupSearchBase() + "'");
        }
        SpringSecurityLdapTemplate authTemplate;

        authTemplate = (SpringSecurityLdapTemplate) LDAPUtils
                .getLdapTemplateInContext(ctx, ldapTemplate);
        Set<String> userRoles = authTemplate.searchForSingleAttributeValues(
                getGroupSearchBase(), groupSearchFilter, new String[] { userDn,
                        username }, groupRoleAttribute);

        if (logger.isDebugEnabled()) {
            logger.debug("Roles from search: " + userRoles);
        }

        for (String role : userRoles) {

            if (convertToUpperCase) {
                role = role.toUpperCase();
            }

            authorities.add(new SimpleGrantedAuthority(rolePrefix + role));
        }

        return authorities;
    }

    

    protected ContextSource getContextSource() {
        return ldapTemplate.getContextSource();
    }

    protected String getGroupSearchBase() {
        return groupSearchBase;
    }

    /**
     * @deprecated Convert case in the {@code AuthenticationProvider} using a
     *             {@code GrantedAuthoritiesMapper}.
     */
    @Deprecated
    public void setConvertToUpperCase(boolean convertToUpperCase) {
        this.convertToUpperCase = convertToUpperCase;
    }

    /**
     * The default role which will be assigned to all users.
     * 
     * @param defaultRole
     *            the role name, including any desired prefix.
     * @deprecated Assign a default role in the {@code AuthenticationProvider}
     *             using a {@code GrantedAuthoritiesMapper}.
     */
    @Deprecated
    public void setDefaultRole(String defaultRole) {
        Assert.notNull(defaultRole,
                "The defaultRole property cannot be set to null");
        this.defaultRole = new SimpleGrantedAuthority(defaultRole);
    }

    public void setGroupRoleAttribute(String groupRoleAttribute) {
        Assert.notNull(groupRoleAttribute,
                "groupRoleAttribute must not be null");
        this.groupRoleAttribute = groupRoleAttribute;
    }

    public void setGroupSearchFilter(String groupSearchFilter) {
        Assert.notNull(groupSearchFilter, "groupSearchFilter must not be null");
        this.groupSearchFilter = groupSearchFilter;
    }

    /**
     * Sets the prefix which will be prepended to the values loaded from the
     * directory. Defaults to "ROLE_" for compatibility with <tt>RoleVoter/tt>.
     * 
     * @deprecated Map the authorities in the {@code AuthenticationProvider}
     *             using a {@code GrantedAuthoritiesMapper}.
     */
    @Deprecated
    public void setRolePrefix(String rolePrefix) {
        Assert.notNull(rolePrefix, "rolePrefix must not be null");
        this.rolePrefix = rolePrefix;
    }

    /**
     * If set to true, a subtree scope search will be performed. If false a
     * single-level search is used.
     * 
     * @param searchSubtree
     *            set to true to enable searching of the entire tree below the
     *            <tt>groupSearchBase</tt>.
     */
    public void setSearchSubtree(boolean searchSubtree) {
        int searchScope = searchSubtree ? SearchControls.SUBTREE_SCOPE
                : SearchControls.ONELEVEL_SCOPE;
        searchControls.setSearchScope(searchScope);
    }

    /**
     * Sets the corresponding property on the underlying template, avoiding
     * specific issues with Active Directory.
     * 
     * @see LdapTemplate#setIgnoreNameNotFoundException(boolean)
     */
    public void setIgnorePartialResultException(boolean ignore) {
        ldapTemplate.setIgnorePartialResultException(ignore);
    }

    private void getAllRoles(final DirContextOperations user,
            final String userDn, final List<GrantedAuthority> result,
            final String userName, DirContext ctx) {
        if (logger.isDebugEnabled()) {
            logger.debug("getting all roles for user "+userName);
        }

        Set<GrantedAuthority> roles = getGroupMembershipRoles(ctx, userDn,
                userName);

        Set<GrantedAuthority> extraRoles = getAdditionalRoles(ctx, user,
                userName);

        if (extraRoles != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Found extra roles.  Applying.");
            }
            roles.addAll(extraRoles);
        }

        if (defaultRole != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Found default role. Applying.");
            }
            roles.add(defaultRole);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Applying group membership roles");
        }
        result.addAll(roles);
    }
}
