package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.dao.AdminOperationLogMapper;
import org.example.dao.SysAdminMapper;
import org.example.entity.SysAdmin;
import org.example.model.AdminOperationLog;
import org.example.model.response.AdminOperationLogResponse;
import org.example.model.response.PagedResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminOperationLogService {

    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String FORBIDDEN_MESSAGE = "仅超级管理员可查看操作日志";

    private final AdminOperationLogMapper operationLogMapper;
    private final SysAdminMapper sysAdminMapper;

    public void record(Integer operatorId, String module, String action, String targetType, Integer targetId,
                       String targetLabel, Integer fromStatus, Integer toStatus, String remark, String snapshotJson) {
        AdminOperationLog log = new AdminOperationLog();
        log.setOperatorId(operatorId);
        SysAdmin operator = operatorId == null ? null : sysAdminMapper.findById(operatorId);
        if (operator != null) {
            log.setOperatorName(operator.getName());
            log.setOperatorPhone(operator.getPhone());
            log.setOperatorRole(normalizeRole(operator.getRole()));
        }
        log.setModule(trim(module));
        log.setAction(trim(action));
        log.setTargetType(trim(targetType));
        log.setTargetId(targetId);
        log.setTargetLabel(trim(targetLabel));
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setRemark(trim(remark));
        log.setSnapshotJson(snapshotJson == null || snapshotJson.trim().isEmpty() ? "{}" : snapshotJson.trim());
        log.setCreateTime(new Date());
        operationLogMapper.insert(log);
    }

    public PagedResponse<AdminOperationLogResponse> getLogs(Integer operatorId, String module, String action,
                                                            String operatorRole, String keyword,
                                                            String startTime, String endTime,
                                                            Integer page, Integer pageSize) {
        requireSuperAdmin(operatorId);
        int safePage = normalizePage(page);
        int safeSize = normalizePageSize(pageSize);
        String normalizedModule = trim(module);
        String normalizedAction = trim(action);
        String normalizedRole = trim(operatorRole);
        String normalizedKeyword = trim(keyword);
        int total = operationLogMapper.countLogs(normalizedModule, normalizedAction, normalizedRole, normalizedKeyword, trim(startTime), trim(endTime));
        List<AdminOperationLog> logs = operationLogMapper.findLogs(
                normalizedModule,
                normalizedAction,
                normalizedRole,
                normalizedKeyword,
                trim(startTime),
                trim(endTime),
                safePage * safeSize,
                safeSize
        );
        return new PagedResponse<>(toResponses(logs), total, safePage, safeSize);
    }

    public List<AdminOperationLogResponse> getDashboardRecentLogs(Integer operatorId, int limit) {
        if (!isSuperAdmin(operatorId)) {
            return List.of();
        }
        int safeLimit = limit <= 0 ? 6 : Math.min(limit, 10);
        return toResponses(operationLogMapper.findLogs(null, null, null, null, null, null, 0, safeLimit));
    }

    public int countTodayLogs(Integer operatorId, String today) {
        if (!isSuperAdmin(operatorId)) {
            return 0;
        }
        return operationLogMapper.countLogs(null, null, null, null, today + " 00:00:00", today + " 23:59:59");
    }

    private void requireSuperAdmin(Integer operatorId) {
        if (!isSuperAdmin(operatorId)) {
            throw new SecurityException(FORBIDDEN_MESSAGE);
        }
    }

    private boolean isSuperAdmin(Integer operatorId) {
        SysAdmin operator = operatorId == null ? null : sysAdminMapper.findById(operatorId);
        return operator != null && ROLE_SUPER_ADMIN.equals(normalizeRole(operator.getRole()));
    }

    private List<AdminOperationLogResponse> toResponses(List<AdminOperationLog> logs) {
        List<AdminOperationLogResponse> responses = new ArrayList<>();
        if (logs == null) {
            return responses;
        }
        for (AdminOperationLog log : logs) {
            AdminOperationLogResponse response = new AdminOperationLogResponse();
            response.setId(log.getId());
            response.setOperatorId(log.getOperatorId());
            response.setOperatorName(log.getOperatorName());
            response.setOperatorPhoneMasked(maskPhone(log.getOperatorPhone()));
            response.setOperatorRole(log.getOperatorRole());
            response.setModule(log.getModule());
            response.setAction(log.getAction());
            response.setTargetType(log.getTargetType());
            response.setTargetId(log.getTargetId());
            response.setTargetLabel(log.getTargetLabel());
            response.setFromStatus(log.getFromStatus());
            response.setToStatus(log.getToStatus());
            response.setRemark(log.getRemark());
            response.setCreateTime(log.getCreateTime());
            responses.add(response);
        }
        return responses;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }

    private String normalizeRole(String role) {
        return ROLE_SUPER_ADMIN.equals(trim(role)) ? ROLE_SUPER_ADMIN : "ADMIN";
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
