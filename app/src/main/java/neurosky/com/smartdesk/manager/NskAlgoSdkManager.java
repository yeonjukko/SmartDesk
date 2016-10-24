package neurosky.com.smartdesk.manager;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;

import com.neurosky.AlgoSdk.NskAlgoDataType;
import com.neurosky.AlgoSdk.NskAlgoSdk;
import com.neurosky.AlgoSdk.NskAlgoSignalQuality;
import com.neurosky.AlgoSdk.NskAlgoState;
import com.neurosky.AlgoSdk.NskAlgoType;
import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.DataType.MindDataType;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;

/**
 * Created by yeonjukko on 2016. 10. 9..
 */

public class NskAlgoSdkManager {
    Context context;
    NskAlgoSdkListener nskAlgoSdkListener;
    private NskAlgoSdk nskAlgoSdk;

    // canned data variables
    private short raw_data[] = {0};
    private int raw_data_index = 0;

    // COMM SDK handles
    private TgStreamReader tgStreamReader;

    public NskAlgoSdkManager(Context context) {
        this.context = context;
    }

    public void setNskAlgoSdkListener(NskAlgoSdkListener nskAlgoSdkListener) {
        this.nskAlgoSdkListener = nskAlgoSdkListener;
    }

    public void finish(){
        nskAlgoSdk.NskAlgoStop();
        tgStreamReader.close();
    }


    public void init() {
        raw_data = new short[512];
        raw_data_index = 0;


        nskAlgoSdk = new NskAlgoSdk();
        int algoTypes = NskAlgoType.NSK_ALGO_TYPE_MED.value;
        algoTypes += NskAlgoType.NSK_ALGO_TYPE_ATT.value;
        algoTypes += NskAlgoType.NSK_ALGO_TYPE_BP.value;
        nskAlgoSdk.NskAlgoInit(algoTypes, context.getFilesDir().getAbsolutePath());
        tgStreamReader = new TgStreamReader(BluetoothAdapter.getDefaultAdapter(), callback);
        tgStreamReader.connectAndStart();

        nskAlgoSdk.setOnSignalQualityListener(new NskAlgoSdk.OnSignalQualityListener() {
            @Override
            public void onSignalQuality(int level) {
                //Log.d(TAG, "NskAlgoSignalQualityListener: level: " + level);
                final int fLevel = level;
                String sqStr = NskAlgoSignalQuality.values()[fLevel].toString();
                Log.d("test", sqStr);
                if (sqStr.equals("GOOD")) {
                    nskAlgoSdkListener.onSignalQualityListener(NskAlgoSdkListener.CONNECTION_QUALITY_GOOD);
                    nskAlgoSdk.NskAlgoStart(false);
                } else if (sqStr.equals("MEDIUM")) {
                    nskAlgoSdkListener.onSignalQualityListener(NskAlgoSdkListener.CONNECTION_QUALITY_MEDIUM);
                    nskAlgoSdk.NskAlgoStart(false);
                } else if (sqStr.equals("POOR")) {
                    nskAlgoSdkListener.onSignalQualityListener(NskAlgoSdkListener.CONNECTION_QUALITY_POOR);
                    nskAlgoSdk.NskAlgoStart(false);
                } else if (sqStr.equals("NOT DETECTED")) {
                    nskAlgoSdkListener.onSignalQualityListener(NskAlgoSdkListener.CONNECTION_QUALITY_NOT_DETECTED);
                }

            }
        });

        nskAlgoSdk.setOnStateChangeListener(new NskAlgoSdk.OnStateChangeListener() {
            @Override
            public void onStateChange(int state, int reason) {

                final int finalState = state;

                if (finalState == NskAlgoState.NSK_ALGO_STATE_STOP.value || finalState == NskAlgoState.NSK_ALGO_STATE_PAUSE.value) {
                    nskAlgoSdkListener.onStateChangedListener(NskAlgoSdkListener.STATE_STOPPED);
//                    try {
//                        if (tgStreamReader != null && tgStreamReader.isBTConnected()) {
//                            tgStreamReader.stop();
//                            tgStreamReader.close();
//                        }
//                    } catch (Exception ignored) {
//
//                    }
                    nskAlgoSdk.NskAlgoStop();
                }
            }
        });


        nskAlgoSdk.setOnAttAlgoIndexListener(new NskAlgoSdk.OnAttAlgoIndexListener() {
            @Override
            public void onAttAlgoIndex(int value) {
                Log.d("test", "Att=" + value);
                nskAlgoSdkListener.onAttentionValueListener(value);
            }
        });

        nskAlgoSdk.setOnMedAlgoIndexListener(new NskAlgoSdk.OnMedAlgoIndexListener() {
            @Override
            public void onMedAlgoIndex(int value) {
                nskAlgoSdkListener.onMeditationValueListener(value);
            }
        });

        nskAlgoSdk.setOnBPAlgoIndexListener(new NskAlgoSdk.OnBPAlgoIndexListener() {
            @Override
            public void onBPAlgoIndex(final float delta, final float theta, final float alpha, final float beta, final float gamma) {
                Log.d("test", "NskAlgoBPAlgoIndexListener: BP: D[" + delta + " dB] T[" + theta + " dB] A[" +
                        alpha + " dB] B[" + beta + " dB] G[" + gamma + "]");
                nskAlgoSdkListener.onSpectrumChangedListener(delta, theta, alpha, beta, gamma);
            }
        });


    }


