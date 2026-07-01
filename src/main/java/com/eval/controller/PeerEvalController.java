package com.eval.controller;

import com.eval.common.Result;
import com.eval.entity.*;
import com.eval.mapper.*;
import com.eval.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 班级互评控制器
 */
@Controller
@RequestMapping("/student")
public class PeerEvalController {

    private final PeerEvalService peerEvalService;
    private final PeerEvalMapper peerEvalMapper;
    private final BatchService batchService;
    private final IndicatorService indicatorService;
    private final UserService userService;
    private final EvalClassService evalClassService;
    private final AuditLogService auditLogService;

    public PeerEvalController(PeerEvalService pes, PeerEvalMapper pem, BatchService bs,
                               IndicatorService is, UserService us, EvalClassService ecs,
                               AuditLogService als) {
        this.peerEvalService = pes;
        this.peerEvalMapper = pem;
        this.batchService = bs;
        this.indicatorService = is;
        this.userService = us;
        this.evalClassService = ecs;
        this.auditLogService = als;
    }

    /** 互评页面 */
    @GetMapping("/peer-eval")
    public String peerEval(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) return "redirect:/login";

        // 获取当前批次
        Batch batch = batchService.lambdaQuery().eq(Batch::getStatus, 1).orderByDesc(Batch::getId).last("limit 1").one();
        if (batch == null) {
            model.addAttribute("targets", Collections.emptyList());
            return "student/peer-eval";
        }

        model.addAttribute("batchId", batch.getId());
        model.addAttribute("batch", batch);

        // 获取同班同学（排除自己）
        List<Map<String, Object>> classmates = userService.lambdaQuery()
                .eq(User::getClassId, user.getClassId())
                .eq(User::getRole, 0)
                .ne(User::getId, user.getId())
                .list()
                .stream()
                .map(u -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", u.getId());
                    m.put("name", u.getName());
                    return m;
                })
                .collect(Collectors.toList());

        model.addAttribute("targets", classmates);

        // 互评指标
        List<Indicator> peerIndicators = indicatorService.lambdaQuery()
                .eq(Indicator::getStatus, 1)
                .in(Indicator::getEvalType, Arrays.asList(2, 3))
                .list();
        model.addAttribute("peerIndicators", peerIndicators);

        return "student/peer-eval";
    }

    /** 查看催交通知 */
    @GetMapping("/api/my-reminders")
    @ResponseBody
    public Result myReminders(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) return Result.success(java.util.Collections.emptyList());
        var list = auditLogService.lambdaQuery()
            .eq(AuditLog::getUserId, user.getId())
            .eq(AuditLog::getActionType, "互评催交")
            .orderByDesc(AuditLog::getCreateTime)
            .list();
        return Result.success(list);
    }

    /** 保存互评 */
    @PostMapping("/api/peer-eval/save")
    @ResponseBody
    public Result savePeerEval(@RequestBody List<PeerEval> list, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        for (PeerEval pe : list) {
            pe.setReviewerId(user.getId());
            peerEvalService.save(pe);
        }
        return Result.success();
    }
}
