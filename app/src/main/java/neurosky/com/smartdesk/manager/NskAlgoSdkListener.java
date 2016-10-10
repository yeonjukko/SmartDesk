package neurosky.com.smartdesk.manager;

/**
 * Created by yeonjukko on 2016. 10. 9..
 */

public interface NskAlgoSdkListener {
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_STOPPED = 3;


    public static final int CONNECTION_QUALITY_GOOD = 4;
    public static final int CONNECTION_QUALITY_MEDIUM = 5;
    public static final int CONNECTION_QUALITY_POOR = 6;
    public static final int CONNECTION_QUALITY_NOT_DETECTED = 7;


    public void onStateChangedListener(int state);

    public void onSignalQualityListener(int level);

    public void onSpectrumChangedListener(float delta, float theta, float alpha, float beta, float gamma);

    public void onAttentionValueListener(int attention);

    public void onMeditationValueListener(int meditation);


}
