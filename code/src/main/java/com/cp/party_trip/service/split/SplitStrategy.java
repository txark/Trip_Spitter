package com.cp.party_trip.service.split;

import java.util.List;

// Strategy Pattern: วิธีหารบิลแต่ละแบบเป็นคลาสของตัวเอง
// เพิ่มวิธีใหม่ (เช่น หารตามเปอร์เซ็นต์) = เพิ่มคลาสที่เป็น @Component เท่านั้น ไม่ต้องแก้ ExpenseServiceImpl (Open/Closed)
public interface SplitStrategy {
    // รหัสที่หน้าเว็บส่งมาใน splitType เช่น EQUAL, CUSTOM
    String type();

    // คำนวณส่วนของแต่ละคน ผลรวมต้องเท่ากับยอดบิลพอดี ข้อมูลไม่ถูกต้อง = 400
    List<SplitShare> split(SplitContext context);
}
