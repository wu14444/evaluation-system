package com.eval.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eval.entity.Competition;
import com.eval.mapper.CompetitionMapper;
import com.eval.service.CompetitionService;
import org.springframework.stereotype.Service;
@Service
public class CompetitionServiceImpl extends ServiceImpl<CompetitionMapper, Competition> implements CompetitionService {}