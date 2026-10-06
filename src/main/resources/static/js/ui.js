// ตัวช่วย UI ที่ใช้ร่วมกันหลายหน้า (expenses, debts)
// สไตล์ที่คู่กัน (.avatar, .toast, .skeleton, .stat-*) อยู่ใน css/all.css

// ---------- ที่อยู่ backend (ตั้งที่เดียว ทุกหน้าใช้ API_BASE) ----------
// ลำดับ: localStorage "apiBase" -> <meta name="api-base"> -> เดาจากที่อยู่หน้าเว็บ
// - เปิดจาก Spring Boot เอง (เช่น http://192.168.1.5:8090/home.html) = เรียก /api ของเซิร์ฟเวอร์เดียวกัน
// - เปิดผ่าน Live Server ตอนพัฒนา (พอร์ต 5500–5599) = host เดียวกัน พอร์ต 8090
const API_BASE = (() => {
  const clean = (url) => String(url).trim().replace(/\/+$/, "");
  try {
    const saved = localStorage.getItem("apiBase");
    if (saved) return clean(saved);
  } catch {}
  const meta = document.querySelector('meta[name="api-base"]');
  if (meta?.content) return clean(meta.content);
  if (location.protocol === "file:" || !location.hostname) return "http://localhost:8090/api";
  const port = Number(location.port);
  if (port >= 5500 && port <= 5599) return `${location.protocol}//${location.hostname}:8090/api`;
  return `${location.origin}/api`;
})();

// ---------- ตัวตนผู้ใช้: token ต่อเครื่อง (แนบ header X-Auth-Token ให้ทุกคำสั่งที่ไป backend) ----------
// เก็บ token แยกตามชื่อเล่น เปลี่ยนชื่อไปมาในเครื่องเดียวกันจะได้ไม่ต้องใส่ PIN
const AUTH_HEADER = "X-Auth-Token";
const nativeFetch = window.fetch.bind(window);

function readTokens() {
  try {
    return JSON.parse(localStorage.getItem("authTokens") || "{}") || {};
  } catch {
    return {};
  }
}

function authToken(name = localStorage.getItem("username")) {
  return (name && readTokens()[name]) || "";
}

function saveAuth(user, token) {
  try {
    const tokens = readTokens();
    tokens[user.username] = token;
    localStorage.setItem("authTokens", JSON.stringify(tokens));
    localStorage.setItem("username", user.username);
    localStorage.setItem("userId", user.id);
    localStorage.setItem("pinSet", user.pinSet ? "1" : "0");
  } catch {}
}

// เข้าด้วยชื่อเล่น (+ PIN ถ้าชื่อนี้มีเจ้าของในเครื่องอื่น)
// ผล: { ok, user } | { ok:false, needPin } | { ok:false, taken, message } | { ok:false, message }
async function loginAs(username, pin = "") {
  const query = `username=${encodeURIComponent(username)}${pin ? `&pin=${encodeURIComponent(pin)}` : ""}`;
  const token = authToken(username);
  try {
    const res = await nativeFetch(`${API_BASE}/users/login?${query}`, {
      method: "POST",
      headers: token ? { [AUTH_HEADER]: token } : {},
    });
    const data = await res.json().catch(() => ({}));
    if (res.ok) {
      saveAuth(data, data.token);
      return { ok: true, user: data };
    }
    if (res.status === 401 && data.message === "PIN_REQUIRED") return { ok: false, needPin: true };
    if (res.status === 409) return { ok: false, taken: true, message: data.message };
    return { ok: false, message: data.message || "เข้าสู่ระบบไม่สำเร็จ" };
  } catch {
    return { ok: false, offline: true, message: "เชื่อมต่อเซิร์ฟเวอร์ไม่ได้" };
  }
}

// เปิดหน้าไหนก็ได้: มีชื่อแต่ยังไม่มี token (ผู้ใช้เดิมก่อนมีระบบนี้) -> ขอ token ให้อัตโนมัติ
// คำสั่งไป backend ทุกอันรอขั้นนี้เสร็จก่อน จะได้มี token แนบไปเสมอ
const authReady = (async () => {
  const name = localStorage.getItem("username");
  if (!name || authToken(name)) return;
  await loginAs(name);
})();

// เก็บชื่อใหม่ในเครื่องหลังเปลี่ยนชื่อ (token เดิมย้ายไปอยู่กับชื่อใหม่)
function saveRenamed(oldName, user, token) {
  try {
    const tokens = readTokens();
    if (oldName && oldName !== user.username) delete tokens[oldName];
    localStorage.setItem("authTokens", JSON.stringify(tokens));
  } catch {}
  saveAuth(user, token);
}

