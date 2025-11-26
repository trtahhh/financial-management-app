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
    const token = localStorage.getItem('accessToken');
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
    initializeUltraAI();
  });
});

// ============= ULTRA AI BUDGET FEATURES =============

function initializeUltraAI() {
  // Show Ultra AI section
  const ultraSection = document.getElementById('ultra-ai-section');
  if (ultraSection) {
    ultraSection.style.display = 'block';
  }
  
  // Populate month/year selectors
  const monthSelect = document.getElementById('ultra-month');
  const yearSelect = document.getElementById('ultra-year');
  
  if (monthSelect) {
    const currentMonth = new Date().getMonth() + 1;
    monthSelect.innerHTML = '';
    for (let i = 1; i <= 12; i++) {
      const option = document.createElement('option');
      option.value = i;
      option.textContent = `Tháng ${i}`;
      if (i === currentMonth) option.selected = true;
      monthSelect.appendChild(option);
    }
  }
  
  if (yearSelect) {
    const currentYear = new Date().getFullYear();
    yearSelect.innerHTML = '';
    for (let i = currentYear - 1; i <= currentYear + 1; i++) {
      const option = document.createElement('option');
      option.value = i;
      option.textContent = `Năm ${i}`;
      if (i === currentYear) option.selected = true;
      yearSelect.appendChild(option);
    }
  }
  
  // Populate forecast category selector
  loadCategoriesForForecast();
}

function loadCategoriesForForecast() {
  fetch('http://localhost:8080/api/categories', {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + localStorage.getItem('accessToken')
    }
  })
  .then(r => r.json())
  .then(categories => {
    const select = document.getElementById('forecast-category');
    if (select) {
      select.innerHTML = '<option value="">Select category to forecast</option>';
      categories.forEach(cat => {
        if (cat.type === 'EXPENSE') {
          select.innerHTML += `<option value="${cat.name}">${cat.name}</option>`;
        }
      });
    }
  })
  .catch(e => console.error('Error loading categories:', e));
}

async function getUltraInsights() {
  const month = document.getElementById('ultra-month').value;
  const year = document.getElementById('ultra-year').value;
  const resultDiv = document.getElementById('ultra-insights-result');
  
  try {
    resultDiv.innerHTML = `
      <div class="text-center py-4">
        <div class="spinner-border text-primary" role="status"></div>
        <p class="mt-2">🤖 AI đang phân tích với 9 thư viện ML...</p>
      </div>
    `;
    
    const response = await fetch(`http://localhost:8080/api/budgets/ultra-insights?month=${month}&year=${year}`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + localStorage.getItem('accessToken')
      }
    });
    
    const data = await response.json();
    
    if (data.success) {
      displayUltraInsights(data.data);
    } else {
      resultDiv.innerHTML = `<div class="alert alert-danger">❌ ${data.message}</div>`;
    }
  } catch (error) {
    console.error('Ultra insights error:', error);
    resultDiv.innerHTML = `<div class="alert alert-danger">❌ Lỗi kết nối: ${error.message}</div>`;
  }
}

