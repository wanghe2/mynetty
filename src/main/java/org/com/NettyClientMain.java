package org.com;

import io.netty.bootstrap.Bootstrap;
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
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringEncoder;

/**
 * netty的客户端
 * @author wanghe
 *
 */
public class NettyClientMain {
	
	private String host="127.0.0.1";
	private int port=8080;

	public static void main(String[] args) {
		NettyClientMain nettyClient=new NettyClientMain();
		nettyClient.start();
	}
	
	public void start() {
		EventLoopGroup group=new NioEventLoopGroup();
		try {
			Bootstrap bootstrap=new Bootstrap();
			bootstrap.group(group)
					 .option(ChannelOption.SO_KEEPALIVE, true)
					 .channel(NioSocketChannel.class)
					 .handler(new ChannelInitializer<SocketChannel>() {

		    				@Override
							protected void initChannel(SocketChannel ch) throws Exception {
		    					
		    					//字符串编码器，一定要加在ClientHandler 的上面
		    	                ch.pipeline().addLast(new StringEncoder());
		    	                ch.pipeline().addLast(new DelimiterBasedFrameDecoder(   
		    	                        Integer.MAX_VALUE, Delimiters.lineDelimiter()[0])); 
								ch.pipeline().addLast(new ClientHandler());
							}
						});
			
			ChannelFuture future=	bootstrap.connect(this.host, this.port).sync();
			System.err.println("连接成功");
			
			for(int i=0;i<5;i++){
	            String msg = "ssss"+i+"\r\n";
	            future.channel().writeAndFlush(Unpooled.copiedBuffer(msg.getBytes()));
	        }
	        future.channel().closeFuture().sync();//等待服务端的端口关闭（阻塞）
		}catch(Exception e) {
			e.printStackTrace();
			group.shutdownGracefully();
		}
	}
	
	public class ClientHandler extends ChannelInboundHandlerAdapter{
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if(msg instanceof ByteBuf) {//可用telnet窗口测试
				ByteBuf byteBuf=(ByteBuf) msg;
				byte[] data=new byte[byteBuf.readableBytes()];
				byteBuf.readBytes(data);
				String str=new String(data);
				System.err.println(str);
			}	
			ByteBuf out=Unpooled.copiedBuffer("good job".getBytes());
			try {
				ctx.write(out);
			}finally {
				ctx.flush();
				ctx.close();
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			cause.printStackTrace();
			ctx.close();
		}
	
	}

}
