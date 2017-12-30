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
public class AudioSenderThread extends Thread {
    private static final long WAIT_TIME = 5000;//1ms;
    private MediaCodec.BufferInfo eInfo;
    private MediaCodec dstAudioEncoder;
    private RESMediaDataMuxer dataMuxer;

    AudioSenderThread(String name, MediaCodec encoder, RESMediaDataMuxer muxer) {
        super(name);
        eInfo = new MediaCodec.BufferInfo();
        dstAudioEncoder = encoder;
        dataMuxer = muxer;
    }

    private boolean shouldQuit = false;

    void quit() {
        shouldQuit = true;
        this.interrupt();
    }

    @Override
    public void run() {
        while (!shouldQuit) {
            int eobIndex = dstAudioEncoder.dequeueOutputBuffer(eInfo, WAIT_TIME);
            switch (eobIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    LogTools.d("AudioSenderThread,MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
//                        LogTools.d("AudioSenderThread,MediaCodec.INFO_TRY_AGAIN_LATER");
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    LogTools.d("AudioSenderThread,MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:" +
                            dstAudioEncoder.getOutputFormat().toString());
                    MediaFormat format = dstAudioEncoder.getOutputFormat();
                    dataMuxer.addMediaFormat(eInfo, format,RESMediaDataMuxer.TYPE_AUDIO);
                    break;
                default:
                    LogTools.d("AudioSenderThread,MediaCode,eobIndex=" + eobIndex);

                    /**
                     * we send audio SpecificConfig already in INFO_OUTPUT_FORMAT_CHANGED
                     * so we ignore MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                     */
                    if (eInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG && eInfo.size != 0) {
                        ByteBuffer realData = dstAudioEncoder.getOutputBuffers()[eobIndex];
                        dataMuxer.addRealData(eInfo, realData, RESMediaDataMuxer.TYPE_AUDIO);
                    }
                    dstAudioEncoder.releaseOutputBuffer(eobIndex, false);
                    break;
            }
        }
        eInfo = null;
    }
}
