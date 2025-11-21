document.addEventListener('DOMContentLoaded', function () {
 const list = document.getElementById('goal-list');
 const m = new bootstrap.Modal(document.getElementById('goal-modal'));
 const f = document.getElementById('goal-form');
 const title = document.getElementById('goal-modal-title');
 let editing = null;

 function getAuthHeaders() {
 const token = localStorage.getItem('authToken');
 const headers = {
 'Content-Type': 'application/json'
 };
 if (token) {
 headers['Authorization'] = 'Bearer ' + token;
 }
 return headers;
 }

 function updateTotalSavedAmount(goals) {
 const totalSaved = goals
 .filter(g => g.status === 'COMPLETED' || g.status === 'EXECUTED')
 .reduce((sum, g) => sum + (Number(g.targetAmount) || 0), 0);
 
 document.getElementById('total-saved-amount').textContent = totalSaved.toLocaleString('vi-VN') + ' VNĐ';
 }
 
 function updateGoalCounts() {
 const activeGoalsCount = document.querySelectorAll('.goal-item').length;
 const completedGoalsCount = document.querySelectorAll('.completed-goal-item').length;
 
 document.getElementById('active-goals-count').textContent = activeGoalsCount;
 document.getElementById('completed-goals-count').textContent = completedGoalsCount;
 }
 
 function load() {
 loadGoals();
 loadGoalStats();
 loadGoalProgress();
 loadCompletedGoals();
 loadPrediction();
 }

 function loadGoals() {
 fetch('http://localhost:8080/api/goals', {
 headers: getAuthHeaders()
 })
 .then(r => r.json())
 .then(goals => {
 console.log('Goals loaded:', goals);
 
 // Phân loại mục tiêu theo trạng thái
 const activeGoals = goals.filter(g => g.status !== 'COMPLETED' && g.status !== 'EXECUTED');
 const completedGoals = goals.filter(g => g.status === 'COMPLETED');
 const executedGoals = goals.filter(g => g.status === 'EXECUTED');
 
 // Hiển thị mục tiêu đang thực hiện
 renderActiveGoals(activeGoals);
 
 // Hiển thị mục tiêu đã hoàn thành
 renderCompletedGoals(completedGoals);
 
 // Hiển thị mục tiêu đã thực hiện
 renderExecutedGoals(executedGoals);
 
 // Cập nhật số lượng
 updateGoalCounts();
 
 // Cập nhật tổng tiền tiết kiệm
 updateTotalSavedAmount(goals);
 })
 .catch(e => {
 console.error('Error loading goals:', e);
 document.getElementById('goal-list').innerHTML = '<div class="alert alert-danger">Lỗi tải danh sách mục tiêu</div>';
 });
 }

 function loadGoalStats() {
 fetch('http://localhost:8080/api/goals/stats', {
 headers: getAuthHeaders()
 })
 .then(r => r.json())
 .then(stats => {
 document.getElementById('active-goals-count').textContent = stats.activeGoals || 0;
 document.getElementById('completed-goals-count').textContent = stats.completedGoals || 0;
 
 const totalSaved = stats.totalSavedAmount || 0;
 document.getElementById('total-saved-amount').textContent = 
 Number(totalSaved).toLocaleString('vi-VN') + ' VND';
 })
 .catch(e => {
 console.error('Error loading goal stats:', e);
 });
 }

 function loadGoalProgress() {
 fetch('http://localhost:8080/api/goals/progress', {
 headers: getAuthHeaders()
 })
 .then(r => r.json())
 .then(goals => {
 const progressList = document.getElementById('goal-progress-list');
 
 if (goals.length === 0) {
 progressList.innerHTML = '<div class="alert alert-info">Chưa có mục tiêu nào</div>';
 return;
 }
 
 progressList.innerHTML = goals.map(goal => `
 <div class="mb-3">
 <div class="d-flex justify-content-between align-items-center mb-2">
 <h6 class="mb-0">${goal.goalName}</h6>
 <span class="badge ${goal.status === 'completed' ? 'bg-success' : goal.status === 'near-completion' ? 'bg-warning' : 'bg-info'}">
 ${goal.status === 'completed' ? 'Hoàn thành' : goal.status === 'near-completion' ? 'Gần hoàn thành' : 'Đang thực hiện'}
 </span>
 </div>
 <div class="progress">
 <div class="progress-bar ${goal.status === 'completed' ? 'bg-success' : goal.status === 'near-completion' ? 'bg-warning' : 'bg-info'}" 
 role="progressbar" 
 style="width:${Math.min(goal.progressPercentage, 100)}%"
 aria-valuenow="${goal.progressPercentage}" 
 aria-valuemin="0" 
 aria-valuemax="100">
 ${goal.progressPercentage.toFixed(1)}%
 </div>
 </div>
 <small class="text-muted">
 Mục tiêu: ${Number(goal.targetAmount).toLocaleString('vi-VN')} VND |
 Hiện tại: ${Number(goal.currentBalance).toLocaleString('vi-VN')} VND
 </small>
 </div>
 `).join('');
 })
 .catch(e => {
 console.error('Error loading goal progress:', e);
 });
 }

 function loadCompletedGoals() {
 fetch('http://localhost:8080/api/goals/completed', {
 headers: getAuthHeaders()
 })
 .then(r => r.json())
 .then(goals => {
 const completedList = document.getElementById('completed-goals-list');
 
 if (goals.length === 0) {
 completedList.innerHTML = '<div class="alert alert-info">Chưa có mục tiêu nào hoàn thành</div>';
 return;
 }
 
 completedList.innerHTML = goals.map(goal => `
 <div class="card mb-2 border-success">
 <div class="card-body">
 <div class="d-flex justify-content-between align-items-center">
 <div>
 <h6 class="card-title text-success mb-1">${goal.name}</h6>
 <small class="text-muted">
 Hoàn thành: ${goal.completedAt ? new Date(goal.completedAt).toLocaleDateString('vi-VN') : 'Chưa xác định'}
 </small>
 </div>
 <div class="text-end">
 <div class="text-success fw-bold">
 ${Number(goal.targetAmount).toLocaleString('vi-VN')} VND
 </div>
 <small class="text-muted">Số tiền tiết kiệm</small>
 </div>
 </div>
 </div>
 </div>
 `).join('');
 })
 .catch(e => {
 console.error('Error loading completed goals:', e);
 });
 }

 function loadPrediction() {
 fetch('http://localhost:8080/api/goals/predict', {
 headers: getAuthHeaders()
 })
 .then(r => r.json())
 .then(p => { 
 document.getElementById('predict').textContent = p.message; 
 })
 .catch(e => {});
 }

 function renderActiveGoals(goals) {
 const list = document.getElementById('goal-list');
 list.innerHTML = '';
 
 if (goals.length === 0) {
 list.innerHTML = '<div class="alert alert-info">Chưa có mục tiêu nào đang thực hiện. Hãy thêm mục tiêu đầu tiên!</div>';
 return;
 }
 
 for (const goal of goals) {
 const div = document.createElement('div');
 div.className = 'card mb-3 goal-item';
 
 // Format target amount with VND currency
 const targetAmount = goal.targetAmount ? Number(goal.targetAmount).toLocaleString('vi-VN') + ' VND' : 'Chưa xác định';
 
 // Format due date
 const dueDate = goal.dueDate ? new Date(goal.dueDate).toLocaleDateString('vi-VN') : 'Chưa xác định';
 
 // Calculate progress
 const progress = goal.progress || 0;
 
 div.innerHTML = `
 <div class="card-body">
 <div class="d-flex justify-content-between align-items-center mb-2">
 <h5 class="card-title mb-0">${goal.name}</h5>
 <div>
 <button class="btn btn-sm btn-outline-primary edit" data-id="${goal.id}">Sửa</button>
 <button class="btn btn-sm btn-outline-danger ms-2 del" data-id="${goal.id}">Xoá</button>
 ${progress >= 100 ? '<button class="btn btn-sm btn-success ms-2 complete" data-id="' + goal.id + '">Hoàn thành</button>' : ''}
 ${progress >= 100 && !goal.isExecuted ? '<button class="btn btn-sm btn-warning ms-2 execute" data-id="' + goal.id + '">Thực hiện mục tiêu</button>' : ''}
 ${goal.isExecuted ? '<span class="badge bg-success ms-2">Đã thực hiện</span>' : ''}
 </div>
 </div>
 <div class="mb-2">
 <strong>Cần đạt:</strong> ${targetAmount}<br>
 <strong>Đến:</strong> ${dueDate}
 </div>
 <div class="progress mb-2">
 <div class="progress-bar ${progress >= 100 ? 'bg-success' : progress >= 50 ? 'bg-warning' : 'bg-info'}" 
 role="progressbar" 
 style="width:${Math.min(progress, 100)}%"
 aria-valuenow="${progress}" 
 aria-valuemin="0" 
 aria-valuemax="100">
 ${progress.toFixed(1)}%
 </div>
 </div>
 </div>
 `;
 list.appendChild(div);
 }
 }
 
 function renderCompletedGoals(goals) {
 const list = document.getElementById('completed-goals-list');
 list.innerHTML = '';
 
 if (goals.length === 0) {
 list.innerHTML = '<div class="alert alert-info">Chưa có mục tiêu nào đã hoàn thành.</div>';
 return;
 }
 
 for (const goal of goals) {
 const div = document.createElement('div');
 div.className = 'card mb-3 completed-goal-item bg-light';
 
 const targetAmount = goal.targetAmount ? Number(goal.targetAmount).toLocaleString('vi-VN') + ' VND' : 'Chưa xác định';
 const completedDate = goal.completedAt ? new Date(goal.completedAt).toLocaleDateString('vi-VN') : 'Chưa xác định';
 
 div.innerHTML = `
 <div class="card-body">
 <div class="d-flex justify-content-between align-items-center mb-2">
 <h6 class="card-title mb-0 text-success">${goal.name}</h6>
 <span class="badge bg-success">Đã hoàn thành</span>
 </div>
 <div class="mb-2">
 <strong>Mục tiêu:</strong> ${targetAmount}<br>
 <strong>Hoàn thành:</strong> ${completedDate}
 </div>
 <div class="text-center">
 <button class="btn btn-sm btn-warning execute" data-id="${goal.id}">Thực hiện mục tiêu</button>
 </div>
 </div>
 `;
 list.appendChild(div);
 }
 }
 
 function renderExecutedGoals(goals) {
 const list = document.getElementById('completed-goals-list');
 
 if (goals.length === 0) {
 return;
 }
 
 for (const goal of goals) {
 const div = document.createElement('div');
 div.className = 'card mb-3 executed-goal-item bg-success text-white';
 
 const targetAmount = goal.targetAmount ? Number(goal.targetAmount).toLocaleString('vi-VN') + ' VND' : 'Chưa xác định';
 const executedDate = goal.executedAt ? new Date(goal.executedAt).toLocaleDateString('vi-VN') : 'Chưa xác định';
 
 div.innerHTML = `
 <div class="card-body">
 <div class="d-flex justify-content-between align-items-center mb-2">
 <h6 class="card-title mb-0">${goal.name}</h6>
 <span class="badge bg-light text-dark">Đã thực hiện</span>
 </div>
 <div class="mb-2">
 <strong>Số tiền:</strong> ${targetAmount}<br>
 <strong>Thực hiện:</strong> ${executedDate}
 </div>
 <div class="text-center">
 <span class="badge bg-light text-success"> Mục tiêu đã được thực hiện thành công!</span>
 </div>
 </div>
 `;
 list.appendChild(div);
 }
 }

 document.getElementById('goal-add-btn').addEventListener('click', function () {
 editing = null;
 f.reset();
 title.textContent = 'Thêm mục tiêu';
 m.show();
 });

 // � AI Planning Wizard button
 document.getElementById('ai-planning-btn').addEventListener('click', function () {
 const planningModal = new bootstrap.Modal(document.getElementById('ai-planning-modal'));
 planningModal.show();
 // Reset to step 1
 document.getElementById('planning-step-1').style.display = 'block';
 document.getElementById('planning-step-2').style.display = 'none';
 document.getElementById('planning-loading').style.display = 'none';
 document.getElementById('ai-planning-form').reset();
 });

 // � AI Planning Form Submit
 document.getElementById('ai-planning-form').addEventListener('submit', async function(e) {
 e.preventDefault();
 
 const months = parseInt(document.getElementById('plan-months').value);
 const targetSavings = parseFloat(document.getElementById('target-savings').value);
 
 // Show loading
 document.getElementById('planning-step-1').style.display = 'none';
 document.getElementById('planning-loading').style.display = 'block';
 
 try {
 const response = await fetch('http://localhost:8080/api/ai/long-term-plan', {
 method: 'POST',
 headers: getAuthHeaders(),
 body: JSON.stringify({
 months: months,
 targetSavings: targetSavings
 })
 });
 
 const data = await response.json();
 
 if (data.success && data.plan) {
 displayAIPlan(data.plan, months, targetSavings);
 } else {
 alert('Không thể tạo kế hoạch: ' + (data.error || 'Lỗi không xác định'));
 document.getElementById('planning-step-1').style.display = 'block';
 }
 } catch (error) {
 console.error('AI Planning error:', error);
 alert('Lỗi kết nối: ' + error.message);
 document.getElementById('planning-step-1').style.display = 'block';
 } finally {
 document.getElementById('planning-loading').style.display = 'none';
 }
 });

 list.addEventListener('click', function (e) {
 const id = e.target.dataset.id;
 if (e.target.classList.contains('edit')) {
 fetch('http://localhost:8080/api/goals/' + id, {
 headers: getAuthHeaders()
 })
 .then(r => r.json())
 .then(response => {
 const goal = response.data || response;
 editing = id;
 f.name.value = goal.name || '';
 f.target_amount.value = goal.targetAmount || '';
 f.due_date.value = goal.dueDate ? goal.dueDate.substring(0,10) : '';
 title.textContent = 'Sửa mục tiêu';
 m.show();
 }).catch(e => {
 console.error('Error loading goal:', e);
 alert('Lỗi tải thông tin mục tiêu: ' + e.message);
 });
 }
 if (e.target.classList.contains('del')) {
 if (confirm('Bạn chắc chắn xoá mục tiêu này?')) {
 fetch('http://localhost:8080/api/goals/' + id, { 
 method: 'DELETE',
 headers: getAuthHeaders()
 })
 .then(r => r.json())
 .then(response => {
 if (response.message) {
 // Success case
 load();
 } else if (response.error) {
 // Error case
 alert('Lỗi xóa mục tiêu: ' + response.error);
 } else {
 // Fallback
 load();
 }
 })
 .catch(e => {
 console.error('Error deleting goal:', e);
 alert('Lỗi xóa mục tiêu: ' + e.message);
 });
 }
 }
 
 if (e.target.classList.contains('execute')) {
 if (confirm('Bạn chắc chắn muốn thực hiện mục tiêu này? Hệ thống sẽ trừ tiền từ ví và tạo giao dịch chi tiêu.')) {
 fetch('http://localhost:8080/api/goals/' + id + '/execute', { 
 method: 'POST',
 headers: getAuthHeaders()
 })
 .then(r => r.json())
 .then(response => {
 if (response.success) {
 alert(` ${response.message}\n\nSố tiền: ${Number(response.transaction.amount).toLocaleString('vi-VN')} VND\nVí: ${response.walletName}\nSố dư mới: ${Number(response.newBalance).toLocaleString('vi-VN')} VND`);
 
 // Tự động xóa mục tiêu đã thực hiện khỏi danh sách đang thực hiện
 const goalElement = e.target.closest('.goal-item');
 if (goalElement) {
 goalElement.style.animation = 'fadeOut 0.5s ease-out';
 setTimeout(() => {
 goalElement.remove();
 // Cập nhật số lượng mục tiêu đang thực hiện
 updateGoalCounts();
 }, 500);
 }
 
 // Reload để cập nhật UI và danh sách mục tiêu đã thực hiện
 load();
 } else {
 alert(' Lỗi: ' + response.error);
 }
 })
 .catch(e => {
 console.error('Error executing goal:', e);
 alert('Lỗi thực hiện mục tiêu: ' + e.message);
 });
 }
 }
 });

 f.addEventListener('submit', function (e) {
 e.preventDefault();
 const data = {
 name: f.name.value,
 targetAmount: +f.target_amount.value,
 dueDate: f.due_date.value
 };
 
 if (!data.name.trim()) {
 alert('Tên mục tiêu không được để trống');
 return;
 }
 
 if (!data.targetAmount || data.targetAmount <= 0) {
 alert('Số tiền mục tiêu phải lớn hơn 0');
 return;
 }
 
 const method = editing ? 'PUT' : 'POST';
 const url = 'http://localhost:8080/api/goals' + (editing ? '/' + editing : '');
 
 fetch(url, {
 method,
 headers: getAuthHeaders(),
 body: JSON.stringify(data)
 })
 .then(r => r.json())
 .then(response => {
 if (response.success !== false) {
 m.hide();
 load();
 } else {
 alert('Lỗi lưu mục tiêu: ' + (response.message || 'Unknown error'));
 }
 })
 .catch(e => {
 console.error('Error saving goal:', e);
 alert('Lỗi lưu mục tiêu: ' + e.message);
 });
 });

 load();
});

