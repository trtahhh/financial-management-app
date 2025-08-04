console.log("Dashboard JS loaded");

const userId = 1; // User ID for API calls
let pieChart, barChart;

document.addEventListener('DOMContentLoaded', function () {
  const monthInput = document.getElementById('dash-month');

  // **SỬA LẠI HOÀN TOÀN: Sử dụng endpoint dashboard mới**
  function fetchDashboardData(month) {
    const [year, monthNum] = month.split('-').map(Number);
    const url = `http://localhost:8080/api/dashboard/data?month=${monthNum}&year=${year}`;
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

  function fetchTransactions() {
    const url = `http://localhost:8080/api/transactions`;
    console.log("📡 Fetching transactions from:", url);
    
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
      mode: 'cors'
    })
      .then(res => {
        console.log("🔍 Transactions response status:", res.status);
        if (!res.ok) {
          return res.text().then(text => { 
            console.error("❌ Transactions error:", text);
            throw new Error(`HTTP ${res.status}: ${text}`); 
          });
        }
        return res.json();
      })
      .then(data => {
        console.log("✅ Transactions data received:", data);
        // Filter by userId on frontend
        return data.filter(t => t.userId === userId);
      })
      .catch(err => {
        console.error("🚨 Transactions fetch failed:", err);
        throw err;
      });
  }

  function fetchCategories() {
    const url = `http://localhost:8080/api/categories`;
    console.log("📡 Fetching categories from:", url);
    
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
      mode: 'cors'
    })
      .then(res => {
        console.log("🔍 Categories response status:", res.status);
        if (!res.ok) {
          return res.text().then(text => { 
            console.error("❌ Categories error:", text);
            throw new Error(`HTTP ${res.status}: ${text}`); 
          });
        }
        return res.json();
      })
      .then(data => {
        console.log("✅ Categories data received:", data);
        // Filter by userId on frontend if needed
        return data.filter(c => c.userId === userId || !c.userId);
      })
      .catch(err => {
        console.error("🚨 Categories fetch failed:", err);
        throw err;
      });
  }

  function renderStats(data) {
    console.log("✅ Rendering stats:", data);
    document.getElementById('totalIncome').textContent = (data.totalIncome || 0).toLocaleString('vi-VN') + ' đ';
    document.getElementById('totalExpense').textContent = (data.totalExpense || 0).toLocaleString('vi-VN') + ' đ';
    document.getElementById('balance').textContent = (data.balance || 0).toLocaleString('vi-VN') + ' đ';
  }

  function initCharts() {
    Promise.all([fetchTransactions(), fetchCategories()])
      .then(([transactions, categories]) => {
        renderPieChart(transactions, categories);
        renderBarChart(transactions);
      })
      .catch(err => {
        console.error("Error loading chart data:", err);
        showChartError("Không thể tải dữ liệu biểu đồ: " + err.message);
      });
  }

  function renderPieChart(transactions, categories) {
    // Create category map
    const categoryMap = {};
    categories.forEach(cat => {
      categoryMap[cat.id] = cat.name;
    });
    
    console.log("📊 Rendering pie chart with transactions:", transactions);
    console.log("📊 Category map:", categoryMap);
    
    // Calculate expenses by category from real data
    const expensesByCategory = {};
    
    transactions
      .filter(t => {
        const isExpense = t.type === 'CHI' || t.type === 'EXPENSE' || t.type === 'expense';
        console.log(`Transaction ${t.id}: type=${t.type}, isExpense=${isExpense}`);
        return isExpense;
      })
      .forEach(t => {
        const categoryName = categoryMap[t.categoryId] || t.category?.name || t.category || 'Khác';
        const amount = t.amount || 0;
        expensesByCategory[categoryName] = (expensesByCategory[categoryName] || 0) + amount;
        console.log(`Added ${amount} to category ${categoryName}`);
      });

    console.log("📊 Expenses by category:", expensesByCategory);

    const ctx = document.getElementById('chart-pie').getContext('2d');
    if (pieChart) pieChart.destroy();
    
    if (Object.keys(expensesByCategory).length === 0) {
      // Show empty state
      ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
      ctx.fillStyle = '#6c757d';
      ctx.font = '16px Arial';
      ctx.textAlign = 'center';
      ctx.fillText('Chưa có dữ liệu chi tiêu', ctx.canvas.width / 2, ctx.canvas.height / 2);
      return;
    }
    
    pieChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: Object.keys(expensesByCategory),
        datasets: [{
          data: Object.values(expensesByCategory),
          backgroundColor: [
            '#28a745', '#dc3545', '#ffc107', '#17a2b8',
            '#6f42c1', '#fd7e14', '#20c997', '#6c757d'
          ],
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
                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                const percentage = ((value / total) * 100).toFixed(1);
                return `${context.label}: ${value.toLocaleString('vi-VN')} đ (${percentage}%)`;
              }
            }
          }
        }
      }
    });
  }

  function renderBarChart(transactions) {
    // Calculate weekly data from real transactions for current month
    const currentMonth = new Date().getMonth() + 1;
    const currentYear = new Date().getFullYear();
    
    console.log("📊 Rendering bar chart for month:", currentMonth, "year:", currentYear);
    console.log("📊 All transactions:", transactions);
    
    const weeklyData = { income: [0,0,0,0], expense: [0,0,0,0] };
    
    transactions
      .filter(t => {
        const tDate = new Date(t.date);
        const isCurrentMonth = tDate.getMonth() + 1 === currentMonth && tDate.getFullYear() === currentYear;
        console.log(`Transaction ${t.id}: date=${t.date}, isCurrentMonth=${isCurrentMonth}`);
        return isCurrentMonth;
      })
      .forEach(t => {
        const date = new Date(t.date);
        const week = Math.floor((date.getDate() - 1) / 7);
        const weekIndex = Math.min(week, 3);
        
        const isIncome = t.type === 'THU' || t.type === 'INCOME' || t.type === 'income';
        const isExpense = t.type === 'CHI' || t.type === 'EXPENSE' || t.type === 'expense';
        
        console.log(`Transaction ${t.id}: type=${t.type}, isIncome=${isIncome}, isExpense=${isExpense}, week=${weekIndex}, amount=${t.amount}`);
        
        if (isIncome) {
          weeklyData.income[weekIndex] += (t.amount || 0);
        } else if (isExpense) {
          weeklyData.expense[weekIndex] += (t.amount || 0);
        }
      });

    console.log("📊 Weekly data:", weeklyData);

    const ctx = document.getElementById('chart-bar').getContext('2d');
    if (barChart) barChart.destroy();
    
    barChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: ['Tuần 1', 'Tuần 2', 'Tuần 3', 'Tuần 4'],
        datasets: [{
          label: 'Thu nhập',
          data: weeklyData.income,
          backgroundColor: '#28a745',
          borderRadius: 4
        }, {
          label: 'Chi tiêu',
          data: weeklyData.expense,
          backgroundColor: '#dc3545',
          borderRadius: 4
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top',
            labels: {
              usePointStyle: true,
              padding: 20
            }
          },
          tooltip: {
            callbacks: {
              label: function(context) {
                return `${context.dataset.label}: ${context.parsed.y.toLocaleString('vi-VN')} đ`;
              }
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: {
              callback: function(value) {
                return value.toLocaleString('vi-VN') + ' đ';
              }
            }
          }
        }
      }
    });
  }

  function showChartError(message) {
    // Show error in both chart containers
    ['chart-pie', 'chart-bar'].forEach(chartId => {
      const canvas = document.getElementById(chartId);
      const ctx = canvas.getContext('2d');
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.fillStyle = '#dc3545';
      ctx.font = '14px Arial';
      ctx.textAlign = 'center';
      ctx.fillText(message, canvas.width / 2, canvas.height / 2);
    });
  }

  function loadDashboard() {
    console.log("🔄 Đang load dữ liệu dashboard cho:", monthInput.value);
    
    // **SỬA LẠI: Sử dụng endpoint dashboard mới**
    fetchDashboardData(monthInput.value)
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
    
    // Cập nhật thông tin tháng hiện tại
    const monthText = `Tháng ${data.currentMonth}/${data.currentYear}`;
    const monthDisplay = document.querySelector('.dashboard-month-display');
    if (monthDisplay) {
      monthDisplay.textContent = monthText;
    }
    
    // Cập nhật thông tin ví
    updateWalletDisplay(data.wallets || []);
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
    }
    
    // Có thể thêm bar chart sau
    // renderBarChartFromData(data);
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
      // Show empty state
      ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
      ctx.fillStyle = '#6c757d';
      ctx.font = '16px Arial';
      ctx.textAlign = 'center';
      ctx.fillText('Chưa có dữ liệu chi tiêu', ctx.canvas.width / 2, ctx.canvas.height / 2);
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
      })
      .catch(e => {
        console.error("💥 Lỗi khi tải dữ liệu dashboard:", e);
        // Show error in stats
        document.getElementById('totalIncome').textContent = 'Lỗi';
        document.getElementById('totalExpense').textContent = 'Lỗi';
        document.getElementById('balance').textContent = 'Lỗi';
      });
  }

  function refreshCharts() {
    console.log("🔄 Refreshing charts...");
    initCharts();
  }

  // Set default month to current month
  if (monthInput) {
    const now = new Date();
    const currentMonth = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0');
    monthInput.value = currentMonth;
    monthInput.addEventListener('change', function() {
      loadDashboard(); // Sử dụng function mới thay vì load() và initCharts()
    });
  }
  
  // Initialize - GỌI DASHBOARD MỚI
  console.log("🚀 Initializing dashboard...");
  loadDashboard(); // Chỉ gọi function mới này thôi
});

