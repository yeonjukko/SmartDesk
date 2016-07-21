package neurosky.com.smartdesk;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.TextView;

import neurosky.com.smartdesk.activity.SmartDeskActivity;
import neurosky.com.smartdesk.service.BluetoothService;

public class MainActivity extends SmartDeskActivity {

    private TextView textViewReceive;
    private BroadcastReceiver receiver;
    private Messenger messenger;

    private ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            messenger = new Messenger(service);
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
        findViewById(R.id.bt_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bindService(new Intent(getContext(), BluetoothService.class), connection,
                        Context.BIND_AUTO_CREATE);
            }
        });

        findViewById(R.id.bt_disconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (messenger != null) {

                    Message msg = Message.obtain(null, BluetoothService.SEND_DATA, 0, 0);
                    Bundle bundle = new Bundle();
                    bundle.putString(BluetoothService.FLAG_DATA, "hello");
                    msg.setData(bundle);
                    try {
                        messenger.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        regReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregReceiver();
    }

    private void regReceiver() {
        receiver = new BluetoothReceiver();
        registerReceiver(receiver, new IntentFilter(BluetoothService.ACTION_RECEIVED_DATA));
    }

    private void unregReceiver() {
        unregisterReceiver(receiver);
        receiver = null;
    }

    private class BluetoothReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothService.ACTION_RECEIVED_DATA)) {
                String data = intent.getStringExtra(BluetoothService.FLAG_DATA);
                textViewReceive.append(data + "\n");
            }
        }
    }
}
