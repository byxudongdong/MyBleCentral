package com.example.frank.main;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpUser
{

    public static String getJsonContent(String urlStr)
    {
        try
        {// 获取HttpURLConnection连接对象
            URL url = new URL(urlStr);
            HttpURLConnection httpConn = (HttpURLConnection) url
                    .openConnection();
            // 设置连接属性
            httpConn.setConnectTimeout(3000);
            httpConn.setDoInput(true);
            httpConn.setRequestMethod("GET");
            // 获取相应码
            int respCode = httpConn.getResponseCode();
            Log.i("网络状态代码：", String.valueOf(respCode));
            if (respCode == 200)
            {
                Log.i("网络通畅","服务器回应信息");
                return ConvertStream2Json(httpConn.getInputStream());
            }
        }
        catch (MalformedURLException e)
        {
            // TODO Auto-generated catch block
            Log.i("网络异常","服务器找不到");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            Log.i("网络异常","本地错误");
            e.printStackTrace();
        }
        return "";
    }


    private static String ConvertStream2Json(InputStream inputStream)
    {
        String jsonStr = "";
        // ByteArrayOutputStream相当于内存输出流
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        // 将输入流转移到内存输出流中
        try
        {
            while ((len = inputStream.read(buffer, 0, buffer.length)) != -1)
            {
                out.write(buffer, 0, len);
            }
            // 将内存流转换为字符串
            jsonStr = new String(out.toByteArray());
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return jsonStr;
    }
}
