console.log("Dashboard JS loaded");

let pieChart, barChart;

// JWT Utils
function getUserIdFromToken() {
  try {
    const token = localStorage.getItem('authToken');
    if (!token) return null;
    
    // Decode JWT token (payload part only)
    const payload = token.split('.')[1];
    const decoded = JSON.parse(atob(payload));
    console.log("🔍 JWT payload:", decoded);
    return decoded.userId || decoded.sub || null;
  } catch (error) {
    console.error('Error extracting userId from token:', error);
    return null;
  }
}

document.addEventListener('DOMContentLoaded', function () {
  const dateFromInput = document.getElementById('dash-date-from');
  const dateToInput = document.getElementById('dash-date-to');
  
  // Set default dates: from first day of current month to TODAY
  const now = new Date();
  const currentYear = now.getFullYear(); 
  const currentMonth = now.getMonth(); // 0-11 (August = 7)
  
  const firstDay = new Date(currentYear, currentMonth, 1);
  const today = new Date(); // Ngày hôm nay thực tế
  
  if (dateFromInput) {
    dateFromInput.value = firstDay.toISOString().split('T')[0];
  }
  if (dateToInput) {
    dateToInput.value = today.toISOString().split('T')[0];
  }
  
  console.log("📅 Date range set to:", firstDay.toISOString().split('T')[0], "→", today.toISOString().split('T')[0]);

  // Lấy dữ liệu dashboard theo đúng khoảng ngày được chọn (from/to)
  function fetchDashboardData() {
    const from = document.getElementById('dash-date-from')?.value;
    const to = document.getElementById('dash-date-to')?.value;
    
    console.log("📅 Date range from frontend:", from, "→", to);
    
    // Sử dụng endpoint đúng từ backend
    let url;
    if (from && to) {
      // Nếu có date range, dùng data-by-date
      const userId = getUserIdFromToken();
      url = `http://localhost:8080/api/dashboard/data-by-date?userId=${encodeURIComponent(userId)}&dateFrom=${encodeURIComponent(from)}&dateTo=${encodeURIComponent(to)}`;
    } else {
      // Nếu không có date range, dùng endpoint chính
      url = `http://localhost:8080/api/dashboard/data`;
    }
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
      credentials: 'include',
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
        // No need to filter on frontend - backend already filters by user
        return data;
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
        // No need to filter on frontend - backend already handles user-specific data
        return data;
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
    // Sử dụng dữ liệu từ dashboard response thay vì fetch riêng
    fetchDashboardData()
      .then(data => {
        console.log("📊 Dashboard data for charts:", data);
        
        // Render biểu đồ tròn với dữ liệu từ backend
        if (data.expensesByCategory && data.expensesByCategory.length > 0) {
          renderPieChartFromData(data.expensesByCategory);
        } else {
          renderPieChartFromTransactions();
        }
        
        // Render biểu đồ cột với dữ liệu từ backend
        if (data.weeklyTrend && data.weeklyTrend.length > 0) {
          renderBarChartFromData(data.weeklyTrend);
        } else {
          renderBarChartFromTransactions();
        }
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

  function renderPieChartFromData(expensesByCategory) {
    console.log("📊 Rendering pie chart from backend data:", expensesByCategory);
    
    const ctx = document.getElementById('chart-pie').getContext('2d');
    if (pieChart) pieChart.destroy();
    
    if (expensesByCategory.length === 0) {
      showEmptyChart(ctx, 'Chưa có dữ liệu chi tiêu');
      return;
    }
    
    const labels = expensesByCategory.map(item => item.categoryName);
    const data = expensesByCategory.map(item => item.totalAmount);
    const colors = expensesByCategory.map(item => item.categoryColor || '#007bff');
    
    pieChart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: data,
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
              font: { size: 12 }
            }
          },
          tooltip: {
            callbacks: {
              label: function(context) {
                return `${context.label}: ${Number(context.parsed).toLocaleString('vi-VN')} đ`;
              }
            }
          }
        }
      }
    });
  }

  function renderBarChartFromData(weeklyTrend) {
    console.log("📊 Rendering bar chart from backend data:", weeklyTrend);
    
    const ctx = document.getElementById('chart-bar').getContext('2d');
    if (barChart) barChart.destroy();
    
    if (weeklyTrend.length === 0) {
      showEmptyChart(ctx, 'Chưa có dữ liệu thu chi theo tuần');
      return;
    }
    
    const labels = weeklyTrend.map(item => item.week);
    const incomeData = weeklyTrend.map(item => Number(item.income));
    const expenseData = weeklyTrend.map(item => Number(item.expense));
    
    barChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Thu nhập',
            data: incomeData,
            backgroundColor: '#28a745',
            borderColor: '#28a745',
            borderWidth: 1
          },
          {
            label: 'Chi tiêu',
            data: expenseData,
            backgroundColor: '#dc3545',
            borderColor: '#dc3545',
            borderWidth: 1
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'top',
            labels: { usePointStyle: true, padding: 20 }
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

  function renderPieChartFromTransactions() {
    // Fallback: sử dụng dữ liệu từ transactions nếu không có từ dashboard
    Promise.all([fetchTransactions(), fetchCategories()])
      .then(([transactions, categories]) => {
        renderPieChart(transactions, categories);
      })
      .catch(err => {
        console.error("Error loading fallback chart data:", err);
        showChartError("Không thể tải dữ liệu biểu đồ");
      });
  }

  function renderBarChartFromTransactions() {
    // Fallback: sử dụng dữ liệu từ transactions nếu không có từ dashboard
    fetchTransactions()
      .then(transactions => {
        renderBarChart(transactions);
      })
      .catch(err => {
        console.error("Error loading fallback chart data:", err);
        showChartError("Không thể tải dữ liệu biểu đồ");
      });
  }

  function showEmptyChart(ctx, message) {
    ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
    ctx.fillStyle = '#6c757d';
    ctx.font = '16px Arial';
    ctx.textAlign = 'center';
    ctx.fillText(message, ctx.canvas.width / 2, ctx.canvas.height / 2);
  }

  function loadDashboard() {
    console.log("🔄 Đang load dữ liệu dashboard...");
    console.log("🔍 User ID from token:", getUserIdFromToken());
    console.log("🔍 Auth token exists:", !!localStorage.getItem('authToken'));
    
    fetchDashboardData()
      .then(dashboardData => {
        console.log("📊 Dashboard data loaded:", dashboardData);
        
        // Update UI với dữ liệu mới
        updateDashboardUI(dashboardData);
        
        // Update charts với dữ liệu mới
        updateChartsWithNewData(dashboardData);

        // Update Goal progress card
        try {
          const goalsRaw = Array.isArray(dashboardData.goalProgress) ? dashboardData.goalProgress : (dashboardData.goals || []);
          // Chuẩn hóa tên và phần trăm tiến độ từ API backend (goalProgress)
          const goals = goalsRaw.map(g => ({
            name: g.goalName || g.name || 'Mục tiêu',
            targetAmount: Number(g.targetAmount || g.target_amount || 0),
            currentAmount: Number(g.currentAmount || g.current_amount || 0),
            progressPercentage: Number(g.progressPercentage || g.progress || 0)
          }));
          const recentTx = Array.isArray(dashboardData.recentTransactions) ? dashboardData.recentTransactions : [];
          const normalizedTx = recentTx.map(t => ({
            type: t.type === 'income' ? 'THU' : 'CHI',
            amount: Number(t.amount || 0),
            date: t.date
          }));
          updateGoalProgress(goals, normalizedTx);
        } catch (e) { console.warn('Goal progress render error:', e); }

        // Update Budget alerts card (đồng bộ field usagePercent/status từ backend)
        try {
          const alerts = Array.isArray(dashboardData.budgetWarnings) ? dashboardData.budgetWarnings : [];
          const container = document.getElementById('budget-alerts');
          if (container) {
            if (alerts.length === 0) {
              container.innerHTML = '<div class="text-center text-muted"><p>Tất cả ngân sách đều trong tầm kiểm soát</p><a href="/budgets" class="btn btn-warning btn-sm">Xem ngân sách</a></div>';
            } else {
              container.innerHTML = alerts.slice(0, 4).map(a => `
                <div class="d-flex justify-content-between align-items-center mb-2">
                  <small>${a.categoryName || 'Danh mục'}</small>
                  <span class="badge ${a.status === 'EXCEEDED' ? 'bg-danger' : 'bg-warning'}">${a.status === 'EXCEEDED' ? 'Vượt' : 'Cảnh báo'}</span>
                </div>
                <div class="progress mb-2" style="height: 6px;">
                  <div class="progress-bar ${Number(a.usagePercent||0) >= 100 ? 'bg-danger' : 'bg-warning'}" style="width: ${Math.min(Math.round(Number(a.usagePercent||0)), 100)}%"></div>
                </div>
              `).join('') + '<a href="/budgets" class="btn btn-warning btn-sm">Xem ngân sách</a>';
            }
          }
        } catch (e) { console.warn('Budget alerts render error:', e); }

        // Update quick stats card (tháng theo phạm vi chọn)
        try {
          const from = new Date(document.getElementById('dash-date-from')?.value);
          const to = new Date(document.getElementById('dash-date-to')?.value);
          const tx = Array.isArray(dashboardData.recentTransactions) ? dashboardData.recentTransactions : [];
          const inRangeCount = tx.filter(t => {
            const d = new Date(t.date);
            return d >= from && d <= to;
          }).length;
          const transactionCountEl = document.getElementById('transaction-count');
          if (transactionCountEl) transactionCountEl.textContent = inRangeCount;
          const avgTransactionEl = document.getElementById('average-transaction');
          if (avgTransactionEl) {
            const total = tx.reduce((s, t) => s + Number(t.amount || 0), 0);
            const avg = tx.length > 0 ? total / tx.length : 0;
            avgTransactionEl.textContent = avg.toLocaleString('vi-VN') + 'đ';
          }
        } catch (e) { console.warn('Quick stats render error:', e); }
        
      })
      .catch(err => {
        console.error("❌ Error loading dashboard:", err);
        showError("Không thể tải dữ liệu dashboard: " + err.message);
      });
  }
  
  function updateDashboardUI(data) {
    console.log("🔧 Dashboard data structure:", data); // Debug log
    
    // Lấy dữ liệu theo khoảng ngày; fallback monthlyStats nếu có
    const monthlyStats = data.monthlyStats || {};
    const income = (typeof data.totalIncome !== 'undefined' ? data.totalIncome : monthlyStats.monthlyIncome) || 0;
    const expense = (typeof data.totalExpense !== 'undefined' ? data.totalExpense : monthlyStats.monthlyExpense) || 0;
    const totalBalance = data.totalBalance || 0;
    
    console.log("💰 Income:", income);
    console.log("💸 Expense:", expense); 
    console.log("💳 Total balance:", totalBalance);
    
    // Cập nhật các số liệu chính
    document.getElementById('totalIncome').textContent = Number(income || 0).toLocaleString('vi-VN') + ' đ';
    document.getElementById('totalExpense').textContent = Number(expense || 0).toLocaleString('vi-VN') + ' đ';
    document.getElementById('balance').textContent = totalBalance.toLocaleString('vi-VN') + ' đ';
    
    // Cập nhật thông tin tháng hiện tại
    const from = document.getElementById('dash-date-from')?.value;
    const to = document.getElementById('dash-date-to')?.value;
    const monthText = from && to ? `${from} → ${to}` : `Tháng ${data.currentMonth}/${data.currentYear}`;
    const monthDisplay = document.querySelector('.dashboard-month-display');
    if (monthDisplay) {
      monthDisplay.textContent = monthText;
    }
    
    // Cập nhật % ngân sách đã dùng từ budgetProgress
    try {
      const budgetUsageEl = document.getElementById('budget-usage');
      const progress = Array.isArray(data.budgetProgress) ? data.budgetProgress : [];
      const totalBudget = progress.reduce((sum, b) => sum + (Number(b.budgetAmount) || 0), 0);
      const usedBudget = progress.reduce((sum, b) => sum + (Number(b.spentAmount) || 0), 0);
      const usagePercent = totalBudget > 0 ? Math.round((usedBudget / totalBudget) * 100) : 0;
      if (budgetUsageEl) {
        budgetUsageEl.innerHTML = `
          <div class="text-center">
            <div class="h5 mb-0">${usagePercent}%</div>
            <small class="text-muted">Đã sử dụng</small>
            <div class="progress mt-2" style="height: 8px;">
              <div class="progress-bar ${usagePercent > 100 ? 'bg-danger' : usagePercent > 80 ? 'bg-warning' : 'bg-success'}" style="width: ${Math.min(usagePercent, 100)}%"></div>
            </div>
          </div>`;
      }
    } catch (e) {
      console.warn('Cannot render budget usage:', e);
    }

    // Cập nhật giao dịch gần đây
    if (Array.isArray(data.recentTransactions)) {
      const normalized = data.recentTransactions.map(t => ({
        type: t.type === 'income' ? 'THU' : 'CHI',
        amount: t.amount || 0,
        date: t.date,
        note: t.note,
        category: t.categoryName || 'Khác'
      }));
      updateRecentTransactions(normalized);
    }

    // Cập nhật thông tin ví (nếu có)
    if (Array.isArray(data.wallets)) {
      updateWalletDisplay(data.wallets);
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
    console.log("📊 Updating charts with data:", data);
    console.log("🔍 ExpensesByCategory:", data.expensesByCategory);
    console.log("🔍 SpendingTrend:", data.spendingTrend);
    
    // Cập nhật pie chart với expenses by category từ API dashboard
    if (data.expensesByCategory && data.expensesByCategory.length > 0) {
      renderPieChartFromDashboardData(data.expensesByCategory);
    } else {
      console.log("⚠️ No expensesByCategory data found. Data:", data.expensesByCategory);
    }
    
    // Cập nhật bar chart với spending trend nếu backend trả về; nếu không, tính từ transactions theo khoảng ngày
    if (data.spendingTrend && data.spendingTrend.length > 0) {
      renderBarChartFromTrend(data.spendingTrend);
    } else if (Array.isArray(data.recentTransactions)) {
      const byWeek = {};
      data.recentTransactions.forEach(t => {
        const d = new Date(t.date);
        // tạo nhãn tuần dạng YYYY-Wn (đơn giản hóa)
        const firstJan = new Date(d.getFullYear(),0,1);
        const week = Math.ceil((((d - firstJan) / 86400000) + firstJan.getDay()+1)/7);
        const key = `${d.getFullYear()}-W${week}`;
        byWeek[key] = (byWeek[key] || 0) + (t.type === 'expense' ? Number(t.amount||0) : 0);
      });
      const trend = Object.keys(byWeek).sort().map(k => ({ period: k, amount: byWeek[k] }));
      renderBarChartFromTrend(trend);
    } else {
      console.log("⚠️ No spendingTrend or recentTransactions data available for chart.");
    }
  }
  
  function renderPieChartFromDashboardData(expensesByCategory) {
    const ctx = document.getElementById('chart-pie')?.getContext('2d');
    if (!ctx) {
      console.error("❌ Không tìm thấy canvas chart-pie");
      return;
    }
    
    console.log("📊 Rendering pie chart from dashboard data:", expensesByCategory);
    
    // Destroy existing chart
    if (pieChart) {
      pieChart.destroy();
    }
    
    // Transform API data to chart format
    const labels = expensesByCategory.map(item => item.categoryName || 'Không xác định');
    const amounts = expensesByCategory.map(item => item.totalAmount || 0);
    const colors = expensesByCategory.map(item => item.categoryColor || '#6c757d');
    
    if (labels.length === 0 || amounts.every(amount => amount === 0)) {
      // Show empty state
      ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
      ctx.fillStyle = '#6c757d';
      ctx.font = '16px Arial';
      ctx.textAlign = 'center';
      ctx.fillText('Chưa có dữ liệu chi tiêu', ctx.canvas.width / 2, ctx.canvas.height / 2);
      return;
    }
    
    // Create new chart
    pieChart = new Chart(ctx, {
      type: 'pie',
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
            position: 'right',
            labels: {
              padding: 20,
              font: {
                size: 12
              }
            }
          },
          tooltip: {
            callbacks: {
              label: function(context) {
                const value = context.parsed;
                const total = amounts.reduce((sum, amount) => sum + amount, 0);
                const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
                return `${context.label}: ${value.toLocaleString('vi-VN')}đ (${percentage}%)`;
              }
            }
          }
        }
      }
    });
  }

  function renderBarChartFromTrend(spendingTrend) {
    console.log("📈 Rendering bar chart from trend data:", spendingTrend);
    const barChartCanvas = document.getElementById('chart-bar');
    if (!barChartCanvas) {
      console.warn("⚠️ Bar chart canvas not found");
      return;
    }

    // Destroy existing chart if exists
    if (window.barChartInstance) {
      window.barChartInstance.destroy();
    }

    // Prepare data
    const labels = spendingTrend.map(item => item.period || 'N/A');
    const amounts = spendingTrend.map(item => parseFloat(item.amount) || 0);

    console.log("📊 Bar chart data:", { labels, amounts });

    // Create bar chart
    window.barChartInstance = new Chart(barChartCanvas, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Chi tiêu',
          data: amounts,
          backgroundColor: 'rgba(75, 192, 192, 0.6)',
          borderColor: 'rgba(75, 192, 192, 1)',
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        plugins: {
          legend: {
            display: true
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

  function refreshCharts() {
    console.log("🔄 Refreshing charts...");
    initCharts();
  }

  function refreshCharts() {
    console.log("🔄 Refreshing charts...");
    initCharts();
  }

  // Add event listeners for date inputs
  if (dateFromInput && dateToInput) {
    dateFromInput.addEventListener('change', function() {
      loadDashboard();
    });
    dateToInput.addEventListener('change', function() {
      loadDashboard();
    });
  }
  
  // Initialize - GỌI DASHBOARD MỚI
  console.log("🚀 Initializing dashboard...");
  loadDashboard(); 
});

// 🔗 ENHANCED INTEGRATION FUNCTIONS - Các hàm liên kết nâng cao

/**
 * Fetch budgets data
 */
function fetchBudgets(month) {
  const [year, monthNum] = month.split('-').map(Number);
  const userId = getUserIdFromToken();
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
  const userId = getUserIdFromToken();
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
  if (!budgets || budgets.length === 0) {
    alertContainer.innerHTML = '<div class="text-center text-muted"><p>Tất cả ngân sách đều trong tầm kiểm soát</p><a href="/budgets" class="btn btn-warning btn-sm">Xem ngân sách</a></div>';
    return;
  }
  
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
  
  alertContainer.innerHTML = alertsHtml || '<div class="text-center text-muted"><p>Tất cả ngân sách đều trong tầm kiểm soát</p><a href="/budgets" class="btn btn-warning btn-sm">Xem ngân sách</a></div>';
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
    // Nếu backend đã cung cấp % tiến độ thì dùng trực tiếp để đồng bộ với trang Mục tiêu
    const progress = Math.min(
      (typeof goal.progressPercentage !== 'undefined' ? Number(goal.progressPercentage) : ((goal.currentAmount || 0) / (goal.targetAmount || 1) * 100)),
      100
    );
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
