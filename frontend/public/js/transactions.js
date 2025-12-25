console.log('🚀 transactions.js loaded');

let transactions = [];
let categories = []; // Store categories for AI lookup
let wallets = []; // Store wallets for lookup
let editingTransaction = null;

console.log('✅ Global variables initialized');

// JWT Utils
function getUserIdFromToken() {
 try {
 const token = localStorage.getItem('accessToken');
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

 // Load initial data - wallets and categories first, then transactions
 Promise.all([
 loadWallets(),
 loadCategories()
 ]).then(() => {
 loadTransactions();
 });

 // Re-populate dropdowns when modal is shown
 const txModal = document.getElementById('txModal');
 if (txModal) {
 txModal.addEventListener('show.bs.modal', function() {
 console.log('🔔 Modal opening, re-populating dropdowns...');
 if (categories.length > 0) {
 updateCategoryDropdowns(categories);
 }
 if (wallets.length > 0) {
 updateWalletDropdowns(wallets);
 }
 });
 }
 
 // Attach save button handler ONCE at page load
 const saveBtn = document.getElementById('saveTransactionBtn');
 if (saveBtn) {
 console.log('✅ Save button found, attaching handler');
 saveBtn.addEventListener('click', function(e) {
 console.log('🖱️ SAVE BUTTON CLICKED!');
 e.preventDefault();
 e.stopPropagation();
 
 try {
 console.log('📞 Calling saveTransaction()...');
 saveTransaction();
 console.log('✅ saveTransaction() completed');
 } catch (err) {
 console.error('💥 Error calling saveTransaction:', err);
 alert('Lỗi: ' + err.message);
 }
 });
 } else {
 console.error('❌ Save button not found by ID: saveTransactionBtn');
 }

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

 // Time range filter event
 const timeRangeFilter = document.getElementById('time-range-filter');
 if (timeRangeFilter) {
 timeRangeFilter.addEventListener('change', handleTimeRangeChange);
 }

 // Set default date to today
 const dateInput = form?.querySelector('input[name="date"]');
 if (dateInput) {
 dateInput.value = new Date().toISOString().split('T')[0];
 }

 // Add AI auto-categorize feature
 addAIAutoCategorize();
});

// ============= AI AUTO-CATEGORIZE FEATURE =============
function addAIAutoCategorize() {
 const noteInput = document.querySelector('input[name="note"], textarea[name="note"]');
 const amountInput = document.querySelector('input[name="amount"]');
 
 if (!noteInput || !amountInput) return;
 
 let autoCategorizeTimeout;
 
 noteInput.addEventListener('input', function() {
 clearTimeout(autoCategorizeTimeout);
 const description = this.value.trim();
 const amount = parseFloat(amountInput.value);
 
 if (description.length > 3 && amount > 0) {
 autoCategorizeTimeout = setTimeout(() => {
 triggerAutoCategorize(description, amount);
 }, 1500);
 }
 });
 
 amountInput.addEventListener('blur', function() {
 const description = noteInput.value.trim();
 const amount = parseFloat(this.value);
 
 if (description.length > 3 && amount > 0) {
 setTimeout(() => triggerAutoCategorize(description, amount), 500);
 }
 });
}

async function triggerAutoCategorize(description, amount) {
 const categoryIdInput = document.getElementById('suggestedCategoryId');
 const categoryNameInput = document.getElementById('suggestedCategoryName');
 const categoryHint = document.getElementById('categoryHint');
 const categoryConfidence = document.getElementById('categoryConfidence');
 
 if (!categoryIdInput || !categoryNameInput) return;
 
 // Show loading state
 const originalPlaceholder = categoryNameInput.placeholder;
 categoryNameInput.placeholder = 'AI đang phân tích...';
 categoryNameInput.style.backgroundColor = '#fff3cd';
 
 try {
 const token = localStorage.getItem('accessToken');
 const response = await fetch('http://localhost:8080/api/ai/auto-categorize', {
 method: 'POST',
 headers: {
 'Content-Type': 'application/json',
 'Authorization': token ? `Bearer ${token}` : ''
 },
 body: JSON.stringify({
 description: description,
 amount: amount
 })
 });
 
 if (!response.ok) throw new Error('AI service error');
 
 const result = await response.json();
 console.log('🤖 AI Response:', result);
 
 if (result.success && result.primaryCategory) {
 const category = result.primaryCategory;
 console.log('📂 Primary Category:', category);
 
 if (category.id) {
 // Set hidden input value
 categoryIdInput.value = category.id;
 console.log('✅ Set categoryId:', category.id);
 
 // Set display name
 categoryNameInput.value = category.name;
 categoryNameInput.style.backgroundColor = '#d1e7dd';
 
 // Show confidence badge
 if (categoryConfidence) {
 categoryConfidence.style.display = 'block';
 categoryConfidence.innerHTML = `<i class="fas fa-check-circle text-success"></i> ${category.confidence.toFixed(0)}%`;
 }
 
 // Update hint
 if (categoryHint) {
 categoryHint.textContent = `AI đã phân loại tự động với độ tin cậy ${category.confidence.toFixed(0)}%`;
 categoryHint.className = 'text-success';
 }
 
 console.log(' Auto-categorized:', category.name, '(ID:', category.id, ')');
 
 setTimeout(() => {
 categoryNameInput.style.backgroundColor = '#f8f9fa';
 }, 2000);
 }
 } else {
 // AI không tìm thấy category phù hợp
 categoryNameInput.placeholder = 'AI chưa tìm thấy danh mục phù hợp';
 categoryNameInput.style.backgroundColor = '#f8d7da';
 
 if (categoryHint) {
 categoryHint.textContent = 'Vui lòng nhập mô tả chi tiết hơn để AI có thể phân loại';
 categoryHint.className = 'text-danger';
 }
 
 console.log(' Could not find category ID for:', description);
 
 setTimeout(() => {
 categoryNameInput.placeholder = originalPlaceholder;
 categoryNameInput.style.backgroundColor = '#f8f9fa';
 if (categoryHint) {
 categoryHint.textContent = 'AI sẽ tự động phân loại sau khi bạn nhập ghi chú';
 categoryHint.className = 'text-muted';
 }
 }, 3000);
 }
 } catch (error) {
 console.error('Auto-categorize error:', error);
 categoryNameInput.placeholder = originalPlaceholder;
 categoryNameInput.style.backgroundColor = '#f8f9fa';
 }
}

