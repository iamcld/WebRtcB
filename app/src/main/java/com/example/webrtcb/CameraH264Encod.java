package com.example.webrtcb;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;

public class CameraH264Encod {
    private static final String TAG = "CameraH264Encod";
    MediaCodec mMediaCodec;
    int frameIndex;
    int width;
    int height;
    // 射线头采集到的yuv数据旋转90度，即旋转宽高
    byte[] yuv;

    // nv12也叫做yuv420。只有Android手机摄像头采集到的数据是nv21，需要把nv21转换成nv12格式
    // nv12格式为：yuv比例为4:1:1。y数据4行8列，共32字节。 u单独1行8列 uuuuuuuu，共8字节，v单独1行8列共8字节,vvvvvvvv
    // nv21格式为：yuv比例为4:1:1。y数据4行8列，共32字节。 u和v2行8列，共16字节，交叉排序uvuvuvuvuv........
    byte[] nv12;

    public static final int NAL_I = 5;//i帧
    public static final int NAL_SPS = 7;//sps帧

    // 保存H264文件开头是sps+pps
    private byte[] sps_pps_buf;

    SocketLiveSever socketLive;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public CameraH264Encod(int width, int height, SocketLiveSever.SocketCallback socketCallback) {
        socketLive = new SocketLiveSever(socketCallback);
        socketLive.start();
        this.width = width;
        this.height = height;
    }

    public void startLive() {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        // 旋转宽高需要转换宽高位置
//        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, height, width);
        try {
            // 创建H264编码器
            mMediaCodec = MediaCodec.createEncoderByType("video/avc");
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);//帧率，1秒钟15帧
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);// 每2秒1个I帧
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width*height);//设置码率，码率越高，视频越清晰.编码文件越大
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            nv12 = new byte[width*height*3/2];
            yuv = new byte[width*height*3/2];
            mMediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // 数据流：摄像头-》CPU-》给到dsp进行编码-》编码后的数据重新给到cpu
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public int encodeFrame(byte[] input) {
        // Android手机摄像头采集到的数据是nv21，需要把nv21转换成nv12格式
        nv12 = YUVUtils.nv21ToNv12(input);

        // 将nv12宽高旋转
        YUVUtils.portraitData2Raw(nv12, yuv, width, height);

        // 输入,需要对input数据进行处理，不然画面是横的
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(10000);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        if(inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
            inputBuffer.clear();
            inputBuffer.put(yuv);
            mMediaCodec.queueInputBuffer(inputBufferIndex, 0, yuv.length,
                    computPts(), 0);
            frameIndex++;
        }

        // 输出
        int outIndex = mMediaCodec.dequeueOutputBuffer(info, 10000);
        if (outIndex >= 0) {
            // 拿到编码后的数据
            ByteBuffer byteBuffer = mMediaCodec.getOutputBuffer(outIndex);

            // 网络传输功能
            byteBuffer.rewind();
            dealFram(byteBuffer, info);

            // 投屏功能
            byte[] ba = new byte[info.size];
//                byte[] ba = new byte[byteBuffer.remaining()];
            byteBuffer.rewind();
            byteBuffer.get(ba);// 调用此语句，byteBuffer里面的数组指针position会跟着移动ba.length大小，
            YUVUtils.writeBytes("/sdcard/codec.h264",ba);
            YUVUtils.writeContent("/sdcard/codecH264.txt",ba);


            mMediaCodec.releaseOutputBuffer(outIndex, false);
        }

        return -1;

    }
    /**
     * pts时间
     * 假设帧率是1秒钟15帧，所以第1帧的播放时间是 100 000(微秒) /15
     * 第2帧的播放时间是 100 000(微秒)/15 * 2
     * 第3帧的播放时间是 100 000(微秒)/15 *3
     */
    public int computPts() {
        return 1000000/15 * frameIndex;
    }

    public void dealFram(ByteBuffer bb, MediaCodec.BufferInfo bufferInfo) {
        // ox67 二进制为8位，分为3部分：
        //  第一位表示这一帧是否可用 0 可用，1不可用
        // 第二 三位表示重要性
        // 后5位才表示帧类型

        // 分隔符由00 00 00 00 01 和 00 00 00 01 两种
        // 默认分隔符是00 00 00 00 01
        // 帧类型信息所在的位数，偏移量4处为 帧类型信息所在的位数67
        int offset = 4;
        if (bb.get(2) == 0x01) {
            // 分隔符是00 00 00 01，偏移量3处为 帧类型信息所在的位数67
            offset = 3;
        }

        // 取得帧类型信息
        int type = (bb.get(offset) & 0x1f);

        // 此句代码关键，若要重新读取bytebuffer中数据，需要调用此句，对byteoffer里面的数组下标复位
        bb.rewind();

        // type为7，代表sps帧类型，编码器只会输出1次，故要保存起来
        if(type == NAL_SPS) {
            Log.d(TAG, "dealFram NAL_SPS "+ bufferInfo.size);

            // 第一次时，就会缓存sps+pps帧数据
            sps_pps_buf = new byte[bufferInfo.size];//存放sps+pps帧数据
            bb.get(sps_pps_buf);

        } else if (type == NAL_I) {
            Log.d(TAG, "dealFram NAL_I ------------------> "+ bufferInfo.size);
            byte[] bytes = new byte[bufferInfo.size];// 存放I帧数据
            bb.get(bytes);
            byte[] newBuf = new byte[sps_pps_buf.length + bytes.length];// 存放sps+pps+i帧数据
            System.arraycopy(sps_pps_buf, 0, newBuf, 0, sps_pps_buf.length);//拷贝sps+pps数据到新容器newBuf中
            System.arraycopy(bytes, 0, newBuf, sps_pps_buf.length, bytes.length);//拷贝I数据到新容器newBuf中，这样新容器的数据就是sps+pps+I帧数据
            // 2台设备双向发送音视频数据，实现对讲功能
            socketLive.sendData(newBuf);
        } else {
            Log.d(TAG, "dealFram Nor NAL_I " + bufferInfo.size);
            // 非I、sps、pps帧，直接发送
            byte[] bytes = new byte[bufferInfo.size];
            bb.get(bytes);
            socketLive.sendData(bytes);
        }
    }
}