// เปลี่ยนชื่อจากเครื่องอื่นของเรา: ชื่อในเครื่องนี้ยังเป็นชื่อเก่า -> อัปเดตแล้วโหลดหน้าใหม่ (หน้าเว็บหา "ฉัน" ในทริปจากชื่อ)
(async () => {
  const name = localStorage.getItem("username");
  const token = authToken(name);
  if (!name || !token) return;
  try {
    const res = await nativeFetch(`${API_BASE}/users/me`, { headers: { [AUTH_HEADER]: token } });
    if (!res.ok) return;
    const me = await res.json();
    try {
      localStorage.setItem("pinSet", me.pinSet ? "1" : "0"); // ตั้ง/ลบ PIN จากเครื่องอื่น
    } catch {}
    if (me.username && me.username !== name) {
      saveRenamed(name, me, token);
      location.reload();
    }
  } catch {}
})();

let authWarned = false;
let notMemberWarned = false;
window.fetch = async (input, init = {}) => {
  const url = typeof input === "string" ? input : input?.url || "";
  if (!url.startsWith(API_BASE)) return nativeFetch(input, init);
  await authReady;
  const headers = new Headers(init.headers || (input instanceof Request ? input.headers : undefined));
  const token = authToken();
  if (token && !headers.has(AUTH_HEADER)) headers.set(AUTH_HEADER, token);
  const res = await nativeFetch(input, { ...init, headers });
  // token หาย/ไม่ตรงชื่อ: บอกครั้งเดียวต่อหน้า ให้กลับไปเข้าใหม่ที่หน้าแรก
  if (res.status === 401 && !url.includes("/users/login") && !authWarned) {
    authWarned = true;
    setTimeout(() => showToast("ยืนยันตัวตนไม่ผ่าน กรุณากลับไปเข้าสู่ระบบที่หน้าแรก", "error"), 0);
  }
  // เปิดลิงก์ทริปที่ตัวเองไม่ได้เป็นสมาชิก: ข้อมูลทริปดูได้เฉพาะสมาชิก -> พากลับหน้าแรกไปเข้าร่วมด้วยรหัสเชิญ
  if (res.status === 403 && !notMemberWarned && !/home\.html$/.test(location.pathname)) {
    res
      .clone()
      .text() // บางคอนโทรลเลอร์ตอบเป็นข้อความล้วน บางอันเป็น {"message": ...}
      .then((body) => {
        if (notMemberWarned || !body.includes("คุณไม่ได้เป็นสมาชิกในทริปนี้")) return;
        notMemberWarned = true;
        showToast("คุณยังไม่ได้เป็นสมาชิกทริปนี้ ขอรหัสเชิญจากเพื่อนแล้วกดเข้าร่วมที่หน้าแรก", "error");
        setTimeout(() => location.replace("home.html"), 2500);
      })
      .catch(() => {});
  }
  return res;
};

// จัดรูปแบบเงินบาท เช่น 1234.5 -> ฿1,234.5
function fmt(n) {
  return `฿${Number(n || 0).toLocaleString(undefined, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  })}`;
}

// ---------- หมวดหมู่ค่าใช้จ่าย (ตรงกับประเภทในแพลน เทียบแผนกับจ่ายจริงได้) ----------
const EXPENSE_CATEGORIES = [
  { value: "FOOD", label: "อาหาร", icon: "fa-solid fa-utensils" },
  { value: "ACCOMMODATION", label: "ที่พัก", icon: "fa-solid fa-hotel" },
  { value: "TRANSPORT", label: "เดินทาง", icon: "fa-solid fa-car" },
  { value: "SIGHTSEEING", label: "เที่ยวชม", icon: "fa-solid fa-camera" },
  { value: "ACTIVITY", label: "กิจกรรม", icon: "fa-solid fa-person-hiking" },
  { value: "SHOPPING", label: "ช้อปปิ้ง", icon: "fa-solid fa-shopping-bag" },
  { value: "OTHER", label: "อื่นๆ", icon: "fa-solid fa-star" },
];
const expenseCategoryOf = (value) =>
  EXPENSE_CATEGORIES.find((c) => c.value === value) || EXPENSE_CATEGORIES[EXPENSE_CATEGORIES.length - 1];
// ประเภทในแพลน -> หมวดบิล
const PLAN_TO_EXPENSE_CATEGORY = {
  FOOD: "FOOD",
  STAY: "ACCOMMODATION",
  TRAVEL: "TRANSPORT",
  SIGHTSEEING: "SIGHTSEEING",
  ACTIVITY: "ACTIVITY",
  OTHER: "OTHER",
};

