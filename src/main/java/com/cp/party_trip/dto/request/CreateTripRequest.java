package com.cp.party_trip.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

// สร้างทริปใหม่ (คนสร้าง = เจ้าของ token ไม่ได้มาจาก request)
public class CreateTripRequest {
    @NotBlank(message = "กรุณาตั้งชื่อทริป")
    @Size(max = 100, message = "ชื่อทริปยาวได้ไม่เกิน 100 ตัวอักษร")
    private String title;

    @NotNull(message = "กรุณาเลือกวันเริ่มต้น")
    private LocalDate startDate;

    @NotNull(message = "กรุณาเลือกวันสิ้นสุด")
    private LocalDate endDate;

    // สกุลเงินหลักของทริป (ไม่ส่ง = บาท) + เรท 1 หน่วย = กี่บาท
    @Size(max = 3, message = "รหัสสกุลเงินต้องเป็นตัวอักษร 3 ตัว")
    private String currency;

    private BigDecimal exchangeRate;

    @Size(max = 50, message = "เขตเวลาไม่ถูกต้อง")
    private String timeZone;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
}
