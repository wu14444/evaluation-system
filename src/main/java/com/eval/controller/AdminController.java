package com.eval.controller;

import com.eval.common.Result;
import com.eval.entity.*;
import com.eval.mapper.*;
import com.eval.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final EvalClassService evalClassService;
    private final EvalClassMapper evalClassMapper;
    private final BatchService batchService;
    private final CategoryService categoryService;
    private final IndicatorService indicatorService;
    private final IndicatorMapper indicatorMapper;
    private final TotalScoreMapper totalScoreMapper;

    private final PeerEvalService peerEvalService;
    private final RewardPunishService rewardPunishService;

    public AdminController(UserService us, UserMapper um, EvalClassService ecs, EvalClassMapper ecm,
                           BatchService bs, CategoryService cs, IndicatorService is,
                           IndicatorMapper im, TotalScoreMapper tsm,
                           PeerEvalService pes, RewardPunishService rps) {
        this.userService = us;
        this.userMapper = um;
        this.evalClassService = ecs;
        this.evalClassMapper = ecm;
        this.batchService = bs;
        this.categoryService = cs;
        this.indicatorService = is;
        this.indicatorMapper = im;
        this.totalScoreMapper = tsm;
        this.peerEvalService = pes;
        this.rewardPunishService = rps;
    }

    /** 仪表盘 */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("userCount", userService.count());
        model.addAttribute("classCount", evalClassService.count());
        model.addAttribute("indicatorCount", indicatorService.count());
        model.addAttribute("batchCount", batchService.count());
        List<Category> cats = categoryService.list();
        model.addAttribute("categories", cats);
        // 最新批次的TOP5学生
        List<Batch> batches = batchService.lambdaQuery().orderByDesc(Batch::getId).list();
        if (!batches.isEmpty()) {
            List<Map<String, Object>> ranking = totalScoreMapper.selectRankingByBatch(batches.get(0).getId());
            model.addAttribute("topList", ranking.stream().limit(5).collect(Collectors.toList()));
        } else {
            model.addAttribute("topList", Collections.emptyList());
        }
        return "admin/dashboard";
    }

    // ===== 班级管理 =====
    @GetMapping("/classes")
    public String classes(Model model) {
        model.addAttribute("list", evalClassMapper.selectClassList());
        model.addAttribute("advisors", userService.lambdaQuery().eq(User::getRole, 1).list());
        return "admin/classes";
    }

    @GetMapping("/api/class/list")
    @ResponseBody
    public Result classList() {
        return Result.success(evalClassMapper.selectClassList());
    }

    @PostMapping("/api/class/save")
    @ResponseBody
    public Result saveClass(@RequestBody EvalClass ec) {
        evalClassService.saveOrUpdate(ec);
        return Result.success();
    }

    @DeleteMapping("/api/class/delete/{id}")
    @ResponseBody
    public Result deleteClass(@PathVariable Integer id) {
        evalClassService.removeById(id);
        return Result.success();
    }

    // ===== 用户管理 =====
    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("list", userMapper.selectUserList());
        model.addAttribute("classes", evalClassService.list());
        return "admin/users";
    }

    @GetMapping("/api/peer-overview")
    @ResponseBody
    public Result peerOverview() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<EvalClass> classes = evalClassService.list();
        for (EvalClass cls : classes) {
            Map<String, Object> item = new HashMap<>();
            item.put("className", cls.getClassName());
            long total = userService.lambdaQuery().eq(User::getClassId, cls.getId()).eq(User::getRole, 0).count();
            item.put("total", total);
            long done = 0;
            var students = userService.lambdaQuery().eq(User::getClassId, cls.getId()).eq(User::getRole, 0).list();
            for (User stu : students) {
                long cnt = peerEvalService.lambdaQuery().eq(PeerEval::getReviewerId, stu.getId()).count();
                if (cnt > 0) done++;
            }
            item.put("done", done);
            result.add(item);
        }
        return Result.success(result);
    }

    @GetMapping("/api/user/list")
    @ResponseBody
    public Result userList() {
        return Result.success(userMapper.selectUserList());
    }

    @PostMapping("/api/user/save")
    @ResponseBody
    public Result saveUser(@RequestBody User user) {
        if (user.getId() == null) user.setPassword("123456");
        else { User old = userService.getById(user.getId()); if (old != null && user.getPassword() == null) user.setPassword(old.getPassword()); }
        userService.saveOrUpdate(user);
        return Result.success();
    }

    @DeleteMapping("/api/user/delete/{id}")
    @ResponseBody
    public Result deleteUser(@PathVariable Integer id) {
        userService.removeById(id);
        return Result.success();
    }

    // ===== 批次管理 =====
    @GetMapping("/batches")
    public String batches(Model model) {
        model.addAttribute("list", batchService.list());
        return "admin/batches";
    }

    @PostMapping("/api/batch/save")
    @ResponseBody
    public Result saveBatch(@RequestBody Batch batch) {
        batchService.saveOrUpdate(batch);
        return Result.success();
    }

    @DeleteMapping("/api/batch/delete/{id}")
    @ResponseBody
    public Result deleteBatch(@PathVariable Integer id) {
        batchService.removeById(id);
        return Result.success();
    }

    // ===== 指标管理 =====
    @GetMapping("/indicators")
    public String indicators(Model model) {
        model.addAttribute("list", indicatorMapper.selectIndicatorList());
        model.addAttribute("categories", categoryService.lambdaQuery().eq(Category::getStatus, 1).list());
        return "admin/indicators";
    }

    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("list", categoryService.list());
        return "admin/categories";
    }

    @PostMapping("/api/category/save")
    @ResponseBody
    public Result saveCategory(@RequestBody Category cat) {
        categoryService.saveOrUpdate(cat);
        return Result.success();
    }

    @DeleteMapping("/api/category/delete/{id}")
    @ResponseBody
    public Result deleteCategory(@PathVariable Integer id) {
        categoryService.removeById(id);
        return Result.success();
    }

    @PostMapping("/api/indicator/save")
    @ResponseBody
    public Result saveIndicator(@RequestBody Indicator ind) {
        indicatorService.saveOrUpdate(ind);
        return Result.success();
    }

    @DeleteMapping("/api/indicator/delete/{id}")
    @ResponseBody
    public Result deleteIndicator(@PathVariable Integer id) {
        indicatorService.removeById(id);
        return Result.success();
    }

    // ===== 仪表盘数据API =====
    @GetMapping("/api/dashboard")
    @ResponseBody
    public Result getDashboardData() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userService.count());
        data.put("classCount", evalClassService.count());
        data.put("indicatorCount", indicatorService.count());
        data.put("batchCount", batchService.count());

        // 角色分布（饼图）
        Map<String, Long> roleDist = new LinkedHashMap<>();
        roleDist.put("学生", userService.lambdaQuery().eq(User::getRole, 0).count());
        roleDist.put("教师", userService.lambdaQuery().eq(User::getRole, 1).count());
        roleDist.put("管理员", userService.lambdaQuery().eq(User::getRole, 2).count());
        data.put("roleDistribution", roleDist);

        // 班级平均分
        List<Batch> batches = batchService.lambdaQuery().orderByDesc(Batch::getId).list();
        if (!batches.isEmpty()) {
            data.put("classScores", totalScoreMapper.selectClassAvgByBatch(batches.get(0).getId()));
            List<Map<String,Object>> ranking = totalScoreMapper.selectRankingByBatch(batches.get(0).getId());
            data.put("topStudents", ranking.stream().limit(5).collect(Collectors.toList()));
            // 分数段分布
            Map<String, Long> scoreDist = new LinkedHashMap<>();
            scoreDist.put("90-100", ranking.stream().filter(r -> {
                Object v = r.get("final_score");
                return v != null && new BigDecimal(v.toString()).compareTo(BigDecimal.valueOf(90)) >= 0;
            }).count());
            scoreDist.put("80-89", ranking.stream().filter(r -> {
                Object v = r.get("final_score");
                return v != null && new BigDecimal(v.toString()).compareTo(BigDecimal.valueOf(80)) >= 0 && new BigDecimal(v.toString()).compareTo(BigDecimal.valueOf(90)) < 0;
            }).count());
            scoreDist.put("70-79", ranking.stream().filter(r -> {
                Object v = r.get("final_score");
                return v != null && new BigDecimal(v.toString()).compareTo(BigDecimal.valueOf(70)) >= 0 && new BigDecimal(v.toString()).compareTo(BigDecimal.valueOf(80)) < 0;
            }).count());
            scoreDist.put("60-69", ranking.stream().filter(r -> {
                Object v = r.get("final_score");
                return v != null && new BigDecimal(v.toString()).compareTo(BigDecimal.valueOf(60)) >= 0 && new BigDecimal(v.toString()).compareTo(BigDecimal.valueOf(70)) < 0;
            }).count());
            scoreDist.put("60以下", ranking.stream().filter(r -> {
                Object v = r.get("final_score");
                return v == null || new BigDecimal(v.toString()).compareTo(BigDecimal.valueOf(60)) < 0;
            }).count());
            data.put("scoreDistribution", scoreDist);
        }

        return Result.success(data);
    }
}