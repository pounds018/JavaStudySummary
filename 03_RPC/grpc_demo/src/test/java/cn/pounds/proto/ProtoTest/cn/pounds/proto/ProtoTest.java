package cn.pounds.proto.ProtoTest.cn.pounds.proto;

import cn.pounds.grpc.examples.helloword.HelloWorldProto;
import org.junit.Test;

/**
 * @Author: pounds
 * @Date: 2022/9/9 17:22
 * @Description: protoBuffer的相关测试
 */
public class ProtoTest {
    @Test
    public void test(){
        System.out.println(HelloWorldProto.RequestType.HEART_REQ.getNumber());
        System.out.println(HelloWorldProto.RequestType.MESSAGE_REQ.getNumber());
    }
}