// 🔗 ENHANCED INTEGRATION FUNCTIONS - Các hàm liên kết nâng cao

/**
 * Fetch budgets data
 */
function fetchBudgets(month) {
  const [year, monthNum] = month.split('-').map(Number);
  const url = `http://localhost:8080/api/budgets?userId=${userId}&month=${monthNum}&year=${year}`;
  
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
    mode: 'cors'
  })
    .then(res => res.ok ? res.json() : [])
    .catch(err => {
      console.error("🚨 Budgets fetch failed:", err);
      return [];
    });
}

/**
 * Fetch goals data
 */
function fetchGoals() {
  const url = `http://localhost:8080/api/goals?userId=${userId}`;
  
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
    mode: 'cors'
  })
    .then(res => res.ok ? res.json() : [])
    .catch(err => {
      console.error("🚨 Goals fetch failed:", err);
      return [];
    });
}

/**
 * Calculate enhanced statistics with cross-functional data
 */
function calculateEnhancedStats(stats, transactions, budgets, goals) {
  const currentDate = new Date();
  const currentMonth = currentDate.getMonth() + 1;
  const currentYear = currentDate.getFullYear();
  
  // Filter current month transactions
  const currentMonthTransactions = transactions.filter(t => {
    const transactionDate = new Date(t.date);
    return transactionDate.getMonth() + 1 === currentMonth && 
           transactionDate.getFullYear() === currentYear;
  });
  
  // Calculate budget usage
  const totalBudget = budgets.reduce((sum, b) => sum + (b.amount || 0), 0);
  const usedBudget = budgets.reduce((sum, b) => sum + (b.usedAmount || 0), 0);
  const budgetUsagePercent = totalBudget > 0 ? Math.round((usedBudget / totalBudget) * 100) : 0;
  
  // Calculate goals progress
  const totalGoalsTarget = goals.reduce((sum, g) => sum + (g.targetAmount || 0), 0);
  const totalGoalsProgress = goals.reduce((sum, g) => sum + (g.currentAmount || 0), 0);
  const goalsProgressPercent = totalGoalsTarget > 0 ? Math.round((totalGoalsProgress / totalGoalsTarget) * 100) : 0;
  
  return {
    ...stats,
    totalBudget: totalBudget,
    usedBudget: usedBudget,
    budgetUsagePercent: budgetUsagePercent,
    totalGoalsTarget: totalGoalsTarget,
    totalGoalsProgress: totalGoalsProgress,
    goalsProgressPercent: goalsProgressPercent,
    transactionCount: currentMonthTransactions.length
  };
}

