"""ตัวช่วยที่ทุกชุดทดสอบใช้ร่วมกัน (ยิง API จริงของ backend ที่รันอยู่)

- API_BASE: ที่อยู่ API (ค่าเริ่มต้น http://localhost:8090/api) ตั้งผ่าน environment variable ได้
- ข้อมูลทดสอบทุกชิ้นใช้ชื่อขึ้นต้นด้วย USER_PREFIX และทริปชื่อ TRIP_TITLE
  เพื่อให้ cleanup.sql ลบทิ้งได้ทั้งหมดโดยไม่แตะข้อมูลจริง
"""
import json
import os
import random
import sys
import threading
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor

BASE = os.environ.get("API_BASE", "http://localhost:8090/api").rstrip("/")
USER_PREFIX = "zt_"
TRIP_TITLE = "[api-test]"
TRIP = {"title": TRIP_TITLE, "startDate": "2026-10-01", "endDate": "2026-10-05"}

# ต่อท้ายชื่อทุกคนในรอบนี้ ไม่ให้ชนกับรอบก่อน
sfx = str(random.randint(10000, 99999))

results = []
_read_token = None


def name(tag):
    """ชื่อเล่นสำหรับทดสอบ เช่น name("A") -> zt_A12345"""
    return f"{USER_PREFIX}{tag}{sfx}"


def set_read_token(token):
    """GET ที่ไม่ได้ส่ง token จะใช้ token นี้แทน (อ่านข้อมูลทริปต้องเป็นสมาชิก)"""
    global _read_token
    _read_token = token


def call_h(method, path, token=None, body=None, origin=None):
    """ยิง API แล้วคืน (status, body, headers)"""
    if token is None and method == "GET":
        token = _read_token
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    if data is not None:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("X-Auth-Token", token)
    if origin:
        req.add_header("Origin", origin)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, _parse(r.read().decode()), dict(r.headers)
    except urllib.error.HTTPError as e:
        return e.code, _parse(e.read().decode()), dict(e.headers)


def call(method, path, token=None, body=None):
    """ยิง API แล้วคืน (status, body)"""
    status, parsed, _ = call_h(method, path, token, body)
    return status, parsed


def _parse(raw):
    try:
        return json.loads(raw)
    except ValueError:
        return raw


def check(label, cond, info=""):
    results.append((label, bool(cond)))
    print(("PASS " if cond else "FAIL ") + label + ("" if cond else f"  -> {str(info)[:400]}"), flush=True)


def parallel(fns, workers=16):
    """เรียกทุกฟังก์ชันพร้อมกันจริง (รอที่ barrier แล้วปล่อยพร้อมกัน)"""
    barrier = threading.Barrier(len(fns))

    def run(fn):
        barrier.wait()
        return fn()

    with ThreadPoolExecutor(max_workers=max(workers, len(fns))) as ex:
        return list(ex.map(run, fns))


def finish():
    passed = sum(ok for _, ok in results)
    print(f"\n{passed}/{len(results)} passed")
    sys.exit(0 if passed == len(results) else 1)


def require_server():
    try:
        urllib.request.urlopen(BASE + "/users/me", timeout=5)
    except urllib.error.HTTPError:
        return  # ตอบกลับ (401) = เซิร์ฟเวอร์ทำงานอยู่
    except OSError as e:
        print(f"เชื่อมต่อ {BASE} ไม่ได้ ({e}) เปิด backend ก่อน หรือตั้ง API_BASE ให้ถูก")
        sys.exit(2)


# Windows console เดิมเป็น cp1252: บังคับ UTF-8 ให้พิมพ์ภาษาไทยได้
try:
    sys.stdout.reconfigure(encoding="utf-8")
except AttributeError:
    pass
require_server()
