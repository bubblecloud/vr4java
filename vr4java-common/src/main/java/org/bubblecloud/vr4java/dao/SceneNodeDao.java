/**
 * Copyright 2013 Tommi S.E. Laukkanen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bubblecloud.vr4java.dao;

import org.apache.log4j.Logger;
import org.bubblecloud.vr4java.model.Scene;
import org.bubblecloud.vr4java.model.SceneNode;

import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * SceneNode data access object.
 *
 * @author Tommi S.E. Laukkanen
 */
public class SceneNodeDao {

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(SceneNodeDao.class);

    /**
     * Adds scene nodes to database.
     *
     * @param entityManager the entity manager
     * @param sceneNodes the scene nodes to add
     */
    public static final void addSceneNodes(final EntityManager entityManager, final List<SceneNode> sceneNodes) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            for (final SceneNode sceneNode : sceneNodes) {
                sceneNode.setCreated(new Date());
                sceneNode.setModified(sceneNode.getCreated());
                entityManager.persist(sceneNode);
            }
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in add sceneNode.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates scene nodes to database.
     *
     * @param entityManager the entity manager
     * @param sceneNodes the scene nodes to update
     */
    public static final void updateSceneNodes(final EntityManager entityManager, final List<SceneNode> sceneNodes) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            for (final SceneNode sceneNode : sceneNodes) {
                sceneNode.setModified(new Date());
                entityManager.persist(sceneNode);
            }
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in update sceneNode.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes scene nodes from database.
     *
     * @param entityManager the entity manager
     * @param sceneNodes the scene nodes to remove
     */
    public static final void removeSceneNodes(final EntityManager entityManager, final List<SceneNode> sceneNodes) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            for (final SceneNode sceneNode : sceneNodes) {
                entityManager.remove(sceneNode);
            }
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in remove sceneNode.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets given sceneNode.
     *
     * @param entityManager the entity manager.
     * @param sceneNodeId        the sceneNode ID
     * @return the group
     */
    public static final SceneNode getSceneNode(final EntityManager entityManager, final UUID sceneNodeId) {
        try {
            return entityManager.getReference(SceneNode.class, sceneNodeId.toString());
        } catch (final EntityNotFoundException e) {
            return null;
        }
    }

    /**
     * Gets given scene nodes of given scene.
     *
     * @param entityManager the entity manager.
     * @param scene  the scene
     * @return the group
     */
    public static final List<SceneNode> getSceneNodes(final EntityManager entityManager, final Scene scene) {
        final CriteriaBuilder builder =  entityManager.getCriteriaBuilder();
        final CriteriaQuery<SceneNode> query = builder.createQuery(SceneNode.class);
        final Root<SceneNode> root = query.from(SceneNode.class);
        query.select(root).where(builder.equal(root.get("scene"), scene)).orderBy(builder.asc(root.get("name")));
        final TypedQuery<SceneNode> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }

}