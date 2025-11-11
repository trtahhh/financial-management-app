// Simple budget.js - Backend-heavy approach
let budgets = [];
let editingBudget = null;

document.addEventListener('DOMContentLoaded', function() {
    loadBudgets();
    loadCategories();
    
    // Add AI budget features
    addSmartBudgetFeatures();
});

function addSmartBudgetFeatures() {
    // Add smart budget generation button
    const budgetContainer = document.querySelector('.budget-container') || document.body;
    
    if (!document.getElementById('smart-budget-section')) {
        const smartSection = document.createElement('div');
        smartSection.id = 'smart-budget-section';
        smartSection.className = 'card mb-4';
        smartSection.innerHTML = `
            <div class="card-header">
                <h5>🤖 Ngân sách thông minh</h5>
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
                                🎯 Tạo ngân sách AI
                            </button>
                        </div>
                    </div>
                </div>
                <div id="smart-budget-result" class="mt-3"></div>
            </div>
        `;
        
        budgetContainer.insertBefore(smartSection, budgetContainer.firstChild);
    }
    
    // Add budget analysis section
    if (!document.getElementById('budget-analysis-section')) {
        const analysisSection = document.createElement('div');
        analysisSection.id = 'budget-analysis-section';
        analysisSection.className = 'card mb-4';
        analysisSection.innerHTML = `
            <div class="card-header d-flex justify-content-between">
                <h5>📊 Phân tích ngân sách</h5>
                <button class="btn btn-sm btn-outline-primary" onclick="analyzeBudgetPerformance()">
                    🔍 Phân tích hiện tại
                </button>
            </div>
            <div class="card-body">
                <div id="budget-analysis-result"></div>
            </div>
        `;
        
        budgetContainer.appendChild(analysisSection);
    }
}