// ยอดประมาณการทั้งรายการในแพลน เป็นบาท (สูตรเดียวกับหน้าแพลน)
// ที่พัก = ราคา/ห้อง/คืน × ห้อง × คืน, เดินทาง/กิจกรรมที่ระบุคน = × คนที่ไป, อื่น ๆ = × ทุกคน
// กิน/รถส่วนตัว ไม่มีงบในแพลน
function planEstimateBaht(act, trip, memberCount) {
  const noCost = act.category === "FOOD" || (act.category === "TRAVEL" && act.transportMode === "CAR");
  const cost = noCost ? 0 : Number(act.cost) || 0;
  if (cost <= 0) return 0;
  let rate = 1;
  if (isForeign(act.costCurrency)) {
    rate =
      trip?.currency === act.costCurrency && Number(trip.exchangeRate) > 0
        ? Number(trip.exchangeRate)
        : Number(act.costRate) || 0;
  }
  const unit = cost * rate;
  if (act.category === "STAY") {
    const day = (v) => new Date(`${String(v).slice(0, 10)}T00:00:00`);
    const nights = act.endDate ? Math.max(Math.round((day(act.endDate) - day(act.activityDate)) / 86400000), 0) : 0;
    return unit * (act.rooms || 1) * Math.max(nights, 1);
  }
  const people =
    ["ACTIVITY", "TRAVEL"].includes(act.category) && act.participantIds?.length
      ? act.participantIds.length
      : memberCount;
  return unit * Math.max(people, 1);
}

// ---------- สกุลเงิน ----------
// เงินหลักของระบบคือบาท (หนี้/งบคิดเป็นบาทเสมอ) เงินอื่นเก็บคู่กับเรท "1 หน่วย = กี่บาท"
const BASE_CURRENCY = "THB";
const CURRENCIES = [
  { code: "THB", symbol: "฿", name: "บาท", dp: 2 },
  { code: "JPY", symbol: "¥", name: "เยน", dp: 0 },
  { code: "KRW", symbol: "₩", name: "วอน", dp: 0 },
  { code: "CNY", symbol: "CN¥", name: "หยวน", dp: 2 },
  { code: "HKD", symbol: "HK$", name: "ดอลลาร์ฮ่องกง", dp: 2 },
  { code: "TWD", symbol: "NT$", name: "ดอลลาร์ไต้หวัน", dp: 0 },
  { code: "SGD", symbol: "S$", name: "ดอลลาร์สิงคโปร์", dp: 2 },
  { code: "MYR", symbol: "RM", name: "ริงกิต", dp: 2 },
  { code: "IDR", symbol: "Rp", name: "รูเปียห์", dp: 0 },
  { code: "PHP", symbol: "₱", name: "เปโซ", dp: 2 },
  { code: "VND", symbol: "₫", name: "ดอง", dp: 0 },
  { code: "LAK", symbol: "₭", name: "กีบ", dp: 0 },
  { code: "MMK", symbol: "K", name: "จ๊าด", dp: 0 },
  { code: "INR", symbol: "₹", name: "รูปี", dp: 2 },
  { code: "NPR", symbol: "Rs", name: "รูปีเนปาล", dp: 2 },
  { code: "AED", symbol: "AED ", name: "ดีแรห์ม", dp: 2 },
  { code: "TRY", symbol: "₺", name: "ลีรา", dp: 2 },
  { code: "EUR", symbol: "€", name: "ยูโร", dp: 2 },
  { code: "GBP", symbol: "£", name: "ปอนด์", dp: 2 },
  { code: "CHF", symbol: "CHF ", name: "ฟรังก์สวิส", dp: 2 },
  { code: "USD", symbol: "$", name: "ดอลลาร์สหรัฐ", dp: 2 },
  { code: "AUD", symbol: "A$", name: "ดอลลาร์ออสเตรเลีย", dp: 2 },
  { code: "NZD", symbol: "NZ$", name: "ดอลลาร์นิวซีแลนด์", dp: 2 },
];
const isForeign = (code) => !!code && String(code).toUpperCase() !== BASE_CURRENCY;
function currencyOf(code) {
  const c = String(code || BASE_CURRENCY).toUpperCase();
  return CURRENCIES.find((x) => x.code === c) || { code: c, symbol: `${c} `, name: c, dp: 2 };
}
const currencySymbol = (code) => currencyOf(code).symbol.trim();

// เช่น fmtCur(3000, "JPY") -> ¥3,000 (บาทใช้ fmt เดิม)
function fmtCur(n, code) {
  if (!isForeign(code)) return fmt(n);
  const c = currencyOf(code);
  return `${c.symbol}${Number(n || 0).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: c.dp })}`;
}

// ยอดเงินต่างประเทศ -> บาท (ปัดเป็นสตางค์)
const toBaht = (amount, rate) => Math.round(Number(amount || 0) * Number(rate || 0) * 100) / 100;
const roundRate = (rate) => Math.round(Number(rate || 0) * 1e6) / 1e6;

