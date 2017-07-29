package julioarellano.wiiselandroid7.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import julioarellano.wiiselandroid7.R;
import julioarellano.wiiselandroid7.application.WiiselApplication;
import julioarellano.wiiselandroid7.constants.AppConstants;
import julioarellano.wiiselandroid7.manager.HttpUrlConnectionManager;
import julioarellano.wiiselandroid7.service.AccelerometerService;
import julioarellano.wiiselandroid7.service.BluetoothLeServiceLeft;
import julioarellano.wiiselandroid7.service.BluetoothLeServiceRight;
import julioarellano.wiiselandroid7.utils.CustomToast;

public class AcSettings extends Activity implements OnCheckedChangeListener, OnClickListener, OnSeekBarChangeListener, TextWatcher {

    private boolean wasChanged = false;

    private CheckBox cbUseDefault;
    private CheckBox cbStartAuto;
    private CheckBox cbAutoAuth;
    private CheckBox cbAutoConnect;
    private CheckBox cbFallDetect;
    private CheckBox cbShowLogger;

    private SeekBar sbAccelerometerSensitivity;
    private SeekBar sbPermitedAngle;
    private SeekBar sbNoMovementAcceleration;
    private SeekBar sbDelayTime;
    private SeekBar sbPressSensorsSensitivity;

