"""ใช้งานพร้อมกันหลายเครื่อง/หลายคน: สมัคร เข้าร่วม เพิ่ม/แก้บิล โหวต รับเงิน พร้อมกัน ข้อมูลต้องไม่ซ้ำ/ไม่เพี้ยน"""
from decimal import Decimal

from common import *

created = {"trips": set(), "users": set()}
N = 8
names = [name(f"c{i}_") for i in range(N)]

# ---------- 1. สมัครพร้อมกัน 8 คน ----------
logins = parallel([lambda n=n: call("POST", f"/users/login?username={n}") for n in names])
check("8 users sign up at once -> all 200", all(s == 200 for s, _ in logins), [s for s, _ in logins])
tok = {n: r["token"] for n, (s, r) in zip(names, logins)}
for _, r in logins:
    created["users"].add(r["id"])

# ---------- 2. ชื่อใหม่ชื่อเดียวกัน 2 เครื่องพร้อมกัน ----------
same = name("twin")
twin = parallel([lambda: call("POST", f"/users/login?username={same}") for _ in range(2)])
codes = sorted(s for s, _ in twin)
check("same NEW name on 2 devices at once -> one 200, other 409 (no duplicate account)", codes == [200, 409], twin)
st, r = call("POST", f"/users/login?username={same}", token=next((x["token"] for s, x in twin if s == 200), None))
check("that name still logs in afterwards (no 500 from duplicate rows)", st == 200, (st, r))
for s, x in twin:
    if s == 200:
        created["users"].add(x["id"])

# ---------- 3. คนเดียว 2 เครื่อง (PIN) ----------
owner = names[0]
call("POST", "/users/pin?pin=2468", token=tok[owner])
st, dev2 = call("POST", f"/users/login?username={owner}&pin=2468")
check("same user, 2nd device via PIN gets same token", st == 200 and dev2["token"] == tok[owner], (st, dev2))

# ---------- 4. สร้างทริป + เข้าร่วมพร้อมกัน (รวมคนเดิมกดซ้ำ 2 เครื่อง) ----------
st, trip = call("POST", "/trips/create", token=tok[owner],
                body=TRIP)
tripId, code = trip["id"], trip["inviteCode"]
set_read_token(tok[owner])
created["trips"].add(tripId)
joiners = names[1:] + [names[1]]  # คนที่ 1 กดเข้าร่วมซ้ำจากอีกเครื่องพร้อมกัน
joins = parallel([lambda n=n: call("POST", f"/trips/join/{code}", token=tok[n]) for n in joiners])
check("7 people (+1 double-tap) join at once -> all 200", all(s == 200 for s, _ in joins), [s for s, _ in joins])
st, members = call("GET", f"/trips/{tripId}/members")
gn = [m["guestName"] for m in members]
check("no duplicate members after concurrent joins", len(gn) == len(set(gn)) == N, sorted(gn))
mid = {m["guestName"]: m["id"] for m in members}
everyone = ",".join(str(mid[n]) for n in names)

# ---------- 5. ทุกคนเพิ่มบิลพร้อมกัน คนละ 5 ใบ ----------
def add_bill(n, k):
    amt = 100 + k * 37 + names.index(n)
    body = {"title": f"bill {n[-1]}-{k}", "totalAmount": amt, "splitType": "EQUAL", "category": "FOOD",
            "currency": "THB", "expenseDate": "2026-10-02"}
    return amt, call("POST", f"/expenses/add/{tripId}?paidByMemberId={mid[n]}&participantIds={everyone}", token=tok[n], body=body)
