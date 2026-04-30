package com.scutmmq.web.service;

import com.scutmmq.web.dao.RtuGatewayDao;
import com.scutmmq.web.model.RtuConfig;
import com.scutmmq.web.model.RtuGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * RTU 网关业务服务
 * 提供 RTU 注册、查询、更新等业务逻辑
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-15
 */
public class RtuGatewayService {
    
    private static final Logger log = LoggerFactory.getLogger(RtuGatewayService.class);
    private final RtuGatewayDao rtuDao = new RtuGatewayDao();
    private final RtuConfigService configService = new RtuConfigService();
    
    /**
     * 检查 RTU 是否存在
     * 
     * @param rtuId RTU 唯一标识
     * @return 存在返回 true，否则返回 false
     */
    public boolean exists(String rtuId) {
        return rtuDao.exists(rtuId);
    }
    
    /**
     * 根据 rtuId 查询 RTU 信息
     * 
     * @param rtuId RTU 唯一标识
     * @return RTU 信息（不存在返回 null）
     */
    public RtuGateway findByRtuId(String rtuId) {
        return rtuDao.findByRtuId(rtuId);
    }
    
    /**
     * 分页查询 RTU 列表
     * 
     * @param page 页码（从 1 开始）
     * @param size 每页数量
     * @param status 状态筛选（可选）
     * @param online 在线状态筛选（可选）
     * @return RTU 列表
     */
    public List<RtuGateway> findPage(int page, int size, String status, String online, String rtuIdKeyword) {
        int offset = (page - 1) * size;
        return rtuDao.findPageByFilters(status, online, rtuIdKeyword, offset, size);
    }

    public long count(String status, String online, String rtuIdKeyword) {
        return rtuDao.countByFilters(status, online, rtuIdKeyword);
    }
    
    /**
     * 注册 RTU
     * 
     * @param rtu RTU 对象
     * @return 注册后的 RTU 对象（包含生成的 secret）
     * @throws IllegalArgumentException RTU 已存在时抛出
     */
    public RtuGateway register(RtuGateway rtu, Integer samplingInterval) {
        // 检查是否已存在
        if (rtuDao.exists(rtu.getRtuId())) {
            throw new IllegalArgumentException("RTU 已存在：" + rtu.getRtuId());
        }
        
        // 生成随机 secret（32 位 UUID）
        String secret = generateRandomSecret();
        rtu.setSecret(secret);
        
        // 设置默认状态
        if (rtu.getStatus() == null || rtu.getStatus().trim().isEmpty()) {
            rtu.setStatus("ENABLED");
        }
        if (rtu.getOnline() == null || rtu.getOnline().trim().isEmpty()) {
            rtu.setOnline("OFFLINE");
        }
        
        // 保存到数据库
        boolean success = rtuDao.save(rtu);
        if (!success) {
            throw new RuntimeException("保存 RTU 失败");
        }

        configService.createDefaultConfig(rtu, samplingInterval);
        
        log.info("RTU 注册成功：rtuId={}, name={}, secret={}", rtu.getRtuId(), rtu.getName(), secret);
        return rtu;
    }

    public RtuGateway register(RtuGateway rtu) {
        return register(rtu, null);
    }
    
    /**
     * 更新 RTU 信息
     * 
     * @param rtu RTU 对象
     * @return 是否成功
     * @throws IllegalArgumentException RTU 不存在时抛出
     */
    public boolean update(RtuGateway rtu) {
        // 检查是否存在
        RtuGateway existingRtu = rtuDao.findByRtuId(rtu.getRtuId());
        if (existingRtu == null) {
            throw new IllegalArgumentException("RTU 不存在：" + rtu.getRtuId());
        }
        
        if (rtu.getName() == null) {
            rtu.setName(existingRtu.getName());
        }
        if (rtu.getLocation() == null) {
            rtu.setLocation(existingRtu.getLocation());
        }
        if (rtu.getSerialPort() == null) {
            rtu.setSerialPort(existingRtu.getSerialPort());
        }
        if (rtu.getStatus() == null) {
            rtu.setStatus(existingRtu.getStatus());
        }
        if (rtu.getOnline() == null) {
            rtu.setOnline(existingRtu.getOnline());
        }
        if (rtu.getHeartbeatTime() == null) {
            rtu.setHeartbeatTime(existingRtu.getHeartbeatTime());
        }

        boolean success = rtuDao.update(rtu);
        if (success) {
            log.info("RTU 更新成功：rtuId={}", rtu.getRtuId());
        }
        return success;
    }
    
    /**
     * 删除 RTU
     * 
     * @param rtuId RTU 唯一标识
     * @return 是否成功
     * @throws IllegalArgumentException RTU 不存在时抛出
     */
    public boolean delete(String rtuId) {
        RtuGateway existing = rtuDao.findByRtuId(rtuId);
        if (existing == null) {
            throw new IllegalArgumentException("RTU 不存在：" + rtuId);
        }

        if ("ONLINE".equalsIgnoreCase(existing.getOnline())) {
            throw new IllegalStateException("RTU 在线，不允许删除：" + rtuId);
        }

        configService.deleteByRtuId(rtuId);
        
        // 删除 RTU
        boolean success = rtuDao.deleteByRtuId(rtuId);
        if (success) {
            log.info("RTU 删除成功：rtuId={}", rtuId);
        }
        return success;
    }
    
    /**
     * 更新 RTU 在线状态
     * 
     * @param rtuId RTU 唯一标识
     * @param online 在线状态
     * @return 是否成功
     */
    public boolean updateOnlineStatus(String rtuId, String online) {
        String normalizedOnline = online == null ? null : online.trim().toUpperCase();
        java.time.LocalDateTime heartbeatTime = "ONLINE".equals(normalizedOnline)
            ? java.time.LocalDateTime.now()
            : null;
        boolean success = rtuDao.updateOnlineStatus(rtuId, normalizedOnline, heartbeatTime);
        if (success) {
            log.info("RTU 在线状态更新成功：rtuId={}, online={}", rtuId, normalizedOnline);
        }
        return success;
    }
    
    /**
     * 生成随机 secret
     * 使用 UUID 生成 32 位随机字符串
     * 
     * @return 随机 secret
     */
    private String generateRandomSecret() {
        // 生成 32 位 UUID（去掉横杠）
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    public RtuConfig getConfig(String rtuId) {
        return configService.getByRtuId(rtuId);
    }
}
