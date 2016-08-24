package com.example.frank.main;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.example.frank.main.bar.CircleProgressBar;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by doyle on 2016/8/18 0018.
 */
public class CtrolThread {

    /* 封包起始和结尾字节 */
    byte    COMM_PAKET_START_BYTE    = 0x40;
    byte    COMM_PAKET_END_BYTE		=(byte)	(0x2A);
    /* 收发类型 */
    byte	COMM_TRANS_TYPE_SEND		=	(0x53);	/* 'S'---send */
    byte	COMM_TRANS_TYPE_RESP		=	(0x52);	/* 'R'---response */
    /* 命令类型 */
    byte	COMM_CMD_TYPE_UPDATE		=(byte)	(0xD0);	//软件升级
    byte	COMM_CMD_TYPE_VERSION		=	(byte)(0xE0);	//R11版本信息
    /* 封包最小长度 */
    int     UPDATE_SEND_PAKET_SIZE  = 112;
    final int UPDATE_REQUEST_ID	 =	(int)	(0xFFFF);
    final int UPDATE_CRC_RESP_ID 	=(int) (0xFFFE);
    final byte UPDATE_REQUST_OK			=		(0x00);//升级请求被接受
    final byte UPDATE_REJECT_REASON_HW_ERR		=	(0x01);//硬件版本错误
    final byte UPDATE_REJECT_REASON_SIZE_ERR	=	(0x02);//升级包大小错误(超过限制)

    final int UPDATE_STEP_SEND_REQUEST	=	0;
    final int UPDATE_STEP_WAIT_REQUEST_RES=	1;
    final int UPDATE_STEP_SEND_IMAGE		=	2;
    final int UPDATE_STEP_WAIT_IMAGE_RES	=	3;
    final int UPDATE_STEP_WAIT_CRC_RES	=	4;
    final int UPDATE_STEP_CRC_RES_RECV 	=	5;
    private int update_sendLen=0,filedataLen=0,updateIdex= 0,update_step = UPDATE_STEP_SEND_REQUEST;
    private long startTime=0,consumingTime=0;  //開始時間
    FileInputStream fin = null;
    private byte [] buffer = null;
    private int imageIndex = 0,imageNum=0;
    private Boolean supportCipher = false;
    private tUpdate_info Update_info = new tUpdate_info();
    private MyNative myNative = new MyNative();
    private UpdateOpt updateOpt = new UpdateOpt();
    private Thread mthread;
    Boolean updateFlag = false;
    private Boolean receiveDataFlag = false;
    Boolean WriteCharacterRspFlag = false;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic writecharacteristic;
    private int update_sendSize;
    private BluetoothDevice bluetoothDevice;
    private int myProgress = 0;

    public void writeBleDevice(BluetoothDevice device)
    {
        bluetoothDevice = device;
    }

    public BluetoothDevice readBleDevice()
    {
        return bluetoothDevice;
    }

