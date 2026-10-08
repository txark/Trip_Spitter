"""รหัสกู้คืน: คนสร้างทริปออกรหัสให้เพื่อนที่เปลี่ยนเครื่อง, ใช้ครั้งเดียว, เครื่องเก่าถูกตัด, กันเดารหัส"""
from common import *
A, B, C, D = (name(x) for x in "ABCD")
a, b, c, d = (call("POST", f"/sessions?username={n}")[1] for n in (A, B, C, D))
tA, tB, tC, tD = a["token"], b["token"], c["token"], d["token"]
check("new user has pinSet=false (banner shows)", a["pinSet"] is False and b["pinSet"] is False)

trip = call("POST", "/trips", tA, TRIP)[1]
call("POST", f"/invitations/{trip['inviteCode']}/members", tB)
call("POST", f"/invitations/{trip['inviteCode']}/members", tC)
mem = {m["guestName"]: m["id"] for m in call("GET", f"/trips/{trip['id']}/members", tA)[1]}

# B "lost phone": login from new device without token -> 409, message mentions recovery code
st, r = call("POST", f"/sessions?username={B}")
check("lost device, no PIN -> 409 hints recovery code", st == 409 and "รหัสกู้คืน" in r.get("message", ""), (st, r))

st, r = call("POST", f"/trips/{trip['id']}/members/{mem[B]}/recovery-codes", tC)
check("non-admin member can't issue code -> 403", st == 403, (st, r))
st, r = call("POST", f"/trips/{trip['id']}/members/{mem[B]}/recovery-codes", tD)
check("outsider can't issue code -> 403", st == 403, (st, r))
st, r = call("POST", f"/trips/{trip['id']}/members/{mem[A]}/recovery-codes", tA)
check("admin can't issue code for self -> 400", st == 400, (st, r))
st, r = call("POST", f"/trips/{trip['id']}/members/{mem[B]}/recovery-codes")
check("no token -> 401", st == 401, (st, r))
st, rec = call("POST", f"/trips/{trip['id']}/members/{mem[B]}/recovery-codes", tA)
check("admin issues 6-digit code", st == 200 and len(rec["code"]) == 6 and rec["name"] == B and rec["expiresInMinutes"] == 30, (st, rec))
code = rec["code"]

st, r = call("POST", f"/sessions?username={B}")
check("with active code -> PIN_REQUIRED (prompt shows)", st == 401 and r.get("message") == "PIN_REQUIRED", (st, r))
wrong = "000000" if code != "000000" else "111111"
st, r = call("POST", f"/sessions?username={B}&pin={wrong}")
check("wrong code -> 401", st == 401 and r.get("message") == "PIN ไม่ถูกต้อง", (st, r))
st, nb = call("POST", f"/sessions?username={B}&pin={code}")
check("right code -> logged in as same account, NEW token, pinSet false",
      st == 200 and nb["id"] == b["id"] and nb["token"] != tB and nb["pinSet"] is False, (st, nb))
st, r = call("GET", f"/trips/{trip['id']}", nb["token"])
check("recovered device sees the old trip", st == 200, (st, r))
st, r = call("GET", f"/trips/{trip['id']}", tB)
check("old (lost) device token no longer works", st == 401, (st, r))
st, r = call("POST", f"/sessions?username={B}&pin={code}")
check("code is single-use", st == 409, (st, r))

# B sets PIN afterwards
st, r = call("PUT", f"/users/me/pin?pin=8642", nb["token"])
check("set PIN after recovery", st == 200 and r["pinSet"] is True, (st, r))
st, r = call("GET", "/users/me", nb["token"])
check("/users/me reports pinSet (sync to other devices)", st == 200 and r["pinSet"] is True, (st, r))

# brute force limit applies to codes too
st, rec2 = call("POST", f"/trips/{trip['id']}/members/{mem[C]}/recovery-codes", tA)
bad = [call("POST", f"/sessions?username={C}&pin={'%06d' % i}")[0] for i in range(5) if "%06d" % i != rec2["code"]]
st, r = call("POST", f"/sessions?username={C}&pin={rec2['code']}")
check("5 wrong codes -> locked even with the right code (429)", st == 429, (st, r, bad))


finish()
