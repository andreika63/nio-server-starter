package com.kay.nioserver;

import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.function.Consumer;

public class TcpSession {

    private final UUID uuid;
    private final SocketChannel socketChannel;
    private final Flux<byte[]> flux;
    private final Consumer<SocketChannel> close;

    private final SocketAddress remoteAddress;

    public TcpSession(UUID uuid, SocketChannel socketChannel, Flux<byte[]> flux, Consumer<SocketChannel> close) {
        this.uuid = uuid;
        this.socketChannel = socketChannel;
        this.flux = flux;
        this.close = close;

        try {
            remoteAddress = socketChannel.getRemoteAddress();
        } catch (IOException e) {
            throw new NioServerException(e.getMessage(), e.getCause());
        }
    }

    public Flux<byte[]> getData() {
        return flux.doOnCancel(this::closeSession);
    }

    public void write(byte[] byteArray) {
        try {
            socketChannel.write(ByteBuffer.wrap(byteArray));
        } catch (IOException e) {
            closeSession();
            throw new NioServerException(e.getMessage(), e.getCause());
        }

    }

    public void closeSession() {
        close.accept(socketChannel);
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public String toString() {
        return uuid + " [" + remoteAddress + "]";
    }
}
