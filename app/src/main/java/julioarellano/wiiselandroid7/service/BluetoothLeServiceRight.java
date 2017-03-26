package julioarellano.wiiselandroid7.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import julioarellano.wiiselandroid7.constants.AppConstants;
import julioarellano.wiiselandroid7.parser.InsoleDataParserFirst;

import java.util.List;

public class BluetoothLeServiceRight extends Service {
    static final String TAG = "BluetoothLeServiceRight";

    public final static String ACTION_GATT_CONNECTED = "com.android.wiisel.service.right.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.android.wiisel.service.right.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.android.wiisel.service.right.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_READ = "com.android.wiisel.service.right.ACTION_DATA_READ";
//    public final static String ACTION_DATA_NOTIFY = "com.android.wiisel.service.right.ACTION_DATA_NOTIFY";
    public final static String ACTION_DATA_WRITE = "com.android.wiisel.service.right.ACTION_DATA_WRITE";
    public final static String ACTION_RSSI_READ = "com.android.wiisel.service.right.ACTION_RSSI_READ";
    public final static String EXTRA_DATA = "com.android.wiisel.service.right.EXTRA_DATA";
    public final static String EXTRA_INT = "com.android.wiisel.service.right.EXTRA_INT";
    public final static String EXTRA_UUID = "com.android.wiisel.service.right.EXTRA_UUID";
    public final static String EXTRA_STATUS = "com.android.wiisel.service.right.EXTRA_STATUS";
    public final static String EXTRA_ADDRESS = "com.android.wiisel.service.right.EXTRA_ADDRESS";

    // BLE
    private BluetoothManager mBluetoothManager = null;
    private BluetoothAdapter mBtAdapter = null;
    private BluetoothGatt mBluetoothGatt = null;
    private static BluetoothLeServiceRight mThis = null;
    private volatile boolean mBusy = false; // Write/read pending response
    private String mBluetoothDeviceAddress;
    public boolean isConected = false;

    /**
     * GATT client callbacks
     */
    private BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (mBluetoothGatt == null) {
                Log.e(TAG, "mBluetoothGatt not created!");
                return;
            }

            BluetoothDevice device = gatt.getDevice();
            String address = device.getAddress();
            Log.d(TAG, "onConnectionStateChange (" + address + ") " + newState + " status: " + status);

            try {
                switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    broadcastUpdate(ACTION_GATT_CONNECTED, address, status);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    broadcastUpdate(ACTION_GATT_DISCONNECTED, address, status);
                    break;
                default:
                    Log.e(TAG, "New state not processed: " + newState);
                    break;
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothDevice device = gatt.getDevice();
            broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, device.getAddress(), status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            broadcastUpdate(ACTION_RSSI_READ, rssi, status);

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            InsoleDataParserFirst.getInstance().convertInsoleData(characteristic.getValue());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            /*byte  [] value = characteristic.getValue();
            Integer lowerByte = (int) value[0] & 0xFF; 
            Integer upperByte = (int) value[1] & 0xFF;
            Integer qaz=(upperByte << 8) + lowerByte;
            Log.i(TAG, "char = " + qaz);*/

            byte[] value = characteristic.getValue();

            Log.e(TAG, value[0] + "");

            broadcastUpdate(ACTION_DATA_READ, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            broadcastUpdate(ACTION_DATA_WRITE, characteristic, status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            mBusy = false;
            Log.i(TAG, "onDescriptorRead");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            mBusy = false;
            Log.i(TAG, "onDescriptorWrite");
        }
    };

    private void broadcastUpdate(final String action, final String address, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_ADDRESS, address);
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
        mBusy = false;
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
        intent.putExtra(EXTRA_DATA, characteristic.getValue());
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
        mBusy = false;
    }

