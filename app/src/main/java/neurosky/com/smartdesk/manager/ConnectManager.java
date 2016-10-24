package neurosky.com.smartdesk.manager;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    public static final String CMD_CENTER_TABLE_UP = "SM4U";
    public static final String CMD_CENTER_TABLE_DOWN = "SM4D";
    public static final String CMD_MAIN_TABLE_UP = "SM3U";
    public static final String CMD_MAIN_TABLE_DOWN = "SM3D";
    public static final String CMD_LED_ON = getCmdChangeLed(0, 0, 0, 150, 150);
    public static final String CMD_LED_OFF = getCmdChangeLed(0, 0, 0, 0, 0);
    public static final String CMD_F_IR_LED_ON = "SM5O";
    public static final String CMD_F_IR_LED_OFF = "SM5F";

    public static final String CMD_LED_STUDY_MODE = getCmdChangeLed(255, 255, 204, 150, 150);
    public static final String CMD_LED_ATTENTION_MODE = getCmdChangeLed(79, 129, 189, 0, 0);
    public static final String CMD_LED_RELAX_MODE = getCmdChangeLed(142, 180, 227, 0, 0);
    public static final String CMD_LED_SLEEP_MODE = getCmdChangeLed(1, 176, 80, 0, 0);
    public static final String CMD_LED_HAPPY_MODE = getCmdChangeLed(255, 255, 2, 0, 0);
    public static final String CMD_LED_DEPRESSION_MODE = getCmdChangeLed(255, 103, 255, 0, 0);


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
        try {
            context.unregisterReceiver(broadcastReceiver);
        } catch (Exception ignored) {

        }
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
                    outputStream.write(data.getBytes("ASCII"));
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
                    Thread.sleep(10000);
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

    public static String getCmdChangeLed(int red, int green, int blue, int white, int c) {
        //STX,W,r000,g000,b000,w000,c000,ETX
        return "Wr" + int2String(red) + "g" + int2String(green) + "b" + int2String(blue) + "w" + int2String(white) + "c" + int2String(c);
    }

    public static String getCmdChangeLed2Meditation(int value) {
        if (value < 5) {                                //red
            return getCmdChangeLed(255, 0, 0, 0, 0);
        } else if (value < 10) {
            return getCmdChangeLed(255, 30, 0, 0, 0);
        } else if (value < 15) {
            return getCmdChangeLed(255, 60, 0, 0, 0);
        } else if (value < 20) {                        //orange
            return getCmdChangeLed(255, 90, 0, 0, 0);
        } else if (value < 25) {
            return getCmdChangeLed(255, 120, 0, 0, 0);
        } else if (value < 30) {
            return getCmdChangeLed(255, 150, 0, 0, 0);
        } else if (value < 35) {                        //yellow
            return getCmdChangeLed(255, 180, 0, 0, 0);
        } else if (value < 40) {
            return getCmdChangeLed(255, 210, 0, 0, 0);
        } else if (value < 45) {
            return getCmdChangeLed(255, 240, 0, 0, 0);
        } else if (value < 50) {                        //green
            return getCmdChangeLed(180, 255, 0, 0, 0);
        } else if (value < 55) {
            return getCmdChangeLed(120, 255, 0, 0, 0);
        } else if (value < 60) {
            return getCmdChangeLed(60, 255, 0, 0, 0);
        } else if (value < 65) {                        //blue
            return getCmdChangeLed(0, 230, 255, 0, 0);
        } else if (value < 70) {
            return getCmdChangeLed(0, 190, 255, 0, 0);
        } else if (value < 75) {
            return getCmdChangeLed(0, 150, 255, 0, 0);
        } else if (value < 80) {                        //dark blue
            return getCmdChangeLed(0, 100, 255, 0, 0);
        } else if (value < 85) {
            return getCmdChangeLed(0, 50, 255, 0, 0);
        } else if (value < 90) {
            return getCmdChangeLed(25, 0, 255, 0, 0);
        } else if (value < 95) {                        //purple
            return getCmdChangeLed(100, 0, 255, 0, 0);
        } else if (value <= 100) {
            return getCmdChangeLed(150, 0, 255, 0, 0);
        }
        return null;
    }

    public static String getCmdChangeLed2Attention(int value) {

        if (value < 5) {                                 //purple
            return getCmdChangeLed(150, 0, 255, 0, 0);
        } else if (value < 10) {
            return getCmdChangeLed(100, 0, 255, 0, 0);
        } else if (value < 15) {                        //dark blue
            return getCmdChangeLed(25, 0, 255, 0, 0);
        } else if (value < 20) {
            return getCmdChangeLed(0, 50, 255, 0, 0);
        } else if (value < 25) {
            return getCmdChangeLed(0, 100, 255, 0, 0);
        } else if (value < 30) {                         //blue
            return getCmdChangeLed(0, 150, 255, 0, 0);
        } else if (value < 35) {
            return getCmdChangeLed(0, 190, 255, 0, 0);
        } else if (value < 40) {
            return getCmdChangeLed(0, 230, 255, 0, 0);
        } else if (value < 45) {//green
            return getCmdChangeLed(60, 255, 0, 0, 0);
        } else if (value < 50) {
            return getCmdChangeLed(120, 255, 0, 0, 0);
        } else if (value < 55) {
            return getCmdChangeLed(180, 255, 0, 0, 0);
        } else if (value < 60) {   //yellow
            return getCmdChangeLed(255, 240, 0, 0, 0);
        } else if (value < 65) {
            return getCmdChangeLed(255, 210, 0, 0, 0);
        } else if (value < 70) {
            return getCmdChangeLed(255, 180, 0, 0, 0);
        } else if (value < 75) {   //orange
            return getCmdChangeLed(255, 150, 0, 0, 0);
        } else if (value < 80) {
            return getCmdChangeLed(255, 120, 0, 0, 0);
        } else if (value < 85) {
            return getCmdChangeLed(255, 90, 0, 0, 0);
        } else if (value < 90) {//red
            return getCmdChangeLed(255, 60, 0, 0, 0);
        } else if (value < 95) {
            return getCmdChangeLed(255, 30, 0, 0, 0);
        } else if (value <= 100) {
            return getCmdChangeLed(255, 0, 0, 0, 0);
        }
        return null;
    }

    private static String int2String(int num) {
        String result = "";
        if (num >= 100) {
            result += Integer.toString(num);
        } else if (num >= 10) {
            result += "0" + Integer.toString(num);
        } else {
            result += "00" + Integer.toString(num);
        }
        return result;
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
