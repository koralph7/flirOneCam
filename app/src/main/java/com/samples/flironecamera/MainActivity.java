/*
 * ******************************************************************
 * @title FLIR THERMAL SDK
 * @file MainActivity.java
 * @Author FLIR Systems AB
 *
 * @brief  Main UI of test application
 *
 * Copyright 2019:    FLIR Systems
 * ******************************************************************/
package com.samples.flironecamera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.androidsdk.live.connectivity.UsbPermissionHandler;
import com.flir.thermalsdk.image.Point;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.log.ThermalLog;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Sample application for scanning a FLIR ONE or a built in emulator
 * <p>
 * See the {@link CameraHandler} for how to preform discovery of a FLIR ONE camera, connecting to it and start streaming images
 * <p>
 * The MainActivity is primarily focused to "glue" different helper classes together and updating the UI components
 * <p/>
 * Please note, this is <b>NOT</b> production quality code, error handling has been kept to a minimum to keep the code as clear and concise as possible
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    //Handles Android permission for eg Network
    private PermissionHandler permissionHandler;

    List<View> list;
    List<TextView>listText;
    int xAxis = 0;
    int yAxis = 0;

    TextView num;
    TextView numCoor;
    Bitmap bitmap;

    //Handles network camera operations
    private CameraHandler cameraHandler;

    float x,y,rX, rY;

//    Canvas canvas;
//    Paint paint;

    private Identity connectedIdentity = null;
    private TextView connectionStatus;
    private TextView discoveryStatus;
    Point point;

    private ImageView msxImage;
    private ImageView photoImage;

    Thread thread;

    private LinkedBlockingQueue<FrameDataHolder> framesBuffer = new LinkedBlockingQueue(21);
    private UsbPermissionHandler usbPermissionHandler = new UsbPermissionHandler();




    /**
     * Show message on the screen
     */
    public interface ShowMessage {
        void show(String message);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ThermalLog.LogLevel enableLoggingInDebug = BuildConfig.DEBUG ? ThermalLog.LogLevel.DEBUG : ThermalLog.LogLevel.NONE;

        list = new ArrayList<>();
        listText = new ArrayList<>();
        //ThermalSdkAndroid has to be initiated from a Activity with the Application Context to prevent leaking Context,
        // and before ANY using any ThermalSdkAndroid functions
        //ThermalLog will show log from the Thermal SDK in standards android log framework
        ThermalSdkAndroid.init(getApplicationContext(), enableLoggingInDebug);

        Context context = getApplicationContext();

        permissionHandler = new PermissionHandler(showMessage, MainActivity.this);

        cameraHandler = new CameraHandler();

        setupViews();

        num = findViewById(R.id.temp);

        Drawable dr = getResources().getDrawable(R.drawable.cross);
        Bitmap bitmap = ((BitmapDrawable) dr).getBitmap();
// Scale it to 50 x 50
        Drawable d = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 50, 50, true));



        final RelativeLayout rr = (RelativeLayout) findViewById(R.id.cross);
        rr.setOnTouchListener(new View.OnTouchListener() {

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if (!list.isEmpty()) {
                    rr.removeView(list.get(0));
                    list.clear();}
                if (!listText.isEmpty()) {
                    rr.removeView(listText.get(0));
                    listText.clear();}

                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    int x = (int) event.getX();
                    int y = (int) event.getY();

                     xAxis = (int) event.getX();
                     yAxis = (int) event.getY();

                    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT);
//                    RelativeLayout.LayoutParams lpt = new RelativeLayout.LayoutParams(
//                            RelativeLayout.LayoutParams.WRAP_CONTENT,
//                            RelativeLayout.LayoutParams.WRAP_CONTENT);

//                    point = new Point(x,y);
//                    cameraHandler.setPoint(point);
//                    numCoor = new TextView(getApplicationContext());

                   // numCoor.setText("x = "+ x+", y = "+ y);
                    ImageView iv = new ImageView(getApplicationContext());



                    lp.setMargins(x-50, y-50, 0, 0);
                   // lpt.setMargins(x -150, y-150,0 , 0);

                    iv.setLayoutParams(lp);
                    iv.setImageDrawable(d);
                    //numCoor.setLayoutParams(lpt);

//                    iv.setImageDrawable(getResources().getDrawable(
//                            d));
//                    ((ViewGroup) v).addView(iv);
                    rr.addView(iv);
                    //rr.addView(numCoor);

                    list.add(iv);
                    //listText.add(numCoor);
                }
                return false;
            }
        });


