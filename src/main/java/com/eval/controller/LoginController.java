package com.eval.controller;

import com.eval.common.Result;
import com.eval.entity.User;
import com.eval.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LoginController {

    private final UserService userService;

    public LoginController(UserService userService) {
        this.userService = userService;
    }

    /** 登录页面 */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /** 登录API */
    @PostMapping("/api/login")
    @ResponseBody
    public Result login(@RequestBody User loginUser, HttpSession session) {
        User user = userService.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getStudentNo, loginUser.getStudentNo())
                .eq(User::getPassword, loginUser.getPassword()));
        if (user == null) return Result.error("账号或密码错误");
        if (user.getStatus() == 0) return Result.error("账号已被禁用");
        user.setPassword(null);
        session.setAttribute("loginUser", user);
        return Result.success(user);
    }

    /** 退出 */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    /** 首页路由 */
    @GetMapping({"/", "/home"})
    public String home(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        if (user == null) return "redirect:/login";
        switch (user.getRole()) {
            case 2: return "redirect:/admin/dashboard";
            case 1: return "redirect:/teacher/dashboard";
            default: return "redirect:/student/dashboard";
        }
    }
}