package com.eval.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eval.entity.Indicator;
import com.eval.mapper.IndicatorMapper;
import com.eval.service.IndicatorService;
import org.springframework.stereotype.Service;

@Service
public class IndicatorServiceImpl extends ServiceImpl<IndicatorMapper, Indicator> implements IndicatorService {}
