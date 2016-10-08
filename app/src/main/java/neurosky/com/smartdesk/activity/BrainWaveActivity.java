package neurosky.com.smartdesk.activity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.AlgoSdk.NskAlgoDataType;
import com.neurosky.AlgoSdk.NskAlgoSdk;
import com.neurosky.AlgoSdk.NskAlgoSignalQuality;
import com.neurosky.AlgoSdk.NskAlgoState;
import com.neurosky.AlgoSdk.NskAlgoType;
import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.DataType.MindDataType;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;

import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.models.BarModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import at.grabner.circleprogress.CircleProgressView;
import co.mobiwise.library.MusicPlayerView;
import neurosky.com.smartdesk.R;
import neurosky.com.smartdesk.manager.ConnectManager;
import neurosky.com.smartdesk.service.BluetoothService;

public class BrainWaveActivity extends SmartDeskActivity implements View.OnClickListener {

    final String TAG = "MainActivityTag";
    private boolean IS_FIRST_GOOD = true;
    /*static {
        System.loadLibrary("NskAlgoAndroid");
    }*/

    // COMM SDK handles
    private TgStreamReader tgStreamReader;
    private BluetoothAdapter bluetoothAdapter;

    // internal variables
    private boolean bInited = false;
    private boolean bRunning = false;
    private NskAlgoType currentSelectedAlgo;

    // canned data variables
    private short raw_data[] = {0};
    private int raw_data_index = 0;

    // UI components
    private EditText text;

    private Button startButton;
    private Button stopButton;

    private Button bpText;

    private TextView attValue;
    private TextView medValue;

    private TextView sqText;

    private NskAlgoSdk nskAlgoSdk;

    private BarChart mBarChart;
    private BarModel bm1;
    private BarModel bm2;
    private BarModel bm3;
    private BarModel bm4;
    private BarModel bm5;

    private CircleProgressView circleProgressViewAttention;
    private CircleProgressView circleProgressViewMeditation;

    private MusicPlayerView musicPlayerView;
    private MediaPlayer mediaPlayer;

    //Temp values
    private int tmpAttentionValue = 0;
    private int tmpMeditationValue = 0;

