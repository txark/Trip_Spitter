package com.cp.party_trip.config;

import com.cp.party_trip.dto.response.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

// error ทุกแบบจากทุกคอนโทรลเลอร์ ส่งกลับรูปแบบเดียวกัน: {"message": "..."} (ข้อความภาษาไทยให้หน้าเว็บแสดงได้เลย)
@RestControllerAdvice
public class ApiErrorHandler {

    // error ที่ service/AuthGuard โยนมา (400/401/403/404/409 ...)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<MessageResponse> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(new MessageResponse(e.getReason() == null ? "" : e.getReason()));
    }

    // @Valid ไม่ผ่าน: ส่งข้อความของช่องแรกที่ผิด
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> invalid(MethodArgumentNotValidException e) {
        FieldError field = e.getBindingResult().getFieldError();
        String message = field != null && field.getDefaultMessage() != null ? field.getDefaultMessage()
                : "ข้อมูลไม่ถูกต้อง";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(message));
    }

    // JSON อ่านไม่ได้ (เช่น วันที่/ตัวเลขผิดรูปแบบ)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MessageResponse> unreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("รูปแบบข้อมูลไม่ถูกต้อง"));
    }
}
