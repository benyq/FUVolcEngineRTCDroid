package com.ss.video.rtc.demo.quickstart;


import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import com.faceunity.core.callback.OnReadBitmapCallback;
import com.faceunity.core.enumeration.FUInputBufferEnum;
import com.faceunity.core.enumeration.FUInputTextureEnum;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.utils.BitmapUtils;
import com.faceunity.core.utils.GlUtil;
import com.faceunity.nama.FUConfig;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.utils.FuDeviceUtils;
import com.ss.bytertc.engine.data.VideoPixelFormat;
import com.ss.bytertc.engine.utils.LogUtil;
import com.ss.bytertc.engine.video.IVideoProcessor;
import com.ss.bytertc.engine.video.VideoFrame;
import com.ss.bytertc.engine.video.builder.CpuBufferVideoFrameBuilder;
import com.ss.bytertc.engine.video.builder.GLTextureVideoFrameBuilder;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleVideoProcessor extends IVideoProcessor {

    private static final String TAG = "SimpleVideoProcessor";

    private final float[] texMatrix = new float[16];

    int mFrameCount = 0;

    public final FURenderer mFURenderer = FURenderer.getInstance();
    public boolean renderSwitch;

    private int skipFrame = 0;

    public SimpleVideoProcessor() {
        Matrix.setIdentityM(texMatrix, 0);
    }

    @Override
    public VideoFrame processVideoFrame(VideoFrame frame) {
//        Log.d(TAG, "processVideoFrame mFrameCount:" + frame.getTextureID() + ", glcontext: " + EGL14.eglGetCurrentContext());
        try {
            mFrameCount++;
            switch (frame.getPixelFormat()) {
                case kVideoPixelFormatI420:
                    return processI420Frame(frame);
                case kVideoPixelFormatTexture2D:
                    return processTextureFrame(frame);
            }
        } catch (Exception e) {
            Log.e("SimpleVideoProcessor", "processFrame Exception" + e.toString());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onGLEnvInitiated() {
        initGL();
        mFURenderer.init();
    }

    @Override
    public void onGLEnvRelease() {
        releaseGl();
        mFURenderer.release();
    }

    public void setRenderEnable(boolean enabled) {
        renderSwitch = enabled;
    }

    public VideoFrame processI420Frame(VideoFrame frame) {
//        LogUtil.d("SimpleVideoProcessor", "processI420Frame renderSwitch" + renderSwitch);
        if (!renderSwitch) {
            return frame;
        }

        int width = frame.getWidth();
        int height = frame.getHeight();
//        LogUtil.d("SimpleVideoProcessor", "processI420Frame textureID:" + frame.getTextureID() + ",width:" + width + ",height:" + height);
        if (skipFrame > 0) {
            skipFrame--;
            return frame;
        }
        mFURenderer.setInputOrientation(frame.getRotation().value());
        if (FUConfig.DEVICE_LEVEL > FuDeviceUtils.DEVICE_LEVEL_MID){
            //高性能设备
            mFURenderer.cheekFaceNum();
        }
        mFURenderer.setInputBufferType(FUInputBufferEnum.FU_FORMAT_I420_BUFFER);
        int texId = mFURenderer.onDrawFrameDualInput(null,
                frame.getTextureID(), frame.getWidth(),
                frame.getHeight());

        CpuBufferVideoFrameBuilder builder = new CpuBufferVideoFrameBuilder(VideoPixelFormat.kVideoPixelFormatI420);
        builder.setWidth(width)
                .setHeight(height)
                .setRotation(frame.getRotation()) // set rotation back if rotation has not been changed.
                .setTimeStampUs(frame.getTimeStampUs())
                .setColorSpace(frame.getColorSpace())
                .setPlaneData(0, frame.getPlaneData(0))
                .setPlaneData(1, frame.getPlaneData(1))
                .setPlaneData(2, frame.getPlaneData(2))
                .setPlaneStride(0, frame.getPlaneStride(0))
                .setPlaneStride(1, frame.getPlaneStride(1))
                .setPlaneStride(2, frame.getPlaneStride(2))
                .setReleaseCallback(() -> {
                });

        return builder.build();
    }

    public synchronized VideoFrame processTextureFrame(VideoFrame frame) {
        Log.d("SimpleVideoProcessor", "processTextureFrame frame rotation: " + frame.getRotation().value());
        if (!renderSwitch) {
            return frame;
        }

        if (skipFrame > 0) {
            skipFrame--;
            return frame;
        }

        mFURenderer.setInputOrientation(frame.getRotation().value());
        if (FUConfig.DEVICE_LEVEL > FuDeviceUtils.DEVICE_LEVEL_MID){
            //高性能设备
            mFURenderer.cheekFaceNum();
        }
        mFURenderer.setInputTextureType(FUInputTextureEnum.FU_ADM_FLAG_COMMON_TEXTURE);
        int texId = mFURenderer.onDrawFrameDualInput(
                null,
                frame.getTextureID(), frame.getWidth(),
                frame.getHeight()
        );

        if (skipFrame > 0) {
            skipFrame--;
            texId = frame.getTextureID();
        }
//        LogUtil.d("SimpleVideoProcessor", "processTextureFrame-texId:"+texId);
        GLTextureVideoFrameBuilder builder = new GLTextureVideoFrameBuilder(VideoPixelFormat.kVideoPixelFormatTexture2D);
        builder.setEGLContext(EGL14.eglGetCurrentContext())
                .setTextureID(texId)
                .setWidth(frame.getWidth())
                .setHeight(frame.getHeight())
                .setRotation(frame.getRotation())
                .setTimeStampUs(frame.getTimeStampUs());
        return builder.build();
    }

    void initGL() {
        Log.d(TAG, "initGL");
    }

    void releaseGl() {
        Log.d(TAG, "releaseGl");
    }

}
