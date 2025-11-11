document.addEventListener('DOMContentLoaded', function () {
  // Check authentication first
  const token = localStorage.getItem('authToken');
  if (!token) {
    alert('Vui lòng đăng nhập lại');
    window.location.href = '/login';
    return;
  }

  const t = document.getElementById('wallet-table');
  const m = new bootstrap.Modal(document.getElementById('wallet-modal'));
  const f = document.getElementById('wallet-form');
  const title = document.getElementById('wallet-modal-title');
  let editing = null;

  function getAuthHeaders() {
    const token = localStorage.getItem('authToken');
    // Token validation
    const headers = {
      'Content-Type': 'application/json'
    };
    if (token) {
      headers['Authorization'] = 'Bearer ' + token;
      // Authorization header configured
    } else {
      console.error('No auth token found in localStorage');
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
        // Wallets data loaded successfully
        
        t.innerHTML =
          '<thead><tr><th>Tên ví</th><th>Số dư</th><th></th></tr></thead><tbody>' +
          d.map(w =>
            `<tr data-id="${w.id}">
              <td>${w.name}</td>
              <td>${formatCurrency(w.balance || 0)}</td>
              <td>
                <button class="btn btn-sm btn-outline-primary edit">Sửa</button>
                <button class="btn btn-sm btn-outline-danger ms-2 del">Xoá</button>

              </td>
            </tr>`
          ).join('') + '</tbody>';
      }).catch(e => alert(e.message));
  }

  function formatCurrency(amount) {
    // Handle null/undefined/NaN cases
    if (amount === null || amount === undefined || isNaN(amount)) {
      amount = 0;
    }
    
    // Convert to number if it's a string
    if (typeof amount === 'string') {
      amount = Number(amount);
      if (isNaN(amount)) {
        amount = 0;
      }
    }
    
    // Hiển thị số tiền với định dạng Việt Nam và đơn vị VND
    const formatted = new Intl.NumberFormat('vi-VN').format(amount);
    return `${formatted} VND`;
  }

  document.getElementById('wallet-add-btn').addEventListener('click', function () {
    editing = null;
    f.reset();
    title.textContent = 'Thêm ví';
    m.show();
  });



  t.addEventListener('click', function (e) {
    const id = e.target.closest('tr')?.dataset.id;
    if (e.target.classList.contains('edit')) {
      fetch('http://localhost:8080/api/wallets/' + id, {
        headers: getAuthHeaders()
      })
        .then(safeJson)
        .then(w => {
          // Wallet data retrieved from API
          editing = id;
          f.name.value = w.name || '';
          
          // Handle different balance formats (BigDecimal from Java can be a number or string)
          let balanceValue = 0;
          if (w.balance !== null && w.balance !== undefined) {
            // Convert to number regardless of whether it's string or number
            balanceValue = Number(w.balance);
            if (isNaN(balanceValue)) {
              balanceValue = 0;
            }
          }
          
          f.balance.value = balanceValue;
          // Balance value updated in form
          title.textContent = 'Sửa ví';
          m.show();
        }).catch(e => {
          console.error('Error fetching wallet:', e);
          if (e.message.includes('401') || e.message.includes('Unauthorized')) {
            alert('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
            localStorage.removeItem('authToken');
            window.location.href = '/login';
          } else {
            alert('Lỗi: ' + e.message);
          }
        });
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