    private void broadcastUpdate(final String action, final int value, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_INT, value);
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
        mBusy = false;
    }

    private boolean checkGatt() {
        if (mBtAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            stopSelf(startId2);
            return false;
        }
        if (mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            stopSelf(startId2);
            return false;
        }

        if (mBusy) {
            Log.w(TAG, "LeService busy");
            return false;
        }
        return true;

    }

    /**
     * Manage the BLE service
     */
    public class LocalBinder extends Binder {
        public BluetoothLeServiceRight getService() {
            return BluetoothLeServiceRight.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that
        // BluetoothGatt.close() is called
        // such that resources are cleaned up properly. In this particular example,
        // close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder binder = new LocalBinder();

    private int startId2;

    /**
     * Initializes a reference to the local Bluetooth adapter.
     * 
     * @return Return true if the initialization is successful.
     */
    public boolean initialize(BluetoothManager manager) {

        Log.d(TAG, "initialize");
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        mThis = this;
        if (mBluetoothManager == null) {
            mBluetoothManager = manager;// (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBtAdapter = mBluetoothManager.getAdapter();
        // mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startId2 = startId;
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    //
    // GATT API
    //
    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported asynchronously through
     * the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     * 
     * @param characteristic
     *            The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (!checkGatt())
            return;
        mBusy = true;
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void readRssi() {
        if (!checkGatt())
            return;
        mBusy = true;
        mBluetoothGatt.readRemoteRssi();
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] b) {
        if (!checkGatt())
            return false;

        // byte[] val = new byte[1];
        // val[0] = b;
        characteristic.setValue(b);

        mBusy = true;
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, boolean b) {
        if (!checkGatt())
            return false;

        byte[] val = new byte[1];

        val[0] = (byte) (b ? 1 : 0);
        characteristic.setValue(val);
        mBusy = true;
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (!checkGatt())
            return false;

        mBusy = true;
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public boolean discoverServices() {
        if (mBluetoothGatt == null) {
            return false;
        }
        if (mBluetoothGatt.discoverServices()) {
            Log.i(TAG, "START SERVICE DISCOVERY");
//		      mServiceList.clear();
//		      setStatus("Service discovery started");
            return true;
        } else {
//			  setError("Service discovery start failed");
            Log.i(TAG, "Service discovery start failed");
            return false;
        }
    }

    /**
     * Retrieves the number of GATT services on the connected device. This should be invoked only after
     * {@code BluetoothGatt#discoverServices()} completes successfully.
     * 
     * @return A {@code integer} number of supported services.
     */
    public int getNumServices() {
        if (mBluetoothGatt == null)
            return 0;
        return mBluetoothGatt.getServices().size();
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be invoked only after
     * {@code BluetoothGatt#discoverServices()} completes successfully.
     * 
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;
        return mBluetoothGatt.getServices();
    }

    /**
     * Enables or disables notification on a give characteristic.
     * 
     * @param characteristic
     *            Characteristic to act on.
     * @param enabled
     *            If true, enable notification. False otherwise.
     */
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
        if (!checkGatt())
            return false;

        if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            Log.w(TAG, "setCharacteristicNotification failed");
            return false;
        }

//	    BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(AppConstants.CLIENT_CHARACTERISTIC_CONFIG);
        if (clientConfig == null)
            return false;

        if (enable) {
            Log.i(TAG, "enable notification");
            clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            Log.i(TAG, "disable notification");
            clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }

        mBusy = true;
        return mBluetoothGatt.writeDescriptor(clientConfig);
    }

    public boolean isNotificationEnabled(BluetoothGattCharacteristic characteristic) {
        if (!checkGatt())
            return false;

        // BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
        BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(AppConstants.CLIENT_CHARACTERISTIC_CONFIG);
        if (clientConfig == null)
            return false;

        return clientConfig.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     * 
     * @param address
     *            The device address of the destination device.
     * 
     * @return Return true if the connection is initiated successfully. The connection result is reported asynchronously
     *         through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        isConected = true;
        if (mBtAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        final BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
        int connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

        if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {

            // Previously connected device. Try to reconnect.
            if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
                Log.d(TAG, "Re-use GATT connection");
                if (mBluetoothGatt.connect()) {
                    return true;
                } else {
                    Log.w(TAG, "GATT re-connect failed.");
                    return false;
                }
            }

            if (device == null) {
                Log.w(TAG, "Device not found.  Unable to connect.");
                return false;
            }
            // We want to directly connect to the device, so we are setting the
            // autoConnect parameter to false.
            Log.d(TAG, "Create a new GATT connection.");
            mBluetoothGatt = device.connectGatt(this, false, mGattCallbacks);
            mBluetoothDeviceAddress = address;
        } else {
            Log.w(TAG, "Attempt to connect in state: " + connectionState);
            return false;
        }
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result is reported
     * asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)} callback.
     */
    public void disconnect(String address) {
        isConected = false;
        if (mBtAdapter == null) {
            Log.w(TAG, "disconnect: BluetoothAdapter not initialized");
            return;
        }
        final BluetoothDevice device = mBtAdapter.getRemoteDevice(address);
        int connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

        if (mBluetoothGatt != null) {
            Log.i(TAG, "disconnect");
            if (connectionState != BluetoothProfile.STATE_DISCONNECTED) {
                mBluetoothGatt.disconnect();
            } else {
                Log.w(TAG, "Attempt to disconnect in state: " + connectionState);
            }
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are released properly.
     */
    public void close() {
        if (mBluetoothGatt != null) {
            Log.i(TAG, "close");
            mBluetoothGatt.close();
            mBluetoothGatt = null;
            stopSelf(startId2);
        }
    }

    public int numConnectedDevices() {
        int n = 0;

        if (mBluetoothGatt != null) {
            List<BluetoothDevice> devList;
            devList = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
            n = devList.size();
        }
        return n;
    }

    //
    // Utility functions
    //
    public static BluetoothGatt getBtGatt() {
        if (mThis == null)
            return null;

        return mThis.mBluetoothGatt;
    }

    public static BluetoothManager getBtManager() {
        return mThis.mBluetoothManager;
    }

    public static BluetoothLeServiceRight getInstance() {
        return mThis;
    }

    public boolean waitIdle(int i) {
        i /= 10;
        while (--i > 0) {
            if (mBusy)
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            else
                break;
        }

        return i > 0;
    }

}
