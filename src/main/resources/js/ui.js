// ตัวช่วย UI ที่ใช้ร่วมกันหลายหน้า (expenses, debts)
// สไตล์ที่คู่กัน (.avatar, .toast, .skeleton, .stat-*) อยู่ใน css/all.css

// จัดรูปแบบเงินบาท เช่น 1234.5 -> ฿1,234.5
function fmt(n) {
  return `฿${Number(n || 0).toLocaleString(undefined, {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  })}`;
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
