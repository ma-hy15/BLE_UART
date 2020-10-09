package com.example.myfirstapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import cc.noharry.blelib.ble.BleAdmin;
import cc.noharry.blelib.ble.connect.MtuTask;
import cc.noharry.blelib.ble.connect.ReadTask;
import cc.noharry.blelib.ble.connect.Task;
import cc.noharry.blelib.ble.connect.WriteTask;
import cc.noharry.blelib.ble.scan.BleScanConfig;
import cc.noharry.blelib.ble.scan.BleScanner;
import cc.noharry.blelib.callback.BleConnectCallback;
import cc.noharry.blelib.callback.BleScanCallback;
import cc.noharry.blelib.callback.DataChangeCallback;
import cc.noharry.blelib.callback.MtuCallback;
import cc.noharry.blelib.callback.ReadCallback;
import cc.noharry.blelib.callback.WriteCallback;
import cc.noharry.blelib.data.BleDevice;
import cc.noharry.blelib.data.Data;
import cc.noharry.blelib.data.WriteData;
import cc.noharry.blelib.util.L;


public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";

    /** Nordic Blinky Service UUID. */
    public final static UUID LBS_UUID_SERVICE = UUID.fromString("00001523-1212-efde-1523-785feabcd123");
    /** BUTTON characteristic UUID. */
    private final static UUID LBS_UUID_BUTTON_CHAR = UUID.fromString("00001524-1212-efde-1523-785feabcd123");
    /** LED characteristic UUID. */
    private final static UUID LBS_UUID_LED_CHAR = UUID.fromString("00001525-1212-efde-1523-785feabcd123");

    /** Nordic UART Service UUID. */
    public final static UUID NUS_UUID_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    /** RX characteristic UUID. */
    private final static UUID NUS_UUID_TX_CHAR = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    /** TX characteristic UUID. */
    private final static UUID NUS_UUID_RX_CHAR = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    private final static int MTU_SIZE = 247;
    private final static String DEVICE_NAME = "Nordic_UART";
    private BluetoothGattCharacteristic buttonCharacteristic, ledCharacteristic;
    private BluetoothGattCharacteristic rxCharacteristic,txCharacteristic;

    public String FILE_SAVE_PATH = Objects.requireNonNull(this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)).getAbsolutePath();

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int PERMISSION_REQUEST_ID=3;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    private BluetoothAdapter mBluetoothAdapter;
    public BleDevice myBleDevice;

    private static final int REQUEST_ENABLE_BT = 2;
    public boolean isFuzzy = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sdPermissionRequest();
        if (this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) == null) {
            FILE_SAVE_PATH = "No Path";
        }
        else {
            FILE_SAVE_PATH = Objects.requireNonNull(this.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)).getAbsolutePath();
        }
        locationPermissionRequest();

        initBle();
        // 一定要扫描到了才能开始连接，因此连接代码不能放到这里
    }

    public void sdPermissionRequest(){
        //一进入程序就向用户申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//android 6.0以上
            //进入程序，申请读写权限
            int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        }
    }

    public void locationPermissionRequest() {
        //获取位置权限，不获取就扫不到任何东西
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_REQUEST_ID);
    }

    public void initBle() {
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // 扫描设置
        //String Name="Nordic_UART";
        BleScanConfig scanConfig = new BleScanConfig.Builder()
                //.setDeviceMac(new String[]{"F2:F4:FC:5F:DF:0E"})
                .setDeviceName(new String[]{DEVICE_NAME},isFuzzy)
                .setScanTime(5000)
                .build();
        // 扫描结果回调
        BleScanCallback mBleScanCallback = new BleScanCallback() {
            @Override
            public void onScanStarted(boolean isStartSuccess) {
                // 加入扫描启动成功的处理代码
                L.i("Scan Started");
            }

            @Override
            public void onFoundDevice(BleDevice bleDevice) {
                L.i("FoundDevice:"+bleDevice.getName());
            }

            @Override
            public void onScanCompleted(List<BleDevice> deviceList) {
                //扫描完成
                //会在扫描时间结束或者主动调用stopScan()时发生回调
                //回调出的设备为该过程中扫描到的所有设备，已过滤重复设备
                L.i("Scan Completed");
                if(deviceList!=null) {
//                    for(int index=0;index<deviceList.size();index++){
//                        L.i("Device"+index+":"+deviceList.get(index).getName());
//                        if(deviceList.get(index).getName()=="Nordic_Blinky")
//                            myBleDevice = deviceList.get(index);
//                    }
                    myBleDevice = deviceList.get(0);
                    L.i("STOP TO FIND IT");
                }
                if(myBleDevice!=null){
                    connectBle();
                }
            }
        };
        // 开始扫描
        BleAdmin
                .getINSTANCE(getApplication())
                .scan(scanConfig,mBleScanCallback);
    }

    public void connectBle() {
        // 连接回调
        BleConnectCallback mBleConnectCallback = new BleConnectCallback() {
            @Override
            public void onDeviceConnecting(BleDevice bleDevice) {
                //开始连接
                L.i("Start Connecting");
            }

            @Override
            public void onDeviceConnected(BleDevice bleDevice) {
                //连接成功
                L.i("Connect Success");
            }

            @Override
            public void onServicesDiscovered(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                //发现服务,由onServicesDiscoveredBase回调出的新线程
                if(bleDevice!=null) {
                    //L.i("Service Discovered:"+gatt.getService(NUS_UUID_SERVICE).toString());
                }
                //发现服务后再开始操作
                if(myBleDevice!=null){
                    bleDataHandle();
                }
            }

            @Override
            public void onDeviceDisconnecting(BleDevice bleDevice) {
                //正在断开连接
                L.i("Disconnecting with "+bleDevice.getName());
            }

            @Override
            public void onDeviceDisconnected(BleDevice bleDevice, int status) {
                //断开连接
                L.i("Disconnected with "+bleDevice.getName());
                myBleDevice = null;
            }

            @Override
            public void onServicesDiscoveredBase(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                L.i("Service Discovered");
//                final BluetoothGattService service = gatt.getService(LBS_UUID_SERVICE);
//                if (service != null) {
//                    buttonCharacteristic = service.getCharacteristic(LBS_UUID_BUTTON_CHAR);
//                    ledCharacteristic = service.getCharacteristic(LBS_UUID_LED_CHAR);
//                }
                final BluetoothGattService service = gatt.getService(NUS_UUID_SERVICE);
                if (service != null) {
                    rxCharacteristic = service.getCharacteristic(NUS_UUID_RX_CHAR);
                    txCharacteristic = service.getCharacteristic(NUS_UUID_TX_CHAR);
                }
                runOnUiThread(()->onServicesDiscovered(bleDevice, gatt, status));
            }
        };
        //开始连接
        BleAdmin
                .getINSTANCE(getApplication())
                .connect(myBleDevice,false,mBleConnectCallback,5000);
    }

    public void bleDataHandle() {

        //更改MTU大小为247
        MtuCallback mMtuCallback = new MtuCallback() {
            @Override
            public void onMtuChanged(BleDevice bleDevice, int mtu) {
                // MTU修改成功会回调此方法
                // 单片机修改MTU会进入这里吗
                L.i("MTU changed to:"+mtu);
            }

            @Override
            public void onOperationSuccess(BleDevice bleDevice) {
                L.i("MTU OPERATION SUCCESS");
            }

            @Override
            public void onFail(BleDevice bleDevice, int statuCode, String message) {
                L.i("MTU OPERATION FAILED");
            }

            @Override
            public void onComplete(BleDevice bleDevice) {
                L.i("MTU OPERATION COMPLETED");
                enableNusRx();
                //NusTx("123456".getBytes());
            }
        };
        MtuTask mtuTask = Task.newMtuTask(myBleDevice,MTU_SIZE).with(mMtuCallback);
        BleAdmin.getINSTANCE(getApplication()).addTask(mtuTask);
    }

    public void enableNusRx(){
        //打开通知
        DataChangeCallback mDataChangeCallback = new DataChangeCallback() {
            @Override
            public void onDataChange(BleDevice bleDevice, Data data) {
                // 收到通知
                L.i("Receive ADC Data:"+data.toString());
            }

            @Override
            public void onOperationSuccess(BleDevice bleDevice) {
                // 操作成功
                L.i("ENABLE RX NOTIFICATION SUCCESS");
            }

            @Override
            public void onFail(BleDevice bleDevice, int statuCode, String message) {
                // 启动失败
                L.i("ENABLE RX NOTIFICATION FAILED");
            }

            @Override
            public void onComplete(BleDevice bleDevice) {
                // 操作完成
                L.i("ENABLE RX NOTIFICATION COMPLETED");
            }
        };
        WriteTask enableTask = Task
                .newEnableNotificationsTask(myBleDevice, rxCharacteristic)
                .with(mDataChangeCallback);
        BleAdmin.getINSTANCE(getApplication()).addTask(enableTask);
    }

    public void NusTx(byte[] data){
        //串口数据发送
        WriteCallback mWriteCallback = new WriteCallback() {
            @Override
            public void onDataSent(BleDevice bleDevice, Data data, int totalPackSize, int remainPackSize) {
                //bleDevice:目标设备
                //data:写入的数据
                //totalPackSize：本次任务发送的总包数
                //remainPackSize:当前剩余的包数
                L.i("DATA SENT:"+data.toString());
            }

            @Override
            public void onOperationSuccess(BleDevice bleDevice) {
                L.i("TX SUCCESS");
            }

            @Override
            public void onFail(BleDevice bleDevice, int statuCode, String message) {
                L.i("TX FAILED");
            }

            @Override
            public void onComplete(BleDevice bleDevice) {
                L.i("TX COMPLETED");
            }
        };
        WriteData writeData= new WriteData();
        writeData.setValue(data);
        WriteTask writeTask= Task.newWriteTask(myBleDevice,txCharacteristic,writeData).with(mWriteCallback);
        BleAdmin.getINSTANCE(getApplication()).addTask(writeTask);
    }

    public void readLED() {
        // 读取LED特征
        ReadCallback mReadCallback = new ReadCallback() {
            @Override
            public void onDataRecived(BleDevice bleDevice, Data data) {
                //读到的数据
                L.i("LED READ DATA:"+data.toString());
                AdcDataManager.saveToSd(FILE_SAVE_PATH,data.toString());
            }

            @Override
            public void onOperationSuccess(BleDevice bleDevice) {
                //操作成功
                L.i("LED READ SUCCESS");
            }

            @Override
            public void onFail(BleDevice bleDevice, int statuCode, String message) {
                //失败回调
                L.i("LED READ FAILED");
            }

            @Override
            public void onComplete(BleDevice bleDevice) {
                //完成回调
                L.i("LED READ COMPLETED");
            }
        };
        ReadTask readTask = Task.newReadTask(myBleDevice,ledCharacteristic).with(mReadCallback);
        BleAdmin.getINSTANCE(getApplication()).addTask(readTask);
    }

    //  byte[] data = {0x01};
    public void writeLED(byte[] data){
        //写入LED特征
        WriteCallback mWriteCallback = new WriteCallback() {
            @Override
            public void onDataSent(BleDevice bleDevice, Data data, int totalPackSize, int remainPackSize) {
                //bleDevice:目标设备
                //data:写入的数据
                //totalPackSize：本次任务发送的总包数
                //remainPackSize:当前剩余的包数
                L.i("DATA SENT:"+data.toString());
            }

            @Override
            public void onOperationSuccess(BleDevice bleDevice) {
                L.i("LED WRITE SUCCESS");
            }

            @Override
            public void onFail(BleDevice bleDevice, int statuCode, String message) {
                L.i("LED WRITE FAILED");
            }

            @Override
            public void onComplete(BleDevice bleDevice) {
                L.i("LED WRITE COMPLETED");
            }
        };
        WriteData writeData= new WriteData();
        writeData.setValue(data);
        WriteTask writeTask= Task.newWriteTask(myBleDevice,ledCharacteristic,writeData).with(mWriteCallback);
        BleAdmin.getINSTANCE(getApplication()).addTask(writeTask);
    }

    /** Called when the user taps the Send button */
    public void sendMessage(View view) {
        // Do something in response to button
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        if(myBleDevice!=null){
            NusTx(message.getBytes());
        }
//        Intent intent = new Intent(this,DisplayMessageActivity.class);
//        EditText editText = (EditText) findViewById(R.id.editText);
//        String message = editText.getText().toString();
//        intent.putExtra(EXTRA_MESSAGE,message);
//        startActivity(intent);
    }

    /** Called when the user taps the Send buttonconnect */
    public void disconnectBle(View view) {
        // Do something in response to button
        if(myBleDevice!=null) {
            BleAdmin.getINSTANCE(getApplication()).disconnect(myBleDevice);
        }
    }
}