//        msxImage.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//
////                bitmap = ((BitmapDrawable)msxImage.getDrawable()).getBitmap();
////                canvas = new Canvas(bitmap);
////                paint = new Paint();
////                paint.setColor(Color.GREEN);
////                msxImage.invalidate();
//
//                x = motionEvent.getX();
//                y = motionEvent.getY();
//                 rX = motionEvent.getRawX();
//                 rY =motionEvent.getRawY();
//                Toast.makeText(context, "I was touched: x = "+x+", y = "+y +
//                        ", rX = "+ rX + ", rY = "+rY , Toast.LENGTH_LONG).show();
//
//                canvas.drawPoint(x, y, paint);
//
//
//                return true;
//            }
//        });

//        thread= new Thread() {
//
//            @Override
//            public void run() {
//                try {
//                    while (!thread.isInterrupted()) {
//                        Thread.sleep(1000);
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                num.setText(getPoint());
//                            }
//                        });
//                    }
//                } catch (InterruptedException e) {
//                }
//            }
//        };
//
//        thread.start();

//        showSDKversion(ThermalSdkAndroid.getVersion());
    }



    public void startDiscovery(View view) {
        startDiscovery();

    }

    public String getPoint(double d) {



        String point = Double.toString(d);

        return point;
    }

    public void stopDiscovery(View view) {
        stopDiscovery();
    }


    public void connectFlirOne(View view) {
        connect(cameraHandler.getFlirOne());
    }

    public void connectSimulatorOne(View view) {
        connect(cameraHandler.getCppEmulator());
    }

    public void connectSimulatorTwo(View view) {
        connect(cameraHandler.getFlirOneEmulator());
    }

    public void disconnect(View view) {
        disconnect();
    }