adds = parallel([lambda n=n, k=k: add_bill(n, k) for n in names for k in range(5)], workers=40)
ok_adds = [a for a, (s, _) in adds if s == 200]
check("40 bills added concurrently -> all 200", len(ok_adds) == 40, [s for _, (s, _) in adds if s != 200][:5])
st, bills = call("GET", f"/expenses/trip/{tripId}")
check("server has exactly 40 bills", len(bills) == 40, len(bills))
total_server = sum(Decimal(str(b["totalAmount"])) for b in bills)
check("bill totals match what was sent", total_server == Decimal(sum(ok_adds)), (total_server, sum(ok_adds)))
bad_split = [b["id"] for b in bills if sum(Decimal(str(s["amountOwed"])) for s in b["splits"]) != Decimal(str(b["totalAmount"]))]
check("every bill's splits sum to its total", not bad_split, bad_split)

# ---------- 6. ยอดหนี้สุทธิสมดุล ----------
st, transfers = call("GET", f"/debts/simplify/{tripId}")
net = {}
for t in transfers:
    a = Decimal(str(t["amount"]))
    net[t["from"]["id"]] = net.get(t["from"]["id"], 0) - a
    net[t["to"]["id"]] = net.get(t["to"]["id"], 0) + a
paid = {}
share = {}
for b in bills:
    paid[b["user"]["id"]] = paid.get(b["user"]["id"], 0) + Decimal(str(b["totalAmount"]))
    for s in b["splits"]:
        share[s["tripMember"]["id"]] = share.get(s["tripMember"]["id"], 0) + Decimal(str(s["amountOwed"]))
expect = {m: paid.get(m, 0) - share.get(m, 0) for m in mid.values()}
diff = [m for m in mid.values() if abs(net.get(m, 0) + 0 - expect[m]) > Decimal("0.05")]
check("simplified transfers match paid-minus-share for every member", not diff, (net, expect))

# ---------- 7. ทุกคนโหวตพร้อมกัน + คนเดียวกดโหวตจาก 2 เครื่องพร้อมกัน ----------
st, poll = call("POST", "/polls", token=tok[owner], body={"tripId": tripId, "question": "กินอะไร", "options": ["A", "B"]})
pollId = poll["id"]
st, polls = call("GET", f"/polls/trip/{tripId}?memberId={mid[owner]}")
opts = next(p for p in polls if p.get("id", p.get("pollId")) == pollId)
opt_ids = [o.get("id", o.get("optionId")) for o in opts.get("options", [])]
voters = names + [names[2]]  # คนที่ 2 กดซ้ำจากอีกเครื่อง
votes = parallel([lambda n=n, i=i: call("POST", f"/polls/{pollId}/vote?optionId={opt_ids[i % 2]}&memberId={mid[n]}", token=tok[n])
                  for i, n in enumerate(voters)])
st, res = call("GET", f"/polls/{pollId}/results")
total_votes = sum(int(r.get("voteCount", r.get("votes", 0))) for r in res) if isinstance(res, list) else -1
check("9 concurrent votes (1 double) -> no 500", all(s in (200, 409) for s, _ in votes), [(s, r) for s, r in votes if s not in (200, 409)])
check("at most one vote per member (≤ 8 votes counted)", 0 < total_votes <= N, (total_votes, res))

# ---------- 8. ยืนยันรับเงินบิลเดียวกันจาก 2 เครื่องพร้อมกัน ----------
b0 = next(b for b in bills if b["user"]["id"] == mid[owner])
debtor = next(s["tripMember"]["id"] for s in b0["splits"] if s["tripMember"]["id"] != mid[owner])
confirms = parallel([lambda: call("PUT", f"/expenses/splits/{b0['id']}/{debtor}/pay", token=tok[owner]) for _ in range(2)])
check("double confirm same split at once -> no error", all(s == 200 for s, _ in confirms), confirms)
st, b0n = call("GET", f"/expenses/trip/{tripId}")
sp = next(s for b in b0n if b["id"] == b0["id"] for s in b["splits"] if s["tripMember"]["id"] == debtor)
check("split marked paid once (paidAmount == owed)", sp["isPaid"] and Decimal(str(sp["paidAmount"])) == Decimal(str(sp["amountOwed"])), sp)