function displayUltraInsights(data) {
  const resultDiv = document.getElementById('ultra-insights-result');
  
  let html = '<div class="row">';
  
  // Ensemble Predictions Card
  if (data.ensemble_predictions) {
    const ensemble = data.ensemble_predictions;
    html += `
      <div class="col-md-4 mb-3">
        <div class="card border-primary h-100">
          <div class="card-header bg-primary text-white">
            <h6 class="mb-0">🎯 Ensemble Predictions</h6>
            <small>XGBoost + LightGBM</small>
          </div>
          <div class="card-body">
            <div class="text-center mb-3">
              <h3 class="text-primary">${(ensemble.ensemble || 0).toLocaleString('vi-VN')} ₫</h3>
              <p class="text-muted mb-0">Dự đoán chi tiêu tháng tới</p>
            </div>
            <hr>
            <div class="d-flex justify-content-between mb-2">
              <span>XGBoost:</span>
              <strong>${(ensemble.xgboost || 0).toLocaleString('vi-VN')} ₫</strong>
            </div>
            <div class="d-flex justify-content-between">
              <span>LightGBM:</span>
              <strong>${(ensemble.lightgbm || 0).toLocaleString('vi-VN')} ₫</strong>
            </div>
          </div>
        </div>
      </div>
    `;
  }
  
  // Prophet Forecasts Card
  if (data.prophet_forecasts) {
    html += `
      <div class="col-md-4 mb-3">
        <div class="card border-info h-100">
          <div class="card-header bg-info text-white">
            <h6 class="mb-0">📈 Prophet Forecasts</h6>
            <small>Time Series Analysis</small>
          </div>
          <div class="card-body">
            ${Object.entries(data.prophet_forecasts).slice(0, 3).map(([category, forecast]) => `
              <div class="mb-3">
                <div class="d-flex justify-content-between align-items-center">
                  <strong>${category}</strong>
                  <span class="badge ${forecast.trend === 'increasing' ? 'bg-danger' : forecast.trend === 'decreasing' ? 'bg-success' : 'bg-secondary'}">
                    ${forecast.trend === 'increasing' ? '📈' : forecast.trend === 'decreasing' ? '📉' : '➡️'} ${forecast.trend}
                  </span>
                </div>
                <div class="text-muted">${(forecast.forecast || 0).toLocaleString('vi-VN')} ₫</div>
                <small class="text-info">Confidence: ${Math.round((forecast.confidence || 0) * 100)}%</small>
              </div>
            `).join('')}
          </div>
        </div>
      </div>
    `;
  }
  
  // Sentiment Analysis Card
  if (data.sentiment_analysis) {
    const sentiment = data.sentiment_analysis;
    const avgScore = sentiment.average_sentiment || 0;
    let moodEmoji = '😐';
    let moodColor = 'secondary';
    if (avgScore > 0.5) { moodEmoji = '😊'; moodColor = 'success'; }
    else if (avgScore > 0.2) { moodEmoji = '🙂'; moodColor = 'info'; }
    else if (avgScore < -0.5) { moodEmoji = '😢'; moodColor = 'danger'; }
    else if (avgScore < -0.2) { moodEmoji = '😕'; moodColor = 'warning'; }
    
    html += `
      <div class="col-md-4 mb-3">
        <div class="card border-warning h-100">
          <div class="card-header bg-warning text-dark">
            <h6 class="mb-0">😊 Sentiment Analysis</h6>
            <small>TextBlob + VADER</small>
          </div>
          <div class="card-body text-center">
            <div class="display-1 mb-3">${moodEmoji}</div>
            <h5 class="text-${moodColor}">${sentiment.overallMood || 'Trung tính'}</h5>
            <p class="text-muted">${sentiment.interpretation || 'Không có dữ liệu'}</p>
            <hr>
            <div class="row text-center">
              <div class="col-4">
                <div class="text-success">😊</div>
                <strong>${sentiment.positive_count || 0}</strong>
              </div>
              <div class="col-4">
                <div class="text-secondary">😐</div>
                <strong>${sentiment.neutral_count || 0}</strong>
              </div>
              <div class="col-4">
                <div class="text-danger">😢</div>
                <strong>${sentiment.negative_count || 0}</strong>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }
  
  html += '</div>';
  
  // SHAP Explanations
  if (data.shap_explanations) {
    html += `
      <div class="card mt-3">
        <div class="card-header bg-dark text-white">
          <h6 class="mb-0">🔍 SHAP Explanations - Why AI recommends this?</h6>
        </div>
        <div class="card-body">
          <div class="row">
            ${Object.entries(data.shap_explanations).map(([feature, importance]) => `
              <div class="col-md-3 mb-2">
                <div class="d-flex justify-content-between">
                  <span>${feature}:</span>
                  <strong>${Math.round(importance * 100)}%</strong>
                </div>
                <div class="progress" style="height: 8px;">
                  <div class="progress-bar bg-info" style="width: ${importance * 100}%"></div>
                </div>
              </div>
            `).join('')}
          </div>
        </div>
      </div>
    `;
  }
  
  // Optimization Suggestions
  if (data.optimization_suggestions && data.optimization_suggestions.length > 0) {
    html += `
      <div class="alert alert-success mt-3">
        <h6>💡 AI Recommendations:</h6>
        <ul class="mb-0">
          ${data.optimization_suggestions.map(sug => `<li>${sug}</li>`).join('')}
        </ul>
      </div>
    `;
  }
  
  // Budget Summary
  if (data.budgetSummary && data.budgetSummary.length > 0) {
    html += `
      <div class="card mt-3">
        <div class="card-header">
          <h6 class="mb-0">📊 Budget vs Actual</h6>
        </div>
        <div class="card-body">
          <div class="table-responsive">
            <table class="table table-sm">
              <thead>
                <tr>
                  <th>Category</th>
                  <th>Budget</th>
                  <th>Spent</th>
                  <th>%</th>
                </tr>
              </thead>
              <tbody>
                ${data.budgetSummary.map(b => `
                  <tr class="${b.percentage >= 100 ? 'table-danger' : b.percentage >= 80 ? 'table-warning' : ''}">
                    <td>${b.categoryName}</td>
                    <td>${(b.amount || 0).toLocaleString('vi-VN')} ₫</td>
                    <td>${(b.spentAmount || 0).toLocaleString('vi-VN')} ₫</td>
                    <td><strong>${Math.round(b.percentage || 0)}%</strong></td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `;
  }
  
  resultDiv.innerHTML = html;
}

async function getCategoryForecast() {
  const category = document.getElementById('forecast-category').value;
  const resultDiv = document.getElementById('forecast-result');
  
  if (!category) {
    alert('Vui lòng chọn danh mục');
    return;
  }
  
  try {
    resultDiv.innerHTML = `
      <div class="text-center py-3">
        <div class="spinner-border spinner-border-sm text-info"></div>
        <p class="mb-0 mt-2"><small>Prophet analyzing...</small></p>
      </div>
    `;
    
    const response = await fetch(`http://localhost:8080/api/budgets/forecast/${encodeURIComponent(category)}`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + localStorage.getItem('accessToken')
      }
    });
    
    const data = await response.json();
    
    if (data.success && data.data && data.data.forecast) {
      const forecast = data.data.forecast;
      const trendIcon = forecast.trend === 'increasing' ? '📈' : 
                       forecast.trend === 'decreasing' ? '📉' : '➡️';
      const trendColor = forecast.trend === 'increasing' ? 'danger' : 
                        forecast.trend === 'decreasing' ? 'success' : 'secondary';
      
      resultDiv.innerHTML = `
        <div class="card">
          <div class="card-body">
            <h6 class="text-${trendColor}">${trendIcon} ${forecast.trend.toUpperCase()}</h6>
            <h4 class="text-primary">${(forecast.forecast || 0).toLocaleString('vi-VN')} ₫</h4>
            <p class="text-muted mb-2">Dự đoán 3 tháng tới</p>
            <div class="progress mb-2" style="height: 10px;">
              <div class="progress-bar bg-info" style="width: ${(forecast.confidence || 0) * 100}%"></div>
            </div>
            <small class="text-info">Confidence: ${Math.round((forecast.confidence || 0) * 100)}%</small>
            ${forecast.seasonality ? `<br><small class="text-muted">Pattern: ${forecast.seasonality}</small>` : ''}
          </div>
        </div>
      `;
    } else {
      resultDiv.innerHTML = `<div class="alert alert-warning">${data.message || 'No forecast available'}</div>`;
    }
  } catch (error) {
    console.error('Forecast error:', error);
    resultDiv.innerHTML = `<div class="alert alert-danger">❌ ${error.message}</div>`;
  }
}

