package com.scutmmq.web.service;

import com.scutmmq.BadRequestException;
import com.scutmmq.NotFoundException;
import com.scutmmq.exception.ErrorCode;
import com.scutmmq.web.dao.RtuDataDao;
import com.scutmmq.web.dao.RtuGatewayDao;
import com.scutmmq.web.model.RtuData;
import com.scutmmq.web.model.RtuDataStatistics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * RTU 温湿度采集数据服务。
 */
public class RtuDataService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RtuGatewayDao gatewayDao = new RtuGatewayDao();
    private final RtuDataDao dataDao = new RtuDataDao();

    public RtuData getRealtime(String rtuId) {
        requireExistingRtu(rtuId);
        if (!dataDao.tableExists()) {
            throw new NotFoundException(ErrorCode.DATA_NOT_FOUND, "rtu_data 表不存在，请先执行最新初始化脚本");
        }
        RtuData latest = dataDao.findLatestByRtuId(rtuId);
        if (latest == null) {
            throw new NotFoundException(ErrorCode.DATA_NOT_FOUND, "RTU 暂无采集数据：" + rtuId);
        }
        return latest;
    }

    public List<RtuData> findHistory(String rtuId, String startTime, String endTime, int page, int size) {
        requireExistingRtu(rtuId);
        TimeRange range = parseRange(startTime, endTime);
        if (!dataDao.tableExists()) {
            return List.of();
        }
        int offset = Math.max(page - 1, 0) * size;
        return dataDao.findPage(rtuId, range.start(), range.end(), offset, size);
    }

    public long countHistory(String rtuId, String startTime, String endTime) {
        requireExistingRtu(rtuId);
        TimeRange range = parseRange(startTime, endTime);
        if (!dataDao.tableExists()) {
            return 0L;
        }
        return dataDao.count(rtuId, range.start(), range.end());
    }

    public RtuDataStatistics statistics(String rtuId, String startTime, String endTime) {
        requireExistingRtu(rtuId);
        TimeRange range = parseRange(startTime, endTime);
        if (!dataDao.tableExists()) {
            RtuDataStatistics empty = new RtuDataStatistics();
            empty.setDataCount(0L);
            return empty;
        }
        RtuDataStatistics statistics = dataDao.statistics(rtuId, range.start(), range.end());
        if (statistics == null) {
            statistics = new RtuDataStatistics();
        }
        if (statistics.getDataCount() == null) {
            statistics.setDataCount(0L);
        }
        return statistics;
    }

    private void requireExistingRtu(String rtuId) {
        if (!gatewayDao.exists(rtuId)) {
            throw new NotFoundException(ErrorCode.RTU_NOT_FOUND, "RTU 不存在：" + rtuId);
        }
    }

    private TimeRange parseRange(String startTime, String endTime) {
        LocalDateTime start = parseDateTime(startTime, "startTime");
        LocalDateTime end = parseDateTime(endTime, "endTime");
        if (start.isAfter(end)) {
            throw new BadRequestException(ErrorCode.TIMESTAMP_INVALID, "开始时间不能大于结束时间");
        }
        return new TimeRange(start, end);
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " 不能为空");
        }
        String normalized = value.trim().replace('T', ' ');
        if (normalized.length() >= 19) {
            normalized = normalized.substring(0, 19);
        }
        try {
            return LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(ErrorCode.TIMESTAMP_INVALID, fieldName + " 时间格式应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private record TimeRange(LocalDateTime start, LocalDateTime end) {
    }
}
