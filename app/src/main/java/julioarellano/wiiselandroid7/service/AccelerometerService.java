package julioarellano.wiiselandroid7.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import julioarellano.wiiselandroid7.utils.*;
import julioarellano.wiiselandroid7.R;
import julioarellano.wiiselandroid7.activity.interfaces.IRefreshable;
import julioarellano.wiiselandroid7.activity.interfaces.SoundManagerListener;
import julioarellano.wiiselandroid7.application.WiiselApplication;
import julioarellano.wiiselandroid7.constants.AppConstants;
import julioarellano.wiiselandroid7.manager.ConnectionManager;
import julioarellano.wiiselandroid7.manager.PhoneStateManager;
import julioarellano.wiiselandroid7.manager.SoundManager;
import julioarellano.wiiselandroid7.utils.DelayedThread;
import julioarellano.wiiselandroid7.utils.DelayedThread.IDelayedThreadListener;
import julioarellano.wiiselandroid7.utils.FallLogicStage;
import julioarellano.wiiselandroid7.utils.FallUtil;
import julioarellano.wiiselandroid7.utils.InsolesUtil;
import julioarellano.wiiselandroid7.utils.UIUtil;

import org.apache.http.*;
import org.json.JSONObject;

public class AccelerometerService extends Service implements SensorEventListener, IRefreshable {
    private SensorManager mSensorManager = null;
    public static final String TAG = AccelerometerService.class.getName();
    public static final int SCREEN_OFF_RECEIVER_DELAY = 500;
    private int insoleAxisXF = -1, insoleAxisYF = -1, insoleAxisZF = -1, insoleAxisXS = -1, insoleAxisYS = -1, insoleAxisZS = -1;
    private Handler handler = new Handler();
    private int accelSensitivity;
    private int noMovementAcceleration = 14;
    private FallLogicStage fallLogicStage = FallLogicStage.ZERO;
    private DelayedThread delayedThreadSleep;
    private int delayTime = 5;
    private int criticalAngle = 45;
    private SoundManager soundManager;
    private int[] pressF;
    private int[] pressS;
    private int pressSensitivity = 0;

