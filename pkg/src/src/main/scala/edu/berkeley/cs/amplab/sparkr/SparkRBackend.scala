package edu.berkeley.cs.amplab.sparkr

import java.io._
import java.net._
import java.util.{Map => JMap}
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel

import io.netty.handler.codec.bytes.ByteArrayDecoder
import io.netty.handler.codec.bytes.ByteArrayEncoder
import io.netty.handler.codec.LengthFieldBasedFrameDecoder

class SparkRBackend(portToBind: Int) {

  var channelFuture: ChannelFuture = null  
  var bootstrap: ServerBootstrap = null
  val backendImpl = new SparkRBackendInterfaceImpl 

  init(portToBind)
  
  def init(port: Int) {
    val bossGroup = new NioEventLoopGroup(SparkRConf.numServerThreads)
    val workerGroup = bossGroup
  
    bootstrap = new ServerBootstrap()
      .group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
  
    bootstrap.childHandler(new ChannelInitializer[SocketChannel]() {
      def initChannel(ch: SocketChannel) = {
        ch.pipeline()
          .addLast("encoder", new ByteArrayEncoder())
          .addLast("frameDecoder",
            // maxFrameLength = 2G
            // lengthFieldOffset = 0
            // lengthFieldLength = 4
            // lengthAdjustment = 0
            // initialBytesToStrip = 4, i.e. strip out the length field itself
            new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4))
          .addLast("decoder", new ByteArrayDecoder())
          .addLast("handler", new SparkRBackendHandler(backendImpl))
      }
    })

    channelFuture = bootstrap.bind(new InetSocketAddress(port))
    channelFuture.syncUninterruptibly()
    println("SparkR Backend server started on port :" + port)
  }

  def run() = {
    channelFuture.channel.closeFuture().syncUninterruptibly()
  }

  def close() = {
    if (channelFuture != null) {
      // close is a local operation and should finish within milliseconds; timeout just to be safe
      channelFuture.channel().close().awaitUninterruptibly(10, TimeUnit.SECONDS)
      channelFuture = null
    }
    if (bootstrap != null && bootstrap.group() != null) {
      bootstrap.group().shutdownGracefully()
    }
    if (bootstrap != null && bootstrap.childGroup() != null) {
      bootstrap.childGroup().shutdownGracefully()
    }
    bootstrap = null
  }

}

object SparkRBackend {
  def main(args: Array[String]) {
    if (args.length < 1) {
      System.err.println("Usage: SparkRBackend <port>")
      System.exit(-1)
    }
    val sparkRBackend = new SparkRBackend(args(0).toInt) 
    sparkRBackend.run()
  }
}
