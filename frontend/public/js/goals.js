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
      .then(response => {
        // Handle both direct array and response object formats
        const goals = Array.isArray(response) ? response : (response.data || response);
        
        if (!Array.isArray(goals)) {
          console.error('Expected array but got:', goals);
          list.innerHTML = '<div class="alert alert-warning">Không có mục tiêu nào</div>';
          return;
        }
        
        list.innerHTML = '';
        if (goals.length === 0) {
          list.innerHTML = '<div class="alert alert-info">Chưa có mục tiêu nào. Hãy thêm mục tiêu đầu tiên!</div>';
          return;
        }
        
        for (const goal of goals) {
          const div = document.createElement('div');
          div.className = 'card mb-3';
          
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
      }).catch(e => {
        console.error('Error loading goals:', e);
        list.innerHTML = '<div class="alert alert-danger">Lỗi tải mục tiêu: ' + e.message + '</div>';
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
          Number(totalSaved).toLocaleString('vi-VN') + ' VNĐ';
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
              Mục tiêu: ${Number(goal.targetAmount).toLocaleString('vi-VN')} VNĐ | 
              Hiện tại: ${Number(goal.currentBalance).toLocaleString('vi-VN')} VNĐ
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
                    ${Number(goal.targetAmount).toLocaleString('vi-VN')} VNĐ
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

  document.getElementById('goal-add-btn').addEventListener('click', function () {
    editing = null;
    f.reset();
    title.textContent = 'Thêm mục tiêu';
    m.show();
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
              alert(`✅ ${response.message}\n\nSố tiền: ${Number(response.transaction.amount).toLocaleString('vi-VN')} VNĐ\nVí: ${response.walletName}\nSố dư mới: ${Number(response.newBalance).toLocaleString('vi-VN')} VNĐ`);
              load(); // Reload để cập nhật UI
            } else {
              alert('❌ Lỗi: ' + response.error);
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