// � Global variables for AI planning
let currentAIPlan = null;

/**
 * Display AI long-term plan result
 */
function displayAIPlan(plan, months, targetSavings) {
 currentAIPlan = plan;
 
 const resultDiv = document.getElementById('ai-plan-result');
 let html = '';
 
 // Plan summary
 html += '<div class="alert alert-success">';
 html += '<h6> Kế hoạch ' + months + ' tháng</h6>';
 html += '<p><strong>Mục tiêu:</strong> ' + targetSavings.toLocaleString('vi-VN') + ' VNĐ</p>';
 
 if (plan.monthlySavingsRequired) {
 html += '<p><strong>Cần tiết kiệm mỗi tháng:</strong> ' + plan.monthlySavingsRequired.toLocaleString('vi-VN') + ' VNĐ</p>';
 }
 
 if (plan.savingsRate) {
 html += '<p><strong>Tỷ lệ tiết kiệm:</strong> ' + plan.savingsRate + '%</p>';
 }
 
 if (plan.feasibility) {
 // Map backend levels: impossible, very_difficult, difficult, achievable, easy
 let color, text;
 const level = plan.feasibility.level || plan.feasibility; // Support both object and string
 
 switch(level.toLowerCase()) {
 case 'impossible':
 color = 'danger';
 text = 'Không khả thi';
 break;
 case 'very_difficult':
 color = 'danger';
 text = 'Rất khó đạt';
 break;
 case 'difficult':
 color = 'warning';
 text = 'Khó đạt được';
 break;
 case 'achievable':
 color = 'info';
 text = 'Có thể đạt được';
 break;
 case 'easy':
 color = 'success';
 text = 'Dễ đạt được';
 break;
 default:
 color = 'secondary';
 text = level;
 }
 
 html += '<p><strong>Độ khả thi:</strong> <span class="badge bg-' + color + '">' + text + '</span></p>';
 }
 
 html += '</div>';
 
 // Monthly breakdown
 if (plan.monthlyBreakdown && plan.monthlyBreakdown.length > 0) {
 html += '<h6>Chi tiết từng tháng:</h6>';
 html += '<div class="table-responsive">';
 html += '<table class="table table-sm table-bordered">';
 html += '<thead><tr><th>Tháng</th><th>Thu nhập dự kiến</th><th>Chi tiêu tối đa</th><th>Tiết kiệm</th><th>Tích lũy</th></tr></thead>';
 html += '<tbody>';
 
 plan.monthlyBreakdown.forEach(month => {
 html += '<tr>';
 html += '<td>Tháng ' + month.month + '</td>';
 html += '<td>' + (month.expectedIncome || 0).toLocaleString('vi-VN') + ' VNĐ</td>';
 html += '<td>' + (month.maxSpending || 0).toLocaleString('vi-VN') + ' VNĐ</td>';
 html += '<td class="text-success"><strong>' + (month.savings || 0).toLocaleString('vi-VN') + ' VNĐ</strong></td>';
 html += '<td class="text-primary"><strong>' + (month.cumulativeSavings || 0).toLocaleString('vi-VN') + ' VNĐ</strong></td>';
 html += '</tr>';
 });
 
 html += '</tbody></table>';
 html += '</div>';
 }
 
 // Recommendations
 if (plan.recommendations && plan.recommendations.length > 0) {
 html += '<h6 class="mt-3"> Đề xuất của AI:</h6>';
 html += '<ul class="list-group">';
 plan.recommendations.forEach(rec => {
 html += '<li class="list-group-item">' + rec + '</li>';
 });
 html += '</ul>';
 }
 
 // Category optimization
 if (plan.categoryOptimizations && plan.categoryOptimizations.length > 0) {
 html += '<h6 class="mt-3"> Tối ưu hóa theo danh mục:</h6>';
 html += '<ul class="list-group">';
 plan.categoryOptimizations.forEach(opt => {
 html += '<li class="list-group-item">';
 html += '<strong>' + opt.category + ':</strong> ';
 html += 'Giảm ' + (opt.currentSpending - opt.suggestedSpending).toLocaleString('vi-VN') + ' VNĐ ';
 html += '(từ ' + opt.currentSpending.toLocaleString('vi-VN') + ' → ' + opt.suggestedSpending.toLocaleString('vi-VN') + ' VNĐ)';
 if (opt.suggestion) {
 html += '<br><small class="text-muted"> ' + opt.suggestion + '</small>';
 }
 html += '</li>';
 });
 html += '</ul>';
 }
 
 resultDiv.innerHTML = html;
 
 // Show step 2
 document.getElementById('planning-step-1').style.display = 'none';
 document.getElementById('planning-step-2').style.display = 'block';
}

