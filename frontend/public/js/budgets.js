// Budget Management with AI Features
document.addEventListener('DOMContentLoaded', function () {
  const t = document.getElementById('budget-table');
  const m = new bootstrap.Modal(document.getElementById('budget-modal'));
  const f = document.getElementById('budget-form');
  const title = document.getElementById('budget-modal-title');
  const categorySelect = f.querySelector('select[name="category_id"]');
  let editing = null;
  let categories = [];

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

  // Load categories first
  function loadCategories() {
    return fetch('http://localhost:8080/api/categories', {
      headers: getAuthHeaders()
    })
      .then(r => r.json())
      .then(cats => {
        categories = cats;
        categorySelect.innerHTML = '<option value="">Chọn danh mục</option>';
        cats.forEach(cat => {
          categorySelect.innerHTML += `<option value="${cat.id}">${cat.name}</option>`;
        });
      });
  }

  function load() {
    Promise.all([
      fetch('http://localhost:8080/api/budgets', {
        headers: getAuthHeaders()
      }).then(r => r.json()),
      fetch('http://localhost:8080/api/categories', {
        headers: getAuthHeaders()
      }).then(r => r.json())
    ]).then(([budgets, categories]) => {
      const categoryMap = {};
      categories.forEach(cat => categoryMap[cat.id] = cat.name);
      
      t.innerHTML =
        '<thead><tr><th>Tháng</th><th>Năm</th><th>Danh mục</th><th>Số tiền</th><th>Đã dùng</th><th></th></tr></thead><tbody>' +
        budgets.map(b =>
          `<tr data-id="${b.id}">
            <td>${b.month}</td>
            <td>${b.year}</td>
            <td>${categoryMap[b.category_id] || 'Không xác định'}</td>
            <td>${b.amount?.toLocaleString('vi-VN')} VND</td>
            <td>
              <div class="d-flex flex-column">
                <span class="fw-bold">${(b.spentAmount || 0).toLocaleString('vi-VN')} VND</span>
                <div class="progress mt-1" style="height: 8px;">
                  <div class="progress-bar ${(b.progress||0) >= 100 ? 'bg-danger' : (b.progress||0) >= 80 ? 'bg-warning' : 'bg-success'}" 
                       role="progressbar" 
                       style="width:${Math.min(b.progress||0, 100)}%"
                       title="${b.progress||0}% đã sử dụng">
                  </div>
                </div>
                <small class="fw-bold ${(b.progress||0) >= 100 ? 'text-danger' : (b.progress||0) >= 80 ? 'text-warning' : 'text-success'}">${b.progress||0}% đã sử dụng</small>
                ${(b.progress||0) > 100 ? '<small class="text-danger"><i class="fas fa-exclamation-triangle"></i> Vượt ngân sách!</small>' : ''}
              </div>
            </td>
            <td>
              <button class="btn btn-sm btn-outline-primary edit">Sửa</button>
              <button class="btn btn-sm btn-outline-danger ms-2 del">Xoá</button>
            </td>
          </tr>`
        ).join('') + '</tbody>';
    }).catch(e => alert(e.message));
  }

  document.getElementById('budget-add-btn').addEventListener('click', function () {
    editing = null;
    f.reset();
    title.textContent = 'Thêm ngân sách';
    f.year.value = new Date().getFullYear();
    f.month.value = new Date().getMonth() + 1;
    m.show();
  });

  t.addEventListener('click', function (e) {
    const id = e.target.closest('tr')?.dataset.id;
    if (e.target.classList.contains('edit')) {
      fetch('http://localhost:8080/api/budgets/' + id, {
        headers: getAuthHeaders()
      })
        .then(r => r.json())
        .then(b => {
          editing = id;
          f.month.value = b.month;
          f.year.value = b.year;
          f.category_id.value = b.category_id;
          f.amount.value = b.amount;
          title.textContent = 'Sửa ngân sách';
          m.show();
        }).catch(e => alert(e.message));
    }
    if (e.target.classList.contains('del')) {
      if (confirm('Bạn chắc chắn xoá ngân sách này?')) {
        fetch('http://localhost:8080/api/budgets/' + id, { 
          method: 'DELETE',
          headers: getAuthHeaders()
        })
          .then(load)
          .catch(e => alert(e.message));
      }
    }
  });

  f.addEventListener('submit', function (e) {
    e.preventDefault();
    
    // Validation
    const categoryId = +f.category_id.value;
    const amount = +f.amount.value;
    const month = +f.month.value;
    const year = +f.year.value;
    
    if (!categoryId) {
      alert('Vui lòng chọn danh mục');
      f.category_id.focus();
      return;
    }
    
    if (!amount || amount <= 0) {
      alert('Số tiền phải lớn hơn 0');
      f.amount.focus();
      return;
    }
    
    if (month < 1 || month > 12) {
      alert('Tháng phải từ 1 đến 12');
      f.month.focus();
      return;
    }
    
    if (year < 2020 || year > 2030) {
      alert('Năm phải từ 2020 đến 2030');
      f.year.focus();
      return;
    }
    
    const data = {
      month: month,
      year: year,
      categoryId: categoryId,
      amount: amount
    };
    
    const method = editing ? 'PUT' : 'POST';
    const url = '/api/budgets' + (editing ? '/' + editing : '');
    
    fetch(url, {
      method,
      headers: getAuthHeaders(),
      body: JSON.stringify(data)
    })
      .then(r => {
        if (!r.ok) {
          return r.json().then(errorData => {
            throw new Error(errorData.message || `HTTP ${r.status}: ${r.statusText}`);
          });
        }
        return r.json();
      })
      .then(response => {
        if (response.success !== false) {
          m.hide();
          load();
          alert(editing ? 'Cập nhật ngân sách thành công!' : 'Tạo ngân sách thành công!');
        } else {
          alert('Lỗi lưu ngân sách: ' + (response.message || 'Unknown error'));
        }
      })
      .catch(e => {
        console.error('Error saving budget:', e);
        alert('Lỗi lưu ngân sách: ' + e.message);
      });
  });

  // Initialize
  loadCategories().then(() => {
    load();
    addSmartBudgetFeatures();
  });
});

