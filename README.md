# gRPC Hello World

I wanted to incorporate gRPC into another application I was working on so I needed to build a "Hello World" project to get familiar with it. There are many tutorials and examples available that provide guidance on implementing gRPC in Java. Rather than git clone a full existing example, I elected to build up my project a little more manually. My environment is IntelliJ Ultimate 2019.2 on Ubuntu 18.04.3 LTS. Here are the steps I followed:

1. Read the available gRPC quick starts, guides, etc.
2. Create an empty project with the Maven 'quickstart' archetype
3. Create java sources directory `org/ericace/grpc/server`
4. Copy `HelloWorldServer.java` from https://github.com/grpc/grpc-java/blob/master/examples/src/main/java/io/grpc/examples/helloworld into that directory
5. Create directory `resources/proto`
6. Copy `helloworld.proto` from https://github.com/grpc/grpc-java/blob/81da3eb95be37fa0647ce8da2e19de96ab84c600/examples/src/main/proto into that directory

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
7. Install the protobuf compiler
    ```shell script
    $ sudo apt install protobuf-compiler
    $ which protoc
    /usr/bin/protoc
    $ protoc --version
    libprotoc 3.0.0
    ```
8. Incorporate POM elements from https://github.com/grpc/grpc-java/blob/master/examples/pom.xml
9. Configure `protobuf-maven-plugin` to  use the installed `protoc`
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
11. Run the app in IntelliJ
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
13. Download and build the `grpc-cli` command-line utility which provides the ability to exercise a gRPC server API from the command line. Instructions to get and build are here: https://github.com/grpc/grpc/blob/master/BUILDING.md

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
15. Call the server. Pass the proto file to the CLI because reflection has not yet been enabled in the Java gRPC server (it's not in the Hello World Java code.)
    ```shell script
     grpc_cli call localhost:50051 SayHello "name: 'gRPC CLI'"\
      --proto_path=/home/eace/IdeaProjects/grpc-server-learn/src/main/resources/proto \
      --protofiles=helloworld.proto
     connecting to localhost:50051
     message: "Hello gRPC CLI"

     Rpc succeeded with OK status
     Reflection request not implemented; is the ServerReflection service enabled?
    ```
16. Add reflection. It requires two changes. First, an additional Maven dependency is required:
    ```xml
    ...
    <dependency>
      <groupId>io.grpc</groupId>
      <artifactId>grpc-services</artifactId>
      <version>${grpc.version}</version>
    </dependency>
    ...
    ```
    Second, one additional line of Java code is required in the HelloWordServer.java. (IntelliJ's intellisense will prompt for the correct import.) The required new line of code is bolded:
    <pre>
    public class HelloWorldServer {
        ...
        private void start() throws IOException {
        ...
            server = ServerBuilder.forPort(port)
                .addService(new GreeterImpl())
                <b>.addService(ProtoReflectionService.newInstance())</b>
    </pre>
17. Now, test reflection by excluding the proto file from the `grpc_cli` commandline options:
    ```shell script
    $ grpc_cli ls localhost:50051 helloworld.Greeter -l
    filename: helloworld.proto
    package: helloworld;
    service Greeter {
      rpc SayHello(helloworld.HelloRequest) returns (helloworld.HelloReply) {}
    }
    ```