function loadTransactions() {
 const url = `http://localhost:8080/api/transactions`;
 console.log(" Loading transactions from:", url);
 
 const token = localStorage.getItem('accessToken');
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
 console.error(" Transactions error:", text);
 throw new Error(`HTTP ${res.status}: ${text}`); 
 });
 }
 return res.json();
 })
 .then(data => {
 console.log(" Transactions loaded:", data);
 transactions = data
 .map(t => {
 const wallet = wallets.find(w => w.id === t.walletId);
 return {
 id: t.id,
 date: t.date,
 type: t.type, // Use the type as is since backend returns 'income'/'expense'
 category: (t.category && t.category.name) ? t.category.name : (t.categoryName || 'Khác'),
 categoryId: t.categoryId || t.categoryId || null,
 amount: t.amount,
 note: t.note || '',
 walletId: t.walletId || null,
 walletName: (t.wallet && t.wallet.name) ? t.wallet.name : (wallet ? wallet.name : 'Ví mặc định')
 };
 });
 console.log(" Transactions loaded and mapped:", transactions.length, 'items');
 return transactions; // ✅ IMPORTANT: Return data to maintain Promise chain
 })
 .catch(err => {
 console.error(" Failed to load transactions:", err);
 showAlert('danger', 'Không thể tải danh sách giao dịch: ' + err.message);
 throw err; // ✅ IMPORTANT: Re-throw to maintain Promise chain for caller
 });
}

function updateStats(filteredData = transactions) {
 // Calculate totals
 const totalIncome = filteredData
 .filter(t => t.type === 'income')
 .reduce((sum, t) => sum + t.amount, 0);
 
 const totalExpense = filteredData
 .filter(t => t.type === 'expense')
 .reduce((sum, t) => sum + t.amount, 0);
 
 const totalCount = filteredData.length;
 const avgAmount = totalCount > 0 ? (totalIncome + totalExpense) / totalCount : 0;
 
 // Update UI
 const totalIncomeEl = document.getElementById('totalIncome');
 const totalExpenseEl = document.getElementById('totalExpense');
 const totalCountEl = document.getElementById('totalCount');
 const avgAmountEl = document.getElementById('avgAmount');
 
 if (totalIncomeEl) totalIncomeEl.textContent = formatCurrency(totalIncome);
 if (totalExpenseEl) totalExpenseEl.textContent = formatCurrency(totalExpense);
 if (totalCountEl) totalCountEl.textContent = totalCount;
 if (avgAmountEl) avgAmountEl.textContent = formatCurrency(avgAmount);
}

function loadCategories() {
 const url = `http://localhost:8080/api/categories`;
 console.log(" Loading categories from:", url);
 
 const token = localStorage.getItem('accessToken');
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
 console.error(" Categories error:", text);
 throw new Error(`HTTP ${res.status}: ${text}`); 
 });
 }
 return res.json();
 })
 .then(data => {
 console.log(" Categories loaded:", data);
 console.log(" Categories type:", typeof data, "isArray:", Array.isArray(data));
 
 // Handle different response formats
 let categoryList;
 if (Array.isArray(data)) {
 categoryList = data;
 } else if (data.content && Array.isArray(data.content)) {
 // Spring Boot Page response
 categoryList = data.content;
 } else if (data.data && Array.isArray(data.data)) {
 categoryList = data.data;
 } else if (typeof data === 'object' && data !== null) {
 // Try to extract array from object
 const keys = Object.keys(data);
 console.log(" Object keys:", keys);
 if (keys.length > 0 && Array.isArray(data[keys[0]])) {
 categoryList = data[keys[0]];
 } else {
 // Object might be key-value pairs, convert to array
 categoryList = Object.values(data);
 }
 } else {
 categoryList = [];
 }
 
 console.log(" Extracted categoryList:", categoryList, "length:", categoryList.length);
 categories = categoryList; // Store for AI lookup
 updateCategoryDropdowns(categoryList);
 })
 .catch(err => {
 console.error(" Failed to load categories:", err);
 console.warn("Using default categories");
 });
}

function loadWallets() {
 const url = `http://localhost:8080/api/wallets`;
 console.log(" Loading wallets from:", url);
 
 const token = localStorage.getItem('accessToken');
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
 if (!res.ok) {
 return res.text().then(text => { 
 throw new Error(`HTTP ${res.status}: ${text}`); 
 });
 }
 return res.json();
 })
 .then(data => {
 console.log(" Wallets loaded:", data);
 console.log(" Wallets type:", typeof data, "isArray:", Array.isArray(data));
 
 // Handle different response formats
 let walletList;
 if (Array.isArray(data)) {
 walletList = data;
 } else if (data.content && Array.isArray(data.content)) {
 walletList = data.content;
 } else if (data.data && Array.isArray(data.data)) {
 walletList = data.data;
 } else if (typeof data === 'object' && data !== null) {
 const keys = Object.keys(data);
 console.log(" Object keys:", keys);
 if (keys.length > 0 && Array.isArray(data[keys[0]])) {
 walletList = data[keys[0]];
 } else {
 walletList = Object.values(data);
 }
 } else {
 walletList = [];
 }
 
 console.log(" Extracted walletList:", walletList, "length:", walletList.length);
 wallets = walletList; // Store for lookup
 updateWalletDropdowns(walletList);
 })
 .catch(err => {
 console.error(" Failed to load wallets:", err);
 });
}

function updateWalletDropdowns(wallets) {
 console.log('🔄 Updating wallet dropdowns with', wallets.length, 'wallets');
 const walletSelects = document.querySelectorAll('select[name="wallet"], #tx-wallet');
 console.log('💰 Found', walletSelects.length, 'wallet select elements');
 
 walletSelects.forEach(select => {
 console.log('➡️ Updating select:', select.id || select.name);
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
 console.log('✅ Select updated with', select.options.length, 'options');
 });
}

