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
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.dd.CircularProgressButton;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.OpacityBar;

import java.nio.ByteBuffer;
import java.util.Arrays;

import neurosky.com.smartdesk.R;
import neurosky.com.smartdesk.manager.ConnectManager;
import neurosky.com.smartdesk.service.BluetoothService;

public class SmartLedActivity extends SmartDeskActivity implements View.OnClickListener, ColorPicker.OnColorSelectedListener, ColorPicker.OnColorChangedListener {

    private static final int RESULT_REG_DEVICE = 100;

    private ViewGroup layoutMood, layoutRelax, layoutStudy, layoutBrainWave;
    private ImageButton buttonSetting;
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
        //setContentView(R.layout.activity_smart_led);
        setContentView(R.layout.activity_smart_led2);
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
        buttonSetting = (ImageButton) findViewById(R.id.bt_setting);
        buttonSetting.setOnClickListener(this);
        ColorPicker picker = (ColorPicker) findViewById(R.id.picker);
        OpacityBar opacityBar = (OpacityBar) findViewById(R.id.opacitybar);
        picker.setShowOldCenterColor(false);
        picker.addOpacityBar(opacityBar);
        picker.setOnColorChangedListener(this);
        picker.setOnColorSelectedListener(this);

        layoutMood = (ViewGroup) findViewById(R.id.layout_mood);
        layoutMood.setOnClickListener(this);
        layoutRelax = (ViewGroup) findViewById(R.id.layout_relax);
        layoutRelax.setOnClickListener(this);
        layoutStudy = (ViewGroup) findViewById(R.id.layout_study);
        layoutStudy.setOnClickListener(this);
        layoutBrainWave = (ViewGroup) findViewById(R.id.layout_brainwave);
        layoutBrainWave.setOnClickListener(this);
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
        Message msg = Message.obtain(null, BluetoothService.SEND_DATA, 0, 0);
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        switch (view.getId()) {
            case R.id.bt_connect:
                //textViewStatus.setText("연결중...");
                buttonConnect.setProgress(0);
                buttonConnect.setIndeterminateProgressMode(true); // turn on indeterminate progress
                buttonConnect.setProgress(50);
                msg = Message.obtain(null, BluetoothService.CONNECT_DEVICE, 0, 0);
                try {
                    if (messenger != null)
                        messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.layout_mood:
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.getCmdChangeLed(23, 43, 243, 220, 0));
                try {
                    if (messenger != null)
                        messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.layout_relax:
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.getCmdChangeLed(230, 143, 43, 120, 0));
                try {
                    if (messenger != null)
                        messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.layout_study:
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.getCmdChangeLed(22, 90, 200, 220, 0));
                try {
                    if (messenger != null)
                        messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.layout_brainwave:
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.getCmdChangeLed(50, 43, 210, 100, 0));
                try {
                    if (messenger != null)
                        messenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.bt_setting:
                startActivityForResult(new Intent(getContext(), RegDeviceActivity.class), RESULT_REG_DEVICE);
                break;

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_REG_DEVICE) {
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
    public void onColorSelected(int color) {
        sendColor(color);
    }

    private void sendColor(int colorValue) {

        int[] bytes = int2Byte(colorValue);

        Message msg = Message.obtain(null, BluetoothService.SEND_DATA, 0, 0);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.getCmdChangeLed(bytes[1], bytes[2], bytes[3], bytes[0], 0));
        msg.setData(bundle);
        try {
            if (messenger != null)
                messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private int[] int2Byte(int data) {
        int[] byteArray = new int[4];
        byteArray[0] = ((byte) (data >> 24)) & 0xff;
        byteArray[1] = ((byte) (data >> 16)) & 0xff;
        byteArray[2] = ((byte) (data >> 8)) & 0xff;
        byteArray[3] = ((byte) (data)) & 0xff;
        return byteArray;
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
            } else if (intent.getAction().equals(BluetoothService.ACTION_NOT_REG_DEVICE)) {
                startActivityForResult(new Intent(getContext(), RegDeviceActivity.class), RESULT_REG_DEVICE);
            }
        }
    }

}
