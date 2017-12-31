package me.lake.librestreaming.mpeg4;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

/**
 * Created by hukanli on 2017_12_30
 * E-Mail: hukanli@hys-inc.cn
 * Copyright: Copyright (c) 2017
 * Title:
 * Description:
 */
public class RESMpeg4Data {
    int type;
    ByteBuffer encodedData;
    MediaFormat format;
    MediaCodec.BufferInfo bufferInfo;
}
