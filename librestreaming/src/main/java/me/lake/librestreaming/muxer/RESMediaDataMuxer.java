package me.lake.librestreaming.muxer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import java.nio.ByteBuffer;

/**
 * Created by hukanli on 2017_12_29
 * E-Mail: hukanli@hys-inc.cn
 * Copyright: Copyright (c) 2017
 * Title:
 * Description:
 */
public interface RESMediaDataMuxer {
    int TYPE_AUDIO = 0;
    int TYPE_VIDEO = 1;
    void addMediaFormat(MediaCodec.BufferInfo info, MediaFormat format, int type);
    void addRealData(MediaCodec.BufferInfo info, ByteBuffer data, int type);
}
