package julioarellano.wiiselandroid7.manager;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

import julioarellano.wiiselandroid7.utils.FingerprintHandler;


public class AuthManager {

    private Context ctx;
    private static AuthManager manager;

    private Cipher cipher;
    private KeyStore keyStore;
    private static final String KEY_NAME = "yourKey";
    private KeyGenerator keyGenerator;
    private FingerprintManager.CryptoObject cryptoObject;



    private AuthManager(Context context) {
        ctx = context;
    }

    public static AuthManager getInstance(Context context) {
        if (manager == null) {
            manager = new AuthManager(context);
        }
        return manager;

    }


    @TargetApi(23)
    private void generateKey() {
        try {
            // Obtain a reference to the Keystore using the standard Android keystore container identifier (“AndroidKeystore”)//
            keyStore = KeyStore.getInstance("AndroidKeyStore");

            //Generate the key//
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");

            //Initialize an empty KeyStore//
            keyStore.load(null);

            //Initialize the KeyGenerator//
            keyGenerator.init(new

                    //Specify the operation(s) this key can be used for//
                    KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)

                    //Configure this key so that the user has to confirm their identity with a fingerprint each time they want to use it//
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());

            //Generate the key//
            keyGenerator.generateKey();

        } catch (KeyStoreException
                | NoSuchAlgorithmException
                | NoSuchProviderException
                | InvalidAlgorithmParameterException
                | CertificateException
                | IOException exc) {
            exc.printStackTrace();

        }
    }

    @TargetApi(23)
    public boolean initCipher() {
        try {
            //Obtain a cipher instance and configure it with the properties required for fingerprint authentication//
            cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException |
                NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }

        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            //Return true if the cipher has been initialized successfully//
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {

            //Return false if cipher initialization failed//
            return false;
        } catch (KeyStoreException | CertificateException
                | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

    @TargetApi(23)
    public String authFingerprint(FingerprintManager fingerprintManager) throws Exception {

        String message = "";

        /*KeyguardManager keyguardManager =
                (KeyguardManager) ctx.getSystemService(KEYGUARD_SERVICE);
        FingerprintManager fingerprintManager =
                (FingerprintManager) ctx.getSystemService(FINGERPRINT_SERVICE);*/

      /*  if (!keyguardManager.isKeyguardSecure()) {

            message = "Keyboard not secure";
            return message;
        }

        ActivityCompat.checkSelfPermission(ctx, Manifest.permission.USE_FINGERPRINT);
        if (!fingerprintManager.hasEnrolledFingerprints()) {
            message = "No saved fingerprints";
            return message;

        }*/

        /*if(keyguardManager.isKeyguardSecure()) {
            try {
                generateKey();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (initCipher()) {
                //If the cipher is initialized successfully, then create a CryptoObject instance//
                cryptoObject = new FingerprintManager.CryptoObject(cipher);


                FingerprintHandler helper = new FingerprintHandler(ctx);
                helper.startAuth(fingerprintManager, cryptoObject);
                message = helper.getMessage();
            }

        }*/


            try {
                generateKey();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (initCipher()) {
                //If the cipher is initialized successfully, then create a CryptoObject instance//
                cryptoObject = new FingerprintManager.CryptoObject(cipher);


                FingerprintHandler helper = new FingerprintHandler(ctx);
                helper.startAuth(fingerprintManager, cryptoObject);
                message = helper.getMessage();
            }




        return message;

    }
}
