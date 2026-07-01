package com.eval.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eval.entity.TeacherEval;
import com.eval.mapper.TeacherEvalMapper;
import com.eval.service.TeacherEvalService;
import org.springframework.stereotype.Service;

@Service
public class TeacherEvalServiceImpl extends ServiceImpl<TeacherEvalMapper, TeacherEval> implements TeacherEvalService {}
