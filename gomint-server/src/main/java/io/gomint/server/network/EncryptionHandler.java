/*
 *  Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
 *
 *  This code is licensed under the BSD license found in the
 *  LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.network;

import io.gomint.server.jni.NativeCode;
import io.gomint.server.jni.hash.Hash;
import io.gomint.server.jni.hash.JavaHash;
import io.gomint.server.jni.hash.NativeHash;
import io.gomint.util.random.FastRandom;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles all encryption needs of the Minecraft Pocket Edition Protocol (ECDH Key Exchange and
 * shared secret generation).
 *
 * @author BlackyPaw
 * @version 1.0
 */
public class EncryptionHandler {

    private static final NativeCode<Hash> HASHING = new NativeCode<>( "hash", JavaHash.class, NativeHash.class );
    private static final Logger LOGGER = LoggerFactory.getLogger( EncryptionHandler.class );
    private static final ThreadLocal<Hash> SHA256_DIGEST = new ThreadLocal<>();

    static {
        HASHING.load();
    }

    // Holder for the server keypair
    private final EncryptionKeyFactory keyFactory;

    // Packet counters
    private AtomicLong sendingCounter = new AtomicLong( 0 );
    private AtomicLong receiveCounter = new AtomicLong( 0 );

    // Client Side:
    private ECPublicKey clientPublicKey;
    private Cipher clientEncryptor;
    private Cipher clientDecryptor;

    // Data for packet and checksum calculations
    @Getter
    private byte[] clientSalt;
    private byte[] key;

    // Server side
    private PublicKey serverPublicKey;
    private Cipher serverEncryptor;
    private Cipher serverDecryptor;
    private AtomicLong serverSendCounter = new AtomicLong( 0 );
    private AtomicLong serverReceiveCounter = new AtomicLong( 0 );
    private byte[] serverKey;

    /**
     * Create a new EncryptionHandler for the client
     *
     * @param keyFactory The keyFactory which created the server keypair
     */
    public EncryptionHandler( EncryptionKeyFactory keyFactory ) {
        this.keyFactory = keyFactory;
    }

    /**
     * Supplies the needed public key of the login to create the right encryption pairs
     *
     * @param key The key which should be used to encrypt traffic
     */
    public void supplyClientKey( ECPublicKey key ) {
        this.clientPublicKey = key;
    }

    /**
     * Sets up everything required to begin encrypting network data sent to or received from the client.
     *
     * @return Whether or not the setup completed successfully
     */
    public boolean beginClientsideEncryption() {
        if ( this.clientEncryptor != null && this.clientDecryptor != null ) {
            // Already initialized:
            LOGGER.debug( "Already initialized" );
            return true;
        }

        // Generate a random salt:
        this.clientSalt = new byte[16];
        FastRandom.current().nextBytes( this.clientSalt );

        // Generate shared secret from ECDH keys:
        byte[] secret = this.generateECDHSecret( this.keyFactory.getKeyPair().getPrivate(), this.clientPublicKey );
        if ( secret == null ) {
            return false;
        }

        // Derive key as salted SHA-256 hash digest:
        this.key = this.hashSHA256( this.clientSalt, secret );
        byte[] iv = this.takeBytesFromArray( this.key, 0, 16 );

        // Initialize BlockCiphers:
        this.clientEncryptor = this.createCipher( true, this.key, iv );
        this.clientDecryptor = this.createCipher( false, this.key, iv );
        return true;
    }

    /**
     * Decrypt data from the clients
     *
     * @param input RAW packet data from RakNet
     * @return Either null when the data was corrupted or the decrypted data
     */
    public byte[] decryptInputFromClient( byte[] input ) {
        byte[] output = this.processCipher( this.clientDecryptor, input );
        if ( output == null ) {
            return null;
        }

        byte[] outputChunked = new byte[input.length - 8];

        System.arraycopy( output, 0, outputChunked, 0, outputChunked.length );

        byte[] hashBytes = calcHash( outputChunked, this.key, this.receiveCounter );
        for ( int i = output.length - 8; i < output.length; i++ ) {
            if ( hashBytes[i - ( output.length - 8 )] != output[i] ) {
                return null;
            }
        }

        return outputChunked;
    }

    /**
     * Sets the server's public ECDH key which is required for decoding packets received from the proxied server and
     * encoding packets to be sent to the proxied server.
     *
     * @param key the key from the server
     */
    public void setServerPublicKey( PublicKey key ) {
        this.serverPublicKey = key;
    }

    /**
     * Sets up everything required for encrypting and decrypting networking data received from the proxied server.
     *
     * @param salt The salt to prepend in front of the ECDH derived shared secret before hashing it (sent to us from the
     *             proxied server in a 0x03 packet)
     */
    public boolean beginServersideEncryption( byte[] salt ) {
        if ( this.isEncryptionFromServerEnabled() ) {
            // Already initialized:
            return true;
        }

        // Generate shared secret from ECDH keys:
        byte[] secret = this.generateECDHSecret( this.keyFactory.getKeyPair().getPrivate(), this.serverPublicKey );
        if ( secret == null ) {
            return false;
        }

        // Derive key as salted SHA-256 hash digest:
        this.serverKey = this.hashSHA256( salt, secret );
        byte[] iv = this.takeBytesFromArray( this.serverKey, 0, 16 );

        // Initialize BlockCiphers:
        this.serverEncryptor = this.createCipher( true, this.serverKey, iv );
        this.serverDecryptor = this.createCipher( false, this.serverKey, iv );
        return true;
    }

