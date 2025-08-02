document.addEventListener('DOMContentLoaded', function () {
  const t = document.getElementById('budget-table');
  const m = new bootstrap.Modal(document.getElementById('budget-modal'));
  const f = document.getElementById('budget-form');
  const title = document.getElementById('budget-modal-title');
  let editing = null;

  function load() {
    fetch('/api/budgets')
      .then(r => r.json())
      .then(d => {
        t.innerHTML =
          '<thead><tr><th>Tháng</th><th>Năm</th><th>Danh mục</th><th>Số tiền</th><th>Đã dùng</th><th></th></tr></thead><tbody>' +
          d.map(b =>
            `<tr data-id="${b.id}">
              <td>${b.month}</td>
              <td>${b.year}</td>
              <td>${b.category_id}</td>
              <td>${b.amount}</td>
              <td>
                <div class="progress">
                  <div class="progress-bar bg-success" role="progressbar" style="width:${b.progress||0}%">${b.progress||0}%</div>
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
    m.show();
  });

  t.addEventListener('click', function (e) {
    const id = e.target.closest('tr')?.dataset.id;
    if (e.target.classList.contains('edit')) {
      fetch('/api/budgets/' + id)
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
        fetch('/api/budgets/' + id, { method: 'DELETE' })
          .then(load)
          .catch(e => alert(e.message));
      }
    }
  });

  f.addEventListener('submit', function (e) {
    e.preventDefault();
    const data = {
      month: +f.month.value,
      year: +f.year.value,
      category_id: f.category_id.value,
      amount: +f.amount.value
    };
    const method = editing ? 'PUT' : 'POST';
    const url = '/api/budgets' + (editing ? '/' + editing : '');
    fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    }).then(() => {
      m.hide();
      load();
    }).catch(e => alert(e.message));
  });

  load();
});