async function generateSmartBudget() {
    const monthlyIncome = document.getElementById('monthly-income').value;
    const budgetType = document.getElementById('budget-type').value;
    
    if (!monthlyIncome || parseFloat(monthlyIncome) <= 0) {
        alert('Vui lòng nhập thu nhập hàng tháng hợp lệ');
        return;
    }

    try {
        showLoading('smart-budget-result');
        
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
            <h6>🎯 Ngân sách được đề xuất (Điểm sức khỏe tài chính: ${data.healthScore}/100)</h6>
        </div>
        <div class="row">
    `;
    
    // Display allocations
    Object.entries(data.allocations).forEach(([category, allocation]) => {
        html += `
            <div class="col-md-6 col-lg-4 mb-3">
                <div class="card">
                    <div class="card-body">
                        <h6 class="card-title">${allocation.category}</h6>
                        <p class="card-text">
                            <strong>${formatCurrency(allocation.amount)}</strong><br>
                            <small class="text-muted">${Math.round(allocation.ratio * 100)}% thu nhập</small><br>
                            <small class="text-info">Độ tin cậy: ${Math.round(allocation.confidence * 100)}%</small>
                        </p>
                        <button class="btn btn-sm btn-outline-success" 
                                onclick="createBudgetFromAI('${category}', ${allocation.amount})">
                            ➕ Tạo ngân sách
                        </button>
                    </div>
                </div>
            </div>
        `;
    });
    
    html += '</div>';
    
    // Display insights
    if (data.insights && data.insights.length > 0) {
        html += '<div class="mt-3"><h6>💡 Thông tin chi tiết:</h6>';
        data.insights.forEach(insight => {
            html += `
                <div class="alert alert-info">
                    ${insight.icon} <strong>${insight.title}:</strong> ${insight.message}
                </div>
            `;
        });
        html += '</div>';
    }
    
    // Display recommendations
    if (data.recommendations && data.recommendations.length > 0) {
        html += '<div class="mt-3"><h6>🎯 Khuyến nghị:</h6>';
        data.recommendations.forEach(rec => {
            html += `
                <div class="alert alert-warning">
                    <strong>${rec.title}:</strong> ${rec.message}
                </div>
            `;
        });
        html += '</div>';
    }
    
    resultDiv.innerHTML = html;
}

async function analyzeBudgetPerformance() {
    try {
        showLoading('budget-analysis-result');
        
        const response = await fetch('/api/ai/budget/performance?period=month', {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('authToken')
            }
        });

        const data = await response.json();
        
        if (data.success) {
            displayBudgetAnalysis(data);
        } else {
            alert('Lỗi phân tích ngân sách: ' + data.error);
        }
    } catch (error) {
        console.error('Budget analysis error:', error);
        alert('Lỗi kết nối AI service');
    }
}

function displayBudgetAnalysis(data) {
    const resultDiv = document.getElementById('budget-analysis-result');
    
    let html = '';
    
    // Achievements
    if (data.achievements && data.achievements.length > 0) {
        html += '<div class="alert alert-success"><h6>🏆 Thành tích:</h6>';
        data.achievements.forEach(achievement => {
            html += `<li>${achievement}</li>`;
        });
        html += '</div>';
    }
    
    // Warnings
    if (data.warnings && data.warnings.length > 0) {
        html += '<div class="alert alert-warning"><h6>⚠️ Cảnh báo:</h6>';
        data.warnings.forEach(warning => {
            html += `<li>${warning}</li>`;
        });
        html += '</div>';
    }
    
    // Performance metrics
    if (data.metrics && Object.keys(data.metrics).length > 0) {
        html += '<h6>📊 Chi tiết theo danh mục:</h6><div class="row">';
        
        Object.entries(data.metrics).forEach(([category, metric]) => {
            const statusColor = metric.status === 'good' ? 'success' : 
                              metric.status === 'warning' ? 'warning' : 'danger';
            
            html += `
                <div class="col-md-6 mb-3">
                    <div class="card border-${statusColor}">
                        <div class="card-body">
                            <h6 class="card-title">${metric.category}</h6>
                            <p class="card-text">
                                Ngân sách: ${formatCurrency(metric.budgeted)}<br>
                                Đã chi: ${formatCurrency(metric.spent)}<br>
                                Còn lại: <span class="text-${statusColor}">${formatCurrency(metric.remaining)}</span><br>
                                Sử dụng: ${Math.round(metric.utilizationRate * 100)}%
                            </p>
                        </div>
                    </div>
                </div>
            `;
        });
        
        html += '</div>';
    }
    
    // Suggestions
    if (data.suggestions && data.suggestions.length > 0) {
        html += '<div class="alert alert-info"><h6>💡 Gợi ý cải thiện:</h6>';
        data.suggestions.forEach(suggestion => {
            html += `<li><strong>${suggestion.title}:</strong> ${suggestion.message}</li>`;
        });
        html += '</div>';
    }
    
    resultDiv.innerHTML = html || '<p class="text-muted">Không có dữ liệu phân tích.</p>';
}

function createBudgetFromAI(category, amount) {
    // Populate the budget form with AI suggestion
    const form = document.getElementById('budget-form');
    if (form) {
        // Find category in dropdown
        const categorySelect = form.querySelector('select[name="category_id"]');
        if (categorySelect) {
            const options = Array.from(categorySelect.options);
            const matchingOption = options.find(option => 
                option.text.toLowerCase().includes(category.toLowerCase())
            );
            
            if (matchingOption) {
                categorySelect.value = matchingOption.value;
            }
        }
        
        // Set amount
        const amountInput = form.querySelector('input[name="amount"]');
        if (amountInput) {
            amountInput.value = Math.round(amount);
        }
        
        // Set period to current month
        const periodInput = form.querySelector('input[name="period"]');
        if (periodInput) {
            const currentMonth = new Date().toISOString().slice(0, 7);
            periodInput.value = currentMonth;
        }
        
        // Scroll to form
        form.scrollIntoView({ behavior: 'smooth' });
    }
}

// Original budget functions (simplified)
function loadBudgets() {
    const token = localStorage.getItem('authToken');
    const headers = {
        'Content-Type': 'application/json'
    };
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    
    fetch('http://localhost:8080/api/budgets', {
        headers: headers,
        mode: 'cors'
    })
    .then(res => res.json())
    .then(data => {
        budgets = data;
        updateBudgetTable();
    })
    .catch(error => {
        console.error('Error loading budgets:', error);
        showError('Không thể tải danh sách ngân sách');
    });
}

function loadCategories() {
    const token = localStorage.getItem('authToken');
    const headers = {
        'Content-Type': 'application/json'
    };
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    
    fetch('http://localhost:8080/api/categories', {
        headers: headers,
        mode: 'cors'
    })
    .then(res => res.json())
    .then(categories => {
        updateCategoryDropdown(categories);
    })
    .catch(error => {
        console.error('Error loading categories:', error);
    });
}

function updateCategoryDropdown(categories) {
    const categorySelect = document.querySelector('select[name="category_id"]');
    
    if (categorySelect) {
        categorySelect.innerHTML = '<option value="">Chọn danh mục</option>';
        categories.forEach(category => {
            categorySelect.innerHTML += `<option value="${category.id}">${category.name}</option>`;
        });
    }
}

function updateBudgetTable() {
    const tbody = document.querySelector('#budget-table tbody');
    if (!tbody) return;
    
    tbody.innerHTML = '';
    
    budgets.forEach(budget => {
        const row = document.createElement('tr');
        
        row.innerHTML = `
            <td>${budget.category ? budget.category.name : 'Không xác định'}</td>
            <td>${formatCurrency(budget.amount)}</td>
            <td>${budget.period}</td>
            <td>
                <button class="btn btn-sm btn-outline-primary" onclick="editBudget(${budget.id})">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger" onclick="deleteBudget(${budget.id})">
                    <i class="fas fa-trash"></i>
                </button>
            </td>
        `;
        
        tbody.appendChild(row);
    });
}

function saveBudget() {
    const form = document.getElementById('budget-form');
    if (!form) return;
    
    const formData = new FormData(form);
    
    const budgetData = {
        categoryId: parseInt(formData.get('category_id')),
        amount: parseFloat(formData.get('amount')),
        period: formData.get('period')
    };
    
    if (!budgetData.categoryId || !budgetData.amount || !budgetData.period) {
        alert('Vui lòng điền đầy đủ thông tin');
        return;
    }
    
    const token = localStorage.getItem('authToken');
    const headers = {
        'Content-Type': 'application/json'
    };
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    
    const url = editingBudget 
        ? `http://localhost:8080/api/budgets/${editingBudget}` 
        : 'http://localhost:8080/api/budgets';
    
    const method = editingBudget ? 'PUT' : 'POST';
    
    fetch(url, {
        method: method,
        headers: headers,
        body: JSON.stringify(budgetData),
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
        console.log('Budget saved:', data);
        loadBudgets();
        clearBudgetForm();
        showSuccess(editingBudget ? 'Cập nhật ngân sách thành công!' : 'Thêm ngân sách thành công!');
    })
    .catch(error => {
        console.error('Error saving budget:', error);
        showError('Lỗi lưu ngân sách: ' + error.message);
    });
}

function editBudget(id) {
    const budget = budgets.find(b => b.id === id);
    if (!budget) return;
    
    editingBudget = id;
    
    const form = document.getElementById('budget-form');
    form.querySelector('select[name="category_id"]').value = budget.category ? budget.category.id : '';
    form.querySelector('input[name="amount"]').value = budget.amount;
    form.querySelector('input[name="period"]').value = budget.period;
    
    const submitBtn = form.querySelector('button[type="button"]');
    if (submitBtn) submitBtn.textContent = 'Cập nhật';
}

function deleteBudget(id) {
    if (!confirm('Bạn có chắc muốn xóa ngân sách này?')) return;
    
    const token = localStorage.getItem('authToken');
    const headers = {};
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    
    fetch(`http://localhost:8080/api/budgets/${id}`, {
        method: 'DELETE',
        headers: headers,
        mode: 'cors'
    })
    .then(res => {
        if (!res.ok) {
            throw new Error(`HTTP ${res.status}`);
        }
        loadBudgets();
        showSuccess('Xóa ngân sách thành công!');
    })
    .catch(error => {
        console.error('Error deleting budget:', error);
        showError('Lỗi xóa ngân sách: ' + error.message);
    });
}

function clearBudgetForm() {
    const form = document.getElementById('budget-form');
    if (form) {
        form.reset();
        editingBudget = null;
        
        const submitBtn = form.querySelector('button[type="button"]');
        if (submitBtn) submitBtn.textContent = 'Thêm ngân sách';
    }
}

// Utility functions
function showLoading(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.innerHTML = '<div class="text-center"><div class="spinner-border" role="status"></div> Đang xử lý...</div>';
    }
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

function showError(message) {
    alert('Lỗi: ' + message);
}

function showSuccess(message) {
    alert(message);
}