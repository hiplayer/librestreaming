package me.lake.librestreaming.muxer;

import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.model.RESCoreParameters;

/**
 * Created by hukanli on 2017_12_29
 * E-Mail: hukanli@hys-inc.cn
 * Copyright: Copyright (c) 2017
 * Title:
 * Description:
 */
public interface RESMediaDataSender {
    void prepare(RESCoreParameters coreParameters);
    void start(String senderAddr);
    void resume();
    void pause();
    void stop();
    void destroy();
    int getTotalSpeed();
    String getServerIpAddr();
    float getSendFrameRate();
    float getSendBufferFreePercent();
    void setConnectionListener(RESConnectionListener connectionListener);
}
