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
 console.log(" JWT payload:", decoded);
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
 
 console.log(" Date range set to:", firstDay.toISOString().split('T')[0], "", today.toISOString().split('T')[0]);

 // Lấy dữ liệu dashboard theo đúng khoảng ngày được chọn (from/to)
 function fetchDashboardData() {
 const from = document.getElementById('dash-date-from')?.value;
 const to = document.getElementById('dash-date-to')?.value;
 
 console.log(" Date range from frontend:", from, "", to);
 
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
 console.log(" Fetching dashboard data from:", url);
 
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
 console.log(" Dashboard response status:", res.status);
 if (!res.ok) {
 return res.text().then(text => { 
 console.error("Dashboard error:", text);
 throw new Error(`HTTP ${res.status}: ${text}`); 
 });
 }
 return res.json();
 })
 .then(data => {
 console.log("Dashboard data received:", data);
 return data;
 })
 .catch(err => {
 console.error(" Dashboard fetch failed:", err);
 throw err;
 });
 }

 function fetchTransactions() {
 const url = `http://localhost:8080/api/transactions`;
 console.log(" Fetching transactions from:", url);
 
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
 console.log(" Transactions response status:", res.status);
 if (!res.ok) {
 return res.text().then(text => { 
 console.error("Transactions error:", text);
 throw new Error(`HTTP ${res.status}: ${text}`); 
 });
 }
 return res.json();
 })
 .then(data => {
 console.log("Transactions data received:", data);
 // No need to filter on frontend - backend already filters by user
 return data;
 })
 .catch(err => {
 console.error(" Transactions fetch failed:", err);
 throw err;
 });
 }

 function fetchCategories() {
 const url = `http://localhost:8080/api/categories`;
 console.log(" Fetching categories from:", url);
 
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
 console.log(" Categories response status:", res.status);
 if (!res.ok) {
 return res.text().then(text => { 
 console.error("Categories error:", text);
 throw new Error(`HTTP ${res.status}: ${text}`); 
 });
 }
 return res.json();
 })
 .then(data => {
 console.log("Categories data received:", data);
 // No need to filter on frontend - backend already handles user-specific data
 return data;
 })
 .catch(err => {
 console.error(" Categories fetch failed:", err);
 throw err;
 });
 }

 function renderStats(data) {
 console.log("Rendering stats:", data);
 document.getElementById('totalIncome').textContent = (data.totalIncome || 0).toLocaleString('vi-VN') + ' VND';
 document.getElementById('totalExpense').textContent = (data.totalExpense || 0).toLocaleString('vi-VN') + ' VND';
 document.getElementById('balance').textContent = (data.balance || 0).toLocaleString('vi-VN') + ' VND';
 }

 function initCharts() {
 // Sử dụng dữ liệu từ dashboard response thay vì fetch riêng
 fetchDashboardData()
 .then(data => {
 console.log("Dashboard data for charts:", data);
 
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
 
 console.log("Rendering pie chart with transactions:", transactions);
 console.log("Category map:", categoryMap);
 
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

 console.log("Expenses by category:", expensesByCategory);

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
 
 // Sử dụng logic màu mới để tránh trùng lặp
 const categoryNames = Object.keys(expensesByCategory);
 const colors = categoryNames.map((categoryName, index) => 
 generateCategoryColor(categoryName, index)
 );
 
 console.log(" Pie chart colors:", colors);
 
 pieChart = new Chart(ctx, {
 type: 'doughnut',
 data: {
 labels: categoryNames,
 datasets: [{
 data: Object.values(expensesByCategory),
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
 const total = context.dataset.data.reduce((a, b) => a + b, 0);
 const percentage = ((value / total) * 100).toFixed(1);
 return `${context.label}: ${value.toLocaleString('vi-VN')} VND (${percentage}%)`;
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
 
 console.log("Rendering bar chart for month:", currentMonth, "year:", currentYear);
 console.log("All transactions:", transactions);
 
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

 console.log(" Weekly data:", weeklyData);

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
 return `${context.dataset.label}: ${context.parsed.y.toLocaleString('vi-VN')} VND`;
 }
 }
 }
 },
 scales: {
 y: {
 beginAtZero: true,
 ticks: {
 callback: function(value) {
 return value.toLocaleString('vi-VN') + ' VND';
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
 console.log(" Rendering pie chart from backend data:", expensesByCategory);
 
 const ctx = document.getElementById('chart-pie').getContext('2d');
 if (pieChart) pieChart.destroy();
 
 if (expensesByCategory.length === 0) {
 showEmptyChart(ctx, 'Chưa có dữ liệu chi tiêu');
 return;
 }
 
 const labels = expensesByCategory.map(item => item.categoryName);
 const data = expensesByCategory.map(item => item.totalAmount);
 
 // Sử dụng logic màu mới để tránh trùng lặp
 const colors = labels.map((categoryName, index) => {
 // Nếu có màu từ backend, sử dụng; nếu không, tạo màu mới
 const backendColor = expensesByCategory[index]?.categoryColor;
 if (backendColor && backendColor !== '#007bff') {
 return backendColor;
 }
 // Tạo màu dựa trên tên category để đảm bảo nhất quán
 return generateCategoryColor(categoryName, index);
 });
 
 console.log(" Pie chart from data colors:", colors);
 
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
 return `${context.label}: ${Number(context.parsed).toLocaleString('vi-VN')} VND`;
 }
 }
 }
 }
 }
 });
 }

 function renderBarChartFromData(weeklyTrend) {
 console.log(" Rendering bar chart from backend data:", weeklyTrend);
 
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
 return `${context.dataset.label}: ${context.parsed.y.toLocaleString('vi-VN')} VND`;
 }
 }
 }
 },
 scales: {
 y: {
 beginAtZero: true,
 ticks: {
 callback: function(value) {
 return value.toLocaleString('vi-VN') + ' VND';
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
 console.log(" Đang load dữ liệu dashboard...");
 console.log(" User ID from token:", getUserIdFromToken());
 console.log(" Auth token exists:", !!localStorage.getItem('authToken'));
 
 fetchDashboardData()
 .then(dashboardData => {
 console.log(" Dashboard data loaded:", dashboardData);
 
 // Update UI với dữ liệu mới
 updateDashboardUI(dashboardData);
 
 // Update charts với dữ liệu mới
 updateChartsWithNewData(dashboardData);

 // Update Goal progress card
 try {
 const goalsRaw = Array.isArray(dashboardData.goalProgress) ? dashboardData.goalProgress : (dashboardData.goals || []);
 console.log(" Raw goal data:", goalsRaw);
 
 // Chuẩn hóa tên và phần trăm tiến độ từ API backend (goalProgress)
 const goals = goalsRaw.map(g => ({
 name: g.goalName || g.name || 'Mục tiêu',
 targetAmount: Number(g.targetAmount || g.target_amount || 0),
 currentAmount: Number(g.currentAmount || g.current_amount || 0),
 progressPercentage: Number(g.progressPercentage || g.progress || 0)
 }));
 
 console.log(" Normalized goals:", goals);
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
 console.log(" Budget warnings:", alerts);
 
 // Gọi updateBudgetAlerts để xử lý cảnh báo ngân sách
 updateBudgetAlerts(alerts);
 
 // 🆕 Fetch AI overspending alerts và hiển thị kèm
 fetchAndDisplayOverspendingAlerts();
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
 avgTransactionEl.textContent = avg.toLocaleString('vi-VN') + ' VND';
 }
 } catch (e) { console.warn('Quick stats render error:', e); }
 
 // Tính toán thống kê nâng cao với dữ liệu tích hợp
 try {
 const enhancedStats = calculateEnhancedStats(
 {
 totalIncome: dashboardData.totalIncome || 0,
 totalExpense: dashboardData.totalExpense || 0,
 balance: dashboardData.totalBalance || 0
 },
 dashboardData.recentTransactions || [],
 dashboardData.budgetProgress || [],
 dashboardData.goalProgress || []
 );
 
 // Cập nhật UI với thống kê nâng cao
 updateStats(enhancedStats);
 } catch (e) { console.warn('Enhanced stats calculation error:', e); }
 
 console.log(" Dashboard loaded successfully with integrated data!");
 
 })
 .catch(err => {
 console.error(" Error loading dashboard:", err);
 showError("Không thể tải dữ liệu dashboard: " + err.message);
 });
 }
 
 function updateDashboardUI(data) {
 // Dashboard data loaded successfully
 
 // Lấy dữ liệu theo khoảng ngày; fallback monthlyStats nếu có
 const monthlyStats = data.monthlyStats || {};
 const income = (typeof data.totalIncome !== 'undefined' ? data.totalIncome : monthlyStats.monthlyIncome) || 0;
 const expense = (typeof data.totalExpense !== 'undefined' ? data.totalExpense : monthlyStats.monthlyExpense) || 0;
 const totalBalance = data.totalBalance || 0;
 
 console.log(" Income:", income);
 console.log(" Expense:", expense); 
 console.log(" Total balance:", totalBalance);
 
 // Cập nhật các số liệu chính
 document.getElementById('totalIncome').textContent = Number(income || 0).toLocaleString('vi-VN') + ' VND';
 document.getElementById('totalExpense').textContent = Number(expense || 0).toLocaleString('vi-VN') + ' VND';
 document.getElementById('balance').textContent = totalBalance.toLocaleString('vi-VN') + ' VND';
 
 // Cập nhật thông tin tháng hiện tại
 const from = document.getElementById('dash-date-from')?.value;
 const to = document.getElementById('dash-date-to')?.value;
 const monthText = from && to ? `${from} ${to}` : `Tháng ${data.currentMonth}/${data.currentYear}`;
 const monthDisplay = document.querySelector('.dashboard-month-display');
 if (monthDisplay) {
 monthDisplay.textContent = monthText;
 }
 
 // Cập nhật % ngân sách đã dùng từ totalBudgetInfo (backend)
 try {
 const budgetUsageEl = document.getElementById('budget-usage');
 const totalBudgetInfo = data.totalBudgetInfo || {};
 
 console.log(" Total budget info from backend:", totalBudgetInfo);
 
 if (totalBudgetInfo.totalBudgetAmount && totalBudgetInfo.totalBudgetAmount > 0) {
 const totalBudget = Number(totalBudgetInfo.totalBudgetAmount);
 const usedBudget = Number(totalBudgetInfo.totalBudgetSpent);
 const usagePercent = Number(totalBudgetInfo.budgetUsagePercent);
 
 console.log(" Budget from backend:", { totalBudget, usedBudget, usagePercent });
 
 if (budgetUsageEl) {
 budgetUsageEl.innerHTML = `
 <div class="text-center">
 <div class="h5 mb-0">${Math.round(usagePercent)}%</div>
 <small class="text-muted">Đã sử dụng</small>
 <div class="progress mt-2" style="height: 8px;">
 <div class="progress-bar ${usagePercent > 100 ? 'bg-danger' : usagePercent > 80 ? 'bg-warning' : 'bg-success'}" style="width: ${Math.min(usagePercent, 100)}%"></div>
 </div>
 <small class="text-muted d-block mt-1">
 ${usedBudget.toLocaleString('vi-VN')}VND / ${totalBudget.toLocaleString('vi-VN')}VND
 </small>
 </div>`;
 }
 } else {
 // Fallback: tính toán từ budgetProgress nếu không có totalBudgetInfo
 const progress = Array.isArray(data.budgetProgress) ? data.budgetProgress : [];
 
 console.log(" Fallback: Budget progress data:", progress);
 
 if (progress.length > 0) {
 const totalBudget = progress.reduce((sum, b) => sum + (Number(b.budgetAmount) || 0), 0);
 const usedBudget = progress.reduce((sum, b) => sum + (Number(b.spentAmount) || 0), 0);
 const usagePercent = totalBudget > 0 ? Math.round((usedBudget / totalBudget) * 100) : 0;
 
 console.log(" Fallback budget calculation:", { totalBudget, usedBudget, usagePercent });
 
 if (budgetUsageEl) {
 budgetUsageEl.innerHTML = `
 <div class="text-center">
 <div class="h5 mb-0">${usagePercent}%</div>
 <small class="text-muted">Đã sử dụng</small>
 <div class="progress mt-2" style="height: 8px;">
 <div class="progress-bar ${usagePercent > 100 ? 'bg-danger' : usagePercent > 80 ? 'bg-warning' : 'bg-success'}" style="width: ${Math.min(usagePercent, 100)}%"></div>
 </div>
 <small class="text-muted d-block mt-1">
 ${usedBudget.toLocaleString('vi-VN')}VND / ${totalBudget.toLocaleString('vi-VN')}VND
 </small>
 </div>`;
 }
 } else {
 // Không có ngân sách nào
 if (budgetUsageEl) {
 budgetUsageEl.innerHTML = `
 <div class="text-center">
 <div class="h5 mb-0">0%</div>
 <small class="text-muted">Đã sử dụng</small>
 <div class="progress mt-2" style="height: 8px;">
 <div class="progress-bar bg-secondary" style="width: 0%"></div>
 </div>
 <small class="text-muted d-block mt-1">
 Chưa thiết lập ngân sách
 </small>
 </div>`;
 }
 }
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
 
 console.log(" Dashboard UI updated successfully");
 }
 
 function updateWalletDisplay(wallets) {
 const walletContainer = document.querySelector('.wallet-summary');
 if (walletContainer && wallets.length > 0) {
 const walletHTML = wallets.map(wallet => 
 `<div class="wallet-item">
 <span class="wallet-name">${wallet.name}</span>
 <span class="wallet-balance">${wallet.balance.toLocaleString('vi-VN')} VNĐ</span>
 </div>`
 ).join('');
 walletContainer.innerHTML = walletHTML;
 }
 }
 
 function updateChartsWithNewData(data) {
 console.log(" Updating charts with data:", data);
 console.log(" ExpensesByCategory:", data.expensesByCategory);
 console.log(" SpendingTrend:", data.spendingTrend);
 
 // Cập nhật pie chart với expenses by category từ API dashboard
 if (data.expensesByCategory && data.expensesByCategory.length > 0) {
 renderPieChartFromDashboardData(data.expensesByCategory);
 } else {
 console.log(" No expensesByCategory data found. Data:", data.expensesByCategory);
 }
 
 // Cập nhật bar chart với spending trend nếu backend trả về; nếu không, tính từ transactions theo khoảng ngày
 if (data.spendingTrend && data.spendingTrend.length > 0) {
 renderBarChartFromTrend(data.spendingTrend);
 } else if (Array.isArray(data.recentTransactions)) {
 const byWeek = { income: {}, expense: {} };
 data.recentTransactions.forEach(t => {
 const d = new Date(t.date);
 // tạo nhãn tuần dạng YYYY-Wn (đơn giản hóa)
 const firstJan = new Date(d.getFullYear(),0,1);
 const week = Math.ceil((((d - firstJan) / 86400000) + firstJan.getDay()+1)/7);
 const key = `${d.getFullYear()}-W${week}`;
 
 // Phân loại theo thu nhập và chi tiêu
 if (t.type === 'income' || t.type === 'THU' || t.type === 'INCOME') {
 byWeek.income[key] = (byWeek.income[key] || 0) + Number(t.amount || 0);
 } else if (t.type === 'expense' || t.type === 'CHI' || t.type === 'EXPENSE') {
 byWeek.expense[key] = (byWeek.expense[key] || 0) + Number(t.amount || 0);
 }
 });
 
 // Tạo trend data với cả thu nhập và chi tiêu
 const allWeeks = new Set([...Object.keys(byWeek.income), ...Object.keys(byWeek.expense)]);
 const trend = Array.from(allWeeks).sort().map(week => ({
 period: week,
 income: byWeek.income[week] || 0,
 amount: byWeek.expense[week] || 0,
 expense: byWeek.expense[week] || 0
 }));
 
 renderBarChartFromTrend(trend);
 } else {
 console.log(" No spendingTrend or recentTransactions data available for chart.");
 }
 }
 
 function renderPieChartFromDashboardData(expensesByCategory) {
 const ctx = document.getElementById('chart-pie')?.getContext('2d');
 if (!ctx) {
 console.error(" Không tìm thấy canvas chart-pie");
 return;
 }
 
 console.log(" Rendering pie chart from dashboard data:", expensesByCategory);
 
 // Destroy existing chart
 if (pieChart) {
 pieChart.destroy();
 }
 
 // Transform API data to chart format
 const labels = expensesByCategory.map(item => item.categoryName || 'Không xác định');
 const amounts = expensesByCategory.map(item => item.totalAmount || 0);
 
 // Sử dụng logic màu mới để tránh trùng lặp
 const colors = labels.map((categoryName, index) => {
 // Nếu có màu từ backend, sử dụng; nếu không, tạo màu mới
 const backendColor = expensesByCategory[index]?.categoryColor;
 if (backendColor && backendColor !== '#6c757d') {
 return backendColor;
 }
 // Tạo màu dựa trên tên category để đảm bảo nhất quán
 return generateCategoryColor(categoryName, index);
 });
 
 console.log(" Chart colors:", colors);
 
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
 },
 // Tùy chỉnh legend để hiển thị màu rõ ràng
 generateLabels: function(chart) {
 const data = chart.data;
 if (data.labels.length && data.datasets.length) {
 return data.labels.map((label, i) => {
 const dataset = data.datasets[0];
 const value = dataset.data[i];
 const total = dataset.data.reduce((a, b) => a + b, 0);
 const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
 
 return {
 text: `${label} (${percentage}%)`,
 fillStyle: dataset.backgroundColor[i],
 strokeStyle: dataset.backgroundColor[i],
 lineWidth: 0,
 hidden: false,
 index: i
 };
 });
 }
 return [];
 }
 }
 },
 tooltip: {
 callbacks: {
 label: function(context) {
 const value = context.parsed;
 const total = amounts.reduce((sum, amount) => sum + amount, 0);
 const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
 return `${context.label}: ${value.toLocaleString('vi-VN')} VNĐ (${percentage}%)`;
 }
 }
 }
 }
 }
 });
 }

 function renderBarChartFromTrend(spendingTrend) {
 console.log(" Rendering bar chart from trend data:", spendingTrend);
 const barChartCanvas = document.getElementById('chart-bar');
 if (!barChartCanvas) {
 console.warn(" Bar chart canvas not found");
 return;
 }

 // Destroy existing chart if exists
 if (barChart) {
 barChart.destroy();
 }

 // Prepare data for both income and expense
 const labels = spendingTrend.map(item => item.period || 'N/A');
 const incomeData = spendingTrend.map(item => parseFloat(item.income) || 0);
 const expenseData = spendingTrend.map(item => parseFloat(item.amount) || 0);

 console.log(" Bar chart data:", { labels, incomeData, expenseData });

 // Create bar chart with both income and expense datasets
 barChart = new Chart(barChartCanvas, {
 type: 'bar',
 data: {
 labels: labels,
 datasets: [
 {
 label: 'Thu nhập',
 data: incomeData,
 backgroundColor: 'rgba(40, 167, 69, 0.7)',
 borderColor: 'rgba(40, 167, 69, 1)',
 borderWidth: 1,
 borderRadius: 4
 },
 {
 label: 'Chi tiêu',
 data: expenseData,
 backgroundColor: 'rgba(220, 53, 69, 0.7)',
 borderColor: 'rgba(220, 53, 69, 1)',
 borderWidth: 1,
 borderRadius: 4
 }
 ]
 },
 options: {
 responsive: true,
 maintainAspectRatio: false,
 plugins: {
 legend: {
 display: true,
 position: 'top',
 labels: { 
 usePointStyle: true, 
 padding: 20,
 font: { size: 12 }
 }
 },
 tooltip: {
 callbacks: {
 label: function(context) {
 return `${context.dataset.label}: ${context.parsed.y.toLocaleString('vi-VN')} VNĐ`;
 }
 }
 }
 },
 scales: {
 y: {
 beginAtZero: true,
 ticks: {
 callback: function(value) {
 return value.toLocaleString('vi-VN') + ' VNĐ';
 }
 }
 }
 }
 }
 });
 }
 
 function generateColors(count) {
 // Bảng màu đủ lớn với 20+ màu khác biệt để tránh trùng lặp
 const colorPalette = [
 // Màu chính - tươi sáng
 '#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF',
 '#FF9F40', '#FF6384', '#C9CBCF', '#FF6384', '#36A2EB',
 
 // Màu phụ - đa dạng
 '#FF6B9D', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7',
 '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE', '#85C1E9',
 
 // Màu bổ sung - độc đáo
 '#F8C471', '#82E0AA', '#F1948A', '#85C1E9', '#D7BDE2',
 '#F9E79F', '#D5A6BD', '#A9CCE3', '#FAD7A0', '#D2B4DE',
 
 // Màu gradient - hiện đại
 '#E74C3C', '#8E44AD', '#2980B9', '#27AE60', '#F39C12',
 '#E67E22', '#95A5A6', '#34495E', '#2C3E50', '#16A085'
 ];
 
 // Logic phân bổ màu thông minh để tránh trùng lặp
 const colors = [];
 const usedColors = new Set();
 
 for (let i = 0; i < count; i++) {
 let color;
 let attempts = 0;
 const maxAttempts = 50;
 
 do {
 // Ưu tiên màu từ bảng màu chính trước
 if (i < colorPalette.length) {
 color = colorPalette[i];
 } else {
 // Nếu vượt quá bảng màu, tạo màu ngẫu nhiên
 color = generateRandomColor();
 }
 
 attempts++;
 
 // Nếu đã thử quá nhiều lần, tạo màu hoàn toàn ngẫu nhiên
 if (attempts > maxAttempts) {
 color = generateRandomColor();
 break;
 }
 
 } while (usedColors.has(color));
 
 colors.push(color);
 usedColors.add(color);
 }
 
 console.log(` Generated ${count} unique colors for chart`);
 return colors;
 }
 
 /**
 * Tạo màu ngẫu nhiên với độ tương phản tốt
 */
 function generateRandomColor() {
 // Tạo màu với độ bão hòa và độ sáng phù hợp cho biểu đồ
 const hue = Math.floor(Math.random() * 360);
 const saturation = Math.floor(Math.random() * 30) + 60; // 60-90% để màu không quá nhạt
 const lightness = Math.floor(Math.random() * 20) + 45; // 45-65% để màu không quá tối
 
 return `hsl(${hue}, ${saturation}%, ${lightness}%)`;
 }
 
 /**
 * Tạo màu dựa trên tên category để đảm bảo nhất quán
 */
 function generateCategoryColor(categoryName, index) {
 // Bảng màu cố định cho các category phổ biến
 const categoryColorMap = {
 'Ăn uống': '#FF6B6B', // Đỏ cam
 'Giao thông': '#4ECDC4', // Xanh lá
 'Giải trí': '#45B7D1', // Xanh dương
 'Sức khỏe': '#96CEB4', // Xanh lá nhạt
 'Giáo dục': '#FFEAA7', // Vàng
 'Mua sắm': '#DDA0DD', // Tím
 'Tiện ích': '#98D8C8', // Xanh lá đậm
 'Du lịch': '#F7DC6F', // Vàng cam
 'Thể thao': '#BB8FCE', // Tím nhạt
 'Lương': '#27AE60', // Xanh lá đậm
 'Thu nhập khác': '#2980B9', // Xanh dương đậm
 'Đầu tư': '#8E44AD', // Tím đậm
 'Kinh doanh': '#E67E22', // Cam
 'Khác': '#95A5A6' // Xám
 };
 
 // Nếu có màu cố định cho category, sử dụng
 if (categoryColorMap[categoryName]) {
 return categoryColorMap[categoryName];
 }
 
 // Nếu không có, sử dụng bảng màu theo index
 const colorPalette = [
 '#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF',
 '#FF9F40', '#FF6B9D', '#4ECDC4', '#45B7D1', '#96CEB4',
 '#FFEAA7', '#DDA0DD', '#98D8C8', '#F7DC6F', '#BB8FCE',
 '#85C1E9', '#F8C471', '#82E0AA', '#F1948A', '#D7BDE2'
 ];
 
 return colorPalette[index % colorPalette.length];
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
 console.log(" Refreshing charts...");
 initCharts();
 }

 function refreshCharts() {
 console.log(" Refreshing charts...");
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
 console.log(" Initializing dashboard...");
 loadDashboard(); 
});

