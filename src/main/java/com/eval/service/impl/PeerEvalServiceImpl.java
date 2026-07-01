package com.eval.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eval.entity.PeerEval;
import com.eval.mapper.PeerEvalMapper;
import com.eval.service.PeerEvalService;
import org.springframework.stereotype.Service;

@Service
public class PeerEvalServiceImpl extends ServiceImpl<PeerEvalMapper, PeerEval> implements PeerEvalService {}
