package com.pompushka.camexpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback{
    private SurfaceHolder mHolder;
    private Camera mCamera;
    
    private String TAG = "CameraEx";
    
  //This variable is responsible for getting and setting the camera settings  
    private Parameters parameters;  
    //this variable stores the camera preview size   
    private Size previewSize;  
    //this array stores the pixels as hexadecimal pairs   
    private int[] pixels;
    
    private FilterView fW;

    public CameraPreview(Context context, Camera camera, FilterView view) {
        super(context);
        mCamera = camera;
        
        fW = view;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            parameters = mCamera.getParameters();
            parameters.getSupportedPreviewFormats().toString();
            mCamera.setParameters(parameters);

            mCamera.setPreviewDisplay(holder);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();
            
            parameters = mCamera.getParameters();  
            previewSize = parameters.getPreviewSize();  
            pixels = new int[previewSize.width * previewSize.height]; 
            
            Log.d(TAG, parameters.getSupportedPreviewFormats().toString());
            
            
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            mCamera.release();  
            mCamera = null; 
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    	mCamera.stopPreview();  
        mCamera.release();  
        mCamera = null; 
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        if (mHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }

        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        try {
        	parameters.setPreviewSize(w, h);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(this);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

	@Override
	public void onPreviewFrame(byte[] arg0, Camera arg1) {
		decodeYUV420SP(pixels, arg0, previewSize.width,  previewSize.height);
		YuvImage yuvImage = new YuvImage(arg0, ImageFormat.NV21, previewSize.width, previewSize.height, null);
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		yuvImage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);
		byte [] imageData = baos.toByteArray();
		Bitmap previewBitmap = BitmapFactory.decodeByteArray(imageData , 0, imageData.length);
		fW.setPreviewBmp(previewBitmap);
		Log.d(TAG, "The top right pixel has the following RGB (hexadecimal) values:"  
                 +Integer.toHexString(previewBitmap.getPixel(0, 0)));
	}
	
	void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {  
        
        final int frameSize = width * height;  

        for (int j = 0, yp = 0; j < height; j++) {       int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;  
          for (int i = 0; i < width; i++, yp++) {  
            int y = (0xff & ((int) yuv420sp[yp])) - 16;  
            if (y < 0)  
              y = 0;  
            if ((i & 1) == 0) {  
              v = (0xff & yuv420sp[uvp++]) - 128;  
              u = (0xff & yuv420sp[uvp++]) - 128;  
            }  

            int y1192 = 1192 * y;  
            int r = (y1192 + 1634 * v);  
            int g = (y1192 - 833 * v - 400 * u);  
            int b = (y1192 + 2066 * u);  

            if (r < 0)                  r = 0;               else if (r > 262143)  
               r = 262143;  
            if (g < 0)                  g = 0;               else if (g > 262143)  
               g = 262143;  
            if (b < 0)                  b = 0;               else if (b > 262143)  
               b = 262143;  

            rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);  
          }  
        }  
      }  
}