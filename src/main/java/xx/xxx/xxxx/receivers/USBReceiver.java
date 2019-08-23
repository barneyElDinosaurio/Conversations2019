package xx.xxx.xxxx.receivers;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import xx.xxx.xxxx.services.WipeDataReceiver;

/**
 * Created by danrosero on 7/05/17.
 */

public class USBReceiver extends BroadcastReceiver {


    DevicePolicyManager mDPM;
    ComponentName mDeviceAdmin;

    @Override
    public void onReceive(Context context, Intent intent) {
//        if(intent.getAction().equals(Intent.ACTION_UMS_CONNECTED)) {




        if(intent.getAction().equals("android.hardware.usb.action.USB_STATE")){

        if(intent.getExtras().getBoolean("connected")) {
          //  Toast.makeText(context, "CONECTADO USB", Toast.LENGTH_LONG).show();

            //		Para el wipeout de la data


            mDPM = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            mDeviceAdmin = new ComponentName(context, WipeDataReceiver.class);



              // mDPM.wipeData(0);



//		****


        }

        }




//        }
    }
}
