package com.example.frank.main;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.frank.main.bar.CircleProgressBar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

//@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {
    private SharedPreferences sp;

    private LeDeviceListAdapter mLeDeviceListAdapter;
    //private ListView listView;
    private List<Map<String, Object>> listItems;

    private final static String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 8000;

    private TextView mDataField = null, numDevice = null;
//    private EditText edtIP = null, edtPort = null;
    private ScrollView svResult = null;
    private Button btnDevice = null;
    private CircleProgressBar mCustomProgressBar1;

    private BluetoothLeService mBluetoothLeService = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private boolean mScanning;
    private String deviceText = null;
    private LinkedList<BluetoothDevice> mDeviceContainer = new LinkedList<BluetoothDevice>();  //所有搜索到的设备
    private LinkedList<BluetoothDevice> mDeviceConnectable = new LinkedList<BluetoothDevice>();  //可连接的设备
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();  //已连接的设备

    CtrolThread ctrolThread = new CtrolThread();
    CtrolThread1 ctrolThread1 = new CtrolThread1();
    CtrolThread ctrolThread2 = new CtrolThread();
    CtrolThread ctrolThread3 = new CtrolThread();
    CtrolThread ctrolThread4 = new CtrolThread();

    Thread update = null;
    Thread update1 = null;
    Thread update2 = null;
    Thread update3 = null;
    Thread update4 = null;

    TextView updatename = null,updatename1 = null,updatename2 = null,updatename3 = null,updatename4 = null;
    TextView update_info = null,update_info1 = null,update_info2 = null,update_info3 = null,update_info4 = null;
    CircleProgressBar mProgressBar =null,mProgressBar1 =null,mProgressBar2 =null,mProgressBar3 =null,mProgressBar4 =null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services);
        deviceText = getString(R.string.device_number);
        iniUI();
        iniBle();

        //获得实例对象
        sp = this.getSharedPreferences("fileInfo", Context.MODE_WORLD_READABLE);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        Log.d(TAG, "Try to bindService=" + bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE));
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        mCustomProgressBar1 = (CircleProgressBar) findViewById(R.id.custom_progress1);

        //listView = (ListView)findViewById(R.id.list_goods);
        mLeDeviceListAdapter = new LeDeviceListAdapter(); //创建适配器
        //listView.setAdapter(mLeDeviceListAdapter);

