package org.bubblecloud.vr4java.server;

import org.vaadin.addons.sitekit.dao.CompanyDao;
import org.vaadin.addons.sitekit.dao.UserDao;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.model.Group;
import org.vaadin.addons.sitekit.model.User;
import org.vaadin.addons.sitekit.site.DefaultSiteUI;
import org.vaadin.addons.sitekit.site.ProcessingContext;
import org.vaadin.addons.sitekit.util.PropertiesUtil;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by tlaukkan on 11/1/2014.
 */
public class ServerContext extends ProcessingContext {

    private static final List<String> APPLICATION_ROLES = Arrays.asList("administrator", "vruser");
    private String serverCertificateFingerprint;
    private String userCertificateFingerprint;
    private Company company;
    private User user;
    final List<Group> groups;

    public static ServerContext buildLocalServerContext(final String serverIdentity) {
        final String serverCertificateFingerprint = PropertiesUtil.getProperty("certificate-alias", serverIdentity);
        final EntityManagerFactory entityManagerFactory = DefaultSiteUI.getEntityManagerFactory();
        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        final EntityManager auditEntityManager = entityManagerFactory.createEntityManager();
        final Company company = CompanyDao.getCompany(entityManager, "*");
        final User user = UserDao.getUser(entityManager, company, "admin@admin.org");
        final List<Group> groups = UserDao.getUserGroups(entityManager, company, user);

        final List<String> roles = new ArrayList<String>();
        for (final Group group : groups) {
            final String roleNameCandidate = group.getName().toLowerCase();
            if (APPLICATION_ROLES.contains(roleNameCandidate)) {
                roles.add(roleNameCandidate);
            }
        }

        return new ServerContext(
            entityManager,
            auditEntityManager,
                serverIdentity,
            "local-ip-undefined",
            -1,
            "vr4java",
            "remote-host-undefined",
            "remote-ip-undefined",
            -1,
            groups,
            roles,
            serverCertificateFingerprint,
            serverCertificateFingerprint,
            company,
            user
        );
    }

    public static ServerContext buildRpcServerContext(final EntityManager entityManager, final EntityManager auditEntityManager,
                                                      final String serverCertificateFingerprint,
                                                      final String userCertificateFingerprint,
                                                      final Company company, final User user, final URI uri) {
        final List<Group> groups = UserDao.getUserGroups(entityManager, company, user);

        final List<String> roles = new ArrayList<String>();
        for (final Group group : groups) {
            final String roleNameCandidate = group.getName().toLowerCase();
            if (APPLICATION_ROLES.contains(roleNameCandidate)) {
                roles.add(roleNameCandidate);
            }
        }

        return new ServerContext(
                entityManager,
                auditEntityManager,
                uri.getHost(),
                "local-ip-undefined",
                uri.getPort(),
                "vr4java",
                "remote-host-undefined",
                "remote-ip-undefined",
                -1,
                groups,
                roles,
                serverCertificateFingerprint,
                userCertificateFingerprint,
                company,
                user
        );
    }

    public ServerContext(
            final EntityManager entityManager,
            final EntityManager auditEntityManager,
            final String serverName,
            final String localIpAddress,
            final Integer componentPort,
            final String componentType,
            final String remoteHost,
            final String remoteIpAddress,
            final Integer remotePort,
            final List<Group> groups,
            final List<String> roles,
            final String serverCertificateFingerprint,
            final String userCertificateFingerprint,
            final Company company,
            final User user) {
        super(entityManager, auditEntityManager,
                serverName, localIpAddress, componentPort, componentType,
                remoteHost, remoteIpAddress, remotePort,
                user.getUserId(), user.getEmailAddress(), roles);
        this.serverCertificateFingerprint = serverCertificateFingerprint;
        this.userCertificateFingerprint = userCertificateFingerprint;
        this.company = company;
        this.user = user;
        this.groups = groups;
    }

    public String getServerCertificateFingerprint() {
        return serverCertificateFingerprint;
    }

    public String getUserCertificateFingerprint() {
        return userCertificateFingerprint;
    }

    public Company getCompany() {
        return company;
    }

    public User getUser() {
        return user;
    }

    public List<Group> getGroups() {
        return groups;
    }
}