    private EditText ipServer;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);

        ipServer = (EditText) findViewById(R.id.et_ip_server);
        ipServer.addTextChangedListener(this);

        cbUseDefault = (CheckBox) findViewById(R.id.cb_deff);
        cbStartAuto = (CheckBox) findViewById(R.id.cb_auto_start);
        cbAutoAuth = (CheckBox) findViewById(R.id.cb_auto_auth);
        cbAutoConnect = (CheckBox) findViewById(R.id.cb_auto_connect);
        cbFallDetect = (CheckBox) findViewById(R.id.cb_fall_detect);
        cbShowLogger = (CheckBox) findViewById(R.id.cb_show_logger);

        cbUseDefault.setOnCheckedChangeListener(this);
        cbUseDefault.setOnCheckedChangeListener(this);
        cbStartAuto.setOnCheckedChangeListener(this);
        cbAutoAuth.setOnCheckedChangeListener(this);
        cbAutoConnect.setOnCheckedChangeListener(this);
        cbFallDetect.setOnCheckedChangeListener(this);
        cbShowLogger.setOnCheckedChangeListener(this);

        Button btnUpdate = (Button) findViewById(R.id.btn_rl_refresh_update);
        Button btnSave = (Button) findViewById(R.id.btn_save_settings_screen);
        Button btnDetails = (Button) findViewById(R.id.btn_test_details_settings_screen);
        Button btnLogOut = (Button) findViewById(R.id.btn_log_out);
        ToggleButton btnClipMode = (ToggleButton) findViewById(R.id.tb_clipmode);
        Button btnCalibrationMode = (Button) findViewById(R.id.btn_calibration_settings_screen);

        btnClipMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton paramCompoundButton, boolean paramBoolean) {
                Editor editor = sharedPreferences.edit();

                if (paramBoolean) {
                    WiiselApplication.setMode(1);
                    editor.putInt(AppConstants.PREFERENCES_CLIMGENERALMODE, 1);

                } else {
                    WiiselApplication.setMode(0);
                    editor.putInt(AppConstants.PREFERENCES_CLIMGENERALMODE, 0);
                }
                editor.commit();
            }
        });

        btnUpdate.setOnClickListener(this);
        btnSave.setOnClickListener(this);
        btnDetails.setOnClickListener(this);
        btnLogOut.setOnClickListener(this);
        btnCalibrationMode.setOnClickListener(this);

        sbAccelerometerSensitivity = (SeekBar) findViewById(R.id.sb_accelerometer_sensitivity);
        sbPermitedAngle = (SeekBar) findViewById(R.id.sb_allowed_angle);
        sbNoMovementAcceleration = (SeekBar) findViewById(R.id.sb_no_movement_acceleration);
        sbDelayTime = (SeekBar) findViewById(R.id.sb_delay_time);
        sbPressSensorsSensitivity = (SeekBar) findViewById(R.id.sb_press_sensors_sensitivity);

        sbAccelerometerSensitivity.setOnSeekBarChangeListener(this);
        sbPermitedAngle.setOnSeekBarChangeListener(this);
        sbNoMovementAcceleration.setOnSeekBarChangeListener(this);
        sbDelayTime.setOnSeekBarChangeListener(this);
        sbPressSensorsSensitivity.setOnSeekBarChangeListener(this);

        ipServer.setText(sharedPreferences.getString(AppConstants.PREFERENCES_IPADDRESSSERVER, AppConstants.DEFAULT_IPADDRESS));

        if (WiiselApplication.getMode() == 1) {
            btnClipMode.setChecked(true);
        } else {
            btnClipMode.setChecked(false);
        }

        cbUseDefault.setChecked(sharedPreferences.getBoolean(AppConstants.PREFERENCES_USEDEFAULT, true));
        cbStartAuto.setChecked(sharedPreferences.getBoolean(AppConstants.PREFERENCES_AUTOSTART, AppConstants.DEFAULT_AUTOSTART));
        cbAutoAuth.setChecked(sharedPreferences.getBoolean(AppConstants.PREFERENCES_AUTOAUTH, AppConstants.DEFAULT_AUTOAUTHORIZATION));
        cbAutoConnect.setChecked(sharedPreferences.getBoolean(AppConstants.PREFERENCES_AUTOCONNECT, AppConstants.DEFAULT_AUTOCONNECT));
        cbFallDetect.setChecked(sharedPreferences.getBoolean(AppConstants.PREFERENCES_FALLDETECT, AppConstants.DEFAULT_FALLETECTION));
        cbShowLogger.setChecked(sharedPreferences.getBoolean(AppConstants.PREFERENCES_SHOW_LOGGER, AppConstants.DEFAULT_SHOW_LOGGER));

        sbAccelerometerSensitivity.setProgress(sharedPreferences.getInt(AppConstants.PREFERENCES_ACCELEROMETERSENSITIVITY,
                AppConstants.DEFAULT_ACCELEROMETERSENSITIVITY));
        sbPermitedAngle.setProgress(sharedPreferences.getInt(AppConstants.PREFERENCES_PERMITED_ANGLE, AppConstants.DEFAULT_PERMITED_ANGLE));
        sbNoMovementAcceleration.setProgress(sharedPreferences.getInt(AppConstants.PREFERENCES_NOMOVEMENTSENSITIVITY,
                AppConstants.DEFAULT_NOMOVEMENTSENSITIVITY));
        sbDelayTime.setProgress(sharedPreferences.getInt(AppConstants.PREFERENCES_DELAY_TIME, AppConstants.DEFAULT_DELAY_TIME));
        sbPressSensorsSensitivity.setProgress(sharedPreferences.getInt(AppConstants.PREFERENCES_PRESS_SENSITIVITY,
                AppConstants.DEFAULT_PRESS_SENSITIVITY));

        wasChanged = false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        wasChanged = true;
        switch (buttonView.getId()) {
            case R.id.cb_deff:
                cbStartAuto.setEnabled(!isChecked);
                cbAutoAuth.setEnabled(!isChecked);
                cbAutoConnect.setEnabled(!isChecked);
                cbFallDetect.setEnabled(!isChecked);
                cbShowLogger.setEnabled(!isChecked);

                sbAccelerometerSensitivity.setEnabled(!isChecked);
                sbPermitedAngle.setEnabled(!isChecked);
                sbNoMovementAcceleration.setEnabled(!isChecked);
                sbDelayTime.setEnabled(!isChecked);
                sbPressSensorsSensitivity.setEnabled(!isChecked);

                if (isChecked) {
                    setDefault();
                }
                break;
        }
    }

    private void setDefault() {
        Editor editor = sharedPreferences.edit();
        editor.putBoolean(AppConstants.PREFERENCES_AUTOSTART, AppConstants.DEFAULT_AUTOSTART);
        editor.putBoolean(AppConstants.PREFERENCES_AUTOCONNECT, AppConstants.DEFAULT_AUTOCONNECT);
        editor.putBoolean(AppConstants.PREFERENCES_AUTOAUTH, AppConstants.DEFAULT_AUTOAUTHORIZATION);
        editor.putBoolean(AppConstants.PREFERENCES_FALLDETECT, AppConstants.DEFAULT_FALLETECTION);
        editor.putBoolean(AppConstants.PREFERENCES_SHOW_LOGGER, AppConstants.DEFAULT_SHOW_LOGGER);

        editor.putInt(AppConstants.PREFERENCES_ACCELEROMETERSENSITIVITY, AppConstants.DEFAULT_ACCELEROMETERSENSITIVITY);
        editor.putInt(AppConstants.PREFERENCES_PERMITED_ANGLE, AppConstants.DEFAULT_PERMITED_ANGLE);
        editor.putInt(AppConstants.PREFERENCES_NOMOVEMENTSENSITIVITY, AppConstants.DEFAULT_NOMOVEMENTSENSITIVITY);

        editor.commit();

        cbStartAuto.setChecked(sharedPreferences.getBoolean(AppConstants.PREFERENCES_AUTOSTART, AppConstants.DEFAULT_AUTOSTART));
        cbAutoAuth.setChecked(sharedPreferences.getBoolean(AppConstants.PREFERENCES_AUTOAUTH, AppConstants.DEFAULT_AUTOAUTHORIZATION));
        cbAutoConnect.setChecked(sharedPreferences.getBoolean(AppConstants.PREFERENCES_AUTOCONNECT, AppConstants.DEFAULT_AUTOCONNECT));
        cbFallDetect.setChecked(sharedPreferences.getBoolean(AppConstants.PREFERENCES_FALLDETECT, AppConstants.DEFAULT_FALLETECTION));
        cbShowLogger.setChecked(sharedPreferences.getBoolean(AppConstants.PREFERENCES_SHOW_LOGGER, AppConstants.DEFAULT_SHOW_LOGGER));

        sbAccelerometerSensitivity.setProgress(sharedPreferences.getInt(AppConstants.PREFERENCES_ACCELEROMETERSENSITIVITY,
                AppConstants.DEFAULT_ACCELEROMETERSENSITIVITY));
        sbPermitedAngle.setProgress(sharedPreferences.getInt(AppConstants.PREFERENCES_PERMITED_ANGLE, AppConstants.DEFAULT_PERMITED_ANGLE));
        sbNoMovementAcceleration.setProgress(sharedPreferences.getInt(AppConstants.PREFERENCES_NOMOVEMENTSENSITIVITY,
                AppConstants.DEFAULT_NOMOVEMENTSENSITIVITY));
        sbDelayTime.setProgress(sharedPreferences.getInt(AppConstants.PREFERENCES_DELAY_TIME, AppConstants.DEFAULT_DELAY_TIME));
        sbPressSensorsSensitivity.setProgress(sharedPreferences.getInt(AppConstants.PREFERENCES_PRESS_SENSITIVITY,
                AppConstants.DEFAULT_PRESS_SENSITIVITY));
    }

    private void storeSettings() {

        if (wasChanged) {
            Editor editor = sharedPreferences.edit();
            editor.putBoolean(AppConstants.PREFERENCES_AUTOSTART, cbStartAuto.isChecked());
            editor.putBoolean(AppConstants.PREFERENCES_AUTOCONNECT, cbAutoConnect.isChecked());
            editor.putBoolean(AppConstants.PREFERENCES_AUTOAUTH, cbAutoAuth.isChecked());
            editor.putBoolean(AppConstants.PREFERENCES_FALLDETECT, cbFallDetect.isChecked());
            editor.putBoolean(AppConstants.PREFERENCES_SHOW_LOGGER, cbShowLogger.isChecked());
            editor.putBoolean(AppConstants.PREFERENCES_USEDEFAULT, cbUseDefault.isChecked());
            editor.putInt(AppConstants.PREFERENCES_ACCELEROMETERSENSITIVITY, sbAccelerometerSensitivity.getProgress());
            editor.putInt(AppConstants.PREFERENCES_PERMITED_ANGLE, sbPermitedAngle.getProgress());
            editor.putInt(AppConstants.PREFERENCES_NOMOVEMENTSENSITIVITY, sbNoMovementAcceleration.getProgress());
            editor.putInt(AppConstants.PREFERENCES_DELAY_TIME, sbDelayTime.getProgress());
            editor.putInt(AppConstants.PREFERENCES_PRESS_SENSITIVITY, sbPressSensorsSensitivity.getProgress());
            editor.putString(AppConstants.PREFERENCES_IPADDRESSSERVER, ipServer.getText().toString());
            editor.commit();

            CustomToast.makeText(AcSettings.this, getString(R.string.mess_datawassaved), Toast.LENGTH_SHORT).show();
            wasChanged = false;

            if (BluetoothLeServiceRight.getBtGatt() != null || BluetoothLeServiceLeft.getBtGatt() != null) {
                CustomToast.makeText(AcSettings.this, getString(R.string.restart_app), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_rl_refresh_update:
                // update user information from server
                Thread thread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        //ConnectionManager manager = ConnectionManager.getInstance(getApplicationContext());
                        HttpUrlConnectionManager httpURLConnectionManager = HttpUrlConnectionManager.getInstance(getApplicationContext());

                        try {
                            //HttpResponse updateUserData = manager.updateUserData();
                            String updateMessageAttempt = httpURLConnectionManager.updateUserData();
                            //int statusCode = updateUserData.getStatusLine().getStatusCode();
                            if (updateMessageAttempt != "Update successful") {
                                //JSONObject fromHttpToJson = manager.fromHttpToJson(updateUserData);
                                //String msg = fromHttpToJson.getString(AppConstants.PARAM_MESSAGE);
                                Toast.makeText(AcSettings.this, updateMessageAttempt, Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                thread.start();
                break;
            case R.id.btn_save_settings_screen:
                storeSettings();
                break;
            case R.id.btn_test_details_settings_screen:
                Intent intent = new Intent(AcSettings.this, AcDetails.class);
                startActivity(intent);
                break;
            case R.id.btn_log_out:
                Intent intent2 = new Intent(AcSettings.this, AccelerometerService.class);
                stopService(intent2);

                // delete token and log out from the application
                sharedPreferences.edit().remove(AppConstants.PREFERENCES_TOKEN).commit();

                setResult(AppConstants.REQUEST_SETTINGSONACTIVITYRESULT);
                finish();
                break;
            case R.id.btn_calibration_settings_screen:
                WiiselApplication.isCalibrationMode = true;
                finish();
                break;
        }

    }

    @Override
    public void onBackPressed() {
        storeSettings();
        super.onBackPressed();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        wasChanged = true;
        switch (seekBar.getId()) {
            case R.id.sb_accelerometer_sensitivity:
                ((TextView) findViewById(R.id.tv_accelerometer_sensitivity_label)).setText(progress + "");
                break;
            case R.id.sb_allowed_angle:
                ((TextView) findViewById(R.id.tv_allowed_angle_label)).setText(progress + "");
                break;
            case R.id.sb_no_movement_acceleration:
                ((TextView) findViewById(R.id.tv_no_movement_acceleration_label)).setText(progress + "");
                break;
            case R.id.sb_delay_time:
                ((TextView) findViewById(R.id.tv_delay_time_label)).setText(progress + "");
                break;
            case R.id.sb_press_sensors_sensitivity:
                ((TextView) findViewById(R.id.tv_press_sensors_sensitivity_label)).setText(progress + "");
                break;
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        wasChanged = true;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void afterTextChanged(Editable arg0) {
    }

    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
    }

}
