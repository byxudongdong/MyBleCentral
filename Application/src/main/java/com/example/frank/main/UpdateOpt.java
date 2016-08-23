package com.example.frank.main;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Environment;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

//import org.apache.http.util.EncodingUtils;

/**
 * Created by byxdd on 2016/6/17 0017.
 */
public class UpdateOpt {
    public static final byte[] wakeupData = {0x40, 0x05, 0x53, 0x01, (byte) 0xAA, (byte) 0xAA, 0x57, 0x2A};
    public static final byte[] NullRecData = {0x40, 0x40, 0x2A, 0x2A,};

    public static final int UpdateStepSendRequst = 0,
            UpdateStepWaitRequestRes = 1,
            UpdateStepSendImage = 2,
            UpdateStepWaitImageRes = 3,
            UpdateStepWaitCRCRes = 4,
            UpdateStepCRCResRecv = 5;
    static int updateIdex,update_step = UpdateStepSendRequst;
    static long  startTime,consumingTime;  //開始時間


    //写数据到SD中的文件
    public void writeFileSdcardFile(String fileName, String write_str) throws IOException {
        try{

            FileOutputStream fout = new FileOutputStream(fileName);
            byte [] bytes = write_str.getBytes();

            fout.write(bytes);
            fout.close();
        }

        catch(Exception e){
            e.printStackTrace();
        }
    }


    //读SD中的文件
    public static byte[] readFileSdcardFile(String fileName) throws IOException {
        String res="";
        byte [] buffer = null;
        try{
            FileInputStream fin = new FileInputStream(getSdCardPath() + fileName);
            int length = fin.available();
            buffer = new byte[length];
            fin.read(buffer);
            //res = EncodingUtils.getString(buffer, "UTF-8");
            fin.close();
        } catch(Exception e){
            e.printStackTrace();
        }
        return buffer;
    }

    /**
     * 获取SD卡根目录路径
     *
     * @return
     */
    public static String getSdCardPath() {
        boolean exist = isSdCardExist();
        String sdpath = "";
        if (exist) {
            sdpath = Environment.getExternalStorageDirectory()
                    .getAbsolutePath();
        } else {
            sdpath = "不适用";
        }
        return sdpath;

    }


    /**
     * 判断SDCard是否存在 [当没有外挂SD卡时，内置ROM也被识别为存在sd卡]
     *
     * @return
     */
    public static boolean isSdCardExist() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    /**
     * 从一个byte[]数组中截取一部分
     * @param src
     * @param begin
     * @param count
     * @return
     */
    public static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        for (int i=begin; i<begin+count; i++) {
            if(i>=src.length)
            {
                break;
            }
            bs[i - begin] = src[i];
        }
        return bs;
    }

    //byte 数组与 int 的相互转换
    public static int byteArrayToInt(byte[] b) {
        return   b[0] & 0xFF |
                (b[1] & 0xFF) << 8 |
                (b[2] & 0xFF) << 16 |
                (b[3] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a) {
        return new byte[] {
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

}
