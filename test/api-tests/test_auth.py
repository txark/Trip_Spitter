"""ยืนยันตัวตน + สิทธิ์: token ต่อเครื่อง, PIN, ทำแทนคนอื่นไม่ได้, อ่านข้อมูลทริปได้เฉพาะสมาชิก, CORS วง LAN"""
import json

from common import *

call = call_h  # ชุดนี้ดู header ด้วย (CORS)
A, B, C = name("A"), name("B"), name("C")

# --- login ---
st, a, _ = call("POST", f"/sessions?username={A}")
check("new user gets token", st == 200 and len(a.get("token", "")) == 48, (st, a))
tokA = a["token"]
st, b, _ = call("POST", f"/sessions?username={B}")
tokB = b["token"]
st, c, _ = call("POST", f"/sessions?username={C}")
tokC = c["token"]
check("token not leaked in user JSON fields", "authToken" not in a and "pinHash" not in a, a)

st, r, _ = call("POST", f"/sessions?username={A}")
check("same name other device without PIN -> 409", st == 409, (st, r))
st, r, _ = call("POST", f"/sessions?username={A}", token=tokA)
check("same device token -> 200 same token", st == 200 and r["token"] == tokA, (st, r))

st, r, _ = call("PUT", f"/users/me/pin?pin=4321")
check("set PIN without token -> 401", st == 401, (st, r))
st, r, _ = call("PUT", f"/users/me/pin?pin=4321", token=tokA)
check("set PIN with token", st == 200 and r["pinSet"] is True, (st, r))
st, r, _ = call("POST", f"/sessions?username={A}")
check("other device -> PIN_REQUIRED", st == 401 and r.get("message") == "PIN_REQUIRED", (st, r))
st, r, _ = call("POST", f"/sessions?username={A}&pin=0000")
check("wrong PIN -> 401", st == 401 and r.get("message") == "PIN ไม่ถูกต้อง", (st, r))
st, r, _ = call("POST", f"/sessions?username={A}&pin=4321")
check("right PIN -> same token", st == 200 and r["token"] == tokA, (st, r))

# --- trip create/join ---
st, r, _ = call("POST", f"/trips?creatorName={B}", body=TRIP)
check("create trip without token -> 401", st == 401, (st, r))
st, trip, _ = call("POST", f"/trips?creatorName={B}", token=tokA,
                   body=TRIP)
check("create trip with token", st == 201, (st, trip))
tripId, code = trip["id"], trip["inviteCode"]
st, members, _ = call("GET", f"/trips/{tripId}/members", token=tokA)
check("creator is token owner (not creatorName param)", [m["guestName"] for m in members] == [A], members)
st, r, _ = call("POST", f"/invitations/{code}/members?memberName={A}", token=tokB)
st, members, _ = call("GET", f"/trips/{tripId}/members", token=tokA)
names = sorted(m["guestName"] for m in members)
check("join uses token owner name (B), ignores memberName=A", names == sorted([A, B]), names)
mA = next(m["id"] for m in members if m["guestName"] == A)
mB = next(m["id"] for m in members if m["guestName"] == B)

# --- expenses ---
bill = {"title": "dinner", "totalAmount": 300, "splitType": "EQUAL", "category": "FOOD", "currency": "THB"}
st, r, _ = call("POST", f"/trips/{tripId}/expenses?paidByMemberId={mA}&participantIds={mA},{mB}", body=bill)
check("add bill without token -> 401", st == 401, (st, r))
st, r, _ = call("POST", f"/trips/{tripId}/expenses?paidByMemberId={mA}&participantIds={mA},{mB}", token=tokC, body=bill)
check("non-member C adds bill -> 403", st == 403, (st, r))
st, r, _ = call("POST", f"/trips/{tripId}/expenses?paidByMemberId={mA}&recordedByMemberId={mA}&participantIds={mA},{mB}", token=tokB, body=bill)
check("B pretends recordedBy=A -> 403", st == 403, (st, r))
st, exp, _ = call("POST", f"/trips/{tripId}/expenses?paidByMemberId={mA}&participantIds={mA},{mB}", token=tokB, body=bill)
check("B records bill paid by A", st == 201 and exp.get("recordedById") == mB, (st, exp))
expId = exp["id"]
st, r, _ = call("DELETE", f"/expenses/{expId}?memberId={mA}", token=tokB)
check("B deletes as memberId=A -> 403", st == 403, (st, r))
st, r, _ = call("PUT", f"/expenses/{expId}/splits/{mB}/paid", token=tokB)
check("B (debtor) marks own split paid -> 403", st == 403, (st, r))
st, r, _ = call("PUT", f"/expenses/{expId}/splits/{mB}/paid", token=tokA)
check("A (payer) confirms B paid -> 200", st == 200, (st, r))

