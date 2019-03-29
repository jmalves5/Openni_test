package com.example.testing;

import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.openni.CropArea;
import org.openni.Device;
import org.openni.DeviceInfo;
import org.openni.ImageRegistrationMode;
import org.openni.OpenNI;
import org.openni.PixelFormat;
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
    private OpenNIView openNIView2;
    private OpenNIHelper openNIHelper;
    private Device device;
    private VideoStream videoStream;
    private VideoStream video2Stream;
    private Thread streamThread;
    private boolean startStream = true;
    private final Object m_sync = new Object();




    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OpenNI.setLogAndroidOutput(true);
        OpenNI.setLogMinSeverity(0);
        OpenNI.initialize();

        setContentView(R.layout.activity_simple_viewer);
        openNIView = findViewById(R.id.frame1View);
        openNIView2 = findViewById(R.id.frame2View);

        openNIHelper = new OpenNIHelper(getApplicationContext());
        openNIHelper.requestDeviceOpen(deviceOpenListener);
    }

    OpenNIHelper.DeviceOpenListener deviceOpenListener = new OpenNIHelper.DeviceOpenListener() {
        @Override
        public void onDeviceOpened(UsbDevice usbDevice) {
            //list devices
            List<DeviceInfo> deviceList=OpenNI.enumerateDevices();
            if(deviceList.size()==0){
                return;
            }
            //open the device
            for (int i = 0; i < deviceList.size(); i ++){
                if(deviceList.get(i).getUsbProductId() == usbDevice.getProductId()){
                    device = Device.open();
                }
            }

            //create video streams
            if(device!=null){
                videoStream = VideoStream.create(device, SensorType.DEPTH);
                video2Stream = VideoStream.create(device, SensorType.COLOR);
            }

            //some experiments with device
            device.setImageRegistrationMode(ImageRegistrationMode.DEPTH_TO_COLOR);
            //device.setDepthColorSyncEnabled(true);
            //device.setDepthOptimizationEnable(true);

            //FIRST STREAM
            Log.i("VISLAB-IGOR", "stream chosen: >>> "+ videoStream.getSensorType()  + " <<< ");
            Log.i("VISLAB-IGOR", "BEFORE STREAM Videomode: "+ videoStream.getVideoMode().getResolutionX() + "x" + videoStream.getVideoMode().getResolutionY()
                    +" pf:"+videoStream.getVideoMode().getPixelFormat() + " fps:"+ videoStream.getVideoMode().getFps());
            //change video-mode
            List<VideoMode> videoModes = videoStream.getSensorInfo().getSupportedVideoModes();
            for (VideoMode mode : videoModes) {
                int X = mode.getResolutionX();
                int Y = mode.getResolutionY();
                int fps = mode.getFps();
                Log.i("VISLAB-IGOR", "Supported video-mode: "+ X + "x" + Y +" pf:"+mode.getPixelFormat()+ " fps:"+ fps);
                //set this video-mode
                if (X == 640 && Y == 480 && mode.getPixelFormat() == PixelFormat.DEPTH_1_MM) {
                    videoStream.setVideoMode(mode);
                    Log.i("VISLAB-IGOR", ">>>>>> SET VIDEO-MODE: " + X + "x" + Y + mode.getPixelFormat());
                }
            }
            Log.i("VISLAB-IGOR", "AFTER STREAM Video-mode: "+ videoStream.getVideoMode().getResolutionX() + "x" + videoStream.getVideoMode().getResolutionY()
                    +" pf:"+videoStream.getVideoMode().getPixelFormat()+ " fps:"+ videoStream.getVideoMode().getFps());

            //SECOND STREAM
            Log.i("VISLAB-IGOR", "stream chosen: >>> "+ video2Stream.getSensorType()  + " <<< ");
            Log.i("VISLAB-IGOR", "BEFORE 2STREAM Videomode: "+ video2Stream.getVideoMode().getResolutionX() + "x" + video2Stream.getVideoMode().getResolutionY()
                    +" pf:"+video2Stream.getVideoMode().getPixelFormat()+ " fps:"+ video2Stream.getVideoMode().getFps());
            //change video-mode
            videoModes = video2Stream.getSensorInfo().getSupportedVideoModes();
            for (VideoMode mode : videoModes) {
                int X = mode.getResolutionX();
                int Y = mode.getResolutionY();
                int fps = mode.getFps();
                Log.i("VISLAB-IGOR", "Supported 2video-mode: "+ X + "x" + Y +" pf:"+mode.getPixelFormat()+ " fps:"+ fps);
                //set this video-mode
                if (X == 640 && Y == 480 && mode.getPixelFormat() == PixelFormat.YUYV && mode.getFps()==30) {
                    video2Stream.setVideoMode(mode);
                    Log.i("VISLAB-IGOR", ">>>>>> SET 2VIDEO-MODE: " + X + "x" + Y + mode.getPixelFormat());
                }
            }
            Log.i("VISLAB-IGOR", "AFTER 2STREAM Video-mode: "+ video2Stream.getVideoMode().getResolutionX() + "x" + video2Stream.getVideoMode().getResolutionY()
                    +" pf:"+video2Stream.getVideoMode().getPixelFormat()+ " fps:"+ video2Stream.getVideoMode().getFps());

            //starting the thread to avoid freezing GUI
            startStreamThread();
        }

        @Override
        public void onDeviceOpenFailed(String s) {
            System.out.println("OnDeviceFailed");
        }


    };

    private void startStreamThread(){
        streamThread = new Thread() {
            @Override
            public void run(){
                //list of streams
                List <VideoStream> streams = new ArrayList<>();
                //adding to the list the streams
                streams.add(videoStream);
                streams.add(video2Stream);

                //starting the stream
                videoStream.start();
                video2Stream.start();


                while(startStream){
                    try{
                        //waiting for stream
                        OpenNI.waitForAnyStream(streams, 2000);
                    } catch (TimeoutException e){
                        e.printStackTrace();
                    }
                    //synchronized
                    synchronized (m_sync) {
                        if(videoStream!=null){
                            //VideoFrameRef videoFrameRef = videoStream.readFrame();
                            //ByteBuffer buf = videoFrameRef.getData();
                            //update view
                            openNIView.update(videoStream);
                            openNIView2.update(video2Stream);
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
        //stopping stream
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
