package com.example.webrtcb;

import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.RequiresApi;

import java.io.IOException;

public class LocalSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private Camera camera;
    private Camera.Size cameraSize;

    // 知道了宽高，就能知道一帧总字节大小
    private byte[] buffer;


    private CameraH264Encod cameraH264Encod;

    public LocalSurfaceView(Context context) {
        super(context);
    }

    public LocalSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 创建surface回调
        getHolder().addCallback(this);
    }

    public LocalSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // 摄像头预览
    private void startPerview() {

        // 打开前置摄像头
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);

        // 获取预览大小尺寸
        Camera.Parameters parameters = camera.getParameters();
        cameraSize = parameters.getPreviewSize();

        try {
            camera.setPreviewDisplay(getHolder());
            // 显示画面旋转
            camera.setDisplayOrientation(90);
            // 假设这张图像yuv的比例为：4:1:1，则大小为：width*height(y的总大小) + width*height/4(u的总大小) + width*height/4(ｖ的总大小)
            buffer = new byte[cameraSize.width* cameraSize.height*3/2];

            camera.addCallbackBuffer(buffer);
            camera.setPreviewCallbackWithBuffer(this);
            camera.startPreview();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // surface创建成功，开始摄像头预览
        startPerview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // 摄像头实时返回的画面帧数据，格式是YUV，数据是横屏时的数据。所以编码时需要宽高交换
        if (cameraH264Encod == null) {
            cameraH264Encod = new CameraH264Encod(cameraSize.width, cameraSize.height);
            // 启动MediaCodec编码器
            cameraH264Encod.startLive();
        }
        // 将摄像头采集到的帧画面数据送到mediacodec进行编码
        cameraH264Encod.encodeFrame(data);

        //重新设置监听
        camera.addCallbackBuffer(data);
    }
}
