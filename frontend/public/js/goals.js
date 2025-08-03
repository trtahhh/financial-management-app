document.addEventListener('DOMContentLoaded', function () {
  const list = document.getElementById('goal-list');
  const m = new bootstrap.Modal(document.getElementById('goal-modal'));
  const f = document.getElementById('goal-form');
  const title = document.getElementById('goal-modal-title');
  let editing = null;

  function load() {
    fetch('/api/goals')
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
    fetch('/api/goals/predict')
      .then(r => r.json())
      .then(p => { document.getElementById('predict').textContent = p.message; })
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
      fetch('/api/goals/' + id)
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
        fetch('/api/goals/' + id, { method: 'DELETE' })
          .then(r => r.json())
          .then(response => {
            if (response.success !== false) {
              load();
              alert('Xóa mục tiêu thành công!');
            } else {
              alert('Lỗi xóa mục tiêu: ' + (response.message || 'Unknown error'));
            }
          })
          .catch(e => {
            console.error('Error deleting goal:', e);
            alert('Lỗi xóa mục tiêu: ' + e.message);
          });
      }
    }
  });

  f.addEventListener('submit', function (e) {
    e.preventDefault();
    
    const data = {
      name: f.name.value.trim(),
      targetAmount: parseFloat(f.target_amount.value),
      dueDate: f.due_date.value
    };
    
    // Validation
    if (!data.name) {
      alert('Tên mục tiêu không được để trống');
      return;
    }
    
    if (!data.targetAmount || data.targetAmount <= 0) {
      alert('Số tiền cần đạt phải lớn hơn 0');
      return;
    }
    
    if (!data.dueDate) {
      alert('Ngày hoàn thành dự kiến không được để trống');
      return;
    }
    
    const method = editing ? 'PUT' : 'POST';
    const url = '/api/goals' + (editing ? '/' + editing : '');
    
    fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    .then(r => r.json())
    .then(response => {
      if (response.success !== false) {
        m.hide();
        load();
        alert(editing ? 'Cập nhật mục tiêu thành công!' : 'Thêm mục tiêu thành công!');
      } else {
        alert('Lỗi: ' + (response.message || 'Unknown error'));
      }
    })
    .catch(e => {
      console.error('Error saving goal:', e);
      alert('Lỗi lưu mục tiêu: ' + e.message);
    });
  });

  load();
});
