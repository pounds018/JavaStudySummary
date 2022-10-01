package cn.pounds.netty.codec.demo.fixed;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @Author: pounds
 * @Date: 2022/7/30 16:35
 * @Description:
 */
public class FixedLengthFrameDecoderTest {

    @Test
    public void decode() {
        ByteBuf buffer = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            buffer.writeByte(i);
        }
        ByteBuf duplicate = buffer.duplicate();
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new FixedLengthFrameDecoder(3));
        assertTrue(embeddedChannel.writeInbound(duplicate.retain()));
        assertTrue(embeddedChannel.finish());

        // 读一次3个字节
        ByteBuf read = (ByteBuf) embeddedChannel.readInbound();
        assertEquals(buffer.readSlice(3), read);
        read.release();

        read = (ByteBuf) embeddedChannel.readInbound();
        assertEquals(buffer.readSlice(3), read);
        read.release();

        read = (ByteBuf) embeddedChannel.readInbound();
        assertEquals(buffer.readSlice(3), read);
        read.release();
    }

    @Test
    public void decode2(){
        ByteBuf buffer = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            buffer.writeByte(i);
        }
        ByteBuf duplicate = buffer.duplicate();
        EmbeddedChannel embeddedChannel = new EmbeddedChannel(new FixedLengthFrameDecoder(3));
        assertFalse(embeddedChannel.writeInbound(duplicate.readBytes(2)));
        assertTrue(embeddedChannel.writeInbound(duplicate.readBytes(7)));

        assertTrue(embeddedChannel.finish());

        // 读一次3个字节
        ByteBuf read = (ByteBuf) embeddedChannel.readInbound();
        assertEquals(buffer.readSlice(3), read);
        read.release();

        read = (ByteBuf) embeddedChannel.readInbound();
        assertEquals(buffer.readSlice(3), read);
        read.release();

        read = (ByteBuf) embeddedChannel.readInbound();
        assertEquals(buffer.readSlice(3), read);
        read.release();

        // 没有数据读了
        assertNull(embeddedChannel.readInbound());
        buffer.release();
    }

}