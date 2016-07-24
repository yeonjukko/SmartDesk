package neurosky.com.smartdesk.service;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import neurosky.com.smartdesk.activity.RegDeviceActivity;
import neurosky.com.smartdesk.manager.ConnectManager;

public class BluetoothService extends Service implements ConnectManager.OnBluetoothListener {

    private static final String TAG = BluetoothService.class.getSimpleName();

    public static final int SEND_DATA = 200;
    public static final int CONNECT_DEVICE = 300;

    public static final String ACTION_RECEIVED_DATA = BluetoothService.class.getName() + ".Received";
    public static final String ACTION_CONNECT_DEVICE = BluetoothService.class.getName() + ".Connected";
    public static final String ACTION_DISCONNECT_DEVICE = BluetoothService.class.getName() + ".Disconnected";
    public static final String ACTION_ERROR = BluetoothService.class.getName() + ".Error";

    public static final String FLAG_DATA = "data";
    public static final String FLAG_ERROR = "error";

    public static final String FLAG_TEMPERATURE = "temperature";
    public static final String FLAG_HUMIDITY = "humidity";
    public static final String FLAG_LIGHT = "light";


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
            connectManager.onCreate();
            connectManager.setOnBluetoothListener(this);
        }
    }


    @Override
    public void onConnected() {
        sendConnected();
        Log.d(TAG, "onConnected");
    }

    @Override
    public void onReceive(String data) {
        sendReceiveData(data);
        Log.d(TAG, "onReceive:" + data);
    }

    @Override
    public void onError(Exception e) {
        sendError(e);
        Log.d(TAG, "onError" + e.getCause());
    }

    @Override
    public void onDisconnected() {
        sendDisconnected();
        Log.d(TAG, "onDisconnected");
    }

    private void sendDisconnected() {
        Intent intent = new Intent(ACTION_DISCONNECT_DEVICE);
        sendBroadcast(intent);
    }

    private void sendError(Exception e) {
        Intent intent = new Intent(ACTION_ERROR);
        intent.putExtra(FLAG_ERROR, e);
        sendBroadcast(intent);
    }

    private void sendConnected() {
        sendBroadcast(new Intent(ACTION_CONNECT_DEVICE));
    }

    private void sendReceiveData(String data) {
        Intent intent = new Intent(ACTION_RECEIVED_DATA);
        Bundle result = ConnectManager.getParseStatus(data);
        if (result != null) {
            intent.putExtras(result);
            sendBroadcast(intent);
        } else {
            Log.d(TAG, "온도 습도 조도 파싱 실패(Data:" + data + ")");
        }
    }

    private void startRegDeviceActivity() {
        Intent intent = new Intent(getContext(), RegDeviceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SEND_DATA:
                    connectManager.sendData(msg.getData().getString(FLAG_DATA, null));
                    break;
                case CONNECT_DEVICE:
                    connectBluetoothDevice();
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
        connectManager.onDestroy();
        connectManager = null;
    }

    public void connectBluetoothDevice() {
        try {
            if (!connectManager.isBluetoothEnable()) {
                Toast.makeText(getContext(), "블루투스를 켜주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!connectManager.isConnected()) {
                connectManager.setConnectDevice();
                connectManager.connect();
            }else{
                sendConnected();
            }
        } catch (ConnectManager.NotSavedDeviceException e) {
            startRegDeviceActivity();
        }
    }

    public static IntentFilter getIntentFilterSet() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_RECEIVED_DATA);
        filter.addAction(ACTION_CONNECT_DEVICE);
        filter.addAction(ACTION_DISCONNECT_DEVICE);
        filter.addAction(ACTION_ERROR);
        return filter;
    }

}