// "1 ¥ = ฿0.213" (เงินที่ค่าน้อยมากแสดงเป็น 100/1,000 หน่วย จะได้อ่านง่าย)
function rateText(code, rate) {
  const r = Number(rate) || 0;
  const unit = r && r < 0.01 ? 1000 : r && r < 0.1 ? 100 : 1;
  const baht = (r * unit).toLocaleString(undefined, { maximumFractionDigits: unit > 1 ? 2 : 4 });
  return `${fmtCur(unit, code)} = ฿${baht}`;
}

// สกุลเงินที่น่าจะใช้ ตามเขตเวลาของทริป
const ZONE_CURRENCY = {
  "Asia/Bangkok": "THB", "Asia/Yangon": "MMK", "Asia/Kolkata": "INR", "Asia/Kathmandu": "NPR",
  "Asia/Singapore": "SGD", "Asia/Manila": "PHP", "Asia/Makassar": "IDR", "Asia/Hong_Kong": "HKD",
  "Asia/Shanghai": "CNY", "Asia/Taipei": "TWD", "Asia/Tokyo": "JPY", "Asia/Seoul": "KRW",
  "Asia/Dubai": "AED", "Europe/Istanbul": "TRY", "Europe/London": "GBP", "Europe/Paris": "EUR",
  "Australia/Sydney": "AUD", "Pacific/Auckland": "NZD", "America/Los_Angeles": "USD", "America/New_York": "USD",
};

// เรทกลาง (mid-market) วันนี้ -> { rate, date, source }
// 1) ECB ผ่าน frankfurter.dev (สกุลหลัก อัปเดตวันทำการละครั้ง)
// 2) สกุลที่ ECB ไม่มี (ดอง กีบ จ๊าด ฯลฯ) หรือดึงไม่ได้ -> open.er-api.com (อัปเดตวันละครั้ง)
// ไม่มีทั้งสองแหล่ง = null ให้กรอกเอง
const ECB_CODES = new Set(["AUD", "CHF", "CNY", "EUR", "GBP", "HKD", "IDR", "INR", "JPY", "KRW", "MYR", "NZD", "PHP", "SGD", "TRY", "USD"]);
const rateCache = new Map();

async function rateFromEcb(c) {
  const res = await fetch(`https://api.frankfurter.dev/v1/latest?base=${c}&symbols=THB`);
  if (!res.ok) throw new Error("rate unavailable");
  const data = await res.json();
  const rate = Number(data?.rates?.THB);
  if (!(rate > 0)) throw new Error("rate unavailable");
  return { rate: roundRate(rate), date: data.date, source: "ECB" };
}

async function rateFromOpenEr(c) {
  const res = await fetch(`https://open.er-api.com/v6/latest/${c}`);
  if (!res.ok) throw new Error("rate unavailable");
  const data = await res.json();
  const rate = Number(data?.rates?.THB);
  if (data?.result !== "success" || !(rate > 0)) return null;
  const date = new Date(Number(data.time_last_update_unix) * 1000).toISOString().slice(0, 10);
  return { rate: roundRate(rate), date, source: "ExchangeRate-API" };
}

// ช่องเรท = ตัวแปลงเงินสองทาง: [₺ 1] = [฿ 0.6836] กรอกฝั่งไหนก็ได้ อีกฝั่งคำนวณให้
// เรท (1 หน่วย = กี่บาท) มาจากเรทกลาง/เรทของทริป แก้ในช่องไม่ได้ ช่องนี้มีไว้เทียบราคาเท่านั้น
// ช่องเงินสกุลอื่นมี data-out = id ช่องบาท, ช่องบาทมี data-from = id ช่องเงินสกุลอื่น
// เรทแบบ 1 หน่วย = กี่บาท เช่น ₺1 = ฿0.6836, ₫1 = ฿0.001292
function bahtUnitText(code, rate) {
  const r = Number(rate) || 0;
  const dp = r !== 0 && r < 0.01 ? 6 : r < 1 ? 4 : 2;
  return `${currencySymbol(code)}1 = ฿${r.toLocaleString(undefined, { maximumFractionDigits: dp })}`;
}

// ปัดให้อ่านง่าย: ต่ำกว่า 1 เก็บ 4 ตำแหน่ง ไม่งั้น 2 ตำแหน่ง
function roundFine(n) {
  const v = Number(n) || 0;
  const a = Math.abs(v);
  const f = v === 0 ? 100 : a < 0.01 ? 1e6 : a < 1 ? 1e4 : 100;
  return Math.round(v * f) / f;
}

// เงินสกุลอื่น -> บาท
function updateRateOut(input) {
  const out = input.dataset.out && document.getElementById(input.dataset.out);
  if (!out) return;
  const rate = Number(input.dataset.rate) || 0;
  const amount = Number(input.value);
  out.disabled = !(rate > 0);
  out.placeholder = rate > 0 ? "0" : "–";
  out.value = rate > 0 && input.value !== "" && amount >= 0 ? roundFine(amount * rate) : "";
}

