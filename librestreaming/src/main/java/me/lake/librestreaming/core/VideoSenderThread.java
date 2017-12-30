package me.lake.librestreaming.core;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

import me.lake.librestreaming.muxer.RESMediaDataMuxer;
import me.lake.librestreaming.rtmp.RESFlvData;
import me.lake.librestreaming.rtmp.RESFlvDataCollecter;
import me.lake.librestreaming.rtmp.RESRtmpSender;
import me.lake.librestreaming.tools.LogTools;

/**
 * Created by lakeinchina on 26/05/16.
 */
public class VideoSenderThread extends Thread {
    private static final long WAIT_TIME = 5000;
    private MediaCodec.BufferInfo eInfo;
    private MediaCodec dstVideoEncoder;
    private final Object syncDstVideoEncoder = new Object();
    private RESMediaDataMuxer dataMuxer;

    VideoSenderThread(String name, MediaCodec encoder, RESMediaDataMuxer muxer) {
        super(name);
        eInfo = new MediaCodec.BufferInfo();
        dstVideoEncoder = encoder;
        dataMuxer = muxer;
    }

    public void updateMediaCodec(MediaCodec encoder) {
        synchronized (syncDstVideoEncoder) {
            dstVideoEncoder = encoder;
        }
    }

    private boolean shouldQuit = false;

    void quit() {
        shouldQuit = true;
        this.interrupt();
    }

    @Override
    public void run() {
        while (!shouldQuit) {
            synchronized (syncDstVideoEncoder) {
                int eobIndex = MediaCodec.INFO_TRY_AGAIN_LATER;
                try {
                    eobIndex = dstVideoEncoder.dequeueOutputBuffer(eInfo, WAIT_TIME);
                } catch (Exception ignored) {
                }
                switch (eobIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        LogTools.d("VideoSenderThread,MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
//                        LogTools.d("VideoSenderThread,MediaCodec.INFO_TRY_AGAIN_LATER");
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        LogTools.d("VideoSenderThread,MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:" +
                                dstVideoEncoder.getOutputFormat().toString());
                        dataMuxer.addMediaFormat(eInfo, dstVideoEncoder.getOutputFormat(), RESMediaDataMuxer.TYPE_VIDEO);
                        break;
                    default:
                        LogTools.d("VideoSenderThread,MediaCode,eobIndex=" + eobIndex);

                        /**
                         * we send sps pps already in INFO_OUTPUT_FORMAT_CHANGED
                         * so we ignore MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                         */
                        if (eInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG && eInfo.size != 0) {
                            ByteBuffer realData = dstVideoEncoder.getOutputBuffers()[eobIndex];
                            dataMuxer.addRealData(eInfo, realData, RESMediaDataMuxer.TYPE_VIDEO);
                        }
                        dstVideoEncoder.releaseOutputBuffer(eobIndex, false);
                        break;
                }
            }
            try {
                sleep(5);
            } catch (InterruptedException ignored) {
            }
        }
        eInfo = null;
    }
}