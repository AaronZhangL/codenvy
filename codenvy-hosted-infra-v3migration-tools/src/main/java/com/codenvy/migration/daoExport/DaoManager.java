package com.codenvy.migration.daoExport;

import com.codenvy.api.organization.server.dao.OrganizationDao;
import com.codenvy.api.organization.server.exception.OrganizationException;
import com.codenvy.api.organization.shared.dto.Organization;
import com.codenvy.api.organization.shared.dto.Subscription;
import com.codenvy.api.user.server.dao.MemberDao;
import com.codenvy.api.user.server.dao.UserDao;
import com.codenvy.api.user.server.dao.UserProfileDao;
import com.codenvy.api.user.server.exception.MembershipException;
import com.codenvy.api.user.server.exception.UserException;
import com.codenvy.api.user.server.exception.UserProfileException;
import com.codenvy.api.user.shared.dto.Profile;
import com.codenvy.api.user.shared.dto.User;
import com.codenvy.api.workspace.server.dao.WorkspaceDao;
import com.codenvy.api.workspace.server.exception.WorkspaceException;
import com.codenvy.api.workspace.shared.dto.Workspace;
import com.codenvy.api.dao.ldap.UserAttributesMapper;
import com.codenvy.api.dao.ldap.UserDaoImpl;
import com.codenvy.api.dao.mongo.OrganizationDaoImpl;
import com.codenvy.api.dao.mongo.MemberDaoImpl;
import com.codenvy.api.dao.mongo.UserProfileDaoImpl;
import com.codenvy.api.dao.mongo.WorkspaceDaoImpl;
import com.mongodb.DB;
import com.mongodb.MongoClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.Properties;

public class DaoManager {
    private static final Logger LOG = LoggerFactory.getLogger(DaoManager.class);

    /* Names of attributes which use for configuration for LDAP */
    protected static final String LDAP_URL            = "java.naming.provider.url";
    protected static final String LDAP_PRINCIPAL      = "java.naming.security.principal";
    protected static final String LDAP_CREDENTIAL     = "java.naming.security.credentials";
    protected static final String LDAP_AUTH           = "java.naming.security.authentication";
    protected static final String LDAP_POOL           = "com.sun.jndi.ldap.connect.pool";
    protected static final String LDAP_POOL_SIZE      = "com.sun.jndi.ldap.connect.pool.initsize";
    protected static final String LDAP_POOL_MAX_SIZE  = "com.sun.jndi.ldap.connect.pool.maxsize";
    protected static final String LDAP_PREF_SIZE      = "com.sun.jndi.ldap.connect.pool.prefsize";
    protected static final String LDAP_TIMEOUT        = "com.sun.jndi.ldap.connect.pool.timeout";
    protected static final String LDAP_USER_CONTAINER = "user.ldap.user_container_dn";

    /* Names of attributes which use for configuration for Mongo */
    protected static final String DB_URL                  = "organization.storage.db.url";
    protected static final String DB_NAME                 = "organization.storage.db.name";
    protected static final String DB_USERNAME             = "organization.storage.db.username";
    protected static final String DB_PASSWORD             = "organization.storage.db.password";
    protected static final String COLLECTION_PROFILE      = "organization.storage.db.profile.collection";
    protected static final String COLLECTION_WORKSPACE    = "organization.storage.db.workspace.collection";
    protected static final String COLLECTION_ORGANIZATION = "organization.storage.db.organization.collection";
    protected static final String COLLECTION_WS_MEMBER    = "organization.storage.db.ws.member.collection";
    protected static final String COLLECTION_SUBSCRIPTION = "organization.storage.db.subscription.collection";
    protected static final String COLLECTION_ACC_MEMBER   = "organization.storage.db.org.member.collection";

    private UserDao         userDao;
    private UserProfileDao  userProfileDao;
    private WorkspaceDao    workspaceDao;
    private MemberDao       memberDao;
    private OrganizationDao organizationDao;

    private volatile DB db;

    public DaoManager(Properties ldapProperties, Properties mongoProperties) {
        userDao = new UserDaoImpl(ldapProperties.getProperty(LDAP_URL),
                                  ldapProperties.getProperty(LDAP_PRINCIPAL),
                                  ldapProperties.getProperty(LDAP_CREDENTIAL),
                                  ldapProperties.getProperty(LDAP_AUTH),
                                  ldapProperties.getProperty(LDAP_POOL),
                                  ldapProperties.getProperty(LDAP_POOL_SIZE),
                                  ldapProperties.getProperty(LDAP_POOL_MAX_SIZE),
                                  ldapProperties.getProperty(LDAP_PREF_SIZE),
                                  ldapProperties.getProperty(LDAP_TIMEOUT),
                                  ldapProperties.getProperty(LDAP_USER_CONTAINER),
                                  new UserAttributesMapper()
        );

        try {
            MongoClient mongoClient = new MongoClient(mongoProperties.getProperty(DB_URL));
            db = mongoClient.getDB(mongoProperties.getProperty(DB_NAME));
            if (!db.authenticate(mongoProperties.getProperty(DB_USERNAME), mongoProperties.getProperty(DB_PASSWORD).toCharArray()))
                throw new RuntimeException("Incorrect MongoDB credentials: authentication failed.");
        } catch (UnknownHostException e) {
            throw new RuntimeException("Can't connect to MongoDB.");
        }

        userProfileDao = new UserProfileDaoImpl(userDao, db, mongoProperties.getProperty(COLLECTION_PROFILE));
        workspaceDao = new WorkspaceDaoImpl(userDao, db, mongoProperties.getProperty(COLLECTION_WORKSPACE));
        memberDao = new MemberDaoImpl(userDao, workspaceDao, db, mongoProperties.getProperty(COLLECTION_WS_MEMBER));
        organizationDao = new OrganizationDaoImpl(db, mongoProperties.getProperty(COLLECTION_ORGANIZATION),
                                                  mongoProperties.getProperty(COLLECTION_SUBSCRIPTION),
                                                  mongoProperties.getProperty(COLLECTION_ACC_MEMBER));
    }

    public void addUser(User user) throws UserException {
        userDao.create(user);
        LOG.debug("User was created: " + user.toString());
    }

    public void addProfile(Profile profile) throws UserProfileException {
        userProfileDao.create(profile);
        LOG.debug("Profile was created: " + profile);
    }

    public void addWorkspace(Workspace workspace) throws WorkspaceException {
        workspaceDao.create(workspace);
        LOG.debug("Workspace was created: " + workspace);
    }

    public void addOrganization(Organization organization) throws OrganizationException {
        organizationDao.create(organization);
        LOG.debug("Organization was created: " + organization);
    }

    public void addOrganizationMember(com.codenvy.api.organization.shared.dto.Member member) throws OrganizationException {
        if (!organizationDao.getMembers(member.getOrganizationId()).contains(member)) {
            organizationDao.addMember(member);
            LOG.debug("Member was added to organization: " + member);
        }
    }

    //TODO remove not needed accoutId
    public void addOrganizationSubcription(Subscription subscription, String accountId) throws OrganizationException {
        organizationDao.addSubscription(subscription);
        LOG.debug("Subscription was created: " + subscription);
    }

    public void addWorkspaceMember(com.codenvy.api.user.shared.dto.Member member) throws MembershipException {
        memberDao.create(member);
        LOG.debug("Member was added to workspace: " + member);
    }
}
