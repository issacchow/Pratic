package com.isc.tcp.impl.reactor;

import com.isc.tcp.IRequestHandler;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;


/**
 * 比{@link SimpleHandler} 多了线程池处理方式
 * handler本身作为事件执行器，改为事件处理中介，将任务委托给线程池处理
 * <p>
 * 主要有三个阶段:
 * 1. 读数据: 负责读取一条完整应用协议的数据
 * 2. 处理数据: 根据应用协议数据进行处理, 处理后并将要响应的结果写入缓冲区，等待切换写数据状态时直接发送
 * 3. 写数据: 将响应数据发送给客户端
 * <p>
 * 其中，只有处理数据是交由线程池处理,原来 {@link SimpleHandler} 只是单线程执行
 *
 * 存在线程安全问题:
 * 因为writeBuffer是共享的, 对于同一个连接的多个并行数据处理任务的情况,存在对writeBuffer的竞争， 当其中一个任务被执行完毕后需要写数据，这个时候就切换到写的状态，
 * 但同时有可能其他任务在执行，并且往writeBuffer里写入待发送数据,这样就存在冲突了。
 *
 * 存在问题的并行时序
 * 【时间偏移】       【Reactor线程】              【Worker1】          【Worker2】
 * ----------------------------------------------------------------------------------------
 * 100ms             Read
 * 110ms             Read
 * 120ms                                          writeBuffer.put()
 *
 * 130ms             socket.send(writeBuffer)
 * 130ms                                                               writeBuffer.put()
 *
 *
 * 第130ms 发生两条线程并行操作writeBuffer
 */
public class ThreadPoolHandler extends SimpleHandler {


    Executor executor;


    ThreadPoolHandler(Selector sel, SocketChannel c, IRequestHandler requestHandler, Executor executor) throws IOException {
        super(sel, c, requestHandler);
        this.executor = executor;
    }

//
//    private synchronized void send() throws IOException {
//
//        socket.write(writeBuffer);
//
//        if (sendIsComplete()) {
//            processSendComplete();
//            // 切换状态, 下次触发事件时调用run方法会触发read逻辑
//            readingState(true);
//        }
//
//    }
//
//    private synchronized void read() throws IOException {
//
//        this.socket.read(readBuffer);
//
//        if (readIsComplete()) {
//
//            processingState();
//            this.executor.execute(new Runnable() {
//                @Override
//                public void run() {
//                    processReadComplete();
//                }
//            });
//
//        }
//
//    }



    @Override
    protected void processReadComplete() {

        if (requestHandler != null) {
            System.out.println();
            try {
                System.out.printf("submit task,connection: %s" , socket.getRemoteAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println();
            this.executor.execute(new Runnable() {
                @Override
                public void run() {

                    // 这里存在线程安全问题,多条线程同时向writeBuffer写入数据
                    requestHandler.handle(socket, readBuffer, writeBuffer);
                    sendingState(false);
                }
            });
        }


    }
}
