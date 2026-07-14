package com.eval.controller;
import com.eval.common.Result;
import com.eval.entity.RewardPunish;
import com.eval.entity.User;
import com.eval.mapper.RewardPunishMapper;
import com.eval.service.RewardPunishService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;
// 学生互评
@Controller
public class RewardPunishController {
    private final RewardPunishService rpService;
    private final RewardPunishMapper rpMapper;
    public RewardPunishController(RewardPunishService rs, RewardPunishMapper rm) { this.rpService = rs; this.rpMapper = rm; }

    @GetMapping("/admin/rewards")
    public String adminPage(Model model) {
        model.addAttribute("list", rpMapper.selectAllWithStudent());
        return "admin/rewards";
    }

    @GetMapping("/student/rewards")
    public String studentPage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        model.addAttribute("list", rpService.lambdaQuery().eq(RewardPunish::getStudentId, user.getId()).orderByDesc(RewardPunish::getCreateTime).list());
        return "student/rewards";
    }

    @PostMapping("/api/reward-punish/save")
    @ResponseBody
    public Result save(@RequestBody RewardPunish rp, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (rp.getType() == 1 && rp.getScoreChange().compareTo(BigDecimal.ZERO) > 0) {
            rp.setScoreChange(rp.getScoreChange().negate());
        }
        if (rp.getId() != null) {
            rpService.updateById(rp);
        } else {
            rp.setStatus(0);
            rpService.save(rp);
        }
        return Result.success();
    }

    @DeleteMapping("/api/reward-punish/delete/{id}")
    @ResponseBody
    public Result delete(@PathVariable Integer id) {
        rpService.removeById(id);
        return Result.success();
    }

    @PostMapping("/api/reward-punish/audit")
    @ResponseBody
    public Result audit(@RequestBody Map<String,Integer> params, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        RewardPunish rp = rpService.getById(params.get("id"));
        if (rp != null) { rp.setStatus(params.get("status")); rp.setAuditorId(user.getId()); rpService.updateById(rp); }
        return Result.success();
    }
}