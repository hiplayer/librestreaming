package me.lake.librestreaming.mpeg4;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.SystemClock;

import java.nio.ByteBuffer;

import me.lake.librestreaming.muxer.RESMediaDataMuxer;
import me.lake.librestreaming.tools.LogTools;

/**
 * Created by hukanli on 2017_12_30
 * E-Mail: hukanli@hys-inc.cn
 * Copyright: Copyright (c) 2017
 * Title:
 * Description:
 */
public class RESMpeg4Muxer implements RESMediaDataMuxer {
    RESMpeg4Writer mWriter;
    public RESMpeg4Muxer(RESMpeg4Writer writer) {
        mWriter = writer;

    }
    @Override
    public void addMediaFormat(MediaCodec.BufferInfo info, MediaFormat format, int type) {

        RESMpeg4Data data = new RESMpeg4Data();

        MediaCodec.BufferInfo copy = new MediaCodec.BufferInfo();
        switch (type) {
            case TYPE_AUDIO:
                copy.set(info.offset, info.size, info.presentationTimeUs, info.flags);
                data.bufferInfo = copy;
                data.format = format;
                data.type = TYPE_AUDIO;
                mWriter.feed(data);
                break;
            case TYPE_VIDEO:
                copy.set(info.offset, info.size, info.presentationTimeUs, info.flags);
                data.bufferInfo = copy;
                data.format = format;
                data.type = TYPE_VIDEO;
                mWriter.feed(data);
                break;
        }
    }

    @Override
    public void addRealData(MediaCodec.BufferInfo info, ByteBuffer byteBuffer, int type) {
        RESMpeg4Data data = new RESMpeg4Data();

        MediaCodec.BufferInfo copy = new MediaCodec.BufferInfo();
        switch (type) {
            case TYPE_AUDIO:
                byteBuffer.position(info.offset);
                byteBuffer.limit(info.offset + info.size);
                copy.set(info.offset, info.size, info.presentationTimeUs, info.flags);
                LogTools.e("addRealData presentationTimeUs: "+info.presentationTimeUs);
                data.type = TYPE_AUDIO;
                data.bufferInfo = copy;
                data.encodedData = byteBuffer.duplicate();
                mWriter.feed(data);
                break;
            case TYPE_VIDEO:
                byteBuffer.position(info.offset);
                byteBuffer.limit(info.offset + info.size);
                copy.set(info.offset, info.size, info.presentationTimeUs, info.flags);
                data.type = TYPE_VIDEO;
                data.bufferInfo = copy;
                data.encodedData = byteBuffer.duplicate();
                mWriter.feed(data);
                break;
        }
    }
}
