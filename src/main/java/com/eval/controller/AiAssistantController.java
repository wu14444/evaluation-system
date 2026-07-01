package com.eval.controller;

import com.eval.common.Result;
import com.eval.entity.*;
import com.eval.mapper.*;
import com.eval.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AiAssistantController {

    private final TotalScoreMapper totalScoreMapper;
    private final IndicatorService indicatorService;
    private final CategoryService categoryService;
    private final BatchService batchService;
    private final SelfEvalService selfEvalService;
    private final UserService userService;
    private final CompetitionService competitionService;
    private final RewardPunishService rewardPunishService;
    private final AuditLogService auditLogService;
    private final HttpServletRequest request;

    // Spring AI ChatClient — 负责与 DeepSeek 大模型通信
    private final ChatClient chatClient;

    // AI 数据库工具 — 注册为 tool 让模型自主调用
    private final AiTools aiTools;

    // 系统提示词：限定 AI 的回答范围为本系统相关
    private static final String SYSTEM_PROMPT = """
            你是"学生综合素质评价系统"的智能助手。你的职责是：

            【你能做的】
            - 解答德智体美劳各维度的评分标准和指标含义
            - 解释加分规则、竞赛证书管理、申诉流程
            - 引导学生完成自评、互评、教师打分等操作
            - 解释排名规则、综合分计算方式
            - 回答系统功能使用问题
            - 你可以调用数据库工具查询实时数据，包括学生信息、得分、排名、竞赛标准等
            - 当用户问某个学生的具体情况时，应该主动查询数据库获取真实数据

            【评分规则概要】
            - 六大维度：德育20% + 智育35% + 体育15% + 美育10% + 劳育10% + 奖惩10% = 100%
            - 每维度得分 = 自评30% + 教师评50% + 互评20%
            - 综合总分 = 各维度得分 × 权重 + 奖惩加分

            【回答风格】
            - 简洁友好，口语化
            - 涉及操作步骤时用数字列出
            - 不确定的问题诚实说不知道，引导联系管理员
            """;

    public AiAssistantController(TotalScoreMapper tsm, IndicatorService is,
                                  CategoryService cs, BatchService bs, SelfEvalService ses,
                                  UserService us, CompetitionService comps, RewardPunishService rps,
                                  AuditLogService als, HttpServletRequest req,
                                  ChatClient.Builder builder, AiTools aiTools) {
        this.totalScoreMapper = tsm;
        this.indicatorService = is;
        this.categoryService = cs;
        this.batchService = bs;
        this.selfEvalService = ses;
        this.userService = us;
        this.competitionService = comps;
        this.rewardPunishService = rps;
        this.auditLogService = als;
        this.request = req;
        this.aiTools = aiTools;

        // 配置对话记忆：滑动窗口，保留最近 20 条消息
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();

        // 构建 ChatClient：配置系统提示词、记忆拦截器、数据库工具
        // .defaultTools(aiTools) 会把 AiTools 中所有 @Tool 方法注册给大模型
        // 当用户问到相关问题时，模型会自主决定调用哪个工具查询数据库
        this.chatClient = builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(aiTools)
                .build();
    }

    @GetMapping("/ai-assistant")
    public String aiAssistant(HttpSession session, Model model) {
        model.addAttribute("user", session.getAttribute("loginUser"));
        return "ai-assistant";
    }

    /** AI搜索学生全部信息 */
    @GetMapping("/api/ai/search-student")
    @ResponseBody
    public Result searchStudent(@RequestParam String keyword) {
        var users = userService.lambdaQuery()
            .and(w -> w.like(User::getName, keyword).or().like(User::getStudentNo, keyword))
            .eq(User::getRole, 0).list();
        if (users.isEmpty()) return Result.error("没有找到该学生，请输入姓名或学号");
        User stu = users.get(0);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("姓名", stu.getName());
        info.put("学号", stu.getStudentNo());
        List<Batch> batches = batchService.lambdaQuery().orderByDesc(Batch::getId).list();
        if (!batches.isEmpty()) {
            var scores = totalScoreMapper.selectRankingByBatch(batches.get(0).getId());
            var my = scores.stream().filter(s -> stu.getId().equals(s.get("student_id"))).findFirst().orElse(null);
            if (my != null) {
                info.put("综合总分", my.get("final_score"));
                info.put("当前排名", "第" + my.get("ranking") + "名");
                info.put("自评总分", my.get("self_total"));
                info.put("奖惩得分", my.get("bonus_total"));
            } else { info.put("得分状态", "暂未计算，请在分数汇总页点击计算"); }
        }
        var rewards = rewardPunishService.lambdaQuery().eq(RewardPunish::getStudentId, stu.getId()).eq(RewardPunish::getStatus, 1).list();
        List<String> certList = new ArrayList<>();
        for (RewardPunish r : rewards) certList.add(r.getTitle() + "(" + r.getScoreChange() + "分)");
        info.put("获得证书/竞赛", certList.isEmpty() ? "暂无" : String.join("、", certList));
        info.put("评分权重规则", "德育20% + 智育35% + 体育15% + 美育10% + 劳育10% + 奖惩10% = 100%");
        info.put("评分方式", "每维度 = 自评30% + 教师评50% + 互评20%");
        return Result.success(info);
    }

    /** AI智能分析API (当前用户) */
    @PostMapping("/api/ai/analyze")
    @ResponseBody
    public Result analyze(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        Map<String, Object> result = new HashMap<>();
        List<Batch> batches = batchService.lambdaQuery().orderByDesc(Batch::getId).list();
        if (batches.isEmpty()) { result.put("message", "暂无评价数据"); return Result.success(result); }
        Batch latestBatch = batches.get(0);
        List<Map<String, Object>> ranking = totalScoreMapper.selectRankingByBatch(latestBatch.getId());
        Map<String, Object> myScore = ranking.stream()
                .filter(r -> r.get("student_id") != null && r.get("student_id").equals(user.getId()))
                .findFirst().orElse(null);
        if (myScore == null) { result.put("message", "您暂无评价得分，请先完成评价"); return Result.success(result); }
        result.put("batchName", latestBatch.getBatchName());
        result.put("finalScore", myScore.get("final_score"));
        result.put("ranking", myScore.get("ranking"));
        result.put("selfTotal", myScore.get("self_total"));
        result.put("teacherTotal", myScore.get("teacher_total"));
        result.put("peerTotal", myScore.get("peer_total"));

        List<Category> categories = categoryService.lambdaQuery().eq(Category::getStatus, 1).list();
        List<Map<String, Object>> dimAnalysis = new ArrayList<>();
        for (Category cat : categories) {
            Map<String, Object> dim = new HashMap<>();
            dim.put("category", cat.getCategoryName());
            List<Indicator> indicators = indicatorService.lambdaQuery().eq(Indicator::getCategoryId, cat.getId()).eq(Indicator::getStatus, 1).list();
            BigDecimal dimTotal = BigDecimal.ZERO;
            List<Integer> indIds = indicators.stream().map(Indicator::getId).toList();
            List<SelfEval> selfEvals = selfEvalService.lambdaQuery()
                    .eq(SelfEval::getStudentId, user.getId()).eq(SelfEval::getBatchId, latestBatch.getId())
                    .in(SelfEval::getIndicatorId, indIds).list();
            for (SelfEval se : selfEvals) dimTotal = dimTotal.add(se.getSelfScore() != null ? se.getSelfScore() : BigDecimal.ZERO);
            dim.put("score", dimTotal);
            dim.put("percent", indicators.size() > 0 ? dimTotal.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(indicators.size() * 100), 1, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO);
            dimAnalysis.add(dim);
        }
        dimAnalysis.sort((a, b) -> ((Comparable) a.get("percent")).compareTo(b.get("percent")));
        result.put("dimAnalysis", dimAnalysis);
        // 前2个最低的是待提升，后2个最高的是优势
        List<Map<String, Object>> weaknesses = new ArrayList<>();
        List<Map<String, Object>> strengths = new ArrayList<>();
        for (int i = 0; i < Math.min(2, dimAnalysis.size()); i++) weaknesses.add(dimAnalysis.get(i));
        for (int i = Math.max(0, dimAnalysis.size() - 2); i < dimAnalysis.size(); i++) strengths.add(dimAnalysis.get(i));
        result.put("weaknesses", weaknesses);
        result.put("strengths", strengths);
        List<String> suggestions = new ArrayList<>();
        for (Map<String, Object> w : dimAnalysis.subList(0, Math.min(2, dimAnalysis.size()))) {
            String cat = (String) w.get("category");
            if (cat.contains("德")) suggestions.add("建议多参加志愿活动和党团活动");
            if (cat.contains("智")) suggestions.add("建议加强专业学习，积极参与学科竞赛");
            if (cat.contains("体")) suggestions.add("建议坚持每日锻炼，参加体育赛事");
            if (cat.contains("美")) suggestions.add("建议选修艺术类课程，参与校园文化活动");
            if (cat.contains("劳")) suggestions.add("建议参加社会实践和勤工助学活动");
        }
        result.put("suggestions", suggestions);
        result.put("overallComment", "分析完成，请查看各维度得分情况");
        return Result.success(result);
    }

    /** AI问答 - 智能回复 + 记录反馈 */
    @PostMapping("/api/ai/chat")
    @ResponseBody
    public Result chat(@RequestBody Map<String, String> params, HttpSession session) {
        String q = params.getOrDefault("question", "").trim();
        if (q.isEmpty()) return Result.error("请输入问题");
        User loginUser = (User) session.getAttribute("loginUser");
        String a;

        // 尝试搜索学生
        var users = userService.lambdaQuery()
            .and(w -> w.like(User::getName, q).or().like(User::getStudentNo, q))
            .eq(User::getRole, 0).list();
        if (!users.isEmpty()) {
            User stu = users.get(0);
            StringBuilder sb = new StringBuilder();
            sb.append("【学生信息】\n姓名：" + stu.getName() + "\n学号：" + stu.getStudentNo() + "\n");
            List<Batch> batches = batchService.lambdaQuery().orderByDesc(Batch::getId).list();
            if (!batches.isEmpty()) {
                var scores = totalScoreMapper.selectRankingByBatch(batches.get(0).getId());
                var my = scores.stream().filter(s -> stu.getId().equals(s.get("student_id"))).findFirst().orElse(null);
                if (my != null) {
                    sb.append("总分：" + my.get("final_score") + "分\n排名：第" + my.get("ranking") + "名\n");
                    sb.append("自评总分：" + my.get("self_total") + "\n奖惩得分：" + my.get("bonus_total") + "\n");
                } else sb.append("得分：暂未计算\n");
            }
            var rewards = rewardPunishService.lambdaQuery().eq(RewardPunish::getStudentId, stu.getId()).eq(RewardPunish::getStatus, 1).list();
            if (!rewards.isEmpty()) {
                sb.append("已获证书：").append(rewards.stream().map(RewardPunish::getTitle).collect(Collectors.joining("、"))).append("\n");
            }
            sb.append("\n权重计算：德20%+智35%+体15%+美10%+劳10%+奖惩10%=100%");
            a = sb.toString();
        }
        else if (q.contains("得分") || q.contains("成绩") || q.contains("我的")) {
            // 查当前用户得分
            if (loginUser != null && loginUser.getRole() == 0) {
                return chat(Map.of("question", loginUser.getName()), session);
            }
            a = "请输入学生姓名或学号来查询具体得分信息。";
        }
        else if (q.contains("排名")) a = "排名按综合得分降序排列，同分按德育得分排序。教师和管理员在分数汇总页面可查看和导出完整排名。";
        else if (q.contains("加分") || q.contains("竞赛") || q.contains("证书")) a = "竞赛证书在「竞赛证书管理」中维护，学生自评时可勾选已获证书（需上传证明），教师审核通过后自动加分。国家级一等奖最高可加20分。";
        else if (q.contains("德育") || q.contains("志愿")) a = "德育占20%：志愿服务时长30分、政治思想表现25分、道德品质与诚信25分、遵纪守法20分。志愿服务以志愿汇记录为准。";
        else if (q.contains("智育") || q.contains("GPA")) a = "智育占35%：学业成绩GPA×10(50分)、学科竞赛(20分)、论文专利(15分)、外语计算机(10分)、科研项目(5分)。";
        else if (q.contains("体育") || q.contains("体测")) a = "体育占15%：体育课成绩30分、体质健康测试25分、日常锻炼打卡25分、体育竞赛20分。";
        else if (q.contains("美育") || q.contains("艺术")) a = "美育占10%：艺术鉴赏与课程学习35分、文化艺术活动参与35分、艺术创作与获奖30分。";
        else if (q.contains("劳育") || q.contains("劳动")) a = "劳育占10%：劳动实践参与35分、志愿服务与社会工作35分、宿舍内务与生活技能30分。";
        else if (q.contains("作弊") || q.contains("违纪") || q.contains("惩罚")) a = "考试作弊扣40分并取消评优资格，学术不端扣30分，违纪处分扣10-20分，旷课每超1次扣2分，宿舍违规每次扣5分。所有惩罚需辅导员审核确认。";
        else a = "您可以：\n1. 输入学生姓名或学号查询完整信息\n2. 问我关于德/智/体/美/劳各维度的评分标准\n3. 问我关于加分、惩罚的规则\n4. 使用「AI分析」功能查看您的维度得分率雷达图";

        // 记录到审计日志
        AuditLog log = new AuditLog();
        log.setUserId(loginUser != null ? loginUser.getId() : 0);
        log.setActionType("AI问答");
        log.setActionDetail("问：" + (q.length() > 80 ? q.substring(0, 80) + "..." : q) + " | 答：" + (a.length() > 120 ? a.substring(0, 120) + "..." : a));
        log.setIpAddress(request.getRemoteAddr());
        auditLogService.save(log);

        return Result.success(a);
    }

    /** 管理员查看AI互动记录 */
    @GetMapping("/admin/api/ai-logs")
    @ResponseBody
    public Result aiLogs() {
        var logs = auditLogService.lambdaQuery()
            .eq(AuditLog::getActionType, "AI问答")
            .orderByDesc(AuditLog::getCreateTime)
            .last("limit 50").list();
        return Result.success(logs);
    }

    /**
     * 流式 AI 对话接口（SSE）
     * 使用 Spring AI ChatClient 连接 DeepSeek，逐字返回
     * GET /api/ai/stream?message=你好&sessionId=abc
     */
    @GetMapping(value = "/api/ai/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter stream(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String sessionId) {

        SseEmitter emitter = new SseEmitter(180_000L);

        chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .content()
                .subscribe(
                        token -> {
                            try {
                                emitter.send(token);
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        },
                        emitter::completeWithError,
                        emitter::complete
                );
        return emitter;
    }
}
