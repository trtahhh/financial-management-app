// Recurring Transactions Management
let allRecurring = [];
let currentFilter = 'all';

// Authentication check
const authToken = localStorage.getItem('authToken');
if (!authToken) {
  window.location.href = '/login';
}

// API Configuration
const API_BASE = 'http://localhost:8080/api';
const getHeaders = () => ({
  'Authorization': `Bearer ${authToken}`,
  'Content-Type': 'application/json'
});

// Initialize page
document.addEventListener('DOMContentLoaded', () => {
  loadUserData();
  loadRecurringTransactions();
  loadCategories();
  loadWallets();
  
  // Set default start date to today
  document.getElementById('startDate').valueAsDate = new Date();
});

// Load user data
async function loadUserData() {
  try {
    const response = await fetch(`${API_BASE}/auth/me`, {
      headers: getHeaders()
    });
    if (response.ok) {
      const user = await response.json();
      console.log('User loaded:', user);
    }
  } catch (error) {
    console.error('Error loading user:', error);
  }
}

// Load all recurring transactions
async function loadRecurringTransactions() {
  try {
    const userResponse = await fetch(`${API_BASE}/auth/me`, {
      headers: getHeaders()
    });
    const user = await userResponse.json();
    
    const response = await fetch(`${API_BASE}/recurring-transactions/user/${user.id}`, {
      headers: getHeaders()
    });
    
    if (response.ok) {
      allRecurring = await response.json();
      updateStats();
      displayRecurring();
    } else {
      console.error('Failed to load recurring transactions');
      allRecurring = [];
      displayRecurring();
    }
  } catch (error) {
    console.error('Error loading recurring transactions:', error);
    allRecurring = [];
    displayRecurring();
  }
}

// Update statistics
function updateStats() {
  const active = allRecurring.filter(r => r.active).length;
  const inactive = allRecurring.filter(r => !r.active).length;
  
  // Calculate due transactions (next execution is today or past)
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const due = allRecurring.filter(r => {
    if (!r.active || !r.nextExecution) return false;
    const nextDate = new Date(r.nextExecution);
    nextDate.setHours(0, 0, 0, 0);
    return nextDate <= today;
  }).length;
  
  document.getElementById('activeCount').textContent = active;
  document.getElementById('inactiveCount').textContent = inactive;
  document.getElementById('dueCount').textContent = due;
  document.getElementById('totalCount').textContent = allRecurring.length;
}

// Display recurring transactions with filter
function displayRecurring() {
  const container = document.getElementById('recurringList');
  const emptyState = document.getElementById('emptyState');
  
  let filtered = [...allRecurring];
  
  // Apply filter
  switch(currentFilter) {
    case 'active':
      filtered = filtered.filter(r => r.active);
      break;
    case 'inactive':
      filtered = filtered.filter(r => !r.active);
      break;
    case 'income':
      filtered = filtered.filter(r => r.type === 'income');
      break;
    case 'expense':
      filtered = filtered.filter(r => r.type === 'expense');
      break;
  }
  
  if (filtered.length === 0) {
    container.innerHTML = '';
    emptyState.style.display = 'block';
    return;
  }
  
  emptyState.style.display = 'none';
  
  // Sort by next execution date
  filtered.sort((a, b) => {
    if (!a.nextExecution) return 1;
    if (!b.nextExecution) return -1;
    return new Date(a.nextExecution) - new Date(b.nextExecution);
  });
  
  container.innerHTML = filtered.map(r => createRecurringCard(r)).join('');
}

