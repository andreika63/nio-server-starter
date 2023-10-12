package com.kay.nioserver;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@ConfigurationProperties("nio-server")
public class NIOServerProperties {
    private String bindAddress;
    private Integer tcpPort;
    private DataSize bufferSize;
    private Duration tcpKeepidle;
    private Integer tcpKeepcount;
    private Duration tcpKeepinterval;
    private Boolean tcpNodelay;

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    public Integer getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(Integer tcpPort) {
        this.tcpPort = tcpPort;
    }

    public DataSize getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(DataSize bufferSize) {
        this.bufferSize = bufferSize;
    }

    public Duration getTcpKeepidle() {
        return tcpKeepidle;
    }

    public void setTcpKeepidle(Duration tcpKeepidle) {
        this.tcpKeepidle = tcpKeepidle;
    }

    public Integer getTcpKeepcount() {
        return tcpKeepcount;
    }

    public void setTcpKeepcount(Integer tcpKeepcount) {
        this.tcpKeepcount = tcpKeepcount;
    }

    public Duration getTcpKeepinterval() {
        return tcpKeepinterval;
    }

    public void setTcpKeepinterval(Duration tcpKeepinterval) {
        this.tcpKeepinterval = tcpKeepinterval;
    }

    public Boolean getTcpNodelay() {
        return tcpNodelay;
    }

    public void setTcpNodelay(Boolean tcpNodelay) {
        this.tcpNodelay = tcpNodelay;
    }

    @Override
    public String toString() {
        return "NIOServerProperties{" +
                "bindAddress='" + bindAddress + '\'' +
                ", tcpPort=" + tcpPort +
                ", bufferSize=" + bufferSize +
                ", tcpKeepidle=" + tcpKeepidle +
                ", tcpKeepcount=" + tcpKeepcount +
                ", tcpKeepinterval=" + tcpKeepinterval +
                ", tcpNodelay=" + tcpNodelay +
                '}';
    }
}
