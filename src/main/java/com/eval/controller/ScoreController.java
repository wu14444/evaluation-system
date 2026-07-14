package com.eval.controller;

import com.eval.common.Result;
import com.eval.entity.*;
import com.eval.mapper.SelfEvalMapper;
import com.eval.mapper.TotalScoreMapper;
import com.eval.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;
//分数管理
@Controller
public class ScoreController {

    private final BatchService batchService;
    private final TotalScoreMapper totalScoreMapper;
    private final SelfEvalService selfEvalService;
    private final SelfEvalMapper selfEvalMapper;
    private final TeacherEvalService teacherEvalService;
    private final PeerEvalService peerEvalService;
    private final IndicatorService indicatorService;
    private final UserService userService;
    private final RewardPunishService rewardPunishService;
    private final CategoryService categoryService;

    public ScoreController(BatchService bs, TotalScoreMapper tsm, SelfEvalService ses,
                           SelfEvalMapper sem, TeacherEvalService tes, PeerEvalService pes,
                           IndicatorService is, UserService us, RewardPunishService rps,
                           CategoryService cs) {
        this.batchService = bs;
        this.totalScoreMapper = tsm;
        this.selfEvalService = ses;
        this.selfEvalMapper = sem;
        this.teacherEvalService = tes;
        this.peerEvalService = pes;
        this.indicatorService = is;
        this.userService = us;
        this.rewardPunishService = rps;
        this.categoryService = cs;
    }

    /** 分数汇总页面 */
    @GetMapping("/score/summary")
    public String summary(Model model, @RequestParam(required = false) Integer batchId) {
        List<Batch> batches = batchService.lambdaQuery().orderByDesc(Batch::getId).list();
        model.addAttribute("batches", batches);
        if (batchId == null && !batches.isEmpty()) batchId = batches.get(0).getId();
        if (batchId != null) {
            model.addAttribute("ranking", totalScoreMapper.selectRankingByBatch(batchId));
            model.addAttribute("classAvg", totalScoreMapper.selectClassAvgByBatch(batchId));
            model.addAttribute("currentBatch", batchService.getById(batchId));
        }
        model.addAttribute("selectedBatchId", batchId);
        return "score/summary";
    }

    /** 计算总分 - 按德智体美劳权重 */
    @PostMapping("/api/score/calculate")
    @ResponseBody
    public Result calculate(@RequestBody Map<String, Integer> params) {
        Integer batchId = params.get("batchId");
        if (batchId == null) return Result.error("请指定批次");

        // 获取所有学生
        List<User> students = userService.lambdaQuery().eq(User::getRole, 0).eq(User::getStatus, 1).list();
        int count = 0;
        for (User stu : students) {
            // 每个维度分别计算：自评30% + 教师评50% + 互评20%
            BigDecimal deScore   = calcDimension(stu.getId(), batchId, "德育素质");
            BigDecimal zhiScore  = calcDimension(stu.getId(), batchId, "智育素质");
            BigDecimal tiScore   = calcDimension(stu.getId(), batchId, "体育素质");
            BigDecimal meiScore  = calcDimension(stu.getId(), batchId, "美育素质");
            BigDecimal laoScore  = calcDimension(stu.getId(), batchId, "劳育素质");

            // 总分 = 各维度×权重（和为100%）
            BigDecimal finalScore = deScore.multiply(new BigDecimal("0.20"))
                .add(zhiScore.multiply(new BigDecimal("0.35")))
                .add(tiScore.multiply(new BigDecimal("0.15")))
                .add(meiScore.multiply(new BigDecimal("0.10")))
                .add(laoScore.multiply(new BigDecimal("0.10")));

            // 奖惩附加分（占10%）
            BigDecimal bonus = sumBonus(stu.getId(), batchId);
            BigDecimal bonusWeighted = bonus.multiply(new BigDecimal("0.10"));
            finalScore = finalScore.add(bonusWeighted);

            if (finalScore.compareTo(new BigDecimal("100")) > 0) finalScore = new BigDecimal("100");
            if (finalScore.compareTo(BigDecimal.ZERO) < 0) finalScore = BigDecimal.ZERO;

            // 保存
            TotalScore ts = totalScoreMapper.selectOne(
                new LambdaQueryWrapper<TotalScore>()
                    .eq(TotalScore::getStudentId, stu.getId())
                    .eq(TotalScore::getBatchId, batchId));
            if (ts == null) ts = new TotalScore();
            ts.setStudentId(stu.getId());
            ts.setBatchId(batchId);
            ts.setSelfTotal(deScore.add(zhiScore).add(tiScore).add(meiScore).add(laoScore));
            ts.setBonusTotal(bonus);
            ts.setFinalScore(finalScore);
            totalScoreMapper.insertOrUpdate(ts);
            count++;
        }

        // 生成排名
        generateRanking(batchId);
        return Result.success("已为" + count + "名学生计算得分并生成排名");
    }

