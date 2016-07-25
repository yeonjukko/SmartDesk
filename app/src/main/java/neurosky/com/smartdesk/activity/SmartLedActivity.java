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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.dd.CircularProgressButton;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.OpacityBar;

import neurosky.com.smartdesk.R;
import neurosky.com.smartdesk.manager.ConnectManager;
import neurosky.com.smartdesk.service.BluetoothService;

public class SmartLedActivity extends SmartDeskActivity implements View.OnClickListener, ColorPicker.OnColorSelectedListener, ColorPicker.OnColorChangedListener {

    private CircularProgressButton buttonConnect;
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
        setContentView(R.layout.activity_smart_led);
        regReceiver();
        bindService(new Intent(getContext(), BluetoothService.class), connection, Context.BIND_AUTO_CREATE);
        setLayout();
    }

    @Override
    protected void onDestroy() {
        unregReceiver();
        unbindService(connection);
        super.onDestroy();
    }

    private void setLayout() {
        buttonConnect = (CircularProgressButton) findViewById(R.id.bt_connect);
        buttonConnect.setOnClickListener(this);
        ColorPicker picker = (ColorPicker) findViewById(R.id.picker);
        OpacityBar opacityBar = (OpacityBar) findViewById(R.id.opacitybar);
        picker.addOpacityBar(opacityBar);
        picker.setOnColorChangedListener(this);
        picker.setOnColorSelectedListener(this);
    }

    private long sendTime;

    @Override
    public void onColorChanged(int colorValue) {

        if ((System.currentTimeMillis() - sendTime) < 100) {
            return;
        }
        sendTime = System.currentTimeMillis();

        sendColor(colorValue);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bt_connect:
                //textViewStatus.setText("연결중...");
                buttonConnect.setProgress(0);
                buttonConnect.setIndeterminateProgressMode(true); // turn on indeterminate progress
                buttonConnect.setProgress(50);
                Message msg = Message.obtain(null, BluetoothService.CONNECT_DEVICE, 0, 0);
                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.bt_setting:
                Intent intent = new Intent(getContext(), RegDeviceActivity.class);
                startActivity(intent);
                break;

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
    public void onColorSelected(int color) {
        sendColor(color);
    }

    private void sendColor(int colorValue) {
        String tmp;
        if (colorValue == 0) {
            tmp = "000000000";
        } else {
            tmp = Integer.toHexString(colorValue);
            if (tmp.length() == 7) {
                tmp = "0" + tmp;
            }
        }

        int white = Integer.parseInt(tmp.substring(0, 2), 16);
        int red = Integer.parseInt(tmp.substring(2, 4), 16);
        int green = Integer.parseInt(tmp.substring(4, 6), 16);
        int blue = Integer.parseInt(tmp.substring(5, 8), 16);

        Message msg = Message.obtain(null, BluetoothService.SEND_DATA, 0, 0);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.getCmdChangeLed(red, green, blue, white, 0));
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
            if (intent.getAction().equals(BluetoothService.ACTION_CONNECT_DEVICE)) {
                buttonConnect.setProgress(100);
            } else if (intent.getAction().equals(BluetoothService.ACTION_DISCONNECT_DEVICE)) {
                buttonConnect.setProgress(0);
                buttonConnect.setProgress(-1);
            } else if (intent.getAction().equals(BluetoothService.ACTION_ERROR)) {
                buttonConnect.setProgress(0);
                buttonConnect.setProgress(-1);
            }
        }
    }

}
