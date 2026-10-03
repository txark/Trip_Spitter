package com.cp.party_trip.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalTime;

// เมืองต่อเครื่องของเที่ยวบิน: ถึงเมืองนี้กี่โมง และออกจากเมืองนี้กี่โมง
@Embeddable
public class ActivityStop {

    @Column(length = 150)
    private String place;

    private LocalTime arriveTime;

    private LocalTime departTime;

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public LocalTime getArriveTime() {
        return arriveTime;
    }

    public void setArriveTime(LocalTime arriveTime) {
        this.arriveTime = arriveTime;
    }

    public LocalTime getDepartTime() {
        return departTime;
    }

    public void setDepartTime(LocalTime departTime) {
        this.departTime = departTime;
    }
}
