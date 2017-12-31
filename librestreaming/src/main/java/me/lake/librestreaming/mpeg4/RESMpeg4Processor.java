package me.lake.librestreaming.mpeg4;

import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.muxer.RESMediaDataMuxer;
import me.lake.librestreaming.muxer.RESMediaDataProcessor;
import me.lake.librestreaming.muxer.RESMediaDataSender;

/**
 * Created by hukanli on 2017_12_30
 * E-Mail: hukanli@hys-inc.cn
 * Copyright: Copyright (c) 2017
 * Title:
 * Description:
 */
public class RESMpeg4Processor implements RESMediaDataProcessor {
    private RESMpeg4Muxer mMuxer;
    private RESMpeg4Writer mWriter;
    @Override
    public void prepare(RESCoreParameters coreParameters) {
        mWriter = new RESMpeg4Writer();
        mWriter.prepare(coreParameters);
        mMuxer = new RESMpeg4Muxer(mWriter);
    }

    @Override
    public RESMediaDataMuxer getMuxer() {
        return mMuxer;
    }

    @Override
    public RESMediaDataSender getSender() {
        return mWriter;
    }
}