    @Override
    public void onAccuracyChanged(Sensor paramSensor, int paramInt) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        switch (fallLogicStage) {
        case ZERO:
            if (FallUtil.isFallDetected(event, accelSensitivity)) {
                UIUtil.appendLoggerMessage(getApplicationContext(), "Fall was detected");
                setFallStage(FallLogicStage.ONE);
            }
            break;
        case ONE:
            if (delayedThreadSleep == null) {
                delayedThreadSleep = new DelayedThread(delayTime * 1000, new IDelayedThreadListener() {

                    @Override
                    public void finish() {
                        UIUtil.appendLoggerMessage(getApplicationContext(), "Timer was canceled. " + delayTime + " sec is over.");
                        delayedThreadSleep = null;
                        setFallStage(FallLogicStage.TWO);
                    }

                    @Override
                    public void stop() {
                        delayedThreadSleep = null;
                        setFallStage(FallLogicStage.ZERO);
                    }
                });
                delayedThreadSleep.start();
            }
            break;
        case TWO:
            if (soundManager == null) {
                soundManager = SoundManager.getInstance(this, new SoundManagerListener() {

                    @Override
                    public void spoped() {
                        setFallStage(FallLogicStage.ZERO);
                        soundManager = null;
                    }

                    @Override
                    public void finished() {
                        sendEmailSms();
                        UIUtil.appendLoggerMessage(getApplicationContext(), "Email and sms sending...");
                    }
                });
                soundManager.start();
            }

            // check acceleration
            if (noMovementAcceleration != 0) {
                double acceleration = FallUtil.getAcceleration(event);
                if (acceleration > noMovementAcceleration) {
                    UIUtil.appendLoggerMessage(getApplicationContext(), "Acceleration: " + acceleration);
                    if (soundManager != null) {
                        soundManager.stop();
                    }
                }
            }

            // check angle
            if (criticalAngle != 0) {
                int insolesMaxAngle = FallUtil.getInsolesMaxAngle(insoleAxisXF, insoleAxisYF, insoleAxisZF, insoleAxisXS, insoleAxisYS,
                    insoleAxisZS);
                if (insolesMaxAngle != -1 && insolesMaxAngle < criticalAngle) {
                    UIUtil.appendLoggerMessage(getApplicationContext(), "Isole angle is: " + insolesMaxAngle + " Critical is: "
                        + criticalAngle);
                    if (soundManager != null) {
                        soundManager.stop();
                    }
                }
            }

            // check pressure
            if (pressSensitivity != 0) {
                if (pressF[2] > pressSensitivity || pressF[10] > pressSensitivity || pressS[8] > pressSensitivity
                    || pressS[4] > pressSensitivity) {

                    UIUtil.appendLoggerMessage(getApplicationContext(), "Isole pressure bigger than: " + pressSensitivity
                        + " \nFirst insole press:" + pressF[2] + ", " + pressF[10] + " \nSecond insole press: " + pressF[8] + ", "
                        + pressF[4]);
                    if (soundManager != null) {
                        soundManager.stop();
                    }
                }
            }

            break;
        case THREE:

            break;

        }

    }

    @Override
    public IBinder onBind(Intent paramIntent) {
        return null;
    }

    private void registerListener() {
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
    }

    private void unregisterListener() {
        mSensorManager.unregisterListener(this);
    }

    public BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {

                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        unregisterListener();
                        registerListener();
                    }
                }, SCREEN_OFF_RECEIVER_DELAY);
            }

        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences sharedPreferences = getSharedPreferences(getResources().getString(R.string.app_name), Context.MODE_PRIVATE);
        accelSensitivity = sharedPreferences.getInt(AppConstants.PREFERENCES_ACCELEROMETERSENSITIVITY,
            AppConstants.DEFAULT_ACCELEROMETERSENSITIVITY);
        noMovementAcceleration = sharedPreferences.getInt(AppConstants.PREFERENCES_NOMOVEMENTSENSITIVITY,
            AppConstants.DEFAULT_NOMOVEMENTSENSITIVITY);
        criticalAngle = sharedPreferences.getInt(AppConstants.PREFERENCES_PERMITED_ANGLE, AppConstants.DEFAULT_PERMITED_ANGLE);
        delayTime = sharedPreferences.getInt(AppConstants.PREFERENCES_DELAY_TIME, AppConstants.DEFAULT_DELAY_TIME);
        if (delayTime == 0) {
            delayTime = 1;
        }
        pressSensitivity = sharedPreferences.getInt(AppConstants.PREFERENCES_PRESS_SENSITIVITY, AppConstants.DEFAULT_PRESS_SENSITIVITY);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        IntentFilter intentFilterPackect = new IntentFilter();
        intentFilterPackect.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, intentFilterPackect);

        PhoneStateManager.getInstance().subscribe(this);

        // show notification
        showNotificationForeground(2, "WIISEL", getString(R.string.mess_falldetactivated));

        registerListener();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        unregisterListener();
        stopForeground(true);
        PhoneStateManager.getInstance().unsubscribe(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        return START_STICKY;
    }

    public void setFallStage(FallLogicStage fallLogicStage) {
        this.fallLogicStage = fallLogicStage;
    }

    private void showNotificationForeground(int msgID, String title, String text) {

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title).setContentText(text);

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = mBuilder.build();

        mNotificationManager.notify(msgID, notification);
        startForeground(msgID, notification);

    }

    public void sendEmailSms() {
        // send alarm to the server. fall is detected
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {

                ConnectionManager connectionManager = ConnectionManager.getInstance(AccelerometerService.this);

                // send EMAIL alarm to the server
                try {
                    HttpResponse response = connectionManager.fallDetectionAlarmNotifViaEmail();
                    JSONObject fromHttpToJson = connectionManager.fromHttpToJson(response);
                    final String msg = fromHttpToJson.getString(AppConstants.PARAM_MESSAGE);
                    WiiselApplication.showLog("d", msg);
                    UIUtil.appendLoggerMessage(getApplicationContext(), "Send Email: " + msg);
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(AccelerometerService.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(AccelerometerService.this, getString(R.string.mess_alertdoesnotsent), Toast.LENGTH_SHORT).show();
                        }
                    });
                    e.printStackTrace();
                }

                // send SMS alarm to the clinical
                try {
                    connectionManager.fallDetectionAlarmNotifViaSMS();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                InsolesUtil.setCalibrationMode(-1);

            }
        });
        thread.start();
    }

    @Override
    public void putFirstInsoleData(int[] data, int[] press) {
        this.pressF = press;
        insoleAxisXF = data[4];
        insoleAxisYF = data[5];
        insoleAxisZF = data[6];
    }

    @Override
    public void putSecondInsoleData(int[] data, int[] press) {
        this.pressS = press;
        insoleAxisXS = data[4];
        insoleAxisYS = data[5];
        insoleAxisZS = data[6];
    }
}
