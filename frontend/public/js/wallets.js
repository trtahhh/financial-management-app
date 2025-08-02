document.addEventListener('DOMContentLoaded', function () {
  const t = document.getElementById('wallet-table');
  const m = new bootstrap.Modal(document.getElementById('wallet-modal'));
  const f = document.getElementById('wallet-form');
  const title = document.getElementById('wallet-modal-title');
  let editing = null;

  function safeJson(res) {
    if (!res.ok) return res.text().then(text => { throw new Error(text); });
    if (res.headers.get('content-type')?.includes('application/json')) return res.json();
    return res.text().then(text => { throw new Error(text); });
  }

  function load() {
    fetch('/api/wallets')
      .then(safeJson)
      .then(d => {
        t.innerHTML =
          '<thead><tr><th>Tên ví</th><th>Số dư</th><th></th></tr></thead><tbody>' +
          d.map(w =>
            `<tr data-id="${w.id}">
              <td>${w.name}</td>
              <td>${w.balance}</td>
              <td>
                <button class="btn btn-sm btn-outline-primary edit">Sửa</button>
                <button class="btn btn-sm btn-outline-danger ms-2 del">Xoá</button>
              </td>
            </tr>`
          ).join('') + '</tbody>';
      }).catch(e => alert(e.message));
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
      fetch('/api/wallets/' + id)
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
        fetch('/api/wallets/' + id, { method: 'DELETE' })
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
      balance: +f.balance.value
    };
    const method = editing ? 'PUT' : 'POST';
    const url = '/api/wallets' + (editing ? '/' + editing : '');
    fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
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
