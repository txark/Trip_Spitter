"""ฟีเจอร์ตามใบงาน: ประวัติความเคลื่อนไหว (Observer), แบ่งหน้า/เรียงลำดับ, วิธีหาร (Strategy), Swagger"""
import json
import urllib.request

from common import *

A, B, C = name("A"), name("B"), name("C")
st, a, _ = call_h("POST", f"/users/login?username={A}")
st, b, _ = call_h("POST", f"/users/login?username={B}")
st, c, _ = call_h("POST", f"/users/login?username={C}")
ta, tb, tc = a["token"], b["token"], c["token"]
st, trip, _ = call_h("POST", "/trips/create", token=ta, body=TRIP)
tid = trip["id"]
call_h("POST", f"/trips/join/{trip['inviteCode']}", token=tb)
st, members, _ = call_h("GET", f"/trips/{tid}/members", token=ta)
mA, mB = members[0]["id"], members[1]["id"]
both = f"participantIds={mA},{mB}"

# --- Strategy: EQUAL / CUSTOM / ชนิดที่ไม่รองรับ ---
st, e1, _ = call_h("POST", f"/expenses/add/{tid}?paidByMemberId={mA}&{both}", token=ta,
                   body={"title": "equal", "totalAmount": 100, "splitType": "EQUAL"})
amounts = sorted(s["amountOwed"] for s in e1["expenseSplits"])
check("EQUAL splits 100 -> 50/50", st == 201 and amounts == [50, 50], (st, e1))
st, e2, _ = call_h("POST", f"/expenses/add/{tid}?paidByMemberId={mA}", token=ta,
                   body={"title": "custom", "totalAmount": 100, "splitType": "CUSTOM",
                         "splits": [{"memberId": mA, "amount": 30}, {"memberId": mB, "amount": 70}]})
check("CUSTOM splits 30/70", st == 201 and sorted(s["amountOwed"] for s in e2["expenseSplits"]) == [30, 70], (st, e2))
st, r, _ = call_h("POST", f"/expenses/add/{tid}?paidByMemberId={mA}", token=ta,
                  body={"title": "x", "totalAmount": 100, "splitType": "PERCENT"})
check("unknown splitType -> 400 with supported list", st == 400 and "EQUAL" in r["message"] and "CUSTOM" in r["message"], (st, r))
st, r, _ = call_h("POST", f"/expenses/add/{tid}?paidByMemberId={mA}", token=ta,
                  body={"title": "x", "totalAmount": 100, "splitType": "CUSTOM",
                        "splits": [{"memberId": mA, "amount": 30}]})
check("CUSTOM sum mismatch -> 400", st == 400, (st, r))

# --- Observer: ประวัติความเคลื่อนไหว ---
st, ev, _ = call_h("GET", f"/trips/{tid}/events", token=ta)
check("events recorded for added bills", st == 200 and len(ev) >= 2 and ev[0]["type"] == "EXPENSE_ADDED"
      and A in ev[0]["message"], (st, ev))
st, r, _ = call_h("GET", f"/trips/{tid}/events", token=tc)
check("non-member cannot read events -> 403", st == 403, (st, r))
st, r, _ = call_h("GET", f"/trips/{tid}/events")
check("events without token -> 401", st == 401, (st, r))

# --- Pagination & Sorting ---
for i in range(3):
    call_h("POST", f"/expenses/add/{tid}?paidByMemberId={mA}&{both}", token=ta,
           body={"title": f"bill{i}", "totalAmount": 10 * (i + 1), "splitType": "EQUAL"})
st, p, _ = call_h("GET", f"/expenses/trip/{tid}/page?page=0&size=2&sort=totalAmount,desc", token=ta)
check("page 0 size 2 sorted by amount desc", st == 200 and p["size"] == 2 and p["totalElements"] == 5
      and p["totalPages"] == 3 and [x["totalAmount"] for x in p["content"]] == [100, 100], (st, p))
st, p2, _ = call_h("GET", f"/expenses/trip/{tid}/page?page=2&size=2&sort=totalAmount,asc", token=ta)
check("last page has the remaining 1 item", st == 200 and len(p2["content"]) == 1, (st, p2))
st, r, _ = call_h("GET", f"/expenses/trip/{tid}/page?sort=splitType,asc", token=ta)
check("sort by disallowed field -> 400", st == 400, (st, r))
st, r, _ = call_h("GET", f"/expenses/trip/{tid}/page?size=500", token=ta)
check("page size over limit -> 400", st == 400, (st, r))
st, r, _ = call_h("GET", f"/expenses/trip/{tid}/page", token=tc)
check("non-member cannot page expenses -> 403", st == 403, (st, r))

# --- Swagger / OpenAPI ---
root = BASE[: BASE.index("/api/")]
with urllib.request.urlopen(root + "/swagger-ui.html", timeout=10) as r:
    check("swagger-ui.html reachable", r.status == 200)
with urllib.request.urlopen(root + "/v3/api-docs", timeout=10) as r:
    docs = json.loads(r.read().decode("utf-8"))
check("openapi lists /api/v1 paths", any(p.startswith("/api/v1/") for p in docs["paths"]), list(docs["paths"])[:3])

finish()