    /** 计算单个维度的加权得分: 自评30% + 教师评50% + 互评20% */
    private BigDecimal calcDimension(Integer studentId, Integer batchId, String categoryKeyword) {
        // 自评该维度分数
        BigDecimal selfScore = BigDecimal.ZERO;
        int selfCount = 0;
        List<Map<String, Object>> selfList = selfEvalMapper.selectByStudentAndBatch(studentId, batchId);
        for (Map<String, Object> row : selfList) {
            String cat = (String) row.get("category_name");
            if (cat != null && cat.contains(categoryKeyword.substring(0, 2))) {
                Object s = row.get("self_score");
                if (s != null) { selfScore = selfScore.add(new BigDecimal(s.toString())); selfCount++; }
            }
        }

        // 教师评该维度分数
        BigDecimal teacherScore = BigDecimal.ZERO;
        int teacherCount = 0;
        var indicators = indicatorService.lambdaQuery().eq(Indicator::getStatus, 1).list();
        var teacherEvals = teacherEvalService.lambdaQuery().eq(TeacherEval::getStudentId, studentId).eq(TeacherEval::getBatchId, batchId).list();
        for (TeacherEval te : teacherEvals) {
            for (Indicator ind : indicators) {
                if (ind.getId().equals(te.getIndicatorId())) {
                    String catName = getCategoryName(ind.getCategoryId());
                    if (catName != null && catName.contains(categoryKeyword.substring(0, 2))) {
                        if (te.getScore() != null) { teacherScore = teacherScore.add(te.getScore()); teacherCount++; }
                    }
                }
            }
        }

        // 互评该维度分数
        BigDecimal peerScore = BigDecimal.ZERO;
        int peerCount = 0;
        var peerEvals = peerEvalService.lambdaQuery().eq(PeerEval::getTargetId, studentId).eq(PeerEval::getBatchId, batchId).list();
        for (PeerEval pe : peerEvals) {
            for (Indicator ind : indicators) {
                if (ind.getId().equals(pe.getIndicatorId())) {
                    String catName = getCategoryName(ind.getCategoryId());
                    if (catName != null && catName.contains(categoryKeyword.substring(0, 2))) {
                        if (pe.getScore() != null) { peerScore = peerScore.add(pe.getScore()); peerCount++; }
                    }
                }
            }
        }

        // 加权: 自评30% + 教师评50% + 互评20%
        BigDecimal selfAvg = selfCount > 0 ? selfScore.divide(new BigDecimal(selfCount), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        BigDecimal teacherAvg = teacherCount > 0 ? teacherScore.divide(new BigDecimal(teacherCount), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        BigDecimal peerAvg = peerCount > 0 ? peerScore.divide(new BigDecimal(peerCount), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;

        return selfAvg.multiply(new BigDecimal("0.30"))
            .add(teacherAvg.multiply(new BigDecimal("0.50")))
            .add(peerAvg.multiply(new BigDecimal("0.20")));
    }

    private String getCategoryName(Integer categoryId) {
        var cat = categoryService.getById(categoryId);
        return cat != null ? cat.getCategoryName() : "";
    }

    private BigDecimal sumBonus(Integer studentId, Integer batchId) {
        List<RewardPunish> list = rewardPunishService.lambdaQuery()
            .eq(RewardPunish::getStudentId, studentId)
            .eq(RewardPunish::getStatus, 1).list();
        BigDecimal total = BigDecimal.ZERO;
        for (RewardPunish rp : list) {
            if (rp.getScoreChange() != null) total = total.add(rp.getScoreChange());
        }
        return total;
    }

    private void generateRanking(Integer batchId) {
        List<Map<String, Object>> list = totalScoreMapper.selectRankingByBatch(batchId);
        for (int i = 0; i < list.size(); i++) {
            TotalScore ts = totalScoreMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TotalScore>()
                    .eq(TotalScore::getStudentId, list.get(i).get("student_id"))
                    .eq(TotalScore::getBatchId, batchId));
            if (ts != null) {
                ts.setRanking(i + 1);
                totalScoreMapper.updateById(ts);
            }
        }
    }

    /** 导出排名Excel */
    @GetMapping("/score/export")
    public void exportExcel(@RequestParam Integer batchId, HttpServletResponse response) throws Exception {
        List<Map<String,Object>> ranking = totalScoreMapper.selectRankingByBatch(batchId);
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("学生排名");
        Row header = sheet.createRow(0);
        String[] cols = {"排名", "学号", "姓名", "班级", "自评总分", "教师评分", "互评总分", "加分", "最终得分"};
        for (int i = 0; i < cols.length; i++) { header.createCell(i).setCellValue(cols[i]); }
        for (int i = 0; i < ranking.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Map<String,Object> r = ranking.get(i);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(String.valueOf(r.getOrDefault("student_no","")));
            row.createCell(2).setCellValue(String.valueOf(r.getOrDefault("student_name","")));
            row.createCell(3).setCellValue(String.valueOf(r.getOrDefault("class_name","")));
            row.createCell(4).setCellValue(String.valueOf(r.getOrDefault("self_total","0")));
            row.createCell(5).setCellValue(String.valueOf(r.getOrDefault("teacher_total","0")));
            row.createCell(6).setCellValue(String.valueOf(r.getOrDefault("peer_total","0")));
            row.createCell(7).setCellValue(String.valueOf(r.getOrDefault("bonus_total","0")));
            row.createCell(8).setCellValue(String.valueOf(r.getOrDefault("final_score","0")));
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode("学生排名.xlsx", "UTF-8"));
        OutputStream os = response.getOutputStream();
        wb.write(os);
        wb.close();
        os.flush();
    }
}