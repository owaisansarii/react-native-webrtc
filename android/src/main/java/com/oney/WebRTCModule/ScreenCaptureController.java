package com.oney.WebRTCModule;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.OrientationEventListener;

import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.VideoCapturer;

public class ScreenCaptureController extends AbstractVideoCaptureController {
    /**
     * The {@link Log} tag with which {@code ScreenCaptureController} is to log.
     */
    private static final String TAG = ScreenCaptureController.class.getSimpleName();

    private static final int DEFAULT_FPS = 30;

    private final Intent mediaProjectionPermissionResultData;

    private final OrientationEventListener orientationListener;

    private final Context context;

    private boolean isCapturing;

    public ScreenCaptureController(Context context,
                                   int width,
                                   int height,
                                   Intent mediaProjectionPermissionResultData) {
        super(width, height, DEFAULT_FPS);

        this.mediaProjectionPermissionResultData = mediaProjectionPermissionResultData;

        this.context = context;

        this.isCapturing = false;

        this.orientationListener = new OrientationEventListener(context) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (isCapturing) {
                    Log.d(TAG, "Orientation changed to " + orientation);
                    try {
                        DisplayMetrics displayMetrics = DisplayUtils.getDisplayMetrics((Activity) context);
                        Log.d(TAG, "Display metrics: " + displayMetrics.widthPixels + "x" + displayMetrics.heightPixels);
                        final int width = displayMetrics.widthPixels;
                        final int height = displayMetrics.heightPixels;

                        // Pivot to the executor thread because videoCapturer.changeCaptureFormat runs in the main
                        // thread and may deadlock.
                        ThreadUtils.runOnExecutor(() -> {
                        try {
                            videoCapturer.changeCaptureFormat(width, height, DEFAULT_FPS);
                        } catch (Exception ex) {
                             Log.e(TAG, "Error changing capture format", ex);
                            // We ignore exceptions here. The video capturer runs on its own
                            // thread and we cannot synchronize with it.
                        }
                    });    
                    } catch (Exception e) {
                        Log.e(TAG, "Error stopping video capturer", e);
                    }
                    
                }
            }
        };

        if (this.orientationListener.canDetectOrientation()) {
            this.orientationListener.enable();
        }
    }

    @Override
    public void dispose() {
        MediaProjectionService.abort(context);
        orientationListener.disable();
        super.dispose();
    }

    @Override
    protected VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer =
                new ScreenCapturerAndroid(mediaProjectionPermissionResultData, new MediaProjection.Callback() {
                    @Override
                    public void onStop() {
                        Log.w(TAG, "Media projection stopped.");
                        orientationListener.disable();
                        stopCapture();
                        isCapturing = false;
                        if (capturerEventsListener != null) {
                            capturerEventsListener.onCapturerEnded();
                        }
                    }
                });

        return videoCapturer;
    }
}
