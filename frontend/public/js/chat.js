document.addEventListener('DOMContentLoaded', function () {
  const input = document.getElementById('chat-input');
  const btn = document.getElementById('chat-send');
  const pane = document.getElementById('chat-pane');

  function append(cls, text) {
    const div = document.createElement('div');
    div.className = 'bubble ' + cls + ' mb-2';
    div.style.maxWidth = '75%';
    div.style.padding = '10px';
    div.style.borderRadius = '14px';
    div.style.background = cls === 'me' ? '#DCF8C6' : '#fff';
    div.style.alignSelf = cls === 'me' ? 'flex-end' : 'flex-start';
    div.textContent = text;
    pane.appendChild(div);
    pane.scrollTop = pane.scrollHeight;
  }

  btn.addEventListener('click', function () {
    const q = input.value.trim();
    if (!q) return;
    append('me', q);
    input.value = '';
    append('bot', '...');
    axios.post('/api/ai/chat', { message: q })
      .then(r => {
        pane.lastChild.textContent = r.data.reply;
      })
      .catch(e => {
        pane.lastChild.remove();
        alert(e.message);
      });
  });

  input.addEventListener('keydown', function (e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      btn.click();
    }
  });
});
