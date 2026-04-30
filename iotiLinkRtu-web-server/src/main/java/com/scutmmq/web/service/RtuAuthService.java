package com.scutmmq.web.service;

import com.scutmmq.utils.Sha256Util;
import com.scutmmq.web.dao.RtuGatewayDao;
import com.scutmmq.web.model.RtuGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTU 认证服务
 * 提供 RTU 身份验证功能，实现基于 SHA-256 的密钥验证机制
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-15
 */
public class RtuAuthService {
    
    private static final Logger log = LoggerFactory.getLogger(RtuAuthService.class);
    private final RtuGatewayDao rtuDao = new RtuGatewayDao();
    
    /**
     * 验证 RTU 身份
     * 
     * @param rtuId RTU 唯一标识
     * @param secretHash 接收到的密钥哈希值
     * @param timestamp 时间戳（毫秒）
     * @return 验证结果对象
     */
    public VerifyResult verify(String rtuId, String secretHash, long timestamp) {
        VerifyResult result = new VerifyResult();
        result.setRtuId(rtuId);
        
        // 1. 检查时间戳有效性（防止重放攻击）
        if (!Sha256Util.isTimestampValid(timestamp)) {
            log.warn("RTU 认证失败：时间戳无效，rtuId={}, timestamp={}", rtuId, timestamp);
            result.setValid(false);
            result.setMessage("时间戳无效，请在 5 分钟内完成认证");
            return result;
        }
        
        // 2. 查询 RTU 信息
        RtuGateway rtu = rtuDao.findByRtuId(rtuId);
        if (rtu == null) {
            log.warn("RTU 认证失败：RTU 不存在，rtuId={}", rtuId);
            result.setValid(false);
            result.setMessage("RTU 不存在：" + rtuId);
            return result;
        }
        
        // 3. 检查 RTU 状态
        if ("DISABLED".equals(rtu.getStatus())) {
            log.warn("RTU 认证失败：RTU 已禁用，rtuId={}", rtuId);
            result.setValid(false);
            result.setMessage("RTU 已被禁用");
            return result;
        }
        
        // 4. 验证密钥哈希
        String secret = rtu.getSecret();
        boolean hashMatch = Sha256Util.verifySecret(secret, rtuId, secretHash, timestamp);
        
        if (!hashMatch) {
            log.warn("RTU 认证失败：密钥不匹配，rtuId={}", rtuId);
            result.setValid(false);
            result.setMessage("认证密钥错误");
            return result;
        }
        
        // 5. 认证成功
        rtuDao.updateOnlineStatus(rtuId, "ONLINE", java.time.LocalDateTime.now());
        log.info("RTU 认证成功：rtuId={}, status={}", rtuId, rtu.getStatus());
        result.setValid(true);
        result.setStatus(rtu.getStatus());
        result.setMessage("认证成功");
        
        return result;
    }
    
    /**
     * 验证结果封装类
     */
    public static class VerifyResult {
        /** RTU 唯一标识 */
        private String rtuId;
        
        /** 是否验证通过 */
        private boolean valid;
        
        /** RTU 状态（ENABLED/DISABLED） */
        private String status;
        
        /** 验证消息 */
        private String message;
        
        // Getters and Setters
        
        public String getRtuId() {
            return rtuId;
        }
        
        public void setRtuId(String rtuId) {
            this.rtuId = rtuId;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public void setValid(boolean valid) {
            this.valid = valid;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        @Override
        public String toString() {
            return "VerifyResult{" +
                    "rtuId='" + rtuId + '\'' +
                    ", valid=" + valid +
                    ", status='" + status + '\'' +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
