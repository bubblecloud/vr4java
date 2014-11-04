package org.bubblecloud.vr4java.rpc;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.bubblecloud.vr4java.security.CertificateService;
import org.bubblecloud.vr4java.util.BytesUtil;
import org.vaadin.addons.sitekit.util.CertificateUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Created by tlaukkan on 9/20/14.
 */
public class RpcSealerImpl implements RpcSealer {
    private static final Logger LOGGER = Logger.getLogger(RpcWsClient.class.getName());

    private final String identity;
    private String ownCertificateAlias;
    private final byte[] ownCertificateFingerprint;
    private final X509Certificate ownCertificate;
    private final PrivateKey privateKey;
    private String remoteFingerprint;
    private X509Certificate remoteCertificate;

    public RpcSealerImpl(final String identity) {
        this.identity = identity;
        ownCertificateAlias = CertificateService.getCertificateAlias(identity);
        if (ownCertificateAlias == null) {
            ownCertificateAlias = CertificateService.generateCertificate(identity);
        }
        ownCertificate = CertificateService.getCertificateCached(ownCertificateAlias);
        try {
            final Hex hex = new Hex();
            ownCertificateFingerprint = hex.decode(ownCertificateAlias.getBytes());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        privateKey = CertificateService.getPrivateKey(ownCertificateAlias);
    }

    @Override
    public byte[] getHandshake(final boolean request) {

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        final byte protocolVersion = RpcConstants.VERSION;
        final byte messageType;
        if (request) {
            messageType = RpcConstants.TYPE_HANDSHAKE_REQUEST;
        } else {
            messageType = RpcConstants.TYPE_HANDSHAKE_RESPONSE;
        }
        byte[] idBytes = new byte[4];
        byte[] sealBytes = new byte[RpcConstants.SEAL_LENGTH];

        outputStream.write(protocolVersion);
        outputStream.write(messageType);

        try {
            outputStream.write(idBytes);
            outputStream.write(sealBytes);
            outputStream.write(ownCertificate.getEncoded());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        final byte[] handshakeBytes = outputStream.toByteArray();
        seal(handshakeBytes);
        return handshakeBytes;
    }

    @Override
    public boolean verifyHandshake(byte[] handshakeBytes) {
        final byte protocolVersion = handshakeBytes[RpcConstants.VERSION_INDEX];
        if (protocolVersion != RpcConstants.VERSION) {
            LOGGER.warn("Handshake verification failed, invalid protocol version: " + protocolVersion);
            return false;
        }
        final byte messageType = handshakeBytes[RpcConstants.TYPE_INDEX];
        if (messageType != RpcConstants.TYPE_HANDSHAKE_REQUEST &&
                messageType != RpcConstants.TYPE_HANDSHAKE_RESPONSE) {
            LOGGER.warn("Handshake verification failed, invalid message type: " + messageType);
            return false;
        }
        final long timestamp = BytesUtil.readLong(handshakeBytes, RpcConstants.TIMESTAMP_INDEX);
        if (Math.abs(System.currentTimeMillis() - timestamp) > RpcConstants.VALIDITY_TIME_MILLIS) {
            LOGGER.warn("Handshake verification failed, expired timestamp: " + new Date(timestamp));
            return false;
        }

        try {
            final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            final InputStream inputStream = new ByteArrayInputStream(handshakeBytes,
                    RpcConstants.HEADER_LENGTH, handshakeBytes.length - RpcConstants.HEADER_LENGTH);
            remoteCertificate = (X509Certificate)certificateFactory.generateCertificate(inputStream);
            remoteFingerprint = DigestUtils.sha256Hex(remoteCertificate.getEncoded());
            if (CertificateService.getCertificateCached(remoteFingerprint) == null) {
                CertificateService.saveCertificate(remoteCertificate);
                LOGGER.trace("Saved new remote certificate: " + remoteCertificate);
            } else {
                LOGGER.trace("Known remote certificate: " + remoteFingerprint);
            }
            LOGGER.trace("Handshake completed with peer: " + remoteFingerprint);
        } catch (final Exception e) {
            LOGGER.warn("Invalid certificate as response to handshake.", e);
            return false;
        }

        return verify(handshakeBytes);
    }

    @Override
    public void seal(byte[] message) {
        final Hex hex = new Hex();
        final long timestamp = System.currentTimeMillis();
        BytesUtil.writeLong(timestamp, message, RpcConstants.TIMESTAMP_INDEX);
        BytesUtil.writeBytes(ownCertificateFingerprint, message,
                RpcConstants.SOURCE_INDEX, RpcConstants.SOURCE_LENGTH);

        try {
            //LOGGER.debug("Message being signed: " + new String(hex.encode(message)));
            final int signatureOffset = RpcConstants.SIGNATURE_INDEX;
            final int signatureLength = RpcConstants.SIGNATURE_LENGTH;
            final byte[] signatureBytes = message;
            sign(message, signatureBytes, signatureOffset, signatureLength);
            //LOGGER.debug("Message signature: " + new String(hex.encode(signatureBytes)));
        } catch (final Exception e) {
            throw new SecurityException("Error signing RPC message.", e);
        }
    }

    @Override
    public void sign(byte[] message, byte[] signatureBytes, int signatureOffset, int signatureLength) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        final Signature signature = Signature.getInstance(CertificateUtil.CERTIFICATE_SIGNATURE_ALGORITHM,
                CertificateUtil.PROVIDER);
        signature.initSign(privateKey);
        signature.update(message);
        signature.sign(signatureBytes, signatureOffset, signatureLength);
    }

    @Override
    public boolean verify(byte[] message) {
        final Hex hex = new Hex();
        final long timestamp = BytesUtil.readLong(message, RpcConstants.TIMESTAMP_INDEX);
        final byte[] sourceFingerprint = BytesUtil.readBytes(message,
                RpcConstants.SOURCE_INDEX, RpcConstants.SOURCE_LENGTH);
        final String sourceAlias = new String(hex.encode(sourceFingerprint));

        final X509Certificate sourceCertificate = CertificateService.getCertificateCached(sourceAlias);
        if (sourceCertificate == null) {
            LOGGER.debug("Unknown source: " + sourceAlias);
            return false;
        }

        if (Math.abs(System.currentTimeMillis() - timestamp) > RpcConstants.VALIDITY_TIME_MILLIS) {
            return false;
        }

        try {

            final byte[] signatureBytes = BytesUtil.readBytes(message, RpcConstants.SIGNATURE_INDEX, RpcConstants.SIGNATURE_LENGTH);
            BytesUtil.writeBytes(new byte[RpcConstants.SIGNATURE_LENGTH], message, RpcConstants.SIGNATURE_INDEX, RpcConstants.SIGNATURE_LENGTH);

            final boolean verified = verify(sourceCertificate, message, signatureBytes);
            if (verified) {
                return true;
            } else {
                LOGGER.warn("Signature verification failed.");
                return false;
            }
        } catch (final Exception e) {
            throw new SecurityException("Error verifying RPC message signature.", e);
        }
    }

    @Override
    public boolean verify(X509Certificate signerCertificate, byte[] message, byte[] signatureBytes) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException {
        final PublicKey publicKey = signerCertificate.getPublicKey();
        final Signature signature = Signature.getInstance(CertificateUtil.CERTIFICATE_SIGNATURE_ALGORITHM,
                CertificateUtil.PROVIDER);
        signature.initVerify(publicKey);
        signature.update(message);

        //LOGGER.debug("Message being verified: " + new String(hex.encode(message)));
        //LOGGER.debug("Message signature: " + new String(hex.encode(signatureBytes)));

        return signature.verify(signatureBytes);
    }

    @Override
    public String getIdentity() {
        return identity;
    }

    public String getRemoteFingerprint() {
        return remoteFingerprint;
    }

    public X509Certificate getRemoteCertificate() {
        return remoteCertificate;
    }

    @Override
    public String getFingerprint() {
        return ownCertificateAlias;
    }
}
