package julioarellano.wiiselandroid7.utils;

import julioarellano.wiiselandroid7.parser.InsoleDataParserFirst;
import julioarellano.wiiselandroid7.parser.InsoleDataParserSecond;
import julioarellano.wiiselandroid7.service.BluetoothLeServiceLeft;
import julioarellano.wiiselandroid7.service.BluetoothLeServiceRight;

public class InsolesUtil {

    private InsolesUtil() {
    }

    /**
     * @param i
     *            - stage of calibration mode. 0 - none , 1 - 6 stage of mode
     */
    public static void setCalibrationMode(int i) {
//        DataReceiveFromCallbackFirst.calibrationMode = i;
        InsoleDataParserFirst.getInstance().calibrationMode = i;
        InsoleDataParserSecond.getInstance().calibrationMode = i;
    }

    /**
     * @param can
     *            If true then writing to file is allowed
     */
    public static void setWriteDataToFilePermission(boolean can) {
        InsoleDataParserFirst.getInstance().isReadWriteData = can;
        InsoleDataParserSecond.getInstance().isReadWriteData = can;
    }

    /**
     * Clear buffer before next step
     */
    public static void clearBuffer() {
        InsoleDataParserFirst.getInstance().clearBuffer();
        InsoleDataParserSecond.getInstance().clearBuffer();
    }

    /**
     * 
     * @return true of false. if true than both insoles connected
     */
    public static boolean isBothInsolesConected() {
        if ((BluetoothLeServiceRight.getBtGatt() != null) && (BluetoothLeServiceLeft.getBtGatt() != null)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 
     * @return true of false. if true than both insoles disconnected
     */
    public static boolean isBothInsolesDisconected() {
        if ((BluetoothLeServiceRight.getBtGatt() == null) && (BluetoothLeServiceLeft.getBtGatt() == null)) {
            return true;
        } else {
            return false;
        }
    }

}