// Create recurring transaction card
function createRecurringCard(recurring) {
  const isIncome = recurring.type === 'income';
  const typeClass = isIncome ? 'success' : 'danger';
  const typeIcon = isIncome ? 'arrow-up' : 'arrow-down';
  
  // Calculate if due
  const isDue = recurring.active && recurring.nextExecution && 
                new Date(recurring.nextExecution) <= new Date();
  
  const statusBadge = recurring.active 
    ? '<span class="badge bg-success">Đang hoạt động</span>'
    : '<span class="badge bg-secondary">Tạm dừng</span>';
  
  const dueBadge = isDue 
    ? '<span class="badge bg-warning text-dark ms-2"><i class="fas fa-clock"></i> Cần thực thi</span>'
    : '';
  
  const nextExecution = recurring.nextExecution 
    ? new Date(recurring.nextExecution).toLocaleDateString('vi-VN')
    : 'Chưa xác định';
  
  const lastExecution = recurring.lastExecution
    ? new Date(recurring.lastExecution).toLocaleDateString('vi-VN')
    : 'Chưa từng thực thi';
  
  const frequencyLabels = {
    daily: 'Hàng ngày',
    weekly: 'Hàng tuần',
    monthly: 'Hàng tháng',
    yearly: 'Hàng năm'
  };
  
  return `
    <div class="col-md-6 col-lg-4">
      <div class="card border-0 shadow-sm h-100 hover-shadow transition">
        <div class="card-body">
          <div class="d-flex justify-content-between align-items-start mb-3">
            <div>
              <h5 class="mb-1">${recurring.description}</h5>
              <small class="text-muted">
                <i class="fas fa-repeat me-1"></i> ${frequencyLabels[recurring.frequency]}
              </small>
            </div>
            <div class="text-end">
              ${statusBadge}
              ${dueBadge}
            </div>
          </div>
          
          <div class="mb-3">
            <h3 class="text-${typeClass} mb-0">
              <i class="fas fa-${typeIcon}"></i>
              ${formatCurrency(recurring.amount)}
            </h3>
            <small class="text-muted">${recurring.categoryName || 'Không có danh mục'}</small>
          </div>
          
          <div class="mb-3">
            <div class="row g-2 small">
              <div class="col-6">
                <div class="text-muted">
                  <i class="fas fa-calendar-check me-1"></i> Lần sau:
                </div>
                <div class="fw-bold">${nextExecution}</div>
              </div>
              <div class="col-6">
                <div class="text-muted">
                  <i class="fas fa-history me-1"></i> Lần trước:
                </div>
                <div class="fw-bold">${lastExecution}</div>
              </div>
            </div>
          </div>
          
          <div class="d-flex gap-2">
            ${recurring.active ? `
              <button class="btn btn-sm btn-primary flex-fill" onclick="executeNow(${recurring.id})">
                <i class="fas fa-play"></i> Thực thi ngay
              </button>
              <button class="btn btn-sm btn-warning" onclick="toggleActive(${recurring.id})" title="Tạm dừng">
                <i class="fas fa-pause"></i>
              </button>
            ` : `
              <button class="btn btn-sm btn-success flex-fill" onclick="toggleActive(${recurring.id})">
                <i class="fas fa-play"></i> Kích hoạt
              </button>
            `}
            <button class="btn btn-sm btn-outline-secondary" onclick="editRecurring(${recurring.id})" title="Chỉnh sửa">
              <i class="fas fa-edit"></i>
            </button>
            <button class="btn btn-sm btn-outline-danger" onclick="deleteRecurring(${recurring.id})" title="Xóa">
              <i class="fas fa-trash"></i>
            </button>
          </div>
        </div>
      </div>
    </div>
  `;
}

// Filter recurring transactions
function filterRecurring(filter) {
  currentFilter = filter;
  displayRecurring();
}

// Show add modal
function showAddModal() {
  document.getElementById('modalTitle').textContent = 'Tạo giao dịch định kỳ';
  document.getElementById('recurringForm').reset();
  document.getElementById('recurringId').value = '';
  document.getElementById('startDate').valueAsDate = new Date();
  document.getElementById('isActive').checked = true;
  
  const modal = new bootstrap.Modal(document.getElementById('recurringModal'));
  modal.show();
}