// บาท -> เงินสกุลอื่น (แปลงกลับ)
function updateRateIn(bahtInput) {
  const foreign = document.getElementById(bahtInput.dataset.from);
  if (!foreign) return;
  const rate = Number(foreign.dataset.rate) || 0;
  const baht = Number(bahtInput.value);
  if (rate > 0) foreign.value = bahtInput.value !== "" && baht >= 0 ? roundFine(baht / rate) : "";
}

// ตั้งเรทให้ช่อง: เริ่มที่ 1 หน่วย = กี่บาท
function setRateInput(input, rate) {
  if (Number(rate) > 0) {
    input.dataset.rate = roundRate(rate);
    input.value = 1;
  } else {
    input.value = "";
    delete input.dataset.rate;
  }
  updateRateOut(input);
}

// เรทของช่อง (บาท/หน่วย) — ไม่มีเรท = 0
const readRateInput = (input) => Number(input.dataset.rate) || 0;

document.addEventListener("input", (e) => {
  if (e.target?.dataset?.out) updateRateOut(e.target);
  else if (e.target?.dataset?.from) updateRateIn(e.target);
});

// ใต้ช่องเรท: "ข้อมูลวันที่ 2026-10-02 เรทกลาง **₺1 = ฿0.6836**"
const rateNoteHtml = (code, r) => `ข้อมูลวันที่ ${esc(r.date)} เรทกลาง <strong>${esc(bahtUnitText(code, r.rate))}</strong>`;

async function fetchRateToBaht(code) {
  const c = String(code || "").toUpperCase();
  if (!/^[A-Z]{3}$/.test(c) || c === BASE_CURRENCY) return null;
  if (rateCache.has(c)) return rateCache.get(c);
  let result = null;
  if (ECB_CODES.has(c)) {
    try {
      result = await rateFromEcb(c);
    } catch {
      result = null;
    }
  }
  if (!result) result = await rateFromOpenEr(c);
  if (result) rateCache.set(c, result);
  return result;
}

// ชื่อคน/ชื่อบิลมาจากผู้ใช้ ต้อง escape ก่อนใส่ลง innerHTML ทุกครั้ง
function esc(s) {
  return String(s ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c],
  );
}

// วงกลมตัวอักษรแรกของชื่อ สีคงที่ตามชื่อ (ชื่อเดิมได้สีเดิมทุกครั้ง)
// โทนเข้มพอให้อ่านตัวอักษรบนพื้นพาสเทลได้ (.avatar ใน all.css ผสมเป็นพื้นอ่อนเอง)
const AVATAR_COLORS = ["#4f46e5", "#ea580c", "#16a34a", "#db2777", "#0284c7", "#7c3aed", "#ca8a04"];

function avatar(name, size = "") {
  const str = String(name || "?");
  let hash = 0;
  for (const ch of str) hash = (hash * 31 + ch.codePointAt(0)) >>> 0;
  const color = AVATAR_COLORS[hash % AVATAR_COLORS.length];
  const initial = esc([...str.trim()][0] || "?").toUpperCase();
  return `<div class="avatar ${size}" style="--av:${color}">${initial}</div>`;
}

// หาสมาชิกทริปที่เป็นผู้ใช้คนปัจจุบัน (TripMember.guestName = username ใน localStorage)
// หาไม่เจอคืน null — ห้ามเดาเป็นสมาชิกคนแรก ไม่งั้นจะบันทึกบิล/ยืนยันรับเงิน/โหวตแทนคนอื่น
function findMyMember(members) {
  const username = localStorage.getItem("username");
  return (members || []).find((m) => username && m.guestName === username) || null;
}

// เวลาตอนนี้ "ที่ที่เที่ยว" เป็น Date ที่ตัวเลข วัน/ชม./นาที ตรงกับนาฬิกาของเขตเวลานั้น
// (แพลนเก็บเวลาท้องถิ่นของที่เที่ยว เทียบกับนาฬิกาเครื่องตรง ๆ จะคลาดเมื่อไปต่างประเทศ)
const DEFAULT_TIME_ZONE = "Asia/Bangkok";
function zoneNow(zone) {
  const now = new Date();
  try {
    const parts = {};
    new Intl.DateTimeFormat("en-US", {
      timeZone: zone || DEFAULT_TIME_ZONE,
      hourCycle: "h23",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    })
      .formatToParts(now)
      .forEach((p) => (parts[p.type] = p.value));
    return new Date(+parts.year, +parts.month - 1, +parts.day, +parts.hour, +parts.minute, +parts.second);
  } catch {
    return now;
  }
}

const NOT_MEMBER_TEXT = "ไม่พบชื่อของคุณในทริปนี้ เข้าร่วมทริปด้วยรหัสเชิญที่หน้าแรกก่อน";

