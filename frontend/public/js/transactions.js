let transactions = [];
let editingTransaction = null;

// JWT Utils
function getUserIdFromToken() {
  try {
    const token = localStorage.getItem('authToken');
    if (!token) return null;
    
    // Decode JWT token (payload part only)
    const payload = token.split('.')[1];
    const decoded = JSON.parse(atob(payload));
    return decoded.userId || null;
  } catch (error) {
    console.error('Error extracting userId from token:', error);
    return null;
  }
}

function getCurrentUserId() {
  const userId = getUserIdFromToken();
  if (!userId) {
    console.error('User not authenticated');
    return null;
  }
  return userId;
}

document.addEventListener('DOMContentLoaded', function() {
  const table = document.getElementById('tx-table');
  const form = document.getElementById('tx-form');
  const filter = document.getElementById('tx-filter');
  const typeFilter = document.getElementById('type-filter');

  // Load initial data
  loadTransactions();
  loadCategories();
  loadWallets();

  // OCR file input change
  const ocrInput = document.getElementById('ocrFileInput');
  if (ocrInput) {
    ocrInput.addEventListener('change', async function(e) {
      const file = e.target.files && e.target.files[0];
      if (!file) return;
      try {
        await handleOcrFile(file);
      } finally {
        // reset for next selection
        e.target.value = '';
      }
    });
  }

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
        .map(t => ({
          id: t.id,
          date: t.date,
          type: t.type, // Use the type as is since backend returns 'income'/'expense'
          category: (t.category && t.category.name) ? t.category.name : (t.categoryName || 'Khác'),
          categoryId: t.categoryId || t.categoryId || null,
          amount: t.amount,
          note: t.note || '',
          walletId: t.walletId || null
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
      updateCategoryDropdowns(data);
    })
    .catch(err => {
      console.error("🚨 Failed to load categories:", err);
      console.warn("Using default categories");
    });
}

function loadWallets() {
  const url = `http://localhost:8080/api/wallets`;
  console.log("📡 Loading wallets from:", url);
  
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
      if (!res.ok) {
        return res.text().then(text => { 
          throw new Error(`HTTP ${res.status}: ${text}`); 
        });
      }
      return res.json();
    })
    .then(data => {
      updateWalletDropdowns(data);
    })
    .catch(err => {
      console.error("🚨 Failed to load wallets:", err);
    });
}

