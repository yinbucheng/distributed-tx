package cn.mst.server.holder;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author ：yinchong
 * @create ：2019/7/11 19:56
 * @description：远程tcp连接持有器
 * @modified By：
 * @version:
 */
public class RemoteConnectionHolder {

    private static Logger logger = LoggerFactory.getLogger(RemoteConnectionHolder.class);
    private static Map<String, Struct> connectionHolder = new ConcurrentHashMap<>();

    public static void markRollback(String uuid){
        Struct struct = connectionHolder.get(uuid);
        if(struct==null){
            logger.error(" can not find struct by "+uuid);
            return;
        }
        struct.exception=true;
    }

    //添加事务连接进去
    public static void addChannel(String uuid, Channel channel) {
        Struct struct = connectionHolder.get(uuid);
        if (struct == null) {
            synchronized (RemoteConnectionHolder.class) {
                if (struct == null) {
                    struct = new Struct();
                    struct.beginTime = System.currentTimeMillis();
                    struct.channls = new LinkedBlockingQueue<>();
                    connectionHolder.put(uuid, struct);
                }
            }
        }
        struct.channls.add(channel);
    }

    public static long getTXStartTime(String uuid){
        Struct struct = connectionHolder.get(uuid);
        if(struct==null){
            logger.error(" can not find struct by "+uuid);
            return -1;
        }
        return struct.beginTime;
    }

    public static boolean ableCommit(String uuid){
        Struct struct = connectionHolder.get(uuid);
        if(struct==null){
            logger.error("can not find struct by "+uuid);
            return false;
        }
        return !struct.exception;
    }

    public static LinkedBlockingQueue<Channel> listChannels(String uuid){
        Struct struct = connectionHolder.get(uuid);
        if(struct==null){
            logger.error("can not find struct by "+uuid);
            return null;
        }
        return struct.channls;
    }

    public static void reomve(String uuid){
        connectionHolder.remove(uuid);
    }


    static class Struct {
        //是否发送异常(如果为true则进行回滚操作)
        volatile boolean exception;
        //事务开始时间
        volatile long beginTime;
        //事务所涉及的客户端
        volatile LinkedBlockingQueue<Channel> channls;
    }
}
