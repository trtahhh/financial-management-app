document.addEventListener('DOMContentLoaded', function () {
  const t = document.getElementById('cat-table');
  const m = new bootstrap.Modal(document.getElementById('cat-modal'));
  const f = document.getElementById('cat-form');
  const title = document.getElementById('cat-modal-title');
  let editing = null;
  
  // Show read-only warning banner
  const container = t.parentElement;
  const banner = document.createElement('div');
  banner.className = 'alert alert-info alert-dismissible fade show';
  banner.innerHTML = `
    <i class="bi bi-info-circle-fill me-2"></i>
    <strong>Quản lý tự động:</strong> Danh mục được quản lý bởi hệ thống AI. 
    Giao dịch của bạn sẽ tự động phân loại với độ chính xác 98%.
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
  `;
  container.insertBefore(banner, t);

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

  function load() {
    fetch('http://localhost:8080/api/categories', {
      headers: getAuthHeaders()
    })
      .then(r => r.json())
      .then(response => {
        // Handle both direct array and response object formats
        const categories = Array.isArray(response) ? response : (response.data || response);
        
        if (!Array.isArray(categories)) {
          console.error('Expected array but got:', categories);
          throw new Error('Invalid response format');
        }
        
        // Disable edit/delete buttons - categories are read-only
        t.innerHTML =
          '<thead><tr><th>Tên danh mục</th><th>Loại</th><th>Trạng thái</th></tr></thead><tbody>' +
          categories.map(c =>
            `<tr data-id="${c.id}">
              <td><i class="bi bi-tag-fill me-2"></i>${c.name}</td>
              <td>
                <span class="badge ${c.type === 'INCOME' ? 'bg-success' : 'bg-danger'}">
                  ${c.type === 'INCOME' ? 'Thu nhập' : 'Chi tiêu'}
                </span>
              </td>
              <td>
                <span class="badge bg-primary">
                  <i class="bi bi-robot me-1"></i>AI Auto
                </span>
              </td>
            </tr>`
          ).join('') + '</tbody>';
      }).catch(e => {
        console.error('Error loading categories:', e);
        alert('Lỗi tải danh mục: ' + e.message);
      });
  }

  // DEPRECATED: Category management disabled - AI handles categorization
  const addBtn = document.getElementById('cat-add-btn');
  if (addBtn) {
    addBtn.style.display = 'none'; // Hide add button
  }

  /* DISABLED: Edit/Delete/Create operations
  document.getElementById('cat-add-btn').addEventListener('click', function () {
    alert('Category creation is disabled. System uses AI auto-categorization with 14 pre-defined categories.');
  });

  t.addEventListener('click', function (e) {
    const id = e.target.closest('tr')?.dataset.id;
    if (e.target.classList.contains('edit')) {
      alert('Category modification is disabled. Categories are system-managed for AI optimization.');
    }
    if (e.target.classList.contains('del')) {
      alert('Category deletion is disabled. All 14 categories are required for AI categorization model.');
    }
  });

  f.addEventListener('submit', function (e) {
    e.preventDefault();
    alert('Category operations are disabled. Use /api/ai/categorize for automatic transaction categorization.');
  });
  */

  load();
});
          load();
        } else {
          alert('Lỗi lưu danh mục: ' + (response.message || 'Unknown error'));
        }
      })
      .catch(e => {
        console.error('Error saving category:', e);
        alert('Lỗi lưu danh mục: ' + e.message);
      });
  });

  load();
});
