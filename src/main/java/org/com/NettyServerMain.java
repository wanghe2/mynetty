package org.com;


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
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * 简单的netty(服务端)
 *
 */
public class NettyServerMain 
{
	
	public int port=8080;
	
    public static void main( String[] args )
    {
    	NettyServerMain nettyServer=new NettyServerMain();
    	nettyServer.serverstart();
    }
    
    public void serverstart() {
    	//bossGroup、workerGroup 是线程池（一个负责连接，一个负责读写处理）
    	EventLoopGroup bossGroup=new NioEventLoopGroup();
    	EventLoopGroup workerGroup=new NioEventLoopGroup();
    	try {
    		ServerBootstrap server=new ServerBootstrap();
    		server.group(bossGroup,workerGroup)
    			  .channel(NioServerSocketChannel.class)
    			  .option(ChannelOption.SO_BACKLOG, 128)//连接线程数
    			  .childOption(ChannelOption.SO_KEEPALIVE, true)//保存长连接
    			  .childHandler(new ChannelInitializer<SocketChannel>() {

    				@Override
					protected void initChannel(SocketChannel ch) throws Exception {
    					/*****
    					 * 
	    					//屏蔽的是 针对http协议的编解码 (如果没有编解码，handlder的httrequest的处理逻辑就进不去)
	    					//Netty对HTTP协议的封装，顺序有要求
							// HttpResponseEncoder 编码器
	    					ch.pipeline().addLast(new HttpResponseEncoder());
							// HttpRequestDecoder 解码器
	    					ch.pipeline().addLast(new HttpRequestDecoder());
    					*
    					*/
    					
    					//发送端要加分隔符，如 \r \n分隔符
    					ch.pipeline().addLast(new DelimiterBasedFrameDecoder(Integer.MAX_VALUE, Delimiters.lineDelimiter()[0]));
    		            // 业务逻辑处理
						ch.pipeline().addLast(new ServerHandler());
					}
				});
    			//绑定接口，同步等待其成功	
    			ChannelFuture future=server.bind(this.port).sync();
    			System.err.println("启动成功");
    			future.channel().closeFuture().sync();//等待服务端的监听端口关闭（阻塞）
    	}catch (Exception e) {
    		e.printStackTrace();
    		/**
    		 * 优雅退出，释放线程池资源
    		 */
    		bossGroup.shutdownGracefully();
    		workerGroup.shutdownGracefully();
		}
    	
    }
  
    public  class ServerHandler extends ChannelInboundHandlerAdapter {
		
    	/**
    	 * 
    	 */
    	@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {//有数据过来，接收并做处理
			if (msg instanceof HttpRequest){//这里其实是针对 浏览器发出的http请求接收做了解析
				HttpRequest req = (HttpRequest) msg;
				System.err.println("uri--"+req.uri());//传入（读入的信息）
				String out="hello,world--"+req.uri();//要写入的信息
				try {
					// 设置 http协议及请求头信息
					FullHttpResponse response = new DefaultFullHttpResponse(
						// 设置http版本为1.1
						HttpVersion.HTTP_1_1,
						// 设置响应状态码
						HttpResponseStatus.OK,
						// 将输出值写出 编码为UTF-8
						Unpooled.wrappedBuffer(out.getBytes("UTF-8")));

					response.headers().set("Content-Type", "text/html;");
					ctx.write(response);
				} finally {
					ctx.flush();
					ctx.close();
				}
			}else if(msg instanceof ByteBuf) {//可用telnet窗口测试
				ByteBuf byteBuf=(ByteBuf) msg;
				byte[] data=new byte[byteBuf.readableBytes()];
				byteBuf.readBytes(data);
				String str=new String(data);
				System.err.println(str);
				ByteBuf out=Unpooled.copiedBuffer("good job\r\n".getBytes());
				try {
					ctx.write(out);
				}finally {
					ctx.flush();
					ctx.close();
				}
			}
			
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {//出现异常
			cause.printStackTrace();
			ctx.close();
		}
		
		@Override
	    public void channelActive(ChannelHandlerContext ctx) throws Exception {//tcp连接建立后，走该方法
	        ctx.fireChannelActive();
	        System.err.println("服务端打印：建立了一个tcp连接");
	    }
		
	}
   
}
