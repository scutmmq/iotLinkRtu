package com.scutmmq.manager;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RTU 连接管理器
 *
 * <p>负责管理 rtuId 与 Channel 的双向映射关系</p>
 * <p>用于认证后建立映射，数据上报时识别来源，配置下发时查找目标</p>
 *
 * @author Claude
 * @since 2026-03-10
 */
@Slf4j
public class RtuConnectionManager {

    /**
     * rtuId → Channel 映射（用于配置下发时查找 channel）
     */
    private final Map<String, Channel> rtuIdToChannel = new ConcurrentHashMap<>();

    /**
     * Channel → rtuId 映射（用于数据上报时识别 rtuId）
     */
    private final Map<Channel, String> channelToRtuId = new ConcurrentHashMap<>();

    /**
     * 注册 RTU 连接（认证成功后调用）
     *
     * @param rtuId RTU 唯一标识
     * @param channel Netty Channel
     */
    public void register(String rtuId, Channel channel) {
        // 检查是否已存在该 rtuId 的连接（踢掉旧连接）
        Channel oldChannel = rtuIdToChannel.get(rtuId);
        if (oldChannel != null && oldChannel.isActive()) {
            log.warn("RTU {} 已存在连接，踢掉旧连接", rtuId);
            channelToRtuId.remove(oldChannel);
            oldChannel.close();
        }

        // 建立双向映射
        rtuIdToChannel.put(rtuId, channel);
        channelToRtuId.put(channel, rtuId);

        log.info("RTU {} 注册成功，当前连接数：{}", rtuId, rtuIdToChannel.size());
    }

    /**
     * 注销 RTU 连接（连接断开时调用）
     *
     * @param channel Netty Channel
     */
    public void unregister(Channel channel) {
        String rtuId = channelToRtuId.remove(channel);
        if (rtuId != null) {
            rtuIdToChannel.remove(rtuId);
            log.info("RTU {} 注销成功，当前连接数：{}", rtuId, rtuIdToChannel.size());
        }
    }

    /**
     * 根据 rtuId 获取 Channel
     *
     * @param rtuId RTU 唯一标识
     * @return Channel，如果不存在则返回 null
     */
    public Channel getChannel(String rtuId) {
        return rtuIdToChannel.get(rtuId);
    }

    /**
     * 根据 Channel 获取 rtuId
     *
     * @param channel Netty Channel
     * @return rtuId，如果不存在则返回 null
     */
    public String getRtuId(Channel channel) {
        return channelToRtuId.get(channel);
    }

    /**
     * 检查 RTU 是否已认证
     *
     * @param channel Netty Channel
     * @return true 如果已认证
     */
    public boolean isAuthenticated(Channel channel) {
        return channelToRtuId.containsKey(channel);
    }

    /**
     * 获取当前连接数
     *
     * @return 连接数
     */
    public int getConnectionCount() {
        return rtuIdToChannel.size();
    }
}
