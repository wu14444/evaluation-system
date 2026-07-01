package com.eval.config;

import com.eval.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {
    @ModelAttribute("role")
    public Integer addRole(HttpSession session) {
        User user = (User) session.getAttribute("loginUser");
        return user != null ? user.getRole() : -1;
    }
}
