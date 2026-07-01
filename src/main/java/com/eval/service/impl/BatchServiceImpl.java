package com.eval.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eval.entity.Batch;
import com.eval.mapper.BatchMapper;
import com.eval.service.BatchService;
import org.springframework.stereotype.Service;

@Service
public class BatchServiceImpl extends ServiceImpl<BatchMapper, Batch> implements BatchService {}
