package com.eval.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.eval.common.Result;
import com.eval.entity.*;
import com.eval.mapper.*;
import com.eval.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    private final TeacherEvalService teacherEvalService;
    private final TeacherEvalMapper teacherEvalMapper;
    private final BatchService batchService;
    private final IndicatorService indicatorService;
    private final TotalScoreMapper totalScoreMapper;
    private final EvalClassService evalClassService;
    private final UserService userService;
    private final PeerEvalService peerEvalService;

    private final AuditLogService auditLogService;
    private final HttpServletRequest request;
    private final RewardPunishService rewardPunishService;

    public TeacherController(TeacherEvalService tes, TeacherEvalMapper tem,
                             BatchService bs, IndicatorService is, TotalScoreMapper tsm,
                             EvalClassService ecs, UserService us, PeerEvalService pes,
                             AuditLogService als, HttpServletRequest req,
                             RewardPunishService rps) {
        this.teacherEvalService = tes;
        this.teacherEvalMapper = tem;
        this.batchService = bs;
        this.indicatorService = is;
        this.totalScoreMapper = tsm;
        this.evalClassService = ecs;
        this.userService = us;
        this.peerEvalService = pes;
        this.auditLogService = als;
        this.request = req;
        this.rewardPunishService = rps;
    }

    /** 互评完成度监控API */
    @GetMapping("/api/peer-monitor")
    @ResponseBody
    public Result peerMonitor(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        var classes = evalClassService.lambdaQuery().eq(EvalClass::getAdvisorId, user.getId()).list();
        for (EvalClass cls : classes) {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("classId", cls.getId());
            item.put("className", cls.getClassName());
            List<String> undoneNames = new java.util.ArrayList<>();
            long total = userService.lambdaQuery().eq(User::getClassId, cls.getId()).eq(User::getRole, 0).count();
            item.put("total", total);
            long done = 0;
            var students = userService.lambdaQuery().eq(User::getClassId, cls.getId()).eq(User::getRole, 0).list();
            for (User stu : students) {
                long cnt = peerEvalService.lambdaQuery().eq(PeerEval::getReviewerId, stu.getId()).count();
                if (cnt > 0) done++;
                else undoneNames.add(stu.getName());
            }
            item.put("done", done);
            item.put("undone", undoneNames);
            result.add(item);
        }
        return Result.success(result);
    }

    /** 催交互评提醒 */
    @PostMapping("/api/remind-peer")
    @ResponseBody
    public Result remindPeer(@RequestBody Map<String, Integer> params, HttpSession session) {
        Integer classId = params.get("classId");
        if (classId == null) return Result.error("请指定班级");
        User teacher = (User) session.getAttribute("loginUser");
        int count = 0;
        var students = userService.lambdaQuery().eq(User::getClassId, classId).eq(User::getRole, 0).list();
        for (User stu : students) {
            long cnt = peerEvalService.lambdaQuery().eq(PeerEval::getReviewerId, stu.getId()).count();
            if (cnt == 0) {
                // 记录提醒到审计日志
                AuditLog log = new AuditLog();
                log.setUserId(stu.getId());
                log.setActionType("互评催交");
                log.setActionDetail("教师" + teacher.getName() + "催促完成班级互评");
                log.setIpAddress(request.getRemoteAddr());
                auditLogService.save(log);
                count++;
            }
        }
        return Result.success("已向" + count + "名未完成互评的学生发送催交通知！学生可在互评页面查看");
    }

    /** 学生成长档案页面 */
    @GetMapping("/growth")
    public String growth(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("myStudents", teacherEvalMapper.selectMyStudents(user.getId()));
        return "teacher/growth";
    }

    /** 学生成长档案数据 */
    @GetMapping("/api/growth-data")
    @ResponseBody
    public Result growthData(@RequestParam Integer studentId) {
        User stu = userService.getById(studentId);
        if (stu == null) return Result.error("学生不存在");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", stu.getName());
        result.put("studentNo", stu.getStudentNo());
        // 查所有批次的得分
        List<Batch> batches = batchService.lambdaQuery().orderByAsc(Batch::getId).list();
        List<Map<String, Object>> history = new ArrayList<>();
        for (Batch batch : batches) {
            TotalScore ts = totalScoreMapper.selectList(new LambdaQueryWrapper<TotalScore>()
                .eq(TotalScore::getStudentId, studentId).eq(TotalScore::getBatchId, batch.getId())
                .last("limit 1")).stream().findFirst().orElse(null);
            if (ts != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("batchName", batch.getBatchName());
                item.put("finalScore", ts.getFinalScore());
                item.put("ranking", ts.getRanking());
                item.put("selfTotal", ts.getSelfTotal());
                item.put("teacherTotal", ts.getTeacherTotal());
                item.put("peerTotal", ts.getPeerTotal());
                item.put("bonusTotal", ts.getBonusTotal());
                history.add(item);
            }
        }
        result.put("history", history);
        // 最新批次的奖惩详情
        if (!batches.isEmpty()) {
            var rewards = rewardPunishService.lambdaQuery()
                .eq(RewardPunish::getStudentId, studentId)
                .eq(RewardPunish::getStatus, 1).list();
            result.put("rewards", rewards.stream().map(r -> {
                Map<String, Object> m = new HashMap<>();
                m.put("title", r.getTitle() != null ? r.getTitle() : r.getCategory());
                m.put("score", r.getScoreChange());
                m.put("type", r.getType() == 0 ? "奖励" : "惩罚");
                m.put("category", r.getCategory());
                return m;
            }).collect(Collectors.toList()));
        }
        return Result.success(result);
    }

    /** AI生成评语 */
    @PostMapping("/api/generate-comment")
    @ResponseBody
    public Result generateComment(@RequestBody Map<String, Integer> params) {
        Integer studentId = params.get("studentId");
        User stu = userService.getById(studentId);
        if (stu == null) return Result.error("学生不存在");
        // 查最新批次得分
        Batch batch = batchService.lambdaQuery().orderByDesc(Batch::getId).last("limit 1").one();
        StringBuilder comment = new StringBuilder();
        comment.append(stu.getName()).append("同学在本学期综合素质评价中");
        if (batch != null) {
            TotalScore ts = totalScoreMapper.selectList(new LambdaQueryWrapper<TotalScore>()
                .eq(TotalScore::getStudentId, studentId).eq(TotalScore::getBatchId, batch.getId())
                .last("limit 1")).stream().findFirst().orElse(null);
            if (ts != null && ts.getFinalScore() != null) {
                BigDecimal score = ts.getFinalScore();
                if (score.compareTo(new BigDecimal("90")) >= 0) comment.append("表现优秀，综合得分").append(score).append("分，排名第").append(ts.getRanking()).append("名。各维度均衡发展，建议继续保持并发挥榜样作用。");
                else if (score.compareTo(new BigDecimal("75")) >= 0) comment.append("表现良好，综合得分").append(score).append("分，排名第").append(ts.getRanking()).append("名。部分维度仍有提升空间，建议重点提升薄弱环节。");
                else if (score.compareTo(new BigDecimal("60")) >= 0) comment.append("表现一般，综合得分").append(score).append("分。需要重点关注薄弱维度，制定针对性提升计划。");
                else comment.append("得分偏低，需引起重视。建议与辅导员沟通，制定全面提升方案。");
            } else {
                comment.append("尚未完成全部评价，请尽快完成自评和互评。");
            }
        }
        // 加入奖惩信息
        var rewards = rewardPunishService.lambdaQuery().eq(RewardPunish::getStudentId, studentId).eq(RewardPunish::getStatus, 1).list();
        if (!rewards.isEmpty()) {
            comment.append(" 该生获得：");
            for (RewardPunish r : rewards) {
                comment.append(r.getTitle()).append("(").append(r.getScoreChange()).append("分)、");
            }
            comment.setLength(comment.length() - 1);
        }
        return Result.success(comment.toString());
    }
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        List<Batch> batches = batchService.lambdaQuery().eq(Batch::getStatus, 1).orderByDesc(Batch::getId).list();
        model.addAttribute("activeBatches", batches);
        model.addAttribute("myStudents", teacherEvalMapper.selectMyStudents(user.getId()));
        model.addAttribute("indicators", indicatorService.lambdaQuery().eq(Indicator::getStatus, 1)
                .in(Indicator::getEvalType, Arrays.asList(1, 3)).list());
        if (!batches.isEmpty()) {
            model.addAttribute("currentBatch", batches.get(0));
        }
        return "teacher/dashboard";
    }

    /** 打分页面 */
    @GetMapping("/scoring")
    public String scoring(HttpSession session, Model model, @RequestParam(required = false) Integer batchId,
                          @RequestParam(required = false) Integer studentId) {
        User user = (User) session.getAttribute("loginUser");
        if (batchId == null) {
            Batch b = batchService.lambdaQuery().eq(Batch::getStatus, 1).orderByDesc(Batch::getId).last("limit 1").one();
            if (b != null) batchId = b.getId();
        }
        model.addAttribute("batchId", batchId);
        model.addAttribute("batch", batchId != null ? batchService.getById(batchId) : null);
        model.addAttribute("students", teacherEvalMapper.selectMyStudents(user.getId()));
        model.addAttribute("indicators", indicatorService.lambdaQuery().eq(Indicator::getStatus, 1)
                .in(Indicator::getEvalType, Arrays.asList(1, 3)).list());

        if (studentId != null) {
            model.addAttribute("selectedStudent", studentId);
            List<Map<String,Object>> existing = teacherEvalMapper.selectByTeacherAndBatch(user.getId(), batchId);
            // Filter for this student
            existing = existing.stream().filter(e -> e.get("student_id").equals(studentId)).collect(Collectors.toList());
            Map<Integer, Map<String,Object>> em = new HashMap<>();
            for (Map<String,Object> e : existing) em.put((Integer)e.get("indicator_id"), e);
            model.addAttribute("existing", em);
        }
        return "teacher/scoring";
    }

    /** 保存打分 */
    @PostMapping("/api/teacher-eval/save")
    @ResponseBody
    public Result saveScore(@RequestBody List<TeacherEval> list, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        for (TeacherEval te : list) {
            te.setTeacherId(user.getId());
            TeacherEval exist = teacherEvalService.lambdaQuery()
                    .eq(TeacherEval::getTeacherId, user.getId())
                    .eq(TeacherEval::getStudentId, te.getStudentId())
                    .eq(TeacherEval::getBatchId, te.getBatchId())
                    .eq(TeacherEval::getIndicatorId, te.getIndicatorId()).one();
            if (exist != null) { te.setId(exist.getId()); }
            te.setStatus(1);
            teacherEvalService.saveOrUpdate(te);
        }
        return Result.success();
    }
}