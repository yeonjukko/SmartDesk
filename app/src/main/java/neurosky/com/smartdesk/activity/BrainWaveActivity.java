package neurosky.com.smartdesk.activity;

import android.content.ComponentName;
import android.content.Context;
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
import android.widget.TextView;
import android.widget.Toast;

import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.models.BarModel;

import at.grabner.circleprogress.CircleProgressView;
import co.mobiwise.library.MusicPlayerView;
import neurosky.com.smartdesk.R;
import neurosky.com.smartdesk.manager.ConnectManager;
import neurosky.com.smartdesk.manager.NskAlgoSdkListener;
import neurosky.com.smartdesk.manager.NskAlgoSdkManager;
import neurosky.com.smartdesk.service.BluetoothService;

public class BrainWaveActivity extends SmartDeskActivity implements View.OnClickListener, NskAlgoSdkListener {

    private NskAlgoSdkManager nskAlgoSdkManager;

    private TextView sqText;

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

        nskAlgoSdkManager = new NskAlgoSdkManager(getContext());
        nskAlgoSdkManager.setNskAlgoSdkListener(this);
        nskAlgoSdkManager.init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mediaPlayer = MediaPlayer.create(getContext(), R.raw.moonlightsonata);
    }

    private void setLayout() {

        //BarChart 세팅
        mBarChart = (BarChart) findViewById(R.id.barchart);
        setBarChart();

        circleProgressViewAttention = (CircleProgressView) findViewById(R.id.circleViewAttention);
        circleProgressViewMeditation = (CircleProgressView) findViewById(R.id.circleViewMediation);

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


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onResume() {
        super.onResume();
        mBarChart.startAnimation();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {


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

    @Override
    public void onStateChangedListener(final int state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (state) {
                    case NskAlgoSdkListener.STATE_CONNECTED:
                        Toast.makeText(getContext(), "연결이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                        break;
                    case NskAlgoSdkListener.STATE_CONNECTING:
                        Toast.makeText(getContext(), "연결중입니다.", Toast.LENGTH_SHORT).show();
                        spinCircleProgressView();
                        break;
                    case NskAlgoSdkListener.STATE_STOPPED:
                        stopSpinCircleProgressView();
                        break;
                }
            }
        });
    }

    @Override
    public void onSignalQualityListener(final int level) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (level) {
                    case NskAlgoSdkListener.CONNECTION_QUALITY_GOOD:
                        sqText.setText("GOOD");
                        break;
                    case NskAlgoSdkListener.CONNECTION_QUALITY_MEDIUM:
                        sqText.setText("MEDIUM");
                        break;
                    case NskAlgoSdkListener.CONNECTION_QUALITY_NOT_DETECTED:
                        sqText.setText("NOT DETECTED");
                        break;
                    case NskAlgoSdkListener.CONNECTION_QUALITY_POOR:
                        sqText.setText("POOR");
                        break;
                }
            }
        });
    }

    @Override
    public void onSpectrumChangedListener(final float delta, final float theta, final float alpha, final float beta, final float gamma) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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

    @Override
    public void onAttentionValueListener(final int value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                circleProgressViewAttention.setValueAnimated(tmpAttentionValue, value, 500);
                circleProgressViewAttention.setBarColor(Color.parseColor(getAttentionColor(value)));
                circleProgressViewAttention.setTextColor(Color.parseColor(getAttentionColor(value)));
                tmpAttentionValue = value;

                //블루투스 기기로 데이터 전송
//                Message msg = Message.obtain(null, BluetoothService.SEND_DATA, 0, 0);
//                Bundle bundle = new Bundle();
//                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.getCmdChangeLed(255, 255, 255, 0, 0));
//                msg.setData(bundle);
//                try {
//                    if (messenger != null)
//                        messenger.send(msg);
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
            }
        });
    }

    @Override
    public void onMeditationValueListener(final int value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                circleProgressViewMeditation.setValueAnimated(tmpMeditationValue, value, 500);
                circleProgressViewMeditation.setBarColor(Color.parseColor(getMeditationColor(value)));
                circleProgressViewMeditation.setTextColor(Color.parseColor(getMeditationColor(value)));
                tmpMeditationValue = value;

                //블루투스 기기로 데이터 전송
//                Message msg = Message.obtain(null, BluetoothService.SEND_DATA, 0, 0);
//                Bundle bundle = new Bundle();
//                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.getCmdChangeLed(241, 90, 90, 0, 0));
//                msg.setData(bundle);
//                try {
//                    if (messenger != null)
//                        messenger.send(msg);
//                } catch (RemoteException e) {
//                    e.printStackTrace();
//                }
            }
        });
    }
}
