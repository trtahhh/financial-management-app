console.log("Dashboard JS loaded");

const userId = 1;

document.addEventListener('DOMContentLoaded', function () {
  const monthInput = document.getElementById('dash-month');

  function fetchStats(month) {
    const [year, monthNum] = month.split('-').map(Number);
    const url = `http://localhost:3000/api/statistics/summary?userId=${userId}&month=${monthNum}&year=${year}`;
    console.log("📡 Fetching URL:", url);   // ✅ in URL để debug
    return fetch(url)
      .then(res => {
        console.log("🔍 Response status:", res.status); // ✅ in mã status HTTP
        if (!res.ok) return res.text().then(text => { 
          console.error("❌ Server error text:", text); // ✅ in chi tiết error trả về từ server
          throw new Error(text); 
        });
        return res.json();
      })
      .catch(err => {
        console.error("🚨 Fetch failed:", err); // ✅ in error nếu fetch bị fail
        throw err;
      });
  }

  function renderStats(data) {
    console.log("✅ Data nhận được:", data); // ✅ in dữ liệu nhận về để debug
    document.getElementById('totalIncome').textContent  = data.totalIncome?.toLocaleString()  ?? '0';
    document.getElementById('totalExpense').textContent = data.totalExpense?.toLocaleString() ?? '0';
    document.getElementById('balance').textContent      = data.balance?.toLocaleString()      ?? '0';
  }

  function load() {
    console.log("🔄 Đang load dữ liệu dashboard cho:", monthInput.value);
    fetchStats(monthInput.value)
      .then(renderStats)
      .catch(e => {
        console.error("💥 Lỗi dashboard:", e);
        alert('Lỗi dashboard: ' + e.message);
      });
  }

  monthInput.addEventListener('change', load);
  load();
});
