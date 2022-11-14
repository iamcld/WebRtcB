package com.example.webrtcb;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;

public class H264DecodecPlayer implements SocketLiveSever.SocketCallback {
    private static final String TAG = H264DecodecPlayer.class.getSimpleName();
    private MediaCodec mediaCodec;

    public void init(Surface surface) {
        try {
            // 创建H264解码器
            mediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 1080, 1920);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);//帧率，1秒钟20帧
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);// 设置I帧间隔。每隔30帧有1个I帧
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1080*1920);//设置码率，码率越高，视频越清晰.编码文件越大

            mediaCodec.configure(mediaFormat, surface, null, 0);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //服务端A设备推给客户端B就是1帧数据，所以无需做读取分隔符的操作
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void callBack(byte[] data) {
        Log.d(TAG, "解码前长度：" + data.length);
        int index = mediaCodec.dequeueInputBuffer(10000);
        if (index >= 0) {
            ByteBuffer inputBuffer = mediaCodec.getInputBuffer(index);
            inputBuffer.clear();
            // 将帧数据放到容器中
            inputBuffer.put(data, 0, data.length);

            // 将数据输入到dps中，对帧数据进行解码
            mediaCodec.queueInputBuffer(index, 0, data.length, System.currentTimeMillis(), 0);
        }


        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        // 取出解码后dsp中解码的帧数据
        int outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 100000);//dsp解码后的数据长度

        // 解码后的长度 = 宽*高*3/2 。这里是YUV420格式
        Log.d(TAG, "解码后长度：" + bufferInfo.size);
        // 1帧的数据很大时，解码器没办法一次性解出，需要分批次解。所以用while。当解码器把1帧数据都解完成dequeueOutputBuffer返回的值才小于0，
        while (outputIndex >= 0) {
            mediaCodec.releaseOutputBuffer(outputIndex, true);
            // 帧数据过大时，解码器分批次解
            outputIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);//dsp解码后的数据长度
        }
    }
}
