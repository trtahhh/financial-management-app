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
            <td>${b.amount?.toLocaleString('vi-VN')} VNĐ</td>
            <td>
              <div class="d-flex flex-column">
                <span class="fw-bold">${(b.spentAmount || 0).toLocaleString('vi-VN')} VNĐ</span>
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
    f.year.value = new Date().getFullYear(); // Set default year
    f.month.value = new Date().getMonth() + 1; // Set default month
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
    
    console.log('Sending budget data:', data);
    
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
  loadCategories().then(load);
});
