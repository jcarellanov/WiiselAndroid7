package julioarellano.wiiselandroid7.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import julioarellano.wiiselandroid7.R;
import julioarellano.wiiselandroid7.activity.interfaces.IRefreshable;
import julioarellano.wiiselandroid7.application.WiiselApplication;
import julioarellano.wiiselandroid7.constants.AppConstants;
import julioarellano.wiiselandroid7.interfaces.InsolesType;
import julioarellano.wiiselandroid7.interfaces.OnDataReseivedListener;
import julioarellano.wiiselandroid7.manager.PhoneStateManager;
import julioarellano.wiiselandroid7.receivers.AlarmReceiver;
import julioarellano.wiiselandroid7.service.AccelerometerService;
import julioarellano.wiiselandroid7.service.BluetoothLeServiceLeft;
import julioarellano.wiiselandroid7.service.BluetoothLeServiceRight;
import julioarellano.wiiselandroid7.service.ConnectionService;
import julioarellano.wiiselandroid7.utils.CustomToast;
import julioarellano.wiiselandroid7.utils.InsolesUtil;
import julioarellano.wiiselandroid7.utils.UIUtil;

//Acreo_johros: added

/**
 * Control screen in the application
 */

public class AcMainScreen extends Activity implements IRefreshable {

    private static final int INSOLE_SYNC_TIME = 39;
    Handler handler = new Handler();
    private Handler mHandler = handler;

    public boolean retrySendData = true;
    private TextView tv_right_isole;
    private TextView tv_left_isole;

    // Insoles statistics
    private ImageView right_isole_signal;
    private ImageView left_isole_signal;
    private ImageView right_battery_level;
    private ImageView left_battery_level;

    private ImageView right_isole_isconnect;
    private ImageView left_isole_isconnect;

    private TextView tvFirstInsolePackets;
    private TextView tvSecondInsolePackets;
    // private TextView tvTimer;
    private ImageView imageAmpel;

    final int TIME_BEFORE_CLOSING = 1000;
    private Timer timer = null;
    private Timer timer2 = null;

    private boolean readDataFromFirstRightInsole = true;

    // Housekeeping
    private boolean mInitialised = false;

    // BLE management
    private boolean mBleSupported = true;
    private boolean mScanning = false;
    // private int mNumDevs = 0;
    private List<BleDeviceInfo> mDeviceInfoList;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBtAdapter = null;
    private BluetoothDevice mBluetoothDeviceRight = null;
    private BluetoothDevice mBluetoothDeviceLeft = null;
    private BluetoothLeServiceRight mBluetoothLeServiceRight = null;
    private BluetoothLeServiceLeft mBluetoothLeServiceLeft = null;
    BluetoothGattService mDataServiceRight = null;
    BluetoothGattService mDataServiceLeft = null;
    BluetoothGattService mBatteryServiceLeft = null;
    BluetoothGattService mBatteryServiceRight = null;
    Button connection_toggle;

    // private boolean refreshGUI = false;

    private IntentFilter mFilter;
    private String[] mDeviceFilter = null;
    // private BluetoothLeBatteryServiceLeft bluetoothLeBatteryServiceLeft =
    // null;

    private Random mRandom = new Random(); // Acreo_johros: added
    private int mSynchCounterLeft = 0; // Acreo_johros: added
    private int mSynchCounterRight = 0; // Acreo_johros: added
    private final int CONNECTION_ATTEMPS_MAX = 50;

    private List<BluetoothGattService> mServiceListRight = null;
    private List<BluetoothGattService> mServiceListLeft = null;

    // //////////////// Calibration
    public OnDataReseivedListener dataReseivedListener;

    private final int CALIBRATION_TIME = 10000;
    private final int AUTOCONNECTION_TIME = 5000;

    private final int RSSI_INTERVAL = 1000;
    private final int BATTERY_CHECK_INTERVAL = 30000; // check battery every 30s

    private long leftTimestamp, leftTimediff;
    private long rightTimestamp, rightTimediff;

    // private boolean isAutoConnectionOn = false;
    private boolean isDisconectedManually = true;

    private boolean mConnectionEstablished = false;
    private boolean mPrevConnectionEstablished = false;

    private AlertDialog mCurrentDialog = null;
    private int mCalibrationMode = 0;
    private boolean mCalibrationWaitingForConnect = false;

    // private boolean isConnectedManually = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bluetoothTurnOn();
        setContentView(R.layout.ac_main);
        createView();
        registerReceiver();

        ///
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(noDataReceiver, intentFilter);
        ///

        restoreViewStatus();
        mServiceListRight = new ArrayList<BluetoothGattService>();
        mServiceListLeft = new ArrayList<BluetoothGattService>();
        connection_toggle = (Button) findViewById(R.id.toggleButton);
        connection_toggle.setText("OFF");

