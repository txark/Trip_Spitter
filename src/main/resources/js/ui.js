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
