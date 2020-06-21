package app.web.pavelk.cloud1.common.send;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import org.apache.logging.log4j.LogManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class ProtoFileSender {

    public static void sendFile(byte option, Path path, Channel channel, ChannelFutureListener finishListener) throws IOException {
        FileRegion region = new DefaultFileRegion(new FileInputStream(path.toFile()).getChannel(), 0, Files.size(path));
        byte[] filenameBytes = path.getFileName().toString().getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length + 13);
        buf.writeByte(option);
        buf.writeInt(filenameBytes.length);
        buf.writeBytes(filenameBytes);
        buf.writeLong(Files.size(path));
        channel.writeAndFlush(buf);
        ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
        LogManager.getRootLogger().info("Send file");
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

    public static void sendContents(byte option, Path path, Channel channel) throws IOException {
        AtomicInteger numberFile = new AtomicInteger((int) Files.list(path).count());
        if (numberFile.get() != 0) {
            ProtoFileSender.sendCommandByte(option, channel);
            LogManager.getRootLogger().info("Send contents");
        } else {
            LogManager.getRootLogger().info("Contents file no");
            ProtoFileSender.sendCommandByte((byte) 35, channel);
        }
        Files.list(path).forEach(r -> {
            byte[] filenameBytes = r.getFileName().toString().getBytes(StandardCharsets.UTF_8);
            ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length + 12);
            buf.writeInt(filenameBytes.length);
            buf.writeBytes(filenameBytes);
            buf.writeLong(r.toFile().length());
            buf.writeInt(numberFile.decrementAndGet());
            channel.writeAndFlush(buf);
        });
    }

    public static void sendCommandByte(byte option, Channel channel) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(1);
        buf.writeByte(option);
        channel.writeAndFlush(buf);
    }

    public static void sendCommandByteAndOneString(byte option, String name, Channel channel) {
        byte[] nameB = name.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(5 + nameB.length);
        buf.writeByte(option);
        buf.writeInt(nameB.length);
        buf.writeBytes(nameB);
        channel.writeAndFlush(buf);
    }

    public static void sendCommandByteAndTwoString(byte option, String login, String pass, Channel channel) {
        byte[] loginBytes = login.getBytes(StandardCharsets.UTF_8);
        byte[] passBytes = pass.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = ByteBufAllocator.DEFAULT.directBuffer(loginBytes.length + passBytes.length + 9);
        buf.writeByte(option);
        buf.writeInt(loginBytes.length);
        buf.writeInt(passBytes.length);
        buf.writeBytes(loginBytes);
        buf.writeBytes(passBytes);
        channel.writeAndFlush(buf);
    }
}

