package khs.bleexampletruiton;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

@TargetApi(21)
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    // Target > 21
//    private BluetoothLeScanner mLEScanner;
//    private ScanSettings settings;
//    private List<ScanFilter> filters;
    private BluetoothGatt mConnectedGatt;

    // SylvacRead1 member vars
    private String mLastWrite = "";

    // queue read/write requests
    private Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<BluetoothGattDescriptor>();
    private Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<BluetoothGattCharacteristic>();
    // TODO use queue of characteristic and the value written so that this can be correctly returned instead of mLastWrite
    private Queue<BluetoothGattCharacteristic> characteristicWriteQueue = new LinkedList<BluetoothGattCharacteristic>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            // Target > 21
//            if (Build.VERSION.SDK_INT >= 21) {
//                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
//                settings = new ScanSettings.Builder()
//                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//                        .build();
//                filters = new ArrayList<ScanFilter>();
//            }
            scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (mConnectedGatt == null) {
            return;
        }
        mConnectedGatt.close();
        mConnectedGatt = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                    // Target > 21
//                    } else {
//                        mLEScanner.stopScan(mScanCallback);
//
//                    }
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }
            // Target > 21
//            } else {
//                mLEScanner.startScan(filters, settings, mScanCallback);
//            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
            // Target > 21
//            } else {
//                mLEScanner.stopScan(mScanCallback);
//            }
        }
    }

    // Target > 21
