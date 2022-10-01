package cn.pounds.grpc.examples.helloword;


import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

/**
 * @author: pounds
 * @date: 2022/2/19 0:07
 * @desc:
 */
public class HelloWorldClient {
	// 服务端的代理对象
	private final GreeterGrpc.GreeterBlockingStub blockingStub;

	public HelloWorldClient(Channel channel){
		// 这里的channel不是得ManageChannel, 所以无需在这里关闭, 用channel传递是为了方便
		blockingStub = GreeterGrpc.newBlockingStub(channel);
	}

	public void greet(String name){
		HelloWorldProto.HelloRequest helloRequest = HelloWorldProto.HelloRequest.newBuilder().setName(name).build();
		HelloWorldProto.HelloResponse response = HelloWorldProto.HelloResponse.newBuilder().build();
		try {
			response = blockingStub.sayHello(helloRequest);
		}catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("服务端响应数据为: " + response.getMessage());
	}

	public static void main(String[] args) throws InterruptedException {
		String user = "world";
		String target = "localhost:50051";

		// 允许user 和 target 通过 命令行传入
		if (args.length > 0) {
			if ("--help".equals(args[0])) {
				System.err.println("Usage: [name [target]]");
				System.err.println("");
				System.err.println("  name    The name you wish to be greeted by. Defaults to " + user);
				System.err.println("  target  The server to connect to. Defaults to " + target);
				System.exit(1);
			}
			user = args[0];
		}

		if (args.length > 1){
			target = args[1];
		}

		// 创建一个channel管理对象, 可以复用channel, 等于一个channel线程池
		ManagedChannel managedChannel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
		try {
			HelloWorldClient helloWorldClient = new HelloWorldClient(managedChannel);
			helloWorldClient.greet(user);
			System.out.print("1");
		} finally {
			managedChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
		}
	}
}
