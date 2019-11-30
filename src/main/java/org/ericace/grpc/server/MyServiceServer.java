package org.ericace.grpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;

import java.io.IOException;

public class MyServiceServer {
    private Server server;

    private static long itemCount = 1;

    private void start() throws IOException {
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new MyServiceServer.MyService())
                .addService(ProtoReflectionService.newInstance())
                .build()
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                MyServiceServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        final MyServiceServer server = new MyServiceServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class MyService extends TheServiceGrpc.TheServiceImplBase {

        @Override
        public void getNumberOfItems(com.google.protobuf.Empty request,
                                     io.grpc.stub.StreamObserver<org.ericace.grpc.server.ItemCount> responseObserver) {
            ItemCount cnt = ItemCount.newBuilder().setItemCount(itemCount).build();
            responseObserver.onNext(cnt);
            responseObserver.onCompleted();
        }

        @Override
        public void setNumberOfItems(org.ericace.grpc.server.ItemCount request,
                                     io.grpc.stub.StreamObserver<org.ericace.grpc.server.ItemCount> responseObserver) {
            itemCount = request.getItemCount();
            ItemCount cnt = ItemCount.newBuilder().setItemCount(itemCount).build();
            responseObserver.onNext(cnt);
            responseObserver.onCompleted();
        }

        @Override
        public void getThreadName(com.google.protobuf.Empty request,
                                  io.grpc.stub.StreamObserver<org.ericace.grpc.server.ThreadName> responseObserver) {
            String threadName = Thread.currentThread().getName();
            ThreadName tn = ThreadName.newBuilder().setThreadName(threadName).build();
            responseObserver.onNext(tn);
            responseObserver.onCompleted();
        }
    }

}
