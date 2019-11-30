package org.ericace.grpc.server;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

public class MyServiceClient {

    private final ManagedChannel channel;
    private final TheServiceGrpc.TheServiceBlockingStub blockingStub;

    public MyServiceClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
    }

    MyServiceClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = TheServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        System.out.println("Begin Channel Shutdown");
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("Complete Channel Shutdown");
    }

    long setItemCount(long itemCount) {
        ItemCount cnt = ItemCount.newBuilder().setItemCount(itemCount).build();
        return blockingStub.setNumberOfItems(cnt).getItemCount();
    }

    // arg[0] - count value to set
    // arg[1] - number of iterations
    public static void main(String[] args) throws Exception {
        int exitCode = 0;
        MyServiceClient client = new MyServiceClient("localhost", 50051);
        System.out.println("Begin Java Client");
        try {
            long itemCount = Long.parseLong(args[0]);
            int iterations = Integer.parseInt(args[1]);
            for (int i = 0; i < iterations; ++i) {
                System.out.println("item_count: " + client.setItemCount(itemCount));
            }
        } catch (Exception e) {
            System.out.println(e);
            exitCode = 1;
        } finally {
            client.shutdown();
        }
        System.exit(exitCode);
    }
}
