// Chat functionality for ChatGPT-style interface
let chatHistory = [];
let isProcessing = false;
let chatCount = 0;

// Initialize chat when page loads
document.addEventListener('DOMContentLoaded', function() {
    loadAIStatus();
    setupEventListeners();
    updateStats();
});

// Setup event listeners
function setupEventListeners() {
    const messageInput = document.getElementById('message-input');
    const sendButton = document.getElementById('send-button');

    // Auto-resize textarea
    messageInput.addEventListener('input', function() {
        this.style.height = 'auto';
        this.style.height = Math.min(this.scrollHeight, 200) + 'px';
    });

    // Send message on Enter (Shift+Enter for new line)
    messageInput.addEventListener('keydown', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    // Send button click
    sendButton.addEventListener('click', sendMessage);
}

// Load AI status
async function loadAIStatus() {
    try {
        // Lấy JWT token từ localStorage
        const token = localStorage.getItem('authToken');
        if (!token) {
            document.getElementById('ai-status-badge').innerHTML = '<i class="fas fa-circle me-1"></i>❌ Chưa đăng nhập';
            document.getElementById('ai-status-badge').style.background = '#ef4444';
            return;
        }

        const response = await fetch('/api/chat/status', {
            headers: {
                'Authorization': 'Bearer ' + token
            }
        });
        const data = await response.json();
        
        if (data.success) {
            const status = data.status;
            const badge = document.getElementById('ai-status-badge');
            
            if (status.provider === 'openrouter' && status.openrouter_available) {
                badge.innerHTML = '<i class="fas fa-circle me-1"></i>🟢 OpenRouter AI';
                badge.style.background = '#19c37d';
            } else if (status.provider === 'openrouter') {
                badge.innerHTML = '<i class="fas fa-circle me-1"></i>🟡 OpenRouter Offline';
                badge.style.background = '#f59e0b';
            } else {
                badge.innerHTML = '<i class="fas fa-circle me-1"></i>🌐 OpenRouter';
                badge.style.background = '#3b82f6';
            }
        }
    } catch (error) {
        console.error('Error loading AI status:', error);
        document.getElementById('ai-status-badge').innerHTML = '<i class="fas fa-circle me-1"></i>❌ Lỗi kết nối';
        document.getElementById('ai-status-badge').style.background = '#ef4444';
    }
}

// Send message
async function sendMessage() {
    const messageInput = document.getElementById('message-input');
    const sendButton = document.getElementById('send-button');
    const message = messageInput.value.trim();
    
    if (!message || isProcessing) return;
    
    // Add user message to chat
    appendMessage('user', message);
    
    // Clear input and disable send button
    messageInput.value = '';
    messageInput.style.height = 'auto';
    sendButton.disabled = true;
    isProcessing = true;
    
    // Show typing indicator
    showTypingIndicator();
    
    const startTime = Date.now();
    
    try {
        // Lấy JWT token từ localStorage
        const token = localStorage.getItem('authToken');
        if (!token) {
            appendMessage('ai', 'Xin lỗi, bạn cần đăng nhập để sử dụng AI Chat. Vui lòng đăng nhập lại.');
            return;
        }

        const response = await fetch('/api/chat/message', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify({ message: message }),
            signal: AbortSignal.timeout(200000) // 200 seconds timeout
        });
        
        const data = await response.json();
        
        if (data.success) {
            // Add AI response to chat
            appendMessage('ai', data.message);
            
            // Update chat count
            chatCount++;
            
            // Update chat history
            updateChatHistory(message, data.message);
        } else {
            appendMessage('ai', 'Xin lỗi, có lỗi xảy ra: ' + data.message);
        }
    } catch (error) {
        if (error.name === 'TimeoutError') {
            appendMessage('ai', 'Xin lỗi, câu hỏi của bạn hơi phức tạp và tôi cần thời gian suy nghĩ. Hãy thử đặt câu hỏi khác.');
        } else {
            appendMessage('ai', 'Có lỗi xảy ra khi kết nối với AI. Vui lòng thử lại.');
        }
        console.error('Error sending message:', error);
    } finally {
        // Hide typing indicator and re-enable send button
        hideTypingIndicator();
        sendButton.disabled = false;
        isProcessing = false;
        
        // Update stats
        updateStats();
    }
}

