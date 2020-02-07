package com.example.chargeon;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.felhr.usbserial.SerialBuffer;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.sdsmdg.harjot.crollerTest.Croller;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import android.widget.Toast;


//class Connect extends Thread{
//
//MainActivity mainActivity = new MainActivity();
//    public void run() {
//        while(!(mainActivity.flag)){
//
//            try {
//                Thread.sleep(2000);
//                new MainActivity().onClickStart(null);
//
//
//            } catch (Exception e) { }
//
//
//            }
//        }
//    }




    public class MainActivity extends AppCompatActivity {

    public final String ACTION_USB_PERMISSION = "com.example.chargeon.USB_PERMISSION";
    Button startButton , stopButton;
    TextView textView;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    int level=100,Max=100;
    private TextView batteryPercentage;
    Croller croller;
    static Boolean flag;

    private BroadcastReceiver mBatInfoReciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
            batteryPercentage.setText(String.valueOf(level));
            croller.setMin(level);
            try {
                if(Max>level)
                    serialPort.write(("1").getBytes());
                else
                    serialPort.write(("0").getBytes());

            } catch (Exception e) {

            }

        }
    };

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");
                tvAppend(textView, data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.

                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvAppend(textView,"Serial Connection Opened!\n");
                            flag=true;


                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                            Toast.makeText(context, "PORT NOT OPEN", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                        Toast.makeText(context, "PORT IS NULL", Toast.LENGTH_SHORT).show();

                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                    Toast.makeText(context, "PORT NOT GRANTED", Toast.LENGTH_SHORT).show();

                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(startButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(stopButton);

            }


            try {
                Thread.sleep(1000);

                if(Max>level)
                    serialPort.write(("1").getBytes());
                else
                    serialPort.write(("0").getBytes());

            } catch (Exception e) {

            }
        }


    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        startButton = (Button) findViewById(R.id.buttonStart);


        textView = (TextView) findViewById(R.id.textView);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        flag = false;
        //battery charge
        batteryPercentage = (TextView) this.findViewById(R.id.batteryChargeView);
        this.registerReceiver(this.mBatInfoReciver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        croller = (Croller) findViewById(R.id.croller);
        croller.setProgress(level);
        final TextView percentageTxt = (TextView) findViewById(R.id.batteryMaxChargeView);
        croller.setOnProgressChangedListener(new Croller.onProgressChangedListener() {
            @Override
            public void onProgressChanged(int progress) {
                percentageTxt.setText(progress + "%");
                Max = progress;
                try {

                    if (Max > level)
                        serialPort.write(("1").getBytes());
                    else
                        serialPort.write(("0").getBytes());

                } catch (Exception e) {

                }

            }
        });
    }
        @Override
        protected void onResume()
        {
            super.onResume();


            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            onClickStart(null);





    }

    public void onClickStart(View view) {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID =device.getVendorId();
                Toast.makeText(this, ""+deviceVID, Toast.LENGTH_SHORT).show();
                if (deviceVID == 0x16C0 || deviceVID == 0x16d0 || deviceVID== 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;

                } else {
                    connection = null;
                    device = null;
                }
                if (!keep){
                    break;
                }
            }
        }
    }

    public void begin(View view) {
        try{
            Thread.sleep(2000);
            onClickStart(view);
            Toast.makeText(this, "begin", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e){
            Toast.makeText(this, "fail", Toast.LENGTH_SHORT).show();
        }

    }
    public void onClickStop(View view) {

        serialPort.close();
        tvAppend(textView,"\nSerial Connection Closed! \n");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        }
        else{
            this.finishAffinity();
        }
    }
    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }


}
