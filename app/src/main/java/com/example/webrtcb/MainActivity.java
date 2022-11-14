package com.example.webrtcb;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class MainActivity extends AppCompatActivity  implements SocketLiveSever.SocketCallback{

    LocalSurfaceView localSurfaceView;
    SurfaceView removeSurfaceView;
    Surface surface;
    H264DecodecPlayer h264DecodecPlayer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        checkPermissions();

    }

    private void initView() {
        localSurfaceView = findViewById(R.id.localSurfaceView);
        removeSurfaceView = findViewById(R.id.removeSurfaceView);
        removeSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                surface = holder.getSurface();
                h264DecodecPlayer = new H264DecodecPlayer();
                h264DecodecPlayer.init(surface);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

    }


    public void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
            }, 1);

        }
    }


    // 会一直被网络调用，接收另外设备的音视频数据
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void callBack(byte[] data) {
        if (h264DecodecPlayer != null) {
            // 把网络端的音视频放到解码层进行解码
            h264DecodecPlayer.callBack(data);
        }
    }

    public void connect(View view) {
    }
}