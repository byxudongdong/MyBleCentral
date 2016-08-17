/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.frank.main;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private Thread serviceDiscoveryThread = null;
    private BluetoothManager mBluetoothManager = null;
    private static BluetoothAdapter mBluetoothAdapter = null;
    //private static ArrayList<BluetoothGatt> mBluetoothGatt = new ArrayList<BluetoothGatt>();
    private static ArrayList<BluetoothGatt> connectionQueue = new ArrayList<BluetoothGatt>();

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String WRITE_STATUS =
            "com.example.bluetooth.le.WRITE_STATUS";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    Boolean WriteCharacterRspFlag = false;

    public final static UUID UUID_NOTIFY =
            UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_SERVICE =
            UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    public final static String DEVICE_SERVICE_POWER = "4002530D622A";// 电量获取
    public final static UUID WRITE_DEVICE_UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    
    public BluetoothGattCharacteristic mNotifyCharacteristic;
    
    public void findService(BluetoothGatt gatt)
    {
        List<BluetoothGattService> gattServices = gatt.getServices();
    	Log.i(TAG, "Count is:" + gattServices.size());
    	for (BluetoothGattService gattService : gattServices)
    	{
    		//Log.i(TAG, gattService.getUuid().toString());
			//Log.i(TAG, UUID_SERVICE.toString());
    		if(gattService.getUuid().toString().equalsIgnoreCase(UUID_SERVICE.toString()))
    		{
    			List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
    			Log.i(TAG, "Count is:" + gattCharacteristics.size());
    			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics)
    			{
    				if(gattCharacteristic.getUuid().toString().equalsIgnoreCase(UUID_NOTIFY.toString()))
    				{
    					Log.i("找到UUID：", gattCharacteristic.getUuid().toString());
    					//Log.i(TAG, UUID_NOTIFY.toString());
    					mNotifyCharacteristic = gattCharacteristic;
    					setCharacteristicNotification(gattCharacteristic, true);
    					broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, gatt.getDevice().getAddress());
    					return;
    				}
    			}
    		}
    	}
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            Log.i(TAG, "oldStatus=" + status + " NewStates=" + newState);
            if(status == BluetoothGatt.GATT_SUCCESS)
            {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    intentAction = ACTION_GATT_CONNECTED;

                    broadcastUpdate(intentAction);
                    Log.i(TAG, "Connected to GATT server.");
                    // Attempts to discover services after successful connection.
    //                Log.i(TAG, "Attempting to start service discovery:" +
    //                        mBluetoothGatt.discoverServices());
                    Log.i(TAG, "Attempting to start service discovery:" + gatt.discoverServices());
    //                initServiceDiscovery(gatt);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    intentAction = ACTION_GATT_DISCONNECTED;
    //                mBluetoothGatt.close();
    //                mBluetoothGatt = null;
                    listClose(gatt);
                    Log.e(TAG, "Disconnected from GATT server.");
                    broadcastUpdate(intentAction, gatt.getDevice().getAddress());
                }
        	}
        }

