package com.cp.party_trip.service;

import com.cp.party_trip.dto.request.ActivityRequest;
import com.cp.party_trip.model.Activity;
import java.util.List;

// แพลนเที่ยว: เพิ่ม/แก้/ลบกิจกรรมในทริป
// ตัวจริงอยู่ที่ service/impl/ActivityServiceImpl (Controller ขึ้นกับ interface นี้ ไม่ใช่คลาสจริง)
public interface ActivityService {
    List<Activity> getTripActivities(Long tripId);

    Activity addActivity(Long tripId, ActivityRequest request);

    Activity updateActivity(Long activityId, ActivityRequest request);

    void deleteActivity(Long activityId, Long memberId);
}
