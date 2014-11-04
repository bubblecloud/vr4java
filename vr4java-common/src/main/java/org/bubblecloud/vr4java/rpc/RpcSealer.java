package org.bubblecloud.vr4java.rpc;

import org.apache.log4j.Logger;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.X509Certificate;

/**
 * Created by tlaukkan on 9/20/14.
 */
public interface RpcSealer {

    /**
     * Gets identity of this sealer.
     * @return the identity
     */
    String getIdentity();

    /**
     * Get remote certificate fingerprint.
     * @return the remote certificate fingerprint
     */
    String getRemoteFingerprint();

    /**
     * Get remote certificate.
     * @return the remote certificate
     */
    X509Certificate getRemoteCertificate();

    /**
     * Get handshake request.
     * @return the handshake request bytes.
     */
    byte[] getHandshake(final boolean request);

    /**
     * Verify handshake response.
     * @param response the response
     * @return TRUE if handshake was success.
     */
    boolean verifyHandshake(final byte[] response);

    /**
     * Seal RPC message.
     * @param message the message bytes
     */
    void seal(final byte[] message);

    /**
     * Sign message.
     * @param message
     * @param signatureBytes
     * @param signatureOffset
     * @param signatureLength
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    void sign(byte[] message, byte[] signatureBytes, int signatureOffset, int signatureLength) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException;

    /**
     * Verify message signature.
     * @param signerCertificate the signer certificate
     * @param message the message
     * @param signatureBytes the signature bytes
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    boolean verify(X509Certificate signerCertificate, byte[] message, byte[] signatureBytes) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, SignatureException;

    /**
     * Verify RPC message.
     * @param message the message bytes
     * @return true if verification was successful.
     */
    boolean verify(final byte[] message);

    String getFingerprint();
}
