package julioarellano.wiiselandroid7.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.json.JSONObject;

import julioarellano.wiiselandroid7.R;
import julioarellano.wiiselandroid7.constants.AppConstants;
import julioarellano.wiiselandroid7.manager.ConnectionManager;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        receiveDataOpen();
        setContentView(R.layout.ac_login);
        email = (EditText) findViewById(R.id.et_email_input);
        password = (EditText) findViewById(R.id.et_password_input);

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

                            try {
                                HttpResponse serverLoginResponse = connectionManager.serverLogin(emailText, passText);
                                int statusCode = serverLoginResponse.getStatusLine().getStatusCode();
                                if (statusCode == HttpStatus.SC_UNAUTHORIZED) {

                                    //JSONObject fromHttpToJson = connectionManager.fromHttpToJson(serverLoginResponse);
                                    //final String msg = fromHttpToJson.getString(AppConstants.PARAM_MESSAGE);

                                    handler.post(new Runnable() {

                                        @Override
                                        public void run() {
                                            progressDialog.dismiss();
                                            CustomToast.makeText(AcLogin.this, "Invalid email or password", Toast.LENGTH_LONG).show();
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

                            try {
                                HttpResponse updateUserData = connectionManager.updateUserData();
                                int statusCode = updateUserData.getStatusLine().getStatusCode();
                                if (statusCode != HttpStatus.SC_OK) {
                                    JSONObject fromHttpToJson = connectionManager.fromHttpToJson(updateUserData);

                                    final String msg = fromHttpToJson.getString(AppConstants.PARAM_MESSAGE);
                                    handler.post(new Runnable() {

                                        @Override
                                        public void run() {
                                            progressDialog.dismiss();
                                            removeToken();
                                            CustomToast.makeText(AcLogin.this, msg, Toast.LENGTH_SHORT).show();
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

        findViewById(R.id.buttonDemo).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AcLogin.this, AcMainScreen.class);
                startActivity(intent);
            }
        });
    }

    private void removeToken() {
        SharedPreferences sharedPreferences = getSharedPreferences(getResources().getString(R.string.app_name),
                Context.MODE_PRIVATE);
        sharedPreferences.edit().remove(AppConstants.PREFERENCES_TOKEN).commit();
    }

    private void receiveDataOpen() {
        // Update users data
        final ProgressDialog waitDialog = new ProgressDialog(AcLogin.this);
        waitDialog.setMessage(getString(R.string.mess_wait));
        waitDialog.show();
        new Thread(new Runnable() {

            @Override
            public void run() {
                ConnectionManager.getInstance(getApplicationContext()).updateUserData();
                
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

}