    private TgStreamHandler callback = new TgStreamHandler() {

        @Override
        public void onStatesChanged(int connectionStates) {
            // TODO Auto-generated method stub
            switch (connectionStates) {
                case ConnectionStates.STATE_CONNECTING:
                    // Do something when connecting
                    nskAlgoSdkListener.onStateChangedListener(NskAlgoSdkListener.STATE_CONNECTING);

                    break;
                case ConnectionStates.STATE_CONNECTED:
                    tgStreamReader.start();
                    nskAlgoSdk.NskAlgoStart(false);
                    nskAlgoSdkListener.onStateChangedListener(NskAlgoSdkListener.STATE_CONNECTED);
                    break;
                case ConnectionStates.STATE_DISCONNECTED:
                    if (tgStreamReader != null && tgStreamReader.isBTConnected()) {
                        tgStreamReader.stop();
                        tgStreamReader.close();
                    }
                case ConnectionStates.STATE_GET_DATA_TIME_OUT:
                case ConnectionStates.STATE_STOPPED:
                case ConnectionStates.STATE_ERROR:
                case ConnectionStates.STATE_FAILED:
                    nskAlgoSdkListener.onStateChangedListener(NskAlgoSdkListener.STATE_STOPPED);
                    nskAlgoSdk.NskAlgoStop();
                    break;

            }
        }

        @Override
        public void onRecordFail(int flag) {
        }

        @Override
        public void onChecksumFail(byte[] payload, int length, int checksum) {
        }

        @Override
        public void onDataReceived(int datatype, int data, Object obj) {
            // You can handle the received data here
            // You can feed the raw data to algo sdk here if necessary.
            //Log.i(TAG,"onDataReceived");
            switch (datatype) {
                case MindDataType.CODE_ATTENTION:
                    short attValue[] = {(short) data};
                    nskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_ATT.value, attValue, 1);
                    break;
                case MindDataType.CODE_MEDITATION:
                    short medValue[] = {(short) data};
                    nskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_MED.value, medValue, 1);
                    break;
                case MindDataType.CODE_POOR_SIGNAL:
                    short pqValue[] = {(short) data};
                    nskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_PQ.value, pqValue, 1);
                    break;
                case MindDataType.CODE_RAW:
                    raw_data[raw_data_index++] = (short) data;
                    if (raw_data_index == 512) {
                        nskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_EEG.value, raw_data, raw_data_index);
                        raw_data_index = 0;
                    }
                    break;
                default:
                    break;
            }
        }

    };

}
