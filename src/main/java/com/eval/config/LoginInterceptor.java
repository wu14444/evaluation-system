package com.eval.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        // 登录相关请求直接放行
        if (uri.equals("/login") || uri.equals("/api/login") || uri.startsWith("/css/") || uri.startsWith("/js/")) {
            return true;
        }
        HttpSession session = request.getSession();
        Object user = session.getAttribute("loginUser");
        if (user == null) {
            if (uri.startsWith("/api/")) {
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"msg\":\"请先登录\"}");
            } else {
                response.sendRedirect("/login");
            }
            return false;
        }
        return true;
    }
}
