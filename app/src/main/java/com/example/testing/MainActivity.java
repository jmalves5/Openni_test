package com.example.testing;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.openni.Device;
import org.openni.DeviceInfo;
import org.openni.OpenNI;
import org.openni.SensorType;
import org.openni.VideoMode;
import org.openni.VideoStream;
import org.openni.android.OpenNIHelper;
import org.openni.android.OpenNIView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "NiViewer";
    private OpenNIView openNIView;
    private OpenNIHelper openNIHelper;
    private Device device;
    private VideoStream videoStream;
    private Thread streamThread;
    private boolean startStream = true;
    private final Object m_sync = new Object();




    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OpenNI.setLogAndroidOutput(true);
        OpenNI.setLogMinSeverity(0);
        OpenNI.initialize();

        setContentView(R.layout.activity_simple_viewer);
        openNIView = (OpenNIView) findViewById(R.id.frameView);

        requestDevice();
    }

    private void requestDevice(){
        openNIHelper = new OpenNIHelper(getApplicationContext());
        openNIHelper.requestDeviceOpen(deviceOpenListener);
        Log.d("myTag", "RIP2");
    }

    OpenNIHelper.DeviceOpenListener deviceOpenListener = new OpenNIHelper.DeviceOpenListener() {
        @Override
        public void onDeviceOpened(UsbDevice usbDevice) {
            List<DeviceInfo> deviceInfos=OpenNI.enumerateDevices();
            if(deviceInfos.size()==0){
                return;
            }



            for (int i = 0; i < deviceInfos.size(); i ++){
                if(deviceInfos.get(i).getUsbProductId() == usbDevice.getProductId()){
                    device = Device.open();
                }
            }

            if(device!=null){
                videoStream = VideoStream.create(device, SensorType.DEPTH);

            }


            List<VideoMode> videoModes = videoStream.getSensorInfo().getSupportedVideoModes();

            for (int i = 0; i<videoModes.size(); i++){
                Log.i("IGOR", "supported mode: "+videoModes.get(i).getResolutionX() + "x" + videoModes.get(i).getResolutionY());
            }


            for(int j = 0; j<videoModes.size(); j++){
                VideoMode videoMode = videoModes.get(j);
                if(videoMode.getResolutionX()==1208 && videoMode.getResolutionY()==800){
                    videoStream.setVideoMode(videoMode);
                    break;
                }
            }

            startStreamThread();
        }

        @Override
        public void onDeviceOpenFailed(String s) {

            System.out.println("OnDeviceFailed");

            return;
        }


    };

    private void startStreamThread(){
        streamThread = new Thread() {
            @Override
            public void run(){
                List <VideoStream> streams = new ArrayList<>();
                streams.add(videoStream);

                videoStream.start();

                while(startStream){
                    try{
                        OpenNI.waitForAnyStream(streams, 2000);
                    } catch (TimeoutException e){
                        e.printStackTrace();
                    }
                    synchronized (m_sync) {
                        if(videoStream!=null){
                            //VideoFrameRef videoFrameRef = videoStream.readFrame();
                            //ByteBuffer buf = videoFrameRef.getData();
                            openNIView.update(videoStream);
                            //videoFrameRef.release();

                        }
                    }

                }
            }
        };
        streamThread.start();
    }

    /*VideoStream.NewFrameListener newFrameListener = new VideoStream.NewFrameListener() {
        @Override
        public void onFrameReady(VideoStream videoStream) {
            if(videoStream!=null){
                VideoFrameRef videoFrameRef = videoStream.readFrame();
                openNIView.update(videoFrameRef);
                videoFrameRef.release();
            }
        }
    };*/

    private void stopStream(){
        startStream = false;
        if(streamThread != null){
            try{
                streamThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(videoStream != null){
            videoStream.stop();
        }

        //videoStream.removeNewFrameListener(newFrameListener);
    }

    private void destroyStream(){
        if(videoStream!=null){
            videoStream.destroy();
            videoStream=null;
        }
    }

    private void closeDevice(){
        if(device != null){
            device.close();
        }
    }

    private void shutdownOpenNIHelper(){
        if(openNIHelper!=null){
            Log.i(TAG, "shutdownOpenNIHelper");
            openNIHelper.shutdown();
            openNIHelper=null;
        }
    }

    protected void onDestroy() {
        OpenNI.shutdown();
        super.onDestroy();
    }
}
