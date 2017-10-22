package gash.router.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

import gash.router.container.RoutingConf;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;

public class SocketAddressHttpServer {
	
	private ChannelFuture channel;
	private final EventLoopGroup masterGroup;
	private final EventLoopGroup slaveGroup;
	private RoutingConf conf;  
	  
	    
	  public SocketAddressHttpServer(RoutingConf conf) {
		  this.conf = conf;
	  }
	  
	  {
	    masterGroup = new NioEventLoopGroup();
	    slaveGroup = new NioEventLoopGroup();        
	  }

	  public void start() {
	    
		  Runtime.getRuntime().addShutdownHook(new Thread(){
			  @Override
			  public void run() { shutdown(); }
			  }
		  );
	        
		  try {
			  
			  final ServerBootstrap bootstrap = new ServerBootstrap()
			  .group(masterGroup, slaveGroup)
			  .channel(NioServerSocketChannel.class)
			  .childHandler(new ChannelInitializer<SocketChannel>() {
				  @Override
				  public void initChannel(final SocketChannel ch) throws Exception {
	              ch.pipeline().addLast("codec", new HttpServerCodec());
	              ch.pipeline().addLast("aggregator", new HttpObjectAggregator(512*1024));
	              ch.pipeline().addLast("request", new ChannelInboundHandlerAdapter() {
	                @Override
	                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
	                  if (msg instanceof FullHttpRequest) {
	                	  String socketServerAddress = getSocketServerAddress();
	                	  ByteBuf responseBuff = Unpooled.wrappedBuffer(socketServerAddress.getBytes());
	                                            
	                	  FullHttpResponse response = new DefaultFullHttpResponse(
	                			  HttpVersion.HTTP_1_1,
	                			  HttpResponseStatus.OK,
	                			  responseBuff
	                	  );
	    
	                    response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
	                    response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, socketServerAddress.length());                      
	                    ctx.writeAndFlush(response);
	                    
	                  } else {
	                    super.channelRead(ctx, msg);
	                  }
	                }
	    
	                @Override
	                public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
	                  ctx.flush();
	                }
	    
	                @Override
	                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
	                	ByteBuf responseBuff = Unpooled.wrappedBuffer(getSocketServerAddress().getBytes());
	                	
	                	ctx.writeAndFlush(new DefaultFullHttpResponse(
	                    HttpVersion.HTTP_1_1,
	                    HttpResponseStatus.INTERNAL_SERVER_ERROR,
	                    responseBuff
	                    ));
	                }
	                });
	              }
	          }).option(ChannelOption.SO_BACKLOG, 128)
	          .childOption(ChannelOption.SO_KEEPALIVE, true);
	          
	      channel = bootstrap.bind(conf.getHttpPort()).sync();
	      
	      System.out.println("socket address server started on port: " + conf.getHttpPort());
	      
	    } catch (final InterruptedException e) { 
	    	
	    }
		  }
	    
	  public void shutdown() // #2
	  {
	    slaveGroup.shutdownGracefully();
	    masterGroup.shutdownGracefully();

	    try
	    {
	      channel.channel().closeFuture().sync();
	    }
	    catch (InterruptedException e) { }
	  }
	  
	  private String getSocketServerAddress() throws UnknownHostException {
		  return InetAddress.getLocalHost().getHostAddress() + ":" + conf.getPort();
	  }
}
