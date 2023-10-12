package com.kay.nioserver;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import jdk.net.ExtendedSocketOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Component
public class NIOServerImpl implements NIOServer, CommandLineRunner {

    private final NIOServerProperties nioServerProperties;
    private final Selector selector;
    private final Logger log = LoggerFactory.getLogger(NIOServerImpl.class);
    private final BiMap<UUID, SocketChannel> channels = HashBiMap.create();
    private final Map<UUID, Sinks.Many<byte[]>> sinkMap = new HashMap<>();
    private final Executor executor = Executors.newCachedThreadPool();
    private final Sinks.Many<TcpSession> newSessionSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<UUID> closedSessionSink = Sinks.many().multicast().onBackpressureBuffer();

    public NIOServerImpl(NIOServerProperties nioServerProperties) {
        this.nioServerProperties = nioServerProperties;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new NioServerException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public Flux<TcpSession> getNewSessions() {
        return newSessionSink.asFlux();
    }

    @Override
    public Flux<UUID> getClosedSessions() {
        return closedSessionSink.asFlux().publishOn(Schedulers.fromExecutor(executor));
    }

    @Override
    public List<UUID> getConnectionList() {
        return channels.keySet()
                .stream()
                .toList();
    }

    @Override
    public void closeConnection(UUID channelId) {
        Optional.ofNullable(channels.get(channelId))
                .ifPresent(this::closeConnection);
    }

    @Override
    public void write(UUID channelId, byte[] byteArray) {
        CompletableFuture.runAsync(() ->
                        Optional.ofNullable(channels.get(channelId))
                                .ifPresent(ch -> {
                                    try {
                                        ch.write(ByteBuffer.wrap(byteArray));
                                    } catch (IOException e) {
                                        throw new NioServerException(e.getMessage(), e.getCause());
                                    }
                                })
                , executor);
    }

    @Override
    public Mono<TcpSession> openConnection(SocketAddress socketAddress) {
        return Mono.fromCallable(() -> {
                            SocketChannel socketChannel = SocketChannel.open();
                            if (tryConnect(socketChannel, socketAddress)) {
                                socketChannel.configureBlocking(false);
                                socketChannel.register(selector, SelectionKey.OP_READ);
                                return registerConnection(socketChannel);
                            } else {
                                throw new NioServerException("Failed to connect to Server at: " + socketAddress);
                            }
                        }
                ).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public void run(String... args) throws Exception {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(nioServerProperties.getBindAddress(), nioServerProperties.getTcpPort()));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, serverSocketChannel.validOps(), null);
        ByteBuffer buffer = ByteBuffer.allocate(Long.valueOf(nioServerProperties.getBufferSize().toBytes()).intValue());
        log.info(nioServerProperties.toString());
        while (true) {
            try {
                selector.select(1000);
                Iterator<SelectionKey> selectionKeyIterator = selector.selectedKeys().iterator();
                while (selectionKeyIterator.hasNext()) {
                    SelectionKey key = selectionKeyIterator.next();
                    selectionKeyIterator.remove();
                    if (!key.isValid()) {
                    } else if (key.isAcceptable()) {
                        SocketChannel socketChannel = serverSocketChannel.accept();
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        registerConnectionAsync(socketChannel);
                    } else if (key.isReadable()) {
                        SocketChannel socketChannel = (SocketChannel) key.channel();
                        var read = -1;
                        try {
                            read = socketChannel.read(buffer);
                        } catch (IOException ignore) {
                        }
                        if (read == -1) {
                            closeConnection(socketChannel);
                            continue;
                        }
                        byte[] bytes = new byte[read];
                        buffer.position(0)
                                .get(bytes);
                        consumeBytes(socketChannel, bytes);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                buffer.position(0);
            }
        }
    }

    private boolean tryConnect(SocketChannel socketChannel, SocketAddress socketAddress) {
        try {
            return socketChannel.connect(socketAddress);
        } catch (Exception e) {
            return false;
        }
    }

    private void registerConnectionAsync(SocketChannel socketChannel) {
        CompletableFuture.runAsync(() ->
                        registerConnection(socketChannel),
                executor);
    }

    private TcpSession registerConnection(SocketChannel socketChannel) {
        try {
            socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
            socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, nioServerProperties.getTcpNodelay());
            socketChannel.setOption(ExtendedSocketOptions.TCP_KEEPIDLE, Long.valueOf(nioServerProperties.getTcpKeepidle().toSeconds()).intValue());
            socketChannel.setOption(ExtendedSocketOptions.TCP_KEEPCOUNT, nioServerProperties.getTcpKeepcount());
            socketChannel.setOption(ExtendedSocketOptions.TCP_KEEPINTERVAL, Long.valueOf(nioServerProperties.getTcpKeepinterval().toSeconds()).intValue());
        } catch (IOException e) {
            throw new NioServerException(e.getMessage(), e.getCause());
        }
        UUID key = UUID.randomUUID();
        channels.put(key, socketChannel);
        Sinks.Many<byte[]> sink = Sinks.many().unicast().onBackpressureBuffer();
        sinkMap.put(key, sink);
        TcpSession tcpSession = new TcpSession(key, socketChannel, sink.asFlux().publishOn(Schedulers.fromExecutor(executor)), this::closeConnection);
        newSessionSink.tryEmitNext(tcpSession);
        return tcpSession;
    }

    private void closeConnection(SocketChannel socketChannel) {
        try {
            socketChannel.close();
        } catch (IOException ignore) {
        } finally {
            BiMap<SocketChannel, UUID> inverse = channels.inverse();
            UUID key = inverse.get(socketChannel);
            inverse.remove(socketChannel);
            sinkMap.remove(key).tryEmitComplete();
            closedSessionSink.tryEmitNext(key);
        }
    }

    private void consumeBytes(SocketChannel socketChannel, byte[] byteArray) {
        UUID key = channels.inverse().get(socketChannel);
        Optional.ofNullable(sinkMap.get(key)).ifPresent(sink -> sink.tryEmitNext(byteArray));
    }
}
