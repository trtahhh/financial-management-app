// Chat functionality for Finance AI
let chatHistory = [];
let isProcessing = false;
let chatCount = 0;

// Initialize chat when page loads
document.addEventListener('DOMContentLoaded', function() {
    console.log('Initializing Finance AI Chat...');
    try {
        initializeChat();
        console.log('Chat initialized successfully');
    } catch (error) {
        console.error('Error initializing chat:', error);
        showToast('Lỗi khởi tạo chat', 'error');
    }
});

function initializeChat() {
    loadAIStatus();
    setupEventListeners();
    setupExportForm();
    setupAnalysisButtons();
    setupMobileToggle();
    loadChatHistory();
}

function setupEventListeners() {
    const messageInput = document.getElementById('message-input');
    const sendButton = document.getElementById('send-button');

    if (messageInput && sendButton) {
        // Auto-resize textarea
        messageInput.addEventListener('input', function() {
            this.style.height = 'auto';
            this.style.height = Math.min(this.scrollHeight, 120) + 'px';
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
        
        // Focus input on page load
        messageInput.focus();
        
        console.log('Event listeners setup successfully');
    } else {
        console.error('Message input or send button not found');
    }
}

function setupExportForm() {
    console.log('Setting up export form...');
    const exportForm = document.getElementById('export-form');
    if (exportForm) {
        exportForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            console.log('Export form submitted');
            
            const format = document.getElementById('export-modal').dataset.format;
            const startDate = document.getElementById('start-date').value;
            const endDate = document.getElementById('end-date').value;
            
            if (!startDate || !endDate) {
                showToast('Vui lòng chọn đầy đủ ngày bắt đầu và kết thúc', 'error');
                return;
            }
            
            if (new Date(startDate) > new Date(endDate)) {
                showToast('Ngày bắt đầu không thể lớn hơn ngày kết thúc', 'error');
                return;
            }
            
            // Close modal
            closeExportModal();
            
            // Show loading
            showToast(`Đang xuất file ${format.toUpperCase()}...`, 'info');
            
            // Perform export
            await performExport(format, startDate, endDate);
        });
        console.log('Export form setup successfully');
    } else {
        console.error('Export form not found');
    }
}

function setupAnalysisButtons() {
    const analysisButtons = document.querySelectorAll('.analysis-btn');
    analysisButtons.forEach(button => {
        button.addEventListener('click', function() {
            const analysisType = this.dataset.type;
            performAnalysis(analysisType);
        });
    });
}

function setupMobileToggle() {
    const mobileToggle = document.querySelector('.mobile-toggle');
    if (mobileToggle) {
        mobileToggle.addEventListener('click', toggleSidebar);
    }
}

function toggleSidebar() {
    const sidebar = document.getElementById('chat-sidebar');
    if (sidebar) {
        sidebar.classList.toggle('show');
    }
}

async function loadAIStatus() {
    try {
        const response = await fetch('/api/ai/status', {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
                'Content-Type': 'application/json'
            }
        });
        
        if (response.ok) {
            const status = await response.json();
            updateAIStatus(status.available);
        } else {
            updateAIStatus(false);
        }
    } catch (error) {
        console.error('Error loading AI status:', error);
        updateAIStatus(false);
    }
}

function updateAIStatus(available) {
    const statusElement = document.getElementById('ai-status');
    if (statusElement) {
        if (available) {
            statusElement.innerHTML = '<span class="status-online">🟢 AI Online</span>';
            statusElement.className = 'ai-status online';
        } else {
            statusElement.innerHTML = '<span class="status-offline">🔴 AI Offline</span>';
            statusElement.className = 'ai-status offline';
        }
    }
}

async function sendMessage() {
    const messageInput = document.getElementById('message-input');
    const message = messageInput.value.trim();
    
    if (!message || isProcessing) return;
    
    // Add user message to chat
    addMessageToChat('user', message);
    messageInput.value = '';
    messageInput.style.height = 'auto';
    
    // Show typing indicator
    showTypingIndicator();
    isProcessing = true;
    
    try {
        const response = await fetch('/api/ai/chat', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ message: message })
        });
        
        if (response.ok) {
            const data = await response.json();
            addMessageToChat('ai', data.answer);
        } else {
            const errorText = await response.text();
            addMessageToChat('ai', `❌ **Lỗi**: ${errorText}`);
        }
    } catch (error) {
        console.error('Error sending message:', error);
        addMessageToChat('ai', '❌ **Đã có lỗi xảy ra**. Vui lòng thử lại sau hoặc kiểm tra kết nối mạng.');
    } finally {
        hideTypingIndicator();
        isProcessing = false;
    }
}

