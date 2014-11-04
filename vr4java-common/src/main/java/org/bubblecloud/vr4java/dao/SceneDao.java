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
import org.vaadin.addons.sitekit.model.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Scene data access object.
 *
 * @author Tommi S.E. Laukkanen
 */
public class SceneDao {

    /**
     * The logger.
     */
    private static final Logger LOG = Logger.getLogger(SceneDao.class);

    /**
     * Adds scene to database.
     *
     * @param entityManager the entity manager
     * @param scene          the scene
     */
    public static final void addScene(final EntityManager entityManager, final Scene scene) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            scene.setCreated(new Date());
            scene.setModified(scene.getCreated());
            entityManager.persist(scene);
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in add scene.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates scene to database.
     *
     * @param entityManager the entity manager
     * @param scene          the scene
     */
    public static final void updateScene(final EntityManager entityManager, final Scene scene) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            scene.setModified(new Date());
            entityManager.persist(scene);
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in update scene.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes scene from database.
     *
     * @param entityManager the entity manager
     * @param scene          the scene
     */
    public static final void removeScene(final EntityManager entityManager, final Scene scene) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            entityManager.remove(scene);
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in remove scene.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets given scene.
     *
     * @param entityManager the entity manager.
     * @param sceneId        the scene ID
     * @return the group
     */
    public static final Scene getScene(final EntityManager entityManager, final UUID sceneId) {
        try {
            return entityManager.getReference(Scene.class, sceneId.toString());
        } catch (final EntityNotFoundException e) {
            return null;
        }
    }

    /**
     * Gets scenes of given company.
     *
     * @param entityManager the entity manager.
     * @param company the company
     * @return the group
     */
    public static final List<Scene> getScenes(final EntityManager entityManager, final Company company) {
        final CriteriaBuilder builder =  entityManager.getCriteriaBuilder();
        final CriteriaQuery<Scene> criteriaQuery = builder.createQuery(Scene.class);
        final Root<Scene> sceneRoot = criteriaQuery.from(Scene.class);
        criteriaQuery.select(sceneRoot).where(builder.equal(sceneRoot.get("owner"), company));
        final TypedQuery<Scene> query = entityManager.createQuery(criteriaQuery);
        return query.getResultList();
    }

}