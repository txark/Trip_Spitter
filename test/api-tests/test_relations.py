"""ความสัมพันธ์ในฐานข้อมูล: One-to-One (ตั้งค่าทริป), Many-to-Many (ผู้ร่วมกิจกรรม / ผู้รับผิดชอบเช็กลิสต์)"""
from common import *

A, B = name("A"), name("B")
st, a, _ = call_h("POST", f"/sessions?username={A}")
st, b, _ = call_h("POST", f"/sessions?username={B}")
ta, tb = a["token"], b["token"]
st, trip, _ = call_h("POST", "/trips", token=ta, body={**TRIP, "currency": "JPY", "exchangeRate": 0.24, "timeZone": "Asia/Tokyo"})
check("create trip 201 + settings", st == 201 and trip["currency"] == "JPY" and trip["timeZone"] == "Asia/Tokyo", (st, trip))
tid = trip["id"]
call_h("POST", f"/invitations/{trip['inviteCode']}/members", token=tb)
st, members, _ = call_h("GET", f"/trips/{tid}/members", token=ta)
ids = [m["id"] for m in members]
st, act, _ = call_h("POST", f"/trips/{tid}/activities", token=ta, body={"title": "hike", "category": "ACTIVITY", "activityDate": "2026-10-02", "memberId": ids[0], "participantIds": ids})
check("activity with participants", st == 201 and sorted(act["participantIds"]) == sorted(ids), (st, act))
st, acts, _ = call_h("GET", f"/trips/{tid}/activities", token=ta)
check("participants persisted + ordered", [x["participantIds"] for x in acts if x["id"] == act["id"]] == [ids], acts)
st, item, _ = call_h("POST", f"/trips/{tid}/checklist-items?itemName=tent&assignedToMemberId={ids[1]}", token=ta)
check("checklist single assignee", st == 201 and item["assigneeIds"] == [ids[1]], (st, item))
st, item, _ = call_h("PUT", f"/checklist-items/{item['id']}/assignees", token=ta, body=ids)
check("checklist set 2 assignees", st == 200 and item["assigneeIds"] == ids, (st, item))
st, item, _ = call_h("PUT", f"/checklist-items/{item['id']}/assignees", token=ta, body=[])
check("checklist clear assignees", st == 200 and item["assigneeIds"] == [], (st, item))
st, r, _ = call_h("PUT", f"/trips/{tid}/budget?memberId={ids[0]}&amount=5000", token=ta)
st, t2, _ = call_h("GET", f"/trips/{tid}", token=ta)
check("budget saved in settings", t2["budgetPerPerson"] == 5000, t2)
st, r, _ = call_h("DELETE", f"/activities/{act['id']}?memberId={ids[0]}", token=ta)
check("delete activity keeps members", st == 204 and len(call_h("GET", f"/trips/{tid}/members", token=ta)[1]) == 2, (st, r))
finish()