// ============= AI SMART BUDGET FEATURES =============

function addSmartBudgetFeatures() {
  const budgetContainer = document.querySelector('.budget-container') || document.querySelector('.container');
  if (!budgetContainer || document.getElementById('smart-budget-section')) return;
  
  const smartSection = document.createElement('div');
  smartSection.id = 'smart-budget-section';
  smartSection.className = 'card mb-4';
  smartSection.innerHTML = `
    <div class="card-header">
      <h5> Ngân sách thông minh</h5>
    </div>
    <div class="card-body">
      <div class="row">
        <div class="col-md-4">
          <label class="form-label">Thu nhập hàng tháng</label>
          <input type="number" id="monthly-income" class="form-control" placeholder="VD: 5000000">
        </div>
        <div class="col-md-4">
          <label class="form-label">Loại ngân sách</label>
          <select id="budget-type" class="form-select">
            <option value="balanced">Cân bằng</option>
            <option value="conservative">Tiết kiệm</option>
            <option value="flexible">Linh hoạt</option>
          </select>
        </div>
        <div class="col-md-4">
          <label class="form-label">&nbsp;</label>
          <div>
            <button class="btn btn-primary" onclick="generateSmartBudget()">
               Tạo ngân sách AI
            </button>
          </div>
        </div>
      </div>
      <div id="smart-budget-result" class="mt-3"></div>
    </div>
  `;
  
  budgetContainer.insertBefore(smartSection, budgetContainer.firstChild);
}