/**
 * Back to planning step 1
 */
function backToPlanningStep1() {
 document.getElementById('planning-step-1').style.display = 'block';
 document.getElementById('planning-step-2').style.display = 'none';
 currentAIPlan = null;
}

/**
 * Apply AI plan as a new goal
 */
function applyPlanAsGoal() {
 if (!currentAIPlan) {
 alert('Không có kế hoạch để áp dụng');
 return;
 }
 
 const months = parseInt(document.getElementById('plan-months').value);
 const targetSavings = parseFloat(document.getElementById('target-savings').value);
 
 // Calculate due date (today + months)
 const dueDate = new Date();
 dueDate.setMonth(dueDate.getMonth() + months);
 
 // Create goal from AI plan
 const goalData = {
 name: 'Kế hoạch tiết kiệm ' + months + ' tháng (AI)',
 targetAmount: targetSavings,
 dueDate: dueDate.toISOString().split('T')[0]
 };
 
 fetch('http://localhost:8080/api/goals', {
 method: 'POST',
 headers: getAuthHeaders(),
 body: JSON.stringify(goalData)
 })
 .then(r => r.json())
 .then(response => {
 if (response.success !== false) {
 alert(' Đã tạo mục tiêu từ kế hoạch AI!');
 // Close modal
 const modal = bootstrap.Modal.getInstance(document.getElementById('ai-planning-modal'));
 modal.hide();
 // Reload goals
 load();
 } else {
 alert('Lỗi tạo mục tiêu: ' + (response.message || 'Unknown error'));
 }
 })
 .catch(e => {
 console.error('Error creating goal from AI plan:', e);
 alert('Lỗi: ' + e.message);
 });
}

