package me.lake.librestreaming.rtmp;

import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.muxer.RESMediaDataMuxer;
import me.lake.librestreaming.muxer.RESMediaDataProcessor;
import me.lake.librestreaming.muxer.RESMediaDataSender;

/**
 * Created by hukanli on 2017_12_29
 * E-Mail: hukanli@hys-inc.cn
 * Copyright: Copyright (c) 2017
 * Title:
 * Description:
 */
public class RESRtmpProcessor implements RESMediaDataProcessor {
    RESRtmpSender mRtmpSender;
    RESFlvDataMuxer mDataMuxer;

    public void prepare(RESCoreParameters coreParameters) {
        mRtmpSender = new RESRtmpSender();
        mRtmpSender.prepare(coreParameters);
        mDataMuxer = new RESFlvDataMuxer(mRtmpSender);
    }
    @Override
    public RESMediaDataMuxer getMuxer() {
        return mDataMuxer;
    }

    @Override
    public RESMediaDataSender getSender() {
        return mRtmpSender;
    }
}
