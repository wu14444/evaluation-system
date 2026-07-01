package com.eval.controller;

import com.eval.common.Result;
import com.eval.entity.*;
import com.eval.mapper.TotalScoreMapper;
import com.eval.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据大屏控制器（创新功能）
 */
@Controller
public class DataScreenController {

    private final BatchService batchService;
    private final UserService userService;
    private final EvalClassService evalClassService;
    private final TotalScoreMapper totalScoreMapper;
    private final IndicatorService indicatorService;
    private final CategoryService categoryService;

    public DataScreenController(BatchService bs, UserService us, EvalClassService ecs,
                                 TotalScoreMapper tsm, IndicatorService is, CategoryService cs) {
        this.batchService = bs;
        this.userService = us;
        this.evalClassService = ecs;
        this.totalScoreMapper = tsm;
        this.indicatorService = is;
        this.categoryService = cs;
    }

    /** 数据大屏页面 */
    @GetMapping("/data-screen")
    public String dataScreen(Model model) {
        model.addAttribute("batches", batchService.lambdaQuery().orderByDesc(Batch::getId).list());
        return "data-screen";
    }

    /** 大屏数据API */
    @GetMapping("/api/data-screen")
    @ResponseBody
    public Result getScreenData(@RequestParam(required = false) Integer batchId) {
        if (batchId == null) {
            Batch b = batchService.lambdaQuery().orderByDesc(Batch::getId).last("limit 1").one();
            if (b != null) batchId = b.getId();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("totalStudents", userService.lambdaQuery().eq(User::getRole, 0).count());
        data.put("totalClasses", evalClassService.count());
        data.put("totalIndicators", indicatorService.count());
        data.put("completedBatches", batchService.lambdaQuery().eq(Batch::getStatus, 2).count());

        if (batchId != null) {
            List<Map<String, Object>> ranking = totalScoreMapper.selectRankingByBatch(batchId);
            data.put("rankCount", ranking.size());
            // 最高分最低分
            if (!ranking.isEmpty()) {
                data.put("highestScore", ranking.get(0).get("final_score"));
                data.put("lowestScore", ranking.get(ranking.size()-1).get("final_score"));
                // 平均分
                double avg = ranking.stream().mapToDouble(r -> {
                    Object v = r.get("final_score");
                    return v != null ? Double.parseDouble(v.toString()) : 0;
                }).average().orElse(0);
                data.put("averageScore", String.format("%.2f", avg));
            }
            // 班级平均分
            data.put("classScores", totalScoreMapper.selectClassAvgByBatch(batchId));
            // 分数段分布
            Map<String, Integer> distribution = new LinkedHashMap<>();
            distribution.put("90-100", 0); distribution.put("80-89", 0); distribution.put("70-79", 0);
            distribution.put("60-69", 0); distribution.put("60以下", 0);
            for (Map<String,Object> r : ranking) {
                double score = Double.parseDouble(String.valueOf(r.getOrDefault("final_score","0")));
                if (score >= 90) distribution.put("90-100", distribution.get("90-100")+1);
                else if (score >= 80) distribution.put("80-89", distribution.get("80-89")+1);
                else if (score >= 70) distribution.put("70-79", distribution.get("70-79")+1);
                else if (score >= 60) distribution.put("60-69", distribution.get("60-69")+1);
                else distribution.put("60以下", distribution.get("60以下")+1);
            }
            data.put("distribution", distribution);
            // Top 10
            data.put("top10", ranking.stream().limit(10).collect(Collectors.toList()));
        }

        return Result.success(data);
    }
}
