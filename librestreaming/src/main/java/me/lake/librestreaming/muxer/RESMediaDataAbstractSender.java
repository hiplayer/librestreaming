package me.lake.librestreaming.muxer;

import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.model.RESCoreParameters;

/**
 * Created by hukanli on 2017_12_30
 * E-Mail: hukanli@hys-inc.cn
 * Copyright: Copyright (c) 2017
 * Title:
 * Description:
 */
public abstract class RESMediaDataAbstractSender implements RESMediaDataSender{
    @Override
    public void prepare(RESCoreParameters coreParameters) {

    }

    @Override
    public void start(String sendAddr) {

    }

    @Override
    public void resume() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public int getTotalSpeed() {
        return 0;
    }

    @Override
    public String getServerIpAddr() {
        return "";
    }

    @Override
    public float getSendFrameRate() {
        return 0;
    }

    @Override
    public float getSendBufferFreePercent() {
        return 100;
    }

    @Override
    public void setConnectionListener(RESConnectionListener connectionListener) {

    }
}