//    private ScanCallback mScanCallback = new ScanCallback() {
//        @Override
//        public void onScanResult(int callbackType, ScanResult result) {
//            Log.i("callbackType", String.valueOf(callbackType));
//            Log.i("result", result.toString());
//            BluetoothDevice btDevice = result.getDevice();
//            connectToDevice(btDevice);
//        }
//
//        @Override
//        public void onBatchScanResults(List<ScanResult> results) {
//            for (ScanResult sr : results) {
//                Log.i("ScanResult - Results", sr.toString());
//            }
//        }
//
//        @Override
//        public void onScanFailed(int errorCode) {
//            Log.e("Scan Failed", "Error Code: " + errorCode);
//        }
//    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("onLeScan", device.toString());
                            connectToDevice(device);
                        }
                    });
                }
            };

    public void connectToDevice(BluetoothDevice device) {
        if (mConnectedGatt == null) {
            mConnectedGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status + "; newState: " + newState);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            // TODO call display services here
            // gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onServicesDiscovered GATT_SUCCESS: " + status);
                Log.d(TAG, "onServicesDiscovered Services = " + gatt.getServices());
                displayGattServices(mConnectedGatt.getServices());
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        // TODO old Truiton code
//        @Override
//        public void onCharacteristicRead(BluetoothGatt gatt,
//                                         BluetoothGattCharacteristic
//                                                 characteristic, int status) {
//            Log.i("onCharacteristicRead", characteristic.toString());
//            gatt.disconnect();
//        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onCharacteristicRead GATT_SUCCESS: " + characteristic);
                Log.d(TAG, "onCharacteristicRead UUID = " + characteristic.getUuid());

                // For all other profiles, writes the data formatted in HEX.
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for(byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));

                    Log.d(TAG, "onCharacteristicRead value = " + new String(data) + "\n" + stringBuilder.toString());
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            Log.d(TAG, "onCharacteristicChanged char = : " + characteristic);
            Log.d(TAG, "onCharacteristicChanged UUID = " + characteristic.getUuid());

            if (characteristic.getUuid().equals(UUID.fromString(SylvacGattAttributes.ANSWER_TO_REQUEST_OR_CMD_FROM_INSTRUMENT))) {
                Log.d(TAG, "onCharacteristicChanged last write = " + mLastWrite);
                Log.d(TAG, "onCharacteristicChanged getValue() = " + new String(characteristic.getValue()));
            }

            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            Log.d(TAG, "onCharacteristicChanged value = " + new String(data) + "\n" + byteArrayToString(data));

            /*
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));

                Log.d(TAG, "onCharacteristicChanged value = " + new String(data) + "\n" + stringBuilder.toString());
            }
            */
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onCharacteristicWrite GATT_SUCCESS: " + status);
                Log.d(TAG, "onCharacteristicWrite UUID = " + characteristic.getUuid());
                Log.i(TAG, "onCharacteristicWrite Char Value = " + characteristic.getValue().toString());
            } else {
                Log.w(TAG, "onCharacteristicWrite received: " + status);
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.d(TAG, "onReliableWriteCompleted(" + status + ")");
        }
    };

    // SylvacRead1 code is below
    // Demonstrates how to iterate through the supported GATT
    // Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the
    // ExpandableListView on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        // clear BLE command queues
        descriptorWriteQueue.clear();
        characteristicReadQueue.clear();
        characteristicWriteQueue.clear();

        for (BluetoothGattService service : gattServices) {
            Log.d(TAG, "Found service: " + service.getUuid());
            Log.d(TAG, "Included service(s): " + service.getIncludedServices());

            // skip if not Sylvac Metrology service
            if (!service.getUuid().equals(UUID.fromString(SylvacGattAttributes.SYLVAC_METROLOGY_SERVICE))) {
                Log.d(TAG, "Skip service: " + service.getUuid());
                continue;
            }

            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                Log.d(TAG, "Found characteristic: " + characteristic.getUuid());
                Log.d(TAG, "Descriptor: " + characteristic.getDescriptors());
                Log.d(TAG, "Properties: " + characteristic.getProperties());

                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                    Log.d(TAG, "Found descriptor: " + descriptor.getUuid());
                    Log.d(TAG, "Value: " + descriptor.getValue());
                    Log.d(TAG, "Permissions: " + descriptor.getPermissions());
                }

                if(hasProperty(characteristic,
                        BluetoothGattCharacteristic.PROPERTY_READ)) {
                    Log.d(TAG, "Found Read characteristic: " + characteristic.getUuid());
                    // TODO before queue - remove later
                    // mConnectedGatt.readCharacteristic(characteristic);
                    // TODO read here not required - remove later
                    // readCharacteristic(characteristic);
                }

                if(hasProperty(characteristic,
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) {
                    Log.d(TAG, "Found Write No Resp characteristic: " + characteristic.getUuid());
                }

                if(hasProperty(characteristic,
                        BluetoothGattCharacteristic.PROPERTY_INDICATE)) {
                    Log.d(TAG, "Found indication for characteristic: " + characteristic.getUuid());

                    // enable indication on the Sylvac data received (from instrument) characteristic only
                    if(characteristic.getUuid().equals(UUID.fromString(SylvacGattAttributes.DATA_RECEIVED_FROM_INSTRUMENT))) {
                        Log.d(TAG, "Register indication for characteristic: " + characteristic.getUuid());
                        Log.d(TAG, "Register Success = " + mConnectedGatt.setCharacteristicNotification(characteristic, true));

                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                UUID.fromString(SylvacGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

                        // TODO before queue - remove later
                        // mConnectedGatt.writeDescriptor(descriptor);
                        writeGattDescriptor(descriptor);
                    }
                }

                if(hasProperty(characteristic,
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY)) {
                    Log.d(TAG, "Found notification for characteristic: " + characteristic.getUuid());

                    // enable notify on the Sylvac answer to request or cmd (from instrument) characteristic only
                    if(characteristic.getUuid().equals(UUID.fromString(SylvacGattAttributes.ANSWER_TO_REQUEST_OR_CMD_FROM_INSTRUMENT))) {
                        Log.d(TAG, "Register notification for characteristic: " + characteristic.getUuid());
                        Log.d(TAG, "Register Success = " + mConnectedGatt.setCharacteristicNotification(characteristic, true));

                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                UUID.fromString(SylvacGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        // descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

                        // TODO before queue - remove later
                        // mConnectedGatt.writeDescriptor(descriptor);
                        writeGattDescriptor(descriptor);
                    }
                }
            }
        }
    }

    public static boolean hasProperty(BluetoothGattCharacteristic
                                              characteristic, int property) {
        int prop = characteristic.getProperties() & property;
        return prop == property;
    }

    // dequeue next BLE command
    private boolean dequeueBleCommand() {

        // handle asynchronous BLE callbacks via queues
        // GIVE PRECEDENCE to descriptor writes.  They must all finish first?
        if (descriptorWriteQueue.size() > 0) {
            return mConnectedGatt.writeDescriptor(descriptorWriteQueue.element());
        } else if (characteristicReadQueue.size() > 0) {
            return mConnectedGatt.readCharacteristic(characteristicReadQueue.element());
        } else if (characteristicWriteQueue.size() > 0) {
            return mConnectedGatt.writeCharacteristic(characteristicWriteQueue.element());
        } else {
            return true;
        }
    }

    // queue Gatt Descriptor writes
    private boolean writeGattDescriptor(BluetoothGattDescriptor d){
        boolean success = false;

        // check Bluetooth GATT connected
        if (mConnectedGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }

        //put the descriptor into the write queue
        success = descriptorWriteQueue.add(d);

        // execute BLE command immediately if there is nothing else queued up
        if((descriptorWriteQueue.size() == 1) && (characteristicReadQueue.size() == 0) && (characteristicWriteQueue.size() == 0)) {
            return mConnectedGatt.writeDescriptor(d);
        } else {
            return success;
        }
    }

    // queue BLE characteristic writes
    private boolean writeCharacteristic(BluetoothGattCharacteristic c) {
        boolean success = false;

        // check Bluetooth GATT connected
        if (mConnectedGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }

        // BluetoothGattService s = mBluetoothGatt.getService(UUID.fromString(kYourServiceUUIDString));
        // BluetoothGattCharacteristic c = s.getCharacteristic(UUID.fromString(characteristicName));

        //put the characteristic into the read queue
        success = characteristicWriteQueue.add(c);

        // execute BLE command immediately if there is nothing else queued up
        if((descriptorWriteQueue.size() == 0) && (characteristicReadQueue.size() == 0) && (characteristicWriteQueue.size() == 1)) {
            return mConnectedGatt.writeCharacteristic(c);
        }
        else {
            return success;
        }
    }

    public boolean writeCharacteristic(String value) {

        // check Bluetooth GATT connected
        if (mConnectedGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }

        /*
        // check write is allowed
        if (mCanWrite == false) {
            Log.e(TAG, "write not allowed");
            return false;
        }
        */

        // extract the Service
        BluetoothGattService gattService = mConnectedGatt.getService(UUID.fromString(SylvacGattAttributes.SYLVAC_METROLOGY_SERVICE));
        if (gattService == null) {
            Log.e(TAG, "service not found");
            return false;
        }

        // extract the Characteristic
        BluetoothGattCharacteristic gattChar = gattService.getCharacteristic(UUID.fromString(SylvacGattAttributes.DATA_REQUEST_OR_CMD_TO_INSTRUMENT));
        if (gattChar == null) {
            Log.e(TAG, "characteristic not found");
            return false;
        }

        // set the Characteristic
        if (gattChar.setValue(value) == false) {
            Log.e(TAG, "characteristic set failed");
            return false;
        }

        // write the Characteristic
        if (mConnectedGatt.writeCharacteristic(gattChar) == false) {
            Log.e(TAG, "characteristic write failed");
            return false;
        }

        mLastWrite = value;
        // mCanWrite = false;
        return true;
    }

    public boolean readCharacteristic() {
        // check Bluetooth GATT connected
        if (mConnectedGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }

        // extract the Service
        BluetoothGattService gattService = mConnectedGatt.getService(UUID.fromString(SylvacGattAttributes.SYLVAC_METROLOGY_SERVICE));
        if (gattService == null) {
            Log.e(TAG, "service not found");
            return false;
        }

        // extract the Characteristic
        BluetoothGattCharacteristic gattChar = gattService.getCharacteristic(UUID.fromString(SylvacGattAttributes.ANSWER_TO_REQUEST_OR_CMD_FROM_INSTRUMENT));
        // BluetoothGattCharacteristic gattChar = gattService.getCharacteristic(uuidChar);
        if (gattChar == null) {
            Log.e(TAG, "characteristic not found");
            return false;
        }

        // read the Characteristic
        return mConnectedGatt.readCharacteristic(gattChar);
    }

    public String byteArrayToString(byte[] data) {
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for(byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));

            return stringBuilder.toString();
        }
        else {
            return "";
        }
    }

}
