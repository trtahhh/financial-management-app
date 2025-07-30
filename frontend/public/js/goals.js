document.addEventListener('DOMContentLoaded', function () {
  const list = document.getElementById('goal-list');
  const m = new bootstrap.Modal(document.getElementById('goal-modal'));
  const f = document.getElementById('goal-form');
  const title = document.getElementById('goal-modal-title');
  let editing = null;

  function load() {
    fetch('/api/goals')
      .then(r => r.json())
      .then(g => {
        list.innerHTML = '';
        for (const goal of g) {
          const div = document.createElement('div');
          div.className = 'mb-4';
          div.innerHTML = `
            <div class="d-flex justify-content-between align-items-center">
              <h5>${goal.name}</h5>
              <div>
                <button class="btn btn-sm btn-outline-primary edit" data-id="${goal.id}">Sửa</button>
                <button class="btn btn-sm btn-outline-danger ms-2 del" data-id="${goal.id}">Xoá</button>
              </div>
            </div>
            <div class="mb-2">Cần đạt: <b>${goal.target_amount}</b> &nbsp;|&nbsp; Đến: ${goal.due_date ? new Date(goal.due_date).toLocaleDateString() : ''}</div>
            <div class="progress"><div class="progress-bar" role="progressbar" style="width:${goal.progress||0}%">${goal.progress||0}%</div></div>
          `;
          list.appendChild(div);
        }
      }).catch(e => alert(e.message));
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
        .then(g => {
          editing = id;
          f.name.value = g.name;
          f.target_amount.value = g.target_amount;
          f.due_date.value = g.due_date ? g.due_date.substring(0,10) : '';
          title.textContent = 'Sửa mục tiêu';
          m.show();
        }).catch(e => alert(e.message));
    }
    if (e.target.classList.contains('del')) {
      if (confirm('Bạn chắc chắn xoá mục tiêu này?')) {
        fetch('/api/goals/' + id, { method: 'DELETE' })
          .then(load)
          .catch(e => alert(e.message));
      }
    }
  });

  f.addEventListener('submit', function (e) {
    e.preventDefault();
    const data = {
      name: f.name.value,
      target_amount: +f.target_amount.value,
      due_date: f.due_date.value
    };
    const method = editing ? 'PUT' : 'POST';
    const url = '/api/goals' + (editing ? '/' + editing : '');
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
