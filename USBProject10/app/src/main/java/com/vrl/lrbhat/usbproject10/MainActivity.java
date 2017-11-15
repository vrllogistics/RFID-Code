package com.vrl.lrbhat.usbproject10;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends Activity {
    public final String ACTION_USB_PERMISSION = "com.vrl.lrbhat.usbproject10.USB_PERMISSION";
    private static UsbManager manager = null;
    private static UsbDevice device = null;
    private static UsbSerialDevice serialPort=null;
    private static UsbDeviceConnection connection=null;
    private Byte[] bytes;
    private static int TIMEOUT = 0;
    private boolean forceClaim = true;

    TextView tvdevicename=null;
    TextView tvvendorid=null;
    TextView tvproductid=null;
    Button btngetdevice=null;
    Button btnsend=null;
    EditText etdata=null;
    ScrollView scrollView=null;
    TextView textView=null;

    // get device
    private void getDevice(){
        try {
            manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
            for (Map.Entry<String, UsbDevice> entry : deviceList.entrySet()) {
                UsbDevice d = entry.getValue();
                int deviceId=d.getVendorId();
                if ( deviceId == 9025) //Arduino Vendor ID
                {
                    //found our Arduino device
                    device=d;
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    manager.requestPermission(device, pi);
                    tvdevicename.setText("Dev Name : "+device.getDeviceName());
                    tvvendorid.setText("Ven Id : "+device.getVendorId());
                    tvproductid.setText("Prod Id : "+device.getProductId());
                }
            }
        }catch (Exception e){
            Toast.makeText(this,"Error in getDevice : "+e.getLocalizedMessage(),Toast.LENGTH_LONG).show();}
    }

    // send data
    private void sendData(){
        try {
            serialPort.write(etdata.getText().toString().getBytes());
            tvAppend(textView," Data Sent " + etdata.getText().toString()+"\n");
            scrollView.smoothScrollTo(0, textView.getBottom());
        } catch (Exception e) {
            Toast.makeText(this,"Error in sendData : "+e.getLocalizedMessage(),Toast.LENGTH_LONG).show();}
    }

    //receive data
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = manager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            serialPort.setBaudRate(115200);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvAppend(textView, " Serial Connection Opened!\n" );
                            scrollView.smoothScrollTo(0, textView.getBottom());

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                            tvAppend(textView," PORT NOT OPEN\n" );
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                tvAppend(textView," USB ATTACHED\n" );
                scrollView.smoothScrollTo(0, textView.getBottom());
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                tvAppend(textView," USB DETACHED\n" );
                scrollView.smoothScrollTo(0, textView.getBottom());
            }
        }

        ;
    };
    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                //data = new String(arg0, "UTF-8");
                data = new String(arg0);
                data.replace("\n","");
                tvAppend(textView,data+"\n");
                scrollView.smoothScrollTo(0, textView.getBottom());
            } catch (Exception e) {
                tvAppend(textView,"Error in mCallback"+e.getLocalizedMessage()+"\n");
                scrollView.smoothScrollTo(0, textView.getBottom());}
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvdevicename=(TextView) findViewById(R.id.tvdevicename);
        tvvendorid=(TextView) findViewById(R.id.tvvendorid);
        tvproductid=(TextView) findViewById(R.id.tvproductid);
        btngetdevice=(Button) findViewById(R.id.btngetdevice);
        etdata=(EditText) findViewById(R.id.etdata);
        btnsend=(Button) findViewById(R.id.btnsend);
        scrollView=(ScrollView) findViewById(R.id.scrollView);
        textView = (TextView) findViewById(R.id.textView);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

    }

    public void onbtngetdeviceonClick(View v)
    {
        getDevice();
    }
    public void onbtnsendClick(View v)
    {
        sendData();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
