# gRPC Hello World

I wanted to incorporate gRPC into another application I was working on so I needed to build a "Hello World" project to get familiar with it. There are many tutorials and examples available that provide guidance on implementing gRPC in Java. Rather than git clone a full existing example, I elected to build up my project a little more manually. My environment is IntelliJ Ultimate 2019.3 on Ubuntu 18.04.3 LTS. Here are the steps I followed:

1. Read the available gRPC quick starts, guides, etc.
2. Create an empty project with the Maven 'quickstart' archetype
3. Create java sources directory `org/ericace/grpc/server`
4. Copy `HelloWorldServer.java` from [here](https://github.com/grpc/grpc-java/blob/master/examples/src/main/java/io/grpc/examples/helloworld) into that directory
5. Create directory `resources/proto`
6. Copy `helloworld.proto` from [here](https://github.com/grpc/grpc-java/blob/81da3eb95be37fa0647ce8da2e19de96ab84c600/examples/src/main/proto) into that directory

    So far, the relevant directory structure looks like this:
    ```
     ── src
         └── main
             ├── java
             │   └── org
             │       └── ericace
             │           └── grpc
             │               └── server
             │                   └── HelloWorldServer.java
             └── resources
                 └── proto
                     └── helloworld.proto
    ```
7. Install the protobuf compiler, to convert the `.proto` file to Java code
    ```shell script
    $ sudo apt install protobuf-compiler
    $ which protoc
    /usr/bin/protoc
    $ protoc --version
    libprotoc 3.0.0
    ```
8. Incorporate POM elements from [here](https://github.com/grpc/grpc-java/blob/master/examples/pom.xml) into the POM generated by the quickstart Maven archetype
9. Configure `protobuf-maven-plugin` to  use the installed `protoc`. This integrates generation of Java sources from the `.proto` file into the Maven build
    ```xml
    ...
      <plugin>
        <groupId>org.xolstice.maven.plugins</groupId>
        <artifactId>protobuf-maven-plugin</artifactId>
        <version>0.6.1</version>
        <configuration>
          <protocExecutable>/usr/bin/protoc</protocExecutable>
    ...
    ```
10. Build the project in IntelliJ, and also from the command line
    ```shell script
    $ mvn clean compile
    ...
    [INFO] ------------------------------------------------------------------------
    [INFO] BUILD SUCCESS
    [INFO] ------------------------------------------------------------------------
    [INFO] Total time:  1.336 s
    [INFO] Finished at: 2019-11-27T11:44:48-05:00
    [INFO] ------------------------------------------------------------------------
    ```
11. Run the app in IntelliJ. Observe the output in the IntelliJ _Run_ window
    ```
    Nov 27, 2019 11:52:19 AM org.ericace.grpc.server.HelloWorldServer start
    INFO: Server started, listening on 50051
    ```
12. Confirm something is listening on the port
    ```
    $ sudo netstat -tlpn | grep 'PID\|:50051'
    Proto Recv-Q Send-Q Local Address           Foreign Address         State       PID/Program name    
    tcp6       0      0 :::50051                :::*                    LISTEN      22423/java 
    ```
13. Download and build the `grpc-cli` command-line utility which provides the ability to exercise a gRPC server API from the command line. Instructions to get and build are [here](https://github.com/grpc/grpc/blob/master/BUILDING.md)

14. After the build completes, verify the binary was built, and copy to a directory in the $PATH:
    ```shell script
    $ pwd
    /home/eace/grpc
    $ find . -name grpc_cli
    ./bins/opt/grpc_cli
    $ cp $(find . -name grpc_cli) ~/.local/bin
    $ which grpc_cli
    /home/eace/.local/bin/grpc_cli
    ```
15. Use the CLI tool to call the server. Pass the proto file to the CLI because reflection has not yet been enabled in the Java gRPC server (it's not in the Hello World Java code.)
    ```shell script
     grpc_cli call localhost:50051 SayHello "name: 'gRPC CLI'"\
      --proto_path=/home/eace/IdeaProjects/grpc-server-learn/src/main/resources/proto \
      --protofiles=helloworld.proto
     connecting to localhost:50051
     message: "Hello gRPC CLI"

     Rpc succeeded with OK status
     Reflection request not implemented; is the ServerReflection service enabled?
    ```
16. Add reflection to the server. It requires two changes. First, an additional Maven dependency is required:
    ```xml
    ...
    <dependency>
      <groupId>io.grpc</groupId>
      <artifactId>grpc-services</artifactId>
      <version>${grpc.version}</version>
    </dependency>
    ...
    ```
    Second, one additional line of Java code is required in the HelloWordServer.java. (IntelliJ's intellisense will prompt for the required import.) The required new line of code is bolded:
    <pre>
    public class HelloWorldServer {
        ...
        private void start() throws IOException {
            ...
            server = ServerBuilder.forPort(port)
                .addService(new GreeterImpl())
                <b>.addService(ProtoReflectionService.newInstance())</b>
            ...
    </pre>
17. Now, verify reflection by excluding the proto file from the `grpc_cli` command line options:
    ```shell script
    $ grpc_cli ls localhost:50051 helloworld.Greeter -l
    filename: helloworld.proto
    package: helloworld;
    service Greeter {
      rpc SayHello(helloworld.HelloRequest) returns (helloworld.HelloReply) {}
    }
    ```
18. Run a simple script to get a sense of the round-trip time to make a gRPC call using the grpc_cli CLI 
    ```shell script
    $ /home/eace/IdeaProjects/grpc-server-learn/scripts/call-hello
    Begin 10000 gRPC calls.......................
    Done
    Elapsed time (H:MM:SS:NNN) 0:02:08:289
    ```
    So, it looks like approximately 12.8 milliseconds per call when using the `grpc_cli` tool running on a Ubuntu system with 12 Intel i7-8700 cores running at 3.20GHz. (Though - the number of cores probably doesn't make much of a difference.)
19. Add `myservice.proto` and associated server and client classes to learn a little more about implementing gRPC functionality. Add a shell script to compare the performance of calling a gRPC method from within a Java client vs. using the grpc_cli CLI:
   ```shell script
   $ /home/eace/IdeaProjects/grpc-server-learn/scripts/compare-call-set-cnt
   Begin 10000 gRPC calls using the Java client
   Done
   Elapsed time (H:MM:SS:NNN) 0:00:02:409
   Begin 10000 gRPC calls using the grpc_cli client......
   Done
   Elapsed time (H:MM:SS:NNN) 0:02:06:643
   ```
   So this test shows 0.2409 milliseconds per call when making the gRPC calls within a running Java client, vs. 12.6643 milliseconds per call using the CLI. The difference is the overhead of repeatedly running the CLI for each method invocation as opposed to starting the Java client once, and then calling the server within the running client.
