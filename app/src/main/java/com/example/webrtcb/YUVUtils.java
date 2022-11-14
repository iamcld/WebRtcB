package com.example.webrtcb;

import android.util.Log;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class YUVUtils {
    // 摄像头预览的宽高正常，但是采集到的宽高数据是反的，所以需要旋转宽高数据
    public static void portraitData2Raw(byte[] data, byte[] output, int width, int height) {
        int y_len = width * height;
        int uvHeight = height >> 1; // uv数据高为y数据的一半
        int k = 0;
        for(int j = 0; j < width; j++) {
            for (int i = height - 1; i >= 0; i--) {
                output[k++] = data[width*i + j];
            }
        }
        for(int j = 0; j < width; j += 2) {
            for (int i = uvHeight -1; i >= 0; i--) {
                output[k++] = data[y_len + width * i + j];
                output[k++] = data[y_len + width * i + j + 1];
            }
        }
    }

    // nv12也叫做yuv420。只有Android手机摄像头采集到的数据是nv21，需要把nv21转换成nv12格式
    // nv12格式为：yuv比例为4:1:1。y数据4行8列，共32字节。 u单独1行8列 uuuuuuuu，共8字节，v单独1行8列共8字节,vvvvvvvv
    // nv21格式为：yuv比例为4:1:1。y数据4行8列，共32字节。 u和v2行8列，共16字节，交叉排序uvuvuvuvuv........
    public static byte[] nv21ToNv12(byte[] nv21) {
        byte[] nv12;
        int size = nv21.length;

        nv12 = new byte[size];
        int yLen = size * 2/3;// yuv比例为4：1：1，所以一组yuv数据中，y的长度为2/3
        System.arraycopy(nv21, 0, nv12, 0, yLen);//将原nv21中y数据拷贝到nv12中

        int i = yLen;
        while (i < size -1) {
            nv12[i] = nv21[i+1];
            nv12[i+1] = nv21[i];
            i += 2;
        }
        return nv12;
    }

    public static void writeBytes(String fileName, byte[] array) {
        FileOutputStream writer = null;
        try {
            //ture表示以追加方式写文件
//            writer = new FileOutputStream(Environment.getExternalStorageState() + "/codec.h264", true);
            writer = new FileOutputStream(fileName, true);

            writer.write(array);
            writer.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeContent(String fileName, byte[] array) {
        char[] HEX_CHAR_TABLE = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        StringBuilder sb = new StringBuilder();

        for (byte b : array) {
            sb.append(HEX_CHAR_TABLE[(b & 0xf0) >> 4]);
            sb.append(HEX_CHAR_TABLE[b & 0x0f]);
        }

        Log.i("wirte--->", "writContent:" + sb.toString());
        FileWriter writer = null;
        try {
//            writer = new FileWriter(Environment.getExternalStorageState() + "/codecH264.txt", true);
            writer = new FileWriter(fileName, true);

            writer.write(sb.toString());
            writer.write('\n');

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

