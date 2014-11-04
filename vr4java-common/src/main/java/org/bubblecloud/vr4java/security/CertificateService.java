package org.bubblecloud.vr4java.security;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.vaadin.addons.sitekit.util.CertificateUtil;

import javax.security.cert.Certificate;
import java.io.*;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 * Created by tlaukkan on 9/20/14.
 */
public class CertificateService {
    private static final Logger LOGGER = Logger.getLogger(CertificateService.class.getName());

    public static final String CERTIFICATE_ALIAS_PROPERTIES_PATH = "./certificate-alias.properties";

    public static final String CERTIFICATE_STORE_PATH = "./certificate-store.bks";

    public static final String CERTIFICATE_STORE_PASSWORD = "changeme";

    public static final String CERTIFICATE_STORE_KEY_ENTRY_PASSWORD = "changeme";

    public synchronized static String generateCertificate(final String identity) {
        final String alias = CertificateUtil.generateSelfSignedCertificate(identity, null,
                CERTIFICATE_STORE_PATH, CERTIFICATE_STORE_PASSWORD, CERTIFICATE_STORE_KEY_ENTRY_PASSWORD);
        putCertificateAlias(identity, alias);
        CertificateCache.evict(alias);
        return alias;
    }

    public synchronized static String saveCertificate(final X509Certificate certificate) {
        try {
            final String alias = DigestUtils.sha256Hex(certificate.getEncoded());
            if (getCertificate(alias) != null) {
                throw new SecurityException("Certificate already stored: " + certificate);
            }
            final String identity = alias; // For remote certificates alias equals identity.
            CertificateUtil.saveCertificate(alias, CERTIFICATE_STORE_PATH, CERTIFICATE_STORE_PASSWORD, certificate);
            putCertificateAlias(identity, alias);
            CertificateCache.evict(alias);
            return alias;
        } catch (final Exception e) {
            throw new SecurityException("Erros saving certificate: " + certificate.toString(), e);
        }
    }

    public synchronized static X509Certificate getCertificateCached(final String alias) {
        return CertificateCache.getCertificate(alias);
    }

    protected synchronized static X509Certificate getCertificate(final String alias) {
        return CertificateUtil.getCertificate(alias, CERTIFICATE_STORE_PATH, CERTIFICATE_STORE_PASSWORD);
    }


    public synchronized static PrivateKey getPrivateKey(final String alias) {
        return CertificateUtil.getPrivateKey(alias, CERTIFICATE_STORE_PATH, CERTIFICATE_STORE_PASSWORD,
                CERTIFICATE_STORE_KEY_ENTRY_PASSWORD);
    }

    public synchronized static void removeCertificate(final String identity) {
        final String alias = getCertificateAlias(identity);
        CertificateUtil.removeCertificate(alias, CERTIFICATE_STORE_PATH, CERTIFICATE_STORE_PASSWORD);
        removeCertificateAlias(identity);
        CertificateCache.evict(alias);
    }

    public synchronized static String getCertificateAlias(final String identity) {
        try {
            final Properties properties = new Properties();
            final File file = new File(CERTIFICATE_ALIAS_PROPERTIES_PATH);
            if (!file.exists()) {
                return null;
            }
            final InputStream inputStream = new FileInputStream(CERTIFICATE_ALIAS_PROPERTIES_PATH);
            properties.load(inputStream);
            inputStream.close();
            return (String) properties.get(identity);
        } catch (final IOException e) {
            LOGGER.error("Error loading certificate alias properties.", e);
            throw new RuntimeException(e);
        }
    }

    private static void putCertificateAlias(final String identity, final String alias) {
        try {
            final Properties properties = new Properties();
            if (new File(CERTIFICATE_ALIAS_PROPERTIES_PATH).exists()) {
                final InputStream inputStream = new FileInputStream(CERTIFICATE_ALIAS_PROPERTIES_PATH);
                properties.load(inputStream);
                inputStream.close();
            } else {
                //properties.load((Reader) null);
            }
            properties.put(identity, alias);

            final OutputStream outputStream = new FileOutputStream(CERTIFICATE_ALIAS_PROPERTIES_PATH, false);
            properties.store(outputStream, "");
        } catch (final IOException e) {
            LOGGER.error("Error loading certificate alias properties.", e);
            throw new RuntimeException(e);
        }
    }

    private static void removeCertificateAlias(final String identity) {
        try {
            final Properties properties = new Properties();
            if (new File(CERTIFICATE_ALIAS_PROPERTIES_PATH).exists()) {
                final InputStream inputStream = new FileInputStream(CERTIFICATE_ALIAS_PROPERTIES_PATH);
                properties.load(inputStream);
                inputStream.close();
            } else {
                properties.load((InputStream) null);
            }
            properties.remove(identity);

            final OutputStream outputStream = new FileOutputStream(CERTIFICATE_ALIAS_PROPERTIES_PATH, false);
            properties.store(outputStream, "");
        } catch (final IOException e) {
            LOGGER.error("Error loading certificate alias properties.", e);
            throw new RuntimeException(e);
        }
    }

}
