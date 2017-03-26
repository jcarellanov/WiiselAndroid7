package julioarellano.wiiselandroid7.parser;

import com.android.wiisel.application.WiiselApplication;
import com.android.wiisel.manager.DataQueueManager;
import com.android.wiisel.manager.PhoneStateManager;

import java.text.SimpleDateFormat;
import java.util.Date;

public class InsoleDataParserFirst {

    private static volatile InsoleDataParserFirst instance;

    public static InsoleDataParserFirst getInstance() {
        InsoleDataParserFirst localInstance = instance;
        if (localInstance == null) {
            synchronized (InsoleDataParserFirst.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new InsoleDataParserFirst();
                }
            }
        }
        return localInstance;
    }

    private int i, gyrInvX, gyrInvY, gyrInvZ, accInvX, accInvY, accInvZ, accStY, accStX, accStZ;
    private int press0, press1, press2, press3, press4, press5, press6, press7, press8, press9, press10, press11, press12, press13;
    private int d[] = new int[20];
    private int timestamp;
    static boolean readDataFlag;
//    public long count = 0;
    public volatile int calibrationMode = 0;
    public volatile boolean isReadWriteData = true;

    // work with serializable data
    private StringBuffer stringBuffer = new StringBuffer();
    int sizeBuffer;
    private String tmp;
    private int maxSizeBuffer = 50;

    public void convertInsoleData(byte[] byteArray) {

        for (i = 0; i < 20; i++) {
            d[i] = ((int) byteArray[i]) & 0x000000FF;
        }

        if (d[0] % 2 == 0) { // check if packet starts with even or odd
                             // counter value

            readDataFlag = true;

            timestamp = d[1] + d[2] * 256 + d[3] * 65536 + d[4] * 16777216;

            // MPU6500 gyro and accelerometer data is 16 bit signed
            gyrInvX = d[6] * 256 + d[5];
            if (gyrInvX >= 0x8000)
                gyrInvX = gyrInvX - 0x10000;
            gyrInvY = d[8] * 256 + d[7];
            if (gyrInvY >= 0x8000)
                gyrInvY = gyrInvY - 0x10000;
            gyrInvZ = d[10] * 256 + d[9];
            if (gyrInvZ >= 0x8000)
                gyrInvZ = gyrInvZ - 0x10000;

            accInvX = d[12] * 256 + d[11];
            if (accInvX >= 0x8000)
                accInvX = accInvX - 0x10000;
            accInvY = d[14] * 256 + d[13];
            if (accInvY >= 0x8000)
                accInvY = accInvY - 0x10000;
            accInvZ = d[16] * 256 + d[15];
            if (accInvZ >= 0x8000)
                accInvZ = accInvZ - 0x10000;

            // LIS3DH data is 16 bit signed of which only the 12 most
            // significant bits are transferred over the
            // air
            accStX = (d[18] << 12 | d[17] << 4) & 0x00fff0;
            if (accStX >= 0x8000)
                accStX = accStX - 0x10000;
            accStY = (d[19] << 8 | d[18]) & 0x00fff0;
            if (accStY >= 0x8000)
                accStY = accStY - 0x10000;

        } else {
            accStZ = (d[2] << 12 | d[1] << 4) & 0x00fff0;
            if (accStZ >= 0x8000)
                accStZ = accStZ - 0x10000;

            // pressure data is 10 bit unsigned

            press0 = ((d[2] & 0xf0) >> 4) + ((d[3] & 0x3f) << 4);
            press1 = ((d[3] & 0xc0) >> 6) + (d[4] << 2);
            press2 = d[5] + ((d[6] & 0x03) << 8);
            press3 = ((d[6] & 0xfc) >> 2) + ((d[7] & 0x0f) << 6);

            press4 = ((d[7] & 0xf0) >> 4) + ((d[8] & 0x3f) << 4);
            press5 = ((d[8] & 0xc0) >> 6) + (d[9] << 2);
            press6 = d[10] + ((d[11] & 0x03) << 8);
            press7 = ((d[11] & 0xfc) >> 2) + ((d[12] & 0x0f) << 6);

            press8 = ((d[12] & 0xf0) >> 4) + ((d[13] & 0x3f) << 4);
            press9 = ((d[13] & 0xc0) >> 6) + (d[14] << 2);
            press10 = d[15] + ((d[16] & 0x03) << 8);
            press11 = ((d[16] & 0xfc) >> 2) + ((d[17] & 0x0f) << 6);

            press12 = ((d[17] & 0xf0) >> 4) + ((d[18] & 0x3f) << 4);
            press13 = ((d[18] & 0xc0) >> 6) + (d[19] << 2);
            if (readDataFlag) {

                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                String deviceName = "Right_" + sdf.format(new Date());

                Long tsLong = System.currentTimeMillis() / 1000;
                String ts = tsLong.toString();

                serializableData(
                    String.valueOf(WiiselApplication.getMode() + "," + timestamp + "," + gyrInvX + "," + gyrInvY + "," + gyrInvZ + ","
                        + accInvX + "," + accInvY + "," + accInvZ + "," + accStX + "," + accStY + "," + accStZ + "," + press0 + ","
                        + press1 + "," + press2 + "," + press3 + "," + press4 + "," + press5 + "," + press6 + "," + press7 + "," + press8
                        + "," + press9 + "," + press10 + "," + press11 + "," + press12 + "," + press13 + "," + ts + "," + calibrationMode
                        + ";\n"), deviceName);

                if (calibrationMode == -1) {
                    calibrationMode = 0;
                }
                
                readDataFlag = false;

                // send accelerometer data to fall detector
//                Intent accIntent = new Intent(AppConstants.ACTION_ACCELEROMETER_PACKETS_FIRST);
//                accIntent.putExtra(AppConstants.EXTRA_DATA_INSOLE_FALL_DETECTION_FIRST,
//                        String.valueOf(accInvX + "," + accInvY + "," + accInvZ));
//                paramContext.sendBroadcast(accIntent);

                int[] press = new int[14];
                press[0] = press0;
                press[1] = press1;
                press[2] = press2;
                press[3] = press3;
                press[4] = press4;
                press[5] = press5;
                press[6] = press6;
                press[7] = press7;
                press[8] = press8;
                press[9] = press9;
                press[10] = press10;
                press[11] = press11;
                press[12] = press12;
                press[13] = press13;

                PhoneStateManager.getInstance().putFirstInsoleData(d[0], gyrInvX, gyrInvY, gyrInvZ, accInvX, accInvY, accInvZ, accStX,
                    accStY, accStZ, press);

                // send intent that updates packet counter and debug view on
                // every 16th sample
//                if ((d[0] & 0x001e) == 0) {
//                    Intent intent = new Intent(AppConstants.ACTION_STATUS_PACKETS_FIRST);
//                    intent.putExtra(AppConstants.EXTRA_STATUS_PACKETS_FIRST, String.valueOf(count));
//                    intent.putExtra(
//                        AppConstants.EXTRA_DATA_INSOLE_FALL_DETECTION_FIRST,
//                        String.valueOf(gyrInvX + "," + gyrInvY + "," + gyrInvZ + "," + accInvX + "," + accInvY + "," + accInvZ + ","
//                            + accStX + "," + accStY + "," + accStZ + "," + press0 + "," + press1 + "," + press2 + "," + press3 + ","
//                            + press4 + "," + press5 + "," + press6 + "," + press7 + "," + press8 + "," + press9 + "," + press10 + ","
//                            + press11 + "," + press12 + "," + press13));
//                    paramContext.sendBroadcast(intent);
//
//                    count = count + 16;
//                }

            }

        }
    }

    public void clearBuffer() {
        if (stringBuffer != null) {
            stringBuffer.setLength(0);
            sizeBuffer = 0;
        }
    }

    // buffer for collection data packets
    private void serializableData(final String str, String devName) {
        if (isReadWriteData) {
            stringBuffer.append(str + "\n");
            sizeBuffer++;
            if (sizeBuffer > maxSizeBuffer) {
                tmp = stringBuffer.toString();
                sizeBuffer = 0;
                stringBuffer.setLength(0);
                new DataQueueManager().writeDataToFile(tmp, devName);
            }
        }
    }

}
