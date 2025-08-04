console.log("Dashboard JS loaded");

const userId = 1; // User ID for API calls
let pieChart, barChart;

document.addEventListener('DOMContentLoaded', function () {
  const dateFromInput = document.getElementById('dash-date-from');
  const dateToInput = document.getElementById('dash-date-to');

  // Set default values to current month
  const now = new Date();
  const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
  const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
  
  dateFromInput.value = firstDay.toISOString().split('T')[0];
  dateToInput.value = lastDay.toISOString().split('T')[0];

  // **SỬA LẠI HOÀN TOÀN: Sử dụng endpoint dashboard mới**
  function fetchDashboardData(params) {
    // Remove undefined values from params
    const cleanParams = {};
    Object.keys(params).forEach(key => {
      if (params[key] !== undefined && params[key] !== null && params[key] !== '') {
        cleanParams[key] = params[key];
      }
    });
    
    const url = `http://localhost:8080/api/dashboard/data?${new URLSearchParams(cleanParams)}`;
    console.log("📡 Fetching dashboard data from:", url);
    
    const token = localStorage.getItem('authToken');
    const headers = {
      'Content-Type': 'application/json'
    };
    if (token) {
      headers['Authorization'] = 'Bearer ' + token;
    }
    
    return fetch(url, { 
      method: 'GET',
      headers: headers,
      credentials: 'include', // Quan trọng: để gửi session cookies
      mode: 'cors'
    })
      .then(res => {
        console.log("🔍 Dashboard response status:", res.status);
        if (!res.ok) {
          return res.text().then(text => { 
            console.error("❌ Dashboard error:", text);
            throw new Error(`HTTP ${res.status}: ${text}`); 
          });
        }
        return res.json();
      })
      .then(data => {
        console.log("✅ Dashboard data received:", data);
        return data;
      })
      .catch(err => {
        console.error("🚨 Dashboard fetch failed:", err);
        throw err;
      });
  }
  
  function loadDashboard() {
    console.log("🔄 Đang load dữ liệu dashboard");
    
    // Xác định parameters để gửi API
    let params = {};
    
    if (dateFromInput.value && dateToInput.value) {
      // Always use date range since we have unified date picker
      params.dateFrom = dateFromInput.value;
      params.dateTo = dateToInput.value;
      console.log("📅 Using date range:", params.dateFrom, "to", params.dateTo);
    }
    
    // **SỬA LẠI: Sử dụng endpoint dashboard mới**
    fetchDashboardData(params)
      .then(dashboardData => {
        console.log("📊 Dashboard data loaded:", dashboardData);
        
        // Update UI với dữ liệu mới
        updateDashboardUI(dashboardData);
        
        // Update charts với dữ liệu mới
        updateChartsWithNewData(dashboardData);
        
      })
      .catch(err => {
        console.error("❌ Error loading dashboard:", err);
        showError("Không thể tải dữ liệu dashboard: " + err.message);
      });
  }
  
  function updateDashboardUI(data) {
    // Cập nhật các số liệu chính
    document.getElementById('totalIncome').textContent = (data.monthlyIncome || 0).toLocaleString('vi-VN') + ' đ';
    document.getElementById('totalExpense').textContent = (data.monthlyExpense || 0).toLocaleString('vi-VN') + ' đ';
    document.getElementById('balance').textContent = (data.totalBalance || 0).toLocaleString('vi-VN') + ' đ';
    
    // Cập nhật phần trăm ngân sách
    updateBudgetUsage(data);
    
    // Cập nhật thông tin tháng hiện tại
    const monthText = `Tháng ${data.currentMonth}/${data.currentYear}`;
    const monthDisplay = document.querySelector('.dashboard-month-display');
    if (monthDisplay) {
      monthDisplay.textContent = monthText;
    }
    
    // Cập nhật thông tin ví
    updateWalletDisplay(data.wallets || []);
    
    // Cập nhật thống kê nhanh
    updateQuickStats(data);
    
    // Cập nhật giao dịch gần đây
    updateRecentTransactions(data.transactions || []);
    
    // Cập nhật tiến độ mục tiêu và cảnh báo ngân sách
    updateGoalsAndBudgets(data);
  }
  
  function updateBudgetUsage(data) {
    const budgetUsageElement = document.getElementById('budget-usage');
    if (!budgetUsageElement) return;
    
    if (data.budgetWarnings && data.budgetWarnings.length > 0) {
      // Tính phần trăm trung bình của tất cả ngân sách
      const totalPercentage = data.budgetWarnings.reduce((sum, w) => sum + w.percentage, 0);
      const avgPercentage = Math.round(totalPercentage / data.budgetWarnings.length);
      
      const colorClass = avgPercentage >= 100 ? 'text-danger' : avgPercentage >= 80 ? 'text-warning' : 'text-success';
      
      budgetUsageElement.innerHTML = `
        <h4 class="${colorClass} mb-0">${avgPercentage}%</h4>
        <small class="text-muted">Đã sử dụng</small>
      `;
    } else {
      budgetUsageElement.innerHTML = `
        <h4 class="text-success mb-0">0%</h4>
        <small class="text-muted">Chưa có ngân sách</small>
      `;
    }
  }
  
  function updateQuickStats(data) {
    console.log("🔍 updateQuickStats called with data:", data);
    console.log("🔍 transactions data:", data.transactions);
    
    const transactionCount = (data.transactions || []).length;
    const totalIncome = data.monthlyIncome || 0;
    const totalExpense = data.monthlyExpense || 0;
    const avgTransaction = transactionCount > 0 ? 
      (totalIncome + totalExpense) / transactionCount : 0;
    
    console.log("📊 Transaction count:", transactionCount);
    console.log("📊 Average transaction:", avgTransaction);
    
    const countElement = document.getElementById('transaction-count');
    const avgElement = document.getElementById('average-transaction');
    
    if (countElement) {
      countElement.textContent = transactionCount;
      console.log("✅ Updated transaction count to:", transactionCount);
    }
    if (avgElement) {
      avgElement.textContent = avgTransaction.toLocaleString('vi-VN') + 'đ';
      console.log("✅ Updated average transaction to:", avgTransaction.toLocaleString('vi-VN') + 'đ');
    }
  }
  
  function updateRecentTransactions(transactions) {
    console.log("🔍 updateRecentTransactions called with:", transactions);
    
    const container = document.getElementById('recent-transactions');
    if (!container) {
      console.log("❌ recent-transactions container not found");
      return;
    }
    
    if (!transactions || transactions.length === 0) {
      console.log("📝 No transactions, showing empty state");
      container.innerHTML = `
        <div class="text-center text-muted">
          <i class="fas fa-clipboard-list fa-2x mb-2"></i>
          <p>Chưa có giao dịch nào trong tháng này</p>
          <a href="/transactions" class="btn btn-success btn-sm">
            <i class="fas fa-plus"></i> Thêm giao dịch đầu tiên
          </a>
        </div>
      `;
      return;
    }
    
    console.log("📝 Found", transactions.length, "transactions, displaying...");
    const recentList = transactions.slice(0, 5); // Chỉ hiển thị 5 giao dịch gần nhất
    const transactionsHTML = recentList.map(t => `
      <div class="d-flex justify-content-between align-items-center border-bottom py-2">
        <div>
          <strong>${t.note || 'Giao dịch'}</strong>
          <br>
          <small class="text-muted">${t.categoryName || 'Không phân loại'} • ${formatDate(t.date)}</small>
        </div>
        <div class="text-end">
          <span class="fw-bold ${t.type === 'income' ? 'text-success' : 'text-danger'}">
            ${t.type === 'income' ? '+' : '-'}${(t.amount || 0).toLocaleString('vi-VN')}đ
          </span>
        </div>
      </div>
    `).join('');
    
    container.innerHTML = transactionsHTML;
    console.log("✅ Updated recent transactions display");
  }
  
  function updateGoalsAndBudgets(data) {
    // Cập nhật tiến độ mục tiêu
    const goalProgress = document.getElementById('goal-progress');
    if (goalProgress) {
      if (data.goalProgress && data.goalProgress.length > 0) {
        console.log("🎯 Goal progress found:", data.goalProgress);
        const goalsHTML = data.goalProgress.map(goal => {
          const percentage = parseFloat(goal.progressPercentage || 0);
          let progressBarClass = 'bg-success';
          if (percentage >= 100) {
            progressBarClass = 'bg-success';
          } else if (percentage >= 80) {
            progressBarClass = 'bg-warning';
          } else {
            progressBarClass = 'bg-info';
          }
          
          return `
            <div class="goal-item mb-3 p-3 border rounded">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <h6 class="mb-0">${goal.goalName}</h6>
                <span class="badge ${goal.status === 'completed' ? 'bg-success' : 
                                    goal.status === 'near-completion' ? 'bg-warning' : 'bg-info'}">
                  ${goal.status === 'completed' ? 'Hoàn thành' : 
                    goal.status === 'near-completion' ? 'Gần hoàn thành' : 'Đang thực hiện'}
                </span>
              </div>
              <div class="progress mb-2" style="height: 8px;">
                <div class="progress-bar ${progressBarClass}" role="progressbar" 
                     style="width: ${Math.min(percentage, 100)}%" 
                     aria-valuenow="${percentage}" aria-valuemin="0" aria-valuemax="100">
                </div>
              </div>
              <div class="d-flex justify-content-between">
                <small class="text-muted">
                  Đã tích lũy: <span class="text-primary">${(goal.currentAmount || 0).toLocaleString('vi-VN')}đ</span>
                </small>
                <small class="text-muted">
                  Mục tiêu: <span class="text-success">${goal.targetAmount.toLocaleString('vi-VN')}đ</span>
                </small>
              </div>
              <div class="text-center mt-2">
                <small class="fw-bold ${percentage >= 100 ? 'text-success' : 'text-warning'}">
                  ${percentage.toFixed(1)}% - 
                  ${goal.remainingAmount > 0 ? 
                    `Còn thiếu ${goal.remainingAmount.toLocaleString('vi-VN')}đ` : 
                    'Đã đạt mục tiêu!'}
                </small>
              </div>
            </div>
          `;
        }).join('');
        goalProgress.innerHTML = goalsHTML;
      } else {
        // Nếu không có mục tiêu, hiển thị thông báo
        goalProgress.innerHTML = `
          <div class="text-center text-muted">
            <p>Chưa có mục tiêu tài chính nào</p>
            <a href="/goals" class="btn btn-primary btn-sm">
              Tạo mục tiêu
            </a>
          </div>
        `;
      }
    }
    
    // Cập nhật cảnh báo ngân sách
    const budgetAlerts = document.getElementById('budget-alerts');
    if (budgetAlerts) {
      if (data.budgetWarnings && data.budgetWarnings.length > 0) {
        console.log("🚨 Budget warnings found:", data.budgetWarnings);
        const warningsHTML = data.budgetWarnings.map(w => `
          <div class="alert ${w.status === 'exceeded' ? 'alert-danger' : 'alert-warning'} alert-sm mb-2">
            <div class="d-flex justify-content-between align-items-center">
              <div>
                <strong>${w.categoryName}</strong><br>
                <small>${w.message}</small>
              </div>
              <div class="text-end">
                <span class="badge ${w.status === 'exceeded' ? 'bg-danger' : 'bg-warning'}">${w.percentage}%</span><br>
                <small class="text-muted">${w.spentAmount.toLocaleString('vi-VN')}đ / ${w.budgetAmount.toLocaleString('vi-VN')}đ</small>
              </div>
            </div>
          </div>
        `).join('');
        budgetAlerts.innerHTML = warningsHTML;
      } else {
        // Nếu không có cảnh báo, hiển thị trạng thái tốt
        budgetAlerts.innerHTML = `
          <div class="text-center text-muted">
            <p>Tất cả ngân sách đều trong tầm kiểm soát</p>
            <a href="/budgets" class="btn btn-warning btn-sm">
              Xem ngân sách
            </a>
          </div>
        `;
      }
    }
  }
  
  function formatDate(dateStr) {
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString('vi-VN');
    } catch {
      return dateStr;
    }
  }
  
  function updateWalletDisplay(wallets) {
    const walletContainer = document.querySelector('.wallet-summary');
    if (walletContainer && wallets.length > 0) {
      const walletHTML = wallets.map(wallet => 
        `<div class="wallet-item">
          <span class="wallet-name">${wallet.name}</span>
          <span class="wallet-balance">${wallet.balance.toLocaleString('vi-VN')} đ</span>
        </div>`
      ).join('');
      walletContainer.innerHTML = walletHTML;
    }
  }
  
  function updateChartsWithNewData(data) {
    // Cập nhật pie chart với category expenses
    if (data.categoryExpenses && Object.keys(data.categoryExpenses).length > 0) {
      renderPieChartFromData(data.categoryExpenses);
    } else {
      // Show empty chart
      showEmptyChart('chart-pie', 'Chưa có dữ liệu chi tiêu');
    }
  }
  
  function renderPieChartFromData(categoryExpenses) {
    const ctx = document.getElementById('chart-pie')?.getContext('2d');
    if (!ctx) {
      console.error("❌ Không tìm thấy canvas chart-pie");
      return;
    }
    
    console.log("📊 Rendering pie chart with data:", categoryExpenses);
    
    // Destroy existing chart
    if (pieChart) {
      pieChart.destroy();
    }
    
    const labels = Object.keys(categoryExpenses);
    const amounts = Object.values(categoryExpenses);
    
    if (labels.length === 0) {
      showEmptyChart('chart-pie', 'Chưa có dữ liệu chi tiêu');
      return;
    }
    
    const colors = generateColors(labels.length);
    
    pieChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: amounts,
          backgroundColor: colors,
          borderWidth: 2,
          borderColor: '#fff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              padding: 15,
              usePointStyle: true,
              font: {
                size: 12
              }
            }
          },
          tooltip: {
            callbacks: {
              label: function(context) {
                const value = context.parsed;
                const total = amounts.reduce((sum, amt) => sum + amt, 0);
                const percentage = ((value / total) * 100).toFixed(1);
                return `${context.label}: ${value.toLocaleString('vi-VN')} đ (${percentage}%)`;
              }
            }
          }
        }
      }
    });
  }
  
  function generateColors(count) {
    const colors = [
      '#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', 
      '#9966FF', '#FF9F40', '#FF6384', '#C9CBCF'
    ];
    return Array.from({length: count}, (_, i) => colors[i % colors.length]);
  }
  
  function showEmptyChart(chartId, message) {
    const canvas = document.getElementById(chartId);
    if (!canvas) return;
    
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = '#6c757d';
    ctx.font = '16px Arial';
    ctx.textAlign = 'center';
    ctx.fillText(message, canvas.width / 2, canvas.height / 2);
  }
  
  function showError(message) {
    // Hiển thị lỗi cho user
    const errorDiv = document.querySelector('.error-message') || document.createElement('div');
    errorDiv.className = 'error-message';
    errorDiv.textContent = message;
    errorDiv.style.cssText = 'color: red; padding: 10px; background: #ffebee; border-radius: 4px; margin: 10px 0;';
    
    const container = document.querySelector('.dashboard-container') || document.body;
    container.insertBefore(errorDiv, container.firstChild);
    
    // Tự động ẩn sau 5 giây
    setTimeout(() => errorDiv.remove(), 5000);
  }

  // Add event listeners for date inputs
  if (dateFromInput && dateToInput) {
    dateFromInput.addEventListener('change', function() {
      if (dateFromInput.value && dateToInput.value) {
        loadDashboard();
      }
    });
    
    dateToInput.addEventListener('change', function() {
      if (dateFromInput.value && dateToInput.value) {
        loadDashboard();
      }
    });
  }
  
  // Initialize - GỌI DASHBOARD MỚI
  console.log("🚀 Initializing dashboard...");
  loadDashboard(); // Chỉ gọi function mới này thôi
});
