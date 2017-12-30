package me.lake.librestreaming.rtmp;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

import me.lake.librestreaming.core.Packager;
import me.lake.librestreaming.muxer.RESMediaDataMuxer;

/**
 * Created by hukanli on 2017_12_29
 * E-Mail: hukanli@hys-inc.cn
 * Copyright: Copyright (c) 2017
 * Title:
 * Description:
 */
public class RESFlvDataMuxer implements RESMediaDataMuxer {
    private RESFlvDataCollecter mDataCollecter;
    private RESRtmpSender mRtmpSender;
    public RESFlvDataMuxer(RESRtmpSender rtmpSender) {
        mRtmpSender = rtmpSender;
        mDataCollecter = new RESFlvDataCollecter() {
            @Override
            public void collect(RESFlvData flvData, int type) {
                mRtmpSender.feed(flvData, type);
            }
        };
    }
    private long videoStartTime = 0;
    private long audioStartTime = 0;
    private void sendAVCDecoderConfigurationRecord(long tms, MediaFormat format) {
        byte[] AVCDecoderConfigurationRecord = Packager.H264Packager.generateAVCDecoderConfigurationRecord(format);
        int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                AVCDecoderConfigurationRecord.length;
        byte[] finalBuff = new byte[packetLen];
        Packager.FLVPackager.fillFlvVideoTag(finalBuff,
                0,
                true,
                true,
                AVCDecoderConfigurationRecord.length);
        System.arraycopy(AVCDecoderConfigurationRecord, 0,
                finalBuff, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH, AVCDecoderConfigurationRecord.length);
        RESFlvData resFlvData = new RESFlvData();
        resFlvData.droppable = false;
        resFlvData.byteBuffer = finalBuff;
        resFlvData.size = finalBuff.length;
        resFlvData.dts = (int) tms;
        resFlvData.flvTagType = RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO;
        resFlvData.videoFrameType = RESFlvData.NALU_TYPE_IDR;
        mDataCollecter.collect(resFlvData, RESRtmpSender.FROM_VIDEO);
    }

    private void sendVideoRealData(long tms, ByteBuffer realData) {
        int realDataLength = realData.remaining();
        int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                Packager.FLVPackager.NALU_HEADER_LENGTH +
                realDataLength;
        byte[] finalBuff = new byte[packetLen];
        realData.get(finalBuff, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                        Packager.FLVPackager.NALU_HEADER_LENGTH,
                realDataLength);
        int frameType = finalBuff[Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                Packager.FLVPackager.NALU_HEADER_LENGTH] & 0x1F;
        Packager.FLVPackager.fillFlvVideoTag(finalBuff,
                0,
                false,
                frameType == 5,
                realDataLength);
        RESFlvData resFlvData = new RESFlvData();
        resFlvData.droppable = true;
        resFlvData.byteBuffer = finalBuff;
        resFlvData.size = finalBuff.length;
        resFlvData.dts = (int) tms;
        resFlvData.flvTagType = RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO;
        resFlvData.videoFrameType = frameType;
        mDataCollecter.collect(resFlvData, RESRtmpSender.FROM_VIDEO);
    }


    private void sendAudioSpecificConfig(long tms, MediaFormat format) {
        ByteBuffer realData = format.getByteBuffer("csd-0");
        int packetLen = Packager.FLVPackager.FLV_AUDIO_TAG_LENGTH +
                realData.remaining();
        byte[] finalBuff = new byte[packetLen];
        realData.get(finalBuff, Packager.FLVPackager.FLV_AUDIO_TAG_LENGTH,
                realData.remaining());
        Packager.FLVPackager.fillFlvAudioTag(finalBuff,
                0,
                true);
        RESFlvData resFlvData = new RESFlvData();
        resFlvData.droppable = false;
        resFlvData.byteBuffer = finalBuff;
        resFlvData.size = finalBuff.length;
        resFlvData.dts = (int) tms;
        resFlvData.flvTagType = RESFlvData.FLV_RTMP_PACKET_TYPE_AUDIO;
        mDataCollecter.collect(resFlvData, RESRtmpSender.FROM_AUDIO);
    }

    private void sendAudioRealData(long tms, ByteBuffer realData) {
        int packetLen = Packager.FLVPackager.FLV_AUDIO_TAG_LENGTH +
                realData.remaining();
        byte[] finalBuff = new byte[packetLen];
        realData.get(finalBuff, Packager.FLVPackager.FLV_AUDIO_TAG_LENGTH,
                realData.remaining());
        Packager.FLVPackager.fillFlvAudioTag(finalBuff,
                0,
                false);
        RESFlvData resFlvData = new RESFlvData();
        resFlvData.droppable = true;
        resFlvData.byteBuffer = finalBuff;
        resFlvData.size = finalBuff.length;
        resFlvData.dts = (int) tms;
        resFlvData.flvTagType = RESFlvData.FLV_RTMP_PACKET_TYPE_AUDIO;
        mDataCollecter.collect(resFlvData, RESRtmpSender.FROM_AUDIO);
    }

    @Override
    public void addMediaFormat(MediaCodec.BufferInfo info, MediaFormat format, int type) {
        switch (type) {
            case TYPE_AUDIO:
                sendAudioSpecificConfig(0, format);
                break;
            case TYPE_VIDEO:
                sendAVCDecoderConfigurationRecord(0, format);
                break;
        }
    }

    @Override
    public void addRealData(MediaCodec.BufferInfo eInfo, ByteBuffer realData, int type) {
        switch (type) {
            case TYPE_AUDIO:
                if (audioStartTime == 0) {
                    audioStartTime = eInfo.presentationTimeUs / 1000;
                }
                realData.position(eInfo.offset);
                realData.limit(eInfo.offset + eInfo.size);
                sendAudioRealData((eInfo.presentationTimeUs / 1000) - audioStartTime, realData);
                break;
            case TYPE_VIDEO:
                if (videoStartTime == 0) {
                    videoStartTime = eInfo.presentationTimeUs / 1000;
                }
                realData.position(eInfo.offset + 4);
                realData.limit(eInfo.offset + eInfo.size);
                sendVideoRealData((eInfo.presentationTimeUs / 1000) - videoStartTime, realData);
                break;
        }
    }
}
