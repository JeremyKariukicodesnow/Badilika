package com.badilika.timerfixture;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

/** Separate harmless foreground app used to measure a real daily timer. */
public class TimerFixture extends Activity {
    public void onCreate(Bundle state){
        super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        TextView label=new TextView(this);label.setText("Timer test app\n\nThis screen stays open until the daily allowance is used.");label.setTextSize(24);label.setGravity(Gravity.CENTER);label.setPadding(32,32,32,32);setContentView(label);
    }
}
