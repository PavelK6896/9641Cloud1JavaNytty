package app.web.pavelk.cloud1.common.handler;

import app.web.pavelk.cloud1.common.call.ClientMainContentsCallback;
import app.web.pavelk.cloud1.common.call.ServerBaseCallBack;
import app.web.pavelk.cloud1.common.send.ProtoFileSender;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ProtoHandler extends ChannelInboundHandlerAdapter {
    public enum State {
        IDLE, NAME_LENGTH, NAME, FILE_LENGTH, FILE, AUTHOR, CONTENTS
    }

    static final Logger rootLogger = LogManager.getRootLogger();
    private ServerBaseCallBack serverBaseCallBack;
    private ClientMainContentsCallback mainContentsCallback;
    private State currentState = State.IDLE;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;
    private byte readed;
    private String folder;
    private String name;
    private int sizeBuf = 7000;
    private int sizeBuf2 = 70;
    private byte[] writeBuffer = new byte[sizeBuf];
    private byte[] writeBuffer2 = new byte[sizeBuf2];
    private int lenName;

    public ProtoHandler(ServerBaseCallBack sbcb, String folder) throws IOException {
        this.serverBaseCallBack = sbcb;
        this.folder = folder;
        this.currentState = State.AUTHOR;
        if (!Files.exists(Paths.get(folder))) {
            Files.createDirectories(Paths.get(folder));
        }
    }

    public ProtoHandler(ClientMainContentsCallback mck, String folder) throws IOException {
        this.mainContentsCallback = mck;
        this.folder = folder;
        if (!Files.exists(Paths.get(folder))) {
                Files.createDirectories(Paths.get(folder));
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = ((ByteBuf) msg);

        while (buf.readableBytes() > 0) {

            if (currentState == State.AUTHOR) {
                readed = buf.readByte();
                if (readed == (byte) 26) {
                    rootLogger.info("STATE:AUTHOR: start connect login and password " + buf.readableBytes());
                    if (buf.readableBytes() >= 8) {
                        byte[] loginB = new byte[buf.readInt()];
                        byte[] passB = new byte[buf.readInt()];
                        if (buf.readableBytes() >= loginB.length + passB.length) {
                            buf.readBytes(loginB);
                            buf.readBytes(passB);
                            if (serverBaseCallBack.autRequestBase(new String(loginB), new String(passB))) {
                                rootLogger.info("STATE:AUTHOR: Clien aut ok");
                                ProtoFileSender.sendCommandByte((byte) 34, ctx.channel());
                                ProtoFileSender.sendContents((byte) 17, Paths.get(folder), ctx.channel());
                                currentState = State.IDLE;
                            } else {
                                rootLogger.info("STATE:AUTHOR: Clien aut not");
                                ProtoFileSender.sendCommandByte((byte) 33, ctx.channel());
                            }
                            break;
                        }
                    }
                }
            }

            if (currentState == State.CONTENTS) {
                    if (buf.readableBytes() >= 4 ) {
                        if (lenName == 0){
                            lenName = buf.readInt();
                        }
                        if(buf.readableBytes() >= lenName + 8 + 4) {
                            byte[] name = new byte[lenName];
                            buf.readBytes(name);
                            long fileLen = buf.readLong(); // 8
                            if (mainContentsCallback != null) {
                                mainContentsCallback.updateContentsCallBackClient(new String(name, StandardCharsets.UTF_8), fileLen);
                            }
                            rootLogger.info("STATE:CONTENTS: Read contents "  + buf.readableBytes());
                            if (buf.readInt() == 0) { // 4
                                currentState = State.IDLE;
                            }
                            lenName = 0;
                        }
                    }
            }

            if (currentState == State.IDLE && buf.readableBytes() > 0) {
                readed = buf.readByte();
                rootLogger.info("STATE:IDLE: Read byte = " + readed);
                if (readed == (byte) 25) {
                    currentState = State.NAME_LENGTH;
                    receivedFileLength = 0L;
                    rootLogger.info("STATE:IDLE: Start file receiving ");

                } else if (readed == (byte) 29) {
                    currentState = State.NAME_LENGTH;
                    receivedFileLength = 0L;
                    rootLogger.info("STATE:IDLE: Server delete file");

                } else if (readed == (byte) 30) {
                    currentState = State.NAME_LENGTH;
                    receivedFileLength = 0L;
                    rootLogger.info("STATE:IDLE: Server send file");


                } else if (readed == (byte) 18) {
                    rootLogger.info("STATE:IDLE: Server send catalog contents");
                    ProtoFileSender.sendContents((byte) 17, Paths.get(folder), ctx.channel());

                } else if (readed == (byte) 17) {
                    rootLogger.info("STATE:IDLE: Client start receiving catalog contents ");
                    currentState = State.CONTENTS;
                    if (mainContentsCallback != null) {
                        mainContentsCallback.clearContentsCallBackClient();
                    }

                } else if (readed == (byte) 33) {
                    rootLogger.info("STATE:IDLE: Client aut not");
                    if (mainContentsCallback != null) {
                        mainContentsCallback.autNotOkClient();
                    }

                } else if (readed == (byte) 34) {
                    rootLogger.info("STATE:IDLE: Client aut ok");
                    if (mainContentsCallback != null) {
                        mainContentsCallback.autOkClient();
                    }

                }  else if (readed == (byte) 35) {
                        rootLogger.info("STATE:IDLE: Client clear ");
                        if (mainContentsCallback != null) {
                            mainContentsCallback.clearContentsCallBackClient();
                        }

                } else {
                    rootLogger.info("ERROR:IDLE: Invalid first byte - " + readed);
                }
            }

            if (currentState == State.NAME_LENGTH) {
                if (buf.readableBytes() >= 4) {
                    nextLength = buf.readInt();
                    currentState = State.NAME;
                    rootLogger.info("STATE:NAME_LENGTH: Get filename length - " + nextLength);
                }
            }

            if (currentState == State.NAME) {
                if (buf.readableBytes() >= nextLength) {
                    byte[] fileName = new byte[nextLength];
                    buf.readBytes(fileName);
                    name = new String(fileName);

                    if (readed == 25) { //rec
                        out = new BufferedOutputStream(new FileOutputStream(folder + "/" + new String(fileName)));
                        currentState = State.FILE_LENGTH;
                        rootLogger.info("STATE:NAME Filename received: - " + folder + "/" + new String(fileName, "UTF-8"));

                    } else if (readed == 29) { // delete

                        String folderNameFile = folder + "/" + new String(fileName);
                        if (Files.exists(Paths.get(folderNameFile))) {
                           if ( Paths.get(folderNameFile).toFile().delete()){
                               rootLogger.info("STATE:NAME: Server delete file - " + folderNameFile);
                           }else{
                               rootLogger.info("ERROR:NAME: Server not delete - " + folderNameFile);
                           }
                        } else {
                            rootLogger.info("ERROR:NAME: Server not file - " + folderNameFile);
                        }
                        ProtoFileSender.sendContents((byte) 17, Paths.get(folder), ctx.channel());
                        currentState = State.IDLE;

                    } else if (readed == 30) { //send

                        String folderNameFile = folder + "/" + new String(fileName);
                        if (Files.exists(Paths.get(folderNameFile))) {

                            ProtoFileSender.sendFile((byte) 25, Paths.get(folderNameFile), ctx.channel(), future -> {
                                if (!future.isSuccess()) {
                                    future.cause().printStackTrace();
                                }
                                if (future.isSuccess()) {
                                    rootLogger.info("STATE:NAME: Server successfully transferred file - " + folderNameFile);
                                }
                            });
                            rootLogger.info("STATE:NAME: Server Send file - " + folderNameFile);
                        }
                        rootLogger.info("STATE:NAME: Server not file - " + folderNameFile);
                        currentState = State.IDLE;
                    }
                }
            }

            if (currentState == State.FILE_LENGTH) {
                if (buf.readableBytes() >= 8) {
                    fileLength = buf.readLong();
                    rootLogger.info("STATE:FILE_LENGTH: File received length - " + fileLength);
                    currentState = State.FILE;
                }
            }

            if (currentState == State.FILE) { //rec
                if (fileLength == 0) {
                    if (mainContentsCallback != null) {
                        mainContentsCallback.clientUpdateContents();
                        rootLogger.info("STATE:FILE: Client call update contents CallBack");
                    }
                    currentState = State.IDLE;
                }

                while (buf.readableBytes() > 0) {
                    if (fileLength - receivedFileLength > sizeBuf && buf.readableBytes() > sizeBuf) {
                        buf.readBytes(writeBuffer);
                        out.write(writeBuffer);
                        receivedFileLength += sizeBuf;
                    }else if (fileLength - receivedFileLength > sizeBuf2  && buf.readableBytes() > sizeBuf2  ) {
                            buf.readBytes(writeBuffer2);
                            out.write(writeBuffer2);
                            receivedFileLength += sizeBuf2;
                    } else {
                        while (buf.readableBytes() > 0) {
                            out.write(buf.readByte());
                            receivedFileLength++;
                            if (fileLength == receivedFileLength) {
                                break;
                            }
                        }
                    }

                    if (fileLength == receivedFileLength) {
                        currentState = State.IDLE;
                        rootLogger.info("STATE:FILE: File received - " + out.getClass().getTypeName());
                        out.close();

                        if (mainContentsCallback != null) {
                            mainContentsCallback.clientUpdateContents();
                            rootLogger.info("STATE:FILE: Client call update contents");

                        } else {
                            ProtoFileSender.sendContents((byte) 17, Paths.get(folder), ctx.channel());
                            rootLogger.info("STATE:FILE: Server send update contents");
                        }
                        break;
                    }
                }
            }
        }
        if (buf.readableBytes() == 0) {
            buf.release();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {  // подключился
        super.channelActive(ctx);
        rootLogger.info("Client is connected " + ctx.name());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception { // отключился
        super.channelInactive(ctx);
        rootLogger.info("Client disconnected " + ctx.name());
    }
}