// Edit recurring transaction
function editRecurring(id) {
  const recurring = allRecurring.find(r => r.id === id);
  if (!recurring) return;
  
  document.getElementById('modalTitle').textContent = 'Chỉnh sửa giao dịch định kỳ';
  document.getElementById('recurringId').value = recurring.id;
  document.getElementById('description').value = recurring.description;
  document.getElementById('type').value = recurring.type;
  document.getElementById('amount').value = recurring.amount;
  document.getElementById('categoryId').value = recurring.categoryId;
  document.getElementById('walletId').value = recurring.walletId;
  document.getElementById('frequency').value = recurring.frequency;
  document.getElementById('dayOfExecution').value = recurring.dayOfExecution;
  document.getElementById('startDate').value = recurring.startDate;
  document.getElementById('endDate').value = recurring.endDate || '';
  document.getElementById('isActive').checked = recurring.active;
  
  const modal = new bootstrap.Modal(document.getElementById('recurringModal'));
  modal.show();
}

// Save recurring transaction
async function saveRecurring() {
  const form = document.getElementById('recurringForm');
  if (!form.checkValidity()) {
    form.reportValidity();
    return;
  }
  
  const id = document.getElementById('recurringId').value;
  const data = {
    description: document.getElementById('description').value,
    type: document.getElementById('type').value,
    amount: parseFloat(document.getElementById('amount').value),
    categoryId: parseInt(document.getElementById('categoryId').value),
    walletId: parseInt(document.getElementById('walletId').value),
    frequency: document.getElementById('frequency').value,
    dayOfExecution: parseInt(document.getElementById('dayOfExecution').value),
    startDate: document.getElementById('startDate').value,
    endDate: document.getElementById('endDate').value || null,
    active: document.getElementById('isActive').checked
  };
  
  try {
    const url = id 
      ? `${API_BASE}/recurring-transactions/${id}`
      : `${API_BASE}/recurring-transactions`;
    
    const method = id ? 'PUT' : 'POST';
    
    const response = await fetch(url, {
      method,
      headers: getHeaders(),
      body: JSON.stringify(data)
    });
    
    if (response.ok) {
      showToast(id ? 'Cập nhật thành công!' : 'Tạo mới thành công!', 'success');
      bootstrap.Modal.getInstance(document.getElementById('recurringModal')).hide();
      loadRecurringTransactions();
    } else {
      const error = await response.text();
      showToast('Lỗi: ' + error, 'danger');
    }
  } catch (error) {
    console.error('Error saving recurring:', error);
    showToast('Có lỗi xảy ra!', 'danger');
  }
}

// Toggle active status
async function toggleActive(id) {
  try {
    const response = await fetch(`${API_BASE}/recurring-transactions/${id}/toggle`, {
      method: 'POST',
      headers: getHeaders()
    });
    
    if (response.ok) {
      showToast('Cập nhật trạng thái thành công!', 'success');
      loadRecurringTransactions();
    } else {
      showToast('Không thể cập nhật trạng thái!', 'danger');
    }
  } catch (error) {
    console.error('Error toggling active:', error);
    showToast('Có lỗi xảy ra!', 'danger');
  }
}

// Execute now
async function executeNow(id) {
  if (!confirm('Bạn có chắc muốn thực thi giao dịch này ngay bây giờ?')) {
    return;
  }
  
  try {
    const response = await fetch(`${API_BASE}/recurring-transactions/${id}/execute`, {
      method: 'POST',
      headers: getHeaders()
    });
    
    if (response.ok) {
      const result = await response.json();
      showToast(result.message || 'Thực thi thành công!', 'success');
      loadRecurringTransactions();
    } else {
      const error = await response.json();
      showToast(error.error || 'Không thể thực thi!', 'danger');
    }
  } catch (error) {
    console.error('Error executing recurring:', error);
    showToast('Có lỗi xảy ra!', 'danger');
  }
}

