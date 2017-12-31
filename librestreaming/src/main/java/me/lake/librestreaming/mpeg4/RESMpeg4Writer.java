package me.lake.librestreaming.mpeg4;

import android.media.MediaMuxer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;

import me.lake.librestreaming.core.RESByteSpeedometer;
import me.lake.librestreaming.core.RESFrameRateMeter;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.muxer.RESMediaDataAbstractSender;
import me.lake.librestreaming.tools.LogTools;

/**
 * Created by hukanli on 2017_12_30
 * E-Mail: hukanli@hys-inc.cn
 * Copyright: Copyright (c) 2017
 * Title:
 * Description:
 */
public class RESMpeg4Writer extends RESMediaDataAbstractSender {

    private static final int TIMEGRANULARITY = 3000;
    private final Object syncOp = new Object();
    private WorkHandler workHandler;
    private HandlerThread workHandlerThread;

    @Override
    public void prepare(RESCoreParameters coreParameters) {
        synchronized (syncOp) {
            workHandlerThread = new HandlerThread("RESMpeg4Writer,workHandlerThread");
            workHandlerThread.start();
                workHandler = new RESMpeg4Writer.WorkHandler(coreParameters.senderQueueLength,
                        workHandlerThread.getLooper());
        }
    }

    public void feed(RESMpeg4Data data) {
        synchronized (syncOp) {
            workHandler.sendFood(data);
        }
    }

    @Override
    public void start(String senderAddr) {
        synchronized (syncOp) {
            workHandler.sendStart(senderAddr);
        }
    }

    @Override
    public void resume() {
        synchronized (syncOp) {
            workHandler.sendResume();
        }
    }

    @Override
    public void pause() {
        synchronized (syncOp) {
            workHandler.sendPause();
        }
    }

    @Override
    public void stop() {
        synchronized (syncOp) {
            workHandler.sendStop();
        }
    }

    @Override
    public void destroy() {
        synchronized (syncOp) {
            workHandler.removeCallbacksAndMessages(null);
            workHandlerThread.quit();
        }
    }

    public float getSendFrameRate() {
        synchronized (syncOp) {
            return workHandler == null ? 0 : workHandler.getSendFrameRate();
        }
    }

    public float getSendBufferFreePercent() {
        synchronized (syncOp) {
            return workHandler == null ? 0 : workHandler.getSendBufferFreePercent();
        }
    }

    static class WorkHandler extends Handler {

        private final static int MSG_START = 1;
        private final static int MSG_WRITE = 2;
        private final static int MSG_STOP = 3;
        private final static int MSG_PAUSE = 4;
        private final static int MSG_RESUME = 5;

        private int audioTrackIndex = -1;
        private int videoTrackIndex = -1;

        private int maxQueueLength;
        private int writeMsgNum = 0;
        private final Object syncWriteMsgNum = new Object();
        private RESByteSpeedometer videoByteSpeedometer = new RESByteSpeedometer(TIMEGRANULARITY);
        private RESByteSpeedometer audioByteSpeedometer = new RESByteSpeedometer(TIMEGRANULARITY);
        private RESFrameRateMeter sendFrameRateMeter = new RESFrameRateMeter();
        private MediaMuxer mMuxer;

        private enum STATE {
            IDLE,
            PREPARING,
            RUNNING,
            STOPPED,
            PAUSED
        }

        private STATE state;

        WorkHandler(int maxQueueLength, Looper looper) {
            super(looper);
            this.maxQueueLength = maxQueueLength;
            state = STATE.IDLE;
        }

        public float getSendFrameRate() {
            return sendFrameRateMeter.getFps();
        }

        public float getSendBufferFreePercent() {
            synchronized (syncWriteMsgNum) {
                float res = (float) (maxQueueLength - writeMsgNum) / (float) maxQueueLength;
                return res <= 0 ? 0f : res;
            }
        }

