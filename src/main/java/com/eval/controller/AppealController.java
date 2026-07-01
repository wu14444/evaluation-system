package com.eval.controller;

import com.eval.common.Result;
import com.eval.entity.*;
import com.eval.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class AppealController {
    private final AuditLogService auditLogService;
    private final UserService userService;

    public AppealController(AuditLogService als, UserService us) {
        this.auditLogService = als; this.userService = us;
    }

    /** 学生申诉页面 */
    @GetMapping("/student/appeal")
    public String appealPage() { return "student/appeal"; }

    /** 提交申诉 */
    @PostMapping("/api/appeal/submit")
    @ResponseBody
    public Result submit(@RequestBody Map<String,String> params, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        AuditLog log = new AuditLog();
        log.setUserId(user.getId());
        log.setActionType("申诉");
        log.setActionDetail("学生" + user.getName() + "提交申诉：" + params.getOrDefault("reason",""));
        auditLogService.save(log);
        return Result.success("申诉已提交，请等待辅导员审核处理");
    }

    /** 管理员查看申诉 */
    @GetMapping("/admin/appeals")
    public String appeals(Model model) {
        var list = auditLogService.lambdaQuery()
            .eq(AuditLog::getActionType, "申诉")
            .orderByDesc(AuditLog::getCreateTime).list();
        model.addAttribute("appeals", list);
        return "admin/appeals";
    }

    /** 管理员回复申诉 */
    @PostMapping("/api/appeal/reply")
    @ResponseBody
    public Result reply(@RequestBody Map<String,String> params, HttpSession session) {
        User admin = (User) session.getAttribute("loginUser");
        String studentName = params.getOrDefault("studentName", "");
        AuditLog log = new AuditLog();
        log.setUserId(admin.getId());
        log.setActionType("申诉回复");
        log.setActionDetail("管理员" + admin.getName() + "回复[" + studentName + "]：" + params.getOrDefault("reply",""));
        auditLogService.save(log);
        return Result.success("回复已发送，学生" + studentName + "可查看");
    }

    /** 学生查自己的申诉和回复 */
    @GetMapping("/api/my-appeals")
    @ResponseBody
    public Result myAppeals(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        // 查自己的申诉
        var myAppeals = auditLogService.lambdaQuery()
            .eq(AuditLog::getUserId, user.getId())
            .eq(AuditLog::getActionType, "申诉")
            .orderByDesc(AuditLog::getCreateTime).list();
        // 查给自己的回复（actionDetail包含自己的名字）
        var replies = auditLogService.lambdaQuery()
            .eq(AuditLog::getActionType, "申诉回复")
            .like(AuditLog::getActionDetail, user.getName())
            .orderByDesc(AuditLog::getCreateTime).list();
        List<AuditLog> all = new ArrayList<>();
        all.addAll(myAppeals);
        all.addAll(replies);
        all.sort((a,b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        return Result.success(all);
    }
}
