package app.web.pavelk.cloud1.client.netty;


import app.web.pavelk.cloud1.common.call.ClientMainContentsCallback;
import app.web.pavelk.cloud1.common.handler.ProtoHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.LogManager;


import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;


public class Network {
    private static Network ourInstance = new Network();
    private Channel currentChannel;
    final String folder = "Client/client_storage";
    final String IP_ADDRESS = "localhost";
    final int PORT = 8186;

    public void start(ClientMainContentsCallback ccb, CountDownLatch countDownLatch) {
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap.group(group);
            clientBootstrap.channel(NioSocketChannel.class);
            clientBootstrap.remoteAddress(new InetSocketAddress(IP_ADDRESS, PORT));
            clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    socketChannel.pipeline().addLast(new ProtoHandler(ccb, folder));
                    currentChannel = socketChannel;
                }
            });
            ChannelFuture channelFuture = clientBootstrap.connect().sync();
            countDownLatch.countDown();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            LogManager.getRootLogger().info("Network close");
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        if (ourInstance.currentChannel != null) currentChannel.close();
    }

    public static Network getInstance() {
        return ourInstance;
    }

    public Channel getCurrentChannel() {
        return currentChannel;
    }

}
