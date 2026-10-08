"""แก้วันเริ่ม/วันสิ้นสุดของทริป: สมาชิกแก้ได้, คนนอกแก้ไม่ได้, ตรวจวันที่ผิด"""
from common import *

A, B, C = name("A"), name("B"), name("C")
tA, tB, tC = (call("POST", f"/sessions?username={n}")[1]["token"] for n in (A, B, C))
trip = call("POST", "/trips", tA, TRIP)[1]
call("POST", f"/invitations/{trip['inviteCode']}/members", tB)
mem = {m["guestName"]: m["id"] for m in call("GET", f"/trips/{trip['id']}/members", tA)[1]}
url = f"/trips/{trip['id']}/dates?memberId={mem[B]}"

st, r = call("PUT", f"{url}&startDate=2026-12-31&endDate=2027-01-08", tB)
check("member edits dates (crosses year)", st == 200 and r["startDate"] == "2026-12-31" and r["endDate"] == "2027-01-08", (st, r))
st, r = call("GET", f"/trips/{trip['id']}", tA)
check("other members see new dates", st == 200 and r["startDate"] == "2026-12-31" and r["endDate"] == "2027-01-08", (st, r))
st, r = call("PUT", f"{url}&startDate=2027-01-05&endDate=2027-01-05", tB)
check("one-day trip allowed", st == 200, (st, r))

for label, s, e in [("end before start", "2027-01-05", "2027-01-04"),
                    ("not a date", "2027-02-30", "2027-03-01"),
                    ("garbage", "abc", "2027-03-01"),
                    ("year out of range", "1999-01-01", "1999-01-02"),
                    ("longer than 365 days", "2027-01-01", "2028-01-01")]:
    st, r = call("PUT", f"{url}&startDate={s}&endDate={e}", tB)
    check(f"{label} -> 400", st == 400, (st, r))

st, r = call("PUT", f"{url}&startDate=2027-01-01&endDate=2027-01-02")
check("no token -> 401", st == 401, (st, r))
st, r = call("PUT", f"/trips/{trip['id']}/dates?memberId={mem[B]}&startDate=2027-01-01&endDate=2027-01-02", tC)
check("outsider -> 403", st == 403, (st, r))
st, r = call("PUT", f"/trips/{trip['id']}/dates?memberId={mem[A]}&startDate=2027-01-01&endDate=2027-01-02", tB)
check("edit as another member -> 403", st == 403, (st, r))
st, r = call("GET", f"/trips/{trip['id']}", tA)
check("rejected edits changed nothing", r["startDate"] == "2027-01-05" and r["endDate"] == "2027-01-05", r)

finish()