// Append message to chat - ChatGPT style
function appendMessage(who, message) {
    const chatMessages = document.getElementById('chat-messages');
    
    // Remove welcome message if it exists
    const welcomeMessage = chatMessages.querySelector('.welcome-message');
    if (welcomeMessage) {
        welcomeMessage.remove();
    }
    
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${who}`;
    
    const messageContainer = document.createElement('div');
    messageContainer.className = 'message-container';
    
    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    
    if (who === 'user') {
        avatar.innerHTML = '<i class="fas fa-user"></i>';
    } else {
        avatar.innerHTML = '<i class="fas fa-robot"></i>';
    }
    
    const content = document.createElement('div');
    content.className = 'message-content';
    
    // Format AI response để đẹp như ChatGPT
    if (who === 'ai') {
        // Chuyển đổi text thành HTML với format đẹp
        const formattedMessage = formatAIResponse(message);
        content.innerHTML = formattedMessage;
    } else {
        // User message giữ nguyên text
        content.textContent = message;
    }
    
    messageContainer.appendChild(avatar);
    messageContainer.appendChild(content);
    messageDiv.appendChild(messageContainer);
    
    chatMessages.appendChild(messageDiv);
    
    // Scroll to bottom
    chatMessages.scrollTop = chatMessages.scrollHeight;
    
    // Add to chat history
    chatHistory.push({ who, message, timestamp: new Date() });
}

// Format AI response để đẹp như ChatGPT
function formatAIResponse(message) {
    if (!message) return '';
    
    let formatted = message;
    
    // Thay thế các dấu xuống dòng thành <br>
    formatted = formatted.replace(/\n/g, '<br>');
    
    // Thêm style cho các ý chính (số thứ tự)
    formatted = formatted.replace(/(\d+\.\s)/g, '<strong style="color: var(--chatgpt-text);">$1</strong>');
    
    // Thêm style cho các tiêu đề (dấu - hoặc *)
    formatted = formatted.replace(/^[-*]\s+(.+)$/gm, '<strong style="color: var(--chatgpt-text); display: block; margin-top: 12px; margin-bottom: 8px;">$1</strong>');
    
    // Thêm style cho các từ khóa quan trọng
    formatted = formatted.replace(/\*\*(.+?)\*\*/g, '<strong style="color: var(--chatgpt-text);">$1</strong>');
    
    // Thêm style cho các gạch chân
    formatted = formatted.replace(/__(.+?)__/g, '<em style="color: var(--chatgpt-text-secondary);">$1</em>');
    
    // Thêm style cho các bullet points
    formatted = formatted.replace(/^•\s+(.+)$/gm, '<div style="margin-left: 20px; margin-bottom: 8px;">• $1</div>');
    
    // Thêm style cho các đoạn văn
    formatted = formatted.replace(/<br><br>/g, '</p><p style="margin: 16px 0; line-height: 1.6;">');
    
    // Wrap trong paragraph tags
    formatted = '<p style="margin: 0; line-height: 1.6;">' + formatted + '</p>';
    
    return formatted;
}

// Show typing indicator - ChatGPT style
function showTypingIndicator() {
    const indicator = document.getElementById('typing-indicator');
    indicator.style.display = 'block';
    
    // Scroll to bottom
    const chatMessages = document.getElementById('chat-messages');
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// Hide typing indicator
function hideTypingIndicator() {
    const indicator = document.getElementById('typing-indicator');
    indicator.style.display = 'none';
}

// Send suggestion
function sendSuggestion(suggestion) {
    const messageInput = document.getElementById('message-input');
    messageInput.value = suggestion;
    messageInput.focus();
    sendMessage();
}

// Update chat history sidebar
function updateChatHistory(userMessage, aiMessage) {
    const historyContainer = document.getElementById('chat-history');
    
    const historyItem = document.createElement('div');
    historyItem.className = 'chat-history-item';
    
    const icon = document.createElement('i');
    icon.className = 'fas fa-comment';
    
    const title = document.createElement('span');
    title.textContent = userMessage.length > 30 ? userMessage.substring(0, 30) + '...' : userMessage;
    
    historyItem.appendChild(icon);
    historyItem.appendChild(title);
    
    // Add click to restore chat
    historyItem.onclick = () => restoreChat(userMessage, aiMessage);
    
    historyContainer.insertBefore(historyItem, historyContainer.firstChild);
    
    // Keep only last 10 items
    const items = historyContainer.querySelectorAll('.chat-history-item');
    if (items.length > 10) {
        items[items.length - 1].remove();
    }
}

// Restore chat from history
function restoreChat(userMessage, aiMessage) {
    // Clear current chat
    const chatMessages = document.getElementById('chat-messages');
    chatMessages.innerHTML = '';
    
    // Add restored messages
    appendMessage('user', userMessage);
    appendMessage('ai', aiMessage);
    
    showToast('Đã khôi phục cuộc trò chuyện', 'info');
}

// New chat function
function newChat() {
    if (confirm('Bạn có muốn bắt đầu cuộc trò chuyện mới?')) {
        clearChat();
    }
}

// Clear chat
function clearChat() {
    if (confirm('Bạn có chắc muốn xóa toàn bộ cuộc trò chuyện?')) {
        const chatMessages = document.getElementById('chat-messages');
        chatMessages.innerHTML = '';
        
        // Add welcome message back
        const welcomeMessage = document.createElement('div');
        welcomeMessage.className = 'welcome-message';
        welcomeMessage.innerHTML = `
            <h1>Chào mừng đến với AI Tài chính</h1>
            <p>Tôi có thể giúp bạn với các vấn đề về tài chính, đầu tư, tiết kiệm và quản lý ngân sách.</p>
            
            <div class="examples-grid">
                <div class="example-card" onclick="sendSuggestion('Làm thế nào để tiết kiệm tiền hiệu quả?')">
                    <h4>💰 Tiết kiệm thông minh</h4>
                    <p>Chiến lược tiết kiệm và quản lý chi tiêu hiệu quả</p>
                </div>
                <div class="example-card" onclick="sendSuggestion('Tôi nên đầu tư như thế nào?')">
                    <h4>📈 Hướng dẫn đầu tư</h4>
                    <p>Kiến thức cơ bản về đầu tư cho người mới bắt đầu</p>
                </div>
                <div class="example-card" onclick="sendSuggestion('Cách lập ngân sách gia đình?')">
                    <h4>🏠 Ngân sách gia đình</h4>
                    <p>Quản lý tài chính gia đình một cách khoa học</p>
                </div>
                <div class="example-card" onclick="sendSuggestion('Quản lý nợ thông minh ra sao?')">
                    <h4>💳 Quản lý nợ</h4>
                    <p>Chiến lược trả nợ và quản lý tín dụng hiệu quả</p>
                </div>
            </div>
            
            <div class="capabilities-grid">
                <div class="capability-card">
                    <i class="fas fa-chart-line"></i>
                    <h5>Phân tích tài chính</h5>
                    <p>Đánh giá tình hình thu chi</p>
                </div>
                <div class="capability-card">
                    <i class="fas fa-piggy-bank"></i>
                    <h5>Lập kế hoạch</h5>
                    <p>Kế hoạch tài chính dài hạn</p>
                </div>
                <div class="capability-card">
                    <i class="fas fa-shield-alt"></i>
                    <h5>Quản lý rủi ro</h5>
                    <p>Đánh giá và giảm thiểu rủi ro</p>
                </div>
                <div class="capability-card">
                    <i class="fas fa-graduation-cap"></i>
                    <h5>Giáo dục tài chính</h5>
                    <p>Kiến thức tài chính cơ bản</p>
                </div>
            </div>
        `;
        chatMessages.appendChild(welcomeMessage);
        
        // Reset chat count
        chatCount = 0;
        
        // Clear chat history
        chatHistory = [];
        
        showToast('Đã xóa cuộc trò chuyện', 'success');
    }
}

// Toggle sidebar on mobile
function toggleSidebar() {
    const sidebar = document.getElementById('chat-sidebar');
    sidebar.classList.toggle('open');
}

// Update stats
function updateStats() {
    // Stats are updated in real-time during chat
}

// Show toast notification
function showToast(message, type = 'info') {
    // Create toast element
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.textContent = message;
    
    // Set background color based on type
    if (type === 'success') {
        toast.style.background = '#19c37d';
    } else if (type === 'error') {
        toast.style.background = '#ef4444';
    } else if (type === 'warning') {
        toast.style.background = '#f59e0b';
    } else {
        toast.style.background = '#3b82f6';
    }
    
    document.body.appendChild(toast);
    
    // Remove after 3 seconds
    setTimeout(() => {
        if (toast.parentNode) {
            toast.parentNode.removeChild(toast);
        }
    }, 3000);
}

// Close sidebar when clicking outside on mobile
document.addEventListener('click', function(e) {
    const sidebar = document.getElementById('chat-sidebar');
    const sidebarToggle = document.querySelector('.sidebar-toggle');
    
    if (window.innerWidth <= 768) {
        if (!sidebar.contains(e.target) && !sidebarToggle.contains(e.target)) {
            sidebar.classList.remove('open');
        }
    }
});

// Handle window resize
window.addEventListener('resize', function() {
    const sidebar = document.getElementById('chat-sidebar');
    if (window.innerWidth > 768) {
        sidebar.classList.remove('open');
    }
});