// ยอดเงินจากการบวกลบทศนิยม (เช่น 0.1 + 0.2 - 0.3) ให้ปัดเป็นสตางค์ก่อนเทียบ > 0 / < 0
function roundMoney(n) {
  return Math.round((Number(n) || 0) * 100) / 100;
}

// กล่องข้อความว่าง/ผิดพลาด พร้อมไอคอน
function emptyBox(icon, text, isError = false) {
  return `<div class="empty-box${isError ? " error" : ""}"><i class="${icon}"></i>${esc(text)}</div>`;
}

// แจ้งผลแบบหายเอง (แทน alert ที่ต้องกด OK)
let toastTimeout;
function showToast(message, type = "success") {
  let toast = document.getElementById("toast");
  if (!toast) {
    toast = document.createElement("div");
    toast.id = "toast";
    toast.setAttribute("role", "status");
    toast.setAttribute("aria-live", "polite");
    document.body.appendChild(toast);
  }
  const icon = type === "success" ? "fa-solid fa-circle-check" : "fa-solid fa-circle-exclamation";
  toast.className = `toast ${type}`;
  toast.innerHTML = `<i class="${icon}"></i><span></span>`;
  toast.querySelector("span").textContent = message;
  requestAnimationFrame(() => toast.classList.add("show"));
  clearTimeout(toastTimeout);
  toastTimeout = setTimeout(() => toast.classList.remove("show"), 2800);
}

// ตราประทับ PAID (คู่กับ .paid-stamp ใน all.css) — animate=true เล่นอนิเมชันกระแทก
function paidStampHtml(sub = "จ่ายครบแล้ว", animate = false) {
  return `<div class="paid-stamp${animate ? " slam" : ""}" aria-hidden="true">PAID<small>${esc(sub)}</small></div>`;
}

// กระดาษสีโปรยทั่วจอ (ใช้ตอนเคลียร์หนี้ครบ) — ข้ามถ้าผู้ใช้ตั้งค่าลดการเคลื่อนไหว
function launchConfetti(count = 90) {
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
  const colors = ["#4f46e5", "#16a34a", "#ea580c", "#db2777", "#0284c7", "#ca8a04"];
  const pieces = [];
  for (let i = 0; i < count; i++) {
    const p = document.createElement("div");
    p.className = "confetti-piece";
    p.style.left = `${Math.random() * 100}vw`;
    p.style.background = colors[i % colors.length];
    p.style.animationDelay = `${Math.random() * 0.4}s`;
    p.style.setProperty("--dx", `${Math.random() * 160 - 80}px`);
    p.style.setProperty("--rot", `${Math.random() * 720 - 360}deg`);
    p.style.setProperty("--dur", `${1.8 + Math.random() * 1.4}s`);
    if (i % 3 === 0) {
      p.style.width = "7px";
      p.style.height = "7px";
      p.style.borderRadius = "50%";
    }
    pieces.push(p);
  }
  document.body.append(...pieces);
  setTimeout(() => pieces.forEach((p) => p.remove()), 4000);
}

// ย่อขนาดตัวเลขในแถบสรุป (.stat-value) ให้พอดีช่อง แสดงจำนวนเงินครบทุกหลัก แทนการตัดเป็น "..."
// ทำงานเองทุกหน้าที่มี .stat-summary: ตอนค่าเปลี่ยน และตอนขนาดจอเปลี่ยน
const STAT_MIN_FONT_PX = 11;

function fitStatValues(root = document) {
  root.querySelectorAll(".stat-value").forEach((el) => {
    el.style.fontSize = ""; // เริ่มจากขนาดปกติใน CSS ทุกครั้ง (กรณีตัวเลขสั้นลง/จอกว้างขึ้น)
    let size = parseFloat(getComputedStyle(el).fontSize);
    while (el.scrollWidth > el.clientWidth && size > STAT_MIN_FONT_PX) {
      size -= 0.5;
      el.style.fontSize = `${size}px`;
    }
  });
}

document.addEventListener("DOMContentLoaded", () => {
  const summaries = document.querySelectorAll(".stat-summary");
  if (summaries.length === 0) return;

  let queued = false;
  const refit = () => {
    if (queued) return;
    queued = true;
    requestAnimationFrame(() => {
      queued = false;
      summaries.forEach((s) => fitStatValues(s));
    });
  };

  // ค่าในช่องสรุปถูกเติมด้วย JS หลังโหลดข้อมูล -> ปรับขนาดเมื่อเนื้อหาเปลี่ยน
  const observer = new MutationObserver(refit);
  summaries.forEach((s) =>
    observer.observe(s, { childList: true, characterData: true, subtree: true }),
  );
  window.addEventListener("resize", refit);
  window.addEventListener("load", refit);

  // ช่องเปลี่ยนขนาด (เช่น sidebar โผล่, grid จัดใหม่) -> ปรับใหม่
  if ("ResizeObserver" in window) {
    const ro = new ResizeObserver(refit);
    summaries.forEach((s) => ro.observe(s));
  }

  // ฟอนต์เว็บ (Plus Jakarta Sans / Sarabun ของ ฿) โหลดเสร็จทีหลัง ตัวเลขจะกว้างขึ้นกว่าตอนวัดครั้งแรก
  // fonts.ready อาจจบไปก่อนฟอนต์ที่โหลดทีหลัง จึงฟัง loadingdone ทุกครั้งที่มีฟอนต์โหลดเสร็จด้วย
  if (document.fonts) {
    document.fonts.ready.then(refit);
    document.fonts.addEventListener("loadingdone", refit);
  }
  refit();
});

