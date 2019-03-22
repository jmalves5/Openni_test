package com.example.testing;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.openni.Device;
import org.openni.DeviceInfo;
import org.openni.OpenNI;
import org.openni.SensorType;
import org.openni.VideoFrameRef;
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




    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("myTag", "RIP1");
        OpenNI.setLogAndroidOutput(true);
        OpenNI.setLogMinSeverity(0);
        OpenNI.initialize();

        setContentView(R.layout.activity_main);

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
            Log.d("myTag", "RIP3");
            for (int i = 0; i<deviceInfos.size(); i++){
                if(deviceInfos.get(i).getUsbProductId()==usbDevice.getProductId()){
                    device = Device.open();
                    break;
                }
            }
            Log.d("myTag", "RIP4");
            if(device!=null){
                videoStream = VideoStream.create(device, SensorType.DEPTH);
                Log.d("myTag", "RIP5");
            }

            List<VideoMode> videoModes= videoStream.getSensorInfo().getSupportedVideoModes();
            System.out.println("RIP" + videoModes);
            for(int j = 0; j<videoModes.size(); j++){
                VideoMode videoMode = videoModes.get(j);
                if(videoMode.getResolutionX()==640 && videoMode.getResolutionY()==400){
                    videoStream.setVideoMode(videoMode);
                    break;
                }
            }

            startStreamThread();
        }

        @Override
        public void onDeviceOpenFailed(String s) {

            return;
        }


    };

    private synchronized void startStreamThread(){
        streamThread = new Thread() {
            @Override
            public void run(){
                List<VideoStream> streams = new ArrayList();
                streams.add(videoStream);
                videoStream.start();

                while(startStream){
                    try{
                        OpenNI.waitForAnyStream(streams, 2000);
                    } catch (TimeoutException e){
                        e.printStackTrace();
                    }
                    Log.d("myTag", "RIP6");

                    if(videoStream!=null){
                        VideoFrameRef videoFrameRef = videoStream.readFrame();
                        openNIView.update(videoFrameRef);
                        videoFrameRef.release();
                    }

                }
            }
        };
        streamThread.start();
    }

    VideoStream.NewFrameListener newFrameListener = new VideoStream.NewFrameListener() {
        @Override
        public void onFrameReady(VideoStream videoStream) {
            if(videoStream!=null){
                VideoFrameRef videoFrameRef = videoStream.readFrame();
                openNIView.update(videoFrameRef);
                videoFrameRef.release();
            }
        }
    };

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

        videoStream.removeNewFrameListener(newFrameListener);
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