// ENHANCED INTEGRATION FUNCTIONS - Các hàm liên kết nâng cao

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
 console.error(" Budgets fetch failed:", err);
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
 console.error(" Goals fetch failed:", err);
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
 
 // Calculate budget usage - sử dụng dữ liệu từ backend nếu có
 let totalBudget = 0;
 let usedBudget = 0;
 
 if (budgets && budgets.length > 0) {
 // Sử dụng dữ liệu từ backend (budgetAmount, spentAmount)
 totalBudget = budgets.reduce((sum, b) => sum + (Number(b.budgetAmount) || 0), 0);
 usedBudget = budgets.reduce((sum, b) => sum + (Number(b.spentAmount) || 0), 0);
 } else {
 // Fallback cho dữ liệu cũ
 totalBudget = budgets.reduce((sum, b) => sum + (Number(b.amount) || 0), 0);
 usedBudget = budgets.reduce((sum, b) => sum + (Number(b.usedAmount) || 0), 0);
 }
 
 const budgetUsagePercent = totalBudget > 0 ? Math.round((usedBudget / totalBudget) * 100) : 0;
 
 // Calculate goals progress - sử dụng dữ liệu từ backend nếu có
 let totalGoalsTarget = 0;
 let totalGoalsProgress = 0;
 
 if (goals && goals.length > 0) {
 // Sử dụng dữ liệu từ backend (targetAmount, currentAmount hoặc currentBalance)
 totalGoalsTarget = goals.reduce((sum, g) => sum + (Number(g.targetAmount) || 0), 0);
 totalGoalsProgress = goals.reduce((sum, g) => sum + (Number(g.currentAmount || g.currentBalance) || 0), 0);
 }
 
 const goalsProgressPercent = totalGoalsTarget > 0 ? Math.round((totalGoalsProgress / totalGoalsTarget) * 100) : 0;
 
 console.log(" Enhanced stats calculation:", {
 totalBudget,
 usedBudget,
 budgetUsagePercent,
 totalGoalsTarget,
 totalGoalsProgress,
 goalsProgressPercent,
 transactionCount: currentMonthTransactions.length
 });
 
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
 
 console.log(" Budget alerts data:", budgets);
 
 if (!budgets || budgets.length === 0) {
 alertContainer.innerHTML = '<div class="text-center text-muted"><p>Tất cả ngân sách đều trong tầm kiểm soát</p><a href="/budgets" class="btn btn-warning btn-sm">Xem ngân sách</a></div>';
 return;
 }
 
 // Sử dụng dữ liệu từ backend (spentAmount, budgetAmount, usagePercent, status)
 const exceededBudgets = budgets.filter(b => {
 const usagePercent = Number(b.usagePercent || 0);
 return usagePercent >= 100;
 });
 
 const nearLimitBudgets = budgets.filter(b => {
 const usagePercent = Number(b.usagePercent || 0);
 return usagePercent >= 80 && usagePercent < 100;
 });
 
 let alertsHtml = '';
 
 if (exceededBudgets.length > 0) {
 alertsHtml += '<div class="alert alert-danger mb-2"><strong>Vượt ngân sách:</strong><br>';
 exceededBudgets.forEach(b => {
 const spent = Number(b.spentAmount || 0);
 const budget = Number(b.budgetAmount || 0);
 alertsHtml += `<small>• ${b.categoryName}: ${spent.toLocaleString('vi-VN')}VNĐ / ${budget.toLocaleString('vi-VN')}VNĐ (${Number(b.usagePercent || 0).toFixed(1)}%)</small><br>`;
 });
 alertsHtml += '</div>';
 }
 
 if (nearLimitBudgets.length > 0) {
 alertsHtml += '<div class="alert alert-warning mb-2"><strong>Gần đạt giới hạn:</strong><br>';
 nearLimitBudgets.forEach(b => {
 const spent = Number(b.spentAmount || 0);
 const budget = Number(b.budgetAmount || 0);
 alertsHtml += `<small>• ${b.categoryName}: ${spent.toLocaleString('vi-VN')}VNĐ / ${budget.toLocaleString('vi-VN')}VNĐ (${Number(b.usagePercent || 0).toFixed(1)}%)</small><br>`;
 });
 alertsHtml += '</div>';
 }
 
 if (alertsHtml) {
 alertsHtml += '<a href="/budgets" class="btn btn-warning btn-sm">Xem ngân sách</a>';
 }
 
 alertContainer.innerHTML = alertsHtml || '<div class="text-center text-muted"><p>Tất cả ngân sách đều trong tầm kiểm soát</p><a href="/budgets" class="btn btn-warning btn-sm">Xem ngân sách</a></div>';
 
 console.log(" Budget alerts HTML updated");
}