// ---------- สรุปจบทริป: ข้อความเดียวไว้แชร์ลงกลุ่ม (ใครโอนให้ใคร + ยอดรวม) ----------
// ใช้ได้ทุกหน้า: openTripSummary(tripId)
async function buildTripSummary(tripId) {
  const get = async (path) => {
    const res = await fetch(`${API_BASE}${path}`);
    if (!res.ok) throw new Error("โหลดข้อมูลไม่สำเร็จ");
    return res.json();
  };
  const [trip, members, expenses, transfers] = await Promise.all([
    get(`/trips/${tripId}`),
    get(`/trips/${tripId}/members`),
    get(`/expenses/trip/${tripId}`),
    get(`/debts/simplify/${tripId}`),
  ]);
  const name = (m) => m?.guestName || `สมาชิก #${m?.id}`;
  const total = expenses.reduce((s, e) => s + Number(e.totalAmount || 0), 0);
  const paid = {};
  const share = {};
  const byCat = {};
  expenses.forEach((e) => {
    if (e.user) paid[e.user.id] = (paid[e.user.id] || 0) + Number(e.totalAmount || 0);
    (e.splits || []).forEach((sp) => {
      if (sp.tripMember) share[sp.tripMember.id] = (share[sp.tripMember.id] || 0) + Number(sp.amountOwed || 0);
    });
    const cat = expenseCategoryOf(e.category).value;
    byCat[cat] = (byCat[cat] || 0) + Number(e.totalAmount || 0);
  });

  const day = (v) => new Date(`${String(v).slice(0, 10)}T00:00:00`);
  const range =
    trip.startDate && trip.endDate
      ? `${day(trip.startDate).toLocaleDateString("th-TH", { day: "numeric", month: "short" })} – ${day(
          trip.endDate,
        ).toLocaleDateString("th-TH", { day: "numeric", month: "short", year: "numeric" })}`
      : "";

  const lines = [];
  lines.push(`🧳 สรุปทริป "${trip.title || `ทริป #${tripId}`}"`);
  lines.push([range && `📅 ${range}`, `👥 ${members.length} คน`].filter(Boolean).join(" · "));
  lines.push("");
  lines.push(`💰 ค่าใช้จ่ายทั้งทริป ${fmt(Math.round(total * 100) / 100)} (${expenses.length} บิล)`);
  if (members.length > 0) lines.push(`เฉลี่ยคนละ ${fmt(Math.round(total / members.length))}`);
  lines.push("");

  const owing = (transfers || []).filter((t) => Number(t.amount) > 0.004);
  if (owing.length === 0) {
    lines.push("✅ เคลียร์ครบทุกคนแล้ว ไม่มีใครค้างใคร");
  } else {
    lines.push("💸 ใครต้องโอนให้ใคร");
    owing
      .sort((a, b) => Number(b.amount) - Number(a.amount))
      .forEach((t, i) => lines.push(`${i + 1}. ${name(t.from)} → ${name(t.to)} ${fmt(Number(t.amount))}`));
  }
  lines.push("");

  if (members.length > 0 && expenses.length > 0) {
    lines.push("👤 แต่ละคน (จ่ายไป · ส่วนของตัวเอง)");
    [...members]
      .sort((a, b) => (paid[b.id] || 0) - (paid[a.id] || 0))
      .forEach((m) => lines.push(`• ${name(m)}: ${fmt(paid[m.id] || 0)} · ${fmt(share[m.id] || 0)}`));
    lines.push("");
  }

  const cats = EXPENSE_CATEGORIES.filter((c) => byCat[c.value] > 0);
  if (cats.length > 0) {
    lines.push("📊 แยกตามหมวด");
    lines.push(cats.map((c) => `${c.label} ${fmt(byCat[c.value])}`).join(" · "));
    lines.push("");
  }
  lines.push("ส่งจาก Trip Splitter");
  return lines.join("\n");
}

// คัดลอกข้อความ (clipboard API อาจค้าง/ไม่ได้รับอนุญาต -> สำรองด้วย textarea)
async function copyText(text) {
  try {
    await Promise.race([
      navigator.clipboard.writeText(text),
      new Promise((_, reject) => setTimeout(() => reject(new Error("timeout")), 1500)),
    ]);
    return true;
  } catch {
    const area = document.createElement("textarea");
    area.value = text;
    area.setAttribute("readonly", "");
    area.style.cssText = "position:fixed;opacity:0;top:0;left:0";
    document.body.appendChild(area);
    area.select();
    let ok = false;
    try {
      ok = document.execCommand("copy");
    } catch {}
    area.remove();
    return ok;
  }
}

async function openTripSummary(tripId) {
  let dialog = document.getElementById("trip-summary-dialog");
  if (!dialog) {
    dialog = document.createElement("dialog");
    dialog.id = "trip-summary-dialog";
    dialog.className = "summary-dialog";
    dialog.setAttribute("aria-labelledby", "trip-summary-title");
    dialog.innerHTML = `
      <div class="summary-head">
        <span id="trip-summary-title">สรุปทริปไว้แชร์</span>
        <button type="button" class="summary-close" aria-label="ปิด">&times;</button>
      </div>
      <pre class="summary-text" id="trip-summary-text">กำลังสรุป...</pre>
      <div class="summary-actions">
        <button type="button" class="summary-btn" data-act="copy"><i class="fa-regular fa-copy"></i> คัดลอก</button>
        <a class="summary-btn line" data-act="line" target="_blank" rel="noopener"><i class="fa-brands fa-line"></i> ส่งเข้า LINE</a>
        <button type="button" class="summary-btn" data-act="share" hidden><i class="fa-solid fa-share-nodes"></i> แชร์</button>
      </div>`;
    document.body.appendChild(dialog);
    dialog.querySelector(".summary-close").addEventListener("click", () => dialog.close());
    dialog.addEventListener("click", (e) => {
      if (e.target === dialog) dialog.close();
    });
    dialog.querySelector('[data-act="copy"]').addEventListener("click", async () => {
      const ok = await copyText(dialog.dataset.text || "");
      showToast(ok ? "คัดลอกสรุปแล้ว วางในแชทได้เลย" : "คัดลอกไม่สำเร็จ ลองกดค้างที่ข้อความแล้วคัดลอกเอง", ok ? "success" : "error");
    });
    dialog.querySelector('[data-act="share"]').addEventListener("click", () => {
      navigator.share({ text: dialog.dataset.text || "" }).catch(() => {});
    });
  }
  const pre = dialog.querySelector("#trip-summary-text");
  const line = dialog.querySelector('[data-act="line"]');
  pre.textContent = "กำลังสรุป...";
  dialog.dataset.text = "";
  line.removeAttribute("href");
  dialog.querySelector('[data-act="share"]').hidden = !navigator.share;
  if (!dialog.open) dialog.showModal();
  try {
    const text = await buildTripSummary(tripId);
    pre.textContent = text;
    dialog.dataset.text = text;
    line.href = `https://line.me/R/share?text=${encodeURIComponent(text)}`;
  } catch (error) {
    pre.textContent = error.message || "สรุปไม่สำเร็จ ลองใหม่อีกครั้ง";
  }
}

// ---------- เตือนตั้ง PIN: ไม่มี PIN แล้วเปลี่ยนเครื่อง/ล้างเบราว์เซอร์ = เข้าชื่อนี้ไม่ได้อีก ----------
// แสดงบนสุดของ container (กด "ไว้ทีหลัง" = ซ่อน 3 วัน)
function showPinNudge(container) {
  let pinSet = "1";
  let snoozed = 0;
  try {
    pinSet = localStorage.getItem("pinSet");
    snoozed = Number(localStorage.getItem("pinNudgeUntil") || 0);
  } catch {}
  if (!container || pinSet !== "0" || Date.now() < snoozed || document.getElementById("pin-nudge")) return;
  const box = document.createElement("div");
  box.id = "pin-nudge";
  box.className = "pin-nudge";
  box.setAttribute("role", "status");
  box.innerHTML = `
    <i class="fa-solid fa-shield-halved"></i>
    <div class="pin-nudge-text">
      <strong>ตั้ง PIN กันลืม</strong>
      <span>ถ้าเปลี่ยนมือถือหรือล้างเบราว์เซอร์ จะใช้ PIN เข้าชื่อนี้และทริปเดิมได้</span>
    </div>
    <a class="pin-nudge-btn" href="home.html?setPin=1">ตั้ง PIN</a>
    <button type="button" class="pin-nudge-later" aria-label="ไว้ทีหลัง">ไว้ทีหลัง</button>`;
  box.querySelector(".pin-nudge-later").addEventListener("click", () => {
    try {
      localStorage.setItem("pinNudgeUntil", String(Date.now() + 3 * 86400000));
    } catch {}
    box.remove();
  });
  container.prepend(box);
}
