package neurosky.com.smartdesk.manager;

import android.view.MotionEvent;
import android.view.View;

/**
 * Created by MoonJongRak on 2016. 7. 25..
 */
public abstract class LongPressListener implements View.OnTouchListener {
    private Thread loopThread;

    @Override
    public boolean onTouch(final View view, MotionEvent motionEvent) {
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            if (loopThread != null) {
                loopThread.interrupt();
                loopThread = null;
            }
            loopThread = new Thread() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            view.post(new Runnable() {
                                @Override
                                public void run() {
                                    onPressing(view);
                                }
                            });
                            sleep(300);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
            };
            loopThread.start();
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            if (loopThread != null) {
                loopThread.interrupt();
                loopThread = null;
            }
        }
        return true;
    }

    public abstract void onPressing(View view);
}
