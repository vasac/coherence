/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package helidon;

import com.google.protobuf.Int32Value;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.coherence.grpc.NamedCacheServiceGrpc;
import com.oracle.coherence.grpc.Requests;
import com.oracle.coherence.grpc.proxy.GrpcServerController;
import com.tangosol.net.SessionProvider;
import com.tangosol.net.grpc.GrpcDependencies;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.helidon.microprofile.server.Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test that the gRPC proxy starts when using Helidon MP gRPC server.
 *
 * @author Jonathan Knight  2020.10.08
 */
public class HelidonIT
    {
    @BeforeAll
    static void setupBaseTest() throws Exception
        {
        Integer nPort = LocalPlatform.get().getAvailablePorts().next();

        System.setProperty("coherence.ttl",          "0");
        System.setProperty("coherence.wka",          "127.0.0.1");
        System.setProperty("coherence.localhost",    "127.0.0.1");
        System.setProperty("coherence.clustername",  "HelidonIT");
        System.setProperty("coherence.cacheconfig",  "coherence-config.xml");
        System.setProperty("coherence.pof.enabled",  "true");
        System.setProperty("coherence.log.level",    "9");
        System.setProperty("coherence.grpc.enabled", "false");
        System.setProperty("grpc.port", String.valueOf(nPort));

        s_server  = Server.create().start();

        s_channel = ManagedChannelBuilder
                .forAddress("127.0.0.1", nPort)
                .usePlaintext()
                .build();

        // wait at most 1 minute for the gRPC Server to start
        GrpcServerController.INSTANCE.whenStarted()
                .toCompletableFuture()
                .get(1, TimeUnit.MINUTES);
        }


    @AfterAll
    static void cleanupBaseTest()
        {
        SessionProvider.get().close();

        if (s_channel != null)
            {
            s_channel.shutdownNow();
            }

        if (s_server != null)
            {
            s_server.stop();
            }
        }

    @Test
    public void should() throws Exception
        {
        NamedCacheServiceGrpc.NamedCacheServiceStub client   = NamedCacheServiceGrpc.newStub(s_channel);
        TestStreamObserver<Int32Value>              observer = new TestStreamObserver<>();

        client.size(Requests.size(GrpcDependencies.DEFAULT_SCOPE, "foo"), observer);

        assertThat(observer.await(1, TimeUnit.MINUTES), is(true));
        observer.assertComplete()
                .assertNoErrors()
                .assertValueCount(1);

        Int32Value int32Value = observer.valueAt(0);
        assertThat(int32Value, is(notNullValue()));
        assertThat(int32Value.getValue(), is(0));
        }

    // ----- data members ---------------------------------------------------

    private static Server s_server;

    private static ManagedChannel s_channel;
    }
