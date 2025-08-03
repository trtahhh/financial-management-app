console.log("Dashboard JS loaded");

const userId = 1; // User ID for API calls
let pieChart, barChart;

document.addEventListener('DOMContentLoaded', function () {
  const monthInput = document.getElementById('dash-month');

  function fetchStats(month) {
    const [year, monthNum] = month.split('-').map(Number);
    const url = `http://localhost:8080/api/statistics/summary?userId=${userId}&month=${monthNum}&year=${year}`;
    console.log("📡 Fetching stats from:", url);
    
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
        console.log("🔍 Stats response status:", res.status);
        if (!res.ok) {
          return res.text().then(text => { 
            console.error("❌ Stats error:", text);
            throw new Error(`HTTP ${res.status}: ${text}`); 
          });
        }
        return res.json();
      })
      .then(data => {
        console.log("✅ Stats data received:", data);
        return data;
      })
      .catch(err => {
        console.error("🚨 Stats fetch failed:", err);
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

  function load() {
    console.log("🔄 Đang load dữ liệu dashboard cho:", monthInput.value);
    fetchStats(monthInput.value)
      .then(renderStats)
      .catch(e => {
        console.error("💥 Lỗi khi tải thống kê:", e);
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
      load();
      refreshCharts();
    });
  }
  
  // Initialize
  console.log("🚀 Initializing dashboard...");
  load();
  initCharts();
});
