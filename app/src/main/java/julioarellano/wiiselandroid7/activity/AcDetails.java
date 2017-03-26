package julioarellano.wiiselandroid7.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import julioarellano.wiiselandroid7.R;
import julioarellano.wiiselandroid7.activity.interfaces.IRefreshable;
import julioarellano.wiiselandroid7.manager.PhoneStateManager;

public class AcDetails extends Activity implements IRefreshable {

    private TextView gyro_x_l, gyro_y_l, gyro_z_l;
    private TextView accel_mpu_x_l, accel_mpu_y_l, accel_mpu_z_l;
    private TextView accel_lis_x_l, accel_lis_y_l, accel_lis_z_l;
    private TextView pressure_0_l, pressure_1_l, pressure_2_l, pressure_3_l, pressure_4_l, pressure_5_l, pressure_6_l, pressure_7_l,
        pressure_8_l, pressure_9_l, pressure_10_l, pressure_11_l, pressure_12_l, pressure_13_l;

    private TextView gyro_x_r, gyro_y_r, gyro_z_r;
    private TextView accel_mpu_x_r, accel_mpu_y_r, accel_mpu_z_r;
    private TextView accel_lis_x_r, accel_lis_y_r, accel_lis_z_r;
    private TextView pressure_0_r, pressure_1_r, pressure_2_r, pressure_3_r, pressure_4_r, pressure_5_r, pressure_6_r, pressure_7_r,
        pressure_8_r, pressure_9_r, pressure_10_r, pressure_11_r, pressure_12_r, pressure_13_r;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_details);

        PhoneStateManager.getInstance().subscribe(this);

        gyro_x_l = (TextView) findViewById(R.id.tv_gyro_x_l_label);
        gyro_y_l = (TextView) findViewById(R.id.tv_gyro_y_l_label);
        gyro_z_l = (TextView) findViewById(R.id.tv_gyro_z_l_label);

        accel_mpu_x_l = (TextView) findViewById(R.id.tv_accel_mpu_x_l_label);
        accel_mpu_y_l = (TextView) findViewById(R.id.tv_accel_mpu_y_l_label);
        accel_mpu_z_l = (TextView) findViewById(R.id.tv_accel_mpu_z_l_label);

        accel_lis_x_l = (TextView) findViewById(R.id.tv_accel_lis_x_l_label);
        accel_lis_y_l = (TextView) findViewById(R.id.tv_accel_lis_y_l_label);
        accel_lis_z_l = (TextView) findViewById(R.id.tv_accel_lis_z_l_label);

        pressure_0_l = (TextView) findViewById(R.id.tv_pressure_0_l_label);
        pressure_1_l = (TextView) findViewById(R.id.tv_pressure_1_l_label);
        pressure_2_l = (TextView) findViewById(R.id.tv_pressure_2_l_label);
        pressure_3_l = (TextView) findViewById(R.id.tv_pressure_3_l_label);
        pressure_4_l = (TextView) findViewById(R.id.tv_pressure_4_l_label);
        pressure_5_l = (TextView) findViewById(R.id.tv_pressure_5_l_label);
        pressure_6_l = (TextView) findViewById(R.id.tv_pressure_6_l_label);
        pressure_7_l = (TextView) findViewById(R.id.tv_pressure_7_l_label);
        pressure_8_l = (TextView) findViewById(R.id.tv_pressure_8_l_label);
        pressure_9_l = (TextView) findViewById(R.id.tv_pressure_9_l_label);
        pressure_10_l = (TextView) findViewById(R.id.tv_pressure_10_l_label);
        pressure_11_l = (TextView) findViewById(R.id.tv_pressure_11_l_label);
        pressure_12_l = (TextView) findViewById(R.id.tv_pressure_12_l_label);
        pressure_13_l = (TextView) findViewById(R.id.tv_pressure_13_l_label);

        gyro_x_r = (TextView) findViewById(R.id.tv_gyro_x_r_label);
        gyro_y_r = (TextView) findViewById(R.id.tv_gyro_y_r_label);
        gyro_z_r = (TextView) findViewById(R.id.tv_gyro_z_r_label);

        accel_mpu_x_r = (TextView) findViewById(R.id.tv_accel_mpu_x_r_label);
        accel_mpu_y_r = (TextView) findViewById(R.id.tv_accel_mpu_y_r_label);
        accel_mpu_z_r = (TextView) findViewById(R.id.tv_accel_mpu_z_r_label);

        accel_lis_x_r = (TextView) findViewById(R.id.tv_accel_lis_x_r_label);
        accel_lis_y_r = (TextView) findViewById(R.id.tv_accel_lis_y_r_label);
        accel_lis_z_r = (TextView) findViewById(R.id.tv_accel_lis_z_r_label);

        pressure_0_r = (TextView) findViewById(R.id.tv_pressure_0_r_label);
        pressure_1_r = (TextView) findViewById(R.id.tv_pressure_1_r_label);
        pressure_2_r = (TextView) findViewById(R.id.tv_pressure_2_r_label);
        pressure_3_r = (TextView) findViewById(R.id.tv_pressure_3_r_label);
        pressure_4_r = (TextView) findViewById(R.id.tv_pressure_4_r_label);
        pressure_5_r = (TextView) findViewById(R.id.tv_pressure_5_r_label);
        pressure_6_r = (TextView) findViewById(R.id.tv_pressure_6_r_label);
        pressure_7_r = (TextView) findViewById(R.id.tv_pressure_7_r_label);
        pressure_8_r = (TextView) findViewById(R.id.tv_pressure_8_r_label);
        pressure_9_r = (TextView) findViewById(R.id.tv_pressure_9_r_label);
        pressure_10_r = (TextView) findViewById(R.id.tv_pressure_10_r_label);
        pressure_11_r = (TextView) findViewById(R.id.tv_pressure_11_r_label);
        pressure_12_r = (TextView) findViewById(R.id.tv_pressure_12_r_label);
        pressure_13_r = (TextView) findViewById(R.id.tv_pressure_13_r_label);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PhoneStateManager.getInstance().unsubscribe(this);
    }

    @Override
    public void putFirstInsoleData(final int[] data, final int[] press) {
        handler.post(new Runnable() {

            @Override
            public void run() {
                gyro_x_l.setText("X: " + data[1]);
                gyro_y_l.setText("Y: " + data[2]);
                gyro_z_l.setText("Z: " + data[3]);

                accel_mpu_x_l.setText("X: " + data[4]);
                accel_mpu_y_l.setText("Y: " + data[5]);
                accel_mpu_z_l.setText("Z: " + data[6]);

                accel_lis_x_l.setText("X: " + data[7]);
                accel_lis_y_l.setText("y: " + data[8]);
                accel_lis_z_l.setText("Z: " + data[9]);

                pressure_0_l.setText("[0]: " + press[0]);
                pressure_1_l.setText("[1]: " + press[1]);
                pressure_2_l.setText("[2]: " + press[2]);
                pressure_3_l.setText("[3]: " + press[3]);
                pressure_4_l.setText("[4]: " + press[4]);
                pressure_5_l.setText("[5]: " + press[5]);
                pressure_6_l.setText("[6]: " + press[6]);
                pressure_7_l.setText("[7]: " + press[7]);
                pressure_8_l.setText("[8]: " + press[8]);
                pressure_9_l.setText("[9]: " + press[9]);
                pressure_10_l.setText("[10]: " + press[10]);
                pressure_11_l.setText("[11]: " + press[11]);
                pressure_12_l.setText("[12]: " + press[12]);
                pressure_13_l.setText("[13]: " + press[13]);
            }
        });
    }

    @Override
    public void putSecondInsoleData(final int[] data, final int[] press) {
        handler.post(new Runnable() {

            @Override
            public void run() {
                gyro_x_r.setText("X: " + data[1]);
                gyro_y_r.setText("Y: " + data[2]);
                gyro_z_r.setText("Z: " + data[3]);

                accel_mpu_x_r.setText("X: " + data[4]);
                accel_mpu_y_r.setText("Y: " + data[5]);
                accel_mpu_z_r.setText("Z: " + data[6]);

                accel_lis_x_r.setText("X: " + data[7]);
                accel_lis_y_r.setText("Y: " + data[8]);
                accel_lis_z_r.setText("Z: " + data[9]);

                pressure_0_r.setText("[0]: " + press[0]);
                pressure_1_r.setText("[1]: " + press[1]);
                pressure_2_r.setText("[2]: " + press[2]);
                pressure_3_r.setText("[3]: " + press[3]);
                pressure_4_r.setText("[4]: " + press[4]);
                pressure_5_r.setText("[5]: " + press[5]);
                pressure_6_r.setText("[6]: " + press[6]);
                pressure_7_r.setText("[7]: " + press[7]);
                pressure_8_r.setText("[8]: " + press[8]);
                pressure_9_r.setText("[9]: " + press[9]);
                pressure_10_r.setText("[10]: " + press[10]);
                pressure_11_r.setText("[11]: " + press[11]);
                pressure_12_r.setText("[12]: " + press[12]);
                pressure_13_r.setText("[13]: " + press[13]);
            }
        });
    }

}
