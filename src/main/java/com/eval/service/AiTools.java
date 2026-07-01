package com.eval.service;

import com.eval.entity.*;
import com.eval.mapper.TotalScoreMapper;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 工具集 — 这些方法会被注册为 AI 可以调用的"工具"
 * <p>
 * 原理：Spring AI 把这些方法的定义（名称、描述、参数说明）发给 DeepSeek 模型，
 * 模型根据用户的问题，自主决定"现在需要调用哪个工具来获取数据"。
 * 工具返回数据后，模型再根据数据生成自然语言的回答。
 * <p>
 * 这相当于让 AI 自己学会"查数据库"，不需要你在代码里写 if/else 匹配关键词。
 */
@Service
public class AiTools {

    private final TotalScoreMapper totalScoreMapper;
    private final com.eval.service.UserService userService;
    private final com.eval.service.CategoryService categoryService;
    private final com.eval.service.IndicatorService indicatorService;
    private final com.eval.service.BatchService batchService;
    private final com.eval.service.CompetitionService competitionService;
    private final com.eval.service.RewardPunishService rewardPunishService;
    private final com.eval.service.SelfEvalService selfEvalService;
    private final com.eval.service.TeacherEvalService teacherEvalService;

    public AiTools(TotalScoreMapper totalScoreMapper,
                   com.eval.service.UserService userService,
                   com.eval.service.CategoryService categoryService,
                   com.eval.service.IndicatorService indicatorService,
                   com.eval.service.BatchService batchService,
                   com.eval.service.CompetitionService competitionService,
                   com.eval.service.RewardPunishService rewardPunishService,
                   com.eval.service.SelfEvalService selfEvalService,
                   com.eval.service.TeacherEvalService teacherEvalService) {
        this.totalScoreMapper = totalScoreMapper;
        this.userService = userService;
        this.categoryService = categoryService;
        this.indicatorService = indicatorService;
        this.batchService = batchService;
        this.competitionService = competitionService;
        this.rewardPunishService = rewardPunishService;
        this.selfEvalService = selfEvalService;
        this.teacherEvalService = teacherEvalService;
    }

    @Tool(description = "按姓名或学号搜索学生，返回学生基本信息（学号、姓名、班级、电话等）")
    public Map<String, Object> searchStudent(String keyword) {
        var users = userService.lambdaQuery()
                .and(w -> w.like(User::getName, keyword).or().like(User::getStudentNo, keyword))
                .eq(User::getRole, 0).list();
        if (users.isEmpty()) return Map.of("found", false, "message", "未找到该学生");
        User stu = users.get(0);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("found", true);
        info.put("id", stu.getId());
        info.put("name", stu.getName());
        info.put("studentNo", stu.getStudentNo());
        info.put("phone", stu.getPhone() != null ? stu.getPhone() : "");
        info.put("email", stu.getEmail() != null ? stu.getEmail() : "");
        return info;
    }

    @Tool(description = "查询学生的综合素质得分详情，包含各维度（德智体美劳）得分率、综合总分、排名")
    public Map<String, Object> getStudentScore(Integer studentId) {
        List<Batch> batches = batchService.lambdaQuery().orderByDesc(Batch::getId).list();
        if (batches.isEmpty()) return Map.of("found", false, "message", "暂无评价批次");

        Batch latest = batches.get(0);
        List<Map<String, Object>> ranking = totalScoreMapper.selectRankingByBatch(latest.getId());
        Map<String, Object> myScore = ranking.stream()
                .filter(r -> studentId.equals(r.get("student_id")))
                .findFirst().orElse(null);

        if (myScore == null) return Map.of("found", false, "message", "该学生暂无得分数据");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("found", true);
        result.put("batchName", latest.getBatchName());
        result.put("finalScore", myScore.get("final_score"));
        result.put("ranking", myScore.get("ranking"));
        result.put("selfTotal", myScore.get("self_total"));
        result.put("teacherTotal", myScore.get("teacher_total"));
        result.put("peerTotal", myScore.get("peer_total"));
        result.put("bonusTotal", myScore.get("bonus_total"));

        // 各维度得分率
        List<Category> categories = categoryService.lambdaQuery().eq(Category::getStatus, 1).list();
        List<Map<String, Object>> dimScores = new ArrayList<>();
        for (Category cat : categories) {
            Map<String, Object> dim = new LinkedHashMap<>();
            dim.put("category", cat.getCategoryName());
            List<Indicator> indicators = indicatorService.lambdaQuery()
                    .eq(Indicator::getCategoryId, cat.getId())
                    .eq(Indicator::getStatus, 1).list();
            List<SelfEval> selfEvals = selfEvalService.lambdaQuery()
                    .eq(SelfEval::getStudentId, studentId)
                    .eq(SelfEval::getBatchId, latest.getId())
                    .in(SelfEval::getIndicatorId, indicators.stream().map(Indicator::getId).toList())
                    .list();
            double total = selfEvals.stream()
                    .filter(s -> s.getSelfScore() != null)
                    .mapToDouble(s -> s.getSelfScore().doubleValue())
                    .sum();
            double maxPossible = indicators.size() * 100.0;
            dim.put("score", total);
            dim.put("percent", maxPossible > 0 ? Math.round(total / maxPossible * 1000) / 10.0 : 0);
            dimScores.add(dim);
        }
        result.put("dimensions", dimScores);
        return result;
    }

