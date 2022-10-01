package cn.pounds.netty.codec.demo.fixed;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @Author: pounds
 * @Date: 2022/7/30 16:30
 * @Description:
 * 固定长度解码器, 将byte流按照固定长度生成数据
 */
public class FixedLengthFrameDecoder extends ByteToMessageDecoder {
    private final int frameLength;

    public FixedLengthFrameDecoder(int frameLength) {
        if (frameLength <= 0) {
            System.out.println("frame length must be a positive integer : " + frameLength);
        }
        this.frameLength = frameLength;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (in.readableBytes() >= frameLength) {
            ByteBuf bytes = in.readBytes(frameLength);
            out.add(bytes);
        }
    }
}
