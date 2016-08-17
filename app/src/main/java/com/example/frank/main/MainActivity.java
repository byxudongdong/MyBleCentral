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
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends ActionBarActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private ListView listView;
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
    private LinkedList<BluetoothDevice> mDeviceContainer = new LinkedList<BluetoothDevice>();  //已连接的设备
    private LinkedList<BluetoothDevice> mDeviceConnectable = new LinkedList<BluetoothDevice>();  //可连接的设备
    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();  //所有搜索到的设备

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services);
        deviceText = getString(R.string.device_number);
        iniUI();
        iniBle();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        Log.d(TAG, "Try to bindService=" + bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE));
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        mCustomProgressBar1 = (CircleProgressBar) findViewById(R.id.custom_progress1);

        listView = (ListView)findViewById(R.id.list_goods);
        mLeDeviceListAdapter = new LeDeviceListAdapter(); //创建适配器
        listView.setAdapter(mLeDeviceListAdapter);

        //设置 item 的监听事件
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //获得 item 里面的文本控件
                TextView text1=(TextView)view.findViewById(R.id.device_name);
                TextView text2=(TextView)view.findViewById(R.id.device_address);
                mBluetoothLeService.connect(text2.getText().toString());        //连接设备
                Toast.makeText(getApplicationContext(), text1.getText().toString(), Toast.LENGTH_SHORT).show();
            }
        });

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
    int singleSelectedId = -1;
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
            }
        });

        builder.setNegativeButton("版本信息", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                //自定义功能
                byte[] bytes = UpdateOpt.wakeupData;        //写入发送数据

                WriteComm(mDeviceList.get(singleSelectedId) ,writecharacteristicList.get(singleSelectedId), bytes, bytes.length);
                byte[] data = {0x00,0x00};
                Log.i("获取版本","获取版本");
                comm_send(mDeviceList.get(singleSelectedId) ,writecharacteristicList.get(singleSelectedId),
                        COMM_TRANS_TYPE_SEND,COMM_CMD_TYPE_VERSION,data,2);
            }
        });

        builder.setNeutralButton("升级",null);

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
                clearDevice();
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
                clearDevice();
                break;
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
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    public static ArrayList<BluetoothGattService> mnotyGattServiceList = new ArrayList<BluetoothGattService>();
    public static ArrayList<BluetoothGattCharacteristic> writecharacteristicList = new ArrayList<BluetoothGattCharacteristic>();
    public static BluetoothGattService mnotyGattService;
    public static BluetoothGattCharacteristic writecharacteristic;

    public String data="";
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.w(TAG, "Only gatt, just wait");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                if (!mDeviceList.isEmpty()) {
                    String strAddress = intent.getStringExtra("DEVICE_ADDRESS");
                    if(removeDevice(strAddress)){
                        int deviceNum = mDeviceList.size()-1;
                        String newStirng = deviceText + String.valueOf(deviceNum) ;
                        numDevice.setText(newStirng );
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
                            AddBarview(mDeviceList.get(mDeviceList.size()-1).getName() ,"准备就绪！");
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
                data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                String device = data.substring(0,17);
                String devicedata = data.substring(17);
                Log.w("接收数据：",data);
                if (!devicedata .equals("")) {
                    if (mDataField.length() > 900) {
                        mDataField.setText("");
                    }
                    //mDataField.append(data);
                    if(devicedata.length()>1) {
                        if(devicedata.startsWith(String.valueOf((char)0x40))
                                && devicedata.endsWith(String.valueOf((char)0x2A)) ){
                            mDataField.append(devicedata, 4, devicedata.length() - 2);
                            mDataField.append("\r\n");
                        }else if(devicedata.startsWith(String.valueOf((char)0x40))) {
                            mDataField.append(devicedata, 4, devicedata.length());
                        }else if(devicedata.endsWith(String.valueOf((char)0x2A))) {
                            mDataField.append(devicedata, 0, devicedata.length() - 2);
                            mDataField.append("\r\n");
                        }else {
                            mDataField.append(devicedata, 0, devicedata.length() );
                        }
                    }
                    svResult.post(new Runnable() {
                        public void run() {
                            svResult.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                }
            }
        }
    };

    public void AddBarview(String name,String State )
    {
        LinearLayout myLayout = (LinearLayout) findViewById ( R.id.device_op) ; // myLayout是我这个activity的界面的root layout
        View hiddenView = getLayoutInflater().inflate( R.layout.device, myLayout, false ) ; //hiddenView是隐藏的View，
        //从hidden_view.xml文件导入
        myLayout.addView ( hiddenView ) ;
        TextView updatename = (TextView)findViewById(R.id.update_name);
        TextView update_info = (TextView)findViewById(R.id.update_info);
        updatename.setText(name);
        update_info.setText(State);
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
                    if(count == 4) {
                        count = 0;
                        Log.i("发送短数据：", "发送4次失败");
                        break;
                    }
                    BluetoothLeService.writeCharacteristic(bluetoothDevice, WriteCharacteristic);
                    try {
                        Thread.currentThread().sleep(30);
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

    /*-------------------------------------------------------------------------
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

}
