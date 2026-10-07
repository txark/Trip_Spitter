package com.cp.party_trip.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// เอกสาร API (Swagger UI: /swagger-ui.html, JSON: /v3/api-docs)
// ปุ่ม Authorize ใส่ token ที่ได้จาก POST /api/v1/users/login ลงช่อง X-Auth-Token ได้
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Party & Trip Expense Splitter API")
                        .version("v1")
                        .description("API สำหรับวางแผนทริปและหารค่าใช้จ่าย: ทริป สมาชิก แพลน บิล หนี้ โหวต เช็กลิสต์"))
                .components(new Components().addSecuritySchemes("authToken",
                        new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER).name(AuthGuard.HEADER)))
                .addSecurityItem(new SecurityRequirement().addList("authToken"));
    }
}
