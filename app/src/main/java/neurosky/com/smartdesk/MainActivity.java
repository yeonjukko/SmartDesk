package neurosky.com.smartdesk;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.widget.TextView;

import neurosky.com.smartdesk.activity.SmartDeskActivity;
import neurosky.com.smartdesk.manager.ConnectManager;
import neurosky.com.smartdesk.service.BluetoothService;

public class MainActivity extends SmartDeskActivity implements View.OnClickListener {

    private TextView textViewReceive;
    private TextView textViewStatus;

    private BroadcastReceiver receiver;
    private Messenger messenger;

    private ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            messenger = new Messenger(service);
            textViewReceive.setEnabled(true);
        }

        public void onServiceDisconnected(ComponentName className) {
            messenger = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViewReceive = (TextView) findViewById(R.id.tv_receive);
        textViewReceive.setEnabled(false);
        textViewStatus = (TextView) findViewById(R.id.tv_status);

        findViewById(R.id.bt_connect).setOnClickListener(this);
        findViewById(R.id.bt_1).setOnClickListener(this);
        findViewById(R.id.bt_2).setOnClickListener(this);
        findViewById(R.id.bt_3).setOnClickListener(this);
        findViewById(R.id.bt_4).setOnClickListener(this);
        findViewById(R.id.bt_5).setOnClickListener(this);
        findViewById(R.id.bt_6).setOnClickListener(this);
        findViewById(R.id.bt_7).setOnClickListener(this);
        findViewById(R.id.bt_8).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        regReceiver();
        bindService(new Intent(getContext(), BluetoothService.class), connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregReceiver();
        unbindService(connection);
    }

    private void regReceiver() {
        receiver = new BluetoothReceiver();
        registerReceiver(receiver, BluetoothService.getIntentFilterSet());
    }

    private void unregReceiver() {
        unregisterReceiver(receiver);
        receiver = null;
    }

    @Override
    public void onClick(View view) {
        Message msg = Message.obtain(null, BluetoothService.SEND_DATA, 0, 0);
        Bundle bundle = new Bundle();
        switch (view.getId()) {
            case R.id.bt_connect:
                textViewStatus.setText("연결중...");
                msg = Message.obtain(null, BluetoothService.CONNECT_DEVICE, 0, 0);
                break;
            case R.id.bt_1:
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.CMD_DESK_UP);
                break;
            case R.id.bt_2:
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.CMD_DESK_DOWN);
                break;
            case R.id.bt_3:
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.CMD_LED_UP);
                break;
            case R.id.bt_4:
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.CMD_LED_DOWN);
                break;
            case R.id.bt_5:
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.CMD_CENTER_TABLE_UP);
                break;
            case R.id.bt_6:
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.CMD_CENTER_TABLE_DOWN);
                break;
            case R.id.bt_7:
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.CMD_MAIN_TABLE_UP);
                break;
            case R.id.bt_8:
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.CMD_MAIN_TABLE_DOWN);
                break;
        }
        msg.setData(bundle);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothService.ACTION_RECEIVED_DATA)) {
                String temp = intent.getStringExtra(BluetoothService.FLAG_TEMPERATURE);
                String hum = intent.getStringExtra(BluetoothService.FLAG_HUMIDITY);
                String light = intent.getStringExtra(BluetoothService.FLAG_LIGHT);
                textViewReceive.append("온도:" + temp + ", 습도:" + hum + ", 빛:" + light + "\n");
            } else if (intent.getAction().equals(BluetoothService.ACTION_CONNECT_DEVICE)) {
                textViewStatus.setText("연결완료");
            } else if (intent.getAction().equals(BluetoothService.ACTION_DISCONNECT_DEVICE)) {
                textViewStatus.setText("연결끊김");
            } else if (intent.getAction().equals(BluetoothService.ACTION_ERROR)) {
                textViewStatus.setText("에러");
            }
        }
    }
}