    @Tool(description = "获取综合素质评价排名，按批次查询，可指定返回前几名")
    public Map<String, Object> getRanking(Integer batchId, Integer topN) {
        if (batchId == null) {
            List<Batch> batches = batchService.lambdaQuery().orderByDesc(Batch::getId).list();
            if (batches.isEmpty()) return Map.of("found", false, "message", "暂无评价批次");
            batchId = batches.get(0).getId();
        }
        List<Map<String, Object>> list = totalScoreMapper.selectRankingByBatch(batchId);
        if (topN != null && topN > 0 && topN < list.size()) {
            list = list.subList(0, topN);
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> row = list.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("ranking", i + 1);
            item.put("studentName", row.get("student_name"));
            item.put("studentNo", row.get("student_no"));
            item.put("className", row.get("class_name"));
            item.put("finalScore", row.get("final_score"));
            items.add(item);
        }
        return Map.of("found", true, "batchId", batchId, "rankings", items);
    }

    @Tool(description = "查询所有评价维度大类（德育、智育、体育、美育、劳育）及其权重占比")
    public List<Map<String, Object>> getCategories() {
        List<Category> list = categoryService.lambdaQuery().eq(Category::getStatus, 1)
                .orderByAsc(Category::getSortOrder).list();
        // 权重按固定规则：德20% 智35% 体15% 美10% 劳10%
        Map<String, String> weightMap = new LinkedHashMap<>();
        weightMap.put("德育素质", "20%");
        weightMap.put("智育素质", "35%");
        weightMap.put("体育素质", "15%");
        weightMap.put("美育素质", "10%");
        weightMap.put("劳育素质", "10%");

        return list.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getCategoryName());
            m.put("weight", weightMap.getOrDefault(c.getCategoryName(), "-"));
            return m;
        }).collect(Collectors.toList());
    }

    @Tool(description = "按维度大类查询该维度下的具体指标项（指标名称、满分值、评分标准、评价方式）")
    public List<Map<String, Object>> getIndicators(Integer categoryId) {
        List<Indicator> list = indicatorService.lambdaQuery()
                .eq(Indicator::getCategoryId, categoryId)
                .eq(Indicator::getStatus, 1)
                .orderByAsc(Indicator::getSortOrder).list();
        Map<Integer, String> evalTypeMap = Map.of(0, "自评", 1, "教师评", 2, "互评", 3, "通用");
        return list.stream().map(ind -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("indicatorName", ind.getIndicatorName());
            m.put("maxScore", ind.getMaxScore());
            m.put("weight", ind.getWeight());
            m.put("scoringStandard", ind.getScoringStandard() != null ? ind.getScoringStandard() : "");
            m.put("evalType", evalTypeMap.getOrDefault(ind.getEvalType(), "通用"));
            return m;
        }).collect(Collectors.toList());
    }

    @Tool(description = "查询竞赛证书加分标准，可按级别（国家级/省级/校级）或竞赛名称筛选")
    public List<Map<String, Object>> getCompetitionBonuses(String level, String keyword) {
        var query = competitionService.lambdaQuery().eq(Competition::getStatus, 1);
        if (level != null && !level.isBlank()) {
            query.eq(Competition::getLevel, level);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.like(Competition::getName, keyword);
        }
        return query.list().stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", c.getName());
            m.put("type", c.getType() == 0 ? "竞赛" : "证书");
            m.put("level", c.getLevel());
            m.put("awardLevel", c.getAwardLevel());
            m.put("bonusScore", c.getBonusScore());
            return m;
        }).collect(Collectors.toList());
    }

    @Tool(description = "查询某个学生的奖惩记录（竞赛加分、违纪扣分等），需提供学生ID")
    public List<Map<String, Object>> getStudentRewards(Integer studentId) {
        return rewardPunishService.lambdaQuery()
                .eq(RewardPunish::getStudentId, studentId)
                .eq(RewardPunish::getStatus, 1)
                .list().stream().map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("type", r.getType() == 0 ? "奖励" : "惩罚");
                    m.put("category", r.getCategory());
                    m.put("title", r.getTitle());
                    m.put("scoreChange", r.getScoreChange());
                    m.put("description", r.getDescription() != null ? r.getDescription() : "");
                    return m;
                }).collect(Collectors.toList());
    }

    @Tool(description = "获取综合评分的完整规则说明：各维度权重、计算方式、排名规则")
    public Map<String, Object> getScoringRules() {
        Map<String, Object> rules = new LinkedHashMap<>();
        rules.put("weights", "德育20% + 智育35% + 体育15% + 美育10% + 劳育10% + 奖惩10% = 100%");
        rules.put("dimensionFormula", "每维度得分 = 自评×30% + 教师评×50% + 互评×20%");
        rules.put("finalFormula", "综合总分 = 各维度得分×权重 + 奖惩附加分");
        rules.put("rankingRule", "按综合总分降序排列，同分按德育得分排序");
        return rules;
    }

    @Tool(description = "查询所有活跃的评价批次，返回批次名称、学年学期、状态、起止时间")
    public List<Map<String, Object>> getActiveBatches() {
        return batchService.lambdaQuery()
                .orderByDesc(Batch::getId).list().stream().map(b -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", b.getId());
                    m.put("batchName", b.getBatchName());
                    m.put("academicYear", b.getAcademicYear());
                    m.put("semester", b.getSemester());
                    m.put("status", b.getStatus() == 0 ? "未开始" : b.getStatus() == 1 ? "进行中" : "已结束");
                    m.put("startTime", b.getStartTime() != null ? b.getStartTime().toString() : "");
                    m.put("endTime", b.getEndTime() != null ? b.getEndTime().toString() : "");
                    return m;
                }).collect(Collectors.toList());
    }
}
