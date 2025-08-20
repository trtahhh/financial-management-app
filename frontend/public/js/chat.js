// Chat functionality for ChatGPT-style interface
let chatHistory = [];
let isProcessing = false;
let chatCount = 0;

// Initialize chat when page loads
document.addEventListener('DOMContentLoaded', function() {
    loadAIStatus();
    setupEventListeners();
    updateStats();
    addReportTemplates();
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

// Add report templates to chat
function addReportTemplates() {
    const chatContainer = document.getElementById('chat-container');
    
                // Add welcome message with report templates
            const welcomeMessage = `
                <div class="message ai-message">
                    <div class="message-content">
                        <div class="ai-avatar">🤖</div>
                        <div class="message-text">
                            <p>Xin chào! Tôi là trợ lý AI tài chính thông minh của bạn. Tôi có thể giúp bạn:</p>

                            <div class="report-templates">
                                <h6>📊 <strong>Báo cáo tài chính</strong></h6>
                                <div class="template-buttons">
                                    <button class="btn btn-sm btn-outline-primary template-btn" onclick="useTemplate('Tạo báo cáo tổng hợp tháng này')">
                                        📈 Báo cáo tổng hợp
                                    </button>
                                    <button class="btn btn-sm btn-outline-success template-btn" onclick="useTemplate('Báo cáo giao dịch tháng này')">
                                        📋 Báo cáo giao dịch
                                    </button>
                                    <button class="btn btn-sm btn-outline-info template-btn" onclick="useTemplate('Báo cáo ngân sách tháng này')">
                                        💰 Báo cáo ngân sách
                                    </button>
                                    <button class="btn btn-sm btn-outline-warning template-btn" onclick="useTemplate('Xuất báo cáo Excel tháng này')">
                                        📊 Excel
                                    </button>
                                    <button class="btn btn-sm btn-outline-danger template-btn" onclick="useTemplate('Xuất báo cáo PDF tháng này')">
                                        📄 PDF
                                    </button>
                                </div>

                                <h6>🔍 <strong>Phân tích thông minh</strong></h6>
                                <div class="template-buttons">
                                    <button class="btn btn-sm btn-outline-primary template-btn" onclick="useTemplate('Phân tích chi tiêu của tôi')">
                                        💸 Phân tích chi tiêu
                                    </button>
                                    <button class="btn btn-sm btn-outline-success template-btn" onclick="useTemplate('Phân tích thu nhập hiện tại')">
                                        💰 Phân tích thu nhập
                                    </button>
                                    <button class="btn btn-sm btn-outline-info template-btn" onclick="useTemplate('Đánh giá hiệu quả ngân sách')">
                                        📊 Đánh giá ngân sách
                                    </button>
                                </div>

                                <h6>💡 <strong>Tư vấn tài chính</strong></h6>
                                <div class="template-buttons">
                                    <button class="btn btn-sm btn-outline-warning template-btn" onclick="useTemplate('Tư vấn tiết kiệm hiệu quả')">
                                        💰 Tiết kiệm thông minh
                                    </button>
                                    <button class="btn btn-sm btn-outline-danger template-btn" onclick="useTemplate('Tư vấn đầu tư cơ bản')">
                                        📈 Đầu tư an toàn
                                    </button>
                                    <button class="btn btn-sm btn-outline-secondary template-btn" onclick="useTemplate('Lời khuyên quản lý nợ')">
                                        💳 Quản lý nợ
                                    </button>
                                </div>

                                <h6>🎯 <strong>Mục tiêu tài chính</strong></h6>
                                <div class="template-buttons">
                                    <button class="btn btn-sm btn-outline-primary template-btn" onclick="useTemplate('Lập kế hoạch tài chính dài hạn')">
                                        📋 Kế hoạch dài hạn
                                    </button>
                                    <button class="btn btn-sm btn-outline-success template-btn" onclick="useTemplate('Dự báo tài chính tương lai')">
                                        🔮 Dự báo tương lai
                                    </button>
                                    <button class="btn btn-sm btn-outline-info template-btn" onclick="useTemplate('Tìm cơ hội tăng thu nhập')">
                                        📈 Tăng thu nhập
                                    </button>
                                </div>
                            </div>

                            <p class="mt-3">💡 <strong>Gợi ý</strong>: Bạn có thể hỏi bất kỳ điều gì về tài chính hoặc sử dụng các template trên!</p>
                        </div>
                    </div>
                </div>
            `;
    
    chatContainer.innerHTML = welcomeMessage;
}

// Use template message
function useTemplate(message) {
    document.getElementById('message-input').value = message;
    document.getElementById('message-input').focus();
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
            throw new Error('Chưa đăng nhập');
        }

        // Kiểm tra xem có phải yêu cầu xuất báo cáo không
        if (isReportRequest(message)) {
            await sendReportRequest(message, token);
        } else {
            await sendChatRequest(message, token);
        }
        
    } catch (error) {
        console.error('Error sending message:', error);
        appendMessage('ai', '❌ Xin lỗi, đã có lỗi xảy ra: ' + error.message);
    } finally {
        // Hide typing indicator and re-enable send button
        hideTypingIndicator();
        sendButton.disabled = false;
        isProcessing = false;
        
        // Update stats
        const endTime = Date.now();
        const responseTime = endTime - startTime;
        updateStats(responseTime);
        
        // Scroll to bottom
        scrollToBottom();
    }
}