/**
 * 🆕 Fetch and display AI overspending alerts
 * Tích hợp cảnh báo AI thông minh vào dashboard
 */
async function fetchAndDisplayOverspendingAlerts() {
 try {
 const token = localStorage.getItem('authToken');
 if (!token) return;
 
 const response = await fetch('http://localhost:8080/api/ai/overspending-alerts', {
 method: 'GET',
 headers: {
 'Authorization': 'Bearer ' + token,
 'Content-Type': 'application/json'
 }
 });
 
 if (!response.ok) {
 console.warn('Failed to fetch overspending alerts:', response.status);
 return;
 }
 
 const data = await response.json();
 console.log('🤖 AI Overspending alerts:', data);
 
 // Hiển thị AI alerts cùng với budget alerts
 if (data.success && data.alerts && data.alerts.length > 0) {
 displayAIAlerts(data.alerts, data.summary);
 }
 
 } catch (error) {
 console.warn('Error fetching AI overspending alerts:', error);
 }
}

/**
 * Display AI overspending alerts với màu sắc theo severity
 */
function displayAIAlerts(alerts, summary) {
 const alertContainer = document.getElementById('budget-alerts');
 if (!alertContainer) return;
 
 // Lấy nội dung hiện tại (budget warnings)
 let existingContent = alertContainer.innerHTML;
 
 // Nếu chỉ có message mặc định, xóa nó
 if (existingContent.includes('Tất cả ngân sách đều trong tầm kiểm soát')) {
 existingContent = '';
 }
 
 let aiAlertsHtml = '';
 
 // Group alerts by severity
 const critical = alerts.filter(a => a.severity === 'critical');
 const warning = alerts.filter(a => a.severity === 'warning');
 const info = alerts.filter(a => a.severity === 'info');
 
 // Critical alerts (red)
 if (critical.length > 0) {
 aiAlertsHtml += '<div class="alert alert-danger mb-2">';
 aiAlertsHtml += '<strong>🚨 Cảnh báo nghiêm trọng:</strong><br>';
 critical.forEach(alert => {
 aiAlertsHtml += `<small>• ${alert.message}</small><br>`;
 if (alert.recommendation) {
 aiAlertsHtml += `<small class="text-muted ms-3">💡 ${alert.recommendation}</small><br>`;
 }
 });
 aiAlertsHtml += '</div>';
 }
 
 // Warning alerts (yellow)
 if (warning.length > 0) {
 aiAlertsHtml += '<div class="alert alert-warning mb-2">';
 aiAlertsHtml += '<strong>⚠️ Cảnh báo:</strong><br>';
 warning.forEach(alert => {
 aiAlertsHtml += `<small>• ${alert.message}</small><br>`;
 if (alert.recommendation) {
 aiAlertsHtml += `<small class="text-muted ms-3">💡 ${alert.recommendation}</small><br>`;
 }
 });
 aiAlertsHtml += '</div>';
 }
 
 // Info alerts (blue)
 if (info.length > 0) {
 aiAlertsHtml += '<div class="alert alert-info mb-2">';
 aiAlertsHtml += '<strong>ℹ️ Thông tin:</strong><br>';
 info.forEach(alert => {
 aiAlertsHtml += `<small>• ${alert.message}</small><br>`;
 });
 aiAlertsHtml += '</div>';
 }
 
 // Summary stats
 if (summary) {
 aiAlertsHtml += `<div class="mt-2"><small class="text-muted">Tổng: ${summary.critical} nghiêm trọng, ${summary.warning} cảnh báo, ${summary.info} thông tin</small></div>`;
 }
 
 // Combine existing budget warnings + AI alerts
 alertContainer.innerHTML = existingContent + aiAlertsHtml + '<a href="/budgets" class="btn btn-warning btn-sm mt-2">Xem chi tiết</a>';
 
 console.log('✅ AI alerts displayed on dashboard');
}