# ---------- 9. รับเงินก้อน (repay) ซ้อนกัน 2 ครั้งพร้อมกัน เกินยอดค้าง ----------
payer2 = names[3]
debtor2 = names[4]
st, summ = call("GET", f"/debts/summary-details/{tripId}?userId={mid[payer2]}", token=tok[payer2])
owed = sum(Decimal(str(s["amountOwed"])) - Decimal(str(s.get("paidAmount") or 0))
           for b in summ["myPaidBills"] for s in b["splits"] if s["memberId"] == mid[debtor2] and not s["isPaid"])
half = (owed * Decimal("0.6")).quantize(Decimal("0.01"))  # 2 × 60% = เกินยอดค้าง
reps = parallel([lambda: call("POST", f"/expenses/repay/{tripId}?receiverId={mid[payer2]}&senderId={mid[debtor2]}",
                              token=tok[payer2], body={"amount": float(half)}) for _ in range(2)])
st, summ2 = call("GET", f"/debts/summary-details/{tripId}?userId={mid[payer2]}", token=tok[payer2])
received = sum(Decimal(str(s.get("paidAmount") or 0)) for b in summ2["myPaidBills"] for s in b["splits"] if s["memberId"] == mid[debtor2])
check("two concurrent lump repayments never exceed what was owed", received <= owed + Decimal("0.01"),
      ([s for s, _ in reps], owed, received))
check("at least one repayment accepted", any(s == 200 for s, _ in reps), reps)

# ---------- 10. คนจ่ายแก้บิลเดียวกันจาก 2 เครื่องพร้อมกัน ----------
b1 = next(b for b in bills if b["user"]["id"] == mid[names[5]])
rev = b1.get("revision", 0)
edits = parallel([lambda a=a: call("PUT", f"/expenses/{b1['id']}?memberId={mid[names[5]]}&revision={rev}&participantIds={everyone}",
                                   token=tok[names[5]], body={"title": f"edit {a}", "totalAmount": a, "splitType": "EQUAL",
                                                              "category": "FOOD", "currency": "THB"}) for a in (500, 800)])
st, after = call("GET", f"/expenses/trip/{tripId}")
eb = next(b for b in after if b["id"] == b1["id"])
splits_sum = sum(Decimal(str(s["amountOwed"])) for s in eb["splits"])
check("same bill edited from 2 devices at once -> one saved, other rejected 409",
      sorted(s for s, _ in edits) == [200, 409], [(s, r if s != 200 else "ok") for s, r in edits])
winner = next(r for s, r in edits if s == 200)
check("bill on server equals the accepted edit (no silent overwrite)", Decimal(str(eb["totalAmount"])) == Decimal(str(winner["totalAmount"])), (eb["totalAmount"], winner.get("totalAmount")))
check("edited bill stays consistent (splits sum == total, one split per member)",
      splits_sum == Decimal(str(eb["totalAmount"])) and len(eb["splits"]) == len({s["tripMember"]["id"] for s in eb["splits"]}) == N,
      (eb["totalAmount"], splits_sum, len(eb["splits"])))

# ---------- 11. หลายคนตั้งค่าทริปพร้อมกัน ----------
sets = parallel([lambda n=n, i=i: call("PUT", f"/trips/{tripId}/budget?memberId={mid[n]}&amount={1000 + i}", token=tok[n])
                 for i, n in enumerate(names)])
check("8 concurrent budget updates -> all 200", all(s == 200 for s, _ in sets), [s for s, _ in sets])

# ---------- 12. อ่านหนัก ๆ ระหว่างเขียน ----------
reads = parallel([lambda: call("GET", f"/expenses/trip/{tripId}") for _ in range(40)], workers=40)
check("40 concurrent reads -> all 200", all(s == 200 for s, _ in reads), [s for s, _ in reads if s != 200][:5])


finish()
