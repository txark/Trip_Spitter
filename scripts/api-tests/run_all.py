"""รันทุกชุดทดสอบ API แล้วลบข้อมูลทดสอบออกจากฐานข้อมูล

    python scripts/api-tests/run_all.py            # ทุกชุด
    python scripts/api-tests/run_all.py auth       # เฉพาะชุดที่ชื่อมีคำนี้
    python scripts/api-tests/run_all.py --keep     # ไม่ลบข้อมูลทดสอบ (ไว้เปิดดูในเว็บ)

ต้องเปิด backend ก่อน (ค่าเริ่มต้น http://localhost:8090/api/v1 ตั้ง API_BASE เพื่อเปลี่ยน)
การลบข้อมูลใช้ psql + ค่าฐานข้อมูลจาก .env ที่โฟลเดอร์โปรเจกต์
"""
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent.parent


def read_env():
    values = {}
    env_file = ROOT / ".env"
    if env_file.exists():
        for line in env_file.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.lstrip().startswith("#"):
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip()
    values.update({k: v for k, v in os.environ.items() if k.startswith("DB_")})
    return values


def find_psql():
    found = shutil.which("psql")
    if found:
        return found
    for path in sorted(Path("C:/Program Files/PostgreSQL").glob("*/bin/psql.exe"), reverse=True):
        return str(path)
    return None


def cleanup():
    env = read_env()
    psql = find_psql()
    url = env.get("DB_URL", "jdbc:postgresql://localhost:5432/Trip_Spitter")
    m = re.match(r"jdbc:postgresql://([^:/]+)(?::(\d+))?/([^?]+)", url)
    if not psql or not m or "DB_PASSWORD" not in env:
        print("ข้ามการลบข้อมูลทดสอบ: ไม่พบ psql หรือ DB_PASSWORD (.env) ลบเองด้วย cleanup.sql")
        return False
    host, port, db = m.group(1), m.group(2) or "5432", m.group(3)
    result = subprocess.run(
        [psql, "-h", host, "-p", port, "-U", env.get("DB_USERNAME", "postgres"), "-d", db, "-q", "-v",
         "ON_ERROR_STOP=1", "-f", str(HERE / "cleanup.sql")],
        env={**os.environ, "PGPASSWORD": env["DB_PASSWORD"]}, capture_output=True, text=True, encoding="utf-8")
    if result.returncode != 0:
        print("ลบข้อมูลทดสอบไม่สำเร็จ:", result.stderr.strip())
        return False
    print("ลบข้อมูลทดสอบแล้ว (เหลือ trips | users):", result.stdout.strip().splitlines()[-2].strip())
    return True


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    keep = "--keep" in sys.argv
    suites = sorted(p for p in HERE.glob("test_*.py") if not args or any(a in p.stem for a in args))
    summary = []
    try:
        for suite in suites:
            print(f"\n===== {suite.stem} =====", flush=True)
            code = subprocess.run([sys.executable, str(suite)], cwd=HERE,
                                  env={**os.environ, "PYTHONIOENCODING": "utf-8"}).returncode
            summary.append((suite.stem, code))
            if code == 2:
                break  # เชื่อมต่อเซิร์ฟเวอร์ไม่ได้ ชุดอื่นก็ไม่ผ่านเหมือนกัน
    finally:
        if not keep:
            print()
            cleanup()

    print("\n===== สรุป =====")
    for stem, code in summary:
        print(f"{'ผ่าน   ' if code == 0 else 'ไม่ผ่าน'}  {stem}")
    sys.exit(0 if summary and all(code == 0 for _, code in summary) else 1)


if __name__ == "__main__":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except AttributeError:
        pass
    main()
