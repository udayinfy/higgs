package io.higgs;

import io.higgs.functional.Function;
import io.higgs.sniffing.ProtocolDetector;
import io.higgs.sniffing.ProtocolSniffer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class HiggsServer<T, OM, IM, SM> extends EventProcessor<T, OM, IM, SM> {

    private int port;
    protected ServerBootstrap bootstrap = new ServerBootstrap();
    public Channel channel;
    //set of protocol sniffers
    protected final Set<ProtocolDetector> detectors =
            Collections.newSetFromMap(new ConcurrentHashMap<ProtocolDetector, Boolean>());
    protected boolean enableProtocolSniffing;
    protected boolean enableGZip;
    protected boolean enableSSL;

    public HiggsServer(int port) {
        this.port = port;
        enableProtocolSniffing = true;
    }

    /**
     * Set the server's port. Only has any effect if the server is not already bound to a port.
     *
     * @param port the port to bind to
     */
    public void setPort(final int port) {
        this.port = port;
    }

    public void bind() {
        bind(new Function() {
            public void apply() {
                //NO-OP
            }
        });
    }

    public void bind(Function function) {
        ChannelInitializer<SocketChannel> initializer =
                //if protocol sniffing is enabled then SSL detection is automatic so a new initializer
                //does not need to add it to the pipeline
                newInitializer(!enableProtocolSniffing && enableSSL, false);
        bootstrap.group(parentGroup(), childGroup())
                .channel(channelClass())
                .localAddress(port)
                .childHandler(initializer);
        channel = bootstrap.bind().channel();
        if (function != null) {
            function.apply();
        }
    }

    //make sure protocol sniffer is added if enabled
    protected void beforeSetupPipeline(ChannelPipeline pipeline) {
        if (enableProtocolSniffing) {
            beforeProtocolSniffer(pipeline);
            pipeline.addLast(new ProtocolSniffer(detectors, this, enableSSL, enableGZip));
        } else {
            if (enableGZip) {
                pipeline.addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
                pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
            }
        }
    }

    public void beforeProtocolSniffer(ChannelPipeline pipeline) {
    }

    public void setEnableGZip(final boolean enableGZip) {
        this.enableGZip = enableGZip;
    }

    public void setEnableSSL(final boolean enableSSL) {
        this.enableSSL = enableSSL;
    }

    /**
     * Enables or disables protocol sniffing
     * If sniffing is enabled, {@link #beforeSetupPipeline(ChannelPipeline)} is not called
     * And GZip + SSL detection is automatically done.
     *
     * @param sniff whether to do protocol sniffing or not
     */
    public void setEnableProtocolSniffing(boolean sniff) {
        enableProtocolSniffing = sniff;
        setEnableGZip(sniff);
        setEnableSSL(sniff);
    }

    /**
     * Adds a protocol detector that is used to modify request pipelines automatically
     * Automatically enables sniffing and disables use of {@link #beforeSetupPipeline(ChannelPipeline)}
     *
     * @param detector The detector to be added
     * @param <T>      any protocol detector
     */
    public <T extends ProtocolDetector> void addProtocolDetector(T detector) {
        if (detector == null) {
            throw new NullPointerException("Null detectors not acceptable");
        }
        setEnableProtocolSniffing(true);
        detectors.add(detector);
    }

    public EventLoopGroup parentGroup() {
        return new NioEventLoopGroup();
    }

    public EventLoopGroup childGroup() {
        return new NioEventLoopGroup();
    }

    public Class<? extends ServerChannel> channelClass() {
        return NioServerSocketChannel.class;
    }
}