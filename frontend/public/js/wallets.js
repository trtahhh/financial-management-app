document.addEventListener('DOMContentLoaded', function () {
  const t = document.getElementById('wallet-table');
  const m = new bootstrap.Modal(document.getElementById('wallet-modal'));
  const f = document.getElementById('wallet-form');
  const title = document.getElementById('wallet-modal-title');
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

  function safeJson(res) {
    if (!res.ok) return res.text().then(text => { throw new Error(text); });
    if (res.headers.get('content-type')?.includes('application/json')) return res.json();
    return res.text().then(text => { throw new Error(text); });
  }

  function load() {
    fetch('http://localhost:8080/api/wallets', {
      headers: getAuthHeaders()
    })
      .then(safeJson)
      .then(d => {
        t.innerHTML =
          '<thead><tr><th>Tên ví</th><th>Số dư</th><th></th></tr></thead><tbody>' +
          d.map(w =>
            `<tr data-id="${w.id}">
              <td>${w.name}</td>
              <td>${formatCurrency(w.balance || 0)}</td>
              <td>
                <button class="btn btn-sm btn-outline-primary edit">Sửa</button>
                <button class="btn btn-sm btn-outline-danger ms-2 del">Xoá</button>
                <button class="btn btn-sm btn-outline-info ms-2 update-balance">Cập nhật</button>
              </td>
            </tr>`
          ).join('') + '</tbody>';
      }).catch(e => alert(e.message));
  }

  function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  }

  document.getElementById('wallet-add-btn').addEventListener('click', function () {
    editing = null;
    f.reset();
    title.textContent = 'Thêm ví';
    m.show();
  });

  document.getElementById('update-balances-btn').addEventListener('click', function () {
    if (confirm('Cập nhật số dư tất cả ví dựa trên giao dịch?')) {
      fetch('http://localhost:8080/api/wallets/update-balances', { 
        method: 'POST',
        headers: getAuthHeaders()
      })
        .then(res => {
          if (!res.ok) return res.text().then(text => { throw new Error(text); });
          return res.text();
        })
        .then(message => {
          alert('Đã cập nhật số dư thành công!');
          load();
        })
        .catch(e => alert('Lỗi cập nhật số dư: ' + e.message));
    }
  });

  t.addEventListener('click', function (e) {
    const id = e.target.closest('tr')?.dataset.id;
    if (e.target.classList.contains('edit')) {
      fetch('http://localhost:8080/api/wallets/' + id, {
        headers: getAuthHeaders()
      })
        .then(safeJson)
        .then(w => {
          editing = id;
          f.name.value = w.name;
          f.balance.value = w.balance;
          title.textContent = 'Sửa ví';
          m.show();
        }).catch(e => alert(e.message));
    }
    if (e.target.classList.contains('del')) {
      if (confirm('Bạn chắc chắn xoá ví này?')) {
        fetch('http://localhost:8080/api/wallets/' + id, { 
          method: 'DELETE',
          headers: getAuthHeaders()
        })
          .then(res => {
            if (!res.ok) return res.text().then(text => { throw new Error(text); });
            load();
          })
          .catch(e => alert(e.message));
      }
    }
    if (e.target.classList.contains('update-balance')) {
      if (confirm('Cập nhật số dư ví này dựa trên giao dịch?')) {
        fetch('http://localhost:8080/api/wallets/' + id + '/update-balance', { 
          method: 'POST',
          headers: getAuthHeaders()
        })
          .then(res => {
            if (!res.ok) return res.text().then(text => { throw new Error(text); });
            return res.text();
          })
          .then(message => {
            alert('Đã cập nhật số dư ví thành công!');
            load();
          })
          .catch(e => alert('Lỗi cập nhật số dư: ' + e.message));
      }
    }
  });

  f.addEventListener('submit', function (e) {
    e.preventDefault();
    const data = {
      name: f.name.value,
      balance: +f.balance.value,
      type: 'CASH'  // Default type
    };
    const method = editing ? 'PUT' : 'POST';
    const url = 'http://localhost:8080/api/wallets' + (editing ? '/' + editing : '');
    fetch(url, {
      method,
      headers: getAuthHeaders(),
      body: JSON.stringify(data)
    })
      .then(res => {
        if (!res.ok) return res.text().then(text => { throw new Error(text); });
        m.hide();
        load();
      })
      .catch(e => alert(e.message));
  });

  load();
});
