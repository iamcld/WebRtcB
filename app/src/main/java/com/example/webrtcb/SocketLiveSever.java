package com.example.webrtcb;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;

// 音视频通话客户端
public class SocketLiveSever {
    private static final String TAG = SocketLiveSever.class.getSimpleName();
    private SocketCallback socketCallback;
    private WebSocket webSocket;

    public SocketLiveSever(SocketCallback socketCallback) {
        this.socketCallback = socketCallback;

    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void start() {
        // 此地址为手机的ip地址。2台设备需要连接同一个wifi，保持同一个网段
        webSocketServer.start();
    }

    public void close() {
        try {
            webSocket.close();
            webSocketServer.stop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendData(byte[] bytes) {
        if (webSocket != null && webSocket.isOpen()) {
            webSocket.send(bytes);
        }
    }



    // 服务端，负责把A设备上偷屏的数据发到b设备上。B设备可以看到A设备的投屏画面
    private WebSocketServer webSocketServer = new WebSocketServer(new InetSocketAddress(9007)) {
        @Override
        public void onOpen(WebSocket webSocket, ClientHandshake handshake) {
            SocketLiveSever.this.webSocket = webSocket;
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {

        }

        @Override
        public void onMessage(WebSocket conn, String message) {
        }

        @Override
        public void onMessage(WebSocket conn, ByteBuffer bytes) {
            Log.d(TAG, "消息长度：" + bytes.remaining());
            byte[] buf = new byte[bytes.remaining()];
            socketCallback.callBack(buf);
        }


        @Override
        public void onError(WebSocket conn, Exception ex) {

        }

        @Override
        public void onStart() {

        }
    };


    public interface SocketCallback {
        void callBack(byte[] data);
    }
}