/**
 * Update budget alerts on dashboard
 */
function updateBudgetAlerts(budgets) {
  const alertContainer = document.getElementById('budget-alerts');
  if (!alertContainer) return;
  
  const exceededBudgets = budgets.filter(b => (b.usedAmount || 0) > (b.amount || 0));
  const nearLimitBudgets = budgets.filter(b => {
    const usage = (b.usedAmount || 0) / (b.amount || 1);
    return usage >= 0.8 && usage <= 1.0;
  });
  
  let alertsHtml = '';
  
  if (exceededBudgets.length > 0) {
    alertsHtml += '<div class="alert alert-danger"><strong>⚠️ Vượt ngân sách:</strong> ';
    alertsHtml += exceededBudgets.map(b => b.categoryName).join(', ');
    alertsHtml += '</div>';
  }
  
  if (nearLimitBudgets.length > 0) {
    alertsHtml += '<div class="alert alert-warning"><strong>📊 Gần đạt giới hạn:</strong> ';
    alertsHtml += nearLimitBudgets.map(b => b.categoryName).join(', ');
    alertsHtml += '</div>';
  }
  
  alertContainer.innerHTML = alertsHtml;
}

/**
 * Update goal progress display
 */
function updateGoalProgress(goals, transactions) {
  const goalContainer = document.getElementById('goal-progress');
  if (!goalContainer || goals.length === 0) return;
  
  const savingsTransactions = transactions.filter(t => t.type === 'THU');
  const thisMonthSavings = savingsTransactions
    .filter(t => {
      const date = new Date(t.date);
      const now = new Date();
      return date.getMonth() === now.getMonth() && date.getFullYear() === now.getFullYear();
    })
    .reduce((sum, t) => sum + t.amount, 0);
  
  let goalHtml = '<h6>🎯 Tiến độ mục tiêu</h6>';
  goals.slice(0, 3).forEach(goal => {
    const progress = Math.min(((goal.currentAmount || 0) / (goal.targetAmount || 1)) * 100, 100);
    goalHtml += `
      <div class="mb-2">
        <div class="d-flex justify-content-between">
          <small>${goal.name}</small>
          <small>${progress.toFixed(1)}%</small>
        </div>
        <div class="progress" style="height: 8px;">
          <div class="progress-bar ${progress >= 100 ? 'bg-success' : progress >= 75 ? 'bg-info' : 'bg-warning'}" 
               style="width: ${progress}%"></div>
        </div>
      </div>
    `;
  });
  
  if (thisMonthSavings > 0) {
    goalHtml += `<small class="text-success">💰 Tháng này tiết kiệm: ${thisMonthSavings.toLocaleString('vi-VN')} đ</small>`;
  }
  
  goalContainer.innerHTML = goalHtml;
}

