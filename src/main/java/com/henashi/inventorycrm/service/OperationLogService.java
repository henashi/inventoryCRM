package com.henashi.inventorycrm.service;

import com.henashi.inventorycrm.dto.OperationLogCreateDTO;
import com.henashi.inventorycrm.dto.OperationLogDTO;
import com.henashi.inventorycrm.exception.BusinessException;
import com.henashi.inventorycrm.mapper.OperationLogMapper;
import com.henashi.inventorycrm.pojo.OperationLog;
import com.henashi.inventorycrm.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final Logger log = LoggerFactory.getLogger(OperationLogService.class);
    private final OperationLogRepository operationLogRepository;
    private final OperationLogMapper operationLogMapper;

    public OperationLogDTO findOperationLogDTOById(Long logId) {
        log.debug("查询操作日志详情: id={}", logId);
        if (logId == null || logId <= 0) {
            log.warn("日志ID无效: id={}", logId);
            throw new IllegalArgumentException("日志ID无效");
        }
        return operationLogRepository.findById(logId)
                .map(logEntity -> {
                    log.info("找到操作日志: {}", logEntity.getId());
                    return operationLogMapper.fromEntity(logEntity);
                })
                .orElseThrow(() -> {
                    log.warn("操作日志不存在: id={}", logId);
                    return new BusinessException("OPERATION_LOG_NOT_FOUND",
                            String.format("操作日志(ID: %d)不存在", logId));
                });
    }

    @Transactional
    public OperationLogDTO saveOperationLog(OperationLogCreateDTO logCreateDTO) {
        log.debug("创建操作日志: 模块={}, 操作类型={}",
                logCreateDTO.module(), logCreateDTO.operationType());

        OperationLog operationLog = operationLogMapper.createToEntity(logCreateDTO);
        operationLog.setOperationTime(LocalDateTime.now());

        OperationLog saved = operationLogRepository.save(operationLog);

        log.info("操作日志创建成功: {} - {}", saved.getModule(), saved.getOperationType());
        return operationLogMapper.fromEntity(saved);
    }

    public void logOperation(OperationLogCreateDTO logCreateDTO) {
        try {
            saveOperationLog(logCreateDTO);
        } catch (Exception e) {
            log.error("记录操作日志失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响主业务流程
        }
    }

    public Page<OperationLogDTO> getLogsByModule(String module, Pageable pageable) {
        return operationLogRepository.findByModule(module, pageable)
                .map(operationLogMapper::fromEntity);
    }

    public Page<OperationLogDTO> getLogsByOperator(String operator, Pageable pageable) {
        return operationLogRepository.findByOperator(operator, pageable)
                .map(operationLogMapper::fromEntity);
    }

    public Page<OperationLogDTO> getLogsByStatus(Integer status, Pageable pageable) {
        return operationLogRepository.findByStatus(status, pageable)
                .map(operationLogMapper::fromEntity);
    }

    public Page<OperationLogDTO> searchLogs(String keyword, Pageable pageable) {
        return operationLogRepository.search(keyword, pageable)
                .map(operationLogMapper::fromEntity);
    }
}