let transactions = [];
let editingTransaction = null;
const userId = 1; // User ID for API calls

document.addEventListener('DOMContentLoaded', function() {
  const table = document.getElementById('tx-table');
  const form = document.getElementById('tx-form');
  const filter = document.getElementById('tx-filter');
  const typeFilter = document.getElementById('type-filter');

  // Load initial data
  loadTransactions();
  loadCategories();

  // Filter events
  if (filter) filter.addEventListener('change', applyFilters);
  if (typeFilter) typeFilter.addEventListener('change', applyFilters);

  // Set default date to today
  const dateInput = form?.querySelector('input[name="date"]');
  if (dateInput) {
    dateInput.value = new Date().toISOString().split('T')[0];
  }
});

function loadTransactions() {
  const url = `http://localhost:8080/api/transactions`;
  console.log("📡 Loading transactions from:", url);
  
  const token = localStorage.getItem('authToken');
  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }
  
  fetch(url, { 
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
      console.log("✅ Transactions loaded:", data);
      transactions = data
        .filter(t => t.userId === userId) // Filter by userId on frontend
        .map(t => ({
          id: t.id,
          date: t.date,
          type: t.type === 'CHI' ? 'expense' : 'income',
          category: t.category?.name || 'Khác',
          categoryId: t.categoryId,
          amount: t.amount,
          note: t.note || ''
        }));
      renderTable();
    })
    .catch(err => {
      console.error("🚨 Failed to load transactions:", err);
      showAlert('danger', 'Không thể tải danh sách giao dịch: ' + err.message);
    });
}

function loadCategories() {
  const url = `http://localhost:8080/api/categories`;
  console.log("📡 Loading categories from:", url);
  
  const token = localStorage.getItem('authToken');
  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }
  
  fetch(url, { 
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
      console.log("✅ Categories loaded:", data);
      updateCategoryDropdowns(data.filter(c => c.userId === userId || !c.userId));
    })
    .catch(err => {
      console.error("🚨 Failed to load categories:", err);
      console.warn("Using default categories");
    });
}

function updateCategoryDropdowns(categories) {
  const categorySelects = document.querySelectorAll('select[name="category"]');
  categorySelects.forEach(select => {
    // Keep default option
    const defaultOption = select.querySelector('option[value=""]');
    select.innerHTML = '';
    if (defaultOption) {
      select.appendChild(defaultOption);
    }
    
    // Add categories from API
    categories.forEach(cat => {
      const option = document.createElement('option');
      option.value = cat.name;
      option.textContent = cat.name;
      option.dataset.categoryId = cat.id; // Add categoryId as data attribute
      select.appendChild(option);
    });
  });

  // Also update filter dropdown
  const filterSelect = document.getElementById('tx-filter');
  if (filterSelect) {
    const defaultOption = filterSelect.querySelector('option[value=""]');
    filterSelect.innerHTML = '';
    if (defaultOption) {
      filterSelect.appendChild(defaultOption);
    }
    
    categories.forEach(cat => {
      const option = document.createElement('option');
      option.value = cat.name;
      option.textContent = cat.name;
      filterSelect.appendChild(option);
    });
  }
}

