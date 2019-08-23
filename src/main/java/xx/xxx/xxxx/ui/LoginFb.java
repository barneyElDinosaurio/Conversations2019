package xx.xxx.xxxx.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;

import xx.xxx.xxxx.R;


public class LoginFb extends Activity {

    ActionBar actionBar;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fb_login_layout);

        //Set action bar
        actionBar = getActionBar();
        actionBar.setBackgroundDrawable((getResources().getDrawable(R.drawable.ab)));
        actionBar.setDisplayShowTitleEnabled(false);
    }
}