        SharedPreferences sharedPreferences =
                getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
        boolean isAutoConnectionOn =
                sharedPreferences.getBoolean(AppConstants.PREFERENCES_AUTOCONNECT, AppConstants.DEFAULT_AUTOCONNECT);
        if (isAutoConnectionOn) {
            appendLoggerMessage("Start auto connect. Buetooth status: "
                    + BluetoothAdapter.getDefaultAdapter().isEnabled());
            handler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    connectToInsolesActomaticallyFirstTime();
                }
            }, AUTOCONNECTION_TIME);
        }

        PhoneStateManager.getInstance().subscribe(this);
    }

    /**
     * @param insole      0 - left; 1 - right
     * @param isConnected
     */
    private void isEnabledIndicators(int insole, boolean isConnected) {
        if (right_isole_signal != null) {
            right_isole_signal.setVisibility((isConnected) ? View.VISIBLE : View.INVISIBLE);
        }
        if (left_isole_signal != null) {
            left_isole_signal.setVisibility((isConnected) ? View.VISIBLE : View.INVISIBLE);
        }
        if (right_battery_level != null) {
            right_battery_level.setVisibility((isConnected) ? View.VISIBLE : View.INVISIBLE);
        }
        if (left_battery_level != null) {
            left_battery_level.setVisibility((isConnected) ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override
    protected void onResume() {
        updateUIAfterRefreshing();

        if (WiiselApplication.isCalibrationMode) {
            WiiselApplication.isCalibrationMode = false;

            calibrateInsoles();
        }

        super.onResume();
    }

    private synchronized void showAlert(AlertDialog dialog) {
        if (mCurrentDialog != null) {
            mCurrentDialog.dismiss();
        }

        mCurrentDialog = dialog;
        mCurrentDialog.show();
    }

    private void calibrateInsoles() {
        appendLoggerMessage(getString(R.string.calibration_mode_will_be_started));

        disconnectDevice();
        Toast.makeText(this, getString(R.string.calibration_mode_will_be_started), Toast.LENGTH_SHORT).show();
        mHandler.postDelayed(new Runnable() {

            // Read data = false

            boolean firstInsolesConnected = false;
            boolean secondInsolesConnected = false;

            @Override
            public void run() {

                InsolesUtil.setWriteDataToFilePermission(false);

                RefreshConnectionHelper.execute(AcMainScreen.this, 10);
                startDevicePicker();

                appendLoggerMessage("Calibration scan started");

                dataReseivedListener = new OnDataReseivedListener() {

                    @Override
                    public void onReceived(InsolesType insolesType) {
                        if (insolesType == InsolesType.FIRST) {
                            appendLoggerMessage("First insole connected");
                            firstInsolesConnected = true;
                            ifAllInsolesConnected();
                        }
                        if (insolesType == InsolesType.SECOND) {
                            appendLoggerMessage("Second insole connected");
                            secondInsolesConnected = true;
                            ifAllInsolesConnected();
                        }

                    }

                    private void ifAllInsolesConnected() {
                        if (firstInsolesConnected && secondInsolesConnected) {
                            appendLoggerMessage("Both insoles connected");
                            dataReseivedListener = null;
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    setCalibrationMode(1);
                                }
                            });
                        }
                    }
                };
            }
        }, 10000);
    }

    private void resumeCalibrationIfNeeded() {
        if (mCalibrationMode > 0) {
            mCalibrationWaitingForConnect = false;
            setCalibrationMode(mCalibrationMode);
        }
    }

    /**
     * Invoked in OnResume and update all UI components
     */
    private void updateUIAfterRefreshing() {
        // show user status from preferences
        imageAmpel = (ImageView) findViewById(R.id.iv_triangle_action_mode);
        tvFirstInsolePackets = (TextView) findViewById(R.id.setting_statistic_first_insole);
        tvSecondInsolePackets = (TextView) findViewById(R.id.setting_statistic_second_insole);
        // tvTimer = (TextView) findViewById(R.id.setting_timer);
        TextView tvMode = (TextView) findViewById(R.id.tv_mode_mainscr);

        TextView tvActTime = (TextView) findViewById(R.id.tv_active_time);
        TextView tvNumOfSt = (TextView) findViewById(R.id.tv_num_of_st);
        TextView tvWalkDist = (TextView) findViewById(R.id.tv_walk_dist);

        SharedPreferences sharedPreferences =
                getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
        Double ampel = Double.valueOf(sharedPreferences.getString(AppConstants.PREFERENCES_AMPEL, "1"));
        boolean showAmpel = sharedPreferences.getBoolean(AppConstants.PREFERENCES_SHOWAMPELSTATUS, false);
        String acttime = sharedPreferences.getString(AppConstants.PREFERENCES_ACTTIME, "-");
        String numofst = sharedPreferences.getString(AppConstants.PREFERENCES_NUMOFST, "-");
        String walkdist = sharedPreferences.getString(AppConstants.PREFERENCES_WALKDIST, "-");

        if (ampel < 3.3) {
            imageAmpel.setImageResource(R.drawable.green);
        } else {
            if (ampel < 6.6) {
                imageAmpel.setImageResource(R.drawable.yellow);
            } else {
                imageAmpel.setImageResource(R.drawable.red);
            }
        }

        if (showAmpel) {
            imageAmpel.setVisibility(View.VISIBLE);
        } else {
            imageAmpel.setVisibility(View.GONE);
        }
        if (WiiselApplication.getMode() == 0) {
            tvMode.setText(getResources().getString(R.string.general_mode));
        } else {
            tvMode.setText(getResources().getString(R.string.clip_mode));
        }

        tvActTime.setText(acttime);
        tvNumOfSt.setText(numofst);
        tvWalkDist.setText(walkdist);

        boolean showLogger =
                sharedPreferences.getBoolean(AppConstants.PREFERENCES_SHOW_LOGGER, AppConstants.DEFAULT_SHOW_LOGGER);
        if (showLogger) {
            findViewById(R.id.loggerscrollview).setVisibility(View.VISIBLE);
            findViewById(R.id.tv_active_time_container).setVisibility(View.GONE);
            findViewById(R.id.tv_walk_dist_container).setVisibility(View.GONE);
            findViewById(R.id.tv_num_of_st_container).setVisibility(View.GONE);
        } else {
            findViewById(R.id.loggerscrollview).setVisibility(View.GONE);
            findViewById(R.id.tv_active_time_container).setVisibility(View.VISIBLE);
            findViewById(R.id.tv_walk_dist_container).setVisibility(View.VISIBLE);
            findViewById(R.id.tv_num_of_st_container).setVisibility(View.VISIBLE);
        }
    }

    /**
     * @param mode - Calibration mode stage, by default it = 1
     */
    private void setCalibrationMode(final int mode) {
        appendLoggerMessage("setCalibrationMode: " + String.valueOf(mode));

        mCalibrationMode = mode;

        final Activity context = AcMainScreen.this;
        if (mode == 7) {
            Toast.makeText(context, getResources().getString(R.string.dialod_calibration_success), Toast.LENGTH_SHORT)
                    .show();

            disconnectDevice();

            stopCalibration();

            return;
        }

        if (isConnectionEstablished()) {

            final int[] DIALOG_MESSAGE_IDS =
                    {0, R.string.dialog_message_1, R.string.dialog_message_2, R.string.dialog_message_3,
                            R.string.dialog_message_4, R.string.dialog_message_5, R.string.dialog_message_6};

            int dialogMessageResId = DIALOG_MESSAGE_IDS[mode];
            InsolesUtil.setCalibrationMode(mode);

            showAlert(new AlertDialog.Builder(context)
                    .setTitle(getResources().getString(R.string.dialod_title))
                    .setMessage(dialogMessageResId)
                    .setPositiveButton(R.string.start, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            InsolesUtil.clearBuffer();
                            InsolesUtil.setWriteDataToFilePermission(true);

                            handler.postDelayed(new Runnable() {

                                @Override
                                public void run() {
                                    InsolesUtil.setWriteDataToFilePermission(false);

                                    if (isConnectionEstablished()) {
                                        setCalibrationMode(mode + 1);
                                    }
                                }
                            }, CALIBRATION_TIME);
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            appendLoggerMessage("Calibration has been canceled");
                            stopCalibration();
                        }
                    })
                    .create());
        } else {
            showCalibrationStoppedDialog(context);
        }
    }

    private void showCalibrationStoppedDialog(final Activity context) {
        // showAlert(new AlertDialog.Builder(context)
        // .setTitle("Calibration stopped")
        // .setMessage("Connection to insoled has been failed")
        // .setPositiveButton("Wait", new DialogInterface.OnClickListener() {
        //
        // @Override
        // public void onClick(DialogInterface dialog, int which) {
        //
        // }
        // })
        // .setNegativeButton("Abort", new DialogInterface.OnClickListener() {
        //
        // @Override
        // public void onClick(DialogInterface dialog, int which) {
        // stopCalibration();
        // }
        // })
        // .create());
    }

    private void stopCalibration() {
        RefreshConnectionHelper.stop();
        mCalibrationWaitingForConnect = false;
        mCalibrationMode = 0;
        InsolesUtil.setCalibrationMode(0);
        InsolesUtil.setWriteDataToFilePermission(true);
    }

    /**
     * Creates some view and sets click listeners
     */
    private void createView() {

        right_isole_isconnect = (ImageView) findViewById(R.id.iv_dialog_right_isole_isconnect);
        left_isole_isconnect = (ImageView) findViewById(R.id.iv_dialog_left_isole_isconnect);

        tv_right_isole = (TextView) findViewById(R.id.tv_right_isole);
        tv_left_isole = (TextView) findViewById(R.id.tv_left_isole);

        right_isole_signal = (ImageView) findViewById(R.id.iv_right_isole_signal);
        left_isole_signal = (ImageView) findViewById(R.id.iv_left_isole_signal);

        right_battery_level = (ImageView) findViewById(R.id.iv_right_battery);
        left_battery_level = (ImageView) findViewById(R.id.iv_left_battery);

        findViewById(R.id.toggleButton).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                createDialog(InsolesUtil.isBothInsolesConected());
            }
        });

        findViewById(R.id.btn_rl_settings_image_main).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AcMainScreen.this, AcSettings.class);
                startActivityForResult(intent, AppConstants.REQUEST_SETTINGSONACTIVITYRESULT);
            }
        });

    }

    /**
     * This method updates the insoles status
     */
    private void restoreViewStatus() {
        setConnectionEstablished((BluetoothLeServiceRight.getBtGatt() != null)
                && (BluetoothLeServiceLeft.getBtGatt() != null));

        if (mCalibrationMode > 0
                && !isConnectionEstablished() && hasConnectionStateBeenChanged() && !mCalibrationWaitingForConnect) {
            showCalibrationStoppedDialog(this);
        }

        if (tv_right_isole != null) {
            if (BluetoothLeServiceRight.getBtGatt() != null) {
                right_isole_isconnect.setImageResource(R.drawable.ok);
                tv_right_isole.setText(mBluetoothDeviceRight.getName());
                isEnabledIndicators(1, true);
            } else {
                right_isole_isconnect.setImageResource(R.drawable.failure);
                tv_right_isole.setText(R.string.insole_r);
                isEnabledIndicators(1, false);
            }

            if (BluetoothLeServiceLeft.getBtGatt() != null) {
                left_isole_isconnect.setImageResource(R.drawable.ok);
                isEnabledIndicators(0, true);
                tv_left_isole.setText(mBluetoothDeviceLeft.getName());
            } else {
                left_isole_isconnect.setImageResource(R.drawable.failure);
                tv_left_isole.setText(R.string.insole_l);
                isEnabledIndicators(0, false);
            }
        }

    }

    private void registerReceiver() {
        IntentFilter intentFilterPackect = new IntentFilter();
        // intentFilterPackect.addAction(AppConstants.ACTION_STATUS_PACKETS_FIRST);
        // intentFilterPackect.addAction(AppConstants.ACTION_STATUS_PACKETS_SECOND);
        // intentFilterPackect.addAction(AppConstants.ACTION_UPDATE_TIMER);
        intentFilterPackect.addAction(AppConstants.ACTION_APPEND_MESSAGE);
        registerReceiver(broadcastReceiverPackets, intentFilterPackect);

    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setTitle(R.string.exit_title)
                        .setMessage(R.string.exit_mess)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(AcMainScreen.this, AlarmReceiver.class);
                                sendBroadcast(intent);

                                stopService();

                                finish();
                            }

                        })
                        .setNegativeButton(android.R.string.no, null);
        final AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setOnShowListener(new OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setHeight(
                        getResources().getDimensionPixelSize(R.dimen.alert_dialog_button_height));
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(
                        getResources().getDimensionPixelSize(R.dimen.aler_dialog_button_text));
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setHeight(
                        getResources().getDimensionPixelSize(R.dimen.alert_dialog_button_height));
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(
                        getResources().getDimensionPixelSize(R.dimen.aler_dialog_button_text));
            }
        });

        alertDialog.show();
    }

    /**
     * Creates dialog. when user press at the any insole
     *
     * @param isconnect - Is insoles connected? If true - connect button turns to disconnect
     */
    private void createDialog(final boolean isconnect) {

        final Dialog dialog = new Dialog(AcMainScreen.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        dialog.setContentView(R.layout.dialog_item);

        Button btnCancel = (Button) dialog.findViewById(R.id.btn_dialog_cancel);
        Button btnConnectDisconnect = (Button) dialog.findViewById(R.id.btn_dialog_connect_disconect);

        if (isconnect) {
            btnConnectDisconnect.setText(R.string.disconnect);
        } else {
            btnConnectDisconnect.setText(R.string.connect);
        }

        btnCancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btnConnectDisconnect.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
                if (!isconnect) {
                    // connect();
                    connectToInsolesActomaticallyFirstTime();


                } else {
                    disconnect();

                }
            }
        });

        TextView textTitle = (TextView) dialog.findViewById(R.id.tv_dialog_title);
        textTitle.setText(getString((!isconnect) ? R.string.connecting_insoles : R.string.disconnecting_insoles));
        dialog.show();
    }

    private void connect() {
        isDisconectedManually = false;
        // DataReceiveFromCallbackFirst.count = 0;
        // InsoleDataParserFirst.getInstance().count = 0;
        // InsoleDataParserSecond.getInstance().count = 0;

        receivedCoupnOfFirstDataPachets = 0;
        receivedCoupnOfSecondDataPachets = 0;

        InsolesUtil.clearBuffer();

        startDevicePicker();
        WiiselApplication.showLog("d", "startDevicePicker connect to device");
    }

    private void disconnect() {
        isDisconectedManually = true;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (timer2 != null) {
            timer2.cancel();
            timer2 = null;
        }

        RefreshConnectionHelper.stop();
        // if (refreshConnectionHelper != null) {
        // refreshConnectionHelper.stop();
        // }

        disconnectDevice();
        WiiselApplication.showLog("e", "disconnect from device");
    }

    /**
     * Start connection to insoles
     */
    private void startDevicePicker() {

        startScan();

        // stop scanning after 10 sec
        Timer scanTimer = new Timer();
        scanTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mScanning) // Acreo_johros: only call stopScan() if still
                    // scanning
                    stopScan();
            }
        }, 10000);

    }

    /**
     * disconnect from the Bluetooth Gatt Service. Remove insoles connection
     */
    private void disconnectDevice() {
        //connection_toggle.setText("OFF");
        readDataFromFirstRightInsole = true;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                connection_toggle.setText("OFF");

            }
        });
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                // Acreo_johros: Important to remove insole BatteryCheckers when
                // disconnecting
                handler.removeCallbacks(mRightBatteryChecker);
                handler.removeCallbacks(mLeftBatteryChecker);

                try {
                    Thread.sleep(100);
                    // stop sampling and disconnect from right insole
                    if (mDataServiceRight != null) {
                        BluetoothGattCharacteristic controlCharacteristic =
                                mDataServiceRight.getCharacteristic(AppConstants.UUID_CONTROL);
                        BluetoothGattCharacteristic samplesCharacteristic =
                                mDataServiceRight.getCharacteristic(AppConstants.UUID_DATA_STREAM);

                        // Acreo_johros: Destroy data and battery services when
                        // disconnecting
                        mDataServiceRight = null;
                        mBatteryServiceRight = null;

                        mBluetoothLeServiceRight.writeCharacteristic(controlCharacteristic, new byte[]{(byte) 0x00});
                        Thread.sleep(100);
                        mBluetoothLeServiceRight.setCharacteristicNotification(samplesCharacteristic, false);
                        Thread.sleep(100);
                        mBluetoothLeServiceRight.disconnect(mBluetoothDeviceRight.getAddress());
                        Thread.sleep(100);
                        mBluetoothLeServiceRight.close();
                        Thread.sleep(100);
                    }
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            restoreViewStatus();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(100);

                    // stop sampling and disconnect from lett insole
                    if (mDataServiceLeft != null) {
                        BluetoothGattCharacteristic controlCharacteristic =
                                mDataServiceLeft.getCharacteristic(AppConstants.UUID_CONTROL);
                        BluetoothGattCharacteristic samplesCharacteristic =
                                mDataServiceLeft.getCharacteristic(AppConstants.UUID_DATA_STREAM);

                        // Acreo_johros: Destroy data and battery services when
                        // disconnecting
                        mDataServiceLeft = null;
                        mBatteryServiceLeft = null;

                        mBluetoothLeServiceLeft.writeCharacteristic(controlCharacteristic, new byte[]{(byte) 0x00});
                        Thread.sleep(100);
                        mBluetoothLeServiceLeft.setCharacteristicNotification(samplesCharacteristic, false);
                        Thread.sleep(100);
                        mBluetoothLeServiceLeft.disconnect(mBluetoothDeviceLeft.getAddress());
                        Thread.sleep(100);
                        mBluetoothLeServiceLeft.close();
                        Thread.sleep(100);
                    }
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            restoreViewStatus();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                stopService();
            }
        });
        thread.start();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(noDataReceiver);
        PhoneStateManager.getInstance().unsubscribe(this);

        try {
            // if (broadcastReceiver != null) {
            // unregisterReceiver(broadcastReceiver);
            // }
            if (broadcastReceiverPackets != null) {
                unregisterReceiver(broadcastReceiverPackets);
            }
            if (mBtAdapter != null) {
                scanLeDevice(false);
                unregisterReceiver(mReceiver);
            }

            // the code below will terminate BLE communications with the insoles
            if (mBluetoothLeServiceRight != null) {
                scanLeDevice(false);
                mBluetoothLeServiceRight.close();
                unbindService(mServiceConnectionRight);
                mBluetoothLeServiceRight = null;
            }
            if (mBluetoothLeServiceLeft != null) {
                scanLeDevice(false);
                mBluetoothLeServiceLeft.close();
                unbindService(mServiceConnectionLeft);
                mBluetoothLeServiceLeft = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

    /**
     * stop foreground service. this service does not allow Android OS kill receiving data
     */
    void stopService() {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                Intent intent = new Intent(AcMainScreen.this, ConnectionService.class);
                stopService(intent);
            }
        });

        if (mBluetoothLeServiceRight == null || BluetoothLeServiceRight.getBtGatt() == null) {
            if (mBluetoothLeServiceLeft == null || BluetoothLeServiceLeft.getBtGatt() == null) {
                thread.start();
            }
        }

        Intent intent = new Intent(AcMainScreen.this, AccelerometerService.class);
        stopService(intent);

    }

    /**
     * Update UI. This method has functionality for prevent lost connection.
     */
    BroadcastReceiver broadcastReceiverPackets = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AppConstants.ACTION_APPEND_MESSAGE)) {
                appendLoggerMessage(intent.getExtras().getString(AppConstants.ACTION_APPEND_MESSAGE));
            }

        }
    };

    private void connectToInsolesActomaticallyFirstTime() {
        isDisconectedManually = false;
        connectToInsolesActomatically();
    }

    /**
     * Refresh connection to insoles
     */
    // private boolean isAutoRefreshFunctionalityActivated = false;
    private void connectToInsolesActomatically() {
        if (RefreshConnectionHelper.isRefreshTaskActive()) {
            return;
        }
        if (isDisconectedManually /* && (!mBluetoothLeServiceLeft.isConected || !mBluetoothLeServiceRight.isConected) */) {
            return;
        }
        appendLoggerMessage("Started automatic connection logic. Connection will be refreshed in the case of an unsuccessful attempt.");
        appendLoggerMessage(getString(R.string.connection_will_be_refreshed));
        // appendLoggerMessage("Device disconnected. Starting autoconnection...");
        // isAutoRefreshFunctionalityActivated = true;
        disconnectDevice();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                connection_toggle.setText("ON");

            }
        });
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                RefreshConnectionHelper.execute(AcMainScreen.this, 10);
            }
        }, 10000);
    }

    static class RefreshConnectionHelper {

        private static RefreshConnectionHelper sInstance;

        public static void execute(AcMainScreen activity, int repeatTimes) {
            if (sInstance == null) {
                synchronized (RefreshConnectionHelper.class) {
                    if (sInstance == null) {
                        sInstance = new RefreshConnectionHelper();
                    }
                }
            }

            // sInstance.mActivity = activity;

            synchronized (sInstance) {
                boolean shouldStartAsyncTask = false;
                if (sInstance.mRefreshConnectionTask == null) {
                    sInstance.mRefreshConnectionTask = new RefreshConnectionTask(sInstance);
                    shouldStartAsyncTask = true;
                }

                sInstance.mRefreshConnectionTask.setActivity(activity);
                sInstance.mRefreshConnectionTask.setCount(0);
                sInstance.mRefreshConnectionTask.setRepeatTimes(repeatTimes);

                if (shouldStartAsyncTask) {
                    sInstance.mRefreshConnectionTask.execute();
                }
            }

        }

        public static synchronized boolean isRefreshTaskActive() {
            return sInstance != null && sInstance.mRefreshConnectionTask != null;
        }

        public static void stop() {
            if (sInstance != null) {
                synchronized (RefreshConnectionHelper.class) {
                    if (sInstance != null && sInstance.mRefreshConnectionTask != null) {
                        sInstance.mRefreshConnectionTask.stop();
                        sInstance.mRefreshConnectionTask = null;
                    }
                }
            }
        }

        RefreshConnectionTask mRefreshConnectionTask;

        private static class RefreshConnectionTask extends AsyncTask<Integer, Void, Void> {

            private String TAG = "RefreshConnectionTask";
            private boolean canWork = true;

            RefreshConnectionHelper mParent;
            private AcMainScreen mActivity;
            private int mCount = 0;
            private int mRepeatTimes = 10;

            public RefreshConnectionTask(RefreshConnectionHelper parent) {
                canWork = true;
                mParent = parent;
            }

            @Override
            protected void onPreExecute() {
                // Toast.makeText(AcMainScreen.this, getString(R.string.connection_will_be_refreshed),
                // Toast.LENGTH_SHORT).show();
                // appendLoggerMessage(getString(R.string.connection_will_be_refreshed));
            }

            public void stop() {
                canWork = false;
            }

            @Override
            protected Void doInBackground(Integer... params) {
                getActivity().appendLoggerMessage("RefreshConnectionTask.doInBackground");

                while (true) {
                    getActivity().disconnectDevice();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }

                    if (!PhoneStateManager.getInstance().isSubscribed(getActivity())) {
                        getActivity().appendLoggerMessage("onDestroy was called");
                        break;
                    }

                    getActivity().handler.post(new Runnable() {

                        @Override
                        public void run() {
                            getActivity().connect();
                        }
                    });
                    try {
                        getActivity().appendLoggerMessage(
                                "Refreshing connection... Attempt #" + String.valueOf(getCount()));
                        // DO not change 15 sec.
                        Thread.sleep(15000);
                        setCount(getCount() + 1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (!canWork) {
                        getActivity().appendLoggerMessage("Closing automaitc connection. canWork: " + canWork);
                        UIUtil.appendLoggerMessage(getActivity().getApplicationContext(),
                                "Closing automaitc connection. canWork: " + canWork);
                        break;
                    }
                    if (getCount() > getRepeatTimes()) {
                        getActivity().appendLoggerMessage("Closing automaitc connection. repeatTimes: " + getCount());
                        UIUtil.appendLoggerMessage(getActivity().getApplicationContext(),
                                "Closing automaitc connection. repeatTimes: " + getCount());
                        break;
                    }
                    if (getActivity().isConnectionEstablished()) {
                        getActivity().appendLoggerMessage(
                                "Closing automaitc connection. isConnectionEstablished: "
                                        + getActivity().isConnectionEstablished());
                        UIUtil.appendLoggerMessage(getActivity().getApplicationContext(),
                                "Closing automaitc connection. isConnectionEstablished: "
                                        + getActivity().isConnectionEstablished());

                        getActivity().appendLoggerMessage(getActivity().getString(R.string.succ_sync_complete));
                        getActivity().handler.post(new Runnable() {

                            @Override
                            public void run() {
                                CustomToast
                                        .makeText(getActivity(), R.string.succ_sync_complete, Toast.LENGTH_SHORT)
                                        .show();
                            }
                        });

                        break;
                    }

                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                synchronized (RefreshConnectionHelper.class) {
                    mParent.mRefreshConnectionTask = null;
                }
            }

            public AcMainScreen getActivity() {
                return mActivity;
            }

            public void setActivity(AcMainScreen activity) {
                mActivity = activity;
            }

            public int getCount() {
                return mCount;
            }

            public void setCount(int count) {
                mCount = count;
            }

            public int getRepeatTimes() {
                return mRepeatTimes;
            }

            public void setRepeatTimes(int repeatTimes) {
                mRepeatTimes = repeatTimes;
            }
        }

    }

    /**
     * Bluetooth turn on
     */

    private void bluetoothTurnOn() {
        // Use this check to determine whether BLE is supported on the device.
        // Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(AcMainScreen.this, R.string.mess_ble_notav, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize device list container and device filter
        mDeviceInfoList = new ArrayList<BleDeviceInfo>();
        mDeviceFilter = new String[2];
        mDeviceFilter[0] = "Wiisel_Left "; // note that "Wiisel_Left " ends with
        // a blank space
        mDeviceFilter[1] = "Wiisel_Right";
        // Resources res = getResources();
        // mDeviceFilter = res.getStringArray(R.array.device_filter);

        // Register the BroadcastReceiver
        mFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mFilter.addAction(BluetoothLeServiceRight.ACTION_GATT_CONNECTED);
        mFilter.addAction(BluetoothLeServiceRight.ACTION_GATT_DISCONNECTED);
        mFilter.addAction(BluetoothLeServiceRight.ACTION_GATT_SERVICES_DISCOVERED);
        mFilter.addAction(BluetoothLeServiceRight.ACTION_RSSI_READ);
        mFilter.addAction(BluetoothLeServiceRight.ACTION_DATA_READ);
        mFilter.addAction(BluetoothLeServiceRight.ACTION_DATA_WRITE);

        mFilter.addAction(BluetoothLeServiceLeft.ACTION_GATT_CONNECTED);
        mFilter.addAction(BluetoothLeServiceLeft.ACTION_GATT_DISCONNECTED);
        mFilter.addAction(BluetoothLeServiceLeft.ACTION_GATT_SERVICES_DISCOVERED);
        mFilter.addAction(BluetoothLeServiceLeft.ACTION_RSSI_READ);
        mFilter.addAction(BluetoothLeServiceLeft.ACTION_DATA_READ);
        mFilter.addAction(BluetoothLeServiceLeft.ACTION_DATA_WRITE);

        // Initializes a Bluetooth adapter. For API level 18 and above, get a
        // reference to BluetoothAdapter through BluetoothManager.
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = mBluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBtAdapter == null) {
            Toast.makeText(AcMainScreen.this, R.string.mess_bt_notav, Toast.LENGTH_LONG).show();
            finish();
            return;
        } else {
            Log.d("tag", "  startBluetoothLeServiceRight();+" + "+  startBluetoothLeServiceLeft(); " + mInitialised);
            appendLoggerMessage("startBluetoothLeServiceRight()+"
                    + "startBluetoothLeServiceLeft(); Status: " + mInitialised);
            if (!mInitialised) {
                // Broadcast receiver
                registerReceiver(mReceiver, mFilter);

                if (!mBtAdapter.isEnabled()) {
                    BluetoothAdapter.getDefaultAdapter().enable();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                startBluetoothLeServiceRight();
                startBluetoothLeServiceLeft();

                mInitialised = true;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == AppConstants.REQUEST_ENABLE_BT) {
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                startBluetoothLeServiceRight();
                startBluetoothLeServiceLeft();
                Toast.makeText(this, R.string.mess_bt_on, Toast.LENGTH_SHORT).show();
            } else {
                // User did not enable Bluetooth or an error occurred
                Toast.makeText(this, R.string.mess_bt_off, Toast.LENGTH_LONG).show();
                finish();
            }
        }
        if (resultCode == AppConstants.REQUEST_SETTINGSONACTIVITYRESULT) {
            finish();
        }
    }

    private void startScan() {
        // Start device discovery
        if (mBleSupported) {
            // mNumDevs = 0;
            mDeviceInfoList.clear();
            // mScanView.notifyDataSetChanged();
            scanLeDevice(true);
            if (!mScanning) {
                WiiselApplication.showLog("e", "Device discovery start failed");
                // setBusy(false);
            }
        } else {
            WiiselApplication.showLog("e", "BLE not supported on this device");
        }

    }

    private void stopScan() {
        mScanning = false;
        // mScanView.updateGui(false);
        scanLeDevice(false);
        WiiselApplication.showLog("d", "Stopped BLE scan");
    }

    private boolean scanLeDevice(boolean enable) {
        // Acreo_johros: Added check for mBtAdapter here so it doesent has to be
        // checked everywhere scanLeDevice() is called
        if (mBtAdapter == null)
            return false;

        if (enable) {
            mScanning = mBtAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBtAdapter.stopLeScan(mLeScanCallback);
        }
        return mScanning;
    }

    private BleDeviceInfo createDeviceInfo(BluetoothDevice device, int rssi) {
        BleDeviceInfo deviceInfo = new BleDeviceInfo(device, rssi);

        return deviceInfo;
    }

    private boolean checkDeviceFilter(BluetoothDevice device) {
        int n = mDeviceFilter.length;
        if (n > 0) {
            boolean found = false;
            for (int i = 0; i < n && !found; i++) {
                found = device.getName().equals(mDeviceFilter[i]);
            }
            return found;
        } else
            // Allow all devices if the device filter is empty
            return true;
    }

    private void addDevice(BleDeviceInfo device) {
        // mNumDevs++;
        mDeviceInfoList.add(device);
        /*
         * mScanView.notifyDataSetChanged(); if (mNumDevs > 1) mScanView.setStatus(mNumDevs + " devices"); else
         * mScanView.setStatus("1 device");
         */
    }

    private BleDeviceInfo findDeviceInfo(BluetoothDevice device) {
        for (int i = 0; i < mDeviceInfoList.size(); i++) {
            if (mDeviceInfoList.get(i).getBluetoothDevice().getAddress().equals(device.getAddress())) {
                return mDeviceInfoList.get(i);
            }
        }
        return null;
    }

    private void startBluetoothLeServiceRight() {
        boolean f;

        Intent bindIntent = new Intent(this, BluetoothLeServiceRight.class);
        startService(bindIntent);

        f = bindService(bindIntent, mServiceConnectionRight, Context.BIND_AUTO_CREATE);
        if (f)
            // Log.d(TAG, "BluetoothLeService - success");
            WiiselApplication.showLog("d", String.format(getString(R.string.mess_bind_ok), "BluetoothLeServiceRight"));
        else {
            // CustomToast.middleBottom(this,
            // "Bind to BluetoothLeService failed");
            Toast.makeText(AcMainScreen.this,
                    String.format(getString(R.string.mess_bind_failed), "BluetoothLeServiceRight"), Toast.LENGTH_LONG)
                    .show();
            finish();
        }
    }

    private void startBluetoothLeServiceLeft() {
        boolean isServiveBinded;

        Intent bindIntent = new Intent(this, BluetoothLeServiceLeft.class);
        startService(bindIntent);

        isServiveBinded = bindService(bindIntent, mServiceConnectionLeft, Context.BIND_AUTO_CREATE);
        if (isServiveBinded)
            // Log.d(TAG, "BluetoothLeService - success");
            WiiselApplication.showLog("d", String.format(getString(R.string.mess_bind_ok), "BluetoothLeServiceLeft"));
        else {
            // CustomToast.middleBottom(this,
            // "Bind to BluetoothLeService failed");
            Toast.makeText(AcMainScreen.this,
                    String.format(getString(R.string.mess_bind_failed), "BluetoothLeServiceLeft"), Toast.LENGTH_LONG)
                    .show();
            finish();
        }
    }

    /**
     * Broadcasted actions from Bluetooth adapter and BluetoothLeService
     */

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        private int rssi_left_n, rssi_left_sum;
        private int rssi_right_n, rssi_right_sum;

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                // Bluetooth adapter state change
                switch (mBtAdapter.getState()) {
                    case BluetoothAdapter.STATE_ON:
                        // startBluetoothLeServiceRight();
                        appendLoggerMessage("Bluetooth is enabled");

                        break;
                    case BluetoothAdapter.STATE_OFF:
                        // Toast.makeText(context, R.string.mess_closing, Toast.LENGTH_LONG).show();
                        // finish();
                        appendLoggerMessage("Bluetooth is disabled");
                        break;
                    default:
                        WiiselApplication.showLog("d", "Action STATE CHANGED not processed");
                        break;
                }
            } else if (BluetoothLeServiceRight.ACTION_GATT_CONNECTED.equals(action)) {
                // GATT connect
                int status = intent.getIntExtra(BluetoothLeServiceRight.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    WiiselApplication.showLog("d", "Successful Connect to Right insole");
                    restoreViewStatus();
                    mBluetoothLeServiceRight.discoverServices();
                    // mRightRssiChecker.run();
                    appendLoggerMessage("Successful Connect to Right insole");
                } else {
                    WiiselApplication.showLog("e", "Connect to Right failed. Status: " + status);
                    mBluetoothLeServiceRight.close();
                    restoreViewStatus();
                    appendLoggerMessage("Connect to Right failed. Status: " + status);
                }
            } else if (BluetoothLeServiceLeft.ACTION_GATT_CONNECTED.equals(action)) {
                // GATT connect
                int status = intent.getIntExtra(BluetoothLeServiceLeft.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    WiiselApplication.showLog("d", "Successful Connect to Left insole");
                    appendLoggerMessage("Successful Connect to Left insole");
                    restoreViewStatus();
                    left_isole_isconnect.setImageResource(R.drawable.ok);
                    mBluetoothLeServiceLeft.discoverServices();
                    // mLeftRssiChecker.run();
                } else {
                    WiiselApplication.showLog("e", "Connect to Left failed. Status: " + status);
                    appendLoggerMessage("Connect to Left failed. Status: " + status);
                    mBluetoothLeServiceLeft.close();
                    restoreViewStatus();
                }
            } else if (BluetoothLeServiceRight.ACTION_GATT_DISCONNECTED.equals(action)) {
                // GATT disconnect
                int status = intent.getIntExtra(BluetoothLeServiceRight.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE);
                // stopDeviceActivity();
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // setBusy(false);
                    // mScanView.setStatus(mBluetoothDeviceFirst.getName() +
                    // " disconnected", STATUS_DURATION);
                    WiiselApplication.showLog("d", mBluetoothDeviceRight.getName() + " disconnected");
                    appendLoggerMessage(mBluetoothDeviceRight.getName() + " disconnected");
                } else {
                    // setError("Disconnect failed. Status: " + status);
                    WiiselApplication.showLog("e", "Disconnect Right failed. Status: " + status);
                }
                mBluetoothLeServiceRight.close();
                restoreViewStatus();
                disconnectDevice();
            } else if (BluetoothLeServiceLeft.ACTION_GATT_DISCONNECTED.equals(action)) {
                // GATT disconnect
                int status = intent.getIntExtra(BluetoothLeServiceLeft.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE);
                // stopDeviceActivity();
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // setBusy(false);
                    // mScanView.setStatus(mBluetoothDeviceFirst.getName() +
                    // " disconnected", STATUS_DURATION);
                    WiiselApplication.showLog("d", mBluetoothDeviceLeft.getName() + " disconnected");
                    appendLoggerMessage(mBluetoothDeviceLeft.getName() + " disconnected");
                } else {
                    // setError("Disconnect failed. Status: " + status);
                    WiiselApplication.showLog("e", "Disconnect Left failed. Status: " + status);
                }
                mBluetoothLeServiceLeft.close();
                restoreViewStatus();
                disconnectDevice();
            } else if (BluetoothLeServiceRight.ACTION_RSSI_READ.equals(action)) {

                int rssi_right = intent.getIntExtra(BluetoothLeServiceRight.EXTRA_INT, 0);

                rssi_right_sum = rssi_right_sum + rssi_right;
                rssi_right_n++;
                if (rssi_right_n == 6) {
                    drawRightSignalStrength(rssi_right_sum / rssi_right_n);
                    rssi_right_n = 0;
                    rssi_right_sum = 0;
                }
            } else if (BluetoothLeServiceLeft.ACTION_RSSI_READ.equals(action)) {
                //
                int rssi_left = intent.getIntExtra(BluetoothLeServiceLeft.EXTRA_INT, 0);

                rssi_left_sum = rssi_left_sum + rssi_left;
                rssi_left_n++;

                if (rssi_left_n % 6 == 0) {
                    drawLeftSignalStrength(rssi_left_sum / rssi_left_n);
                    rssi_left_n = 0;
                    rssi_left_sum = 0;
                }
            } else if (BluetoothLeServiceRight.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                readDataFromFirstRightInsole = false;
                scanLeDevice(true);

                if (mServiceListRight != null) {
                    mServiceListRight.clear();
                }

                try {
                    int ns = mBluetoothLeServiceRight.getNumServices();
                    WiiselApplication.showLog("d", "Number of Services = " + ns);
                    mServiceListRight = mBluetoothLeServiceRight.getSupportedGattServices();
                    mDataServiceRight = null;
                    mBatteryServiceRight = null; // Acreo_johros: added
                    for (int i = 0; i < ns; i++) {
                        BluetoothGattService srv = mServiceListRight.get(i);
                        WiiselApplication.showLog("e", "UUID Right = " + srv.getUuid().toString());
                        if (AppConstants.UUID_DATA_SERVICE.equals(srv.getUuid())) {
                            mDataServiceRight = mServiceListRight.get(i);
                            WiiselApplication.showLog("d", "Discovered Wiisel Data service");
                            appendLoggerMessage("Discovered Right Data service");
                        } else if (AppConstants.UUID_BATTERY_SERVICE.equals(srv.getUuid())) {
                            mBatteryServiceRight = mServiceListRight.get(i);
                            WiiselApplication.showLog("d", "Discovered Wiisel Battery service");
                            appendLoggerMessage("Discovered Right Battery service");
                        }

                    }
                    // Acreo_johros: Enable notifications from Right data
                    // service after it is discovered
                    if (mDataServiceRight != null) {
                        WiiselApplication.showLog("d", "RIGHT ENABLE NOTIFY");
                        BluetoothGattCharacteristic samplesCharacteristic =
                                mDataServiceRight.getCharacteristic(AppConstants.UUID_DATA_STREAM);
                        // Thread.sleep(100);
                        // Enable notification messages from data service
                        mBluetoothLeServiceRight.setCharacteristicNotification(samplesCharacteristic, true);
                        // Thread.sleep(100);
                        // mBluetoothLeServiceRight.readRssi();
                        Thread.sleep(100);

                        if (mDataServiceLeft != null) {
                            // Acreo_johros: if both data services are
                            // discovered start time synchronization
                            mSynchCounterRight = 0;
                            mHandler.postDelayed(mRightTimestamp, 300);
                        }

                    } else {
                        WiiselApplication.showLog("e", "Failed to discover Wiisel Data service");
                        appendLoggerMessage("Failed to discover Right Data service");
                        mBluetoothLeServiceRight.close();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (BluetoothLeServiceLeft.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                if (mServiceListLeft != null) {
                    mServiceListLeft.clear();
                }
                try {
                    int ns = mBluetoothLeServiceLeft.getNumServices();
                    WiiselApplication.showLog("d", "Number of Services = " + ns);
                    mServiceListLeft = mBluetoothLeServiceLeft.getSupportedGattServices();
                    mDataServiceLeft = null;
                    mBatteryServiceLeft = null; // Acreo_johros: added
                    for (int i = 0; i < ns; i++) {
                        BluetoothGattService srv = mServiceListLeft.get(i);
                        WiiselApplication.showLog("e", "UUID Left = " + srv.getUuid().toString());
                        if (AppConstants.UUID_DATA_SERVICE.equals(srv.getUuid())) {
                            mDataServiceLeft = mServiceListLeft.get(i);
                            WiiselApplication.showLog("d", "Discovered Wiisel Data service");
                            appendLoggerMessage("Discovered Left Data service");
                        } else if (AppConstants.UUID_BATTERY_SERVICE.equals(srv.getUuid())) {
                            mBatteryServiceLeft = mServiceListLeft.get(i);
                            WiiselApplication.showLog("d", "Discovered Wiisel Battery service");
                            appendLoggerMessage("Discovered Left Battery service");
                        }
                    }
                    // Acreo_johros: Enable notifications from Left data service
                    // after it is discovered
                    if (mDataServiceLeft != null) {
                        WiiselApplication.showLog("d", "LEFT ENABLE NOTIFY");
                        BluetoothGattCharacteristic samplesCharacteristic =
                                mDataServiceLeft.getCharacteristic(AppConstants.UUID_DATA_STREAM);

                        // Enable notification messages from data service
                        mBluetoothLeServiceLeft.setCharacteristicNotification(samplesCharacteristic, true);
                        // Thread.sleep(100);
                        // mBluetoothLeServiceLeft.readRssi();
                        // Thread.sleep(100);

                        // setTimeStampLeft(0);
                        // Thread.sleep(100);
                        if (mDataServiceRight != null) {
                            // Acreo_johros:if both data services are discovered
                            // start time synchronization.
                            // Synchronization starts on right insole so
                            // mRightTimestamp below is correct
                            mSynchCounterRight = 0;
                            mHandler.postDelayed(mRightTimestamp, 300);
                        }

                    } else {
                        WiiselApplication.showLog("e", "Failed to discover Wiisel Data service");
                        appendLoggerMessage("Failed to discover Right Data service");
                        mBluetoothLeServiceLeft.close();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (BluetoothLeServiceRight.ACTION_DATA_READ.equals(action)) {
                String batt_uuid = AppConstants.UUID_BATTERY_SENSOR_LOCATION.toString();
                if (intent.getExtras().get(BluetoothLeServiceRight.EXTRA_UUID).equals(batt_uuid)) {
                    byte[] byteArray = (byte[]) intent.getExtras().get(BluetoothLeServiceRight.EXTRA_DATA);
                    if (byteArray != null)
                        drawRightBatteryLevel(byteArray[0]);
                }
            } else if (BluetoothLeServiceLeft.ACTION_DATA_READ.equals(action)) {
                String batt_uuid = AppConstants.UUID_BATTERY_SENSOR_LOCATION.toString();
                if (intent.getExtras().get(BluetoothLeServiceLeft.EXTRA_UUID).equals(batt_uuid)) {
                    byte[] byteArray = (byte[]) intent.getExtras().get(BluetoothLeServiceLeft.EXTRA_DATA);
                    if (byteArray != null)
                        drawLeftBatteryLevel(byteArray[0]);
                }
            } else if (BluetoothLeServiceRight.ACTION_DATA_WRITE.equals(action)) {
                String time_uuid = AppConstants.UUID_LOCAL_TIME.toString();
                int status = intent.getIntExtra(BluetoothLeServiceRight.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE); // Acreo_johros:
                // added

                if (intent.getExtras().get(BluetoothLeServiceRight.EXTRA_UUID).equals(time_uuid)) {
                    // an acknowledgement of a write to right insole local clock
                    // has been received

                    // calculate the time difference between issuing the write
                    // command and receiving this acknowledgement
                    rightTimediff = SystemClock.elapsedRealtime() - rightTimestamp;
                    WiiselApplication.showLog("d",
                            "Right Return Time: " + rightTimediff + "  " + SystemClock.elapsedRealtime());// leftTimediff);
                    appendLoggerMessage("Right Return Time: " + rightTimediff + "  " + SystemClock.elapsedRealtime());

                    // Acreo_johros: if the time difference is bigger than 29ms
                    // issue a new
                    // write command for setting the left insole clock after a
                    // random delay in the range 60-100ms.
                    if (rightTimediff > INSOLE_SYNC_TIME || status != BluetoothGatt.GATT_SUCCESS) {
                        mSynchCounterRight++;
                        if (mSynchCounterRight < CONNECTION_ATTEMPS_MAX) {
                            mHandler.postDelayed(mRightTimestamp, 60 + mRandom.nextInt(40));
                        } else {
                            // Acreo_johros: disconnect after trying n times
                            // without success
                            Toast.makeText(AcMainScreen.this,
                                    String.format(getString(R.string.mess_sync_tries), CONNECTION_ATTEMPS_MAX),
                                    Toast.LENGTH_SHORT).show();
                            appendLoggerMessage(getString(R.string.mess_sync_tries));
                            disconnectDevice();
                            connectToInsolesActomatically();
                        }
                    } else {
                        // Acreo_johros: when right insole time is synchronized
                        // start synchronization on left insole
                        mSynchCounterLeft = 0;
                        mHandler.postDelayed(mLeftTimestamp, 60 + mRandom.nextInt(40));
                    }

                    /*
                     * else{ //Command right insole to start sampling and stream data. BluetoothGattCharacteristic
                     * controlCharacteristic = mDataServiceRight.getCharacteristic (AppConstants.UUID_CONTROL);
                     * mBluetoothLeServiceRight.writeCharacteristic (controlCharacteristic, new byte[] { (byte) 0xf3});
                     * }
                     */
                }
            } else if (BluetoothLeServiceLeft.ACTION_DATA_WRITE.equals(action)) {
                String time_uuid = AppConstants.UUID_LOCAL_TIME.toString();
                String control_uuid = AppConstants.UUID_CONTROL.toString();
                int status = intent.getIntExtra(BluetoothLeServiceLeft.EXTRA_STATUS, BluetoothGatt.GATT_FAILURE); // Acreo_johros:
                // added

                if (intent.getExtras().get(BluetoothLeServiceLeft.EXTRA_UUID).equals(time_uuid)) {
                    // an acknowledgement of a write to right insole local clock
                    // has been received

                    // calculate the time difference between issuing the write
                    // command and receiving this acknowledgement
                    leftTimediff = SystemClock.elapsedRealtime() - leftTimestamp;
                    WiiselApplication.showLog("d",
                            "Left Return Time: " + leftTimediff + "  " + SystemClock.elapsedRealtime());// leftTimediff);
                    appendLoggerMessage("Left Return Time: " + leftTimediff + "  " + SystemClock.elapsedRealtime());
                    // /Acreo_johros: if the time difference is bigger than 29ms
                    // issue a new
                    // write command for setting the left insole clock after a
                    // random delay in the range 60-100ms.
                    if (leftTimediff > INSOLE_SYNC_TIME || status != BluetoothGatt.GATT_SUCCESS) {
                        mSynchCounterLeft++;
                        if (mSynchCounterLeft < CONNECTION_ATTEMPS_MAX) {
                            mHandler.postDelayed(mLeftTimestamp, 60 + mRandom.nextInt(40));
                        } else {
                            // Acreo_johros: disconnect after trying n times
                            // without success
                            Toast.makeText(AcMainScreen.this,
                                    String.format(getString(R.string.mess_sync_tries), CONNECTION_ATTEMPS_MAX),
                                    Toast.LENGTH_SHORT).show();
                            appendLoggerMessage(getString(R.string.mess_sync_tries));
                            disconnectDevice();
                            connectToInsolesActomatically();
                        }
                    } else {
                        // After both right and left insole local clocks have
                        // been set, command Left insole to start sampling and
                        // stream the data

                        // start data stream from left insole
                        BluetoothGattCharacteristic controlCharacteristic =
                                mDataServiceLeft.getCharacteristic(AppConstants.UUID_CONTROL);
                        mBluetoothLeServiceLeft.writeCharacteristic(controlCharacteristic, new byte[]{(byte) 0xf3});
                    }
                } else if (intent.getExtras().get(BluetoothLeServiceLeft.EXTRA_UUID).equals(control_uuid)) {
                    // Acreo_johros: check that control command was not the stop
                    // sampling command
                    byte[] controlByte = (byte[]) intent.getExtras().get(BluetoothLeServiceLeft.EXTRA_DATA);
                    if (controlByte[0] == 0)
                        return;

                    // Acreo_johros: start data stream from right insole after
                    // succesfully having started stream from left insole
                    BluetoothGattCharacteristic controlCharacteristic =
                            mDataServiceRight.getCharacteristic(AppConstants.UUID_CONTROL);
                    mBluetoothLeServiceRight.writeCharacteristic(controlCharacteristic, new byte[]{(byte) 0xf3});

                    // Acreo_johros: start battery checkers and RSSI checkers
                    if (mBatteryServiceRight != null) {
                        mHandler.postDelayed(mRightBatteryChecker, 500);
                    } else {
                        WiiselApplication.showLog("e", "Failed to discover Wiisel Battery service");
                        appendLoggerMessage("Failed to discover Right Battery service");
                    }

                    if (mBatteryServiceLeft != null) {
                        mHandler.postDelayed(mLeftBatteryChecker, BATTERY_CHECK_INTERVAL / 4);
                    } else {
                        WiiselApplication.showLog("e", "Failed to discover Wiisel Battery service");
                        appendLoggerMessage("Failed to discover Left Battery service");
                    }

                    // Toast.makeText(AcMainScreen.this, R.string.succ_sync_complete, Toast.LENGTH_SHORT).show();
                    // Acreo_johros: start periodic checks of Received signal
                    // strength indication
                    mLeftRssiChecker.run();
                    mRightRssiChecker.run();
                    // isConnectionEstablished = true;

                    // TODO worked fine
                    SharedPreferences sharedPreferences =
                            getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
                    boolean isFallDetectionOn =
                            sharedPreferences.getBoolean(AppConstants.PREFERENCES_FALLDETECT,
                                    AppConstants.DEFAULT_FALLETECTION);
                    if (isFallDetectionOn) {
                        Intent intent2 = new Intent(AcMainScreen.this, AccelerometerService.class);
                        startService(intent2);
                    }

                    setConnectionEstablished(true);

                    resumeCalibrationIfNeeded();
                }
            } else {
                WiiselApplication.showLog("d", "Unknown action: " + action);
            }

        }
    };

    private void drawLeftBatteryLevel(byte batt) {
        if (left_battery_level != null) {
            appendLoggerMessage("Checking left batery level.. " + batt);
            Log.d("BLE device Battery", "Checking left batery level.. " + batt);
            if (batt > 80)
                left_battery_level.setImageResource(R.drawable.battery_100);
            else if (batt > 60)
                left_battery_level.setImageResource(R.drawable.battery_80);
            else if (batt > 40)
                left_battery_level.setImageResource(R.drawable.battery_60);
            else if (batt > 20)
                left_battery_level.setImageResource(R.drawable.battery_40);
            else if (batt > 5)
                left_battery_level.setImageResource(R.drawable.battery_20);
            else
                left_battery_level.setImageResource(R.drawable.battery_0);
        } else {
            Log.d("BLE device Battery", "Null");
        }
    }

    ;

    private void drawRightBatteryLevel(byte batt) {
        if (right_battery_level != null) {
            appendLoggerMessage("Checking right batery level.. " + batt);
            Log.d("BLE device Battery", "Checking right batery level.. " + batt);
            if (batt > 80)
                right_battery_level.setImageResource(R.drawable.battery_100);
            else if (batt > 60)
                right_battery_level.setImageResource(R.drawable.battery_80);
            else if (batt > 40)
                right_battery_level.setImageResource(R.drawable.battery_60);
            else if (batt > 20)
                right_battery_level.setImageResource(R.drawable.battery_40);
            else if (batt > 5)
                right_battery_level.setImageResource(R.drawable.battery_20);
            else
                right_battery_level.setImageResource(R.drawable.battery_0);
        } else {
            Log.d("BLE device Battery", "Null");
        }
    }

    ;

    Runnable mLeftTimestamp = new Runnable() {
        @Override
        public void run() {
            // create a calendar object and set hour, minute, second and ms to
            // zero that is last midnight.
            Calendar time_2400 = Calendar.getInstance();
            time_2400.set(Calendar.HOUR_OF_DAY, 0);
            time_2400.set(Calendar.MINUTE, 0);
            time_2400.set(Calendar.SECOND, 0);
            time_2400.set(Calendar.MILLISECOND, 0);

            // save a time stamp in milliseconds since latest boot of smartphone
            // to later on
            // calculate time between this write and acknowledgement returning
            // back from the insole
            leftTimestamp = SystemClock.elapsedRealtime();

            // calculate current time in milliseconds passed since midnight
            long time = System.currentTimeMillis() - time_2400.getTimeInMillis();

            // write current time to right insole clock
            setTimeStampLeft((int) time);
        }
    };

    Runnable mRightTimestamp = new Runnable() {
        @Override
        public void run() {
            // create a calendar object and set hour, minute, second and ms to
            // zero that is last midnight.
            Calendar time_2400 = Calendar.getInstance();
            time_2400.set(Calendar.HOUR_OF_DAY, 0);
            time_2400.set(Calendar.MINUTE, 0);
            time_2400.set(Calendar.SECOND, 0);
            time_2400.set(Calendar.MILLISECOND, 0);

            // save a time stamp in milliseconds since latest boot of smartphone
            // to later on
            // calculate time between this write and acknowledgement returning
            // back from the insole
            rightTimestamp = SystemClock.elapsedRealtime();

            // calculate current time in milliseconds passed since midnight
            long time = System.currentTimeMillis() - time_2400.getTimeInMillis();

            // write current time to right insole clock
            setTimeStampRight((int) time);
        }
    };

    private boolean setTimeStampLeft(int time) {
        if (mDataServiceLeft == null) {
            return false;
        }

        BluetoothGattCharacteristic timestampCharacteristic =
                mDataServiceLeft.getCharacteristic(AppConstants.UUID_LOCAL_TIME);

        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) (time >>> (i * 8));
        }
        mBluetoothLeServiceLeft.writeCharacteristic(timestampCharacteristic, bytes);
        return true;
    }

    ;

    private boolean setTimeStampRight(int time) {
        if (mDataServiceRight == null) {
            return false;
        }

        BluetoothGattCharacteristic timestampCharacteristic =
                mDataServiceRight.getCharacteristic(AppConstants.UUID_LOCAL_TIME);

        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) (time >>> (i * 8));
        }
        mBluetoothLeServiceRight.writeCharacteristic(timestampCharacteristic, bytes);
        return true;
    }

    ;

    Runnable mLeftRssiChecker = new Runnable() {
        @Override
        public void run() {
            // Acreo_johros: Stop RssiChecker if mDataServiceLeft is destroyed
            if (mDataServiceLeft == null) {
                return;
            }

            if (mBluetoothLeServiceLeft != null) {
                mBluetoothLeServiceLeft.readRssi();
                handler.postDelayed(mLeftRssiChecker, RSSI_INTERVAL);
            }
        }
    };

    Runnable mRightRssiChecker = new Runnable() {
        @Override
        public void run() {
            // Acreo_johros: Stop RssiChecker if mDataServiceLeft is destroyed
            if (mDataServiceRight == null) {
                return;
            }

            if (mBluetoothLeServiceRight != null) {
                mBluetoothLeServiceRight.readRssi();
                handler.postDelayed(mRightRssiChecker, RSSI_INTERVAL);
            }
        }
    };

    private void drawLeftSignalStrength(final int rssi) {
        handler.post(new Runnable() {

            @Override
            public void run() {

                UIUtil.appendLoggerMessage(getApplicationContext(), "RSSI second: " + rssi);
                int localRssi = Math.abs(rssi);

                if (localRssi < 60) {
                    left_isole_signal.setImageResource(R.drawable.low_signal_5);
                } else if (localRssi < 70) {
                    left_isole_signal.setImageResource(R.drawable.low_signal_4);
                } else if (localRssi < 80) {
                    left_isole_signal.setImageResource(R.drawable.low_signal_3);
                } else if (localRssi < 90) {
                    left_isole_signal.setImageResource(R.drawable.low_signal_2);
                } else {
                    left_isole_signal.setImageResource(R.drawable.low_signal_1);
                }

            }
        });
    }

    ;

    private void drawRightSignalStrength(final int rssi) {
        handler.post(new Runnable() {

            @Override
            public void run() {

                UIUtil.appendLoggerMessage(getApplicationContext(), "RSSI first: " + rssi);

                int localRssi = Math.abs(rssi);

                if (localRssi < 60) {
                    right_isole_signal.setImageResource(R.drawable.low_signal_5);
                } else if (localRssi < 70) {
                    right_isole_signal.setImageResource(R.drawable.low_signal_4);
                } else if (localRssi < 80) {
                    right_isole_signal.setImageResource(R.drawable.low_signal_3);
                } else if (localRssi < 90) {
                    right_isole_signal.setImageResource(R.drawable.low_signal_2);
                } else {
                    right_isole_signal.setImageResource(R.drawable.low_signal_1);
                }
            }
        });
    }

    Runnable mLeftBatteryChecker = new Runnable() {
        @Override
        public void run() {
            if (mBatteryServiceLeft != null && mBluetoothLeServiceLeft != null) {
                BluetoothGattCharacteristic readBatteryCharacteristic =
                        mBatteryServiceLeft.getCharacteristic(AppConstants.UUID_BATTERY_SENSOR_LOCATION);
                mBluetoothLeServiceLeft.readCharacteristic(readBatteryCharacteristic);
            }
            handler.postDelayed(mLeftBatteryChecker, BATTERY_CHECK_INTERVAL);
        }
    };

    Runnable mRightBatteryChecker = new Runnable() {
        @Override
        public void run() {
            if (mBatteryServiceRight != null && mBluetoothLeServiceRight != null) {
                BluetoothGattCharacteristic readBatteryCharacteristic =
                        mBatteryServiceRight.getCharacteristic(AppConstants.UUID_BATTERY_SENSOR_LOCATION);
                mBluetoothLeServiceRight.readCharacteristic(readBatteryCharacteristic);
            }
            handler.postDelayed(mRightBatteryChecker, BATTERY_CHECK_INTERVAL);
        }
    };
    /**
     * Code to manage Service life cycle.
     */
    private final ServiceConnection mServiceConnectionRight = new ServiceConnection() {

        public void onServiceConnected(ComponentName componentName, IBinder service) {
            WiiselApplication.showLog("d", "BluetoothLeServiceRight found");
            mBluetoothLeServiceRight = ((BluetoothLeServiceRight.LocalBinder) service).getService();
            if (!mBluetoothLeServiceRight.initialize(mBluetoothManager)) {
                // Log.e(TAG, "Unable to initialize BluetoothLeService");
                WiiselApplication.showLog("e", "Unable to initialize BluetoothLeServiceRight");
                appendLoggerMessage("Unable to initialize BluetoothLeServiceRight");
                finish();
                return;
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeServiceRight = null;
            // Log.i(TAG, "BluetoothLeService disconnected");
            appendLoggerMessage("BluetoothLeServiceRight disconnected");
            WiiselApplication.showLog("d", "BluetoothLeServiceRight disconnected");
        }
    };

    // Code to manage Service life cycle.
    private final ServiceConnection mServiceConnectionLeft = new ServiceConnection() {

        public void onServiceConnected(ComponentName componentName, IBinder service) {
            WiiselApplication.showLog("d", "BluetoothLeServiceLeft found");
            mBluetoothLeServiceLeft = ((BluetoothLeServiceLeft.LocalBinder) service).getService();
            if (!mBluetoothLeServiceLeft.initialize(mBluetoothManager)) {
                appendLoggerMessage("Unable to initialize BluetoothLeServiceLeft");
                WiiselApplication.showLog("e", "Unable to initialize BluetoothLeServiceLeft");
                finish();
                return;
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeServiceLeft = null;
            // Log.i(TAG, "BluetoothLeService disconnected");
            appendLoggerMessage("BluetoothLeServiceLeft disconnected");
            WiiselApplication.showLog("d", "BluetoothLeServiceLeft disconnected");
        }
    };

    /**
     * Device scan callback. NB! Nexus 4 and Nexus 7 (2012) only provide one scan result per scan
     */

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                public void run() {
                    appendLoggerMessage("LeScanCallback found " + device.getName());
                    WiiselApplication.showLog("d", "LeScanCallback found " + device.getName());

                    // Acreo_johros: sometimes device.getName() returns a null
                    // pointer which could crash the program
                    if (device.getName() == null)
                        return;

                    // Filter devices
                    if (checkDeviceFilter(device)) {
                        // if (!deviceInfoExists(device.getAddress())) {
                        // New device
                        BleDeviceInfo deviceInfo = createDeviceInfo(device, rssi);

                        addDevice(deviceInfo);

                        if (readDataFromFirstRightInsole) {
                            if (device.getName().equals("Wiisel_Right")) {
                                if (mBluetoothLeServiceRight != null) {
                                    stopScan();
                                    mBluetoothDeviceRight = device;
                                    mBluetoothLeServiceRight.connect(mBluetoothDeviceRight.getAddress());
                                    WiiselApplication.showLog("d", "Added " + device.getName());
                                    appendLoggerMessage("LeScanCallback. Added " + device.getName());
                                }
                            }
                        } else if (device.getName().equals("Wiisel_Left ")) {
                            if (mBluetoothLeServiceLeft != null) {
                                stopScan();
                                mBluetoothDeviceLeft = device;
                                mBluetoothLeServiceLeft.connect(mBluetoothDeviceLeft.getAddress());
                                WiiselApplication.showLog("d", "Added " + device.getName());
                                appendLoggerMessage("LeScanCallback. Added " + device.getName());
                            }
                        }

                    } else {
                        // Already in list, update RSSI info
                        BleDeviceInfo deviceInfo = findDeviceInfo(device);
                        deviceInfo.updateRssi(rssi);
                        // mScanView.notifyDataSetChanged();
                        appendLoggerMessage("LeScanCallback. Updated " + device.getName());
                        WiiselApplication.showLog("d", "Updated " + device.getName());
                    }
                }
                // }

            });
        }
    };

    private void appendLoggerMessage(final String text) {
        if (text == null) {
            return;
        }

        Log.d("Wiisel logger", "[" + Integer.toHexString(hashCode()) + "] " + text);

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                TextView loggerTextView = (TextView) findViewById(R.id.loggertextview);
                if (loggerTextView != null) {
                    loggerTextView.append(text + "\n");
                }

                ScrollView loggerScrollView = (ScrollView) findViewById(R.id.loggerscrollview);
                if (loggerScrollView != null) {
                    loggerScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            }
        });

    }

    int receivedCoupnOfFirstDataPachets = 0;
    int receivedCoupnOfSecondDataPachets = 0;

    @Override
    public void putFirstInsoleData(int[] data, int[] press) {

        receivedCoupnOfFirstDataPachets++;
        if (tvFirstInsolePackets != null) {
            handler.post(new Runnable() {

                @Override
                public void run() {
                    tvFirstInsolePackets.setText(receivedCoupnOfFirstDataPachets + "");
                }
            });
        } else {
            Log.d(AcMainScreen.class.getSimpleName(), "tvFirstInsolePackets == null");
        }

        if (dataReseivedListener != null) {
            dataReseivedListener.onReceived(InsolesType.FIRST);
        }

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                if (timer2 != null) {
                    timer2.cancel();
                    timer2 = null;
                    // if (!isAutoRefreshFunctionalityActivated) {

                    appendLoggerMessage("putFirstInsoleData");

                    if (!RefreshConnectionHelper.isRefreshTaskActive()) {
                        connectToInsolesActomatically();
                    }
                }
            }
        }, TIME_BEFORE_CLOSING);

    }

    @Override
    public void putSecondInsoleData(int[] data, int[] press) {

        receivedCoupnOfSecondDataPachets++;

        if (tvSecondInsolePackets != null) {
            handler.post(new Runnable() {

                @Override
                public void run() {
                    tvSecondInsolePackets.setText(receivedCoupnOfSecondDataPachets + "");
                }
            });
        } else {
            Log.d(AcMainScreen.class.getSimpleName(), "tvSecondInsolePackets == null");
        }

        if (dataReseivedListener != null) {
            dataReseivedListener.onReceived(InsolesType.SECOND);
        }

        if (timer2 != null) {
            timer2.cancel();
            timer2 = null;
        }

        timer2 = new Timer();
        timer2.schedule(new TimerTask() {

            @Override
            public void run() {

                if (timer != null) {
                    timer.cancel();
                    timer = null;
                    // if (!isAutoRefreshFunctionalityActivated) {

                    appendLoggerMessage("putSecondInsoleData");
                    if (!RefreshConnectionHelper.isRefreshTaskActive()) {
                        connectToInsolesActomatically();
                    }
                }
            }
        }, TIME_BEFORE_CLOSING);
    }

    public boolean isConnectionEstablished() {
        return mConnectionEstablished;
    }

    public void setConnectionEstablished(boolean connectionEstablished) {
        mPrevConnectionEstablished = mConnectionEstablished;
        mConnectionEstablished = connectionEstablished;
    }

    public boolean hasConnectionStateBeenChanged() {
        return mPrevConnectionEstablished ^ mConnectionEstablished;
    }

    BroadcastReceiver noDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();

            NetworkInfo info = (NetworkInfo) extras
                    .getParcelable("networkInfo");

            NetworkInfo.State state = info.getState();
            if (state == State.DISCONNECTED) {
                ConnectivityManager cm =
                        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (activeNetwork == null)
                    AlertNetworkConnection();


            }



        }
        };


    public void AlertNetworkConnection() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Data Connection");
        builder.setMessage("Please check connection settings");
        builder.setNegativeButton("close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        soundAlert();
        vibration();
    }


    public void soundAlert(){
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Vibrator vibration() {

        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        long[] pattern = { 0, 3000, 3000 };

        v.vibrate(pattern, -1);
        return v;

    }


}