async function analyzeSentiment() {
  const month = document.getElementById('ultra-month').value;
  const year = document.getElementById('ultra-year').value;
  const resultDiv = document.getElementById('sentiment-result');
  
  try {
    resultDiv.innerHTML = `
      <div class="text-center py-3">
        <div class="spinner-border spinner-border-sm text-warning"></div>
        <p class="mb-0 mt-2"><small>Analyzing mood...</small></p>
      </div>
    `;
    
    const response = await fetch(`http://localhost:8080/api/budgets/sentiment-analysis?month=${month}&year=${year}`, {
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + localStorage.getItem('accessToken')
      }
    });
    
    const data = await response.json();
    
    if (data.success && data.data) {
      const sentiment = data.data;
      const avgScore = sentiment.average_sentiment || 0;
      let moodEmoji = '😐';
      let moodColor = 'secondary';
      if (avgScore > 0.5) { moodEmoji = '😊'; moodColor = 'success'; }
      else if (avgScore > 0.2) { moodEmoji = '🙂'; moodColor = 'info'; }
      else if (avgScore < -0.5) { moodEmoji = '😢'; moodColor = 'danger'; }
      else if (avgScore < -0.2) { moodEmoji = '😕'; moodColor = 'warning'; }
      
      let html = `
        <div class="card">
          <div class="card-body text-center">
            <div class="display-3">${moodEmoji}</div>
            <h5 class="text-${moodColor} mt-2">${sentiment.overallMood || 'Trung tính'}</h5>
            <p class="text-muted">${sentiment.interpretation || ''}</p>
            <hr>
            <div class="row">
              <div class="col-4">
                <div class="text-success">😊 ${sentiment.positive_count || 0}</div>
                <small>Positive</small>
              </div>
              <div class="col-4">
                <div class="text-secondary">😐 ${sentiment.neutral_count || 0}</div>
                <small>Neutral</small>
              </div>
              <div class="col-4">
                <div class="text-danger">😢 ${sentiment.negative_count || 0}</div>
                <small>Negative</small>
              </div>
            </div>
          </div>
        </div>
      `;
      
      if (sentiment.results && sentiment.results.length > 0) {
        html += `
          <div class="mt-2" style="max-height: 200px; overflow-y: auto;">
            <small class="text-muted">Recent transactions:</small>
            ${sentiment.results.slice(0, 5).map(r => {
              const emoji = r.category === 'positive' ? '😊' : r.category === 'negative' ? '😢' : '😐';
              return `<div class="d-flex justify-content-between align-items-center py-1 border-bottom">
                <small>${emoji} ${r.text}</small>
                <small class="text-muted">${r.sentiment_score > 0 ? '+' : ''}${r.sentiment_score.toFixed(2)}</small>
              </div>`;
            }).join('')}
          </div>
        `;
      }
      
      resultDiv.innerHTML = html;
    } else {
      resultDiv.innerHTML = `<div class="alert alert-warning">${data.message || 'No sentiment data'}</div>`;
    }
  } catch (error) {
    console.error('Sentiment error:', error);
    resultDiv.innerHTML = `<div class="alert alert-danger">❌ ${error.message}</div>`;
  }
}

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
        'Authorization': 'Bearer ' + localStorage.getItem('accessToken')
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