/**
 * Update goal progress display
 */
function updateGoalProgress(goals, transactions) {
 const goalContainer = document.getElementById('goal-progress');
 if (!goalContainer) return;
 
 console.log(" Updating goal progress with:", goals);
 
 const savingsTransactions = transactions.filter(t => t.type === 'THU');
 const thisMonthSavings = savingsTransactions
 .filter(t => {
 const date = new Date(t.date);
 const now = new Date();
 return date.getMonth() === now.getMonth() && date.getFullYear() === now.getFullYear();
 })
 .reduce((sum, t) => sum + t.amount, 0);
 
 let goalHtml = '<h6>Tiến độ mục tiêu</h6>';
 
 if (goals.length === 0) {
 goalHtml += '<div class="text-center text-muted"><p>Chưa có mục tiêu nào được thiết lập</p><a href="/goals" class="btn btn-success btn-sm">Tạo mục tiêu</a></div>';
 } else {
 goals.slice(0, 3).forEach(goal => {
 // Nếu backend đã cung cấp % tiến độ thì dùng trực tiếp để đồng bộ với trang Mục tiêu
 const progress = Math.min(
 (typeof goal.progressPercentage !== 'undefined' ? Number(goal.progressPercentage) : ((goal.currentAmount || 0) / (goal.targetAmount || 1) * 100)),
 100
 );
 
 const currentAmount = Number(goal.currentAmount || goal.currentBalance || 0);
 const targetAmount = Number(goal.targetAmount || 0);
 
 goalHtml += `
 <div class="mb-3">
 <div class="d-flex justify-content-between align-items-center mb-2">
 <small class="fw-bold">${goal.name}</small>
 <small class="badge ${progress >= 100 ? 'bg-success' : progress >= 75 ? 'bg-info' : progress >= 50 ? 'bg-warning' : 'bg-secondary'}">${progress.toFixed(1)}%</small>
 </div>
 <div class="progress mb-2" style="height: 8px;">
 <div class="progress-bar ${progress >= 100 ? 'bg-success' : progress >= 75 ? 'bg-info' : progress >= 50 ? 'bg-warning' : 'bg-secondary'}" 
 style="width: ${progress}%"></div>
 </div>
 <small class="text-muted d-block">
 ${currentAmount.toLocaleString('vi-VN')}VNĐ / ${targetAmount.toLocaleString('vi-VN')}VNĐ
 </small>
 </div>
 `;
 });
 }
 
 // Bỏ phần hiển thị "Tháng này tiết kiệm"
 
 goalContainer.innerHTML = goalHtml;
 
 console.log(" Goal progress HTML updated");
}

