package julioarellano.wiiselandroid7.utils;

import android.hardware.SensorEvent;

public class FallUtil {

    public static boolean isFallDetected(SensorEvent event, double maxAcceleration) {
        double sqrt = getAcceleration(event);
        if (sqrt > maxAcceleration) {
            return true;
        }
        return false;
    }

    public static double getAcceleration(SensorEvent event) {
        final float x = event.values[0];
        final float y = event.values[1];
        final float z = event.values[2];
        double sqrt = Math.sqrt(Math.abs(x * x) + Math.abs(y * y) + Math.abs(z * z));
        return sqrt;
    }

    public static int getInsolesMaxAngle(int insoleAxisXF, int insoleAxisYF, int insoleAxisZF, int insoleAxisXS, int insoleAxisYS,
        int insoleAxisZS) {

        int insoleAngleF = getInsoleAngle(insoleAxisXF, insoleAxisYF, insoleAxisZF);
        if (insoleAngleF == -1) {
            return insoleAngleF;
        }

        int insoleAngleS = getInsoleAngle(insoleAxisXS, insoleAxisYS, insoleAxisZS);
        if (insoleAngleS == -1) {
            return insoleAngleS;
        }

        return Math.max(insoleAngleF, insoleAngleS);
    }

    public static int getInsoleAngle(int axisX, int axisY, int axisZ) {
        int etalon = 4300;
        if (axisZ < 0) {
            return -1;
        }

        axisX = Math.abs(axisX);
        axisY = Math.abs(axisY);
        axisZ = Math.abs(axisZ);

        int maxAxis = Math.max(axisY, axisX);

        double maxAngle = ((double) ((axisZ - maxAxis) * 100)) / etalon;

        return (int) (90 - maxAngle) / 2;
    }
}