function updateCategoryDropdowns(categories) {
 console.log('🔄 Updating category dropdowns with', categories.length, 'categories');
 const categorySelects = document.querySelectorAll('select[name="category"], #tx-category');
 console.log('📋 Found', categorySelects.length, 'category select elements');
 
 categorySelects.forEach(select => {
 console.log('➡️ Updating select:', select.id || select.name);
 // Remember current selected value
 const currentValue = select.value;
 console.log('💾 Current selected value:', currentValue);
 
 // Keep default option
 const defaultOption = select.querySelector('option[value=""]');
 select.innerHTML = '';
 if (defaultOption) {
 select.appendChild(defaultOption.cloneNode(true));
 } else {
 const newDefault = document.createElement('option');
 newDefault.value = '';
 newDefault.textContent = 'Chọn danh mục';
 select.appendChild(newDefault);
 }
 
 // Add categories from API
 categories.forEach(cat => {
 const option = document.createElement('option');
 option.value = cat.name;
 option.textContent = cat.name;
 option.dataset.categoryId = cat.id; // Add categoryId as data attribute
 select.appendChild(option);
 });
 console.log('✅ Select updated with', select.options.length, 'options');
 
 // Restore selected value if it still exists in options
 if (currentValue && Array.from(select.options).some(opt => opt.value === currentValue)) {
 select.value = currentValue;
 console.log('🔄 Restored selected value:', currentValue);
 }
 });

 // Also update filter dropdown
 const filterSelect = document.getElementById('tx-filter');
 if (filterSelect) {
 const defaultOption = filterSelect.querySelector('option[value=""]');
 filterSelect.innerHTML = '';
 if (defaultOption) {
 filterSelect.appendChild(defaultOption.cloneNode(true));
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
 const tbody = document.querySelector('#tx-list');
 
 if (!tbody) {
 console.error('Element #tx-list not found');
 return;
 }
 
 if (filteredData.length === 0) {
 tbody.innerHTML = `
 <tr>
 <td colspan="7" class="text-center py-5">
 <div class="text-muted">Không có giao dịch nào</div>
 </td>
 </tr>
 `;
 return;
 }

 tbody.innerHTML = filteredData.map(tx => `
 <tr>
 <td>
 <input type="checkbox" class="tx-checkbox" data-id="${tx.id}">
 </td>
 <td>${new Date(tx.date).toLocaleDateString('vi-VN')}</td>
 <td>
 <span class="badge ${tx.type === 'income' ? 'badge-success' : 'badge-danger'} badge-sm">
 <i class="bi ${tx.type === 'income' ? 'bi-arrow-down' : 'bi-arrow-up'}"></i>
 ${tx.category || 'Khác'}
 </span>
 </td>
 <td>${tx.note || '<span class="text-secondary">Không có mô tả</span>'}</td>
 <td>
 <i class="bi bi-wallet2"></i>
 ${tx.walletName || 'Ví mặc định'}
 </td>
 <td class="text-end">
 <span class="fw-bold ${tx.type === 'income' ? 'text-success' : 'text-danger'}">
 ${tx.type === 'income' ? '+' : '-'}${formatCurrency(tx.amount)}
 </span>
 </td>
 <td class="text-center">
 <div class="btn-group btn-group-sm">
 <button class="btn btn-outline-primary" onclick="editTransaction(${tx.id})" title="Sửa">
 <i class="bi bi-pencil"></i>
 </button>
 <button class="btn btn-outline-danger" onclick="deleteTransaction(${tx.id})" title="Xóa">
 <i class="bi bi-trash"></i>
 </button>
 </div>
 </td>
 </tr>
 `).join('');
}

// Handle time range filter changes
function handleTimeRangeChange() {
 const timeRangeValue = document.getElementById('time-range-filter')?.value || 'all';
 const customDateRange = document.getElementById('custom-date-range');
 const customDateTo = document.getElementById('custom-date-to');
 const dateFromInput = document.getElementById('date-from');
 const dateToInput = document.getElementById('date-to');
 const today = new Date();
 
 console.log('🕐 Time range changed to:', timeRangeValue);
 
 // Show/hide custom date inputs
 if (timeRangeValue === 'custom') {
 customDateRange?.classList.remove('d-none');
 customDateTo?.classList.remove('d-none');
 return; // Let user set custom dates manually
 } else {
 customDateRange?.classList.add('d-none');
 customDateTo?.classList.add('d-none');
 }
 
 // Calculate and set date range
 let dateFrom, dateTo;
 
 switch(timeRangeValue) {
 case 'month':
 // This month: from 1st to today
 dateFrom = new Date(today.getFullYear(), today.getMonth(), 1);
 dateTo = today;
 console.log('📅 Month range:', dateFrom, 'to', dateTo);
 break;
 case 'year':
 // This year: from Jan 1 to today
 dateFrom = new Date(today.getFullYear(), 0, 1);
 dateTo = today;
 console.log('📅 Year range:', dateFrom, 'to', dateTo);
 break;
 case 'all':
 // All transactions: clear date filters
 dateFrom = null;
 dateTo = null;
 console.log('📅 All transactions - no date filter');
 break;
 default:
 dateFrom = null;
 dateTo = null;
 }
 
 // Update date inputs
 if (dateFrom) {
 const dateFromStr = dateFrom.toISOString().split('T')[0];
 dateFromInput.value = dateFromStr;
 console.log('✅ date-from set to:', dateFromStr);
 } else {
 dateFromInput.value = '';
 console.log('✅ date-from cleared');
 }
 
 if (dateTo) {
 const dateToStr = dateTo.toISOString().split('T')[0];
 dateToInput.value = dateToStr;
 console.log('✅ date-to set to:', dateToStr);
 } else {
 dateToInput.value = '';
 console.log('✅ date-to cleared');
 }
 
 // Apply filters
 if (typeof applyFilters === 'function') {
 applyFilters();
 }
}

function applyFilters() {
 const categoryFilter = document.getElementById('tx-filter')?.value || '';
 const typeFilter = document.getElementById('type-filter')?.value || '';
 const dateFromInput = document.getElementById('date-from')?.value || '';
 const dateToInput = document.getElementById('date-to')?.value || '';
 const searchInput = document.getElementById('search-input')?.value?.toLowerCase() || '';
 
 console.log('🔍 Applying filters:', { categoryFilter, typeFilter, dateFromInput, dateToInput, searchInput });
 console.log('📊 Total transactions before filter:', transactions.length);
 
 let filtered = transactions;
 
 // Filter by category
 if (categoryFilter) {
 filtered = filtered.filter(tx => tx.category === categoryFilter);
 console.log(`✅ After category filter (${categoryFilter}):`, filtered.length);
 }
 
 // Filter by type (income/expense)
 if (typeFilter) {
 filtered = filtered.filter(tx => tx.type === typeFilter);
 console.log(`✅ After type filter (${typeFilter}):`, filtered.length);
 }
 
 // Filter by date range - proper date comparison
 if (dateFromInput) {
 const fromDate = new Date(dateFromInput);
 filtered = filtered.filter(tx => {
 const txDate = new Date(tx.date);
 return txDate >= fromDate;
 });
 console.log(`✅ After dateFrom filter (${dateFromInput}):`, filtered.length);
 }
 
 if (dateToInput) {
 const toDate = new Date(dateToInput);
 // Add 1 day to include the entire 'to' day
 toDate.setDate(toDate.getDate() + 1);
 filtered = filtered.filter(tx => {
 const txDate = new Date(tx.date);
 return txDate < toDate;
 });
 console.log(`✅ After dateTo filter (${dateToInput}):`, filtered.length);
 }
 
 // Filter by search text
 if (searchInput) {
 filtered = filtered.filter(tx => 
 tx.note?.toLowerCase().includes(searchInput) ||
 tx.category?.toLowerCase().includes(searchInput) ||
 tx.walletName?.toLowerCase().includes(searchInput)
 );
 console.log(`✅ After search filter:`, filtered.length);
 }
 
 console.log('📊 Final filtered transactions:', filtered.length);
 
 updateStats(filtered);
 renderTable(filtered);
}

function showAddModal() {
 editingTransaction = null;
 document.getElementById('modalTitle').textContent = 'Thêm giao dịch';
 const form = document.getElementById('txForm');
 if (form) form.reset();
 
 // Reset AI suggestion fields
 const categoryIdInput = document.getElementById('suggestedCategoryId');
 const categoryNameInput = document.getElementById('suggestedCategoryName');
 const categoryHint = document.getElementById('categoryHint');
 const categoryConfidence = document.getElementById('categoryConfidence');
 
 if (categoryIdInput) categoryIdInput.value = '';
 if (categoryNameInput) {
 categoryNameInput.value = '';
 categoryNameInput.placeholder = 'Nhập ghi chú để AI gợi ý danh mục...';
 categoryNameInput.style.backgroundColor = '#f8f9fa';
 }
 if (categoryHint) {
 categoryHint.textContent = 'AI sẽ tự động phân loại sau khi bạn nhập ghi chú';
 categoryHint.className = 'text-muted';
 }
 if (categoryConfidence) {
 categoryConfidence.style.display = 'none';
 categoryConfidence.innerHTML = '';
 }
 
 // Set default date to today
 const dateInput = document.getElementById('tx-date');
 if (dateInput) {
 dateInput.value = new Date().toISOString().split('T')[0];
 }
 
 const modal = new bootstrap.Modal(document.getElementById('txModal'));
 modal.show();
}

// Trigger hidden input to select image
function triggerOcr() {
 const input = document.getElementById('ocrFileInput');
 if (input) input.click();
}

async function handleOcrFile(file) {
 // Prevent duplicate OCR runs
 if (window._ocrInProgress) {
     console.warn('[OCR] Duplicate call ignored - already processing');
     return;
 }
 window._ocrInProgress = true;

 // Show loading modal
 const loadingModal = new bootstrap.Modal(document.getElementById('ocrLoadingModal'));
 loadingModal.show();
 
 try {
 const formData = new FormData();
 formData.append('file', file);

 const token = localStorage.getItem('accessToken');
 const headers = {};
 if (token) headers['Authorization'] = 'Bearer ' + token;

 const res = await fetch('http://localhost:8080/api/ocr/parse-invoice', {
 method: 'POST',
 headers,
 body: formData
 });

 if (!res.ok) {
 const text = await res.text();
 closeOcrModal();
 throw new Error(text || 'OCR failed');
 }
 const data = await res.json();
 if (!data.success) {
 closeOcrModal();
 throw new Error(data.message || 'OCR error');
 }

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

 // Close loading modal
 closeOcrModal();
 
 // Wait a bit for loading modal to close
 await new Promise(resolve => setTimeout(resolve, 300));
 
 // Open form modal
 showAddModal();
 
 // Wait for modal to be fully rendered - check multiple times
 let form = null;
 let attempts = 0;
 const maxAttempts = 20; // Try for 2 seconds
 
 while (!form && attempts < maxAttempts) {
     await new Promise(resolve => setTimeout(resolve, 100));
     form = document.getElementById('txForm');
     attempts++;
 }
 
 if (!form) {
     console.error('[OCR] Form not found after opening modal (waited ' + (attempts * 100) + 'ms)');
     showAlert('danger', 'Không thể mở form giao dịch. Vui lòng thử lại.');
     return;
 }
 
 console.log('[OCR] Form found after ' + (attempts * 100) + 'ms');

 // Date: ưu tiên dateNormalized (backend đã chuẩn) nếu có, fallback suggestedDate
 const dateIso = dateNormalized || (suggestedDate ? normalizeVnDateToIso(suggestedDate) : null);
 if (dateIso) {
 const dateInput = document.getElementById('tx-date');
 if (dateInput) dateInput.value = dateIso;
 } else {
 console.warn('[OCR] Không parse được ngày từ:', suggestedDate, 'dateNormalized=', dateNormalized);
 }

 // Amount - wait for form to be fully ready
 let amountAttempts = 0;
 const maxAmountAttempts = 10;
 const fillAmount = () => {
 const amountInput = document.getElementById('tx-amount');
 if (amountInput && suggestedAmount) {
 // Parse the amount to get raw number
 const parsed = parseVnCurrencyToNumber(suggestedAmount);
 if (parsed && parsed > 0) {
 // Use the raw parsed number, not the formatted string
 amountInput.value = parsed.toString();
 console.log('[OCR] Auto-filled amount:', amountInput.value, 'from:', suggestedAmount);
 return true; // Success
 }
 }
 if (amountAttempts < maxAmountAttempts) {
 amountAttempts++;
 setTimeout(fillAmount, 200);
 }
 return false;
 };
 setTimeout(fillAmount, 300);

 // Category preselect (match by visible text ignoring accents simple lower compare)
 // Lock flag to prevent later blocks from overwriting a manually-forced category
 let categoryLocked = false;
 if (suggestedCategory) {
 const select = document.getElementById('tx-category');
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

// Merchant keyword mappings: common heuristics
// If merchant contains VINMART => Mua sắm; COFFEE/CAFE => Ăn uống
if (merchant) {
    try {
        const m = merchant.toUpperCase();
        let forcedCategory = null;
        if (m.includes('VINMART')) forcedCategory = 'mua sắm';
        else if (m.includes('COFFEE') || m.includes('CAFE')) forcedCategory = 'ăn uống';

        if (forcedCategory) {
            const select = document.getElementById('tx-category');
            if (select) {
                const wanted = forcedCategory.toLowerCase();
                for (const opt of select.options) {
                    const text = opt.text.trim().toLowerCase();
                    if (text.includes(wanted) || (wanted === 'ăn uống' && (text.includes('ăn') || text.includes('uống')))) {
                        select.value = opt.value;
                        // Prevent subsequent auto-selection from overwriting this choice
                        categoryLocked = true;
                        showAlert('info', 'Đã tự động chọn danh mục: ' + (forcedCategory === 'mua sắm' ? 'Mua sắm' : 'Ăn uống'));
                        break;
                    }
                }
            }
        }
    } catch (e) {
        console.warn('Merchant-category mapping failed', e);
    }
}

// Auto-select category by predictedCategoryId or predictedCategoryName from OCR response
// Skip if a forced mapping already locked the category
if (!categoryLocked && (predictedCategoryId || predictedCategoryName)) {
 const select = document.getElementById('tx-category');
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
 const noteEl = document.getElementById('tx-description');
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

 // 🤖 TRIGGER AUTO-CATEGORIZE: Gọi AI để phân loại tự động dựa trên merchant + amount
 // Ưu tiên: merchant name (từ OCR) hoặc note (để AI phân tích)
 const autoCategorizeTrigger = merchant || noteEl?.value || rawText;
 if (autoCategorizeTrigger) {
 console.log('[OCR] Triggering auto-categorize with:', autoCategorizeTrigger.substring(0, 50));
 // Wait longer để form hoàn toàn render và categories được load
 setTimeout(() => {
 triggerAutoCategorize(autoCategorizeTrigger, suggestedAmount ? parseVnCurrencyToNumber(suggestedAmount) : null);
 }, 1000); // Tăng từ 200ms lên 1000ms
 }

 // Build enhanced message
 const parts = [];
 if (suggestedAmount) parts.push('Số tiền: ' + suggestedAmount + (amountConfidence ? ` (~${Math.round(amountConfidence*100)}%)` : ''));
 if (dateIso) parts.push('Ngày: ' + dateIso + (dateConfidence ? ` (~${Math.round(dateConfidence*100)}%)` : ''));
 if (merchant) parts.push('Đơn vị: ' + merchant);
 const previewMsg = (rawText && rawText.length) ? ('Nội dung: ' + rawText.slice(0, 80).replace(/\n/g, ' ') + '...') : 'Không trích xuất được nội dung từ ảnh.';
 parts.push(previewMsg);
 // Không hiển thị thông báo success nữa - người dùng thấy form đã được điền là đủ rồi
 console.log('OCR success:', parts.join(' | '));
 } catch (err) {
     console.error('OCR error:', err);
     // Close loading modal if still open
     closeOcrModal();
     showAlert('danger', 'Không thể quét hóa đơn: ' + err.message);
 } finally {
     // Clear processing flag so future scans can run
     window._ocrInProgress = false;
 }
}

// Helper function to close OCR modal and clean up backdrop
function closeOcrModal() {
 const loadingModalEl = document.getElementById('ocrLoadingModal');
 if (loadingModalEl) {
     const modal = bootstrap.Modal.getInstance(loadingModalEl);
     if (modal) {
         modal.hide();
     }
     // Ensure backdrop is removed after modal closes
     setTimeout(() => {
         document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
         document.body.classList.remove('modal-open');
     }, 300);
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
 // 1) Nếu có >1 dấu (.,) => coi tất cả là phân tách hàng nghìn, bỏ hết dấu.
 // 2) Nếu chỉ 1 dấu:
 //    - Nếu đó là dấu chấm (.) hoặc dấu phẩy (,) ở vị trí cuối với 1-2 chữ số sau => thập phân
 //    - Ngược lại => phân tách hàng nghìn, bỏ hết dấu
 // 3) Không dấu => parse integer trực tiếp
 
 if (separators === 1) {
 const idx = Math.max(raw.indexOf(','), raw.indexOf('.'));
 const fractionalLen = raw.length - idx - 1;
 const intDigits = raw.slice(0, idx).replace(/[^0-9]/g, '');
 const fracDigits = raw.slice(idx + 1).replace(/[^0-9]/g, '');
 
 // Coi là thập phân nếu:
 // - Phần sau dấu có 1-2 chữ số
 // - Phần trước dấu có >= 1 chữ số
 if (fractionalLen > 0 && fractionalLen <= 2 && intDigits.length > 0) {
 // Đây là decimal (ví dụ: 193.00, 1234.50, 99.99)
 // Giữ nguyên giá trị thực tế (không round), chỉ convert sang số
 const asFloat = parseFloat(intDigits + '.' + fracDigits);
 if (isNaN(asFloat)) return null;
 // Nếu phần thập phân chỉ toàn 0 (ví dụ 193.00), trả về số nguyên
 // Nếu có phần lẻ (ví dụ 193.50), trả về giá trị thực
 return asFloat;
 }
 }
 
 // Thousand grouping case hoặc không dấu: bỏ hết dấu và parse integer
 const asInt = parseInt(digitsOnly, 10);
 if (!isNaN(asInt)) return asInt;
 return null;
}

function editTransaction(id) {
 const transaction = transactions.find(tx => tx.id === id);
 if (!transaction) return;
 
 editingTransaction = transaction;
 document.getElementById('modalTitle').textContent = 'Sửa giao dịch';
 
 const form = document.getElementById('txForm');
 if (!form) {
 console.error('❌ Form not found with ID: txForm');
 alert('Lỗi: Không tìm thấy form');
 return;
 }
 
 const txIdField = form.querySelector('#tx-id');
 if (txIdField) txIdField.value = transaction.id;
 
 form.querySelector('input[name="date"]').value = transaction.date;
 form.querySelector('input[name="type"][value="' + transaction.type + '"]').checked = true;
 form.querySelector('select[name="category"]').value = transaction.categoryId;
 form.querySelector('input[name="amount"]').value = transaction.amount;
 form.querySelector('textarea[name="note"]').value = transaction.note || '';
 // If wallet loaded, try to set value
 const walletSelect = form.querySelector('select[name="wallet"]');
 if (walletSelect && transaction.walletId) {
 walletSelect.value = transaction.walletId;
 }
 
 const modal = new bootstrap.Modal(document.getElementById('txModal'));
 modal.show();
}

function clearOcrImagePreview() {
 const wrap = document.getElementById('ocr-image-preview-wrapper');
 const img = document.getElementById('ocr-image-preview');
 if (img) img.src = '';
 if (wrap) wrap.classList.add('d-none');
}

// Zoom OCR Image
function zoomOcrImage() {
 const img = document.getElementById('ocr-image-preview');
 if (!img || !img.src) return;
 
 // Create zoom modal if not exists
 let zoomModal = document.getElementById('ocrZoomModal');
 if (!zoomModal) {
 zoomModal = document.createElement('div');
 zoomModal.id = 'ocrZoomModal';
 zoomModal.className = 'modal fade';
 zoomModal.innerHTML = `
 <div class="modal-dialog modal-fullscreen">
 <div class="modal-content bg-dark">
 <div class="modal-header bg-dark border-secondary">
 <h5 class="modal-title text-white">Ảnh Quét Hóa Đơn</h5>
 <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
 </div>
 <div class="modal-body d-flex align-items-center justify-content-center bg-dark" style="min-height: 80vh; overflow: auto;">
 <img id="zoomImageDisplay" src="" alt="Zoom Image" style="max-width: 100%; height: auto; cursor: zoom-out;" onwheel="handleZoomWheel(event)">
 </div>
 <div class="modal-footer bg-dark border-secondary">
 <button type="button" class="btn btn-light btn-sm" onclick="resetZoom()">
 <i class="bi bi-arrow-counterclockwise"></i>
 Reset
 </button>
 <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>
 </div>
 </div>
 </div>
 `;
 document.body.appendChild(zoomModal);
 }
 
 // Set image src and show modal
 const zoomImg = document.getElementById('zoomImageDisplay');
 if (zoomImg) {
 zoomImg.src = img.src;
 zoomImg.style.transform = 'scale(1)';
 zoomImg.style.transition = 'transform 0.3s ease';
 }
 
 const modal = new bootstrap.Modal(zoomModal);
 modal.show();
}

// Handle zoom wheel on fullscreen image
function handleZoomWheel(event) {
 event.preventDefault();
 const img = event.target;
 const scale = img.style.transform.match(/scale\(([\d.]+)\)/)?.[1] || 1;
 const newScale = event.deltaY > 0 ? Math.max(0.5, scale - 0.2) : Math.min(3, scale + 0.2);
 img.style.transform = `scale(${newScale})`;
}

// Reset zoom
function resetZoom() {
 const img = document.getElementById('zoomImageDisplay');
 if (img) img.style.transform = 'scale(1)';
}

function openFullOcrImage() {
 const img = document.getElementById('ocr-image-preview');
 if (img && img.src) {
 window.open(img.src, '_blank');
 }
}


function saveTransaction() {
 try {
 console.log('💾 saveTransaction called');
 
 const form = document.getElementById('txForm');
 if (!form) {
 console.error('❌ Form not found with ID: txForm');
 alert('Không tìm thấy form');
 return;
 }
 console.log('✅ Form found:', form);
 
 // Get form values directly from inputs
 const typeRadio = form.querySelector('input[name="type"]:checked');
 const amountInput = document.getElementById('tx-amount');
 const dateInput = document.getElementById('tx-date');
 const categorySelect = document.getElementById('tx-category');
 const walletSelect = document.getElementById('tx-wallet');
 const descriptionInput = document.getElementById('tx-description');
 
 console.log('📋 Form elements:', {
 typeRadio: typeRadio?.value,
 amountInput: amountInput?.value,
 dateInput: dateInput?.value,
 categorySelect: categorySelect?.value,
 walletSelect: walletSelect?.value,
 descriptionInput: descriptionInput?.value
 });
 
 // Enhanced validation
 if (!dateInput || !dateInput.value) {
 showAlert('danger', 'Vui lòng chọn ngày giao dịch');
 return;
 }
 
 if (!typeRadio) {
 showAlert('danger', 'Vui lòng chọn loại giao dịch');
 return;
 }
 
 const amount = parseVnCurrencyToNumber(amountInput.value);
 if (!amount || amount <= 0) {
 showAlert('danger', 'Vui lòng nhập số tiền hợp lệ (lớn hơn 0)');
 return;
 }
 
 // Parse amount raw string để giữ format thập phân (ví dụ: 193.00)
 const amountRawStr = String(amountInput.value).trim().replace(/vnd|vnđ|đ/gi, '').trim();
 // Validate nó có phải số hợp lệ không
 let amountValue;
 if (amountRawStr.includes('.') || amountRawStr.includes(',')) {
 // Có dấu thập phân - giữ nguyên format string (sẽ gửi "193.00")
 amountValue = amountRawStr;
 } else {
 // Không dấu - dùng số được parse
 amountValue = amount;
 }
 
 // Get category - first try from select dropdown
 const categoryOption = categorySelect.options[categorySelect.selectedIndex];
 let categoryId = categoryOption ? categoryOption.dataset.categoryId : null;
 
 // Fallback: try to get from hidden input (AI suggested)
 if (!categoryId) {
 const suggestedCategoryId = document.getElementById('suggestedCategoryId');
 categoryId = suggestedCategoryId ? suggestedCategoryId.value : null;
 }
 
 console.log('🔍 Debug - categoryId:', categoryId, 'from select:', categorySelect.value);
 
 if (!categoryId || !categorySelect.value) {
 showAlert('danger', 'Vui lòng chọn danh mục');
 return;
 }
 
 // Wallet selection
 const walletOption = walletSelect.options[walletSelect.selectedIndex];
 const walletId = walletOption ? (walletOption.dataset.walletId || walletOption.value) : null;
 
 if (!walletId) {
 showAlert('danger', 'Vui lòng chọn ví');
 return;
 }
 
 const transactionData = {
 date: dateInput.value,
 type: typeRadio.value === 'income' ? 'income' : 'expense',
 categoryId: parseInt(categoryId),
 amount: typeof amountValue === 'string' ? parseFloat(amountValue) : amountValue,
 note: descriptionInput.value || '',
 walletId: parseInt(walletId)
 };
 
 console.log(" Sending transaction data:", transactionData);
 
 const url = editingTransaction 
 ? `http://localhost:8080/api/transactions/${editingTransaction.id}`
 : `http://localhost:8080/api/transactions`;
 
 const method = editingTransaction ? 'PUT' : 'POST';
 
 console.log(` ${method} transaction to:`, url);
 
 const token = localStorage.getItem('accessToken');
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
 console.log(" Save response status:", res.status);
 if (!res.ok) {
 return res.text().then(text => { 
 console.error(" Save error:", text);
 throw new Error(`HTTP ${res.status}: ${text}`); 
 });
 }
 return res.json();
 })
 .then(data => {
 console.log(" Transaction saved:", data);
 
 // TRIGGER INTEGRATION SYSTEM - Kích hoạt hệ thống tích hợp
 if (window.FinancialIntegration) {
 FinancialIntegration.processTransaction(transactionData, !editingTransaction);
 }
 
 // Dispatch custom event for other modules
 const event = new CustomEvent(editingTransaction ? 'transactionUpdated' : 'transactionSaved', {
 detail: { transaction: { ...transactionData, id: data.id || editingTransaction?.id } }
 });
 window.dispatchEvent(event);
 
 showAlert('success', editingTransaction ? 'Cập nhật giao dịch thành công!' : 'Thêm giao dịch thành công!');
 
 // Close modal FIRST - to ensure UI is responsive
 const modalElement = document.getElementById('txModal');
 if (modalElement) {
 try {
 // Blur any focused element to prevent aria-hidden issues
 document.activeElement?.blur();
 
 const modal = bootstrap.Modal.getInstance(modalElement);
 if (modal) {
 modal.hide();
 } else {
 new bootstrap.Modal(modalElement).hide();
 }
 
 // Force remove backdrop and modal classes after a short delay
 setTimeout(() => {
 const backdrop = document.querySelector('.modal-backdrop');
 if (backdrop) {
 backdrop.remove();
 }
 
 // Remove modal-open class from body
 document.body.classList.remove('modal-open');
 
 // Reset modal state
 modalElement.style.display = 'none';
 modalElement.setAttribute('aria-hidden', 'true');
 modalElement.removeAttribute('aria-modal');
 modalElement.removeAttribute('role');
 }, 100);
 } catch (e) {
 console.warn('Could not close modal:', e);
 }
 }
 
 // Reset form and remove focus
 form.reset();
 editingTransaction = null;
 document.activeElement?.blur();
 
 // Clear AI categorization hidden inputs
 const suggestedCategoryId = document.getElementById('suggestedCategoryId');
 const suggestedCategoryName = document.getElementById('suggestedCategoryName');
 const categoryHint = document.getElementById('categoryHint');
 const categoryConfidence = document.getElementById('categoryConfidence');
 if (suggestedCategoryId) suggestedCategoryId.value = '';
 if (suggestedCategoryName) suggestedCategoryName.value = '';
 if (categoryHint) categoryHint.style.display = 'none';
 if (categoryConfidence) categoryConfidence.style.display = 'none';
 
 // Reload transactions with error handling - wait 300ms for modal to close
 setTimeout(async () => {
 try {
 console.log('⏳ Starting transaction reload...');
 await loadTransactions().catch(err => {
 console.error('Error reloading transactions:', err);
 showAlert('warning', 'Giao dịch đã lưu nhưng lỗi tải lại danh sách. Vui lòng tải lại trang.');
 });
 console.log('✅ Transaction reload complete');

// Ensure UI is updated after reload (apply filters / re-render table)
if (typeof applyFilters === 'function') {
    try {
        applyFilters();
        // Dispatch an event so other components can react
        window.dispatchEvent(new Event('transactionsReloaded'));
    } catch (e) {
        console.warn('Could not applyFilters after reload:', e);
    }
}
 
 // Also reload budgets if on budgets page or globally
 if (window.reloadBudgets && typeof window.reloadBudgets === 'function') {
 console.log('🔄 Reloading budgets...');
 window.reloadBudgets();
 } else {
 // Fallback: Reload budgets via API if function not available
 const token = localStorage.getItem('accessToken');
 fetch('http://localhost:8080/api/budgets', {
 headers: {
 'Authorization': 'Bearer ' + token,
 'Content-Type': 'application/json'
 }
 }).catch(err => console.warn('Could not reload budgets:', err));
 }
 } catch (err) {
 console.error('Error in reload process:', err);
 showAlert('warning', 'Giao dịch đã lưu nhưng lỗi tải lại danh sách. Vui lòng tải lại trang.');
 }
 }, 300);
 })
 .catch(err => {
 console.error(" Failed to save transaction:", err);
 alert('Lỗi khi lưu giao dịch: ' + err.message);
 });
 
 } catch (error) {
 console.error('💥 Exception in saveTransaction:', error);
 alert('Lỗi: ' + error.message);
 }
}

// Make sure saveTransaction is globally accessible
window.saveTransaction = saveTransaction;

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
 console.log(" Deleting transaction:", url);
 
 const token = localStorage.getItem('accessToken');
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
 console.log(" Delete response status:", res.status);
 if (!res.ok) {
 return res.text().then(text => { 
 console.error(" Delete error:", text);
 throw new Error(`HTTP ${res.status}: ${text}`); 
 });
 }
 return res.status === 204 ? null : res.json(); // Handle no content response
 })
 .then(data => {
 console.log(" Transaction deleted");
 showAlert('success', 'Xóa giao dịch thành công!');
 
 // Reload transactions
 loadTransactions();
 })
 .catch(err => {
 console.error(" Failed to delete transaction:", err);
 showAlert('danger', 'Lỗi khi xóa giao dịch: ' + err.message);
 });
 }
}

