package com.eval.controller;
import com.eval.common.Result;
import com.eval.entity.Competition;
import com.eval.service.CompetitionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class CompetitionController {
    private final CompetitionService competitionService;
    public CompetitionController(CompetitionService cs) { this.competitionService = cs; }

    @GetMapping("/competitions")
    public String page(Model model) {
        model.addAttribute("list", competitionService.lambdaQuery().orderByAsc(Competition::getSortOrder).list());
        return "admin/competitions";
    }

    @PostMapping("/api/competition/save")
    @ResponseBody
    public Result save(@RequestBody Competition c) { competitionService.saveOrUpdate(c); return Result.success(); }

    @DeleteMapping("/api/competition/delete/{id}")
    @ResponseBody
    public Result delete(@PathVariable Integer id) { competitionService.removeById(id); return Result.success(); }

    @GetMapping("/api/competition/list")
    @ResponseBody
    public Result list() { return Result.success(competitionService.lambdaQuery().eq(Competition::getStatus,1).orderByAsc(Competition::getSortOrder).list()); }
}