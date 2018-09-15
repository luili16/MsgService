package com.llx278.msgservice.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Debug {
    
    private static final Logger sLogger = LogManager.getLogger(Debug.class);
    
    public static void print(ByteBuf buf) {

        int type = buf.readInt();
        if (type == Type.FRAME_REGISTER) {

            sLogger.log(Level.DEBUG,"---- 注册帧 ----");
            int len = buf.readInt();
            sLogger.log(Level.DEBUG,"预期长度 : " + len + " 真实长度 : " + buf.readableBytes());
            if (len != buf.readableBytes()) {
                sLogger.log(Level.ERROR,"----------- 不相等------------");
            }
            int uid = buf.readInt();
            sLogger.log(Level.DEBUG,"uid : " + uid);

        } else if (type == Type.FRAME_MSG) {
            sLogger.log(Level.DEBUG,"--- 消息帧 -----");
            int len = buf.readInt();
            sLogger.log(Level.DEBUG,"预期长度 : " + len + " 真实长度 : " + buf.readableBytes());
            int fromUid = buf.readInt();
            int toUid = buf.readInt();
            HashMap<String,String> header = new HashMap<>();
            ContentParser.parse(header,buf);
            String body = buf.readCharSequence(buf.readableBytes(),CharsetUtil.UTF_8).toString();
            sLogger.log(Level.DEBUG,"fromUid : " + fromUid + " toUid : " + toUid);
            sLogger.log(Level.DEBUG,"header : " + header.toString());
            sLogger.log(Level.DEBUG,"body : " + body);
        } else if (type == Type.FRAME_HEART) {
            sLogger.log(Level.DEBUG,"心跳帧");
            int len = buf.readInt();
            sLogger.log(Level.DEBUG,"预期长度 : " + len + " 真实长度 : " + buf.readableBytes());
            int uid = buf.readInt();
            sLogger.log(Level.DEBUG,"uid : " + uid);
        }

    }

    public static class ContentParser {

        public static void parse(Map<String,String> headerMap, ByteBuf contentBuf) throws IllegalStateException {

            if (headerMap == null || contentBuf == null) {
                throw new IllegalArgumentException("parameter can not null");
            }
            ByteBuf buf = Unpooled.buffer();
            while (contentBuf.isReadable()) {
                byte ch = contentBuf.readByte();
                if (ch == '\r') {
                    break;
                }
                buf.writeByte(ch);
            }

            String header = buf.toString(CharsetUtil.UTF_8);

            if (header != null) {
                String[] params = header.split("\n");
                for (String param : params) {
                    String[] keyAndVal = param.split(":");
                    headerMap.put(keyAndVal[0],keyAndVal[1]);
                }
            }
        }

        public static String toHeader(Map<String,String> headerMap) {

            if (headerMap == null || headerMap.isEmpty()) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            Set<Map.Entry<String, String>> entries = headerMap.entrySet();
            for (Map.Entry<String,String> entry : entries) {
                String key = entry.getKey();
                String value = entry.getValue();
                sb.append(key).append(":").append(value).append("\n");
            }
            return sb.deleteCharAt(sb.length() -1 ).toString();
        }
    }

}