function formatCurrency(amount) {
 // Hiển thị số tiền với định dạng Việt Nam và đơn vị VND, luôn hiển thị 2 chữ số thập phân
 const formatted = new Intl.NumberFormat('vi-VN', {
 minimumFractionDigits: 2,
 maximumFractionDigits: 2
 }).format(amount);
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

// INTEGRATION FUNCTIONS - Các hàm liên kết giữa các chức năng

/**
 * Cập nhật việc sử dụng ngân sách khi có giao dịch mới
 */
function updateBudgetUsage(categoryId, transactionType, amount) {
 if (transactionType !== 'CHI' || !categoryId) return;
 
 const currentDate = new Date();
 const month = currentDate.getMonth() + 1;
 const year = currentDate.getFullYear();
 
 // Gọi API để cập nhật budget usage
 const token = localStorage.getItem('accessToken');
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
 console.log(" Budget usage updated:", data);
 })
 .catch(err => {
 console.error(" Failed to update budget usage:", err);
 });
}

/**
 * Cập nhật tiến độ mục tiêu khi có giao dịch tiết kiệm
 */
function updateGoalProgress(transactionType, amount) {
 if (transactionType !== 'THU') return;
 
 const token = localStorage.getItem('accessToken');
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
 console.log(" Goal progress updated:", data);
 })
 .catch(err => {
 console.error(" Failed to update goal progress:", err);
 });
}