//    @Override
//    public void sendData(String s) {
//        Handler handler = new Handler(Looper.getMainLooper());
//        handler.post(() -> num.setText(s));
//    }

    /**
     * Handle Android permission request response for Bluetooth permissions
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult() called with: requestCode = [" + requestCode + "], permissions = [" + permissions + "], grantResults = [" + grantResults + "]");
        permissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Connect to a Camera
     */
    private void connect(Identity identity) {
        //We don't have to stop a discovery but it's nice to do if we have found the camera that we are looking for
        cameraHandler.stopDiscovery(discoveryStatusListener);

        if (connectedIdentity != null) {
            Log.d(TAG, "connect(), in *this* code sample we only support one camera connection at the time");
            showMessage.show("connect(), in *this* code sample we only support one camera connection at the time");
            return;
        }

        if (identity == null) {
            Log.d(TAG, "connect(), can't connect, no camera available");
            showMessage.show("connect(), can't connect, no camera available");
            return;
        }



        connectedIdentity = identity;

        updateConnectionText(identity, "CONNECTING");
        //IF your using "USB_DEVICE_ATTACHED" and "usb-device vendor-id" in the Android Manifest
        // you don't need to request permission, see documentation for more information
        if (UsbPermissionHandler.isFlirOne(identity)) {
            usbPermissionHandler.requestFlirOnePermisson(identity, this, permissionListener);
        } else {
            doConnect(identity);
        }

    }

    private UsbPermissionHandler.UsbPermissionListener permissionListener = new UsbPermissionHandler.UsbPermissionListener() {
        @Override
        public void permissionGranted(Identity identity) {
            doConnect(identity);
        }

        @Override
        public void permissionDenied(Identity identity) {
            MainActivity.this.showMessage.show("Permission was denied for identity ");
        }

        @Override
        public void error(UsbPermissionHandler.UsbPermissionListener.ErrorType errorType, final Identity identity) {
            MainActivity.this.showMessage.show("Error when asking for permission for FLIR ONE, error:"+errorType+ " identity:" +identity);
        }
    };

    private void doConnect(Identity identity) {
        new Thread(() -> {
            try {
                cameraHandler.connect(identity, connectionStatusListener);
                runOnUiThread(() -> {
                    updateConnectionText(identity, "POŁĄCZONA");
                    cameraHandler.startStream(streamDataListener);

                });
            } catch (IOException e) {
                runOnUiThread(() -> {
                    Log.d(TAG, "Could not connect: " + e);
                    updateConnectionText(identity, "ROZŁĄCZONA");
                });
            }
        }).start();
    }

    /**
     * Disconnect to a camera
     */
    private void disconnect() {
        updateConnectionText(connectedIdentity, "DISCONNECTING");
        connectedIdentity = null;
        Log.d(TAG, "disconnect() called with: connectedIdentity = [" + connectedIdentity + "]");
        new Thread(() -> {
            cameraHandler.disconnect();
            runOnUiThread(() -> {
                    updateConnectionText(null, "ROZŁĄCZONA");
            });
        }).start();
    }

    /**
     * Update the UI text for connection status
     */
    private void updateConnectionText(Identity identity, String status) {
        String deviceId = identity != null ? identity.deviceId : "";
        connectionStatus.setText(getString(R.string.connection_status_text,  status));
    }

    /**
     * Start camera discovery
     */
    private void startDiscovery() {
        cameraHandler.startDiscovery(cameraDiscoveryListener, discoveryStatusListener);
    }

    /**
     * Stop camera discovery
     */
    private void stopDiscovery() {
        cameraHandler.stopDiscovery(discoveryStatusListener);
    }

    /**
     * Callback for discovery status, using it to update UI
     */
    private CameraHandler.DiscoveryStatus discoveryStatusListener = new CameraHandler.DiscoveryStatus() {
        @Override
        public void started() {
            discoveryStatus.setText(getString(R.string.connection_status_text, "discovering"));
        }

        @Override
        public void stopped() {
            discoveryStatus.setText(getString(R.string.connection_status_text, "nie szukam"));
        }
    };

    /**
     * Camera connecting state thermalImageStreamListener, keeps track of if the camera is connected or not
     * <p>
     * Note that callbacks are received on a non-ui thread so have to eg use {@link #runOnUiThread(Runnable)} to interact view UI components
     */
    private ConnectionStatusListener connectionStatusListener = new ConnectionStatusListener() {
        @Override
        public void onDisconnected(@org.jetbrains.annotations.Nullable ErrorCode errorCode) {
            Log.d(TAG, "onDisconnected errorCode:" + errorCode);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateConnectionText(connectedIdentity, "DISCONNECTED");
                }
            });
        }
    };

    private final CameraHandler.StreamDataListener streamDataListener = new CameraHandler.StreamDataListener() {

        @Override
        public void images(FrameDataHolder dataHolder) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    msxImage.setImageBitmap(dataHolder.msxBitmap);
                    photoImage.setImageBitmap(dataHolder.dcBitmap);


                }
            });
        }



        @Override
        public void images(Bitmap msxBitmap, Bitmap dcBitmap) {

            try {
                framesBuffer.put(new FrameDataHolder(msxBitmap,dcBitmap));

            } catch (InterruptedException e) {
                //if interrupted while waiting for adding a new item in the queue
                Log.e(TAG,"images(), unable to add incoming images to frames buffer, exception:"+e);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"framebuffer size:"+framesBuffer.size());
                    FrameDataHolder poll = framesBuffer.poll();
                    cameraHandler.setPoint(new Point(xAxis, yAxis));
                    num.setText(getPoint(cameraHandler.getTemp()));
                    //numCoor.setText(getPoint(cameraHandler.getTemp()));
                    msxImage.setImageBitmap(poll.msxBitmap);
                    photoImage.setImageBitmap(poll.dcBitmap);

                }
            });

        }
    };

//    public String changeCoordinatesForTemperature(int x, int y){
//
//    }

    /**
     * Camera Discovery thermalImageStreamListener, is notified if a new camera was found during a active discovery phase
     * <p>
     * Note that callbacks are received on a non-ui thread so have to eg use {@link #runOnUiThread(Runnable)} to interact view UI components
     */
    private DiscoveryEventListener cameraDiscoveryListener = new DiscoveryEventListener() {
        @Override
        public void onCameraFound(Identity identity) {
            Log.d(TAG, "onCameraFound identity:" + identity);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cameraHandler.add(identity);
                }
            });
        }

        @Override
        public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode errorCode) {
            Log.d(TAG, "onDiscoveryError communicationInterface:" + communicationInterface + " errorCode:" + errorCode);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stopDiscovery();
                    MainActivity.this.showMessage.show("onDiscoveryError communicationInterface:" + communicationInterface + " errorCode:" + errorCode);
                }
            });
        }
    };

    private ShowMessage showMessage = new ShowMessage() {
        @Override
        public void show(String message) {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    };

//    private void showSDKversion(String version) {
//        TextView sdkVersionTextView = findViewById(R.id.sdk_version);
//        String sdkVersionText = getString(R.string.sdk_version_text, version);
//        sdkVersionTextView.setText(sdkVersionText);
//    }

    private void setupViews() {
        connectionStatus = findViewById(R.id.connection_status_text);
        discoveryStatus = findViewById(R.id.discovery_status);

        msxImage = findViewById(R.id.msx_image);
        photoImage = findViewById(R.id.photo_image);
    }

}