// ============================================================
// SMART BUDGET RECOMMENDATIONS
// ============================================================

async function getSmartBudget() {
  const months = document.getElementById('smart-budget-months')?.value || 3;
  const resultDiv = document.getElementById('smart-budget-result');
  if (!resultDiv) return;
  
  resultDiv.innerHTML = '<div class="text-center"><div class="spinner-border text-success"></div><p>AI đang phân tích...</p></div>';
  
  try {
    const response = await fetch(`http://localhost:8080/api/ai/smart-budget?months=${months}`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('accessToken')}`,
        'Content-Type': 'application/json'
      }
    });
    
    if (!response.ok) throw new Error('Failed to fetch smart budget');
    
    const data = await response.json();
    displaySmartBudget(data);
  } catch (error) {
    console.error('Smart budget error:', error);
    resultDiv.innerHTML = `<div class="alert alert-danger">⚠️ Không thể tải Smart Budget. Vui lòng thử lại sau.</div>`;
  }
}

function displaySmartBudget(data) {
  const resultDiv = document.getElementById('smart-budget-result');
  if (!resultDiv) return;
  
  let html = '<div class="smart-budget-results">';
  
  // Overall Summary
  if (data.summary) {
    html += `
      <div class="card mb-3 border-success">
        <div class="card-body">
          <h6 class="text-success">📊 Tổng quan</h6>
          <div class="row">
            <div class="col-md-4">
              <strong>Tổng chi tiêu TB:</strong><br>
              <span class="fs-5 text-danger">${data.summary.avgTotalSpending?.toLocaleString() || 0} VND</span>
            </div>
            <div class="col-md-4">
              <strong>Ngân sách đề xuất:</strong><br>
              <span class="fs-5 text-success">${data.summary.recommendedTotal?.toLocaleString() || 0} VND</span>
            </div>
            <div class="col-md-4">
              <strong>Tiết kiệm được:</strong><br>
              <span class="fs-5 text-primary">${data.summary.potentialSavings?.toLocaleString() || 0} VND</span>
            </div>
          </div>
        </div>
      </div>
    `;
  }
  
  // Recommended Budgets by Category
  if (data.recommendations && data.recommendations.length > 0) {
    html += `
      <h6 class="mt-3">💰 Ngân sách đề xuất theo danh mục</h6>
      <div class="table-responsive">
        <table class="table table-hover">
          <thead class="table-success">
            <tr>
              <th>Danh mục</th>
              <th>Chi tiêu TB</th>
              <th>Đề xuất</th>
              <th>Tiết kiệm</th>
              <th>Lý do</th>
            </tr>
          </thead>
          <tbody>
    `;
    
    data.recommendations.forEach(rec => {
      const savings = (rec.currentAvg || 0) - (rec.recommended || 0);
      const savingsClass = savings > 0 ? 'text-success' : 'text-danger';
      
      html += `
        <tr>
          <td><strong>${rec.categoryName || rec.category}</strong></td>
          <td>${rec.currentAvg?.toLocaleString() || 0} VND</td>
          <td class="text-primary"><strong>${rec.recommended?.toLocaleString() || 0} VND</strong></td>
          <td class="${savingsClass}">${savings > 0 ? '+' : ''}${savings.toLocaleString()} VND</td>
          <td><small>${rec.reason || 'Dựa trên lịch sử chi tiêu'}</small></td>
        </tr>
      `;
    });
    
    html += '</tbody></table></div>';
  }
  
  // AI Insights
  if (data.insights && data.insights.length > 0) {
    html += `
      <h6 class="mt-3">💡 AI Insights</h6>
      <ul class="list-group">
    `;
    
    data.insights.forEach(insight => {
      html += `<li class="list-group-item">✨ ${insight}</li>`;
    });
    
    html += '</ul>';
  }
  
  // Action Buttons
  html += `
    <div class="mt-3 text-center">
      <button class="btn btn-success" onclick="applySmartBudgets()">
        ✅ Áp dụng tất cả ngân sách đề xuất
      </button>
      <button class="btn btn-outline-secondary ms-2" onclick="getSmartBudget()">
        🔄 Phân tích lại
      </button>
    </div>
  `;
  
  html += '</div>';
  resultDiv.innerHTML = html;
}

function applySmartBudgets() {
  alert('Tính năng này sẽ tự động tạo ngân sách theo đề xuất của AI. Chức năng đang được phát triển!');
}
