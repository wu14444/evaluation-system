package com.eval.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eval.entity.SelfEval;
import com.eval.mapper.SelfEvalMapper;
import com.eval.service.SelfEvalService;
import org.springframework.stereotype.Service;

@Service
public class SelfEvalServiceImpl extends ServiceImpl<SelfEvalMapper, SelfEval> implements SelfEvalService {}
