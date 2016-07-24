package neurosky.com.smartdesk.activity;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

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
        bluetoothListAdapter.setData(connectManager.getBluetoothPairDevices());
    }

    private void setLayout() {
        RecyclerView recyclerViewDeviceList = (RecyclerView) findViewById(R.id.rv_device_list);
        recyclerViewDeviceList.setLayoutManager(new LinearLayoutManager(getContext()));
        bluetoothListAdapter = new BluetoothListAdapter(this, null);
        recyclerViewDeviceList.setAdapter(bluetoothListAdapter);
    }
}
