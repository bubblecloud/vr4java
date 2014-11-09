package org.bubblecloud.vr4java.dao;

import org.bubblecloud.vr4java.model.CuboidNode;
import org.bubblecloud.vr4java.model.Scene;
import org.bubblecloud.vr4java.model.SceneNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.addons.sitekit.dao.CompanyDao;
import org.vaadin.addons.sitekit.dao.UserDao;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.model.Group;
import org.vaadin.addons.sitekit.model.PostalAddress;
import org.vaadin.addons.sitekit.model.User;
import org.vaadin.addons.sitekit.util.PersistenceUtil;
import org.vaadin.addons.sitekit.util.TestUtil;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author Tommi Laukkanen
 *
 */
public final class SceneDaoTest {

    /** The entity manager for test. */
    private EntityManager entityManager;
    /** The company object for test. */
    private Company company;

    @Before
    public void before() throws Exception {
        TestUtil.before("vr4java", "vr4java-test");
        entityManager = TestUtil.getEntityManagerFactory().createEntityManager();
        company = CompanyDao.getCompany(entityManager, "*");
    }

    @After
    public void after() {
        TestUtil.after("vr4java", "vr4java-server");
    }

    /**
     * Tests scene persistence.
     */
    @Test
    public void testPersistence() {
        final Scene scene = new Scene();
        scene.setName("test-scene");
        scene.setOwnerCertificateFingerprint("test-fingerprint");
        scene.setOwner(company);
        scene.setX(1);
        scene.setY(2);
        scene.setZ(3);

        SceneDao.addScene(entityManager, scene);

        Assert.assertEquals(scene, SceneDao.getScene(entityManager, scene.getId()));
        Assert.assertEquals(Collections.singletonList(scene), SceneDao.getScenes(entityManager, company));

        scene.setName("test-scene-2");
        SceneDao.updateScene(entityManager, scene);

        Assert.assertEquals(scene, SceneDao.getScene(entityManager, scene.getId()));

        final SceneNode rootNode = new SceneNode();
        rootNode.setOwnerCertificateFingerprint("test-fingerprint");
        rootNode.setOwner(company);
        rootNode.setScene(scene);
        rootNode.setId(UUID.randomUUID());
        rootNode.setName("root-node");
        rootNode.setParentId(new UUID(0L, 0L));

        final CuboidNode cuboidNode = new CuboidNode(1,1,1,1,"");
        cuboidNode.setOwnerCertificateFingerprint("test-fingerprint");
        cuboidNode.setOwner(company);
        cuboidNode.setScene(scene);
        cuboidNode.setId(UUID.randomUUID());
        cuboidNode.setParentId(rootNode.getParentId());
        cuboidNode.setName("cuboid-node");

        final ArrayList<SceneNode> sceneNodes = new ArrayList<>();
        sceneNodes.add(cuboidNode);
        sceneNodes.add(rootNode);
        SceneNodeDao.addSceneNodes(entityManager, sceneNodes);

        Assert.assertEquals(sceneNodes, SceneNodeDao.getSceneNodes(entityManager, scene));
        cuboidNode.setX(2);

        SceneNodeDao.updateSceneNodes(entityManager, sceneNodes);

        Assert.assertEquals(sceneNodes, SceneNodeDao.getSceneNodes(entityManager, scene));

        SceneNodeDao.removeSceneNodes(entityManager, sceneNodes);

        Assert.assertEquals(Collections.emptyList(), SceneNodeDao.getSceneNodes(entityManager, scene));

        SceneDao.removeScene(entityManager, scene);

        Assert.assertEquals(Collections.emptyList(), SceneDao.getScenes(entityManager, company));
    }
}