/**
 * Enhanced update stats with integrated data
 */
function updateStats(enhancedStats) {
  console.log("✅ Rendering enhanced stats:", enhancedStats);
  
  // Basic stats
  document.getElementById('totalIncome').textContent = (enhancedStats.totalIncome || 0).toLocaleString('vi-VN') + ' đ';
  document.getElementById('totalExpense').textContent = (enhancedStats.totalExpense || 0).toLocaleString('vi-VN') + ' đ';
  document.getElementById('balance').textContent = (enhancedStats.balance || 0).toLocaleString('vi-VN') + ' đ';
  
  // Enhanced stats
  const budgetUsageEl = document.getElementById('budget-usage');
  if (budgetUsageEl) {
    budgetUsageEl.innerHTML = `
      <div class="text-center">
        <div class="h5 mb-0">${enhancedStats.budgetUsagePercent}%</div>
        <small class="text-muted">Đã sử dụng ngân sách</small>
        <div class="progress mt-2" style="height: 8px;">
          <div class="progress-bar ${enhancedStats.budgetUsagePercent > 100 ? 'bg-danger' : enhancedStats.budgetUsagePercent > 80 ? 'bg-warning' : 'bg-success'}" 
               style="width: ${Math.min(enhancedStats.budgetUsagePercent, 100)}%"></div>
        </div>
      </div>
    `;
  }
  
  const goalsProgressEl = document.getElementById('goals-progress');
  if (goalsProgressEl) {
    goalsProgressEl.innerHTML = `
      <div class="text-center">
        <div class="h5 mb-0">${enhancedStats.goalsProgressPercent}%</div>
        <small class="text-muted">Tiến độ mục tiêu</small>
        <div class="progress mt-2" style="height: 8px;">
          <div class="progress-bar bg-primary" style="width: ${Math.min(enhancedStats.goalsProgressPercent, 100)}%"></div>
        </div>
      </div>
    `;
  }
  
  // Update quick stats
  const transactionCountEl = document.getElementById('transaction-count');
  if (transactionCountEl) {
    transactionCountEl.textContent = enhancedStats.transactionCount || 0;
  }
  
  const avgTransactionEl = document.getElementById('average-transaction');
  if (avgTransactionEl && enhancedStats.transactionCount > 0) {
    const avgAmount = (enhancedStats.totalExpense + enhancedStats.totalIncome) / enhancedStats.transactionCount;
    avgTransactionEl.textContent = avgAmount.toLocaleString('vi-VN') + 'đ';
  }
}

/**
 * Update recent transactions display
 */
function updateRecentTransactions(transactions) {
  const container = document.getElementById('recent-transactions');
  if (!container || !transactions.length) {
    if (container) {
      container.innerHTML = `
        <div class="text-center text-muted">
          <i class="fas fa-receipt fa-2x mb-2"></i>
          <p>Chưa có giao dịch nào. <a href="/transactions" class="text-success">Thêm giao dịch đầu tiên</a>?</p>
        </div>
      `;
    }
    return;
  }
  
  const recentTransactionsHtml = transactions.map(tx => {
    const isIncome = tx.type === 'THU';
    const amountClass = isIncome ? 'text-success' : 'text-danger';
    const amountPrefix = isIncome ? '+' : '-';
    const date = new Date(tx.date).toLocaleDateString('vi-VN');
    
    return `
      <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
        <div class="d-flex align-items-center">
          <div class="me-3">
            <i class="fas ${isIncome ? 'fa-arrow-up' : 'fa-arrow-down'} ${amountClass}"></i>
          </div>
          <div>
            <div class="fw-bold">${tx.note || 'Giao dịch'}</div>
            <small class="text-muted">${tx.category || 'Khác'} • ${date}</small>
          </div>
        </div>
        <div class="${amountClass} fw-bold">
          ${amountPrefix}${(tx.amount || 0).toLocaleString('vi-VN')}đ
        </div>
      </div>
    `;
  }).join('');
  
  container.innerHTML = recentTransactionsHtml;
}
