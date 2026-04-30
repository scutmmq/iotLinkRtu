package com.scutmmq.web.dao;

import com.scutmmq.db.BaseDao;
import com.scutmmq.web.model.RtuGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * RTU 网关数据访问对象
 * 提供 rtu_gateway 表的 CRUD 操作
 * 
 * @author mo.mingqin@xlink
 * @date 2026-03-15
 */
public class RtuGatewayDao extends BaseDao {
    
    private static final Logger log = LoggerFactory.getLogger(RtuGatewayDao.class);
    
    /**
     * 根据 rtuId 查询 RTU 信息
     * 
     * @param rtuId RTU 唯一标识
     * @return RTU 信息（不存在返回 null）
     */
    public RtuGateway findByRtuId(String rtuId) {
        String sql = "SELECT * FROM rtu_gateway WHERE rtu_id = ?";
        return queryOne(sql, RtuGateway.class, rtuId);
    }
    
    /**
     * 根据 ID 查询 RTU 信息
     * 
     * @param id 主键 ID
     * @return RTU 信息（不存在返回 null）
     */
    public RtuGateway findById(Long id) {
        String sql = "SELECT * FROM rtu_gateway WHERE id = ?";
        return queryOne(sql, RtuGateway.class, id);
    }
    
    /**
     * 查询所有 RTU 列表
     * 
     * @return RTU 列表
     */
    public List<RtuGateway> findAll() {
        String sql = "SELECT * FROM rtu_gateway ORDER BY create_time DESC";
        return queryList(sql, RtuGateway.class);
    }
    
    /**
     * 分页查询 RTU 列表
     * 
     * @param offset 偏移量
     * @param limit 每页数量
     * @return RTU 列表
     */
    public List<RtuGateway> findPage(int offset, int limit) {
        String sql = "SELECT * FROM rtu_gateway ORDER BY create_time DESC LIMIT ? OFFSET ?";
        return queryList(sql, RtuGateway.class, limit, offset);
    }
    
    /**
     * 根据状态筛选 RTU 列表
     * 
     * @param status 网关状态（ENABLED/DISABLED）
     * @param online 在线状态（ONLINE/OFFLINE）
     * @param offset 偏移量
     * @param limit 每页数量
     * @return RTU 列表
     */
    public List<RtuGateway> findPageByStatus(String status, String online, int offset, int limit) {
        return findPageByFilters(status, online, null, offset, limit);
    }

    public List<RtuGateway> findPageByFilters(String status, String online, String rtuIdKeyword, int offset, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM rtu_gateway WHERE 1=1");
        List<Object> params = new ArrayList<>();

        appendFilters(sql, params, status, online, rtuIdKeyword);

        sql.append(" ORDER BY create_time DESC, id DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        return queryList(sql.toString(), RtuGateway.class, params.toArray());
    }

    public long countByFilters(String status, String online, String rtuIdKeyword) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM rtu_gateway WHERE 1=1");
        List<Object> params = new ArrayList<>();

        appendFilters(sql, params, status, online, rtuIdKeyword);
        return queryLong(sql.toString(), params.toArray());
    }
    
    /**
     * 保存 RTU 信息
     * 
     * @param rtu RTU 对象
     * @return 是否成功
     */
    public boolean save(RtuGateway rtu) {
        String sql = "INSERT INTO rtu_gateway (rtu_id, name, location, serial_port, status, online, secret) VALUES (?, ?, ?, ?, ?, ?, ?)";
        int rows = update(sql, rtu.getRtuId(), rtu.getName(), rtu.getLocation(),
                         rtu.getSerialPort(), rtu.getStatus(), rtu.getOnline(), rtu.getSecret());
        
        // 如果插入成功，查询返回的自动生成字段（id, create_time, update_time）
        if (rows > 0) {
            RtuGateway savedRtu = findByRtuId(rtu.getRtuId());
            if (savedRtu != null) {
                rtu.setId(savedRtu.getId());
                rtu.setCreateTime(savedRtu.getCreateTime());
                rtu.setUpdateTime(savedRtu.getUpdateTime());
            }
        }
        
        return rows > 0;
    }
    
    /**
     * 更新 RTU 信息
     * 
     * @param rtu RTU 对象
     * @return 是否成功
     */
    public boolean update(RtuGateway rtu) {
        String sql = "UPDATE rtu_gateway SET name=?, location=?, serial_port=?, status=?, online=?, heartbeat_time=? WHERE rtu_id=?";
        int rows = update(sql, rtu.getName(), rtu.getLocation(), rtu.getSerialPort(), rtu.getStatus(),
                         rtu.getOnline(), rtu.getHeartbeatTime(), rtu.getRtuId());
        return rows > 0;
    }
    
    /**
     * 更新 RTU 在线状态
     * 
     * @param rtuId RTU 唯一标识
     * @param online 在线状态
     * @param heartbeatTime 心跳时间
     * @return 是否成功
     */
    public boolean updateOnlineStatus(String rtuId, String online, java.time.LocalDateTime heartbeatTime) {
        String sql;
        int rows;
        if ("ONLINE".equalsIgnoreCase(online)) {
            sql = "UPDATE rtu_gateway SET online=?, heartbeat_time=? WHERE rtu_id=?";
            rows = update(sql, online, heartbeatTime, rtuId);
        } else {
            sql = "UPDATE rtu_gateway SET online=? WHERE rtu_id=?";
            rows = update(sql, online, rtuId);
        }
        return rows > 0;
    }
    
    /**
     * 删除 RTU（根据 rtuId）
     * 
     * @param rtuId RTU 唯一标识
     * @return 是否成功
     */
    public boolean deleteByRtuId(String rtuId) {
        String sql = "DELETE FROM rtu_gateway WHERE rtu_id = ?";
        int rows = update(sql, rtuId);
        return rows > 0;
    }
    
    /**
     * 检查 RTU 是否存在
     * 
     * @param rtuId RTU 唯一标识
     * @return 存在返回 true，否则返回 false
     */
    public boolean exists(String rtuId) {
        RtuGateway rtu = findByRtuId(rtuId);
        return rtu != null;
    }
    
    /**
     * 统计 RTU 总数
     * 
     * @return 总数
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM rtu_gateway";
        return (int) queryLong(sql);
    }

    private void appendFilters(StringBuilder sql, List<Object> params, String status, String online, String rtuIdKeyword) {
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (online != null && !online.trim().isEmpty()) {
            sql.append(" AND online = ?");
            params.add(online);
        }
        if (rtuIdKeyword != null && !rtuIdKeyword.trim().isEmpty()) {
            sql.append(" AND rtu_id LIKE ?");
            params.add("%" + rtuIdKeyword.trim() + "%");
        }
    }
}
