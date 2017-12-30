package me.lake.librestreaming.muxer;

import me.lake.librestreaming.model.RESCoreParameters;

/**
 * Created by hukanli on 2017_12_29
 * E-Mail: hukanli@hys-inc.cn
 * Copyright: Copyright (c) 2017
 * Title:
 * Description:
 */
public interface RESMediaDataProcessor {
    void prepare(RESCoreParameters coreParameters);
    RESMediaDataMuxer getMuxer();
    RESMediaDataSender getSender();
}
