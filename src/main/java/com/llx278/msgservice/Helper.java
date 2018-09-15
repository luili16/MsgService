package com.llx278.msgservice;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class Helper {

    private static final Logger sLogger = LogManager.getLogger(Helper.class);

    public static void removeClient(ChannelHandlerContext ctx) {
        Attribute<Map> mapAttr = ctx.channel().attr(MsgServer.sSocketMapAttr);
        Attribute<Integer> uidAttr = ctx.channel().attr(MsgServer.sUidAttr);
        if (mapAttr != null) {
            Map map = mapAttr.get();
            Integer uid = uidAttr.get();
            if (map == null) {
                sLogger.log(Level.ERROR,"map is null");
               return;
            }

            if (uid == null) {
                sLogger.log(Level.ERROR,"uid is null");
                return;
            }
            map.remove(uid);
        }
    }
}
