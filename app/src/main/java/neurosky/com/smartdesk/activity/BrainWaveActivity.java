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
import android.widget.LinearLayout;
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
    private static String CURRENT_MODE = "원하는 모드를 선택하세요.";
    private static final String THERAPY_STUDY_MODE = "Therapy Mode (Study)";
    private static final String THERAPY_ATTENTION_MODE = "Therapy Mode (Attention)";
    private static final String THERAPY_SLEEP_MODE = "Therapy Mode (Sleep)";
    private static final String THERAPY_RELAX_MODE = "Therapy Mode (Relax)";
    private static final String THERAPY_HAPPY_MODE = "Therapy Mode (Happy)";
    private static final String THERAPY_DEPRESSION_MODE = "Therapy Mode (Depression)";
    private static final String MUSIC_MODE = "Classic Music Mode";
    private static final String ATTENTION_MODE = "Attention Mode";
    private static final String MEDITATION_MODE = "Meditation Mode";


    private NskAlgoSdkManager nskAlgoSdkManager;

    private TextView sqText;
    private int notDetectedCount = 30;

    private BarChart mBarChart;
    private BarModel bm1;
    private BarModel bm2;
    private BarModel bm3;
    private BarModel bm4;
    private BarModel bm5;

    private CircleProgressView circleProgressViewAttention;
    private CircleProgressView circleProgressViewMeditation;

    private LinearLayout linearLayoutLeftTime;
    private TextView textViewLeftTime;
    private TextView textViewCurrentMode;

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

        //모바일 작업 시 주석 제거
        //setContentView(R.layout.activity_brainwave2);

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
        circleProgressViewMeditation = (CircleProgressView) findViewById(R.id.circleViewMeditation);

        musicPlayerView = (MusicPlayerView) findViewById(R.id.musicPlayerView);
        musicPlayerView.setCoverURL("http://upload.wikimedia.org/wikipedia/commons/6/");
        musicPlayerView.setOnClickListener(this);
        musicPlayerView.setMax(320);

        linearLayoutLeftTime = (LinearLayout) findViewById(R.id.linearLayoutLeftTime);
        textViewLeftTime = (TextView) findViewById(R.id.textViewLeftCount);
        textViewCurrentMode = (TextView) findViewById(R.id.textViewCurrentMode);
        textViewCurrentMode.setText(CURRENT_MODE);

        //Therapy Mode
        LinearLayout linearLayoutAttention = (LinearLayout) findViewById(R.id.layout_attention);
        linearLayoutAttention.setOnClickListener(this);
        LinearLayout linearLayoutHappy = (LinearLayout) findViewById(R.id.layout_happy);
        linearLayoutHappy.setOnClickListener(this);
        LinearLayout linearLayoutSleep = (LinearLayout) findViewById(R.id.layout_sleep);
        linearLayoutSleep.setOnClickListener(this);
        LinearLayout linearLayoutRelax = (LinearLayout) findViewById(R.id.layout_relax);
        linearLayoutRelax.setOnClickListener(this);
        LinearLayout linearLayoutDepression = (LinearLayout) findViewById(R.id.layout_depression);
        linearLayoutDepression.setOnClickListener(this);
        LinearLayout linearLayoutStudy = (LinearLayout) findViewById(R.id.layout_study);
        linearLayoutStudy.setOnClickListener(this);

        //Attention, Meditation Mode
        View viewAttentionMode = findViewById(R.id.viewAttentionMode);
        viewAttentionMode.setOnClickListener(this);
        View viewMeditationMode = findViewById(R.id.viewMediationMode);
        viewMeditationMode.setOnClickListener(this);

        sqText = (TextView) this.findViewById(R.id.sqText);

    }

    private void spinCircleProgressView() {
        if (circleProgressViewAttention != null && circleProgressViewMeditation != null) {
            circleProgressViewAttention.spin();
            circleProgressViewMeditation.spin();
        }
    }

    private void stopSpinCircleProgressView() {
        if (circleProgressViewAttention != null && circleProgressViewMeditation != null) {
            circleProgressViewAttention.stopSpinning();
            circleProgressViewMeditation.stopSpinning();
        }
    }

    private void setBarChart() {
        bm1 = new BarModel(1f, 0xFFF15A5A);
        bm2 = new BarModel(1f, 0xFFF0C419);
        bm3 = new BarModel(1f, 0xFF4EBA6F);
        bm4 = new BarModel(1f, 0xFF2D95BF);
        bm5 = new BarModel(1f, 0xFF955BA5);

        mBarChart.setShowValues(false);
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
        Message msg = Message.obtain(null, BluetoothService.SEND_DATA, 0, 0);
        Bundle bundle = new Bundle();

        switch (v.getId()) {
            case R.id.musicPlayerView:
                CURRENT_MODE = MUSIC_MODE;
                textViewCurrentMode.setText(MUSIC_MODE);
                if (musicPlayerView.isRotating()) {
                    musicPlayerView.stop();
                    mediaPlayer.pause();
                } else {
                    musicPlayerView.start();
                    mediaPlayer.start();
                }
                break;

            case R.id.layout_attention:
                CURRENT_MODE = THERAPY_ATTENTION_MODE;
                textViewCurrentMode.setText(THERAPY_ATTENTION_MODE);
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.CMD_LED_ATTENTION_MODE);
                break;
            case R.id.layout_study:
                textViewCurrentMode.setText(THERAPY_STUDY_MODE);
                CURRENT_MODE = THERAPY_STUDY_MODE;
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.CMD_LED_STUDY_MODE);
                break;
            case R.id.layout_relax:
                CURRENT_MODE = THERAPY_RELAX_MODE;
                textViewCurrentMode.setText(THERAPY_RELAX_MODE);
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.CMD_LED_RELAX_MODE);
                break;
            case R.id.layout_happy:
                CURRENT_MODE = THERAPY_HAPPY_MODE;
                textViewCurrentMode.setText(THERAPY_HAPPY_MODE);
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.CMD_LED_HAPPY_MODE);
                break;
            case R.id.layout_depression:
                CURRENT_MODE = THERAPY_DEPRESSION_MODE;
                textViewCurrentMode.setText(THERAPY_DEPRESSION_MODE);
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.CMD_LED_DEPRESSION_MODE);
                break;
            case R.id.layout_sleep:
                CURRENT_MODE = THERAPY_SLEEP_MODE;
                textViewCurrentMode.setText(THERAPY_SLEEP_MODE);
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.CMD_LED_SLEEP_MODE);
                break;
            case R.id.viewAttentionMode:
                CURRENT_MODE = ATTENTION_MODE;
                textViewCurrentMode.setText(ATTENTION_MODE);
                return;
            case R.id.viewMediationMode:
                CURRENT_MODE = MEDITATION_MODE;
                textViewCurrentMode.setText(MEDITATION_MODE);
                return;

        }
        msg.setData(bundle);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
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
                        sqText.setText("Good");
                        linearLayoutLeftTime.setVisibility(View.GONE);
                        notDetectedCount = 10;
                        break;
                    case NskAlgoSdkListener.CONNECTION_QUALITY_MEDIUM:
                        sqText.setText("Medium");
                        linearLayoutLeftTime.setVisibility(View.GONE);
                        notDetectedCount = 10;
                        break;
                    case NskAlgoSdkListener.CONNECTION_QUALITY_NOT_DETECTED:
                        sqText.setText("Not Detected");
                        notDetectedCount--;

                        if (notDetectedCount == 0) {
                            linearLayoutLeftTime.setVisibility(View.VISIBLE);
                            nskAlgoSdkManager.init();
                            spinCircleProgressView();
                            notDetectedCount = 60;
                        } else if (notDetectedCount <= 30) {
                            linearLayoutLeftTime.setVisibility(View.VISIBLE);
                        } else if (notDetectedCount > 30) {
                            linearLayoutLeftTime.setVisibility(View.GONE);
                        }

                        textViewLeftTime.setText("Research after "+notDetectedCount+"s");


                        break;
                    case NskAlgoSdkListener.CONNECTION_QUALITY_POOR:
                        sqText.setText("Poor");
                        linearLayoutLeftTime.setVisibility(View.GONE);
                        notDetectedCount = 10;
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
                if (circleProgressViewAttention != null) {
                    circleProgressViewAttention.setValueAnimated(tmpAttentionValue, value, 500);
                    circleProgressViewAttention.setBarColor(Color.parseColor(getAttentionColor(value)));
                    circleProgressViewAttention.setTextColor(Color.parseColor(getAttentionColor(value)));
                    //블루투스 기기로 데이터 전송

                    if (CURRENT_MODE.equals(ATTENTION_MODE)) {
                        new SendThread(tmpAttentionValue, value).start();
                    }
                    tmpAttentionValue = value;


                }
            }
        });
    }

    @Override
    public void onMeditationValueListener(final int value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (circleProgressViewMeditation != null) {
                    circleProgressViewMeditation.setValueAnimated(tmpMeditationValue, value, 500);
                    circleProgressViewMeditation.setBarColor(Color.parseColor(getMeditationColor(value)));
                    circleProgressViewMeditation.setTextColor(Color.parseColor(getMeditationColor(value)));
                    //블루투스 기기로 데이터 전송
                    if (CURRENT_MODE.equals(MEDITATION_MODE)||CURRENT_MODE.equals(MUSIC_MODE)) {
                        new SendThread(tmpMeditationValue, value).start();
                    }
                    tmpMeditationValue = value;

                }
            }
        });
    }

    class SendThread extends Thread {
        int startValue;
        int endValue;

        private SendThread(int startValue, int endValue) {
            this.startValue = startValue;
            this.endValue = endValue;
        }

        @Override
        public void run() {
            int firstValue=0;
            int secondValue=0;
            if (startValue < endValue) {
                firstValue = startValue + (Math.abs(endValue - startValue) / 3);
                secondValue = startValue + (Math.abs(endValue - startValue) / 3) * 2;
            } else if (startValue > endValue) {
                firstValue = startValue - (Math.abs(endValue - startValue) / 3);
                secondValue = startValue - (Math.abs(endValue - startValue) / 3) * 2;
            } else if (startValue == endValue) {
                firstValue = startValue;
                secondValue = startValue;
            }


            Message msg = Message.obtain(null, BluetoothService.SEND_DATA, 0, 0);
            Bundle bundle = new Bundle();

            if (CURRENT_MODE.equals(ATTENTION_MODE)) {
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.getCmdChangeLed2Attention(firstValue));
            } else if (CURRENT_MODE.equals(MEDITATION_MODE)||CURRENT_MODE.equals(MUSIC_MODE)) {
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.getCmdChangeLed2Meditation(firstValue));
            }
            msg.setData(bundle);
            try {
                if (messenger != null)
                    messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            try {
                sleep(333);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            msg = Message.obtain(null, BluetoothService.SEND_DATA, 0, 0);
            bundle = new Bundle();
            if (CURRENT_MODE.equals(ATTENTION_MODE)) {
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.getCmdChangeLed2Attention(secondValue));
            } else if (CURRENT_MODE.equals(MEDITATION_MODE)||CURRENT_MODE.equals(MUSIC_MODE)) {
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.getCmdChangeLed2Meditation(secondValue));
            }
            msg.setData(bundle);
            try {
                if (messenger != null)
                    messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                sleep(333);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            msg = Message.obtain(null, BluetoothService.SEND_DATA, 0, 0);
            bundle = new Bundle();

            if (CURRENT_MODE.equals(ATTENTION_MODE)) {
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.getCmdChangeLed2Attention(endValue));
            } else if (CURRENT_MODE.equals(MEDITATION_MODE)||CURRENT_MODE.equals(MUSIC_MODE)) {
                bundle.putString(BluetoothService.FLAG_DATA, ConnectManager.getCmdChangeLed2Meditation(endValue));
            }
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
