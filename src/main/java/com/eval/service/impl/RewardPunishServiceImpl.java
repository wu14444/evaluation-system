package com.eval.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eval.entity.RewardPunish;
import com.eval.mapper.RewardPunishMapper;
import com.eval.service.RewardPunishService;
import org.springframework.stereotype.Service;
@Service
public class RewardPunishServiceImpl extends ServiceImpl<RewardPunishMapper, RewardPunish> implements RewardPunishService {}