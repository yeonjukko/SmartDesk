package neurosky.com.smartdesk.adapter;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;

import neurosky.com.smartdesk.R;
import neurosky.com.smartdesk.manager.ConnectManager;

/**
 * Created by MoonJongRak on 2016. 7. 21..
 */
public class BluetoothListAdapter extends RecyclerView.Adapter<BluetoothListAdapter.BluetoothHolder> {

    private Activity activity;
    private ArrayList<BluetoothDevice> devices;

    public BluetoothListAdapter(Activity activity, Collection<BluetoothDevice> devices) {
        this.activity = activity;
        if (devices != null) {
            this.devices = new ArrayList<>(devices);
        }
    }


    @Override
    public BluetoothHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new BluetoothHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_bluetooth_list, parent, false));
    }

    @Override
    public void onBindViewHolder(BluetoothHolder holder, int position) {
        final BluetoothDevice device = devices.get(position);
        holder.textViewIndex.setText(position + ".");
        holder.textViewName.setText(device.getName());
        holder.textViewAddress.setText(device.getAddress());
        holder.layoutMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ConnectManager.getInstance(view.getContext()).setConnectDevice(device);
                activity.finish();
            }
        });

    }

    public void setData(Collection<BluetoothDevice> devices) {
        if (devices != null) {
            this.devices = new ArrayList<>(devices);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (devices == null) {
            return 0;
        }
        return devices.size();
    }

    public class BluetoothHolder extends RecyclerView.ViewHolder {
        private TextView textViewIndex, textViewName, textViewAddress;
        private ViewGroup layoutMain;

        public BluetoothHolder(View itemView) {
            super(itemView);
            this.textViewIndex = (TextView) itemView.findViewById(R.id.tv_index);
            this.textViewName = (TextView) itemView.findViewById(R.id.tv_name);
            this.textViewAddress = (TextView) itemView.findViewById(R.id.tv_address);
            this.layoutMain = (ViewGroup) itemView.findViewById(R.id.layout_main);
        }
    }
}