# --- trip settings / polls / activities / checklist ---
st, r, _ = call("PUT", f"/trips/{tripId}/budget?memberId={mA}&amount=5000", token=tokB)
check("B sets budget as A -> 403", st == 403, (st, r))
st, r, _ = call("PUT", f"/trips/{tripId}/budget?memberId={mB}&amount=5000", token=tokB)
check("B sets budget as self -> 200", st == 200, (st, r))
st, poll, _ = call("POST", "/polls", token=tokC, body={"tripId": tripId, "memberId": mA, "question": "q?", "options": ["x", "y"]})
check("C creates poll in A's trip -> 403", st == 403, (st, poll))
st, poll, _ = call("POST", "/polls", token=tokA, body={"tripId": tripId, "memberId": mA, "question": "q?", "options": ["x", "y"]})
check("A creates poll", st == 201, (st, poll))
st, act, _ = call("POST", f"/trips/{tripId}/activities", token=tokC,
                  body={"title": "walk", "category": "ACTIVITY", "activityDate": "2026-10-02", "memberId": mA})
check("C adds activity -> 403", st == 403, (st, act))
st, r, _ = call("POST", f"/trips/{tripId}/checklist-items?itemName=towel", token=tokC)
check("C adds checklist item -> 403", st == 403, (st, r))
st, r, _ = call("POST", f"/trips/{tripId}/checklist-items?itemName=towel", token=tokB)
check("B adds checklist item -> 201", st == 201, (st, r))

# --- reads: members only ---
reads = [f"/trips/{tripId}", f"/trips/{tripId}/members", f"/trips/{tripId}/expenses", f"/trips/{tripId}/debt-transfers",
         f"/trips/{tripId}/activities", f"/trips/{tripId}/checklist-items", f"/trips/{tripId}/polls",
         f"/polls/{poll['id']}/results", f"/trips/{tripId}/members/{mB}/debt-summary"]
anon = [call("GET", u)[0] for u in reads]
check("GET without token -> 401 on every trip read", all(s == 401 for s in anon), list(zip(reads, anon)))
outsider = [(call("GET", u, token=tokC)[:2]) for u in reads]
check("non-member C reads trip -> 403 'not a member' everywhere",
      all(s == 403 and "คุณไม่ได้เป็นสมาชิกในทริปนี้" in json.dumps(r, ensure_ascii=False) for s, r in outsider),
      [(u, s) for u, (s, r) in zip(reads, outsider)])
member = [call("GET", u, token=tokB)[0] for u in reads]
check("member B reads everything -> 200", all(s == 200 for s in member), list(zip(reads, member)))
st, r, _ = call("GET", f"/trips/{tripId}/members/{mA}/debt-summary", token=tokB)
check("B reads A's personal debt summary -> 403", st == 403, (st, r))
st, r, _ = call("GET", f"/trips/{tripId}/polls?memberId={mA}", token=tokB)
check("B asks polls as memberId=A -> 403", st == 403, (st, r))
st, r, _ = call("PUT", f"/users/{c['id']}/trip-history/{tripId}", token=tokC)
check("C records view of foreign trip -> 403", st == 403, (st, r))
st, r, _ = call("PUT", f"/users/{b['id']}/trip-history/{tripId}", token=tokB)
check("member B records own view -> 200", st == 200, (st, r))
st, r, _ = call("PUT", f"/users/{b['id']}/trip-history/{tripId}", token=tokA)
check("A records a view for B's history -> 403", st == 403, (st, r))
st, hist, _ = call("GET", f"/users/{b['id']}/trip-history", token=tokB)
check("own history lists the trip", st == 200 and any(h["tripId"] == tripId for h in hist), (st, hist))
st, r, _ = call("GET", f"/users/{b['id']}/trip-history", token=tokC)
check("C reads B's history -> 403", st == 403, (st, r))
st, r, _ = call("GET", f"/users/{b['id']}/trip-history")
check("history without token -> 401", st == 401, (st, r))
# C joins with the invite code -> now allowed
call("POST", f"/invitations/{code}/members", token=tokC)
st, r, _ = call("GET", f"/trips/{tripId}", token=tokC)
check("after joining, C can read the trip", st == 200 and r["inviteCode"] == code, (st, r))

# --- CORS from LAN origin ---
st, r, h = call("GET", f"/trips/{tripId}", token=tokA, origin="http://192.168.1.50:5500")
allow = {k.lower(): v for k, v in h.items()}.get("access-control-allow-origin")
check("CORS allows LAN origin", allow == "http://192.168.1.50:5500", h)

# cleanup
call("DELETE", f"/expenses/{expId}?memberId={mA}", token=tokA)

finish()
