package gash.router.server;

import java.nio.ByteOrder;
import java.util.List;

import gash.router.container.RoutingConf;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import routing.Pipe.Route;

public class ServerInit extends ChannelInitializer<SocketChannel> {
	boolean compress = false;
	RoutingConf conf;

	public ServerInit(RoutingConf conf, boolean enableCompression) {
		super();
		compress = enableCompression;
		this.conf = conf;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		System.out.println("starting incoming connection");
		
		ChannelPipeline pipeline = ch.pipeline();

		// Enable stream compression (you can remove these two if unnecessary)
		if (compress) {
			pipeline.addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
			pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
		}

		/**
		 * length (4 bytes).
		 * 
		 * Note: max message size is 64 Mb = 67108864 bytes this defines a
		 * framer with a max of 64 Mb message, 4 bytes are the length, and strip
		 * 4 bytes
		 */
		pipeline.addLast("frameDecoder", new MyDecoder(67108864, 0, 4, 0, 4));

		// decoder must be first
		pipeline.addLast("protobufDecoder", new ProtobufDecoder(Route.getDefaultInstance()));
		pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
		pipeline.addLast("protobufEncoder", new ProtobufEncoder());


		// our server processor (new instance for each connection)
		pipeline.addLast("handler", new ServerHandler(conf));
	}
	
	private class MyDecoder extends LengthFieldBasedFrameDecoder {
		
		public MyDecoder(
	            int maxFrameLength,
	            int lengthFieldOffset, int lengthFieldLength,
	            int lengthAdjustment, int initialBytesToStrip) {
	        
			super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
	    }
		
		@Override
		public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception{
			for (int i = 0; i < in.readableBytes(); i ++) {
			     byte b = in.getByte(i);
//			     System.out.println("byte char: " + (char) b + ", byte: " + b);
			 }
			
			System.out.println("time: " + System.nanoTime() + ", byte count: " + in.readableBytes());
			return super.decode(ctx, in);
		}
		
		@Override
		protected long getUnadjustedFrameLength(ByteBuf buf, int offset, int length, ByteOrder order) {
			long framelength = super.getUnadjustedFrameLength(buf, offset, length, order);
			
			long myFrameLength = 0;
			for (int i = offset; i < length; i++) {
				int byteInt = (int)(buf.getByte(i));
//				System.out.println("byte int: " + byteInt);
				myFrameLength = myFrameLength * 256 + byteInt;
			}
			
			System.out.println("My Decoded frame length: " + myFrameLength);
			System.out.println("Decoded frame length: " + framelength);
			
			return framelength;
		}
	}
}
