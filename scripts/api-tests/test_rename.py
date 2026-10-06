"""เปลี่ยนชื่อเล่น: ทริป/บิล/หนี้/ประวัติตามไปด้วย, ชื่อซ้ำ, เปลี่ยนชื่อชนกับสมัครพร้อมกัน"""
import threading

from common import *
A, B, C = name("A"), name("B"), name("C")
a = call("POST", f"/users/login?username={A}")[1]
b = call("POST", f"/users/login?username={B}")[1]
c = call("POST", f"/users/login?username={C}")[1]
tA, tB, tC = a["token"], b["token"], c["token"]

# A สร้าง 2 ทริป, B เข้าทริปแรก, A จ่ายบิลในทริปแรก
t1 = call("POST", "/trips/create", tA, TRIP)[1]
t2 = call("POST", "/trips/create", tA, TRIP)[1]
call("POST", f"/trips/join/{t1['inviteCode']}", tB)
mem = {m["guestName"]: m["id"] for m in call("GET", f"/trips/{t1['id']}/members", tA)[1]}
st, bill = call("POST", f"/expenses/add/{t1['id']}?paidByMemberId={mem[A]}&participantIds={mem[A]},{mem[B]}", tA,
                {"title": "dinner", "totalAmount": 300, "splitType": "EQUAL", "category": "FOOD", "currency": "THB"})
check("setup bill", st == 200, (st, bill))
call("POST", f"/history/view?userId=0&tripId={t1['id']}", tA)

st, r = call("POST", "/users/rename?username=x")
check("rename without token -> 401", st == 401, (st, r))
st, r = call("POST", f"/users/rename?username={B}", tA)
check("rename to existing account -> 409 NAME_TAKEN", st == 409 and r.get("message") == "NAME_TAKEN", (st, r))
st, r = call("POST", "/users/rename?username=%20%20", tA)
check("blank name -> 400", st == 400, (st, r))

NEW = name("N")
st, r = call("POST", f"/users/rename?username={NEW}", tA)
check("rename ok, same id + same token", st == 200 and r["username"] == NEW and r["id"] == a["id"] and r["token"] == tA, (st, r))
st, me = call("GET", "/users/me", tA)
check("/users/me returns new name (other devices sync)", st == 200 and me["username"] == NEW, (st, me))

for t in (t1, t2):
    st, ms = call("GET", f"/trips/{t['id']}/members", tA)
    check(f"trip {t['id']}: member renamed + still readable", st == 200 and NEW in [m["guestName"] for m in ms] and A not in [m["guestName"] for m in ms], (st, ms))
st, bills = call("GET", f"/expenses/trip/{t1['id']}", tA)
check("old bill still paid by me (same member id)", st == 200 and bills[0]["user"]["id"] == mem[A] and bills[0]["user"]["guestName"] == NEW, bills)
st, summ = call("GET", f"/debts/summary-details/{t1['id']}?userId={mem[A]}", tA)
check("my debt summary still works", st == 200, (st, summ))
st, r = call("PUT", f"/expenses/{bill['id']}?memberId={mem[A]}&paidByMemberId={mem[A]}&participantIds={mem[A]},{mem[B]}", tA,
             {"title": "dinner2", "totalAmount": 300, "splitType": "EQUAL", "category": "FOOD", "currency": "THB"})
check("can still edit my old bill", st == 200, (st, r))
st, hist = call("GET", f"/history/recent/{a['id']}", tA)
check("history still lists the trip", st == 200 and any(h["tripId"] == t1["id"] for h in hist), (st, hist))

st, r = call("POST", f"/users/login?username={A}")
check("old name is free again for someone else", st == 200 and r["id"] != a["id"], (st, r))
oldA = r
st, r = call("GET", f"/trips/{t1['id']}", oldA["token"])
check("new owner of the old name can't see my trips", st == 403, (st, r))

# ชื่อชนกับสมาชิกในทริปเดียวกัน (ข้อมูลเก่าที่ guest ไม่มีบัญชี): สร้างสถานการณ์ด้วยการให้ C เข้าทริป แล้วเปลี่ยน C เป็นชื่อที่ว่างแต่ซ้ำ guestName
# (ปกติชื่อเล่นผูกบัญชีเสมอ จึงทดสอบผ่านกรณี B เปลี่ยนเป็นชื่อของ A เดิมที่ตอนนี้มีเจ้าของใหม่ = NAME_TAKEN)
st, r = call("POST", f"/users/rename?username={A}", tB)
check("rename to a name another account now owns -> NAME_TAKEN", st == 409 and r.get("message") == "NAME_TAKEN", (st, r))

# เปลี่ยนชื่อพร้อมกับอีกเครื่องสมัครชื่อเดียวกัน
RACE = name("R")
out = {}
bar = threading.Barrier(2)
def ren():
    bar.wait(); out["rename"] = call("POST", f"/users/rename?username={RACE}", tC)
def sign():
    bar.wait(); out["signup"] = call("POST", f"/users/login?username={RACE}")
ts = [threading.Thread(target=ren), threading.Thread(target=sign)]
[t.start() for t in ts]; [t.join() for t in ts]
codes = sorted([out["rename"][0], out["signup"][0]])
check("rename vs sign-up same name at once -> exactly one wins", codes == [200, 409], out)


finish()
