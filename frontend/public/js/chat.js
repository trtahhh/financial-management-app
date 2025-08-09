document.addEventListener('DOMContentLoaded', function () {
  const input = document.getElementById('chat-input');
  const btn = document.getElementById('chat-send');
  const pane = document.getElementById('chat-pane');

  function append(who, msg) {
    const div = document.createElement('div');
    div.className = 'mb-2';
    div.innerHTML = `<b>${who}:</b> ${msg}`;
    pane.appendChild(div);
    pane.scrollTop = pane.scrollHeight;
    return div;
  }

  btn.addEventListener('click', function () {
    const q = input.value.trim();
    if (!q) {
      alert('Vui lòng nhập câu hỏi!');
      return;
    }
    
    console.log('Sending message:', q);
    append('Bạn', q);
    input.value = '';
    btn.disabled = true;
    btn.textContent = 'Đang gửi...';
    
    // Hiển thị tin nhắn "đang trả lời..."
    const loadingMessage = append('AI', '🤔 Đang suy nghĩ...');
    
    const token = localStorage.getItem('authToken');
    const headers = { 'Content-Type': 'application/json' };
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    
    fetch('http://localhost:8080/api/chat/message', {
      method: 'POST',
      headers: headers,
      body: JSON.stringify({ message: q })
    })
    .then(r => {
      console.log('Response status:', r.status);
      return r.json();
    })
    .then(data => {
      console.log('Response data:', data);
      // Xóa tin nhắn "đang trả lời..."
      pane.removeChild(pane.lastChild);
      
      if (data.success) {
        append('AI', data.message);
      } else {
        append('AI', '❌ Xin lỗi, tôi không thể trả lời lúc này: ' + (data.message || 'Lỗi không xác định'));
      }
    })
    .catch(e => {
      console.error('Chat error:', e);
      // Xóa tin nhắn "đang trả lời..."
      if (pane.lastChild) {
        pane.removeChild(pane.lastChild);
      }
      append('AI', '🔧 Xin lỗi, có lỗi kết nối. Vui lòng thử lại sau: ' + e.message);
    })
    .finally(() => {
      btn.disabled = false;
      btn.textContent = 'Gửi';
    });
  });

  input.addEventListener('keydown', function (e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      btn.click();
    }
  });

  // Function for suggestion buttons
  window.sendSuggestion = function(text) {
    input.value = text;
    btn.click();
  };
});