// Check if message is a report request
function isReportRequest(message) {
    const reportKeywords = ['báo cáo', 'report', 'thống kê', 'tổng hợp', 'xuất', 'tạo báo cáo', 'export'];
    return reportKeywords.some(keyword => message.toLowerCase().includes(keyword));
}

// Send report request
async function sendReportRequest(message, token) {
    try {
        const response = await fetch('/api/ai/export-report', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify({ message: message })
        });
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        const reportContent = await response.text();
        appendMessage('ai', reportContent);
        
        // Add export options if it's a report
        if (reportContent.includes('BÁO CÁO ĐÃ ĐƯỢC TẠO')) {
            addExportOptions(message);
        }
        
    } catch (error) {
        console.error('Error sending report request:', error);
        appendMessage('ai', '❌ Lỗi khi tạo báo cáo: ' + error.message);
    }
}

// Send regular chat request
async function sendChatRequest(message, token) {
    try {
        const response = await fetch('/api/ai/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify({ message: message })
        });
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }
        
        const data = await response.json();
        appendMessage('ai', data.answer);
        
    } catch (error) {
        console.error('Error sending chat request:', error);
        appendMessage('ai', '❌ Lỗi khi gửi tin nhắn: ' + error.message);
    }
}

// Add export options after report
function addExportOptions(message) {
    const chatContainer = document.getElementById('chat-container');
    if (!chatContainer) {
        console.error('Chat container not found');
        return;
    }
    
    const lastMessage = chatContainer.lastElementChild;
    if (lastMessage && lastMessage.classList.contains('ai-message')) {
        const exportDiv = document.createElement('div');
        exportDiv.className = 'export-options mt-3';
        exportDiv.innerHTML = `
            <div class="d-flex gap-2 flex-wrap">
                <button class="btn btn-sm btn-success" onclick="copyReport()">
                    📋 Copy báo cáo
                </button>
                <button class="btn btn-sm btn-primary" onclick="downloadAsText()">
                    📄 Tải về (.txt)
                </button>
                <button class="btn btn-sm btn-warning" onclick="printReport()">
                    🖨️ In báo cáo
                </button>
            </div>
        `;

        const messageText = lastMessage.querySelector('.message-text');
        if (messageText) {
            messageText.appendChild(exportDiv);
        }
    }
}

// Copy report to clipboard
function copyReport() {
    const lastAiMessage = document.querySelector('.ai-message:last-child .message-text');
    if (lastAiMessage) {
        const textToCopy = lastAiMessage.textContent || lastAiMessage.innerText;
        navigator.clipboard.writeText(textToCopy).then(() => {
            showToast('✅ Đã copy báo cáo vào clipboard!', 'success');
        }).catch(() => {
            showToast('❌ Không thể copy báo cáo', 'error');
        });
    }
}

// Download report as text file
function downloadAsText() {
    const lastAiMessage = document.querySelector('.ai-message:last-child .message-text');
    if (lastAiMessage) {
        const textContent = lastAiMessage.textContent || lastAiMessage.innerText;
        const blob = new Blob([textContent], { type: 'text/plain;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `bao-cao-tai-chinh-${new Date().toISOString().split('T')[0]}.txt`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        showToast('📄 Đã tải báo cáo về máy!', 'success');
    }
}

// Print report
function printReport() {
    const lastAiMessage = document.querySelector('.ai-message:last-child .message-text');
    if (lastAiMessage) {
        const printWindow = window.open('', '_blank');
        printWindow.document.write(`
            <html>
                <head>
                    <title>Báo cáo tài chính</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 20px; }
                        .header { text-align: center; margin-bottom: 30px; }
                        .content { white-space: pre-wrap; }
                    </style>
                </head>
                <body>
                    <div class="header">
                        <h1>📊 Báo cáo tài chính</h1>
                        <p>Ngày tạo: ${new Date().toLocaleDateString('vi-VN')}</p>
                    </div>
                    <div class="content">${lastAiMessage.textContent || lastAiMessage.innerText}</div>
                </body>
            </html>
        `);
        printWindow.document.close();
        printWindow.print();
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
function updateStats(responseTime) {
    const responseTimeElement = document.getElementById('response-time');
    if (responseTimeElement) {
        responseTimeElement.textContent = `${responseTime}ms`;
    }
}

// Show toast notification
function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = message;
    toast.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: ${type === 'success' ? '#10b981' : type === 'error' ? '#ef4444' : '#3b82f6'};
        color: white;
        padding: 12px 20px;
        border-radius: 8px;
        z-index: 1000;
        font-size: 14px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    `;
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.remove();
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
