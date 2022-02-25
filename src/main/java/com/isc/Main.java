package com.isc;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.CountDownLatch2;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

/**
 * rocketmq 相关的api 测试使用
 * 官方样例:
 * <p>
 * https://github.com/apache/rocketmq/blob/develop/docs/cn/RocketMQ_Example.md#1producer%E7%AB%AF%E5%8F%91%E9%80%81%E5%90%8C%E6%AD%A5%E6%B6%88%E6%81%AF
 * </p>
 */
public class Main {

    private SendResult result;

    public static void main(String[] args) {

    }

    /**
     * 同步发送消息（保证消息可靠性)
     */
    public static void produceSyncMessage() throws InterruptedException, RemotingException, MQClientException, MQBrokerException {

        DefaultMQProducer producer = new DefaultMQProducer("my-producer-group");
        producer.setNamesrvAddr("127.0.0.1:9876");

        try {
            producer.start();
        } catch (MQClientException e) {
            e.printStackTrace();
        }

        byte[] data = "Hello World".getBytes();
        for(int i=0;i<100;i++) {
            SendResult result = producer.send(
                    new Message("helloworld", "mytag1,mytag2", "mykey1,mykey2", data)
            );
        }


        producer.shutdown();

    }

    public static void produceAsyncMessage() throws InterruptedException, RemotingException, MQClientException, MQBrokerException, UnsupportedEncodingException {

        // 实例化消息生产者Producer
        DefaultMQProducer producer = new DefaultMQProducer("please_rename_unique_group_name");
        // 设置NameServer的地址
        producer.setNamesrvAddr("localhost:9876");
        // 启动Producer实例
        producer.start();
        producer.setRetryTimesWhenSendAsyncFailed(0);

        int messageCount = 100;
        // 根据消息数量实例化倒计时计算器
        final CountDownLatch2 countDownLatch = new CountDownLatch2(messageCount);
        for (int i = 0; i < messageCount; i++) {
            final int index = i;
            // 创建消息，并指定Topic，Tag和消息体
            Message msg = new Message("TopicTest",
                    "TagA",
                    "OrderID188",
                    "Hello world".getBytes(RemotingHelper.DEFAULT_CHARSET));
            // SendCallback接收异步返回结果的回调
            producer.send(msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    countDownLatch.countDown();
                    System.out.printf("%-10d OK %s %n", index,
                            sendResult.getMsgId());
                }
                @Override
                public void onException(Throwable e) {
                    countDownLatch.countDown();
                    System.out.printf("%-10d Exception %s %n", index, e);
                    e.printStackTrace();
                }
            });
        }
        // 等待5s
        countDownLatch.await(5, TimeUnit.SECONDS);
        // 如果不再发送消息，关闭Producer实例。
        producer.shutdown();

    }


}
