package neurosky.com.smartdesk.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import neurosky.com.smartdesk.activity.RegDeviceActivity;
import neurosky.com.smartdesk.manager.ConnectManager;

public class BluetoothService extends Service implements ConnectManager.OnBluetoothListener {

    private static final String TAG = BluetoothService.class.getSimpleName();

    public static final int SEND_DATA = 200;
    public static final String ACTION_RECEIVED_DATA = BluetoothService.class.getName() + ".Received";
    public static final String FLAG_DATA = "data";

    private ConnectManager connectManager;

    final Messenger messenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "스마트 데스크 서비스와 연결 완료!");
        return messenger.getBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (connectManager == null) {
            connectManager = ConnectManager.getInstance(getContext());
            connectManager.setOnBluetoothListener(this);
        }
        try {
            if (!connectManager.isBluetoothEnable()) {
                Toast.makeText(getContext(), "블루투스를 켜주세요.", Toast.LENGTH_SHORT).show();
                stopSelf();
                return;
            }
            connectManager.setConnectDevice();
            connectManager.connect();
        } catch (ConnectManager.NotSavedDeviceException e) {
            startRegDeviceActivity();
            stopSelf();
        }
    }

    private void startRegDeviceActivity() {
        Intent intent = new Intent(getContext(), RegDeviceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onConnected() {

        Toast.makeText(getApplicationContext(), "스마트 테이블과 연결되었습니다.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onReceive(String data) {
        Intent intent = new Intent(ACTION_RECEIVED_DATA);
        intent.putExtra(FLAG_DATA, data);
        sendBroadcast(intent);
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SEND_DATA:
                    connectManager.sendData(msg.getData().getString(FLAG_DATA, null));
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private Context getContext() {
        return this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        connectManager.close();
    }
}
