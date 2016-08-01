package neurosky.com.smartdesk.activity;

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
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dd.CircularProgressButton;

import neurosky.com.smartdesk.R;
import neurosky.com.smartdesk.activity.RegDeviceActivity;
import neurosky.com.smartdesk.activity.SmartDeskActivity;
import neurosky.com.smartdesk.manager.ConnectManager;
import neurosky.com.smartdesk.manager.LongPressListener;
import neurosky.com.smartdesk.service.BluetoothService;

public class SmartDeskMainActivity extends SmartDeskActivity implements View.OnClickListener {
    private static final int RESULT_REG_DEVICE = 100;

    //    private TextView textViewReceive;
//    private TextView textViewStatus;
    private CircularProgressButton buttonConnect;
    private ImageView buttonSetting;

    private BroadcastReceiver receiver;
    private Messenger messenger;
    private ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            messenger = new Messenger(service);
            //  textViewReceive.setEnabled(true);
        }

        public void onServiceDisconnected(ComponentName className) {
            messenger = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_main2);
//        textViewReceive = (TextView) findViewById(R.id.tv_receive);
//        textViewReceive.setEnabled(false);
//        textViewStatus = (TextView) findViewById(R.id.tv_status);
        buttonConnect = (CircularProgressButton) findViewById(R.id.bt_connect);
        buttonSetting = (ImageView) findViewById(R.id.bt_setting);

        buttonConnect.setOnClickListener(this);
        buttonSetting.setOnClickListener(this);

        findViewById(R.id.bt_1).setOnTouchListener(touchListener);
        findViewById(R.id.bt_2).setOnTouchListener(touchListener);
        findViewById(R.id.bt_3).setOnTouchListener(touchListener);
        findViewById(R.id.bt_4).setOnTouchListener(touchListener);
        findViewById(R.id.bt_5).setOnTouchListener(touchListener);
        findViewById(R.id.bt_6).setOnTouchListener(touchListener);
        findViewById(R.id.bt_7).setOnTouchListener(touchListener);
        findViewById(R.id.bt_8).setOnTouchListener(touchListener);

        regReceiver();
        bindService(new Intent(getContext(), BluetoothService.class), connection, Context.BIND_AUTO_CREATE);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregReceiver();
        unbindService(connection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_REG_DEVICE) {
            buttonConnect.setProgress(0);
        }
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
        switch (view.getId()) {
            case R.id.bt_connect:
                //textViewStatus.setText("연결중...");
                buttonConnect.setProgress(0);
                buttonConnect.setIndeterminateProgressMode(true); // turn on indeterminate progress
                buttonConnect.setProgress(50);
                Message msg = Message.obtain(null, BluetoothService.CONNECT_DEVICE, 0, 0);
                try {
                    if (messenger != null)
                        messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.bt_setting:
                Intent intent = new Intent(getContext(), RegDeviceActivity.class);
                startActivityForResult(intent, RESULT_REG_DEVICE);
                break;

        }

    }

    private View.OnTouchListener touchListener = new LongPressListener() {
        @Override
        public void onPressing(View view) {
            Message msg = Message.obtain(null, BluetoothService.SEND_DATA, 0, 0);
            Bundle bundle = new Bundle();
            switch (view.getId()) {
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
                if (messenger != null)
                    messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };


    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothService.ACTION_RECEIVED_DATA)) {
                String temp = intent.getStringExtra(BluetoothService.FLAG_TEMPERATURE);
                String hum = intent.getStringExtra(BluetoothService.FLAG_HUMIDITY);
                String light = intent.getStringExtra(BluetoothService.FLAG_LIGHT);
                // textViewReceive.append("온도:" + temp + ", 습도:" + hum + ", 빛:" + light + "\n");
            } else if (intent.getAction().equals(BluetoothService.ACTION_CONNECT_DEVICE)) {
                buttonConnect.setProgress(100);
                // textViewStatus.setText("연결완료");
            } else if (intent.getAction().equals(BluetoothService.ACTION_DISCONNECT_DEVICE)) {
                buttonConnect.setProgress(0);
                buttonConnect.setProgress(-1);
                //textViewStatus.setText("연결끊김");
            } else if (intent.getAction().equals(BluetoothService.ACTION_ERROR)) {
                buttonConnect.setProgress(0);
                buttonConnect.setProgress(-1);

            } else if (intent.getAction().equals(BluetoothService.ACTION_NOT_REG_DEVICE)) {
                startActivityForResult(new Intent(getContext(), RegDeviceActivity.class), RESULT_REG_DEVICE);
            }
        }
    }
}
