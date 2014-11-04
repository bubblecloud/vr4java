package org.bubblecloud.vr4java.security;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.vaadin.addons.sitekit.util.CertificateUtil;

import javax.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * Created by tlaukkan on 9/20/14.
 */
public class CertificateServiceTest {

    @Test
    public void testCertificates() throws Exception {
        final String identity = "test-identity";

        if (CertificateService.getCertificateAlias(identity) != null) {
            CertificateService.removeCertificate(identity);
        }
        Assert.assertNull(CertificateService.getCertificateAlias(identity));
        final String alias = CertificateService.generateCertificate(identity);
        Assert.assertNotNull(alias);
        Assert.assertEquals(alias, CertificateService.getCertificateAlias(identity));

        final X509Certificate certificate = CertificateService.getCertificateCached(alias);
        Assert.assertNotNull(certificate);
        Assert.assertEquals(alias, DigestUtils.sha256Hex(certificate.getEncoded()));
        Assert.assertNotNull(CertificateService.getCertificateCached(alias));

        CertificateService.removeCertificate(identity);

        Assert.assertNull(CertificateService.getCertificateAlias(identity));
        Assert.assertNull(CertificateService.getCertificateCached(alias));
        Assert.assertNull(CertificateService.getCertificateCached(alias));

        CertificateService.saveCertificate(certificate);
        Assert.assertEquals(alias, CertificateService.getCertificateAlias(alias));
        Assert.assertNotNull(CertificateService.getCertificateCached(alias));
        Assert.assertEquals(certificate, CertificateService.getCertificateCached(alias));
        CertificateService.removeCertificate(alias);

        Assert.assertNull(CertificateService.getCertificateAlias(identity));
        Assert.assertNull(CertificateService.getCertificateCached(alias));
        Assert.assertNull(CertificateService.getCertificateCached(alias));
    }

}
