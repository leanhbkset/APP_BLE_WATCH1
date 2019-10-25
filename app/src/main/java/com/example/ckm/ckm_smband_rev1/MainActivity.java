package com.example.ckm.ckm_smband_rev1;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "nRFUART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;
    private int mState = UART_PROFILE_DISCONNECTED;

    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private LinearLayout videolayout,imagelayout;
    private ImageView imgView;
//    private ListView messageListView;
//    private ArrayAdapter<String> listAdapter;
    private Button btnConnectDisconnect,btnheartbeat,btnheartbullet, btnclimax, btnraindrop, btndance,btngame,btnScene7;
    private CustomVideoView myVideoView;
    private MediaController mediaControls;
    private ProgressDialog progressDialog;
    private int position = 0;
    String sendMsg = "";
    String hMsg = "";
    byte[] value;
    Thread thread;
    long sendTime;
    long receiveTime;
    boolean isReceiveData = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (mediaControls == null) {
            mediaControls = new MediaController(MainActivity.this)
            {
                @Override
                public void hide()
                {
                    mediaControls.show();
                }
            }
            ;
        }
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        videolayout = (LinearLayout) findViewById(R.id.videolayout);
        imagelayout = (LinearLayout) findViewById(R.id.imagelayout);
        imgView = (ImageView)findViewById(R.id.img_view);
        btnConnectDisconnect=(Button) findViewById(R.id.btnConnect);
        myVideoView = (CustomVideoView) findViewById(R.id.video_view);
        btnheartbeat = (Button)findViewById(R.id.btnheartbeat);
        btnheartbullet = (Button)findViewById(R.id.btnheartbulltet);
        btnclimax = (Button)findViewById(R.id.btnclimax);
        btnraindrop = (Button)findViewById(R.id.btnraindrop);
        btndance = (Button)findViewById(R.id.btndance);
        btngame = (Button)findViewById(R.id.btngame);
        //btnScene7 = (Button)findViewById(R.id.btnScene7);
        service_init();

        // Handle Disconnect & Connect button
        myVideoView.setOnClickListener(new ProcessMyEvent());
        btnheartbeat.setOnClickListener(new ProcessMyEvent());
        btnheartbullet.setOnClickListener(new ProcessMyEvent());
        btnclimax.setOnClickListener(new ProcessMyEvent());
        btnraindrop.setOnClickListener(new ProcessMyEvent());
        btndance.setOnClickListener(new ProcessMyEvent());
        btngame.setOnClickListener(new ProcessMyEvent());
        btnConnectDisconnect.setOnClickListener(new ProcessMyEvent());

    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(mediaControls != null) mediaControls.show(0);
        return super.onTouchEvent(event);
    }
    public void setUiEnabled(boolean bool)
    {
        if(bool) btnConnectDisconnect.setText("Disconnect");
        else{
            btnConnectDisconnect.setText("Connect");
        }
        myVideoView.setEnabled(bool);
        btnheartbeat.setEnabled(bool);
        btnheartbullet.setEnabled(bool);
        btnclimax.setEnabled(bool);
        btnraindrop.setEnabled(bool);
        btndance.setEnabled(bool);
        btngame.setEnabled(bool);
    }
    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

        }

        public void onServiceDisconnected(ComponentName classname) {
            ////     mService.disconnect(mDevice);
            mService = null;
        }
    };
    private Handler mHandler = new Handler() {
        @Override
        //Handler events that received from UART service
        public void handleMessage(Message msg) {

        }
    };
    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            String crrentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(TAG,"["+crrentDateTimeString+"] onReceive");
            //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        setUiEnabled(true);
                        Log.d(TAG, "["+currentDateTimeString+"] Connected to: "+ mDevice.getName());
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }

            //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        setUiEnabled(false);
                        stopVideo();
                        Log.d(TAG,"["+currentDateTimeString+"] Disconnected to: "+ mDevice.getName());
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                        //setUiState();

                    }
                });
            }


            //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }
            //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
                String crentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                Log.d(TAG,"["+crentDateTimeString+"]ACTION_DATA_AVAILABLE");
                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            String text = new String(txValue, "UTF-8");
                            if(txValue[0] == '0') isReceiveData = true;
                            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                            //Log.d(TAG,"["+currentDateTimeString+"]ACTION_DATA_AVAILABLE");
                            Log.d(TAG,"["+currentDateTimeString+"] RX: "+text);
                            //showMessage("["+currentDateTimeString+"] RX: "+text);
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }
            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){

                showMessage("Device is not supported. Disconnecting");
                mService.disconnect();
            }


        }
    };

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        stopVideo();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    mService.connect(deviceAddress);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("App is running in background.\n             Disconnect to exit");
        }
        else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.popup_title)
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.popup_no, null)
                    .show();
        }
    }
    public void stopVideo(){
        myVideoView.stopPlayback();
        myVideoView.setBackgroundResource(0);
        myVideoView.suspend();
        videolayout.setVisibility(View.INVISIBLE);
        imagelayout.setVisibility(View.INVISIBLE);
        if(mState == UART_PROFILE_CONNECTED) {
            Log.d(TAG, "Stop Video");
            String message = "r";
            mService.writeRXCharacteristic(message.getBytes());
            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
            Log.d(TAG, "[" + currentDateTimeString + "] TX: " + message);
            long current_time = System.currentTimeMillis();
            Log.d(TAG, "Start Delay at: " + System.currentTimeMillis());
            while (System.currentTimeMillis() - current_time < 500) ;
            Log.d(TAG, "Stop Delay at: " + System.currentTimeMillis());
        }
    }
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        //savedInstanceState.putInt("Position", myVideoView.getCurrentPosition());
        //myVideoView.pause();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        position = savedInstanceState.getInt("Position");
        myVideoView.seekTo(position);
    }
    private class ProcessMyEvent implements View.OnClickListener {
        @Override
        public void onClick(View arg0) {
            if (arg0.getId() == R.id.btnConnect){
                Log.i(TAG, "Connect button onClick" );
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else {
                    if (btnConnectDisconnect.getText().equals("Connect")){
                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice!=null)
                        {
                            stopVideo();
                            mService.disconnect();

                        }
                    }
                }
            }
            /** play fire
            else if(arg0.getId() == R.id.btnScene3){
                String message = "0";
                stopVideo();
                Log.i(TAG, "Scene3 button onClick" );
                sendMsg = "3"; // play jell
                mService.writeRXCharacteristic(message.getBytes());
                long current_time = System.currentTimeMillis();
                Log.d(TAG, "Start Delay at: " + System.currentTimeMillis());
                while (System.currentTimeMillis() - current_time < 100) ;
                mService.writeRXCharacteristic(sendMsg.getBytes());
            }
             **/
            else if(arg0.getId() == R.id.btnheartbeat){
                String message = "0";
                stopVideo();
                imagelayout.setVisibility(View.VISIBLE);
                try {
                    imgView.setImageBitmap(BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + URLEncoder.encode("HEARTBEAT.png", "UTF-8")));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "Scene3 button onClick" );
                sendMsg = "8"; // hearbeat
                mService.writeRXCharacteristic(message.getBytes());
                long current_time = System.currentTimeMillis();
                Log.d(TAG, "Start Delay at: " + System.currentTimeMillis());
                while (System.currentTimeMillis() - current_time < 100) ;
                mService.writeRXCharacteristic(sendMsg.getBytes());
            }
            else if(arg0.getId() == R.id.btnheartbulltet || arg0.getId() == R.id.btnclimax||arg0.getId() == R.id.btnraindrop||arg0.getId() == R.id.btndance || arg0.getId() == R.id.btngame ){
                stopVideo();
                videolayout.setVisibility(View.VISIBLE);
                // Create a progressbar
                //progressDialog = new ProgressDialog(MainActivity.this);
                // Set progressbar title
                //progressDialog.setTitle("SM BAND LOAD VIDEO");
                // Set progressbar message
                //progressDialog.setMessage("Loading...");

                //progressDialog.setCancelable(false);
                // Show progressbar
                //progressDialog.show();
                String filePath = "";

                switch (arg0.getId()) {
                    case R.id.btndance:
                        Log.i(TAG, "btndance onClick" );
                        try {

                            filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + URLEncoder.encode("Scene1.mp4", "UTF-8");
                            sendMsg = "1";
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }

                        break;
                    case R.id.btnclimax:
                        Log.i(TAG, "Scene2 button onClick" );
                        try {

                            filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + URLEncoder.encode("Scene2.mp4", "UTF-8");
                            sendMsg = "2";
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        break;

                    case R.id.btnheartbulltet:
                        Log.i(TAG, "Scene2 button onClick" );
                        try {

                            filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + URLEncoder.encode("HeartBullet.mp4", "UTF-8");
                            sendMsg = "9";
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        break;

                    case R.id.btngame:
                        Log.i(TAG, "Scene4 button onClick" );
                        try {

                            filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + URLEncoder.encode("Scene4.mp4", "UTF-8");
                            sendMsg = "4";
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        break;
                    case R.id.btnraindrop:
                        Log.i(TAG, "Scene4 button onClick" );
                        try {

                            filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + URLEncoder.encode("Scene5.mp4", "UTF-8");
                            sendMsg = "5";
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        break;
                    /**
                    case R.id.btnScene7:
                        Log.i(TAG, "Scene4 button onClick" );
                        try {

                            filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + URLEncoder.encode("HeartBullet.mp4", "UTF-8");
                            sendMsg = "9";
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        break;
                     **/
                }
                myVideoView.suspend();
                Uri videoUri = Uri.parse(filePath);
                myVideoView.setMediaController(mediaControls);
                mediaControls.setVisibility(View.INVISIBLE);
                mediaControls.setAnchorView(myVideoView);
                myVideoView.setVideoURI(videoUri);
                myVideoView.setPlayPauseListener(new CustomVideoView.PlayPauseListener(){
                    @Override
                    public void onPlay() {
                        Log.d("Debug","Play");
                    }

                    @Override
                    public void onPause() {
                        Log.d("Debug","Pause");
                        stopVideo();
                    }
                    @Override
                    public void onTimeBarSeekChanged(int currentTime)
                    {
                        Log.d("Debug","Seek"+currentTime);
                        if(currentTime != 0) {
                            stopVideo();
                        }
                    }
                });
                myVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    // Close the progress bar and play the video
                    @Override
                    public void onPrepared(MediaPlayer mp) {

                        mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener(){
                            @Override
                            public void onSeekComplete(MediaPlayer mp) {
                                //TODO: Your code here
                            }
                        });
                        //progressDialog.dismiss();
                        myVideoView.seekTo(position);
                        if (position == 0) {
                            myVideoView.requestFocus();
                            isReceiveData = false;
                            String message = "0";
                            if(sendMsg.equals("1")||sendMsg.equals("2")||sendMsg.equals("4")||sendMsg.equals("5")||sendMsg.equals("9")) {
                                mService.writeRXCharacteristic(message.getBytes());
                                sendTime = System.currentTimeMillis();
                                Log.d(TAG, "Send Time: " + sendTime + "  TX: " + message);
                                thread = new Thread() {
                                    public void run() {
                                        while (!isReceiveData) {
                                            //Log.d(TAG, "Forever Loop");
                                        }
                                        //isReceiveData = false;
                                        myVideoView.start();
                                        while (myVideoView.getCurrentPosition() == 0) {/*Log.d("Debug","CTime: "+ System.currentTimeMillis());*/}
                                        Log.d(TAG, "Current Pos: " + myVideoView.getCurrentPosition());
                                        //                              outputStream.write(writeByte);
                                        value = sendMsg.getBytes();
                                        mService.writeRXCharacteristic(value);
                                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                                        Log.d(TAG, "[" + currentDateTimeString + "] TX: " + sendMsg);
                                        sendMsg = "s";
                                    }
                                };
                                thread.start();
                            }
                        } else {
                            myVideoView.pause();
                        }
                    }

                });
            }
        }
    }
}
