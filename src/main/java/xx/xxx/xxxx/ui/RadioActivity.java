package xx.xxx.xxxx.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

import xx.xxx.xxxx.R;
import xx.xxx.xxxx.receivers.USBReceiver;

/**
 * Created by m on 21/05/17.
 */

public class RadioActivity extends Activity {


    Button btn1;
    Button btn2;
    Button btn3;
    Button btn4;

    boolean pase1;
    boolean pase2;
    boolean pase3;
    boolean pase4;

    Button button_stop;
    Button button_play;
    private String STREAM_URL ="http://radioscoop.hu:80/live.mp3";
    private MediaPlayer mPlayer;
    private USBReceiver mEventReceiver = new USBReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.radio_activity);

        btn1 = (Button) findViewById(R.id.btn10);
        btn2 = (Button) findViewById(R.id.btn20);
        btn3 = (Button) findViewById(R.id.btn30);
        btn4 = (Button) findViewById(R.id.btn40);

        pase1 = false;
        pase2 = false;
        pase3 = false;

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pase1 = true;
            }
        });
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pase1 == true) {
                    pase2 = true;
                } else {
                    pase1 = false;
                    pase2 = false;
                }

            }
        });
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pase1 == true && pase2 == true) {
                    pase3 = true;
                } else {
                    pase1 = false;
                    pase2 = false;
                }

            }
        });
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (pase1 == true && pase2 == true && pase3 == true) {
                    //paso a otra actividad
                    pase1 = false;
                    pase2 = false;
                    pase3 = false;
                    mPlayer.stop();

                    Intent launchIntent = new Intent(getApplicationContext(), ConversationActivity.class);
                    startActivity(launchIntent);//null pointer check in case package name was not found
                } else {
                    pase1 = false;
                    pase2 = false;
                    pase3 = false;
                }

            }
        });

        button_stop=(Button) findViewById(R.id.btnStop);
        button_play=(Button) findViewById(R.id.btnPlay);

        mPlayer=new MediaPlayer();

        button_play.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){

                try{
                    mPlayer.reset();
                    mPlayer.setDataSource(STREAM_URL);
                    mPlayer.prepareAsync();

                    mPlayer.setOnPreparedListener(new MediaPlayer.
                            OnPreparedListener(){
                        @Override
                        public void onPrepared(MediaPlayer mp){
                            mp.start();

                        }
                    });

                } catch (IOException e){
                    e.printStackTrace();

                }

            }

        });

        button_stop.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                mPlayer.stop();

            }

        });

        //		Registro el receiver para USB

        registerReceiver(mEventReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"));

    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayer.stop();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPlayer.stop();
    }
}

