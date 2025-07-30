document.addEventListener('DOMContentLoaded', function () {
  const t = document.getElementById('cat-table');
  const m = new bootstrap.Modal(document.getElementById('cat-modal'));
  const f = document.getElementById('cat-form');
  const title = document.getElementById('cat-modal-title');
  let editing = null;

  function load() {
    fetch('/api/categories')
      .then(r => r.json())
      .then(d => {
        t.innerHTML =
          '<thead><tr><th>Tên danh mục</th><th></th></tr></thead><tbody>' +
          d.map(c =>
            `<tr data-id="${c.id}">
              <td>${c.name}</td>
              <td>
                <button class="btn btn-sm btn-outline-primary edit">Sửa</button>
                <button class="btn btn-sm btn-outline-danger ms-2 del">Xoá</button>
              </td>
            </tr>`
          ).join('') + '</tbody>';
      }).catch(e => alert(e.message));
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
      fetch('/api/categories/' + id)
        .then(r => r.json())
        .then(c => {
          editing = id;
          f.name.value = c.name;
          title.textContent = 'Sửa danh mục';
          m.show();
        }).catch(e => alert(e.message));
    }
    if (e.target.classList.contains('del')) {
      if (confirm('Bạn chắc chắn xoá danh mục này?')) {
        fetch('/api/categories/' + id, { method: 'DELETE' })
          .then(load)
          .catch(e => alert(e.message));
      }
    }
  });

  f.addEventListener('submit', function (e) {
    e.preventDefault();
    const data = {
      name: f.name.value
    };
    const method = editing ? 'PUT' : 'POST';
    const url = '/api/categories' + (editing ? '/' + editing : '');
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
