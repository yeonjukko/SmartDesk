package neurosky.com.smartdesk.activity;

import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.Set;

import neurosky.com.smartdesk.R;
import neurosky.com.smartdesk.adapter.BluetoothListAdapter;
import neurosky.com.smartdesk.manager.ConnectManager;

public class RegDeviceActivity extends SmartDeskActivity {
    private BluetoothListAdapter bluetoothListAdapter;
    private ConnectManager connectManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        connectManager = ConnectManager.getInstance(getContext());
        setLayout();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Set<BluetoothDevice> devices = connectManager.getBluetoothPairDevices();
        if (devices.size() == 0) {
            new AlertDialog.Builder(getContext())
                    .setTitle("알림")
                    .setMessage("등록된 기기가 없습니다.\n새로운 기기를 등록하시겠습니까?")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intentOpenBluetoothSettings = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                            startActivity(intentOpenBluetoothSettings);
                        }
                    }).show();
        } else {
            bluetoothListAdapter.setData(connectManager.getBluetoothPairDevices());
        }
    }

    private void setLayout() {
        RecyclerView recyclerViewDeviceList = (RecyclerView) findViewById(R.id.rv_device_list);
        recyclerViewDeviceList.setLayoutManager(new LinearLayoutManager(getContext()));
        bluetoothListAdapter = new BluetoothListAdapter(this, null);
        recyclerViewDeviceList.setAdapter(bluetoothListAdapter);
    }
}