/**
 * Cập nhật số dư ví
 */
function updateWalletBalance(transactionType, amount) {
 const balanceChange = transactionType === 'THU' ? amount : -amount;
 
 const token = localStorage.getItem('accessToken');
 const headers = {
 'Content-Type': 'application/json'
 };
 if (token) {
 headers['Authorization'] = 'Bearer ' + token;
 }
 
 // Backend đã tự cập nhật số dư ví khi lưu giao dịch, không cần gọi API riêng
 console.log('ℹ Skip client-side wallet update; handled by backend. Change:', balanceChange);
}

/**
 * Tạo thông báo về tác động của giao dịch
 */
function getTransactionImpactMessage(transaction) {
 let message = '';
 
 if (transaction.type === 'CHI') {
 message = ' Ngân sách đã được cập nhật.';
 } else if (transaction.type === 'THU') {
 message = ' Mục tiêu tiết kiệm đã được cập nhật.';
 }
 
 return message;
}

/**
 * Kiểm tra và cảnh báo vượt ngân sách
 */
function checkBudgetAlert(categoryId, amount) {
 const token = localStorage.getItem('accessToken');
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
 showAlert('warning', ` Cảnh báo: Bạn đã vượt ngân sách ${data.categoryName} ${data.percentage}%!`);
 } else if (data.nearLimit) {
 showAlert('info', ` Thông tin: Bạn đã sử dụng ${data.percentage}% ngân sách ${data.categoryName}.`);
 }
 })
 .catch(err => {
 console.error(" Failed to check budget:", err);
 });
}

