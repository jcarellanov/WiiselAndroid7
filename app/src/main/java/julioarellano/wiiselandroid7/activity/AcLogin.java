package julioarellano.wiiselandroid7.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import julioarellano.wiiselandroid7.R;
import julioarellano.wiiselandroid7.constants.AppConstants;
import julioarellano.wiiselandroid7.manager.ConnectionManager;
import julioarellano.wiiselandroid7.manager.HttpUrlConnectionManager;
import julioarellano.wiiselandroid7.service.BluetoothLeServiceLeft;
import julioarellano.wiiselandroid7.service.BluetoothLeServiceRight;
import julioarellano.wiiselandroid7.utils.CustomToast;


/**
 * login screen.
 */
public class AcLogin extends Activity {

    private EditText email;
    private EditText password;
    Handler handler = new Handler();
    int MY_PERMISSIONS_ACCESS_FINE = 300;
    int MY_PERMISSIONS_REQUEST_CALL_PHONE = 301;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        receiveDataOpen();
        setContentView(R.layout.ac_login);

        ImageView fingerImage = (ImageView) findViewById(R.id.FingerPrintImage);

        if (Build.VERSION.SDK_INT > 22) { //API 23 and higher requires this in order to check BLE scan results
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_ACCESS_FINE);
            enableLocationService();

            //required to call
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    MY_PERMISSIONS_REQUEST_CALL_PHONE);

        }


        email = (EditText) findViewById(R.id.et_email_input);
        password = (EditText) findViewById(R.id.et_password_input);

//text credentials login

        findViewById(R.id.btn_sign_on_login).setOnClickListener(new OnClickListener() {

            ProgressDialog progressDialog = new ProgressDialog(AcLogin.this);

            @Override
            public void onClick(View v) {

                final String emailText = email.getText().toString();
                final String passText = password.getText().toString();

                if (TextUtils.isEmpty(emailText) || TextUtils.isEmpty(passText)) {
                    CustomToast.makeText(AcLogin.this, R.string.mess_fill_fields, Toast.LENGTH_SHORT).show();
                } else {

                    progressDialog.setMessage(getString(R.string.mess_wait));
                    progressDialog.show();


                    Thread thread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            ConnectionManager connectionManager = ConnectionManager
                                    .getInstance(getApplicationContext());
                            HttpUrlConnectionManager httpURLConnectionManager = HttpUrlConnectionManager.getInstance(getApplicationContext());
                            try {
                                //HttpResponse serverLoginResponse = connectionManager.serverLogin(emailText, passText);
                                final String msg = httpURLConnectionManager.serverLogin(emailText, passText);


                                handler.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        progressDialog.dismiss();
                                        CustomToast.makeText(AcLogin.this, msg, Toast.LENGTH_SHORT).show();
                                    }
                                });
                                if (msg.equals("Invalid email or password") || msg.equals("Check internet connection")) {

                                    return;
                                }

                            } catch (Exception e) {

                                handler.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        progressDialog.dismiss();
                                        removeToken();
                                        CustomToast.makeText(AcLogin.this, getString(R.string.mess_checkinternet),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });

                                return;
                            }

                            try {
                                //HttpResponse updateUserData = connectionManager.updateUserData();
                                final String updateMessage = httpURLConnectionManager.updateUserData();
                                //int statusCode = updateUserData.getStatusLine().getStatusCode();
                                if (!updateMessage.equals("Update successful")) {

                                    handler.post(new Runnable() {

                                        @Override
                                        public void run() {
                                            progressDialog.dismiss();
                                            removeToken();
                                            CustomToast.makeText(AcLogin.this, updateMessage, Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                    return;
                                }

                            } catch (Exception e) {
                                handler.post(new Runnable() {

                                    @Override
                                    public void run() {
                                        progressDialog.dismiss();
                                        removeToken();
                                        CustomToast.makeText(AcLogin.this, getString(R.string.mess_checkinternet),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });

                                return;
                            }

                            handler.post(new Runnable() {

                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    finish();
                                    Intent intent = new Intent(AcLogin.this, AcMainScreen.class);
                                    startActivity(intent);

                                }
                            });
                        }
                    });
                    thread.start();
                }
            }
        });


        //Fingerprint Login

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            final SharedPreferences sharedPreferences = this.getSharedPreferences(this.getResources().getString(R.string.app_name),
                    Context.MODE_PRIVATE);

            final boolean authenticated = sharedPreferences.getBoolean("authenticated", false);

            if(authenticated) {
                fingerImage.setVisibility(View.VISIBLE);
                fingerImage.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(AcLogin.this, FingerLogin.class);
                        startActivity(intent);
                    }
                });
            }

        }

    }

    private void removeToken() {
        SharedPreferences sharedPreferences = getSharedPreferences(getResources().getString(R.string.app_name),
                Context.MODE_PRIVATE);
        sharedPreferences.edit().remove(AppConstants.PREFERENCES_TOKEN).apply();
    }

    private void receiveDataOpen() {
        // Update users data
        final ProgressDialog waitDialog = new ProgressDialog(AcLogin.this);
        waitDialog.setMessage(getString(R.string.mess_wait));
        waitDialog.show();
        new Thread(new Runnable() {

            @Override
            public void run() {
                //ConnectionManager.getInstance(getApplicationContext()).updateUserData();
                try {
                    HttpUrlConnectionManager.getInstance(getApplicationContext()).updateUserData();
                } catch (java.lang.Exception e) {
                    e.printStackTrace();
                }
                waitDialog.dismiss();

                // Load everything after done
                if (BluetoothLeServiceRight.getBtGatt() != null || BluetoothLeServiceLeft.getBtGatt() != null) {
                    Intent intent = new Intent(AcLogin.this, AcMainScreen.class);
                    startActivity(intent);
                } else {
                    SharedPreferences sharedPreferences = getSharedPreferences(
                            getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
                    boolean chAutoAuth = sharedPreferences.getBoolean(AppConstants.PREFERENCES_AUTOAUTH,
                            AppConstants.DEFAULT_AUTOAUTHORIZATION);
                    if (chAutoAuth) {
                        boolean containsToken = sharedPreferences.contains(AppConstants.PREFERENCES_TOKEN);
                        if (containsToken) {
                            finish();
                            Intent intent = new Intent(AcLogin.this, AcMainScreen.class);
                            startActivity(intent);
                        }
                    }
                }
            }
        }).start();
    }

    private boolean enableLocationService() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean providerEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) |
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!providerEnabled) {
            Resources res = getResources();
            // notify user
            final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage(res.getString(R.string.EnablePositionService));
            dialog.setPositiveButton(res.getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                            paramDialogInterface.cancel();
                        }
                    });
            dialog.setNegativeButton(res.getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            paramDialogInterface.cancel();
                            Toast.makeText(AcLogin.this,
                                    R.string.LocationNotEnabled,
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }//onClick
                    });
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dialog.show();
                }
            });

        }//if
        return providerEnabled;
    }//enableLocationService

}