function renderTable(filteredData = transactions) {
  const tbody = document.querySelector('#tx-table tbody');
  
  if (filteredData.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="6" class="text-center py-5">
          <div class="text-muted">Không có giao dịch nào</div>
        </td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = filteredData.map(tx => `
    <tr>
      <td>${new Date(tx.date).toLocaleDateString('vi-VN')}</td>
      <td>
        <span class="badge ${tx.type === 'income' ? 'bg-success' : 'bg-danger'}">
          ${tx.type === 'income' ? 'Thu nhập' : 'Chi tiêu'}
        </span>
      </td>
      <td>${tx.category}</td>
      <td class="${tx.type === 'income' ? 'text-success' : 'text-danger'} fw-bold">
        ${tx.type === 'income' ? '+' : '-'}${formatCurrency(tx.amount)}
      </td>
      <td>${tx.note || '<em class="text-muted">Không có ghi chú</em>'}</td>
      <td>
        <button class="btn btn-sm btn-success me-1" onclick="editTransaction(${tx.id})" title="Sửa giao dịch">
          Sửa
        </button>
        <button class="btn btn-sm btn-danger" onclick="deleteTransaction(${tx.id})" title="Xóa giao dịch">
          Xóa
        </button>
      </td>
    </tr>
  `).join('');
}

function applyFilters() {
  const categoryFilter = document.getElementById('tx-filter').value;
  const typeFilter = document.getElementById('type-filter').value;
  
  let filtered = transactions;
  
  if (categoryFilter) {
    filtered = filtered.filter(tx => tx.category === categoryFilter);
  }
  
  if (typeFilter) {
    filtered = filtered.filter(tx => tx.type === typeFilter);
  }
  
  renderTable(filtered);
}

function showAddModal() {
  editingTransaction = null;
  document.getElementById('modalTitle').textContent = 'Thêm giao dịch';
  document.getElementById('tx-form').reset();
  
  // Set default date to today
  const dateInput = document.querySelector('input[name="date"]');
  if (dateInput) {
    dateInput.value = new Date().toISOString().split('T')[0];
  }
  
  const modal = new bootstrap.Modal(document.getElementById('transactionModal'));
  modal.show();
}

function editTransaction(id) {
  const transaction = transactions.find(tx => tx.id === id);
  if (!transaction) return;
  
  editingTransaction = transaction;
  document.getElementById('modalTitle').textContent = 'Sửa giao dịch';
  
  const form = document.getElementById('tx-form');
  const txIdField = form.querySelector('#txId');
  if (txIdField) txIdField.value = transaction.id;
  
  form.querySelector('input[name="date"]').value = transaction.date;
  form.querySelector('select[name="type"]').value = transaction.type;
  form.querySelector('select[name="category"]').value = transaction.category;
  form.querySelector('input[name="amount"]').value = transaction.amount;
  form.querySelector('textarea[name="note"]').value = transaction.note || '';
  
  const modal = new bootstrap.Modal(document.getElementById('transactionModal'));
  modal.show();
}

function saveTransaction() {
  const form = document.getElementById('tx-form');
  const formData = new FormData(form);
  
  // Enhanced validation
  if (!formData.get('date')) {
    showAlert('danger', 'Vui lòng chọn ngày giao dịch');
    return;
  }
  
  if (!formData.get('type')) {
    showAlert('danger', 'Vui lòng chọn loại giao dịch');
    return;
  }
  
  if (!formData.get('category')) {
    showAlert('danger', 'Vui lòng chọn danh mục');
    return;
  }
  
  const amount = parseFloat(formData.get('amount'));
  if (!amount || amount <= 0) {
    showAlert('danger', 'Vui lòng nhập số tiền hợp lệ (lớn hơn 0)');
    return;
  }
  
  // Get categoryId from dropdown
  const categorySelect = form.querySelector('select[name="category"]');
  const selectedOption = categorySelect.options[categorySelect.selectedIndex];
  const categoryId = selectedOption.dataset.categoryId;
  
  const transactionData = {
    date: formData.get('date'),
    type: formData.get('type') === 'income' ? 'THU' : 'CHI',
    categoryId: categoryId ? parseInt(categoryId) : null,
    amount: amount,
    note: formData.get('note') || '',
    userId: userId
  };
  
  console.log("📤 Sending transaction data:", transactionData);
  
  const url = editingTransaction 
    ? `http://localhost:8080/api/transactions/${editingTransaction.id}`
    : `http://localhost:8080/api/transactions`;
  
  const method = editingTransaction ? 'PUT' : 'POST';
  
  console.log(`📡 ${method} transaction to:`, url);
  
  const token = localStorage.getItem('authToken');
  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }
  
  fetch(url, {
    method: method,
    headers: headers,
    mode: 'cors',
    body: JSON.stringify(transactionData)
  })
    .then(res => {
      console.log("🔍 Save response status:", res.status);
      if (!res.ok) {
        return res.text().then(text => { 
          console.error("❌ Save error:", text);
          throw new Error(`HTTP ${res.status}: ${text}`); 
        });
      }
      return res.json();
    })
    .then(data => {
      console.log("✅ Transaction saved:", data);
      showAlert('success', editingTransaction ? 'Cập nhật giao dịch thành công!' : 'Thêm giao dịch thành công!');
      
      // Reload transactions
      loadTransactions();
      
      // Close modal
      const modal = bootstrap.Modal.getInstance(document.getElementById('transactionModal'));
      if (modal) {
        modal.hide();
      }
      
      // Reset form
      form.reset();
      editingTransaction = null;
    })
    .catch(err => {
      console.error("🚨 Failed to save transaction:", err);
      showAlert('danger', 'Lỗi khi lưu giao dịch: ' + err.message);
    });
}

function deleteTransaction(id) {
  const transaction = transactions.find(tx => tx.id === id);
  if (!transaction) return;
  
  // Create a more elegant confirmation modal
  const confirmMessage = `Bạn có chắc muốn xóa giao dịch này không?\n\n` +
                         `Danh mục: ${transaction.category}\n` +
                         `Số tiền: ${formatCurrency(transaction.amount)}\n` +
                         `Ngày: ${new Date(transaction.date).toLocaleDateString('vi-VN')}`;
  
  if (confirm(confirmMessage)) {
    const url = `http://localhost:8080/api/transactions/${id}`;
    console.log("📡 Deleting transaction:", url);
    
    const token = localStorage.getItem('authToken');
    const headers = {
      'Content-Type': 'application/json'
    };
    if (token) {
      headers['Authorization'] = 'Bearer ' + token;
    }
    
    fetch(url, {
      method: 'DELETE',
      headers: headers,
      mode: 'cors'
    })
      .then(res => {
        console.log("🔍 Delete response status:", res.status);
        if (!res.ok) {
          return res.text().then(text => { 
            console.error("❌ Delete error:", text);
            throw new Error(`HTTP ${res.status}: ${text}`); 
          });
        }
        return res.status === 204 ? null : res.json(); // Handle no content response
      })
      .then(data => {
        console.log("✅ Transaction deleted");
        showAlert('success', 'Xóa giao dịch thành công!');
        
        // Reload transactions
        loadTransactions();
      })
      .catch(err => {
        console.error("🚨 Failed to delete transaction:", err);
        showAlert('danger', 'Lỗi khi xóa giao dịch: ' + err.message);
      });
  }
}

function formatCurrency(amount) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount);
}

function showAlert(type, message) {
  const alertDiv = document.createElement('div');
  alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
  alertDiv.style.position = 'fixed';
  alertDiv.style.top = '20px';
  alertDiv.style.right = '20px';
  alertDiv.style.zIndex = '1055';
  alertDiv.style.minWidth = '300px';
  alertDiv.innerHTML = `
    <strong>${message}</strong>
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
  `;
  document.body.appendChild(alertDiv);
  
  // Auto remove after 3 seconds
  setTimeout(() => {
    if (alertDiv.parentNode) {
      alertDiv.remove();
    }
  }, 3000);
}
