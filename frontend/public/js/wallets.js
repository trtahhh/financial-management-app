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
    console.log('Current token:', token); // Debug log
    const headers = {
      'Content-Type': 'application/json'
    };
    if (token) {
      headers['Authorization'] = 'Bearer ' + token;
      console.log('Auth header set:', headers['Authorization']); // Debug log
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
        console.log('Wallets list data:', d); // Debug log
        d.forEach(w => console.log(`Wallet ${w.id}: name=${w.name}, balance=${w.balance} (${typeof w.balance})`)); // Debug each wallet
        
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
          console.log('Wallet data from API:', w); // Debug log
          console.log('Raw balance value:', w.balance, 'Type:', typeof w.balance); // Debug balance
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
          console.log('Final balance set to input:', f.balance.value); // Debug log
          title.textContent = 'Sửa ví';
          m.show();
        }).catch(e => {
          console.error('Error fetching wallet:', e); // Debug error
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
