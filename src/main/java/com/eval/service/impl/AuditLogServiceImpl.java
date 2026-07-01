package com.eval.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eval.entity.AuditLog;
import com.eval.mapper.AuditLogMapper;
import com.eval.service.AuditLogService;
import org.springframework.stereotype.Service;

@Service
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements AuditLogService {}
