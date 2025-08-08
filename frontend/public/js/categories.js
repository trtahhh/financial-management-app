document.addEventListener('DOMContentLoaded', function () {
  const t = document.getElementById('cat-table');
  const m = new bootstrap.Modal(document.getElementById('cat-modal'));
  const f = document.getElementById('cat-form');
  const title = document.getElementById('cat-modal-title');
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
    fetch('/api/categories', {
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
        
        t.innerHTML =
          '<thead><tr><th>Tên danh mục</th><th>Loại</th><th>Thao tác</th></tr></thead><tbody>' +
          categories.map(c =>
            `<tr data-id="${c.id}">
              <td>${c.name}</td>
              <td>
                <span class="badge ${c.type === 'INCOME' ? 'bg-success' : 'bg-danger'}">
                  ${c.type === 'INCOME' ? 'Thu' : 'Chi'}
                </span>
              </td>
              <td>
                <button class="btn btn-sm btn-outline-primary edit">Sửa</button>
                <button class="btn btn-sm btn-outline-danger ms-2 del">Xoá</button>
              </td>
            </tr>`
          ).join('') + '</tbody>';
      }).catch(e => {
        console.error('Error loading categories:', e);
        alert('Lỗi tải danh mục: ' + e.message);
      });
  }

  document.getElementById('cat-add-btn').addEventListener('click', function () {
    editing = null;
    f.reset();
    title.textContent = 'Thêm danh mục';
    m.show();
  });

  t.addEventListener('click', function (e) {
    const id = e.target.closest('tr')?.dataset.id;
    if (e.target.classList.contains('edit')) {
      fetch('/api/categories/' + id, {
        headers: getAuthHeaders()
      })
        .then(r => r.json())
        .then(response => {
          const category = response.data || response;
          editing = id;
          f.name.value = category.name;
          f.type.value = category.type || 'EXPENSE';
          title.textContent = 'Sửa danh mục';
          m.show();
        }).catch(e => {
          console.error('Error loading category:', e);
          alert('Lỗi tải thông tin danh mục: ' + e.message);
        });
    }
    if (e.target.classList.contains('del')) {
      if (confirm('Bạn chắc chắn xoá danh mục này?')) {
        fetch('/api/categories/' + id, { 
          method: 'DELETE',
          headers: getAuthHeaders()
        })
          .then(r => r.json())
          .then(response => {
            if (response.success !== false) {
              load();
            } else {
              alert('Lỗi xóa danh mục: ' + (response.message || 'Unknown error'));
            }
          })
          .catch(e => {
            console.error('Error deleting category:', e);
            alert('Lỗi xóa danh mục: ' + e.message);
          });
      }
    }
  });

  f.addEventListener('submit', function (e) {
    e.preventDefault();
    const data = {
      name: f.name.value,
      type: f.type.value || 'EXPENSE'
    };
    
    if (!data.name.trim()) {
      alert('Tên danh mục không được để trống');
      return;
    }
    
    const method = editing ? 'PUT' : 'POST';
    const url = '/api/categories' + (editing ? '/' + editing : '');
    
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
