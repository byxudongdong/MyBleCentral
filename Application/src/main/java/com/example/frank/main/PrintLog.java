package com.example.frank.main;

import java.io.ByteArrayOutputStream;

/**
 * Created by byxdd on 2016/6/14 0014.
 */
public class PrintLog {

    /**
     * 将指定byte数组以16进制的形式打印到控制台
     *
     * @param hint
     *            String
     * @param b
     *            byte[]
     * @return void
     */
    public static void printHexString(String hint, byte[] b)
    {
        System.out.print(hint);
        if(b == null) return;
        for (int i = 0; i < b.length ;i++)
            {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1)
            {
                hex = '0' + hex;
            }
            System.out.print(hex.toUpperCase() + " ");
        }
        System.out.println("");
    }

    public static String returnHexString(byte[] b)
    {
        String backUp ="";
        if(b == null) return "";
        for (int i = 0; i < b.length ;i++)
        {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1)
            {
                hex = '0' + hex;
            }
            backUp = backUp + hex.toUpperCase() + " ";
        }
        return backUp;
    }

    public static String stringToHexString(String strPart) {
        String hexString = "";
        for (int i = 0; i < strPart.length(); i++) {
            int ch = (int) strPart.charAt(i);
            String strHex = Integer.toHexString(ch);
            hexString = hexString + strHex;
        }
        return hexString;
    }

    private static String hexString="0123456789ABCDEF";

    public static String encode(String str)
    {
        // 根据默认编码获取字节数组
        byte[] bytes=str.getBytes();
        StringBuilder sb=new StringBuilder(bytes.length*2);
        // 将字节数组中每个字节拆解成2位16进制整数
        for(int i=0;i<bytes.length;i++)
        {
            sb.append(hexString.charAt((bytes[i]&0xf0)>>4));
            sb.append(hexString.charAt((bytes[i]&0x0f)>>0));
        }
        return sb.toString();
    }

    public static String decode(String bytes)
    {
        ByteArrayOutputStream baos=new ByteArrayOutputStream(bytes.length()/2);
        // 将每2位16进制整数组装成一个字节
        for(int i=0;i<bytes.length();i+=2)
            baos.write((hexString.indexOf(bytes.charAt(i))<<4 |hexString.indexOf(bytes.charAt(i+1))));
        return new String(baos.toByteArray());
    }

    private static byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[]{src0})).byteValue();
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[]{src1})).byteValue();
        byte ret = (byte) (_b0 | _b1);

        return ret;
    }

    public static byte[] HexString2Bytes(String src)
    {
        byte[] ret = new byte[6];
        byte[] tmp = src.getBytes();
        for (int i = 0; i < 6; ++i) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
        }
        return ret;
    }

        // ====================================================================

        //java 格式化输出十六进制数
        // 以16进制输出文件内容, 每16个数换行一次
    public static void printHex(byte[] chBuf,int nLen) {

        for (int i = 0; i < nLen; i++) {
            if (i % 16 == 0)
                System.out.println();
            String strHex = new String();
            strHex = Integer.toHexString(chBuf[i]).toUpperCase();
            if (strHex.length() > 3)
                System.out.print(strHex.substring(6));
            else if (strHex.length() < 2)
                System.out.print("0" + strHex);
            else
                System.out.print(strHex);

            System.out.print(" ");
        }
    }
}
