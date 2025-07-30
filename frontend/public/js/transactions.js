document.addEventListener('DOMContentLoaded', function() {
  const table = document.getElementById('tx-table');
  const form = document.getElementById('tx-form');
  const filter = document.getElementById('tx-filter');

  function load() {
    fetch('/api/transactions')
      .then(r => r.json())
      .then(d => {
        table.innerHTML =
          '<thead><tr><th>Ngày</th><th>Danh mục</th><th>Số tiền</th><th>Ghi chú</th></tr></thead><tbody>' +
          d.map(v => `<tr>
            <td>${new Date(v.date).toLocaleDateString()}</td>
            <td>${v.category}</td>
            <td>${v.amount}</td>
            <td>${v.note||''}</td>
          </tr>`).join('') + '</tbody>';
      }).catch(e => alert(e.message));
  }

  form.addEventListener('submit', function(e) {
    e.preventDefault();
    const fd = new FormData(form);
    fetch('/api/transactions', { method: 'POST', body: fd })
      .then(() => {
        form.reset();
        load();
      }).catch(e => alert(e.message));
  });

  filter.addEventListener('change', function(e) {
    fetch('/api/transactions?category=' + e.target.value)
      .then(r => r.json())
      .then(d => {
        table.querySelector('tbody').innerHTML = d.map(v =>
          `<tr>
            <td>${new Date(v.date).toLocaleDateString()}</td>
            <td>${v.category}</td>
            <td>${v.amount}</td>
            <td>${v.note||''}</td>
          </tr>`).join('');
      }).catch(e => alert(e.message));
  });

  load();
});