/**
 * Auto-Categorize Feature (AI-powered, direct API call)
 */
function addAIFeatures() {
 const noteInput = document.querySelector('input[name="note"], textarea[name="note"]');
 const amountInput = document.querySelector('input[name="amount"]');
 
 if (noteInput && amountInput) {
 let autoCategorizeTimeout;
 
 noteInput.addEventListener('input', function() {
 clearTimeout(autoCategorizeTimeout);
 const description = this.value.trim();
 const amount = parseFloat(amountInput.value);
 
 if (description.length > 3 && amount > 0) {
 autoCategorizeTimeout = setTimeout(() => {
 triggerAutoCategorize(description, amount);
 }, 1500);
 }
 });
 
 amountInput.addEventListener('blur', function() {
 const description = noteInput.value.trim();
 const amount = parseFloat(this.value);
 
 if (description.length > 3 && amount > 0) {
 setTimeout(() => triggerAutoCategorize(description, amount), 500);
 }
 });
 }
}

async function triggerAutoCategorize(description, amount) {
 const categoryNameField = document.getElementById('suggestedCategoryName');
 const categoryIdField = document.getElementById('suggestedCategoryId');
 const categoryHint = document.getElementById('categoryHint');
 const categoryConfidence = document.getElementById('categoryConfidence');
 
 if (!categoryNameField) return;
 
 categoryNameField.value = 'Đang phân tích...';
 categoryNameField.style.backgroundColor = '#fff3cd';
 
 // Wait for categories to be loaded
 let attempts = 0;
 while ((!categories || categories.length === 0) && attempts < 50) { // Wait up to 5 seconds
 await new Promise(resolve => setTimeout(resolve, 100));
 attempts++;
 }
 
 if (!categories || categories.length === 0) {
 console.error('Categories not loaded after waiting');
 categoryNameField.value = 'Lỗi: Danh mục chưa tải';
 categoryNameField.style.backgroundColor = '#f8d7da';
 return;
 }
 
 console.log('Categories loaded:', categories.length, 'items');
 
 try {
 const token = localStorage.getItem('accessToken');
 const response = await fetch('http://localhost:8080/api/ai/auto-categorize', {
 method: 'POST',
 headers: {
 'Content-Type': 'application/json',
 'Authorization': token ? `Bearer ${token}` : ''
 },
 body: JSON.stringify({
 description: description,
 amount: amount
 })
 });
 
 if (!response.ok) throw new Error('AI service error');
 
 const result = await response.json();
 
 console.log(' AI Response:', result);
 
 if (result.success && result.suggestions && result.suggestions.length > 0) {
 const topSuggestion = result.suggestions[0];
 
 console.log(' Top Suggestion:', topSuggestion);
 
 // Try different field name variations
 const categoryName = topSuggestion.categoryName || topSuggestion.name || 'Không xác định';
 const categoryIdFromAI = topSuggestion.categoryId || topSuggestion.id || '';
 
 // Map category name to actual numeric ID from loaded categories
 let categoryId = categoryIdFromAI;
 if (typeof categoryIdFromAI === 'string') {
 // AI returned string ID like "food", need to find numeric ID
 const matchedCategory = categories.find(cat => 
 cat.name === categoryName || 
 cat.name.toLowerCase() === categoryName.toLowerCase()
 );
 if (matchedCategory) {
 categoryId = matchedCategory.id;
 console.log('� Mapped', categoryName, 'to ID:', categoryId);
 } else {
 console.warn(' Could not find category ID for:', categoryName);
 }
 }
 
 categoryNameField.value = categoryName;
 categoryIdField.value = categoryId;
 categoryNameField.style.backgroundColor = '#d1e7dd';
 
 if (categoryConfidence) categoryConfidence.style.display = 'flex';
 
 if (categoryHint) {
 const confidence = topSuggestion.confidence || 0;
 const confidencePercent = (confidence * 100).toFixed(0);
 categoryHint.innerHTML = `<i class="fas fa-robot text-primary me-1"></i>AI gợi ý: <strong>${categoryName}</strong> (${confidencePercent}% chắc chắn)`;
 categoryHint.classList.remove('text-muted');
 categoryHint.classList.add('text-primary');
 }
 
 // 🎯 AUTO-SELECT DANH MỤC VÀO DROPDOWN tx-category
 const categorySelect = document.getElementById('tx-category');
 if (categorySelect && categoryName) {
 try {
 // Wait a bit more for dropdown to be populated
 setTimeout(() => {
 categorySelect.value = categoryName;
 console.log('✅ Tự động chọn danh mục:', categoryName, '(ID:', categoryId, ') trong dropdown');
 
 // Trigger change event để update UI
 const event = new Event('change', { bubbles: true });
 categorySelect.dispatchEvent(event);
 
 showAlert('success', `✨ AI gợi ý danh mục: ${categoryName} (${(topSuggestion.confidence * 100).toFixed(0)}% tin cậy)`);
 }, 1500); // Increased from 500ms to 1500ms
 } catch (e) {
 console.error('Lỗi khi set danh mục:', e);
 }
 } else {
 console.warn('Category select not found or no categoryName:', categorySelect, categoryName);
 }
 
 console.log(' Auto-categorized:', categoryName, '(ID:', categoryId, ')');
 } else {
 throw new Error('No suggestions');
 }
 } catch (error) {
 console.error('Auto-categorize error:', error);
 categoryNameField.value = 'Không thể phân loại';
 categoryNameField.style.backgroundColor = '#f8d7da';
 if (categoryHint) {
 categoryHint.innerHTML = '<i class="fas fa-exclamation-circle text-danger me-1"></i>Vui lòng thử lại';
 categoryHint.classList.remove('text-success');
 categoryHint.classList.add('text-danger');
 }
 }
}

// Initialize AI features when DOM loads
addAIFeatures();