package com.eval.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eval.entity.EvalClass;
import com.eval.mapper.EvalClassMapper;
import com.eval.service.EvalClassService;
import org.springframework.stereotype.Service;

@Service
public class EvalClassServiceImpl extends ServiceImpl<EvalClassMapper, EvalClass> implements EvalClassService {}
