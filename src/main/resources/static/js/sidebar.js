// Sidebar สำหรับจอคอม (≥1024px) — ใช้ร่วมกันทุกหน้าในทริป
// แสดง/ซ่อนด้วย CSS (.app-sidebar ใน all.css) มือถือและแท็บเล็ตจะไม่เห็น
// ห่อด้วย IIFE เพราะแต่ละหน้าประกาศ const tripId / API_BASE_URL ไว้เองแล้ว ถ้าประกาศซ้ำระดับบนสุดจะ error
(function () {
  const API_BASE_URL = API_BASE; // ตั้งที่ ui.js
  const tripId = new URLSearchParams(window.location.search).get("tripId") || 1;
  const currentPage = window.location.pathname.split("/").pop() || "dashboard.html";

  const NAV_ITEMS = [
    { href: "dashboard.html", icon: "fa-solid fa-house", label: "ภาพรวม" },
    { href: "plan.html", icon: "fa-solid fa-route", label: "แพลนเที่ยว" },
    { href: "expenses.html", icon: "fa-solid fa-receipt", label: "ค่าใช้จ่าย" },
    { href: "debts.html", icon: "fa-solid fa-scale-balanced", label: "สรุปหนี้" },
    { href: "polls.html", icon: "fa-solid fa-square-poll-vertical", label: "โหวต" },
    { href: "checklist.html", icon: "fa-solid fa-list-check", label: "เช็คลิสต์" },
  ];

  function buildSidebar() {
    const aside = document.createElement("aside");
    aside.className = "app-sidebar";
    aside.setAttribute("aria-label", "เมนูหลัก");

    const navLinks = NAV_ITEMS.map((item) => {
      const active = item.href === currentPage;
      return `
        <a href="${item.href}?tripId=${encodeURIComponent(tripId)}"
           class="sidebar-link${active ? " active" : ""}"
           ${active ? 'aria-current="page"' : ""}>
          <i class="${item.icon}"></i><span>${item.label}</span>
        </a>`;
    }).join("");

    aside.innerHTML = `
      <a class="sidebar-brand" href="home.html">
        <i class="fa-solid fa-suitcase-rolling"></i> Trip Splitter
      </a>

      <div class="sidebar-trip">
        <span class="sidebar-label">ทริปปัจจุบัน</span>
        <strong class="sidebar-trip-title">กำลังโหลด...</strong>
        <div class="sidebar-invite">
          <div>
            <span class="sidebar-label">รหัสเชิญ</span>
            <code class="sidebar-invite-code">------</code>
          </div>
          <button type="button" class="sidebar-copy-btn" title="คัดลอกรหัสเชิญ">
            <i class="fa-regular fa-copy"></i>
          </button>
        </div>
      </div>

      <nav class="sidebar-nav">${navLinks}</nav>

      <div class="sidebar-footer">
        <div class="sidebar-user">
          <i class="fa-solid fa-circle-user"></i>
          <span class="sidebar-username"></span>
        </div>
        <a href="home.html" class="sidebar-link">
          <i class="fa-solid fa-arrow-left"></i><span>กลับสู่หน้าแรก</span>
        </a>
      </div>
    `;

    // ข้อมูลจากผู้ใช้/ฐานข้อมูล ใส่ด้วย textContent กัน HTML แทรก
    aside.querySelector(".sidebar-username").textContent =
      localStorage.getItem("username") || "Guest";

    const copyBtn = aside.querySelector(".sidebar-copy-btn");
    copyBtn.addEventListener("click", () => {
      const code = aside.querySelector(".sidebar-invite-code").textContent.trim();
      if (!code || code === "------") return;
      navigator.clipboard.writeText(code).then(() => {
        copyBtn.classList.add("success");
        copyBtn.innerHTML = '<i class="fa-solid fa-check"></i>';
        setTimeout(() => {
          copyBtn.classList.remove("success");
          copyBtn.innerHTML = '<i class="fa-regular fa-copy"></i>';
        }, 1200);
      });
    });

    document.body.prepend(aside);
    document.body.classList.add("has-sidebar");
    return aside;
  }

  async function loadTripInfo(aside) {
    const titleEl = aside.querySelector(".sidebar-trip-title");
    try {
      const res = await fetch(`${API_BASE_URL}/trips/${tripId}`);
      if (!res.ok) throw new Error("ไม่สามารถดึงข้อมูลทริปได้");
      const trip = await res.json();
      titleEl.textContent = trip.title || `ทริป #${tripId}`;
      aside.querySelector(".sidebar-invite-code").textContent = trip.inviteCode || "------";
    } catch (error) {
      console.error("Sidebar: error loading trip:", error);
      titleEl.textContent = "ไม่พบข้อมูลทริป";
    }
  }

  function init() {
    loadTripInfo(buildSidebar());
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