//        private void initServiceDiscovery(final BluetoothGatt gatt){
//            if(serviceDiscoveryThread == null){
//                serviceDiscoveryThread = new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        gatt.discoverServices();
//                        try {
//                            Thread.sleep(250);
//                        } catch (InterruptedException e){}
//                        serviceDiscoveryThread.interrupt();
//                        serviceDiscoveryThread = null;
//                    }
//                });
//
//                serviceDiscoveryThread.start();
//            }
//        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	Log.i(TAG, "onServicesDiscovered received: " + status);  //0：成功
            	findService(gatt);
            } else {
            	if(gatt.getDevice().getUuids() == null)
                Log.e(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate( gatt, ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(gatt, ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            //得到写回应，在这里显示写结果
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(gatt , WRITE_STATUS, characteristic);
                WriteCharacterRspFlag = true;
            }
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final String strAddress) {
        final Intent intent = new Intent(action);
        intent.putExtra("DEVICE_ADDRESS", strAddress);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(BluetoothGatt gatt,final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            intent.putExtra(EXTRA_DATA, gatt.getDevice().getAddress() +new String(data));
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        BluetoothGatt bluetoothGatt;
        for(BluetoothGatt btg:connectionQueue)
        {
            if(btg.getDevice().getAddress().equals( address) )
            {
                Log.w("已存在的设备","无需重复连接");
                return false;
            }
        }
        bluetoothGatt = device.connectGatt(this, false, mGattCallback);
        //mBluetoothGatt = bluetoothGatt;
        if(checkGatt(bluetoothGatt)){           //判断是否已经连接过，没连再连接
            connectionQueue.add(bluetoothGatt);
        }
        
        Log.i(TAG, "Trying to create a new connection(建立新连接！！！).");
        return true;
    }

    private boolean checkGatt(BluetoothGatt bluetoothGatt) {
        if (!connectionQueue.isEmpty()) {
            for(BluetoothGatt btg:connectionQueue){
                if( btg.getDevice().getAddress().equals( bluetoothGatt.getDevice().getAddress() ) )
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || connectionQueue.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
//        mBluetoothGatt.disconnect();
        for(BluetoothGatt bluetoothGatt:connectionQueue){
            bluetoothGatt.disconnect();
        }
    }

    public void disconnect(final String address) {
        if (mBluetoothAdapter == null || connectionQueue.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
//        mBluetoothGatt.disconnect();
        for(BluetoothGatt bluetoothGatt:connectionQueue){
            BluetoothDevice mDevice;
            mDevice = bluetoothGatt.getDevice();
            if(mDevice.getAddress().equals(address))
                bluetoothGatt.disconnect();
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (connectionQueue.isEmpty()) {
            return;
        }
//        mBluetoothGatt.close();
//        mBluetoothGatt = null;
        listClose(null);
    }

    private synchronized void listClose(BluetoothGatt gatt) {
        if (!connectionQueue.isEmpty()) {
            if (gatt != null) {
                for(final BluetoothGatt bluetoothGatt:connectionQueue){
                    if(bluetoothGatt.equals(gatt)){
                        bluetoothGatt.close();

                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    Thread.sleep(250);
                                    connectionQueue.remove(bluetoothGatt);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                }
            }else{
                for (BluetoothGatt bluetoothGatt : connectionQueue) {
                    bluetoothGatt.close();
                }
                connectionQueue.clear();
            }
        }
    }

    public BluetoothGattService getSupportedGattService(BluetoothDevice bluetoothDevice, String uuid) {
        BluetoothGattService flag = null;
        if (connectionQueue == null) return null;
        for(BluetoothGatt bluetoothGatt:connectionQueue) {
            if( bluetoothGatt.getDevice().equals(bluetoothDevice) )
                flag = bluetoothGatt.getService( UUID.fromString(uuid) );
        }
        return flag;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public static void readCharacteristic(BluetoothDevice bluetoothDevice,BluetoothGattCharacteristic characteristic)
    {
        if (mBluetoothAdapter == null || connectionQueue == null) {
            Log.w("Read", "BluetoothAdapter not initialized");
            return;
        }
        Log.i("Gatt读数据",characteristic.getStringValue(0));
        for(BluetoothGatt bluetoothGatt:connectionQueue) {
            if( bluetoothGatt.getDevice().equals(bluetoothDevice) )
                bluetoothGatt.readCharacteristic(characteristic);
        }
    }

    public static Boolean writeCharacteristic(BluetoothDevice bluetoothDevice,BluetoothGattCharacteristic characteristic)
    {
        Boolean flag = false;
        if (mBluetoothAdapter == null || connectionQueue == null) {
            Log.w("Write", "BluetoothAdapter not initialized");
            return false;
        }
        //Log.i("Gatt写数据",characteristic.getStringValue(0));
        //PrintLog.printHexString("Gatt写数据",characteristic.getValue());
        for(BluetoothGatt bluetoothGatt:connectionQueue) {
            if( bluetoothGatt.getDevice().equals(bluetoothDevice) )
                flag =  bluetoothGatt.writeCharacteristic(characteristic);
        }
        return flag;
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || connectionQueue.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        for(BluetoothGatt bluetoothGatt:connectionQueue){
            bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        }
    }
}
