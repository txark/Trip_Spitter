"""รับเงินเป็นยอดรวม (ไม่ระบุรายการ): ต้องหักรายการที่ค้างน้อยที่สุดก่อน"""
from decimal import Decimal

from common import *

A, B = name("A"), name("B")
st, a, _ = call_h("POST", f"/users/login?username={A}")
st, b, _ = call_h("POST", f"/users/login?username={B}")
ta, tb = a["token"], b["token"]
st, trip, _ = call_h("POST", "/trips/create", token=ta, body=TRIP)
tid = trip["id"]
call_h("POST", f"/trips/join/{trip['inviteCode']}", token=tb)
st, members, _ = call_h("GET", f"/trips/{tid}/members", token=ta)
mA = next(m["id"] for m in members if m["guestName"] == A)
mB = next(m["id"] for m in members if m["guestName"] == B)
both = f"participantIds={mA},{mB}"

# A จ่ายบิลใหญ่ก่อน (B ต้องจ่าย 100) แล้วค่อยจ่ายบิลเล็ก (B ต้องจ่าย 15)
st, big, _ = call_h("POST", f"/expenses/add/{tid}?paidByMemberId={mA}&{both}", token=ta,
                    body={"title": "big", "totalAmount": 200, "splitType": "EQUAL"})
st, small, _ = call_h("POST", f"/expenses/add/{tid}?paidByMemberId={mA}&{both}", token=ta,
                      body={"title": "small", "totalAmount": 30, "splitType": "EQUAL"})
check("bills created (big first, small later)", big["id"] < small["id"], (big["id"], small["id"]))

# A กดรับเงินจาก B เป็นยอดรวม 20 (ไม่ระบุรายการ)
st, rep, _ = call_h("POST", f"/expenses/repay/{tid}?receiverId={mA}&senderId={mB}", token=ta, body={"amount": 20})
check("repayment accepted", st == 201, (st, rep))
by_title = {i["title"]: Decimal(str(i["amount"])) for i in rep["items"]}
check("smallest remaining item is cleared first (15 to small)", by_title.get("small") == Decimal("15"), rep["items"])
check("the rest goes to the bigger item (5 to big)", by_title.get("big") == Decimal("5"), rep["items"])


def split_of_b(title):
    st, views, _ = call_h("GET", f"/expenses/trip/{tid}", token=ta)
    exp = next(e for e in views if e["title"] == title)
    return next(s for s in exp["splits"] if s["tripMember"]["id"] == mB)


sm = split_of_b("small")
bg = split_of_b("big")
check("small bill split is fully paid", sm["isPaid"] is True, sm)
check("big bill split is partly paid (5 of 100)", bg["isPaid"] is False and Decimal(str(bg["paidAmount"])) == Decimal("5"), bg)

# ยกเลิกการรับเงิน: ทุกอย่างกลับเป็นเหมือนก่อนรับ
st, r, _ = call_h("DELETE", f"/expenses/repay/{rep['id']}?memberId={mA}", token=ta)
check("undo repayment -> 204", st == 204, (st, r))
check("after undo: small unpaid again", split_of_b("small")["isPaid"] is False, split_of_b("small"))
check("after undo: big has no paid amount", not split_of_b("big").get("paidAmount"), split_of_b("big"))

# ยกเลิกได้เฉพาะรายการรับเงินล่าสุดของคู่นี้: รับ 2 ครั้ง แล้วลองยกเลิกครั้งแรกก่อน
st, first, _ = call_h("POST", f"/expenses/repay/{tid}?receiverId={mA}&senderId={mB}", token=ta, body={"amount": 10})
st, second, _ = call_h("POST", f"/expenses/repay/{tid}?receiverId={mA}&senderId={mB}", token=ta, body={"amount": 5})
check("two repayments recorded", first["id"] < second["id"], (first, second))
st, r, _ = call_h("DELETE", f"/expenses/repay/{first['id']}?memberId={mA}", token=ta)
check("undo an older repayment -> 409", st == 409, (st, r))
st, r, _ = call_h("DELETE", f"/expenses/repay/{second['id']}?memberId={mA}", token=ta)
check("undo the latest repayment -> 204", st == 204, (st, r))
st, r, _ = call_h("DELETE", f"/expenses/repay/{first['id']}?memberId={mA}", token=ta)
check("after the newer one is undone, the older one can be undone -> 204", st == 204, (st, r))
st, r, _ = call_h("DELETE", f"/expenses/repay/{first['id']}?memberId={mA}", token=ta)
check("undo twice -> 404", st == 404, (st, r))

finish()