function addMessageToChat(sender, content) {
    const chatMessages = document.getElementById('chat-messages');
    if (!chatMessages) return;
    
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender}-message`;
    
    const avatar = document.createElement('div');
    avatar.className = `message-avatar ${sender === 'user' ? 'user-avatar' : 'ai-avatar'}`;
    avatar.innerHTML = sender === 'user' ? '👤' : '🤖';
    
    const messageContent = document.createElement('div');
    messageContent.className = 'message-content';
    messageContent.innerHTML = formatMessageContent(content);
    
    messageDiv.appendChild(avatar);
    messageDiv.appendChild(messageContent);
    
    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
    
    // Add to history
    chatHistory.push({ sender, content, timestamp: new Date() });
    chatCount++;
    
    // Save to localStorage
    saveChatHistory();
}

function formatMessageContent(content) {
    // Convert **text** to <strong>text</strong>
    content = content.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    
    // Convert ```text``` to <pre><code>text</code></pre>
    content = content.replace(/```(.*?)```/gs, '<pre><code>$1</code></pre>');
    
    // Format tables and structured data
    content = formatStructuredData(content);
    
    // Convert line breaks to <br>
    content = content.replace(/\n/g, '<br>');
    
    // Convert URLs to clickable links
    content = content.replace(/(https?:\/\/[^\s]+)/g, '<a href="$1" target="_blank" style="color: #42b883; text-decoration: underline;">$1</a>');
    
    return content;
}

function formatStructuredData(content) {
    // Format financial reports with better structure
    if (content.includes('TỔNG KẾT') || content.includes('CHI TIÊU DANH MỤC') || content.includes('GIAO DỊCH GẦN ĐÂY')) {
        return formatFinancialReport(content);
    }
    
    // Format other structured content
    content = content.replace(/=+/g, '<hr style="border: 1px solid #e0e0e0; margin: 15px 0;">');
    
    // Format bullet points
    content = content.replace(/• (.+)/g, '<div style="margin: 5px 0; padding-left: 20px;">• $1</div>');
    
    // Format numbered lists
    content = content.replace(/(\d+)\. (.+)/g, '<div style="margin: 5px 0; padding-left: 20px; font-weight: 500;">$1. $2</div>');
    
    return content;
}

function formatFinancialReport(content) {
    // Split content into sections
    const sections = content.split(/={20,}/);
    let formattedContent = '';
    
    sections.forEach((section, index) => {
        if (section.trim()) {
            if (index > 0) {
                formattedContent += '<div style="margin: 20px 0; padding: 15px; background: #f8f9fa; border-left: 4px solid #42b883; border-radius: 5px;">';
            }
            
            // Format section content
            let sectionContent = section.trim();
            
            // Format headers
            if (sectionContent.includes('TỔNG KẾT') || sectionContent.includes('CHI TIÊU') || sectionContent.includes('GIAO DỊCH')) {
                const lines = sectionContent.split('\n');
                const header = lines[0];
                const body = lines.slice(1).join('\n');
                
                sectionContent = `<h4 style="color: #42b883; margin-bottom: 15px; border-bottom: 2px solid #42b883; padding-bottom: 5px;">${header}</h4>${body}`;
            }
            
            // Format financial data
            sectionContent = sectionContent.replace(/• Tổng thu: (.+)/g, '<div style="margin: 8px 0; padding: 8px; background: #e8f5e9; border-radius: 5px;"><strong style="color: #2e7d32;">💰 Tổng thu: $1</strong></div>');
            sectionContent = sectionContent.replace(/• Tổng chi: (.+)/g, '<div style="margin: 8px 0; padding: 8px; background: #ffebee; border-radius: 5px;"><strong style="color: #c62828;">💸 Tổng chi: $1</strong></div>');
            sectionContent = sectionContent.replace(/• Số dư: (.+)/g, '<div style="margin: 8px 0; padding: 8px; background: #e3f2fd; border-radius: 5px;"><strong style="color: #1565c0;">🏦 Số dư: $1</strong></div>');
            
            // Format categories
            sectionContent = sectionContent.replace(/(\d+)\. (.+): (.+) VND \((.+)%\)/g, 
                '<div style="margin: 5px 0; padding: 10px; background: white; border: 1px solid #e0e0e0; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">' +
                '<div style="display: flex; justify-content: space-between; align-items: center;">' +
                '<span style="font-weight: 500; color: #333;">$2</span>' +
                '<span style="color: #666;">$3 VND</span>' +
                '</div>' +
                '<div style="margin-top: 5px; background: #f0f0f0; border-radius: 10px; height: 6px; overflow: hidden;">' +
                '<div style="width: $4%; height: 100%; background: linear-gradient(90deg, #42b883, #347474);"></div>' +
                '</div>' +
                '<small style="color: #888;">$4%</small>' +
                '</div>'
            );
            
            // Format transactions
            sectionContent = sectionContent.replace(/- (.+) - (.+) VND - (.+)/g, 
                '<div style="margin: 5px 0; padding: 8px; background: white; border-left: 3px solid #42b883; border-radius: 0 5px 5px 0;">' +
                '<div style="font-weight: 500; color: #333;">$1</div>' +
                '<div style="display: flex; justify-content: space-between; margin-top: 3px;">' +
                '<span style="color: #666; font-size: 0.9em;">$3</span>' +
                '<span style="font-weight: 500; color: #42b883;">$2 VND</span>' +
                '</div>' +
                '</div>'
            );
            
            formattedContent += sectionContent;
            
            if (index > 0) {
                formattedContent += '</div>';
            }
        }
    });
    
    return formattedContent || content;
}

function showTypingIndicator() {
    const chatMessages = document.getElementById('chat-messages');
    if (!chatMessages) return;
    
    const typingDiv = document.createElement('div');
    typingDiv.className = 'message ai-message typing-indicator';
    typingDiv.id = 'typing-indicator';
    
    typingDiv.innerHTML = `
        <div class="message-avatar ai-avatar">🤖</div>
        <div class="message-content">
            <div class="typing-dots">
                <div class="typing-dot"></div>
                <div class="typing-dot"></div>
                <div class="typing-dot"></div>
            </div>
        </div>
    `;
    
    chatMessages.appendChild(typingDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function hideTypingIndicator() {
    const typingIndicator = document.getElementById('typing-indicator');
    if (typingIndicator) {
        typingIndicator.remove();
    }
}

function showExportModal(format) {
    const modal = document.getElementById('export-modal');
    if (!modal) return;
    
    modal.dataset.format = format;
    
    const title = document.getElementById('export-modal-title');
    const subtitle = document.getElementById('export-modal-subtitle');
    
    if (format === 'excel') {
        title.textContent = '📊 Xuất báo cáo Excel';
        subtitle.textContent = 'Chọn khoảng thời gian để xuất báo cáo Excel';
    } else if (format === 'pdf') {
        title.textContent = '📄 Xuất báo cáo PDF';
        subtitle.textContent = 'Chọn khoảng thời gian để xuất báo cáo PDF';
    }
    
    // Set default dates (current month)
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    
    document.getElementById('start-date').value = firstDay.toISOString().split('T')[0];
    document.getElementById('end-date').value = lastDay.toISOString().split('T')[0];
    
    modal.style.display = 'flex';
}

function closeExportModal() {
    const modal = document.getElementById('export-modal');
    if (modal) {
        modal.style.display = 'none';
    }
}

function setQuickDate(type) {
    const now = new Date();
    let startDate, endDate;
    
    switch (type) {
        case 'today':
            startDate = endDate = new Date(now);
            break;
        case 'yesterday':
            startDate = endDate = new Date(now.getTime() - 24 * 60 * 60 * 1000);
            break;
        case 'thisWeek':
            const dayOfWeek = now.getDay();
            const diff = now.getDate() - dayOfWeek + (dayOfWeek === 0 ? -6 : 1);
            startDate = new Date(now.getFullYear(), now.getMonth(), diff);
            endDate = new Date(startDate.getTime() + 6 * 24 * 60 * 60 * 1000);
            break;
        case 'lastWeek':
            const lastWeekStart = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
            const dayOfLastWeek = lastWeekStart.getDay();
            const diffLastWeek = lastWeekStart.getDate() - dayOfLastWeek + (dayOfLastWeek === 0 ? -6 : 1);
            startDate = new Date(lastWeekStart.getFullYear(), lastWeekStart.getMonth(), diffLastWeek);
            endDate = new Date(startDate.getTime() + 6 * 24 * 60 * 60 * 1000);
            break;
        case 'thisMonth':
            startDate = new Date(now.getFullYear(), now.getMonth(), 1);
            endDate = new Date(now.getFullYear(), now.getMonth() + 1, 0);
            break;
        case 'lastMonth':
            startDate = new Date(now.getFullYear(), now.getMonth() - 1, 1);
            endDate = new Date(now.getFullYear(), now.getMonth(), 0);
            break;
        case 'thisYear':
            startDate = new Date(now.getFullYear(), 0, 1);
            endDate = new Date(now.getFullYear(), 11, 31);
            break;
        case 'lastYear':
            startDate = new Date(now.getFullYear() - 1, 0, 1);
            endDate = new Date(now.getFullYear() - 1, 11, 31);
            break;
    }
    
    if (startDate && endDate) {
        document.getElementById('start-date').value = startDate.toISOString().split('T')[0];
        document.getElementById('end-date').value = endDate.toISOString().split('T')[0];
    }
}

async function performExport(format, startDate, endDate) {
    try {
        const endpoint = format === 'excel' ? '/api/ai/export-excel' : '/api/ai/export-pdf';
        
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ startDate, endDate })
        });
        
        if (response.ok) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `bao_cao_tai_chinh_${startDate}_${endDate}.${format === 'excel' ? 'xlsx' : 'pdf'}`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            document.body.removeChild(a);
            
            showToast(`✅ Xuất file ${format.toUpperCase()} thành công!`, 'success');
            
            // Add success message to chat
            addMessageToChat('ai', `✅ **File ${format.toUpperCase()} đã được xuất thành công!**\n\n📅 **Thời gian**: ${startDate} → ${endDate}\n📁 **Tên file**: bao_cao_tai_chinh_${startDate}_${endDate}.${format === 'excel' ? 'xlsx' : 'pdf'}\n\nFile đã được tải về máy của bạn.`);
        } else {
            const errorText = await response.text();
            showToast(`❌ Lỗi xuất file: ${errorText}`, 'error');
            addMessageToChat('ai', `❌ **Lỗi xuất file ${format.toUpperCase()}**: ${errorText}`);
        }
    } catch (error) {
        console.error('Export error:', error);
        showToast(`❌ Lỗi xuất file: ${error.message}`, 'error');
        addMessageToChat('ai', `❌ **Lỗi xuất file ${format.toUpperCase()}**: ${error.message}`);
    }
}

async function performAnalysis(analysisType) {
    try {
        showToast(`🔍 Đang phân tích tài chính...`, 'info');
        
        const response = await fetch('/api/ai/analyze', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ type: analysisType })
        });
        
        if (response.ok) {
            const result = await response.json();
            
            if (result.error) {
                showToast(`❌ Lỗi phân tích: ${result.error}`, 'error');
                addMessageToChat('ai', `❌ **Lỗi phân tích**: ${result.error}`);
            } else {
                showToast(`✅ Phân tích hoàn thành!`, 'success');
                
                let analysisContent = '';
                switch (analysisType) {
                    case 'comprehensive':
                        analysisContent = `📊 **PHÂN TÍCH TÀI CHÍNH TOÀN DIỆN**\n\n${result.analysis}`;
                        break;
                    case 'prediction':
                        analysisContent = `🔮 **DỰ BÁO TÀI CHÍNH TƯƠNG LAI**\n\n${result.prediction}`;
                        break;
                    case 'trend':
                        analysisContent = `📈 **PHÂN TÍCH XU HƯỚNG CHI TIÊU**\n\n${result.trends}`;
                        break;
                    case 'budget':
                        analysisContent = `💰 **TỐI ƯU HÓA NGÂN SÁCH**\n\n${result.optimization}`;
                        break;
                    case 'risk':
                        analysisContent = `⚠️ **ĐÁNH GIÁ RỦI RO TÀI CHÍNH**\n\n${result.risk}`;
                        break;
                    case 'investment':
                        analysisContent = `💎 **LỜI KHUYÊN ĐẦU TƯ**\n\n${result.advice}`;
                        break;
                }
                
                addMessageToChat('ai', analysisContent);
            }
        } else {
            const errorText = await response.text();
            showToast(`❌ Lỗi phân tích: ${errorText}`, 'error');
            addMessageToChat('ai', `❌ **Lỗi phân tích**: ${errorText}`);
        }
    } catch (error) {
        console.error('Analysis error:', error);
        showToast(`❌ Lỗi phân tích: ${error.message}`, 'error');
        addMessageToChat('ai', `❌ **Lỗi phân tích**: ${error.message}`);
    }
}

function useTemplate(template) {
    const messageInput = document.getElementById('message-input');
    if (messageInput) {
        messageInput.value = template;
        messageInput.focus();
        messageInput.style.height = 'auto';
        messageInput.style.height = Math.min(messageInput.scrollHeight, 120) + 'px';
    }
}

function sendSuggestion(suggestion) {
    const messageInput = document.getElementById('message-input');
    if (messageInput) {
        messageInput.value = suggestion;
        sendMessage();
    }
}

function showToast(message, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.classList.add('show');
    }, 100);
    
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.remove();
            }
        }, 300);
    }, 4000);
}

function clearChat() {
    if (confirm('Bạn có chắc muốn xóa toàn bộ cuộc trò chuyện?')) {
        const chatMessages = document.getElementById('chat-messages');
        if (chatMessages) {
            chatMessages.innerHTML = `
                <div class="welcome-message">
                    <h1>Chào mừng đến với AI Trợ lý Thông minh! 🚀</h1>
                    <p>Tôi có thể trả lời <strong>tất cả mọi câu hỏi</strong> của bạn, bao gồm tài chính, đầu tư, tiết kiệm, quản lý ngân sách và nhiều chủ đề khác!</p>
                    
                    <div class="report-templates">
                        <h6><strong>📊 Báo cáo tài chính</strong></h6>
                        <div class="template-buttons">
                            <button class="template-btn primary" onclick="useTemplate('Tạo báo cáo tổng hợp tháng này')">
                                📈 Báo cáo tổng hợp
                            </button>
                            <button class="template-btn success" onclick="useTemplate('Báo cáo giao dịch tháng này')">
                                💰 Báo cáo giao dịch
                            </button>
                            <button class="template-btn info" onclick="useTemplate('Báo cáo ngân sách tháng này')">
                                📋 Báo cáo ngân sách
                            </button>
                        </div>
                        
                        <h6><strong>💡 Ví dụ câu hỏi</strong></h6>
                        <div class="template-buttons">
                            <button class="template-btn" onclick="sendSuggestion('Làm thế nào để tiết kiệm tiền hiệu quả?')">
                                💰 Tiết kiệm thông minh
                            </button>
                            <button class="template-btn" onclick="sendSuggestion('Tôi nên đầu tư như thế nào?')">
                                📈 Hướng dẫn đầu tư
                            </button>
                            <button class="template-btn" onclick="sendSuggestion('Cách lập ngân sách gia đình?')">
                                🏠 Ngân sách gia đình
                            </button>
                            <button class="template-btn" onclick="sendSuggestion('Quản lý nợ thông minh ra sao?')">
                                💳 Quản lý nợ
                            </button>
                        </div>
                    </div>
                </div>
            `;
        }
        
        // Reset chat history
        chatHistory = [];
        chatCount = 0;
        
        // Clear localStorage
        localStorage.removeItem('chatHistory');
        
        showToast('Đã xóa cuộc trò chuyện', 'success');
    }
}

function newChat() {
    clearChat();
    showToast('Cuộc trò chuyện mới đã được tạo', 'success');
}

function saveChatHistory() {
    try {
        localStorage.setItem('chatHistory', JSON.stringify(chatHistory));
    } catch (error) {
        console.error('Error saving chat history:', error);
    }
}

function loadChatHistory() {
    try {
        const saved = localStorage.getItem('chatHistory');
        if (saved) {
            chatHistory = JSON.parse(saved);
            chatCount = chatHistory.length;
            
            // Restore messages
            chatHistory.forEach(item => {
                addMessageToChat(item.sender, item.content);
            });
        }
    } catch (error) {
        console.error('Error loading chat history:', error);
        localStorage.removeItem('chatHistory');
    }
}

// Close modal when clicking outside
document.addEventListener('click', function(e) {
    const modal = document.getElementById('export-modal');
    if (modal && e.target === modal) {
        closeExportModal();
    }
});

// Close modal on Escape key
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeExportModal();
    }
});

console.log('Finance AI Chat loaded successfully!');