function updateWalletDropdowns(wallets) {
  const walletSelects = document.querySelectorAll('select[name="wallet"]');
  walletSelects.forEach(select => {
    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.textContent = 'Chọn ví';
    
    select.innerHTML = '';
    select.appendChild(defaultOption);
    
    wallets.forEach(w => {
      const option = document.createElement('option');
      option.value = String(w.id);
      option.dataset.walletId = w.id;
      option.textContent = w.name;
      select.appendChild(option);
    });
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
        <button class="btn btn-sm btn-success me-1" onclick="editTransaction(${tx.id})" title="Sửa giao dịch">Sửa</button>
        <button class="btn btn-sm btn-danger" onclick="deleteTransaction(${tx.id})" title="Xóa giao dịch">Xóa</button>
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

// Trigger hidden input to select image
function triggerOcr() {
  const input = document.getElementById('ocrFileInput');
  if (input) input.click();
}

async function handleOcrFile(file) {
  try {
    showAlert('info', 'Đang quét hóa đơn, vui lòng chờ...');
    const formData = new FormData();
    formData.append('file', file);

    const token = localStorage.getItem('authToken');
    const headers = {};
    if (token) headers['Authorization'] = 'Bearer ' + token;

    const res = await fetch('http://localhost:8080/api/ocr/parse-invoice', {
      method: 'POST',
      headers,
      body: formData
    });

    if (!res.ok) {
      const text = await res.text();
      throw new Error(text || 'OCR failed');
    }
    const data = await res.json();
    if (!data.success) throw new Error(data.message || 'OCR error');

    const {
      rawText,
      suggestedAmount,
      suggestedDate,
      dateNormalized,
      amountConfidence,
      dateConfidence,
      merchant,
      amountLine,
      suggestedCategory,
      predictedCategoryId,
      predictedCategoryName,
      fileUrl
    } = data.data || {};

    // Open modal first
    showAddModal();
    const form = document.getElementById('tx-form');

    // Date: ưu tiên dateNormalized (backend đã chuẩn) nếu có, fallback suggestedDate
    const dateIso = dateNormalized || (suggestedDate ? normalizeVnDateToIso(suggestedDate) : null);
    if (dateIso) {
      form.querySelector('input[name="date"]').value = dateIso;
    } else {
      console.warn('[OCR] Không parse được ngày từ:', suggestedDate, 'dateNormalized=', dateNormalized);
    }

    // Amount
    if (suggestedAmount) {
      const amountNum = parseVnCurrencyToNumber(suggestedAmount);
      if (amountNum && amountNum > 0) {
        form.querySelector('input[name="amount"]').value = amountNum;
      }
    }

    // Category preselect (match by visible text ignoring accents simple lower compare)
    if (suggestedCategory) {
      const select = form.querySelector('select[name="category"]');
      if (select) {
        const normSuggest = suggestedCategory.trim().toLowerCase();
        for (const opt of select.options) {
          if (opt.text.trim().toLowerCase() === normSuggest) {
            select.value = opt.value;
            break;
          }
        }
      }
    }

    // Auto-select category by predictedCategoryId or predictedCategoryName from OCR response
    if (predictedCategoryId || predictedCategoryName) {
      const select = form.querySelector('select[name="category"]');
      if (select) {
        let applied = false;
        if (predictedCategoryId) {
          for (const opt of select.options) {
            if (String(opt.value) === String(predictedCategoryId)) { select.value = opt.value; applied = true; break; }
          }
        }
        if (!applied && predictedCategoryName) {
          const normPred = predictedCategoryName.trim().toLowerCase();
            for (const opt of select.options) {
              if (opt.text.trim().toLowerCase() === normPred) { select.value = opt.value; applied = true; break; }
            }
        }
        if (applied) {
          showAlert('info', 'Đã tự động gợi ý danh mục: ' + (predictedCategoryName || predictedCategoryId));
        }
      }
    }

    // Note composition: merchant (nếu có) + raw snippet
    const noteEl = form.querySelector('textarea[name="note"]');
    if (noteEl) {
      const builder = [];
      if (merchant) builder.push('[Đơn vị] ' + merchant);
      if (amountLine && (!suggestedAmount || !merchant || !amountLine.includes(merchant))) {
        builder.push('[Dòng số tiền] ' + amountLine.trim());
      }
      const preview = (rawText || '').toString().trim();
      if (preview) builder.push('--- OCR ---\n' + preview); // không cắt nữa
      if (builder.length) {
        const noteStr = builder.join('\n');
        noteEl.value = noteStr; // không giới hạn độ dài
      }
    }

    // If have stored fileUrl show preview image region
    if (fileUrl) {
      const previewWrap = document.getElementById('ocr-image-preview-wrapper');
      const imgEl = document.getElementById('ocr-image-preview');
      if (previewWrap && imgEl) {
        imgEl.src = fileUrl.startsWith('http') ? fileUrl : ('http://localhost:8080' + fileUrl); // backend served
        previewWrap.classList.remove('d-none');
      }
    }

    // Build enhanced message
    const parts = [];
    if (suggestedAmount) parts.push('Số tiền: ' + suggestedAmount + (amountConfidence ? ` (~${Math.round(amountConfidence*100)}%)` : ''));
    if (dateIso) parts.push('Ngày: ' + dateIso + (dateConfidence ? ` (~${Math.round(dateConfidence*100)}%)` : ''));
    if (merchant) parts.push('Đơn vị: ' + merchant);
    const previewMsg = (rawText && rawText.length) ? ('Nội dung: ' + rawText.slice(0, 80).replace(/\n/g, ' ') + '...') : 'Không trích xuất được nội dung từ ảnh.';
    parts.push(previewMsg);
    showAlert('success', 'Đã quét hóa đơn xong. ' + parts.join(' | ') + ' Vui lòng kiểm tra và lưu.');
  } catch (err) {
    console.error('OCR error:', err);
    showAlert('danger', 'Không thể quét hóa đơn: ' + err.message);
  }
}

function normalizeVnDateToIso(input) {
  // Matches dd/MM/yyyy or dd-MM-yyyy or yyyy/MM/dd
  if (!input) return null;
  // Clean artifacts: replace V2015 -> 2015, letter preceding year
  let cleaned = input.replace(/V(20\d{2})/gi, '$1')
                     .replace(/([\/\-])[^0-9]?((?:20)?\d{2})/g, '$1$2');
  // If 2-digit year left, expand assuming 20xx
  cleaned = cleaned.replace(/(\b\d{1,2}[\/\-]\d{1,2}[\/\-])(\d{2})\b/g, (m,p,y)=> p + '20' + y);
  const dmy = /(\b(\d{1,2})[\/\- ](\d{1,2})[\/\- ](20\d{2})\b)/;
  const ymd = /(\b(20\d{2})[\/\- ](\d{1,2})[\/\- ](\d{1,2})\b)/;
  let m;
  if ((m = cleaned.match(dmy))) {
    const day = m[2].padStart(2, '0');
    const month = m[3].padStart(2, '0');
    const year = m[4];
    return `${year}-${month}-${day}`;
  }
  if ((m = cleaned.match(ymd))) {
    const year = m[2];
    const month = m[3].padStart(2, '0');
    const day = m[4].padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
  return null;
}

function parseVnCurrencyToNumber(input) {
  if (input == null) return null;
  let raw = String(input).trim();
  if (!raw) return null;
  raw = raw.replace(/vnd|vnđ|đ/gi, '').trim();
  const hasComma = raw.includes(',');
  const hasDot = raw.includes('.');
  const separators = (raw.match(/[.,]/g) || []).length;
  const digitsOnly = raw.replace(/[^0-9]/g, '');
  if (!digitsOnly) return null;

  // Heuristic:
  // 1) Nếu có >1 dấu (.,) => coi tất cả là phân tách hàng nghìn.
  // 2) Nếu chỉ 1 dấu và phần sau dấu dài 1-2 và tổng số chữ số <=4 => coi là thập phân.
  // 3) Còn lại: bỏ hết dấu và parse integer.
  if (separators === 1) {
    const idx = Math.max(raw.indexOf(','), raw.indexOf('.'));
    const fractionalLen = raw.length - idx - 1;
    const intDigits = raw.slice(0, idx).replace(/[^0-9]/g, '');
    const fracDigits = raw.slice(idx + 1).replace(/[^0-9]/g, '');
    if (fractionalLen > 0 && fractionalLen <= 2 && (intDigits + fracDigits).length <= 4) {
      const asFloat = parseFloat(intDigits + '.' + fracDigits);
      return isNaN(asFloat) ? null : Math.round(asFloat);
    }
  }
  // Thousand grouping case
  const asInt = parseInt(digitsOnly, 10);
  if (!isNaN(asInt)) return asInt;
  return null;
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
  // If wallet loaded, try to set value
  const walletSelect = form.querySelector('select[name="wallet"]');
  if (walletSelect && transaction.walletId) {
    walletSelect.value = String(transaction.walletId);
  }
  
  const modal = new bootstrap.Modal(document.getElementById('transactionModal'));
  modal.show();
}

function clearOcrImagePreview() {
  const wrap = document.getElementById('ocr-image-preview-wrapper');
  const img = document.getElementById('ocr-image-preview');
  if (img) img.src = '';
  if (wrap) wrap.classList.add('d-none');
}

function openFullOcrImage() {
  const img = document.getElementById('ocr-image-preview');
  if (img && img.src) {
    window.open(img.src, '_blank');
  }
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
  
  // Wallet selection
  const walletSelect = form.querySelector('select[name="wallet"]');
  const selectedWallet = walletSelect ? walletSelect.options[walletSelect.selectedIndex] : null;
  const walletId = selectedWallet ? (selectedWallet.dataset.walletId || selectedWallet.value) : null;
  
  const transactionData = {
    date: formData.get('date'),
    type: formData.get('type') === 'income' ? 'income' : 'expense',
    categoryId: categoryId ? parseInt(categoryId) : null,
    amount: amount,
    note: formData.get('note') || '',
    walletId: walletId ? parseInt(walletId) : null
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
      
      // 🔗 TRIGGER INTEGRATION SYSTEM - Kích hoạt hệ thống tích hợp
      if (window.FinancialIntegration) {
        FinancialIntegration.processTransaction(transactionData, !editingTransaction);
      }
      
      // Dispatch custom event for other modules
      const event = new CustomEvent(editingTransaction ? 'transactionUpdated' : 'transactionSaved', {
        detail: { transaction: { ...transactionData, id: data.id || editingTransaction?.id } }
      });
      window.dispatchEvent(event);
      
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
  // Hiển thị số tiền với định dạng Việt Nam và đơn vị VND
  const formatted = new Intl.NumberFormat('vi-VN').format(amount);
  return `${formatted} VND`;
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

// 🔗 INTEGRATION FUNCTIONS - Các hàm liên kết giữa các chức năng

/**
 * Cập nhật việc sử dụng ngân sách khi có giao dịch mới
 */
function updateBudgetUsage(categoryId, transactionType, amount) {
  if (transactionType !== 'CHI' || !categoryId) return;
  
  const currentDate = new Date();
  const month = currentDate.getMonth() + 1;
  const year = currentDate.getFullYear();
  
  // Gọi API để cập nhật budget usage
  const token = localStorage.getItem('authToken');
  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }
  
  fetch(`http://localhost:8080/api/budgets/updateUsage`, {
    method: 'POST',
    headers: headers,
    mode: 'cors',
    body: JSON.stringify({
      categoryId: categoryId,
      amount: amount,
      month: month,
      year: year,
      userId: getCurrentUserId()
    })
  })
  .then(res => res.json())
  .then(data => {
    console.log("✅ Budget usage updated:", data);
  })
  .catch(err => {
    console.error("❌ Failed to update budget usage:", err);
  });
}

/**
 * Cập nhật tiến độ mục tiêu khi có giao dịch tiết kiệm
 */
function updateGoalProgress(transactionType, amount) {
  if (transactionType !== 'THU') return;
  
  const token = localStorage.getItem('authToken');
  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }
  
  fetch(`http://localhost:8080/api/goals/updateProgress`, {
    method: 'POST',
    headers: headers,
    mode: 'cors',
    body: JSON.stringify({
      userId: getCurrentUserId(),
      amount: amount
    })
  })
  .then(res => res.json())
  .then(data => {
    console.log("✅ Goal progress updated:", data);
  })
  .catch(err => {
    console.error("❌ Failed to update goal progress:", err);
  });
}

/**
 * Cập nhật số dư ví
 */
function updateWalletBalance(transactionType, amount) {
  const balanceChange = transactionType === 'THU' ? amount : -amount;
  
  const token = localStorage.getItem('authToken');
  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }
  
  // Backend đã tự cập nhật số dư ví khi lưu giao dịch, không cần gọi API riêng
  console.log('ℹ️ Skip client-side wallet update; handled by backend. Change:', balanceChange);
}

/**
 * Tạo thông báo về tác động của giao dịch
 */
function getTransactionImpactMessage(transaction) {
  let message = '';
  
  if (transaction.type === 'CHI') {
    message = '💳 Ngân sách đã được cập nhật.';
  } else if (transaction.type === 'THU') {
    message = '💰 Mục tiêu tiết kiệm đã được cập nhật.';
  }
  
  return message;
}

/**
 * Kiểm tra và cảnh báo vượt ngân sách
 */
function checkBudgetAlert(categoryId, amount) {
  const token = localStorage.getItem('authToken');
  const headers = {
    'Content-Type': 'application/json'
  };
  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }
  
  fetch(`http://localhost:8080/api/budgets/check/${categoryId}?userId=${getCurrentUserId()}&amount=${amount}`, {
    method: 'GET',
    headers: headers,
    mode: 'cors'
  })
  .then(res => res.json())
  .then(data => {
    if (data.exceeded) {
      showAlert('warning', `⚠️ Cảnh báo: Bạn đã vượt ngân sách ${data.categoryName} ${data.percentage}%!`);
    } else if (data.nearLimit) {
      showAlert('info', `📊 Thông tin: Bạn đã sử dụng ${data.percentage}% ngân sách ${data.categoryName}.`);
    }
  })
  .catch(err => {
    console.error("❌ Failed to check budget:", err);
  });
}
