package org.bubblecloud.vr4java.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.addons.sitekit.dao.CompanyDao;
import org.vaadin.addons.sitekit.dao.UserDao;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.model.User;
import org.vaadin.addons.sitekit.util.TestUtil;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.net.URI;
import java.net.URL;

/**
 * Created by tlaukkan on 11/1/2014.
 */
public class SceneRepositoryTest {
    /** The entity manager for test. */
    private EntityManager entityManager;
    /** The audit entity manager for test. */
    private EntityManager auditEntityManager;
    /** The company object for test. */
    private Company company;
    /** The user */
    private User user;
    /** The test server context. */
    private ServerContext serverContext;

    @Before
    public void before() throws Exception {
        TestUtil.before("vr4java", "vr4java-server");
        final EntityManagerFactory entityManagerFactory = TestUtil.getEntityManagerFactory();
        final URI uri = new URL("http://127.0.0.1/rpc").toURI();
        entityManager = entityManagerFactory.createEntityManager();
        auditEntityManager = entityManagerFactory.createEntityManager();
        company = CompanyDao.getCompany(entityManager, "*");
        user = UserDao.getUser(entityManager, company, "admin@admin.org");
        serverContext = ServerContext.buildRpcServerContext(entityManager, auditEntityManager,
                "local-fingerprint", "remote-fingerprint",
                company, user, uri);
    }

    @After
    public void after() {
        TestUtil.after("vr4java", "vr4java-server");
    }

    @Test
    public void testRepository() {

        final SceneRepository sceneRepository = new SceneRepository(serverContext);

    }

}