        long audioStartTime = -1;
        long videoStartTime = -1;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START:
                    if (state == STATE.IDLE) {
                        sendFrameRateMeter.reSet();
                        try {
                            mMuxer = new MediaMuxer((String) msg.obj, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                        } catch (IOException e) {
                            LogTools.e("IOException: " + e.toString());
                        }
                        state = STATE.PREPARING;
                        audioStartTime = -1;
                        videoStartTime = -1;
                    }
                    break;
                case MSG_WRITE: {
                    if (state == STATE.PREPARING) {
                        RESMpeg4Data data = (RESMpeg4Data) msg.obj;
                        /*
                        if (data.format == null && data.encodedData != null) {
                            Message newMsg = this.obtainMessage();
                            newMsg.copyFrom(msg);
                            this.sendMessageDelayed(newMsg, 1000);
                            break;
                        }
                        */
                        if (data.format == null && data.encodedData != null) {
                            break;
                        }

                        switch (data.type) {
                            case RESMpeg4Muxer.TYPE_AUDIO:
                                audioTrackIndex = mMuxer.addTrack(data.format);
                                break;
                            case RESMpeg4Muxer.TYPE_VIDEO:
                                videoTrackIndex = mMuxer.addTrack(data.format);
                                break;
                        }
                        if (audioTrackIndex >= 0 && videoTrackIndex >= 0) {
                            mMuxer.start();
                            state = STATE.RUNNING;
                        }

                    } else if (state == STATE.RUNNING) {
                        RESMpeg4Data data = (RESMpeg4Data) msg.obj;
                        switch (data.type) {
                            case RESMpeg4Muxer.TYPE_AUDIO:
                                if (audioStartTime < 0) {
                                    audioStartTime = data.bufferInfo.presentationTimeUs;
                                }data.bufferInfo.presentationTimeUs -= audioStartTime;
                                //LogTools.e("audio presentationTimeUs: " + data.bufferInfo.presentationTimeUs);
                                mMuxer.writeSampleData(audioTrackIndex, data.encodedData, data.bufferInfo);
                                audioByteSpeedometer.gain(data.bufferInfo.size);
                                break;
                            case RESMpeg4Muxer.TYPE_VIDEO:

                                if (videoStartTime < 0) {
                                    videoStartTime = data.bufferInfo.presentationTimeUs;
                                }
                                data.bufferInfo.presentationTimeUs -= videoStartTime;
                                //LogTools.e("video presentationTimeUs: " + data.bufferInfo.presentationTimeUs);
                                mMuxer.writeSampleData(videoTrackIndex, data.encodedData, data.bufferInfo);
                                videoByteSpeedometer.gain(data.bufferInfo.size);
                                sendFrameRateMeter.count();
                                break;
                        }
                    }
                    synchronized (syncWriteMsgNum) {
                        writeMsgNum--;
                    }
                    break;
                }
                case MSG_STOP:
                    if (state != STATE.RUNNING) {
                        break;
                    }
                    state = STATE.STOPPED;
                    mMuxer.stop();
                    mMuxer.release();
                    break;
                default:
                    break;
            }
        }

        public void sendStart(String senderAddr) {
            this.removeMessages(MSG_START);
            synchronized (syncWriteMsgNum) {
                this.removeMessages(MSG_WRITE);
                writeMsgNum = 0;
            }
            this.sendMessage(this.obtainMessage(MSG_START, senderAddr));
        }

        public void sendStop() {
            this.removeMessages(MSG_STOP);
            synchronized (syncWriteMsgNum) {
                this.removeMessages(MSG_WRITE);
                writeMsgNum = 0;
            }
            this.sendEmptyMessage(MSG_STOP);
        }

        public void sendPause() {
        }

        public void sendResume() {
        }

        public void sendFood(RESMpeg4Data mpeg4Data) {
            synchronized (syncWriteMsgNum) {
                //LAKETODO optimize
                if (writeMsgNum <= maxQueueLength) {
                    this.sendMessage(this.obtainMessage(MSG_WRITE, 0, 0, mpeg4Data));
                    ++writeMsgNum;
                } else {
                    LogTools.d("senderQueue is full,abandon");
                }
            }
        }

    }
}