/**
 * Enhanced update stats with integrated data
 */
function updateStats(enhancedStats) {
 console.log(" Rendering enhanced stats:", enhancedStats);
 
 // Basic stats - chỉ cập nhật nếu chưa được cập nhật bởi updateDashboardUI
 const incomeEl = document.getElementById('totalIncome');
 const expenseEl = document.getElementById('totalExpense');
 const balanceEl = document.getElementById('balance');
 
 if (incomeEl && incomeEl.textContent === '0 VNĐ') {
 incomeEl.textContent = (enhancedStats.totalIncome || 0).toLocaleString('vi-VN') + ' VNĐ';
 console.log(" Income updated in updateStats:", enhancedStats.totalIncome);
 }
 if (expenseEl && expenseEl.textContent === '0 VNĐ') {
 expenseEl.textContent = (enhancedStats.totalExpense || 0).toLocaleString('vi-VN') + ' VNĐ';
 console.log(" Expense updated in updateStats:", enhancedStats.totalExpense);
 }
 if (balanceEl && balanceEl.textContent === '0 VNĐ') {
 balanceEl.textContent = (enhancedStats.balance || 0).toLocaleString('vi-VN') + ' VNĐ';
 console.log(" Balance updated in updateStats:", enhancedStats.balance);
 }
 
 // Enhanced stats - chỉ cập nhật nếu chưa được cập nhật bởi updateDashboardUI
 const budgetUsageEl = document.getElementById('budget-usage');
 if (budgetUsageEl && budgetUsageEl.textContent.includes('0%')) {
 const budgetPercent = enhancedStats.budgetUsagePercent || 0;
 budgetUsageEl.innerHTML = `
 <div class="text-center">
 <div class="h5 mb-0">${budgetPercent}%</div>
 <small class="text-muted">Đã sử dụng ngân sách</small>
 <div class="progress mt-2" style="height: 8px;">
 <div class="progress-bar ${budgetPercent > 100 ? 'bg-danger' : budgetPercent > 80 ? 'bg-warning' : 'bg-success'}" 
 style="width: ${Math.min(budgetPercent, 100)}%"></div>
 </div>
 <small class="text-muted d-block mt-1">
 ${enhancedStats.usedBudget ? enhancedStats.usedBudget.toLocaleString('vi-VN') + 'VNĐ' : '0VNĐ'} / 
 ${enhancedStats.totalBudget ? enhancedStats.totalBudget.toLocaleString('vi-VN') + 'VNĐ' : '0VNĐ'}
 </small>
 </div>
 `;
 
 console.log(" Budget usage updated in updateStats:", budgetPercent);
 }
 
 const goalsProgressEl = document.getElementById('goals-progress');
 if (goalsProgressEl && goalsProgressEl.textContent.includes('0%')) {
 const goalsPercent = enhancedStats.goalsProgressPercent || 0;
 goalsProgressEl.innerHTML = `
 <div class="text-center">
 <div class="h5 mb-0">${goalsPercent}%</div>
 <small class="text-muted">Tiến độ mục tiêu</small>
 <div class="progress mt-2" style="height: 8px;">
 <div class="progress-bar bg-primary" style="width: ${Math.min(goalsPercent, 100)}%"></div>
 </div>
 <small class="text-muted d-block mt-1">
 ${enhancedStats.totalGoalsProgress ? enhancedStats.totalGoalsProgress.toLocaleString('vi-VN') + 'VNĐ' : '0VNĐ'} / 
 ${enhancedStats.totalGoalsTarget ? enhancedStats.totalGoalsTarget.toLocaleString('vi-VN') + 'VNĐ' : '0VNĐ'}
 </small>
 </div>
 `;
 
 console.log(" Goals progress updated in updateStats:", goalsPercent);
 }
 
 // Update quick stats - chỉ cập nhật nếu chưa được cập nhật bởi loadDashboard
 const transactionCountEl = document.getElementById('transaction-count');
 if (transactionCountEl && transactionCountEl.textContent === '0') {
 transactionCountEl.textContent = enhancedStats.transactionCount || 0;
 console.log(" Transaction count updated in updateStats:", enhancedStats.transactionCount);
 }
 
 const avgTransactionEl = document.getElementById('average-transaction');
 if (avgTransactionEl && avgTransactionEl.textContent === '0VNĐ' && enhancedStats.transactionCount > 0) {
 const avgAmount = (enhancedStats.totalExpense + enhancedStats.totalIncome) / enhancedStats.transactionCount;
 avgTransactionEl.textContent = avgAmount.toLocaleString('vi-VN') + ' VNĐ';
 console.log(" Average transaction updated in updateStats:", avgAmount);
 }
 
 console.log(" updateStats completed successfully");
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
 ${amountPrefix}${(tx.amount || 0).toLocaleString('vi-VN')}VNĐ
 </div>
 </div>
 `;
 }).join('');
 
 container.innerHTML = recentTransactionsHtml;
}