    public boolean isEncryptionFromServerEnabled() {
        return ( this.serverEncryptor != null && this.serverDecryptor != null );
    }

    public byte[] decryptInputFromServer( byte[] input ) {
        byte[] output = this.processCipher( this.serverDecryptor, input );
        if ( output == null ) {
            return null;
        }

        byte[] outputChunked = new byte[input.length - 8];

        System.arraycopy( output, 0, outputChunked, 0, outputChunked.length );

        byte[] hashBytes = calcHash( outputChunked, this.serverKey, this.serverReceiveCounter );
        for ( int i = output.length - 8; i < output.length; i++ ) {
            if ( hashBytes[i - ( output.length - 8 )] != output[i] ) {
                return null;
            }
        }

        return outputChunked;
    }

    public byte[] encryptInputForServer( byte[] input ) {
        byte[] hashBytes = calcHash( input, this.serverKey, this.serverSendCounter );
        byte[] finalInput = new byte[8 + input.length];

        System.arraycopy( input, 0, finalInput, 0, input.length );
        System.arraycopy( hashBytes, 0, finalInput, input.length, 8 );

        return this.processCipher( this.serverEncryptor, finalInput );
    }

    /**
     * Encrypt data for the client
     *
     * @param input zlib compressed data
     * @return data ready to be sent directly to the client
     */
    public byte[] encryptInputForClient( byte[] input ) {
        byte[] hashBytes = calcHash( input, this.key, this.sendingCounter );
        byte[] finalInput = new byte[8 + input.length];

        System.arraycopy( input, 0, finalInput, 0, input.length );
        System.arraycopy( hashBytes, 0, finalInput, input.length, 8 );

        return this.processCipher( this.clientEncryptor, finalInput );
    }

    /**
     * Get the servers public key
     *
     * @return BASE64 encoded public key
     */
    public String getServerPublic() {
        return Base64.getEncoder().encodeToString( this.keyFactory.getKeyPair().getPublic().getEncoded() );
    }

    /**
     * Return the private key of the server. This should only be used to sign JWT content
     *
     * @return the private key
     */
    public Key getServerPrivate() {
        return this.keyFactory.getKeyPair().getPrivate();
    }

    private Hash getSHA256() {
        Hash digest = SHA256_DIGEST.get();
        if ( digest != null ) {
            digest.reset();
            return digest;
        }

        digest = HASHING.newInstance();
        SHA256_DIGEST.set( digest );
        return digest;
    }

    private byte[] calcHash( byte[] input, byte[] key, AtomicLong counter ) {
        Hash digest = getSHA256();
        if ( digest == null ) {
            return new byte[8];
        }

        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer( 8 + input.length + key.length );
        buf.writeLongLE( counter.getAndIncrement() );
        buf.writeBytes( input );
        buf.writeBytes( key );
        digest.update( buf );
        buf.release();
        return digest.digest();
    }

    private byte[] processCipher( Cipher cipher, byte[] input ) {
        byte[] output = new byte[cipher.getOutputSize( input.length )];

        try {
            int cursor = cipher.update( input, 0, input.length, output, 0 );
            // cursor += cipher.doFinal( output, cursor );
            if ( cursor != output.length ) {
                throw new ShortBufferException( "Output size did not match cursor" );
            }
        } catch ( ShortBufferException e ) {
            LOGGER.error( "Could not encrypt/decrypt to/from cipher-text", e );
            return null;
        }

        return output;
    }

    // ========================================== Utility Methods

    private byte[] generateECDHSecret( PrivateKey privateKey, PublicKey publicKey ) {
        try {
            KeyAgreement ka = KeyAgreement.getInstance( "ECDH" );
            ka.init( privateKey );
            ka.doPhase( publicKey, true );
            return ka.generateSecret();
        } catch ( NoSuchAlgorithmException | InvalidKeyException e ) {
            LOGGER.error( "Failed to generate Elliptic-Curve-Diffie-Hellman Shared Secret for clientside encryption", e );
            return null;
        }
    }

    private byte[] takeBytesFromArray( byte[] buffer, int offset, int length ) {
        byte[] result = new byte[length];
        System.arraycopy( buffer, offset, result, 0, length );
        return result;
    }

    private byte[] hashSHA256( byte[]... message ) {
        Hash digest = getSHA256();
        if ( digest == null ) {
            return null;
        }

        ByteBuf buf = PooledByteBufAllocator.DEFAULT.directBuffer();
        for ( byte[] bytes : message ) {
            buf.writeBytes( bytes );
        }

        digest.update( buf );
        buf.release();
        return digest.digest();
    }

    private Cipher createCipher( boolean encryptor, byte[] key, byte[] iv ) {
        SecretKey secretKey = new SecretKeySpec( key, "AES" );
        IvParameterSpec ivParameterSpec = new IvParameterSpec( iv );

        try {
            Cipher jdkCipher = Cipher.getInstance( "AES/CFB8/NoPadding" );
            jdkCipher.init( encryptor ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, secretKey, ivParameterSpec );
            return jdkCipher;
        } catch ( NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e ) {
            LOGGER.error( "Could not create cipher", e );
        }

        return null;
    }

}
