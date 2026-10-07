package com.cp.party_trip.config;

import com.cp.party_trip.service.AuthTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

// อ่าน token จาก header X-Auth-Token แล้วแนบผู้ใช้ไว้กับ request (AuthGuard ใช้ต่อ)
// คำสั่งที่แก้ข้อมูล (POST/PUT/PATCH/DELETE) ต้องมี token ที่ถูกต้อง ยกเว้นการเข้าสู่ระบบ
// การอ่านข้อมูล (GET) ผ่านได้ แต่คอนโทรลเลอร์เช็กเองว่าเป็นสมาชิกทริปนั้น (guard.me) ไม่ใช่สมาชิก = 403
@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthTokenService authTokenService;

    public AuthInterceptor(AuthTokenService authTokenService) {
        this.authTokenService = authTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true; // CORS preflight
        }
        authTokenService.findByToken(request.getHeader(AuthGuard.HEADER))
                .ifPresent(user -> request.setAttribute(AuthGuard.ATTRIBUTE, user));

        boolean readOnly = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
        boolean login = request.getRequestURI().endsWith("/api/v1/users/login");
        if (readOnly || login || request.getAttribute(AuthGuard.ATTRIBUTE) != null) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"กรุณาเข้าสู่ระบบใหม่ที่หน้าแรก\"}");
        return false;
    }
}
