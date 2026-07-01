package com.eval.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eval.entity.TotalScore;
import com.eval.mapper.TotalScoreMapper;
import com.eval.service.TotalScoreService;
import org.springframework.stereotype.Service;

@Service
public class TotalScoreServiceImpl extends ServiceImpl<TotalScoreMapper, TotalScore> implements TotalScoreService {}
