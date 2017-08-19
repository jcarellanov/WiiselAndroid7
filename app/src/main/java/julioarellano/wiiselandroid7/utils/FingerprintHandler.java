package julioarellano.wiiselandroid7.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import julioarellano.wiiselandroid7.R;
import julioarellano.wiiselandroid7.activity.AcMainScreen;
import julioarellano.wiiselandroid7.manager.HttpUrlConnectionManager;

import static julioarellano.wiiselandroid7.constants.AppConstants.PREFERENCES_EMAIL;
import static julioarellano.wiiselandroid7.constants.AppConstants.PREFERENCES_PASSWORD;


@TargetApi(Build.VERSION_CODES.M)
public class FingerprintHandler extends FingerprintManager.AuthenticationCallback {


    private CancellationSignal cancellationSignal;
    private Context context;
    public String authMssg = "";

    public FingerprintHandler(Context mContext) {
        context = mContext;
    }

    //Implement the startAuth method, which is responsible for starting the fingerprint authentication process//

    public String getMessage() {
        return authMssg;
    }

    public void startAuth(FingerprintManager manager, FingerprintManager.CryptoObject cryptoObject) {

        cancellationSignal = new CancellationSignal();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.authenticate(cryptoObject, cancellationSignal, 0, this, null);
    }

    @Override
    //onAuthenticationError is called when a fatal error has occurred. It provides the error code and error message as its parameters//

    public void onAuthenticationError(int errMsgId, CharSequence errString) {



        authMssg = "Authentication error\n" + errString;
    }

    @Override

    //onAuthenticationFailed is called when the fingerprint doesn’t match with any of the fingerprints registered on the device//

    public void onAuthenticationFailed() {

        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                CustomToast.makeText(context, "Fingerprint authentication failed", Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override

    //onAuthenticationHelp is called when a non-fatal error has occurred. This method provides additional information about the error,
    //so to provide the user with as much feedback as possible I’m incorporating this information into my toast//
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {

        authMssg = "Authentication help\n" + helpString;
    }

    @Override

    //onAuthenticationSucceeded is called when a fingerprint has been successfully matched to one of the fingerprints stored on the user’s device//
    public void onAuthenticationSucceeded(
            FingerprintManager.AuthenticationResult result) {
        final Handler handler = new Handler();
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                HttpUrlConnectionManager httpUrlConnectionManager = HttpUrlConnectionManager.getInstance(context);
                final SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.app_name),
                        Context.MODE_PRIVATE);
try{
                String email = AESCrypt.decrypt(sharedPreferences.getString(PREFERENCES_EMAIL, ""));
                String password = AESCrypt.decrypt(sharedPreferences.getString(PREFERENCES_PASSWORD, ""));


                    String msg = httpUrlConnectionManager.serverLogin(email, password);
                    if (!msg.equals("Sign in successful")) {

                        return;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        context.startActivity(new Intent(context, AcMainScreen.class));

                    }
                });
            }
        });
        thread.start();


    }

}