    public void writeGattCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        writecharacteristic = characteristic;
    }

    public void setBarProgress(int mProgressBar){
        myProgress = mProgressBar;
    }

    public int getBarProgress()
    {
        return myProgress;
    }

    Runnable sendData = new Runnable()
    {
        @Override
        public void run() {
            //读SD中的文件
            try{
                String filePath = UpdateOpt.getSdCardPath() + "/Downloads/image_W16.hyc";
                int retry =6;
                imageNum = 0;
                myNative.update_checkSetFlag(0);
                while(imageNum <1) {
                    try {
                        imageNum = myNative.update_fileParse(filePath.getBytes());
                    } catch (Exception e) {
                        Log.w("升级文件不存在：", "请放入升级文件");
                        sendMessage(6);
                    }
                    if(imageNum >0) break;
                    retry --;
                    if(retry == 0){
                        Log.i("升级文件个数", String.valueOf(imageNum));
                        break;
                    }
                }

                fin = new FileInputStream(filePath);
                int filedataLenTotal = fin.available();
                Log.i("文件字节数",String.valueOf(filedataLenTotal)+":"+String.valueOf(imageNum));
                if(imageNum >0) {
                    try {
                        fin.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                buffer = new byte[98];
            } catch(Exception e){
                e.printStackTrace();
            }

            updateFlag = true;
            byte[] bytes = UpdateOpt.wakeupData;        //写入发送数据

            if(imageNum <1) {
                sendMessage(6);
                Log.e("没有解析到升级文件","退出");
                updateFlag = false;
            }else {
                int ret = myNative.update_getImageInfo(imageIndex, Update_info.ppVer_Str,
                        Update_info.hw_info,
                        Update_info.image_size,
                        Update_info.image_crc,
                        Update_info.image_data);
                if(Update_info.hw_info[0] == 0x00) {
                    sendMessage(3);
                    myNative.update_checkSetFlag(0);
                    int ret1 = myNative.update_getImageInfo(imageIndex, Update_info.ppVer_Str,
                            Update_info.hw_info,
                            Update_info.image_size,
                            Update_info.image_crc,
                            Update_info.image_data);
                }
                update_step = 0;
                PrintLog.printHexString("输出CRC信息------",Update_info.image_crc);
                while (updateFlag && Update_info.hw_info[0] != 0x00 ) {
                    //发送唤醒
                    //PrintLog.printHexString("输出CRC信息------",Update_info.image_crc);
                    if (update_step == 0) {
                        Log.i("唤醒蓝牙：", "wait...");
                        WriteComm(writecharacteristic, bytes, bytes.length);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Log.i("等待延时：", "wait...");
                        }
                    }
                    if(Update_info.image_size[0] == 0 &&
                            Update_info.image_size[1] == 0 &&
                            Update_info.image_size[2] == 0 &&
                            Update_info.image_size[3] == 0)
                    {
                        sendMessage(4);
                        break;
                    }
                    //Log.i("升级流程切换：", "wait...");

                    update_step = update_Switch();

//                try {
//                    int offset = updateIdex * 98 +1024;
//                    if(offset <= filedataLen)
//                        update_sendLen = fin.read(buffer,offset ,98);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                if(update_sendLen > 0)
//                {
//                    Log.i("发送文件数据：", "wait...");
//                    //WriteComm(writecharacteristic, buffer, sendLen);
//                    comm_send(COMM_TRANS_TYPE_SEND, COMM_CMD_TYPE_UPDATE, buffer, update_sendLen);
//                }
                    if (update_step == 5) {
                        //升级完成后
                        sendMessage(0);
                        updateFlag = false;
                        break;
                    }

                    //updateFlag = false;
                }
            }

            update_step = 0;
            try {
                fin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.i("结束任务：", "back...");
        }
    };

    public void sendMessage(int what)
    {
        Message message = new Message();
        message.what = what;
        handler.sendMessage(message);
    }

    final Handler handler=new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch(msg.what){
                case 0:
                    update_step = 0;
                    //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    if(filedataLen != 0)
                        //Toast.makeText(getApplicationContext(), "升级成功！！！", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    //Toast.makeText(getApplicationContext(), "收到回应", Toast.LENGTH_SHORT).show();
                    //receiveDataFlag = true;
                    //updateState.setText("收到回应");
//                    synchronized(object)
//                    {
//                        Log.i("解锁通知：", "wait...");
//                        object.notify(); // 恢复线程
//                    }
                    break;
                case 2:
                    //updateState.setText("发送升级请求");
                    break;
                case 3:
                    //Toast.makeText(getApplicationContext(), "升级状态冲突？", Toast.LENGTH_LONG).show();
                    break;
                case 4:
                    Log.e("异常","数据丢失");
                    //Toast.makeText(getApplicationContext(), "文件大小异常？", Toast.LENGTH_LONG).show();
                    break;
                case 5:
                    //Toast.makeText(getApplicationContext(), "请确认蓝牙连接状态？", Toast.LENGTH_LONG).show();
                    break;
                case 6:
                    //Toast.makeText(getApplicationContext(), "请确认升级文件存在根目录？", Toast.LENGTH_LONG).show();
                    break;
                case 7:
                    //Toast.makeText(getApplicationContext(), "请确认网络连接？", Toast.LENGTH_LONG).show();
                case 8:
                    //if(!list.isEmpty())
                        //showSingleChoiceButton();
                    break;
                case 9:
                    if(filedataLen == 0) {
                        //updateState.setText("升级异常，请重试！！！");
                    }else {
                        //updateState.setText("升级成功！");
                        imageNum = 0;           //BUG——+++++++++++++++++++++++++++++++++++++++++++++++++++++
                    }
                    break;
            }
        }
    };

    public Boolean WriteComm(BluetoothGattCharacteristic WriteCharacteristic, byte[] SendData, int DateCount)
    {
        Boolean bool = false;
        int count = 0;
        if(DateCount>20){
            for(int i = 0;i<DateCount;i=i+20)
            {
                bool = WriteCharacteristic.setValue(UpdateOpt.subBytes(SendData, i, 20));
                //PrintLog.printHexString("Gatt写长数据",WriteCharacteristic.getValue());
                WriteCharacterRspFlag = false;
                Log.i("写20个字节00000","写调用");
                BluetoothLeService.writeCharacteristic( bluetoothDevice, WriteCharacteristic);//BluetoothLeService.writeCharacteristic(WriteCharacteristic);
//                if (update_sendSize == 79968) {
//                    PrintLog.printHexString("当前数据为：", SendData);
//                }
                try {
                    Thread.currentThread().sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                long newtime = System.currentTimeMillis();  //開始時間
                while (!WriteCharacterRspFlag)
                {
//                    count++;
//                    if(count == 5) {
//                        count = 0;
//                        Log.i("发送数据：", "分段发送5次失败");
//                        break;
//                    }
                    if(System.currentTimeMillis() - newtime > 20){
                        Log.d("写数据等待回应","失败00000？");
                        newtime = System.currentTimeMillis();  //開始時間
                        //break;
                    }

                    //BluetoothLeService.writeCharacteristic(WriteCharacteristic);
                    try {
                        Thread.currentThread().sleep(9);
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
                BluetoothLeService.writeCharacteristic(bluetoothDevice ,WriteCharacteristic);
                WriteCharacterRspFlag = false;
                while (!WriteCharacterRspFlag)
                {
                    count++;
                    if(count == 4) {
                        count = 0;
                        Log.d("发送短数据：", "发送4次失败");
                        break;
                    }
                    BluetoothLeService.writeCharacteristic(bluetoothDevice ,WriteCharacteristic);
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
                Log.w("写特征值：", "本地写失败");
                bool = false;
            }
        }
        return bool;
    }

    int repeatcount = 0,repeatflag =0;
    public int update_Switch()
    {
        //startTime = System.currentTimeMillis();  //開始時間
        switch (update_step)
        {
            case UPDATE_STEP_SEND_REQUEST:
                //发送升级请求
                Log.i("发送升级请求：", "发送升级请求");
                sendMessage( 2 );
                receiveDataFlag = false;
                //while(!receiveDataFlag)
            {
                update_sendUpdateReq();
                try {
                    Thread.currentThread().sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            update_sendSize = 0;
            update_step++;
            startTime = System.currentTimeMillis();  //開始時間
            break;
            case UPDATE_STEP_SEND_IMAGE:
                Log.w("发送升级文件0：", "发送升级文件000000:"+String.valueOf(update_sendSize)
                        + ":"+String.valueOf(filedataLen) );
                //sendMessage( 3 );
                /* 发送升级数据 */
                if( update_sendSize >= filedataLen && update_sendSize>60000) {
                    update_step = UPDATE_STEP_WAIT_CRC_RES;
                    break;
                }
                if(filedataLen == 0)
                {
                    Log.e("文件大小异常:","退出发送");
                    updateFlag = false;
                }
                update_sendLen = update_sendImageData();
                if( update_sendLen < UPDATE_SEND_PAKET_SIZE && update_sendSize>60000) {
                    startTime = System.currentTimeMillis();  //開始時間
                    update_step = UPDATE_STEP_WAIT_CRC_RES;
                    break;
                }
                startTime = System.currentTimeMillis();  //開始時間
                if(repeatcount != update_sendSize)
                {
                    repeatcount = update_sendSize;
                    repeatflag = 0;
                }else{
                    repeatflag++;
                    if(repeatflag >5) {
                        Log.e("多次重发数据:", "得不到回应,认为死机");
                        updateFlag = false;
                    }
                }
                update_step++;
                break;
            case UPDATE_STEP_WAIT_REQUEST_RES:
                consumingTime = System.currentTimeMillis();
                if ((consumingTime - startTime) >= 2000)
                {
			        /* 超时重发 */
                    Log.w("发送升级请求：", "超时重发");
                    update_step = UPDATE_STEP_SEND_REQUEST;
                }
                break;
            case UPDATE_STEP_WAIT_IMAGE_RES:
                /* 等待升级请求和升级数据回应 */
                consumingTime = System.currentTimeMillis();
                if ((consumingTime - startTime) >= 900)
                {
			        /* 超时重发 */
                    Log.w("发送升级文件：", "超时重发");
                    update_step = UPDATE_STEP_SEND_IMAGE;
                }
                break;
            case UPDATE_STEP_WAIT_CRC_RES:
                /* 等待升级请求和升级数据回应 */
                consumingTime = System.currentTimeMillis();
                if ((consumingTime - startTime) >= 5000)
                {
                    /* 超时 */
                    /* 重启，认为升级成功 */
                    //mySetRecvInfo("升级完成");
                    Log.i("升级完成：", "升级完成");
                    update_step = UPDATE_STEP_CRC_RES_RECV;
                }
                break;
            case UPDATE_STEP_CRC_RES_RECV:
                Log.i("CRC校验正确", "升级完成");
                Log.i("升级完成", "升级完成");
                //升级成功
                break;
        }
        return update_step;
    }

    void update_sendUpdateReq()
    {
        byte temp[] = new byte[32];
        int requestId;
        int len;
        //supportCipher = false;
	/* 已发送数据大小 */
        update_sendSize = 0;
        len = 0;
        //发送升级请求，并等待回应
        //requestId = UPDATE_REQUEST_ID;
        //memcpy(&temp[len], &requestId, 2);
        temp[len] = (byte)0xFF;
        temp[len+1] = (byte)0xFF;
        len += 2;
        //memcpy(&temp[len], &tUpdate_info.hw_info, 4);
        System.arraycopy(Update_info.hw_info,0,temp,len,4);
        len += 4;
        //memcpy(&temp[len], &tUpdate_info.image_size, 4);
        System.arraycopy(Update_info.image_size,0,temp,len,4);
        len += 4;
        //memcpy(&temp[len], &tUpdate_info.image_crc, 4);
        System.arraycopy(Update_info.image_crc,0,temp,len,4);
        len += 4;
        //comm_send(COMM_TRANS_TYPE_SEND, COMM_CMD_TYPE_UPDATE, &temp[1], len-1);//只为唤醒目标机
        //Delay(50);
        comm_send(COMM_TRANS_TYPE_SEND, COMM_CMD_TYPE_UPDATE, temp, len);
    }

    /*-------------------------------------------------------------------------
* 函数: comm_send
* 说明: 发送
* 参数: pData---数据buffer
		len-----条码长度
* 返回: HY_OK------发送成功
		HY_ERROR---发送失败
-------------------------------------------------------------------------*/
    Boolean comm_send(byte transType, byte cmd, byte[] pData, int len)
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
//        if(update_sendSize == 79968 ) {
//            PrintLog.printHexString("当前数据为：", temp);
//            //update_sendSize += update_sendLen;
//        }
        WriteComm(writecharacteristic,temp, len+6);
        return true;
    }

    int update_sendImageData() {
        byte UPDATE_SEND_PAKET_SIZE = (112);//(100)//(112)//(32)//(12)//(14)//(64)//
        byte[] temp = new byte[UPDATE_SEND_PAKET_SIZE + 2];
        int imageReadLen = 0;
        int index;

        index = (update_sendSize / UPDATE_SEND_PAKET_SIZE) + 1;
        //memcpy(&temp[0], &index, sizeof(U16));
        temp[0] = (byte) (index >> 8 * 0 & 0xFF);
        temp[1] = (byte) (index >> 8 * 1 & 0xFF);

        imageReadLen = update_readImageData(temp, update_sendSize, UPDATE_SEND_PAKET_SIZE);

        Boolean wait = (update_sendSize+112)/1024-(update_sendSize/1024)>0;
        if(wait)
        {
            //PrintLog.printHexString("当前数据为：", temp);
            try {
                Thread.currentThread().sleep(400);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else{
            try {
                Thread.currentThread().sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (imageReadLen <= 0)
        {
		/* 升级数据发送完成 */
            return 0;
        }

        comm_send(COMM_TRANS_TYPE_SEND, COMM_CMD_TYPE_UPDATE, temp, imageReadLen+2);

        return imageReadLen;
    }

/**-------------------------------------------------------------------------
* 函数: update_readImageData
* 说明: 读取image数据
* 参数: ptUpdataInfo
* 返回: 读取大小
-------------------------------------------------------------------------*/
    int update_readImageData(byte pData[], int offset, int len)
    {
        int readLen;
        byte[] senddata=new byte[UPDATE_SEND_PAKET_SIZE +6];

        if (offset >= filedataLen)
        {
            //readLen = 0;
            return -1;
        }
        else if ((offset+len) > filedataLen)
        {
            readLen = filedataLen - offset;
        }
        else
        {
            readLen = len;
        }

        System.arraycopy(Update_info.image_data,offset,senddata,0,readLen);
        System.arraycopy(senddata , 0, pData, 2 , readLen);

        return readLen;
    }

    void updateReceive_respons(byte[] pdata, int len)
    {
        int index, offset;
        int respons;
        int ret;

        offset = 4;
        //memcpy(&index, &pdata[offset], sizeof(U16));
        index = (pdata[offset] & 0xFF) | (pdata[offset+1] & 0x00FF)<<8 ;
        offset += 2;
        switch (index)
        {
            case UPDATE_REQUEST_ID:
                Log.i("解析升级请求数据....","解析升级请求数据");
                if (len > 3) ret = myNative.update_checkSetFlag(1);
                else ret = myNative.update_checkSetFlag(0);
                if (ret == 0)
                {
                    if (pdata[offset] == UPDATE_REJECT_REASON_HW_ERR)
                    {
				        /* 硬件版本错误 */
                        Log.w("硬件版本错误0....","重新发送升级请求");
                        imageIndex++;
                        if (imageIndex >= imageNum) imageIndex = 0;
                    }
                    ret = myNative.update_getImageInfo(imageIndex, Update_info.ppVer_Str,
                            Update_info.hw_info,
                            Update_info.image_size,
                            Update_info.image_crc,
                            Update_info.image_data);
                    filedataLen = UpdateOpt.byteArrayToInt(Update_info.image_size);
                    Log.w("更换升级文件0：=",String.valueOf(imageIndex) +":" + String.valueOf(filedataLen));
                    return;
                }
		        /* 接收升级请求回应 */
                switch(pdata[offset])
                {
                    case UPDATE_REQUST_OK:
			        /* 升级请求被接受 */
                        Log.i("请求被接收....","请求被接收");
                        try {
                            Thread.currentThread().sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        update_step = UPDATE_STEP_SEND_IMAGE;
                        if (len > 3)
                        {
                            Log.i("芯片支持OAD0....","芯片支持OAD00");
                            supportCipher = true;
                        }
                        else
                        {
                            Log.w("不支持OAD....","");
                            supportCipher = false;
                        }

                        break;
                    case UPDATE_REJECT_REASON_HW_ERR:
			        /* 硬件版本错误 */
                        Log.w("硬件版本错误0011....","硬件版本错误");
                        imageIndex++;
                        if (imageIndex >= imageNum) imageIndex = 0;
                        ret = myNative.update_getImageInfo(imageIndex,Update_info.ppVer_Str,
                                Update_info.hw_info,
                                Update_info.image_size,
                                Update_info.image_crc,
                                Update_info.image_data);
                        filedataLen = UpdateOpt.byteArrayToInt(Update_info.image_size);
                        Log.w("更换升级文件0011：=",String.valueOf(imageIndex) +":" + String.valueOf(filedataLen));
                        break;
                    case UPDATE_REJECT_REASON_SIZE_ERR:
			        /* 升级包大小错误(超过限制) */
                        Log.w("升级包大小错误(超过限制)","超过限制");
                        break;
                }
                break;
            case UPDATE_CRC_RESP_ID:
		    /* 收到CRC校验回应 */
                if (pdata[offset] == 0)
                {
			        /* CRC校验正确 */
                    //mySetRecvInfo("CRC校验正确");
                    Log.i("CRC校验正确0","CRC校验正确0");
                    update_step = UPDATE_STEP_CRC_RES_RECV;
                    PrintLog.printHexString("输出CRC信息------",Update_info.image_crc);
                    sendMessage(9);
                }
                else
                {
			        /* 校验值错误，重发升级请求，重新升级 */
                    Log.w("校验值错误000，重发升级请求，重新升级","校验值错误");
                    update_step = UPDATE_STEP_SEND_REQUEST;
                    update_sendSize = 0;

                }
                break;
            default:
		    /* 升级数据包回应 */
                if(pdata[offset] == 0 && index == (update_sendSize/UPDATE_SEND_PAKET_SIZE)+1 )
                {
			        /* 数据包被正确接收 */
                    update_sendSize += update_sendLen;
                    int persent = (int)Math.floor(100*((double)update_sendSize/filedataLen));

                    //myProgress.setProgress(persent);
                    myProgress = persent;

                    if (update_sendSize >= filedataLen && filedataLen>60000)
                    {
				        /* 数据包发送完成，等待CRC校验结果 */
                        Log.i("数据包发送完成，等待CRC校验结果","等待CRC校验");
                        update_step = UPDATE_STEP_WAIT_CRC_RES;
                        break;
                    }
                }
                else
                {
                    Log.w("数据包接收错误000，重发","数据包接收错误");
			        /* 数据包接收错误，重发 */
                }
                update_step --;         //有待查看,影响相应速度????????????????????????????????????????
                break;
        }

    }

}
