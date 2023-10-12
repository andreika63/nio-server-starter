package com.kay.nioserver;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.SocketAddress;
import java.util.List;
import java.util.UUID;

public interface NIOServer {
    /** New sessions Publisher */
    Flux<TcpSession> getNewSessions();

    /** Closed sessions Publisher */
    Flux<UUID> getClosedSessions();

    /** Active sessions list */
    List<UUID> getConnectionList();

    /** Close session */
    void closeConnection(UUID channelId);

    /** Send data */
    void write(UUID channelId, byte[] byteArray);

    /** Open session */
    Mono<TcpSession> openConnection(SocketAddress socketAddress);
}
