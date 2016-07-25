package neurosky.com.smartdesk.manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import neurosky.com.smartdesk.service.BluetoothService;

/**
 * Created by MoonJongRak on 2016. 7. 21..
 */
public class ConnectManager {
    public static final String CMD_DESK_UP = "SM1U";
    public static final String CMD_DESK_DOWN = "SM1D";
    public static final String CMD_LED_UP = "SM2U";
    public static final String CMD_LED_DOWN = "SM2D";
    public static final String CMD_CENTER_TABLE_UP = "SM3U";
    public static final String CMD_CENTER_TABLE_DOWN = "SM3D";
    public static final String CMD_MAIN_TABLE_UP = "SM4U";
    public static final String CMD_MAIN_TABLE_DOWN = "SM4D";

    private static final String CMD_READ_STATUS = "RDATA";
    //STX,R,000,000,00000,ETX


    private static final String TAG = ConnectManager.class.getSimpleName();
    private static final String SF_CONNECT_MANAGER = "ConnectManager";
    private static final String FLAG_ADDRESS = "address";

    private static final UUID COMMON_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static ConnectManager instance;

    private Context context;
    private OnBluetoothListener listener;
    private AsyncTask receiveTask;
    private AsyncTask requestTask;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;

    private BroadcastReceiver broadcastReceiver;

    final char STX = 0x02;
    final char ETX = 0x03;
    final char LF = 0x0A;

    public static ConnectManager getInstance(Context context) {
        if (instance == null) {
            instance = new ConnectManager(context);
        }
        return instance;
    }

    private ConnectManager(Context context) {
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        broadcastReceiver = new ConnectReceiver();
    }

    public void setConnectDevice() {
        String address = context.getSharedPreferences(SF_CONNECT_MANAGER, Context.MODE_PRIVATE).getString(FLAG_ADDRESS, null);
        if (address == null) {
            throw new NotSavedDeviceException("기기 등록을 먼저 해주세요.");
        }

        setConnectDevice(address);
    }

    public void setConnectDevice(String address) {
        setConnectDevice(bluetoothAdapter.getRemoteDevice(address));
    }

    public void setConnectDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
        context.getSharedPreferences(SF_CONNECT_MANAGER, Context.MODE_PRIVATE)
                .edit()
                .putString(FLAG_ADDRESS, bluetoothDevice.getAddress())
                .apply();

