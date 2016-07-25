package neurosky.com.smartdesk.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.OpacityBar;

import neurosky.com.smartdesk.R;

public class SmartLedActivity extends AppCompatActivity implements ColorPicker.OnColorChangedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_led);
        setLayout();
    }

    private void setLayout() {
        ColorPicker picker = (ColorPicker) findViewById(R.id.picker);
        OpacityBar opacityBar = (OpacityBar) findViewById(R.id.opacitybar);

        picker.addOpacityBar(opacityBar);
        picker.setOnColorChangedListener(this);
    }

    @Override
    public void onColorChanged(int colorValue) {
        Log.d("tttt", "change" + Integer.toHexString(colorValue));
        String tmp;
        if (colorValue == 0) {
            tmp = "000000000";
        } else {
            tmp = Integer.toHexString(colorValue);
            if (tmp.length() == 7) {
                tmp = "0" + tmp;
            }
        }

        String white = tmp.substring(0, 2);
        String red = tmp.substring(2, 4);
        String green = tmp.substring(4, 6);
        String blue = tmp.substring(5, 8);


    }
}
