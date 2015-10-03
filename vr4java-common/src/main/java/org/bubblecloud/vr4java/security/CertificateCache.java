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
package org.bubblecloud.vr4java.security;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.bubblecloud.ilves.cache.InMemoryCache;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Cache for user TLS client certificates.
 *
 * @author Tommi S.E. Laukkanen
 */
public class CertificateCache {

    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(CertificateCache.class);

    /**
     * The certificate cache.
     */
    private static InMemoryCache<String, X509Certificate> certificateCache = new InMemoryCache<String, X509Certificate>(
            10 * 60 * 1000, 60 * 1000, 1000);

    /**
     * The blacklisted certificate cache.
     */
    private static InMemoryCache<String, String> blacklistCache = new InMemoryCache<String, String>(
            2 * 60 * 1000, 30 * 1000, 1000);

    /**
     * Get certificate by alias.
     *
     * @param alias the alias
     * @return the certificate or null
     */
    public static synchronized X509Certificate getCertificate(final String alias) {
        if (blacklistCache.get(alias) != null) {
            LOGGER.trace("Certificate cache blacklist hit: " + alias);
            return null;
        }
        final X509Certificate cachedCertificate = certificateCache.get(alias);
        if (cachedCertificate != null) {
            return cachedCertificate;
        }
        final X509Certificate loadedCertificate = CertificateService.getCertificate(alias);

        if (loadedCertificate != null) {
            LOGGER.trace("Certificate loaded to cache: " + alias);
            certificateCache.put(alias, loadedCertificate);
            return loadedCertificate;
        } else {
            blacklistCache.put(alias, alias);
            LOGGER.trace("Certificate cache blacklist: " + alias);
            return null;
        }
    }

    /**
     * Evicts certificate from cache.
     * @param alias the certificate alias
     */
    public static synchronized void evict(final String alias) {
        certificateCache.remove(alias);
        blacklistCache.remove(alias);
    }
}