async function generateSmartBudget() {
  const monthlyIncome = document.getElementById('monthly-income').value;
  const budgetType = document.getElementById('budget-type').value;
  
  if (!monthlyIncome || parseFloat(monthlyIncome) <= 0) {
    alert('Vui lòng nhập thu nhập hàng tháng hợp lệ');
    return;
  }

  try {
    const resultDiv = document.getElementById('smart-budget-result');
    resultDiv.innerHTML = '<div class="text-center"><div class="spinner-border" role="status"></div> Đang xử lý...</div>';
    
    const response = await fetch('/api/ai/budget/generate', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + localStorage.getItem('authToken')
      },
      body: JSON.stringify({
        monthlyIncome: parseFloat(monthlyIncome),
        budgetType: budgetType
      })
    });

    const data = await response.json();
    
    if (data.success) {
      displaySmartBudgetResult(data);
    } else {
      alert('Lỗi tạo ngân sách: ' + data.error);
    }
  } catch (error) {
    console.error('Smart budget error:', error);
    alert('Lỗi kết nối AI service');
  }
}

function displaySmartBudgetResult(data) {
  const resultDiv = document.getElementById('smart-budget-result');
  
  let html = `
    <div class="alert alert-success">
      <h6> Ngân sách được đề xuất (Điểm sức khỏe tài chính: ${data.healthScore}/100)</h6>
    </div>
    <div class="row">
  `;
  
  Object.entries(data.allocations || {}).forEach(([category, allocation]) => {
    html += `
      <div class="col-md-6 col-lg-4 mb-3">
        <div class="card">
          <div class="card-body">
            <h6 class="card-title">${allocation.category}</h6>
            <p class="card-text">
              <strong>${(allocation.amount || 0).toLocaleString('vi-VN')} VND</strong><br>
              <small class="text-muted">${Math.round((allocation.ratio || 0) * 100)}% thu nhập</small><br>
              <small class="text-info">Độ tin cậy: ${Math.round((allocation.confidence || 0) * 100)}%</small>
            </p>
            <button class="btn btn-sm btn-outline-success" 
                    onclick="createBudgetFromAI('${allocation.category}', ${allocation.amount})">
               Tạo ngân sách
            </button>
          </div>
        </div>
      </div>
    `;
  });
  
  html += '</div>';
  
  if (data.insights && data.insights.length > 0) {
    html += '<div class="mt-3"><h6> Thông tin chi tiết:</h6>';
    data.insights.forEach(insight => {
      html += `<div class="alert alert-info">${insight.icon} <strong>${insight.title}:</strong> ${insight.message}</div>`;
    });
    html += '</div>';
  }
  
  if (data.recommendations && data.recommendations.length > 0) {
    html += '<div class="mt-3"><h6> Khuyến nghị:</h6>';
    data.recommendations.forEach(rec => {
      html += `<div class="alert alert-warning"><strong>${rec.title}:</strong> ${rec.message}</div>`;
    });
    html += '</div>';
  }
  
  resultDiv.innerHTML = html;
}

function createBudgetFromAI(categoryName, amount) {
  const form = document.getElementById('budget-form');
  const categorySelect = form?.querySelector('select[name="category_id"]');
  
  if (categorySelect) {
    const options = Array.from(categorySelect.options);
    const matchingOption = options.find(option => 
      option.text.toLowerCase().includes(categoryName.toLowerCase())
    );
    
    if (matchingOption) {
      categorySelect.value = matchingOption.value;
    }
  }
  
  const amountInput = form?.querySelector('input[name="amount"]');
  if (amountInput) {
    amountInput.value = Math.round(amount);
  }
  
  const monthInput = form?.querySelector('input[name="month"]');
  if (monthInput) {
    monthInput.value = new Date().getMonth() + 1;
  }
  
  const yearInput = form?.querySelector('input[name="year"]');
  if (yearInput) {
    yearInput.value = new Date().getFullYear();
  }
  
  document.getElementById('budget-add-btn')?.click();
}
