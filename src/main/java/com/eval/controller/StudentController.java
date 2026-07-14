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
// 学生相关控制器
@Controller
@RequestMapping("/student")
public class StudentController {

    private final SelfEvalService selfEvalService;
    private final SelfEvalMapper selfEvalMapper;
    private final BatchService batchService;
    private final IndicatorMapper indicatorMapper;
    private final IndicatorService indicatorService;
    private final TotalScoreMapper totalScoreMapper;

    public StudentController(SelfEvalService ses, SelfEvalMapper sem, BatchService bs,
                             IndicatorMapper im, IndicatorService is, TotalScoreMapper tsm) {
        this.selfEvalService = ses;
        this.selfEvalMapper = sem;
        this.batchService = bs;
        this.indicatorMapper = im;
        this.indicatorService = is;
        this.totalScoreMapper = tsm;
    }

    /** 学生仪表盘 */
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loginUser");
        List<Batch> batches = batchService.lambdaQuery().eq(Batch::getStatus, 1).orderByDesc(Batch::getId).list();
        model.addAttribute("activeBatches", batches);
        model.addAttribute("indicators", indicatorService.lambdaQuery().eq(Indicator::getStatus, 1).list());
        if (!batches.isEmpty()) {
            Batch b = batches.get(0);
            model.addAttribute("currentBatch", b);
            model.addAttribute("totalIndicators", indicatorService.lambdaQuery().eq(Indicator::getStatus, 1)
                    .in(Indicator::getEvalType, Arrays.asList(0, 3)).count());
            model.addAttribute("submittedCount", selfEvalMapper.countSubmitted(user.getId(), b.getId()));
            // 得分
            var ts = totalScoreMapper.selectRankingByBatch(b.getId());
            var myScore = ts.stream().filter(m -> m.get("student_id").equals(user.getId())).findFirst().orElse(null);
            model.addAttribute("myScore", myScore);
        }
        return "student/dashboard";
    }

    /** 自评页面 */
    @GetMapping("/self-eval")
    public String selfEval(HttpSession session, Model model, @RequestParam(required = false) Integer batchId) {
        User user = (User) session.getAttribute("loginUser");
        if (batchId == null) {
            Batch b = batchService.lambdaQuery().eq(Batch::getStatus, 1).orderByDesc(Batch::getId).last("limit 1").one();
            if (b != null) batchId = b.getId();
        }
        model.addAttribute("batchId", batchId);
        model.addAttribute("indicators", indicatorService.lambdaQuery().eq(Indicator::getStatus, 1)
                .in(Indicator::getEvalType, Arrays.asList(0, 3)).list());
        // 已有记录
        List<Map<String,Object>> existing = selfEvalMapper.selectByStudentAndBatch(user.getId(), batchId != null ? batchId : 0);
        Map<Integer, Map<String,Object>> existingMap = new HashMap<>();
        for (Map<String,Object> e : existing) existingMap.put((Integer)e.get("indicator_id"), e);
        model.addAttribute("existing", existingMap);
        model.addAttribute("batch", batchId != null ? batchService.getById(batchId) : null);
        return "student/self-eval";
    }

    /** 保存自评 */
    @PostMapping("/api/self-eval/save")
    @ResponseBody
    public Result saveSelfEval(@RequestBody List<SelfEval> list, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        for (SelfEval se : list) {
            se.setStudentId(user.getId());
            // 检查是否已有记录
            SelfEval exist = selfEvalService.lambdaQuery()
                    .eq(SelfEval::getStudentId, user.getId())
                    .eq(SelfEval::getBatchId, se.getBatchId())
                    .eq(SelfEval::getIndicatorId, se.getIndicatorId()).one();
            if (exist != null) {
                se.setId(exist.getId());
            }
            selfEvalService.saveOrUpdate(se);
        }
        return Result.success();
    }

    /** 获取德智体美劳各维度得分 */
    @GetMapping("/api/dimension-scores")
    @ResponseBody
    public Result dimensionScores(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        List<Map<String, Object>> result = new ArrayList<>();
        String[][] dims = {{"德育素质", "20"}, {"智育素质", "35"}, {"体育素质", "15"}, {"美育素质", "10"}, {"劳育素质", "10"}};
        Batch batch = batchService.lambdaQuery().eq(Batch::getStatus, 1).orderByDesc(Batch::getId).last("limit 1").one();
        if (batch == null) return Result.success(result);

        for (String[] dim : dims) {
            Map<String, Object> item = new HashMap<>();
            item.put("name", dim[0]);
            item.put("weight", dim[1]);
            BigDecimal score = calcDimScore(user.getId(), batch.getId(), dim[0]);
            item.put("score", score);
            result.add(item);
        }
        return Result.success(result);
    }

    private BigDecimal calcDimScore(Integer studentId, Integer batchId, String keyword) {
        BigDecimal selfTotal = BigDecimal.ZERO; int sc = 0;
        for (var row : selfEvalMapper.selectByStudentAndBatch(studentId, batchId)) {
            String cat = (String) row.get("category_name");
            if (cat != null && cat.contains(keyword.substring(0, 2))) {
                Object s = row.get("self_score");
                if (s != null) { selfTotal = selfTotal.add(new BigDecimal(s.toString())); sc++; }
            }
        }
        BigDecimal selfAvg = sc > 0 ? selfTotal.divide(new BigDecimal(sc), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        // 简化为只有自评分数（因为没有教师评和互评的详细维度数据），按比例缩放
        // 实际应该整合自评+教师评+互评，这里先展示自评维度数据
        return selfAvg;
    }
    public Result submitSelfEval(@RequestBody Map<String, Integer> params, HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        Integer batchId = params.get("batchId");
        List<SelfEval> list = selfEvalService.lambdaQuery()
                .eq(SelfEval::getStudentId, user.getId())
                .eq(SelfEval::getBatchId, batchId).list();
        for (SelfEval se : list) { se.setStatus(1); selfEvalService.updateById(se); }
        return Result.success();
    }
}