    //Service Connecttion
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
        setContentView(R.layout.activity_brainwave);
        setLayout();
        setNskAlgoSdk();
        bindService(new Intent(getContext(), BluetoothService.class), connection, Context.BIND_AUTO_CREATE);
    }

    private void setNskAlgoSdk() {

        nskAlgoSdk = new NskAlgoSdk();
        int algoTypes = NskAlgoType.NSK_ALGO_TYPE_MED.value;
        algoTypes += NskAlgoType.NSK_ALGO_TYPE_ATT.value;
        algoTypes += NskAlgoType.NSK_ALGO_TYPE_BP.value;

        if (bInited) {
            nskAlgoSdk.NskAlgoUninit();
            bInited = false;
        }
        int ret = nskAlgoSdk.NskAlgoInit(algoTypes, getFilesDir().getAbsolutePath());
        if (ret == 0) {
            bInited = true;
        }


        try {
            // (1) Make sure that the device supports Bluetooth and Bluetooth is on
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                Toast.makeText(
                        this,
                        "Please enable your Bluetooth and re-run this program !",
                        Toast.LENGTH_LONG).show();
                //finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "error:" + e.getMessage());
            return;
        }

        nskAlgoSdk.setOnSignalQualityListener(new NskAlgoSdk.OnSignalQualityListener() {
            @Override
            public void onSignalQuality(int level) {
                //Log.d(TAG, "NskAlgoSignalQualityListener: level: " + level);
                final int fLevel = level;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // change UI elements here
                        String sqStr = NskAlgoSignalQuality.values()[fLevel].toString();
                        sqText.setText(sqStr);
                    }
                });
            }
        });

        nskAlgoSdk.setOnStateChangeListener(new NskAlgoSdk.OnStateChangeListener() {
            @Override
            public void onStateChange(int state, int reason) {
                String stateStr = "";
                String reasonStr = "";
                for (NskAlgoState s : NskAlgoState.values()) {
                    if (s.value == state) {
                        stateStr = s.toString();
                    }
                }
                for (NskAlgoState r : NskAlgoState.values()) {
                    if (r.value == reason) {
                        reasonStr = r.toString();
                    }
                }
                Log.d(TAG, "NskAlgoSdkStateChangeListener: state: " + stateStr + ", reason: " + reasonStr);
                final String finalStateStr = stateStr + " | " + reasonStr;
                final int finalState = state;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // change UI elements here

                        if (finalState == NskAlgoState.NSK_ALGO_STATE_RUNNING.value || finalState == NskAlgoState.NSK_ALGO_STATE_COLLECTING_BASELINE_DATA.value) {
                            bRunning = true;
                            startButton.setText("Pause");
                            startButton.setEnabled(true);
                            stopButton.setEnabled(true);
                        } else if (finalState == NskAlgoState.NSK_ALGO_STATE_STOP.value) {
                            bRunning = false;
                            raw_data = null;
                            raw_data_index = 0;
                            startButton.setText("Start");
                            startButton.setEnabled(true);
                            stopButton.setEnabled(false);


                            if (tgStreamReader != null && tgStreamReader.isBTConnected()) {

                                // Prepare for connecting
                                tgStreamReader.stop();
                                tgStreamReader.close();
                            }

                            System.gc();
                        } else if (finalState == NskAlgoState.NSK_ALGO_STATE_PAUSE.value) {
                            bRunning = false;
                            startButton.setText("Start");
                            startButton.setEnabled(true);
                            stopButton.setEnabled(true);
                        } else if (finalState == NskAlgoState.NSK_ALGO_STATE_ANALYSING_BULK_DATA.value) {
                            bRunning = true;
                            startButton.setText("Start");
                            startButton.setEnabled(false);
                            stopButton.setEnabled(true);
                        } else if (finalState == NskAlgoState.NSK_ALGO_STATE_INITED.value || finalState == NskAlgoState.NSK_ALGO_STATE_UNINTIED.value) {
                            bRunning = false;
                            startButton.setText("Start");
                            startButton.setEnabled(true);
                            stopButton.setEnabled(false);
                        }
                    }
                });
            }
        });

        nskAlgoSdk.setOnSignalQualityListener(new NskAlgoSdk.OnSignalQualityListener() {
            @Override
            public void onSignalQuality(final int level) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // change UI elements here
                        String sqStr = NskAlgoSignalQuality.values()[level].toString();
                        sqText.setText(sqStr);
                        if (sqStr.equals("GOOD") || sqStr.equals("MEDIUM")) {
                            stopSpinCircleProgressView();
                            if (IS_FIRST_GOOD) {
                                Toast.makeText(getApplicationContext(), "start버튼을 눌러주세요", Toast.LENGTH_SHORT).show();
                                IS_FIRST_GOOD = false;
                            }
                        }
                    }
                });
            }
        });


        nskAlgoSdk.setOnAttAlgoIndexListener(new NskAlgoSdk.OnAttAlgoIndexListener() {
            @Override
            public void onAttAlgoIndex(final int value) {
                Log.d(TAG, "NskAlgoAttAlgoIndexListener: Attention:" + value);
                String attStr = "[" + value + "]";
                final String finalAttStr = attStr;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // change UI elements here
                        attValue.setText(finalAttStr);
                        if (bRunning) {
                            if (value == 0) {
                                //circleProgressViewAttention.spin();
                            } else {
                                circleProgressViewAttention.setValueAnimated(tmpAttentionValue, value, 500);
                                circleProgressViewAttention.setBarColor(Color.parseColor(getAttentionColor(value)));
                                circleProgressViewAttention.setTextColor(Color.parseColor(getAttentionColor(value)));
                                tmpAttentionValue = value;

                                //블루투스 기기로 데이터 전송
                                Message msg = Message.obtain(null, BluetoothService.SEND_DATA, 0, 0);
                                Bundle bundle = new Bundle();
                                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.getCmdChangeLed(255, 255, 255, 0, 0));
                                msg.setData(bundle);
                                try {
                                    if (messenger != null)
                                        messenger.send(msg);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
            }
        });

        nskAlgoSdk.setOnMedAlgoIndexListener(new NskAlgoSdk.OnMedAlgoIndexListener() {
            @Override
            public void onMedAlgoIndex(final int value) {
                Log.d(TAG, "NskAlgoMedAlgoIndexListener: Meditation:" + value);
                String medStr = "[" + value + "]";
                final String finalMedStr = medStr;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // change UI elements here
                        medValue.setText(finalMedStr);
                        if (bRunning) {
                            if (value == 0) {
                                //circleProgressViewMeditation.spin();
                            } else {
                                circleProgressViewMeditation.setValueAnimated(tmpMeditationValue, value, 500);
                                circleProgressViewMeditation.setBarColor(Color.parseColor(getMeditationColor(value)));
                                circleProgressViewMeditation.setTextColor(Color.parseColor(getMeditationColor(value)));
                                tmpMeditationValue = value;

                                //블루투스 기기로 데이터 전송
                                Message msg = Message.obtain(null, BluetoothService.SEND_DATA, 0, 0);
                                Bundle bundle = new Bundle();
                                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.getCmdChangeLed(241, 90, 90, 0, 0));
                                msg.setData(bundle);
                                try {
                                    if (messenger != null)
                                        messenger.send(msg);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }


                });
            }


        });

        nskAlgoSdk.setOnBPAlgoIndexListener(new NskAlgoSdk.OnBPAlgoIndexListener() {
            @Override
            public void onBPAlgoIndex(final float delta, final float theta, final float alpha, final float beta, final float gamma) {
                Log.i(TAG, "NskAlgoBPAlgoIndexListener: BP: D[" + delta + " dB] T[" + theta + " dB] A[" +
                        alpha + " dB] B[" + beta + " dB] G[" + gamma + "]");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("test", "NskAlgoBPAlgoIndexListener: BP: D[" + delta + " dB] T[" + theta + " dB] A[" +
                                alpha + " dB] B[" + beta + " dB] G[" + gamma + "]");

                        bm1.setValue(delta);
                        bm2.setValue(theta);
                        bm3.setValue(alpha);
                        bm4.setValue(beta);
                        bm5.setValue(gamma);
                        mBarChart.clearChart();

                        mBarChart.addBar(bm1);
                        mBarChart.addBar(bm2);
                        mBarChart.addBar(bm3);
                        mBarChart.addBar(bm4);
                        mBarChart.addBar(bm5);
                        mBarChart.invalidate();
                    }
                });
            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        raw_data = new short[512];
        raw_data_index = 0;

        startButton.setEnabled(false);

        // Example of constructor public TgStreamReader(BluetoothAdapter ba, TgStreamHandler tgStreamHandler)
        tgStreamReader = new TgStreamReader(bluetoothAdapter, callback);

        if (tgStreamReader != null && tgStreamReader.isBTConnected()) {

            // Prepare for connecting
            tgStreamReader.stop();
            tgStreamReader.close();
        }

        // (4) Demo of  using connect() and start() to replace connectAndStart(),
        // please call start() when the state is changed to STATE_CONNECTED
        tgStreamReader.connect();


        mediaPlayer = MediaPlayer.create(getContext(), R.raw.moonlightsonata);

    }

    private void setLayout() {

        //BarChart 세팅
        mBarChart = (BarChart) findViewById(R.id.barchart);
        setBarChart();

        circleProgressViewAttention = (CircleProgressView) findViewById(R.id.circleViewAttention);
        circleProgressViewMeditation = (CircleProgressView) findViewById(R.id.circleViewMediation);

        startButton = (Button) this.findViewById(R.id.startButton);
        stopButton = (Button) this.findViewById(R.id.stopButton);

        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);

        attValue = (TextView) this.findViewById(R.id.attText);
        medValue = (TextView) this.findViewById(R.id.medText);

        musicPlayerView = (MusicPlayerView) findViewById(R.id.musicPlayerView);
        musicPlayerView.setCoverURL("http://upload.wikimedia.org/wikipedia/commons/6/");
        musicPlayerView.setOnClickListener(this);
        musicPlayerView.setMax(320);


        sqText = (TextView) this.findViewById(R.id.sqText);
    }

    private void spinCircleProgressView() {
        circleProgressViewAttention.spin();
        circleProgressViewMeditation.spin();
    }

    private void stopSpinCircleProgressView() {
        circleProgressViewAttention.stopSpinning();
        circleProgressViewMeditation.stopSpinning();
    }

    private void setBarChart() {
        bm1 = new BarModel(1f, 0xFFF15A5A);
        bm2 = new BarModel(2f, 0xFFF0C419);
        bm3 = new BarModel(3f, 0xFF4EBA6F);
        bm4 = new BarModel(0f, 0xFF2D95BF);
        bm5 = new BarModel(0f, 0xFF955BA5);
        mBarChart.addBar(bm1);
        mBarChart.addBar(bm2);
        mBarChart.addBar(bm3);
        mBarChart.addBar(bm4);
        mBarChart.addBar(bm5);
    }

    private String getAttentionColor(int value) {
        String color = "#000000";
        if (value < 20) {
            color = "#955BA5";
        } else if (value < 40) {
            color = "#2D95BF";
        } else if (value < 60) {
            color = "#4EBA6F";
        } else if (value < 80) {
            color = "#F0C419";
        } else if (value <= 100) {
            color = "#F15A5A";
        }

        return color;
    }

    private String getMeditationColor(int value) {
        String color = "#000000";

        if (value < 20) {
            color = "#F15A5A";
        } else if (value < 40) {
            color = "#F0C419";
        } else if (value < 60) {
            color = "#4EBA6F";
        } else if (value < 80) {
            color = "#2D95BF";
        } else if (value <= 100) {
            color = "#955BA5";
        }
        return color;
    }


    private short[] readData(InputStream is, int size) {
        short data[] = new short[size];
        int lineCount = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            while (lineCount < size) {
                String line = reader.readLine();
                if (line == null || line.isEmpty()) {
                    Log.d(TAG, "lineCount=" + lineCount);
                    break;
                }
                data[lineCount] = Short.parseShort(line);
                lineCount++;
            }
            Log.d(TAG, "lineCount=" + lineCount);
        } catch (IOException e) {

        }
        return data;
    }

    @Override
    public void onBackPressed() {
        nskAlgoSdk.NskAlgoUninit();
        finish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }


    private TgStreamHandler callback = new TgStreamHandler() {

        @Override
        public void onStatesChanged(int connectionStates) {
            // TODO Auto-generated method stub
            Log.d(TAG, "connectionStates change to: " + connectionStates);
            switch (connectionStates) {
                case ConnectionStates.STATE_CONNECTING:
                    // Do something when connecting
                    break;
                case ConnectionStates.STATE_CONNECTED:
                    // Do something when connected
                    tgStreamReader.start();
                    spinCircleProgressView();
                    showToast("Connected", Toast.LENGTH_SHORT);
                    break;
                case ConnectionStates.STATE_WORKING:
                    // Do something when working

                    //(9) demo of recording raw data , stop() will call stopRecordRawData,
                    //or you can add a button to control it.
                    //You can change the save path by calling setRecordStreamFilePath(String filePath) before startRecordRawData
                    //tgStreamReader.startRecordRawData();

                    BrainWaveActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Button startButton = (Button) findViewById(R.id.startButton);
                            startButton.setEnabled(true);
                        }

                    });

                    break;
                case ConnectionStates.STATE_GET_DATA_TIME_OUT:
                    // Do something when getting data timeout

                    //(9) demo of recording raw data, exception handling
                    //tgStreamReader.stopRecordRawData();

                    showToast("Get data time out!", Toast.LENGTH_SHORT);

                    if (tgStreamReader != null && tgStreamReader.isBTConnected()) {
                        tgStreamReader.stop();
                        tgStreamReader.close();
                    }

                    break;
                case ConnectionStates.STATE_STOPPED:
                    // Do something when stopped
                    // We have to call tgStreamReader.stop() and tgStreamReader.close() much more than
                    // tgStreamReader.connectAndstart(), because we have to prepare for that.
                    stopSpinCircleProgressView();

                    break;
                case ConnectionStates.STATE_DISCONNECTED:
                    // Do something when disconnected
                    stopSpinCircleProgressView();

                    break;
                case ConnectionStates.STATE_ERROR:
                    stopSpinCircleProgressView();
                    // Do something when you get error message
                    break;
                case ConnectionStates.STATE_FAILED:
                    stopSpinCircleProgressView();
                    // Do something when you get failed message
                    // It always happens when open the BluetoothSocket error or timeout
                    // Maybe the device is not working normal.
                    // Maybe you have to try again
                    break;
            }
        }

        @Override
        public void onRecordFail(int flag) {
            // You can handle the record error message here
            Log.e(TAG, "onRecordFail: " + flag);

        }

        @Override
        public void onChecksumFail(byte[] payload, int length, int checksum) {
            // You can handle the bad packets here.
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
                    Log.d("test", "atten=" + data);
                    break;
                case MindDataType.CODE_MEDITATION:
                    short medValue[] = {(short) data};
                    nskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_MED.value, medValue, 1);
                    Log.d("test", "med=" + data);

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

    public void showToast(final String msg, final int timeStyle) {
        BrainWaveActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), msg, timeStyle).show();
            }

        });
    }

    private void showDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("")
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        mBarChart.startAnimation();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.startButton:
                if (bRunning == false) {
                    nskAlgoSdk.NskAlgoStart(false);
                    spinCircleProgressView();

                } else {
                    nskAlgoSdk.NskAlgoPause();

                }
                break;
            case R.id.stopButton:
                nskAlgoSdk.NskAlgoStop();
                bRunning = false;
                stopSpinCircleProgressView();
                circleProgressViewAttention.setValue(0f);
                circleProgressViewMeditation.setValue(0f);
                break;

            case R.id.musicPlayerView:
                if (musicPlayerView.isRotating()) {
                    musicPlayerView.stop();
                    mediaPlayer.pause();
                } else {
                    musicPlayerView.start();

                    mediaPlayer.start();
                }

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mediaPlayer.stop();
        mediaPlayer.release();
        finish();
    }
}