//        //设置 item 的监听事件
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                //获得 item 里面的文本控件
//                TextView text1=(TextView)view.findViewById(R.id.device_name);
//                TextView text2=(TextView)view.findViewById(R.id.device_address);
//                mBluetoothLeService.connect(text2.getText().toString());        //连接设备
//                Toast.makeText(getApplicationContext(), text1.getText().toString(), Toast.LENGTH_SHORT).show();
//            }
//        });

    }

    @Override
    public void onResume(){
        super.onResume();
        simulateProgress();
        // Initializes list view adapter.
//        mLeDeviceListAdapter = new LeDeviceListAdapter();
//        setListAdapter(mLeDeviceListAdapter);
    }

    private void iniUI() {
        // Sets up UI references.
        mDataField = (TextView) findViewById(R.id.data_value);
        numDevice = (TextView) findViewById(R.id.device_number);
        numDevice.setText(deviceText + "0");
        svResult = (ScrollView) this.findViewById(R.id.svResult);

        btnDevice = (Button) this.findViewById(R.id.getDevice);
        btnDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });

        myLayout = (LinearLayout) findViewById(R.id.device_opTest);
    }

    public int singleSelectedId1;
    public void devicelist(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.logo);
        builder.setTitle("可用设备：");
        if (!mDeviceContainer.isEmpty()) {
            strDevice = new String[mDeviceContainer.size()];
            strMAC = new String[mDeviceContainer.size()];
            int i = 0;
            for(BluetoothDevice device: mDeviceContainer){
                strDevice[i] = device.getName() ;
                strMAC[i] = device.getAddress();
                i++;
            }
            builder.setSingleChoiceItems(strDevice, -1,new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    singleSelectedId1 = which;
//                    Toast.makeText(MainActivity.this, "您选中了："+strDevice[which], Toast.LENGTH_SHORT).show();
//                    mBluetoothLeService.disconnect(strMAC[which]);      //断开设备———————————————
                }
    });
        }else{
            String[] str = new String[1];
            str[0] = "未找到任何设备！";
            builder.setItems(str, null);
        }
        builder.setPositiveButton("连接设备", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                Toast.makeText(MainActivity.this, "您选中了："+strDevice[singleSelectedId1], Toast.LENGTH_SHORT).show();
                mBluetoothLeService.connect(strMAC[singleSelectedId1]);      //连接设备———————————————
            }
        });

        builder.setNegativeButton("取消选择", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub

            }
        });

        builder.show();

    }

    private void iniBle() {
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                if (mBluetoothAdapter.isEnabled()){
                    scanLeDevice(true);
                    mScanning = true;
                }else {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        }).start();
    }

    String[] strDevice;
    String[] strMAC;
    int singleSelectedId = 0;
    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.logo);
        builder.setTitle(getString(R.string.device_list));
        if (!mDeviceList.isEmpty()) {
            strDevice = new String[mDeviceList.size()];
            strMAC = new String[mDeviceList.size()];
            int i = 0;
            for(BluetoothDevice device: mDeviceList){
                strDevice[i] = device.getName() + ":" + device.getAddress();
                strMAC[i] = device.getAddress();
                i++;
            }
            builder.setSingleChoiceItems(strDevice, -1,new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // TODO Auto-generated method stub
                    singleSelectedId = which;
                    Log.w("选择的项目：",String.valueOf(singleSelectedId));
//                    Toast.makeText(MainActivity.this, "您选中了："+strDevice[which], Toast.LENGTH_SHORT).show();
//                    mBluetoothLeService.disconnect(strMAC[which]);      //断开设备———————————————
                }
            });
        }else{
            String[] str = new String[1];
            str[0] = "未连接任何设备！";
            builder.setItems(str, null);
        }
        builder.setPositiveButton("断开", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                Toast.makeText(MainActivity.this, "您选中了："+strDevice[singleSelectedId], Toast.LENGTH_SHORT).show();
                mBluetoothLeService.disconnect(strMAC[singleSelectedId]);      //断开设备———————————————
                if(singleSelectedId ==0) {
                    Removeview(1);
                    ctrolThread.updateFlag = false;
                    try {
                        Thread.currentThread().sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    update = null;
                }else if (singleSelectedId == 1){
                    Removeview(2);
                    ctrolThread1.updateFlag = false;
                    try {
                        Thread.currentThread().sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    update1 = null;
                }
            }
        });

        builder.setNegativeButton("版本信息", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                //自定义功能
                byte[] bytes = UpdateOpt.wakeupData;        //写入发送数据
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.i("等待延时：", "wait...");
                }

                WriteComm(mDeviceList.get(singleSelectedId) ,writecharacteristicList.get(singleSelectedId), bytes, bytes.length);
                byte[] data = {0x00,0x00};
                Log.w("获取版本:",String.valueOf(singleSelectedId));
                comm_send(mDeviceList.get(singleSelectedId) ,writecharacteristicList.get(singleSelectedId),
                        COMM_TRANS_TYPE_SEND,COMM_CMD_TYPE_VERSION,data,2);
            }
        });

        builder.setNeutralButton("升级",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //WriteComm(mDeviceList.get(singleSelectedId) ,writecharacteristicList.get(singleSelectedId), bytes, bytes.length);
                Log.w("升级功能,启动线程:",String.valueOf(singleSelectedId));
                if(singleSelectedId ==0) {
                    ctrolThread.getBarProgress();
                    ctrolThread.writeBleDevice(mDeviceList.get(singleSelectedId));
                    ctrolThread.writeGattCharacteristic(writecharacteristicList.get(singleSelectedId));
                    sendMessage(1);
                }else if (singleSelectedId ==1){
                    ctrolThread1.getBarProgress();
                    ctrolThread1.writeBleDevice(mDeviceList.get(singleSelectedId));
                    ctrolThread1.writeGattCharacteristic(writecharacteristicList.get(singleSelectedId));
                    sendMessage(2);
                }
            }
        });
        //builder.setCancelable(false);
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                //clearDevice();
                mDeviceContainer.clear();
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(250);
                            scanLeDevice(true);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                if(mDeviceList.size() == 1) {
                    Removeview(1);
                }else if(mDeviceList.size() ==2){
                    Removeview(1);
                    Removeview(2);
                }
                clearDevice();
                break;
            case R.id.menu_files:
                if(!menufiles ) {
                    sendMessage(41);
                    menufiles = true;
                }
            return true;
        }
        return true;
    }

    private void clearDevice() {
        mBluetoothLeService.disconnect();
        mDeviceContainer.clear();
        //写数据的服务和characteristic
        mnotyGattServiceList.clear();
        writecharacteristicList.clear();

        mDeviceList.clear();
        mDataField.setText("");
        numDevice.setText(deviceText + "0");
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(SCAN_PERIOD);

                        if(mScanning)
                        {
                            mScanning = false;
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);
                            invalidateOptionsMenu();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

        invalidateOptionsMenu();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        scanLeDevice(true);
        mScanning = true;
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!mDeviceContainer.isEmpty()) {
                                if(!isEquals(device)){          //不是第一个连接
                                    Log.i("不是第一个连接","这个没连，连上先");
                                    connectBle(device);
                                    mLeDeviceListAdapter.addDevice(device);
                                    mLeDeviceListAdapter.notifyDataSetChanged();
                                }
                            }else{                              //第一个连接
                                Log.i("未连接任何蓝牙设备","连上先");
                                connectBle(device);
                                mLeDeviceListAdapter.addDevice(device);
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            };

    private boolean isEquals(BluetoothDevice device){
        for(BluetoothDevice mDdevice: mDeviceContainer){
            if(mDdevice.equals(device)){
                return true;
            }
        }
        return false;
    }

    private void connectBle(BluetoothDevice device) {
        mDeviceContainer.add(device);
//        //写数据的服务和characteristic
//        mnotyGattService = mBluetoothLeService.getSupportedGattService( device,"0000fff0-0000-1000-8000-00805f9b34fb" );
//        writecharacteristic = mnotyGattService.getCharacteristic(UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"));
//        mnotyGattServiceList.add(mnotyGattService);
//        writecharacteristicList.add(writecharacteristic);
//        while (true) {
//            if (mBluetoothLeService != null) {
//                mBluetoothLeService.connect(device.getAddress());
//                break;
//            } else {
//                try {
//                    Thread.sleep(250);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.WRITE_STATUS);
        intentFilter.addAction(BluetoothDevice.ACTION_UUID);
        return intentFilter;
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            Log.i(TAG, "mBluetoothLeService is okay");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read or notification operations.
    public static ArrayList<BluetoothGattService> mnotyGattServiceList = new ArrayList<BluetoothGattService>();
    public static ArrayList<BluetoothGattCharacteristic> writecharacteristicList = new ArrayList<BluetoothGattCharacteristic>();
    public static BluetoothGattService mnotyGattService;
    public static BluetoothGattCharacteristic writecharacteristic;

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.w(TAG, "Only gatt, just wait");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                if (!mDeviceList.isEmpty()) {
                    String strAddress = intent.getStringExtra("DEVICE_ADDRESS");
                    Log.e("设备断开",strAddress);
//                    if(removeDevice(strAddress)){
//                        int deviceNum = mDeviceList.size()-1;
//                        String newStirng = deviceText + String.valueOf(deviceNum);
//                        numDevice.setText(newStirng );
//                    }
                    for(int ii=0;ii<mDeviceList.size();ii++){
                        if(mDeviceList.get(ii).getAddress().equals(strAddress))
                        {
                            //Removeview(ii+1);
                            Devicelayout.remove(ii);
                        }

                    }
                }

                invalidateOptionsMenu();
            }else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action))
            {
                if (!mDeviceContainer.isEmpty()) {
                    String strAddress = intent.getStringExtra("DEVICE_ADDRESS");
                    for(BluetoothDevice bluetoothDevice: mDeviceContainer){
                        if(bluetoothDevice.getAddress().equals(strAddress)){
                            mDeviceList.add(bluetoothDevice);
                            if(mDeviceList.size() == 1) {
                                AddCtrolview(mDeviceList.get(mDeviceList.size() - 1).getName(), "1准备就绪！");
                            }else if(mDeviceList.size() == 2)
                            {
                                AddCtrolview(mDeviceList.get(mDeviceList.size() - 1).getName(), "2准备就绪！");
                            }
                            //写数据的服务和characteristic
                            mnotyGattService = mBluetoothLeService.getSupportedGattService( bluetoothDevice,"0000fff0-0000-1000-8000-00805f9b34fb" );
                            writecharacteristic = mnotyGattService.getCharacteristic(UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"));
                            mnotyGattServiceList.add(mnotyGattService);
                            writecharacteristicList.add(writecharacteristic);
                        }
                    }
                }
                numDevice.setText(deviceText + mDeviceList.size());
//                Toast.makeText(MainActivity.this, "Discover GATT Services", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Discover GATT Services");
                invalidateOptionsMenu();
            }else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //Log.i(TAG, "ACTION_DATA_AVAILABLE");
                byte[] data,mac = new byte[17];
                data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                System.arraycopy(data, 0, mac, 0, 17);
                String deviceMac = new String(mac);

                byte[] devicedata = new byte[data.length-17];
                System.arraycopy(data,17,devicedata,0,data.length-17);

                Log.e("接收数据：",new String(devicedata));
                PrintLog.printHexString("收到的数据回应：",devicedata);
                if (!String.valueOf(devicedata ).equals("")) {
                    if (mDataField.length() > 900) {
                        mDataField.setText("");
                    }
                    //mDataField.append(data);
                    if(devicedata.length >1) {
                        if( devicedata[0] == (byte)0x40
                                && devicedata[devicedata.length-1] == (byte)0x2A ){
                            mDataField.append( new String(devicedata), 4, devicedata.length - 2);
                            mDataField.append("\r\n");
                            if(devicedata[2] == (byte)0x52  )
                            {
                                if(deviceMac.equals(mDeviceList.get(0).getAddress()) ) {
                                    ctrolThread.updateReceive_respons(devicedata, devicedata.length - 4);
                                }else if(deviceMac.equals(mDeviceList.get(1).getAddress()) ){
                                    ctrolThread1.updateReceive_respons(devicedata, devicedata.length - 4);
                                }
                            }
                        }else if(devicedata[0] == (byte)0x40) {
                            mDataField.append(new String(devicedata), 4, devicedata.length);
                        }else if(devicedata[devicedata.length-1]== (byte)0x2A ) {
                            mDataField.append(new String(devicedata), 0, devicedata.length - 2);
                            mDataField.append("\r\n");
                        }else {
                            mDataField.append(new String(devicedata), 0, devicedata.length );
                        }
                    }
                    svResult.post(new Runnable() {
                        public void run() {
                            svResult.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                }
            }else if(BluetoothLeService.WRITE_STATUS.equals(action))
            {
                WriteCharacterRspFlag = true;
                byte[] mac = new byte[17];
                mac = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String deviceMac = new String(mac);
                if(deviceMac.equals(mDeviceList.get(0).getAddress()) ) {
                    //ctrolThread.WriteCharacterRspFlag = true;
                    Log.d("写数据结果00000000","回应成功");
                }else if(deviceMac.equals(mDeviceList.get(1).getAddress())) {
                    ctrolThread1.WriteCharacterRspFlag = true;
                    Log.d("写数据结果11111111","回应成功");
                }
                //Log.d("写数据结果","回应成功");

            }
        }
    };

    LinearLayout myLayout ; // myLayout是我这个activity的界面的root layout
    View hiddenView,hiddenView1,hiddenView2,hiddenView3,hiddenView4 ; //hiddenView是隐藏的View，
    ArrayList<View> Devicelayout = new ArrayList<View>();
    public void AddCtrolview(String name,String State )
    {
        if(Devicelayout.size() == 0) {
            hiddenView = getLayoutInflater().inflate(R.layout.device, myLayout, false);
            //从hidden_view.xml文件导入
            myLayout.addView(hiddenView);
            Devicelayout.add(hiddenView);

            updatename = (TextView) findViewById(R.id.update_name);
            update_info = (TextView) findViewById(R.id.update_info);
            mProgressBar = (CircleProgressBar) findViewById(R.id.update_progress);

            updatename.setText(name);
            update_info.setText(State);
        }else if(Devicelayout.size() == 1){
            hiddenView1 = getLayoutInflater().inflate(R.layout.device1, myLayout, false);
            //从hidden_view.xml文件导入
            myLayout.addView(hiddenView1);
            Devicelayout.add(hiddenView1);

            updatename1 = (TextView) findViewById(R.id.update_name1);
            update_info1 = (TextView) findViewById(R.id.update_info1);
            mProgressBar1 = (CircleProgressBar) findViewById(R.id.update_progress1);

            updatename1.setText(name);
            update_info1.setText(State);
        }

    }

    public void Removeview(int which)
    {
        //从hidden_view.xml文件导入
        if(which ==1) {
            myLayout.removeView(hiddenView);
        }else if (which ==2){
            myLayout.removeView(hiddenView1);
        }
    }

    private boolean removeDevice(String strAddress) {
        for(final BluetoothDevice bluetoothDevice:mDeviceList){
            if(bluetoothDevice.getAddress().equals(strAddress)){
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(250);
                            mDeviceList.remove(bluetoothDevice);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);

        if(mBluetoothLeService != null)
        {
            mBluetoothLeService.close();
            mBluetoothLeService = null;
        }

        Log.i(TAG, "MainActivity closed!!!");
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceDb;
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceDb = (TextView) view.findViewById(R.id.device_db);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            //viewHolder.deviceDb.setText(device.EXTRA_RSSI);

            return view;
        }
    }

    public static Boolean receiveDataFlag = false;
    public static Boolean WriteCharacterRspFlag = false;
    public static Boolean WriteComm(BluetoothDevice bluetoothDevice,
                                    BluetoothGattCharacteristic WriteCharacteristic,
                                    byte[] SendData, int DateCount)
    {
        Boolean bool = false;
        int count = 0;
        if(DateCount>20){
            for(int i = 0;i<DateCount;i=i+20)
            {
                bool = WriteCharacteristic.setValue(UpdateOpt.subBytes(SendData, i, 20));
                //PrintLog.printHexString("Gatt写长数据",WriteCharacteristic.getValue());
                WriteCharacterRspFlag = false;
                BluetoothLeService.writeCharacteristic(bluetoothDevice, WriteCharacteristic);

                while (!WriteCharacterRspFlag)
                {
//                    count++;
//                    if(count == 5) {
//                        count = 0;
//                        Log.i("发送数据：", "分段发送5次失败");
//                        break;
//                    }
                    //BluetoothLeService.writeCharacteristic(WriteCharacteristic);
                    try {
                        Thread.currentThread().sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                //Log.i("回应标志：", WriteCharacterRspFlag.toString());
                WriteCharacterRspFlag = false;
            }
            bool = true;
        }else {
            bool = WriteCharacteristic.setValue(SendData);
            PrintLog.printHexString("Gatt写短数据",WriteCharacteristic.getValue());
            if (bool) {
                BluetoothLeService.writeCharacteristic(bluetoothDevice, WriteCharacteristic);
                WriteCharacterRspFlag = false;
                while (!WriteCharacterRspFlag)
                {
                    count++;
                    if(count == 3) {
                        count = 0;
                        Log.i("发送短数据：", "发送4次失败");
                        break;
                    }
                    BluetoothLeService.writeCharacteristic(bluetoothDevice, WriteCharacteristic);
                    try {
                        Thread.currentThread().sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                bool = true;
                Log.i("回应标志：", WriteCharacterRspFlag.toString());
                WriteCharacterRspFlag = false;
            } else {
                Log.i("写特征值：", "本地写失败");
                bool = false;
            }
        }
        return bool;
    }

/**-------------------------------------------------------------------------
* 函数: comm_send
* 说明: 发送
* 参数: pData---数据buffer
		len-----条码长度
* 返回: HY_OK------发送成功
		HY_ERROR---发送失败
-------------------------------------------------------------------------*/
    /* 封包起始和结尾字节 */
    byte    COMM_PAKET_START_BYTE    = 0x40;
    byte    COMM_PAKET_END_BYTE		=(byte)	(0x2A);
    /* 收发类型 */
    byte	COMM_TRANS_TYPE_SEND		=	(0x53);	/* 'S'---send */
    byte	COMM_TRANS_TYPE_RESP		=	(0x52);	/* 'R'---response */
    byte	COMM_CMD_TYPE_UPDATE		=(byte)	(0xD0);	//软件升级
    byte	COMM_CMD_TYPE_VERSION		=	(byte)(0xE0);	//R11版本信息


    Boolean comm_send(BluetoothDevice bluetoothDevice,
                      BluetoothGattCharacteristic WriteCharacteristic,
                      byte transType, byte cmd, byte[] pData, int len)
    {
        byte i;
        byte[] temp = new byte[len + 6];
        int sum=0;

        if (pData==null) return false;

        temp[0] = COMM_PAKET_START_BYTE;
        temp[1] = (byte)(len+2);
        temp[2] = (byte)transType;
        temp[3] = (byte)cmd;
        //memcpy(&temp[4], pData, len);
        System.arraycopy(pData,0,temp,4,len);
        temp[len+5] = COMM_PAKET_END_BYTE;
        for(i=0; i<len+3; i++)
        {
            sum += temp[i+1];
        }
        temp[len+4] = (byte)sum;
        //Log.i("调用特征值写：", "wait...");
        WriteComm(bluetoothDevice ,WriteCharacteristic, temp, len+6);

        return true;
    }

    private void simulateProgress() {
        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int progress = (int) animation.getAnimatedValue();
//                mLineProgressBar.setProgress(progress);
//                mSolidProgressBar.setProgress(progress);
                mCustomProgressBar1.setProgress(progress);
                if(Devicelayout.size() == 1) {
                    mProgressBar.setProgress(ctrolThread.getBarProgress());
                }else if(Devicelayout.size() == 2){
                    mProgressBar.setProgress(ctrolThread.getBarProgress());
                    mProgressBar1.setProgress(ctrolThread1.getBarProgress());
                }
            }
        });
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setDuration(4000);
        animator.start();
    }

    /**
     * 菜单、返回键响应
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            exitBy2Click(); //调用双击退出函数
        }
        return false;
    }
    /**
     * 双击退出函数
     */
    private static Boolean isExit = false;

    private void exitBy2Click() {
        Timer tExit = null;
        if (isExit == false) {
            isExit = true; // 准备退出
            Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            tExit = new Timer();
            tExit.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExit = false; // 取消退出
                }
            }, 2000); // 如果2秒钟内没有按下返回键，则启动定时器取消掉刚才执行的任务

        } else {
            finish();
            System.exit(0);
        }
    }

    public void sendMessage(int what)
    {
        Message message = new Message();
        message.what = what;
        handler.sendMessage(message);
    }

    final Handler handler=new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    //update_step = 0;
                    //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    //if (filedataLen != 0)
                    Toast.makeText(getApplicationContext(), "升级成功！！！", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    ctrolThread.updateFlag = false;
                    try {
                        Thread.currentThread().sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(!ctrolThread.updateFlag){
                        update = new Thread(ctrolThread.sendData, "Update");
                        update.start();
                    }else {
                        Toast.makeText(getApplicationContext(), "任务已经启动！", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 2:
                    ctrolThread1.updateFlag = false;
                    try {
                        Thread.currentThread().sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(!ctrolThread1.updateFlag){
                        update1 = new Thread(ctrolThread1.sendData, "Update1");
                        update1.start();
                    }else {
                        Toast.makeText(getApplicationContext(), "任务已经启动！", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case 41:
                    new Thread(runnable,"GetFiles").start();
                    break;
                case 42:
                    Toast.makeText(getApplicationContext(), "升级文件信息："+sp.getString("FileName", "不存在升级文件，请先选择升级文件！"), Toast.LENGTH_SHORT).show();
                    break;
                case 43:
                    new Thread(httpdownload,"Download").start();
                    break;
                case 44:
                    Toast.makeText(getApplicationContext(), "新升级文件准备就绪！", Toast.LENGTH_LONG).show();
                    //updateState.setText( "升级文件信息："+sp.getString("FileName", "不存在升级文件，请先选择升级文件！") );
                    break;
                case 45:
                    if(!list.isEmpty())
                        showSingleChoiceButton();
                    break;
                case 46:
                    Toast.makeText(getApplicationContext(), "请确认网络连接？", Toast.LENGTH_LONG).show();
            }
        }
    };

    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            loads();
            Looper.prepare();
            sendMessage(45);
        }
    };

    private String[] province = new String[] { "上海", "北京", "海南" };
    // 单击事件对象的实例
    private ButtonOnClick buttonOnClick;
    // 在单选选项中显示 确定和取消按钮
    //buttonOnClickg变量的数据类型是ButtonOnClick,一个单击事件类
    private void showSingleChoiceButton()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请选择升级文件版本");
        builder.setSingleChoiceItems(province,province.length-1, buttonOnClick);
        builder.setPositiveButton("确定", buttonOnClick);
        builder.setNegativeButton("取消", buttonOnClick);
        builder.setCancelable(false);
        builder.show();
    }

    public int index; // 表示选项的索引
    private class ButtonOnClick implements DialogInterface.OnClickListener
    {

        public ButtonOnClick(int listindex)
        {
            index = listindex;
        }

        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            // which表示单击的按钮索引，所有的选项索引都是大于0，按钮索引都是小于0的。
            if (which >= 0)
            {
                //如果单击的是列表项，将当前列表项的索引保存在index中。
                //如果想单击列表项后关闭对话框，可在此处调用dialog.cancel()
                //或是用dialog.dismiss()方法。
                index = which;
            }
            else
            {
                //用户单击的是【确定】按钮
                if (which == DialogInterface.BUTTON_POSITIVE)
                {
                    //显示用户选择的是第几个列表项。
                    Log.w("选择的文件是：",province[index]);
                    final AlertDialog ad = new AlertDialog.Builder(
                            MainActivity.this).setMessage(
                            "你选择的文件是：" + index + ":" + province[index]).show();
                    sendMessage(43);
                    //五秒钟后自动关闭。
                    Handler hander = new Handler();
                    Runnable runnable = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            ad.dismiss();
                        }
                    };
                    hander.postDelayed(runnable, 2 * 1000);
                    //TODO
//                    downloadfile(province[index] );
                }
                //用户单击的是【取消】按钮
                else if (which == DialogInterface.BUTTON_NEGATIVE)
                {
                    Toast.makeText(MainActivity.this, "你没有选择任何文件！",
                            Toast.LENGTH_LONG).show();
                }
                menufiles =false;
            }
        }
    }

    Boolean menufiles =false;
    Document doc;
    String Url = "http://hycosoft.cc/w16.json";
    List<Map<String, String>> list = new ArrayList<Map<String, String>>();
    protected void loads() {
        String getdata = HttpUser.getJsonContent(Url);  //请求数据地址
        if(getdata == ""){
            sendMessage(46);
            return;
        }
        //JSON对象 转 JSONModel对象
        Root result = JavaBean.getPerson(getdata, Root.class);
        list.clear();
        for (int i=0;i<result.getKey().size();i++) {{
            Map<String, String> map = new HashMap<String, String>();
            map.put("Name", result.getKey().get(i).getName());
            map.put("Url", result.getKey().get(i).getUrl() );
            list.add(map);
        }
        }
        province = new String[result.getKey().size()];
        for(int j=0;j<result.getKey().size();j++)
        {
            province[j] = list.get(j).get("Name");
        }
        buttonOnClick = new ButtonOnClick(province.length-1);
    }

    Runnable httpdownload =new Runnable() {
        @Override
        public void run() {
            downloadfile( list.get(index).get("Url"));
            Looper.prepare();
        }
    };
    /**
     *
     * @Project: Android_MyDownload
     * @Desciption: 读取任意文件，并将文件保存到手机SDCard
     * @Author: LinYiSong
     * @Date: 2011-3-25~2011-3-25
     */
    public void downloadfile(String  urlStr)
    {
        String path="Downloads";
        String fileName="image_W16.hyc";
        OutputStream output=null;
        try {
            /*
             * 通过URL取得HttpURLConnection
             * 要网络连接成功，需在AndroidMainfest.xml中进行权限配置
             * <uses-permission android:name="android.permission.INTERNET" />
             */
            //urlStr = null;
            URL url=new URL( urlStr);
            HttpURLConnection conn=(HttpURLConnection)url.openConnection();
            //获得文件的长度
            int contentLength = conn.getContentLength();
            System.out.println("长度 :"+contentLength);

            //取得inputStream，并将流中的信息写入SDCard
            //String SDCard= Environment.getExternalStorageDirectory()+"";
            String SDCard= Environment.getExternalStorageDirectory()+"";
            String pathName=SDCard+"/"+path+"/"+fileName;//文件存储路径

            File file=new File(pathName);
            InputStream input=conn.getInputStream();
            if(file.exists()){
                System.out.println("exits");
                file.delete();
            }else{
                String dir=SDCard+"/"+path;
                new File(dir).mkdir();//新建文件夹
                file.createNewFile();//新建文件
                output=new FileOutputStream(file);
                //读取大文件
                byte[] buffer=new byte[1024];
                //Log.w("文件请求大小",String.valueOf(input.available()));
                int len;            //重要参数
                while( (len = input.read(buffer))!=-1 ){
                    output.write(buffer,0,len);
                }
                output.flush();
                input.close();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            try {
                if(output != null) {
                    output.close();
                    System.out.println("success");
                    menufiles = false;
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("FileName", province[index]);
                    editor.commit();

                    sendMessage(44);
                }else {
                    System.out.println("fail");
                    sendMessage(43);
                }
            } catch (IOException e) {
                System.out.println("fail");
                e.printStackTrace();
            }
        }
    }

}
