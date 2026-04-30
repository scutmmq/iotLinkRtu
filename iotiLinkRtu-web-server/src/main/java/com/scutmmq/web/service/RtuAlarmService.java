package com.scutmmq.web.service;

import com.scutmmq.BadRequestException;
import com.scutmmq.NotFoundException;
import com.scutmmq.exception.ErrorCode;
import com.scutmmq.web.dao.RtuAlarmDao;
import com.scutmmq.web.model.RtuAlarm;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RTU 报警服务
 */
public class RtuAlarmService {

    private final RtuAlarmDao alarmDao = new RtuAlarmDao();

    public List<RtuAlarm> findPage(String rtuId, String status, int page, int size) {
        int offset = Math.max(page - 1, 0) * size;
        return alarmDao.findPage(rtuId, status, offset, size);
    }

    public long count(String rtuId, String status) {
        return alarmDao.count(rtuId, status);
    }

    public RtuAlarm findById(Long id) {
        RtuAlarm alarm = alarmDao.findById(id);
        if (alarm == null) {
            throw new NotFoundException(ErrorCode.ALARM_NOT_FOUND, "报警记录不存在：" + id);
        }
        return alarm;
    }

    public void handle(Long id, String handleResult, String handler) {
        RtuAlarm alarm = findById(id);
        if ("HANDLED".equalsIgnoreCase(alarm.getStatus())) {
            throw new BadRequestException(ErrorCode.ALARM_ALREADY_HANDLED, "报警已处理：" + id);
        }
        alarmDao.handle(id, handleResult, handler, LocalDateTime.now());
    }
}
