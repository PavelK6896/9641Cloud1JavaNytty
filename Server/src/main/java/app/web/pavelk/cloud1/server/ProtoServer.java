package app.web.pavelk.cloud1.server;


import app.web.pavelk.cloud1.common.call.ServerBaseCallBack;
import app.web.pavelk.cloud1.common.handler.ProtoHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;


public class ProtoServer {
    private void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        final String folder = "Server/server_storage";
        final String IP_ADDRESS = "localhost";
        final int PORT = 8186;

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup);
            b.channel(NioServerSocketChannel.class);
            b.childHandler(new ChannelInitializer<SocketChannel>() { // (4)
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(new ProtoHandler(new ServerBaseCallBack() {
                        public boolean autRequestBase(String login, String pass) {
                           return BaseService.autRequestBase(login,pass);
                        }
                    }, folder));
                }
            });
            b.childOption(ChannelOption.SO_KEEPALIVE, true);
            ChannelFuture f = b.bind(PORT).sync();
            BaseService.connect();
            LogManager.getRootLogger().info("Server start: " + IP_ADDRESS + PORT);
            f.channel().closeFuture().sync();
        } finally {
            LogManager.getRootLogger().info("Server close");
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            BaseService.disconnect();
        }
    }

    public static void main(String[] args) throws Exception {
        new ProtoServer().run();
    }

}
