package xx.xxx.xxxx.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import xx.xxx.xxxx.R;

/**
 * Created by m on 12/05/17.
 */

public class LigthActivity extends Activity {

    Button btn1;
    Button btn2;
    Button btn3;
    Button btn4;
    Button btnluz;

    private static final int CAMERA_PIC_REQUEST = 1337;
    boolean pase1;
    boolean pase2;
    boolean pase3;
    boolean pase4;

    private Camera camera;
    private boolean isFlashOn;
    private boolean hasFlash;
    Camera.Parameters params;
    MediaPlayer mp;

    public static Camera cam = null;// has to be static, otherwise onDestroy() destroys it


    static final int REQUEST_IMAGE_CAPTURE = 1;


    public static boolean openApp(Context context, String packageName) {
        PackageManager manager = context.getPackageManager();
        Intent i = manager.getLaunchIntentForPackage(packageName);
        if (i == null) {
            return false;
            //throw new PackageManager.NameNotFoundException();
        }
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        context.startActivity(i);
        return true;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.light_layout);

        btnluz = (Button) findViewById(R.id.button1);

        btn1 = (Button) findViewById(R.id.button3);
        btn2 = (Button) findViewById(R.id.button4);
        btn3 = (Button) findViewById(R.id.button5);
        btn4 = (Button) findViewById(R.id.button6);





        btnluz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LigthActivity.this, "Hagase la luz", Toast.LENGTH_SHORT).show();
                //Aqui se prende la linterna

                openApp(getApplicationContext(),"com.android.camera");

            }
        });

        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pase1 = true;
            }
        });
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pase1 == true){
                    pase2 = true;
                }else {
                    pase1 =false;
                    pase2 =false;
                }

            }
        });
        btn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pase1 == true && pase2 == true){
                    pase3 = true;
                }else {
                    pase1 =false;
                    pase2 =false;
                }

            }
        });
        btn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pase1 == true && pase2 == true && pase3 == true){
                    //paso a otra actividad
                    Intent launchIntent = new Intent(getApplicationContext() , ConversationActivity.class);
                    startActivity(launchIntent);//null pointer check in case package name was not found
                }else {
                    pase1 =false;
                    pase2 =false;
                    pase3 =false;
                }

            }
        });



    }


    public void flashLightOn(View view) {

        try {
            if (getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_CAMERA_FLASH)) {
                cam = Camera.open();
                Camera.Parameters p = cam.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                cam.setParameters(p);
                cam.startPreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Exception flashLightOn()",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void flashLightOff(View view) {
        try {
            if (getPackageManager().hasSystemFeature(
                    PackageManager.FEATURE_CAMERA_FLASH)) {
                cam.stopPreview();
                cam.release();
                cam = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), "Exception flashLightOff",
                    Toast.LENGTH_SHORT).show();
        }




    }

}