// Execute all due
async function executeAllDue() {
  const dueCount = allRecurring.filter(r => {
    if (!r.active || !r.nextExecution) return false;
    return new Date(r.nextExecution) <= new Date();
  }).length;
  
  if (dueCount === 0) {
    showToast('Không có giao dịch nào cần thực thi!', 'info');
    return;
  }
  
  if (!confirm(`Bạn có chắc muốn thực thi ${dueCount} giao dịch định kỳ?`)) {
    return;
  }
  
  try {
    const response = await fetch(`${API_BASE}/recurring-transactions/execute-all`, {
      method: 'POST',
      headers: getHeaders()
    });
    
    if (response.ok) {
      const result = await response.json();
      showToast(result.message || 'Thực thi thành công!', 'success');
      loadRecurringTransactions();
    } else {
      const error = await response.json();
      showToast(error.error || 'Không thể thực thi!', 'danger');
    }
  } catch (error) {
    console.error('Error executing all due:', error);
    showToast('Có lỗi xảy ra!', 'danger');
  }
}

// Delete recurring transaction
async function deleteRecurring(id) {
  if (!confirm('Bạn có chắc muốn xóa giao dịch định kỳ này?')) {
    return;
  }
  
  try {
    const response = await fetch(`${API_BASE}/recurring-transactions/${id}`, {
      method: 'DELETE',
      headers: getHeaders()
    });
    
    if (response.ok) {
      showToast('Xóa thành công!', 'success');
      loadRecurringTransactions();
    } else {
      showToast('Không thể xóa!', 'danger');
    }
  } catch (error) {
    console.error('Error deleting recurring:', error);
    showToast('Có lỗi xảy ra!', 'danger');
  }
}

// Load categories
async function loadCategories() {
  try {
    const response = await fetch(`${API_BASE}/categories/user`, {
      headers: getHeaders()
    });
    
    if (response.ok) {
      const categories = await response.json();
      const select = document.getElementById('categoryId');
      select.innerHTML = categories.map(c => 
        `<option value="${c.id}">${c.icon} ${c.name}</option>`
      ).join('');
    }
  } catch (error) {
    console.error('Error loading categories:', error);
  }
}

// Load wallets
async function loadWallets() {
  try {
    const response = await fetch(`${API_BASE}/wallets/user`, {
      headers: getHeaders()
    });
    
    if (response.ok) {
      const wallets = await response.json();
      const select = document.getElementById('walletId');
      select.innerHTML = wallets.map(w => 
        `<option value="${w.id}">${w.name} (${formatCurrency(w.balance)})</option>`
      ).join('');
    }
  } catch (error) {
    console.error('Error loading wallets:', error);
  }
}

// Utility: Format currency
function formatCurrency(amount) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount);
}

// Utility: Show toast notification
function showToast(message, type = 'info') {
  // Create toast container if not exists
  let container = document.getElementById('toastContainer');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toastContainer';
    container.className = 'position-fixed top-0 end-0 p-3';
    container.style.zIndex = '9999';
    document.body.appendChild(container);
  }
  
  const toastId = 'toast-' + Date.now();
  const toast = document.createElement('div');
  toast.id = toastId;
  toast.className = `toast align-items-center text-white bg-${type} border-0`;
  toast.setAttribute('role', 'alert');
  toast.innerHTML = `
    <div class="d-flex">
      <div class="toast-body">${message}</div>
      <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
    </div>
  `;
  
  container.appendChild(toast);
  const bsToast = new bootstrap.Toast(toast);
  bsToast.show();
  
  // Remove after hidden
  toast.addEventListener('hidden.bs.toast', () => {
    toast.remove();
  });
}

// Add CSS for hover effect
const style = document.createElement('style');
style.textContent = `
  .hover-shadow {
    transition: transform 0.2s, box-shadow 0.2s;
  }
  .hover-shadow:hover {
    transform: translateY(-4px);
    box-shadow: 0 0.5rem 1rem rgba(0,0,0,0.15) !important;
  }
`;
document.head.appendChild(style);