        Log.d(TAG, "기기 등록 완료 :" + bluetoothDevice.toString());
    }

    public BluetoothDevice getConnectDevice() {
        return bluetoothDevice;
    }

    public void connect() {
        if (bluetoothDevice == null) {
            throw new IllegalStateException("연결하기 전에 연결할 블루투스 기기를 설정해주세요.(setConnectDevice())!");
        }

        if (receiveTask != null) {
            receiveTask.cancel(true);
        }
        if (requestTask != null) {
            receiveTask.cancel(true);
        }

        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException ignored) {
            }
            bluetoothSocket = null;
        }
        new ConnectTask().execute();
        Log.d(TAG, "블루투스 기기와 연결중...");
    }

    public void close() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        bluetoothSocket = null;
        if (listener != null) {
            listener.onDisconnected();
        }
    }

    public boolean isConnected() {
        return bluetoothSocket != null && bluetoothSocket.isConnected();
    }

    public boolean isBluetoothEnable() {
        return bluetoothAdapter.isEnabled();
    }

    public Set<BluetoothDevice> getBluetoothPairDevices() {
        return bluetoothAdapter.getBondedDevices();
    }

    public void setOnBluetoothListener(OnBluetoothListener listener) {
        this.listener = listener;
    }

    public void sendData(String... data) {
        if (bluetoothSocket == null && listener != null) {
            listener.onError(new IllegalStateException("블루투스 기기를 먼저 연결해 주세요.(Connect())"));
            return;
        }
        new SendDataTask().execute(data);
    }

    public void onCreate() {
        regReceiver();
    }

    public void onDestroy() {
        unRegReceiver();
    }

    private void regReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        context.registerReceiver(broadcastReceiver, filter);
    }

    private void unRegReceiver() {
        context.unregisterReceiver(broadcastReceiver);
    }

    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(COMMON_UUID);
                bluetoothSocket.connect();
                return true;
            } catch (IOException e) {
                if (listener != null) {
                    listener.onError(e);
                }
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            if (success) {
                if (listener != null) {
                    listener.onConnected();
                }
                receiveTask = new ReceiveDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                requestTask = new RequestStatusTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                Log.d(TAG, "블루투스 기기와 연결 완료...");
            } else {
                if (listener != null) {
                    listener.onDisconnected();
                }
                Log.d(TAG, "블루투스 기기와 연결 실패...");
            }
        }
    }

    private class SendDataTask extends AsyncTask<String, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "블루투스 기기로 데이터 전송시작");
        }

        @Override
        protected Void doInBackground(String... strings) {
            try {
                DataOutputStream outputStream = new DataOutputStream(bluetoothSocket.getOutputStream());
                for (String data : strings) {
                    data = makeQuery(data);
                    outputStream.writeChars(data);
                    outputStream.flush();
                    Log.d(TAG, "블루투스 기기로 데이터 전송:" + data);
                }
            } catch (IOException e) {
                if (listener != null) {
                    listener.onError(e);
                }
                close();
                Log.d(TAG, "블루투스 기기로 데이터 전송중 오류...");
            }
            return null;
        }

    }

    private class ReceiveDataTask extends AsyncTask<Void, String, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "블루투스 기기로부터 데이터 받기 시작..");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(bluetoothSocket.getInputStream()));
                String tmp;
                while ((tmp = reader.readLine()) != null) {
                    if (listener != null) {
                        listener.onReceive(tmp);
                    }
                    Log.d(TAG, tmp);
                }

            } catch (IOException e) {
                if (listener != null) {
                    listener.onError(e);
                }
                close();
                Log.d(TAG, "블루투스 기기로부터 데이터 받기 에러..");
            }
            return null;
        }

    }

    private class RequestStatusTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            while (true) {
                sendData(CMD_READ_STATUS);
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (isCancelled()) {
                    break;
                }
            }
            return null;
        }
    }

    private String makeQuery(String data) {
        return STX + data + ETX + LF;
    }

    public interface OnBluetoothListener {
        void onConnected();

        void onReceive(String data);

        void onError(Exception e);

        void onDisconnected();
    }

    public class NotSavedDeviceException extends IllegalStateException {
        public NotSavedDeviceException() {
            super();
        }

        public NotSavedDeviceException(String err) {
            super(err);
        }
    }

    public static class BluetoothOffException extends IllegalStateException {
        public BluetoothOffException() {
            super("블루투스를 켜주세요.");
        }

        public BluetoothOffException(String err) {
            super(err);
        }
    }

    public static String getCmdChangeLed(byte red, byte green, byte blue, byte white, byte c) {
        //STX,W,r000,g000,b000,w000,c000,ETX
        return "Wr" + red + "g" + green + "b" + blue + "w" + white + "c" + c;
    }

    public static Bundle getParseStatus(String data) {
        try {
            int start = data.indexOf("R");
            String tmp = data.substring(start + 1, start + 12);
            Bundle result = new Bundle();

            result.putString(BluetoothService.FLAG_TEMPERATURE, Float.toString(Float.parseFloat(tmp.substring(0, 3)) / 10));
            result.putString(BluetoothService.FLAG_HUMIDITY, Float.toString(Float.parseFloat(tmp.substring(3, 6)) / 10));
            result.putString(BluetoothService.FLAG_LIGHT, Integer.toString(Integer.parseInt(tmp.substring(6, 11))));
            return result;
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            e.printStackTrace();
            return null;
        }
    }

    private class ConnectReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED) || intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (bluetoothDevice.equals(getConnectDevice())) {
                    close();
                }
            }
        }
    }


}
