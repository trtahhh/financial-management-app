// AI-Powered Transaction Assistant
import localAIEngine from '../ai/LocalAIEngine.js';

class AITransactionAssistant {
  constructor(container, transactionService, options = {}) {
    this.container = container;
    this.transactionService = transactionService;
    this.options = {
      enableVoice: true,
      enableCamera: true,
      enableSuggestions: true,
      autoCategory: true,
      smartAmounts: true,
      ...options
    };

    this.isRecording = false;
    this.recognition = null;
    this.currentSuggestions = [];
    
    this.init();
  }

  async init() {
    await this.waitForAI();
    this.render();
    this.attachEventListeners();
    this.initializeVoice();
    this.loadRecentTransactions();
  }

  async waitForAI() {
    // Wait for AI engine to initialize
    let attempts = 0;
    while (!localAIEngine.getStatus().initialized && attempts < 30) {
      await new Promise(resolve => setTimeout(resolve, 100));
      attempts++;
    }
  }

  render() {
    this.container.innerHTML = `
      <div class="ai-transaction-assistant">
        <div class="assistant-header">
          <div class="header-info">
            <h2> AI Transaction Assistant</h2>
            <p>Thêm giao dịch thông minh với AI</p>
          </div>
          <div class="ai-status" id="ai-status">
            <span class="status-indicator ${localAIEngine.getStatus().initialized ? 'active' : 'loading'}"></span>
            <span class="status-text">${localAIEngine.getStatus().initialized ? 'AI Ready' : 'Loading...'}</span>
          </div>
        </div>

        <div class="input-methods">
          ${this.renderTextInput()}
          ${this.options.enableVoice ? this.renderVoiceInput() : ''}
          ${this.options.enableCamera ? this.renderReceiptScan() : ''}
        </div>

        <div class="ai-suggestions" id="ai-suggestions">
          <!-- AI suggestions will appear here -->
        </div>

        <div class="transaction-form" id="transaction-form">
          ${this.renderTransactionForm()}
        </div>

        <div class="quick-actions">
          ${this.renderQuickActions()}
        </div>

        <div class="recent-insights" id="recent-insights">
          ${this.renderInsights()}
        </div>
      </div>
    `;

    this.renderStyles();
  }

  renderTextInput() {
    return `
      <div class="input-method text-input">
        <div class="input-header">
          <h3> Mô tả giao dịch</h3>
          <div class="input-status">
            <span class="typing-indicator" id="typing-indicator">AI đang phân tích...</span>
          </div>
        </div>
        
        <div class="smart-input-container">
          <textarea 
            id="transaction-description" 
            placeholder="VD: Ăn trưa ở quán cơm 45k, Mua sách 150 nghìn đồng..."
            rows="2"
          ></textarea>
          
          <div class="input-tools">
            <button class="btn-smart-suggest" id="btn-smart-suggest" disabled>
              <span class="btn-icon"></span>
              <span class="btn-text">Phân tích AI</span>
            </button>
            
            <div class="confidence-meter" id="confidence-meter" style="display: none;">
              <span class="confidence-label">Độ tin cậy:</span>
              <div class="confidence-bar">
                <div class="confidence-fill" style="width: 0%"></div>
              </div>
              <span class="confidence-value">0%</span>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  renderVoiceInput() {
    return `
      <div class="input-method voice-input">
        <div class="input-header">
          <h3> Nhập bằng giọng nói</h3>
          <div class="voice-status">
            <span class="status-text" id="voice-status">Sẵn sàng</span>
          </div>
        </div>
        
        <div class="voice-controls">
          <button class="btn-voice-record" id="btn-voice-record">
            <div class="voice-icon">
              <div class="microphone-icon"></div>
            </div>
            <span class="voice-text">Nhấn để nói</span>
          </button>
          
          <div class="voice-visualization" id="voice-visualization" style="display: none;">
            <div class="wave-container">
              <div class="wave"></div>
              <div class="wave"></div>
              <div class="wave"></div>
              <div class="wave"></div>
              <div class="wave"></div>
            </div>
          </div>
        </div>
        
        <div class="voice-transcript" id="voice-transcript" style="display: none;">
          <div class="transcript-content"></div>
          <div class="transcript-actions">
            <button class="btn-voice-accept" id="btn-voice-accept"> Xác nhận</button>
            <button class="btn-voice-retry" id="btn-voice-retry"> Thử lại</button>
          </div>
        </div>
      </div>
    `;
  }

  renderReceiptScan() {
    return `
      <div class="input-method receipt-scan">
        <div class="input-header">
          <h3> Quét hóa đơn</h3>
          <div class="scan-status">
            <span class="status-text" id="scan-status">Sẵn sàng quét</span>
          </div>
        </div>
        
        <div class="scan-controls">
          <div class="upload-area" id="upload-area">
            <div class="upload-content">
              <div class="upload-icon"></div>
              <div class="upload-text">
                <p><strong>Chụp hoặc chọn ảnh hóa đơn</strong></p>
                <p>Hỗ trợ JPG, PNG</p>
              </div>
            </div>
            <input type="file" id="receipt-input" accept="image/*" capture="environment">
          </div>
          
          <div class="scan-preview" id="scan-preview" style="display: none;">
            <img id="receipt-image" alt="Receipt">
            <div class="preview-overlay">
              <div class="scan-progress">
                <div class="progress-spinner"></div>
                <span>AI đang xử lý...</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  renderTransactionForm() {
    return `
      <div class="form-container">
        <div class="form-grid">
          <div class="form-group">
            <label for="amount">Số tiền</label>
            <div class="amount-input-container">
              <input type="number" id="amount" placeholder="0">
              <span class="currency">VND</span>
              <div class="smart-amounts" id="smart-amounts" style="display: none;">
                <!-- AI suggested amounts -->
              </div>
            </div>
          </div>
          
          <div class="form-group">
            <label for="category">Danh mục</label>
            <div class="category-container">
              <select id="category">
                <option value="">Chọn danh mục...</option>
              </select>
              <div class="category-suggestions" id="category-suggestions">
                <!-- AI category suggestions -->
              </div>
            </div>
          </div>
          
          <div class="form-group">
            <label for="transaction-type">Loại</label>
            <div class="type-selector">
              <button type="button" class="type-btn active" data-type="expense">
                <span class="type-icon"></span>
                <span class="type-text">Chi tiêu</span>
              </button>
              <button type="button" class="type-btn" data-type="income">
                <span class="type-icon"></span>
                <span class="type-text">Thu nhập</span>
              </button>
            </div>
          </div>
          
          <div class="form-group full-width">
            <label for="final-description">Mô tả chi tiết</label>
            <input type="text" id="final-description" placeholder="Mô tả giao dịch...">
          </div>
          
          <div class="form-group full-width">
            <label for="date">Ngày giao dịch</label>
            <input type="datetime-local" id="date">
          </div>
        </div>
        
        <div class="ai-insights-form" id="ai-insights-form">
          <!-- AI insights about this transaction -->
        </div>
        
        <div class="form-actions">
          <button class="btn-save" id="btn-save-transaction" disabled>
            <span class="btn-icon"></span>
            <span class="btn-text">Lưu giao dịch</span>
          </button>
          
          <button class="btn-save-template" id="btn-save-template" style="display: none;">
            <span class="btn-icon"></span>
            <span class="btn-text">Lưu mẫu</span>
          </button>
          
          <button class="btn-clear" id="btn-clear-form">
            <span class="btn-icon"></span>
            <span class="btn-text">Xóa form</span>
          </button>
        </div>
      </div>
    `;
  }

  renderQuickActions() {
    return `
      <div class="quick-actions-container">
        <h4> Thao tác nhanh</h4>
        <div class="quick-buttons">
          <button class="quick-btn" data-action="coffee">
            <span class="quick-icon"></span>
            <span class="quick-text">Cà phê</span>
            <span class="quick-amount">25k</span>
          </button>
          
          <button class="quick-btn" data-action="lunch">
            <span class="quick-icon"></span>
            <span class="quick-text">Ăn trưa</span>
            <span class="quick-amount">50k</span>
          </button>
          
          <button class="quick-btn" data-action="transport">
            <span class="quick-icon"></span>
            <span class="quick-text">Đi lại</span>
            <span class="quick-amount">30k</span>
          </button>
          
          <button class="quick-btn" data-action="shopping">
            <span class="quick-icon"></span>
            <span class="quick-text">Mua sắm</span>
            <span class="quick-amount">?</span>
          </button>
          
          <button class="quick-btn" data-action="custom" id="btn-add-quick">
            <span class="quick-icon"></span>
            <span class="quick-text">Tùy chỉnh</span>
          </button>
        </div>
      </div>
    `;
  }

  renderInsights() {
    return `
      <div class="insights-container">
        <h4> Thông tin thông minh</h4>
        <div class="insights-content" id="insights-content">
          <div class="insight-placeholder">
            <div class="placeholder-icon"></div>
            <div class="placeholder-text">
              <p>AI sẽ cung cấp thông tin về chi tiêu của bạn</p>
              <p>Thêm vài giao dịch để bắt đầu!</p>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  attachEventListeners() {
    // Text input analysis
    const descriptionInput = document.getElementById('transaction-description');
    const smartSuggestBtn = document.getElementById('btn-smart-suggest');
    
    let typingTimer;
    descriptionInput.addEventListener('input', (e) => {
      clearTimeout(typingTimer);
      const value = e.target.value.trim();
      
      smartSuggestBtn.disabled = value.length < 3;
      
      if (value.length > 3) {
        this.showTypingIndicator(true);
        
        typingTimer = setTimeout(async () => {
          await this.analyzeTextInput(value);
          this.showTypingIndicator(false);
        }, 1000);
      } else {
        this.showTypingIndicator(false);
        this.clearSuggestions();
      }
    });
    
    smartSuggestBtn.addEventListener('click', () => {
      this.analyzeTextInput(descriptionInput.value);
    });
    
    // Voice input
    if (this.options.enableVoice) {
      document.getElementById('btn-voice-record')?.addEventListener('click', () => {
        this.toggleVoiceRecording();
      });
      
      document.getElementById('btn-voice-accept')?.addEventListener('click', () => {
        this.acceptVoiceInput();
      });
      
      document.getElementById('btn-voice-retry')?.addEventListener('click', () => {
        this.retryVoiceInput();
      });
    }
    
    // Receipt scanning
    if (this.options.enableCamera) {
      const receiptInput = document.getElementById('receipt-input');
      const uploadArea = document.getElementById('upload-area');
      
      receiptInput.addEventListener('change', (e) => {
        if (e.target.files[0]) {
          this.processReceiptImage(e.target.files[0]);
        }
      });
      
      uploadArea.addEventListener('click', () => {
        receiptInput.click();
      });
      
      // Drag & drop
      uploadArea.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadArea.classList.add('drag-over');
      });
      
      uploadArea.addEventListener('dragleave', () => {
        uploadArea.classList.remove('drag-over');
      });
      
      uploadArea.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadArea.classList.remove('drag-over');
        
        if (e.dataTransfer.files[0]) {
          this.processReceiptImage(e.dataTransfer.files[0]);
        }
      });
    }
    
    // Form interactions
    document.getElementById('amount')?.addEventListener('input', (e) => {
      this.updateSmartAmounts(parseFloat(e.target.value) || 0);
      this.validateForm();
    });
    
    document.getElementById('category')?.addEventListener('change', () => {
      this.validateForm();
    });
    
    document.querySelectorAll('.type-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        document.querySelectorAll('.type-btn').forEach(b => b.classList.remove('active'));
        e.currentTarget.classList.add('active');
        this.validateForm();
      });
    });
    
    // Quick actions
    document.querySelectorAll('.quick-btn[data-action]').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const action = e.currentTarget.dataset.action;
        this.handleQuickAction(action);
      });
    });
    
    // Form actions
    document.getElementById('btn-save-transaction')?.addEventListener('click', () => {
      this.saveTransaction();
    });
    
    document.getElementById('btn-clear-form')?.addEventListener('click', () => {
      this.clearForm();
    });
    
    // Initialize date to current time
    document.getElementById('date').value = new Date().toISOString().slice(0, 16);
  }

  showTypingIndicator(show) {
    const indicator = document.getElementById('typing-indicator');
    if (indicator) {
      indicator.style.display = show ? 'block' : 'none';
    }
  }

  async analyzeTextInput(text) {
    if (!text || text.length < 3) return;
    
    try {
      // Use AI to analyze the input
      const result = await localAIEngine.categorize(text, 0);
      
      this.updateConfidenceMeter(result.confidence);
      this.showCategorySuggestions(result);
      
      // Try to extract amount from text
      const extractedAmount = this.extractAmountFromText(text);
      if (extractedAmount > 0) {
        document.getElementById('amount').value = extractedAmount;
        this.updateSmartAmounts(extractedAmount);
      }
      
      // Update description field
      document.getElementById('final-description').value = text;
      
      // Show AI insights
      this.showAIInsights(result, text);
      
      this.validateForm();
      
    } catch (error) {
      console.error('Error analyzing text:', error);
      this.showError('Lỗi phân tích AI');
    }
  }

  updateConfidenceMeter(confidence) {
    const meter = document.getElementById('confidence-meter');
    const fill = meter?.querySelector('.confidence-fill');
    const value = meter?.querySelector('.confidence-value');
    
    if (meter && confidence !== undefined) {
      meter.style.display = 'flex';
      
      const percentage = Math.round(confidence * 100);
      fill.style.width = `${percentage}%`;
      value.textContent = `${percentage}%`;
      
      // Color coding
      fill.className = 'confidence-fill';
      if (percentage >= 80) {
        fill.classList.add('high');
      } else if (percentage >= 60) {
        fill.classList.add('medium');
      } else {
        fill.classList.add('low');
      }
    }
  }

  showCategorySuggestions(result) {
    const container = document.getElementById('category-suggestions');
    const categorySelect = document.getElementById('category');
    
    if (!container || !result.suggestions) return;
    
    // Update category select
    categorySelect.value = result.category;
    
    // Show suggestions
    container.innerHTML = result.suggestions.slice(0, 3).map(suggestion => `
      <div class="category-suggestion" data-category="${suggestion.id}">
        <div class="suggestion-info">
          <span class="suggestion-name">${suggestion.name}</span>
          <div class="suggestion-confidence">${Math.round(suggestion.confidence * 100)}%</div>
        </div>
        <button class="btn-select-category" data-category="${suggestion.id}">Chọn</button>
      </div>
    `).join('');
    
    // Add click handlers
    container.querySelectorAll('.btn-select-category').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const categoryId = e.target.dataset.category;
        categorySelect.value = categoryId;
        this.validateForm();
      });
    });
  }

  extractAmountFromText(text) {
    // Similar to AI engine but simpler
    const patterns = [
      /(\d+(?:\.\d+)?)\s*(?:nghìn|k)/gi,
      /(\d+(?:\.\d+)?)\s*(?:triệu|m)/gi,
      /(\d+(?:[\.,]\d+)*)/g
    ];
    
    let amount = 0;
    
    for (const pattern of patterns) {
      const matches = text.match(pattern);
      if (matches) {
        const numberStr = matches[0].replace(/[^\d\.,]/g, '').replace(',', '.');
        let value = parseFloat(numberStr);
        
        if (text.includes('nghìn') || text.includes('k')) {
          value *= 1000;
        } else if (text.includes('triệu') || text.includes('m')) {
          value *= 1000000;
        }
        
        amount = Math.max(amount, value);
      }
    }
    
    return amount;
  }

  showAIInsights(result, originalText) {
    const container = document.getElementById('ai-insights-form');
    if (!container) return;
    
    container.innerHTML = `
      <div class="ai-insight">
        <div class="insight-header">
          <span class="insight-icon"></span>
          <span class="insight-title">AI Analysis</span>
        </div>
        <div class="insight-content">
          <p><strong>Phân loại:</strong> ${result.categoryName}</p>
          <p><strong>Độ tin cậy:</strong> ${Math.round(result.confidence * 100)}%</p>
          ${result.reasoning ? `<p><strong>Lý do:</strong> ${result.reasoning}</p>` : ''}
        </div>
      </div>
    `;
  }

  initializeVoice() {
    if (!this.options.enableVoice || !('webkitSpeechRecognition' in window || 'SpeechRecognition' in window)) {
      return;
    }
    
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    this.recognition = new SpeechRecognition();
    
    this.recognition.continuous = false;
    this.recognition.interimResults = false;
    this.recognition.lang = 'vi-VN';
    
    this.recognition.onstart = () => {
      this.isRecording = true;
      this.updateVoiceStatus('Đang lắng nghe...');
      this.showVoiceVisualization(true);
    };
    
    this.recognition.onend = () => {
      this.isRecording = false;
      this.updateVoiceStatus('Sẵn sàng');
      this.showVoiceVisualization(false);
      this.updateVoiceButton(false);
    };
    
    this.recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript;
      this.handleVoiceResult(transcript);
    };
    
    this.recognition.onerror = (event) => {
      console.error('Voice recognition error:', event.error);
      this.updateVoiceStatus('Lỗi: ' + event.error);
      this.isRecording = false;
      this.showVoiceVisualization(false);
      this.updateVoiceButton(false);
    };
  }

  toggleVoiceRecording() {
    if (this.isRecording) {
      this.recognition?.stop();
    } else {
      this.recognition?.start();
      this.updateVoiceButton(true);
    }
  }

  updateVoiceStatus(status) {
    const statusEl = document.getElementById('voice-status');
    if (statusEl) {
      statusEl.textContent = status;
    }
  }

  showVoiceVisualization(show) {
    const visualization = document.getElementById('voice-visualization');
    if (visualization) {
      visualization.style.display = show ? 'flex' : 'none';
    }
  }

  updateVoiceButton(recording) {
    const btn = document.getElementById('btn-voice-record');
    const text = btn?.querySelector('.voice-text');
    
    if (btn) {
      btn.classList.toggle('recording', recording);
    }
    
    if (text) {
      text.textContent = recording ? 'Đang ghi...' : 'Nhấn để nói';
    }
  }

  async handleVoiceResult(transcript) {
    const transcriptEl = document.getElementById('voice-transcript');
    const content = transcriptEl?.querySelector('.transcript-content');
    
    if (content) {
      content.textContent = transcript;
      transcriptEl.style.display = 'block';
    }
    
    // Process with AI
    try {
      const result = await localAIEngine.processVoice(transcript);
      this.currentVoiceResult = result;
      
      // Update UI with voice result
      this.updateFromVoiceResult(result);
      
    } catch (error) {
      console.error('Voice processing error:', error);
      this.showError('Lỗi xử lý giọng nói');
    }
  }

  updateFromVoiceResult(result) {
    if (result.amount > 0) {
      document.getElementById('amount').value = result.amount;
    }
    
    if (result.category) {
      document.getElementById('category').value = result.category;
    }
    
    if (result.description) {
      document.getElementById('final-description').value = result.description;
    }
    
    this.validateForm();
  }

  acceptVoiceInput() {
    if (this.currentVoiceResult) {
      document.getElementById('voice-transcript').style.display = 'none';
      document.getElementById('transaction-description').value = this.currentVoiceResult.rawTranscript;
    }
  }

  retryVoiceInput() {
    document.getElementById('voice-transcript').style.display = 'none';
    this.toggleVoiceRecording();
  }

  async processReceiptImage(file) {
    const preview = document.getElementById('scan-preview');
    const image = document.getElementById('receipt-image');
    const status = document.getElementById('scan-status');
    
    // Show preview
    image.src = URL.createObjectURL(file);
    preview.style.display = 'block';
    status.textContent = 'Đang xử lý...';
    
    try {
      // Process with AI
      const result = await localAIEngine.scanReceipt(file);
      
      // Update form with extracted data
      if (result.total > 0) {
        document.getElementById('amount').value = result.total;
      }
      
      if (result.suggestedCategory) {
        document.getElementById('category').value = result.suggestedCategory;
      }
      
      if (result.extractedText) {
        document.getElementById('final-description').value = result.extractedText;
      }
      
      status.textContent = `Đã xử lý (${Math.round(result.confidence * 100)}% tin cậy)`;
      
      // Show extracted items
      if (result.items && result.items.length > 0) {
        this.showReceiptItems(result.items);
      }
      
      this.validateForm();
      
    } catch (error) {
      console.error('Receipt processing error:', error);
      status.textContent = 'Lỗi xử lý ảnh';
    }
  }

  showReceiptItems(items) {
    const container = document.getElementById('ai-insights-form');
    if (!container) return;
    
    container.innerHTML = `
      <div class="receipt-items">
        <div class="items-header">
          <span class="items-icon"></span>
          <span class="items-title">Chi tiết hóa đơn</span>
        </div>
        <div class="items-list">
          ${items.map(item => `
            <div class="receipt-item">
              <span class="item-name">${item.name}</span>
              <span class="item-price">${this.formatCurrency(item.price)}</span>
            </div>
          `).join('')}
        </div>
      </div>
    `;
  }

  updateSmartAmounts(baseAmount) {
    if (!this.options.smartAmounts || baseAmount <= 0) return;
    
    const container = document.getElementById('smart-amounts');
    if (!container) return;
    
    const suggestions = [
      baseAmount * 0.8,
      baseAmount,
      baseAmount * 1.2,
      Math.round(baseAmount / 1000) * 1000, // Round to thousands
    ].filter((amount, index, arr) => arr.indexOf(amount) === index); // Remove duplicates
    
    container.innerHTML = suggestions.map(amount => `
      <button class="smart-amount" data-amount="${amount}">
        ${this.formatCurrency(amount)}
      </button>
    `).join('');
    
    container.style.display = 'flex';
    
    // Add click handlers
    container.querySelectorAll('.smart-amount').forEach(btn => {
      btn.addEventListener('click', (e) => {
        const amount = parseFloat(e.target.dataset.amount);
        document.getElementById('amount').value = amount;
        this.validateForm();
      });
    });
  }

  handleQuickAction(action) {
    const quickActions = {
      coffee: { amount: 25000, category: 'food', description: 'Cà phê' },
      lunch: { amount: 50000, category: 'food', description: 'Ăn trưa' },
      transport: { amount: 30000, category: 'transport', description: 'Di chuyển' },
      shopping: { amount: 0, category: 'shopping', description: 'Mua sắm' }
    };
    
    const actionData = quickActions[action];
    if (actionData) {
      if (actionData.amount > 0) {
        document.getElementById('amount').value = actionData.amount;
      }
      
      document.getElementById('category').value = actionData.category;
      document.getElementById('final-description').value = actionData.description;
      
      this.validateForm();
    }
  }

  validateForm() {
    const amount = parseFloat(document.getElementById('amount')?.value) || 0;
    const category = document.getElementById('category')?.value;
    const description = document.getElementById('final-description')?.value?.trim();
    
    const isValid = amount > 0 && category && description;
    
    const saveBtn = document.getElementById('btn-save-transaction');
    if (saveBtn) {
      saveBtn.disabled = !isValid;
      saveBtn.classList.toggle('ready', isValid);
    }
    
    return isValid;
  }

  async saveTransaction() {
    if (!this.validateForm()) return;
    
    const transactionData = {
      amount: parseFloat(document.getElementById('amount').value),
      category: document.getElementById('category').value,
      description: document.getElementById('final-description').value.trim(),
      type: document.querySelector('.type-btn.active')?.dataset.type || 'expense',
      date: document.getElementById('date').value
    };
    
    // If expense, make amount negative
    if (transactionData.type === 'expense') {
      transactionData.amount = -Math.abs(transactionData.amount);
    }
    
    try {
      const result = await this.transactionService.createTransaction(transactionData);
      
      // Learn from this transaction
      await localAIEngine.learnFromTransaction(transactionData);
      
      this.showSuccess('Đã lưu giao dịch thành công!');
      this.clearForm();
      this.updateInsights();
      
      // Trigger event for other components
      window.dispatchEvent(new CustomEvent('transactionAdded', { detail: result }));
      
    } catch (error) {
      console.error('Error saving transaction:', error);
      this.showError('Lỗi khi lưu giao dịch');
    }
  }

  clearForm() {
    document.getElementById('amount').value = '';
    document.getElementById('category').value = '';
    document.getElementById('final-description').value = '';
    document.getElementById('transaction-description').value = '';
    document.getElementById('date').value = new Date().toISOString().slice(0, 16);
    
    // Clear AI suggestions
    this.clearSuggestions();
    
    // Hide smart amounts
    const smartAmounts = document.getElementById('smart-amounts');
    if (smartAmounts) {
      smartAmounts.style.display = 'none';
    }
    
    // Reset confidence meter
    const meter = document.getElementById('confidence-meter');
    if (meter) {
      meter.style.display = 'none';
    }
    
    this.validateForm();
  }

  clearSuggestions() {
    const containers = [
      'ai-suggestions',
      'category-suggestions',
      'ai-insights-form'
    ];
    
    containers.forEach(id => {
      const container = document.getElementById(id);
      if (container) {
        container.innerHTML = '';
      }
    });
  }

  async loadRecentTransactions() {
    try {
      const transactions = await this.transactionService.getTransactions({ limit: 20 });
      await this.updateInsights(transactions);
    } catch (error) {
      console.warn('Could not load recent transactions:', error);
    }
  }

  async updateInsights(transactions = null) {
    const container = document.getElementById('insights-content');
    if (!container) return;
    
    try {
      if (!transactions) {
        transactions = await this.transactionService.getTransactions({ limit: 50 });
      }
      
      const insights = await localAIEngine.getInsights(transactions);
      
      if (insights.insights.length === 0) {
        container.innerHTML = `
          <div class="insight-placeholder">
            <div class="placeholder-icon"></div>
            <div class="placeholder-text">
              <p>Chưa có đủ dữ liệu để phân tích</p>
              <p>Thêm vài giao dịch để xem insights!</p>
            </div>
          </div>
        `;
        return;
      }
      
      container.innerHTML = `
        <div class="insights-summary">
          <div class="financial-score">
            <div class="score-circle" data-score="${insights.score}">
              <span class="score-value">${insights.score}</span>
              <span class="score-label">Điểm</span>
            </div>
            <div class="score-info">
              <h5>Sức khỏe tài chính</h5>
              <p class="score-description">${this.getScoreDescription(insights.score)}</p>
            </div>
          </div>
          
          <div class="key-insights">
            ${insights.insights.slice(0, 3).map(insight => `
              <div class="insight-item priority-${insight.priority}">
                <span class="insight-icon">${insight.icon}</span>
                <div class="insight-text">
                  <h6>${insight.title}</h6>
                  <p>${insight.message}</p>
                </div>
              </div>
            `).join('')}
          </div>
          
          ${insights.recommendations.length > 0 ? `
            <div class="recommendations">
              <h6> Gợi ý cải thiện</h6>
              ${insights.recommendations.slice(0, 2).map(rec => `
                <div class="recommendation-item">
                  <span class="rec-title">${rec.title}</span>
                  <span class="rec-message">${rec.message}</span>
                </div>
              `).join('')}
            </div>
          ` : ''}
        </div>
      `;
      
    } catch (error) {
      console.error('Error updating insights:', error);
    }
  }

  getScoreDescription(score) {
    if (score >= 80) return 'Tuyệt vời! �';
    if (score >= 60) return 'Tốt! �';
    if (score >= 40) return 'Khá ổn �';
    if (score >= 20) return 'Cần cải thiện ';
    return 'Cần chú ý nhiều hơn ';
  }

  formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount);
  }

  showSuccess(message) {
    if (window.showNotification) {
      window.showNotification(message, 'success');
    }
  }

  showError(message) {
    if (window.showNotification) {
      window.showNotification(message, 'error');
    }
  }

  renderStyles() {
    if (document.getElementById('ai-assistant-styles')) return;

    const style = document.createElement('style');
    style.id = 'ai-assistant-styles';
    style.textContent = `
      .ai-transaction-assistant {
        max-width: 1200px;
        margin: 0 auto;
        padding: 2rem;
        background: #f8fafc;
        min-height: 100vh;
      }

      .assistant-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 2rem;
        padding: 1.5rem;
        background: white;
        border-radius: 12px;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
      }

      .header-info h2 {
        margin: 0 0 0.5rem 0;
        color: #1e293b;
        font-size: 1.75rem;
      }

      .header-info p {
        margin: 0;
        color: #64748b;
      }

      .ai-status {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.5rem 1rem;
        background: #f1f5f9;
        border-radius: 20px;
      }

      .status-indicator {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #ef4444;
        animation: pulse 2s infinite;
      }

      .status-indicator.active {
        background: #10b981;
        animation: none;
      }

      .input-methods {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
        gap: 1.5rem;
        margin-bottom: 2rem;
      }

      .input-method {
        background: white;
        border-radius: 12px;
        padding: 1.5rem;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
      }

      .input-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
      }

      .input-header h3 {
        margin: 0;
        color: #1e293b;
        font-size: 1.25rem;
      }

      .typing-indicator {
        font-size: 0.875rem;
        color: #3b82f6;
        display: none;
      }

      .smart-input-container textarea {
        width: 100%;
        min-height: 80px;
        padding: 1rem;
        border: 2px solid #e2e8f0;
        border-radius: 8px;
        font-size: 1rem;
        resize: vertical;
        font-family: inherit;
      }

      .smart-input-container textarea:focus {
        outline: none;
        border-color: #3b82f6;
      }

      .input-tools {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-top: 1rem;
      }

      .btn-smart-suggest {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.75rem 1rem;
        background: #3b82f6;
        color: white;
        border: none;
        border-radius: 8px;
        cursor: pointer;
        font-size: 0.875rem;
        transition: all 0.2s;
      }

      .btn-smart-suggest:disabled {
        background: #94a3b8;
        cursor: not-allowed;
      }

      .confidence-meter {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-size: 0.75rem;
      }

      .confidence-bar {
        width: 80px;
        height: 6px;
        background: #e2e8f0;
        border-radius: 3px;
        overflow: hidden;
      }

      .confidence-fill {
        height: 100%;
        background: #64748b;
        transition: width 0.3s;
      }

      .confidence-fill.high { background: #10b981; }
      .confidence-fill.medium { background: #f59e0b; }
      .confidence-fill.low { background: #ef4444; }

      /* Voice Input Styles */
      .voice-controls {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 1rem;
      }

      .btn-voice-record {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.5rem;
        padding: 1rem;
        background: #f8fafc;
        border: 2px solid #e2e8f0;
        border-radius: 12px;
        cursor: pointer;
        transition: all 0.3s;
        min-width: 120px;
      }

      .btn-voice-record:hover {
        background: #f1f5f9;
        border-color: #3b82f6;
      }

      .btn-voice-record.recording {
        background: #fef2f2;
        border-color: #ef4444;
        animation: pulse 1s infinite;
      }

      .microphone-icon {
        width: 24px;
        height: 24px;
        background: #64748b;
        border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
        position: relative;
      }

      .microphone-icon::before {
        content: '';
        position: absolute;
        bottom: -8px;
        left: 50%;
        transform: translateX(-50%);
        width: 3px;
        height: 8px;
        background: #64748b;
      }

      .voice-visualization {
        display: flex;
        align-items: center;
        gap: 4px;
        height: 40px;
      }

      .wave {
        width: 4px;
        background: #3b82f6;
        border-radius: 2px;
        animation: wave 1.5s infinite ease-in-out;
      }

      .wave:nth-child(2) { animation-delay: -1.4s; }
      .wave:nth-child(3) { animation-delay: -1.2s; }
      .wave:nth-child(4) { animation-delay: -1s; }
      .wave:nth-child(5) { animation-delay: -0.8s; }

      @keyframes wave {
        0%, 40%, 100% { height: 8px; }
        20% { height: 32px; }
      }

      .voice-transcript {
        margin-top: 1rem;
        padding: 1rem;
        background: #f8fafc;
        border-radius: 8px;
        border: 1px solid #e2e8f0;
      }

      .transcript-content {
        font-style: italic;
        color: #4b5563;
        margin-bottom: 1rem;
      }

      .transcript-actions {
        display: flex;
        gap: 0.5rem;
      }

      .btn-voice-accept,
      .btn-voice-retry {
        padding: 0.5rem 1rem;
        border: 1px solid #d1d5db;
        border-radius: 6px;
        background: white;
        cursor: pointer;
        font-size: 0.875rem;
      }

      .btn-voice-accept:hover {
        background: #f0f9ff;
        border-color: #3b82f6;
      }

      /* Receipt Scan Styles */
      .upload-area {
        border: 2px dashed #d1d5db;
        border-radius: 12px;
        padding: 2rem;
        text-align: center;
        cursor: pointer;
        transition: all 0.3s;
        position: relative;
      }

      .upload-area:hover {
        border-color: #3b82f6;
        background: #f8fafc;
      }

      .upload-area.drag-over {
        border-color: #10b981;
        background: #f0fdf4;
      }

      .upload-content {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 1rem;
      }

      .upload-icon {
        font-size: 3rem;
      }

      .upload-text p {
        margin: 0.25rem 0;
      }

      #receipt-input {
        position: absolute;
        opacity: 0;
        pointer-events: none;
      }

      .scan-preview {
        position: relative;
        margin-top: 1rem;
      }

      .scan-preview img {
        width: 100%;
        max-height: 300px;
        object-fit: cover;
        border-radius: 8px;
      }

      .preview-overlay {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 8px;
      }

      .scan-progress {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 1rem;
        color: white;
      }

      .progress-spinner {
        width: 40px;
        height: 40px;
        border: 4px solid rgba(255, 255, 255, 0.3);
        border-top: 4px solid white;
        border-radius: 50%;
        animation: spin 1s linear infinite;
      }

      /* Form Styles */
      .form-container {
        background: white;
        border-radius: 12px;
        padding: 2rem;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        margin-bottom: 2rem;
      }

      .form-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 1.5rem;
        margin-bottom: 2rem;
      }

      .form-group.full-width {
        grid-column: 1 / -1;
      }

      .form-group label {
        display: block;
        margin-bottom: 0.5rem;
        font-weight: 500;
        color: #374151;
      }

      .form-group input,
      .form-group select {
        width: 100%;
        padding: 0.75rem;
        border: 2px solid #e5e7eb;
        border-radius: 8px;
        font-size: 1rem;
      }

      .form-group input:focus,
      .form-group select:focus {
        outline: none;
        border-color: #3b82f6;
      }

      .amount-input-container {
        position: relative;
      }

      .currency {
        position: absolute;
        right: 12px;
        top: 50%;
        transform: translateY(-50%);
        color: #6b7280;
        font-size: 0.875rem;
        pointer-events: none;
      }

      .smart-amounts {
        display: flex;
        gap: 0.5rem;
        margin-top: 0.5rem;
        flex-wrap: wrap;
      }

      .smart-amount {
        padding: 0.25rem 0.75rem;
        background: #f3f4f6;
        border: 1px solid #d1d5db;
        border-radius: 20px;
        cursor: pointer;
        font-size: 0.75rem;
        transition: all 0.2s;
      }

      .smart-amount:hover {
        background: #e5e7eb;
      }

      .type-selector {
        display: flex;
        gap: 0.5rem;
      }

      .type-btn {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.25rem;
        padding: 0.75rem;
        border: 2px solid #e5e7eb;
        border-radius: 8px;
        background: white;
        cursor: pointer;
        transition: all 0.2s;
      }

      .type-btn:hover {
        border-color: #3b82f6;
      }

      .type-btn.active {
        border-color: #3b82f6;
        background: #eff6ff;
      }

      .category-suggestions {
        margin-top: 0.5rem;
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
      }

      .category-suggestion {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.5rem;
        background: #f8fafc;
        border-radius: 6px;
        border: 1px solid #e2e8f0;
      }

      .suggestion-info {
        display: flex;
        align-items: center;
        gap: 0.75rem;
      }

      .suggestion-confidence {
        font-size: 0.75rem;
        color: #6b7280;
        background: #f3f4f6;
        padding: 0.25rem 0.5rem;
        border-radius: 12px;
      }

      .btn-select-category {
        padding: 0.25rem 0.75rem;
        background: #3b82f6;
        color: white;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        font-size: 0.75rem;
      }

      /* Quick Actions */
      .quick-actions-container {
        background: white;
        border-radius: 12px;
        padding: 1.5rem;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
        margin-bottom: 2rem;
      }

      .quick-actions-container h4 {
        margin: 0 0 1rem 0;
        color: #1e293b;
      }

      .quick-buttons {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
        gap: 1rem;
      }

      .quick-btn {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.5rem;
        padding: 1rem 0.5rem;
        background: #f8fafc;
        border: 2px solid #e2e8f0;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.2s;
      }

      .quick-btn:hover {
        background: #f1f5f9;
        border-color: #3b82f6;
      }

      .quick-icon {
        font-size: 1.5rem;
      }

      .quick-text {
        font-size: 0.875rem;
        font-weight: 500;
      }

      .quick-amount {
        font-size: 0.75rem;
        color: #6b7280;
      }

      /* Form Actions */
      .form-actions {
        display: flex;
        gap: 1rem;
        justify-content: center;
        flex-wrap: wrap;
      }

      .btn-save,
      .btn-save-template,
      .btn-clear {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.75rem 1.5rem;
        border: none;
        border-radius: 8px;
        cursor: pointer;
        font-size: 1rem;
        font-weight: 500;
        transition: all 0.2s;
      }

      .btn-save {
        background: #10b981;
        color: white;
      }

      .btn-save:disabled {
        background: #94a3b8;
        cursor: not-allowed;
      }

      .btn-save.ready {
        background: #059669;
        animation: pulse 2s infinite;
      }

      .btn-clear {
        background: #f3f4f6;
        color: #374151;
        border: 1px solid #d1d5db;
      }

      .btn-clear:hover {
        background: #e5e7eb;
      }

      /* Insights */
      .insights-container {
        background: white;
        border-radius: 12px;
        padding: 1.5rem;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
      }

      .insights-container h4 {
        margin: 0 0 1rem 0;
        color: #1e293b;
      }

      .insight-placeholder {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 1rem;
        padding: 2rem;
        text-align: center;
        color: #6b7280;
      }

      .placeholder-icon {
        font-size: 3rem;
      }

      .insights-summary {
        display: flex;
        flex-direction: column;
        gap: 1.5rem;
      }

      .financial-score {
        display: flex;
        align-items: center;
        gap: 1rem;
      }

      .score-circle {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        width: 80px;
        height: 80px;
        border-radius: 50%;
        background: conic-gradient(from 0deg, #10b981 0% var(--score-percent, 70%), #e5e7eb var(--score-percent, 70%) 100%);
        position: relative;
      }

      .score-circle::before {
        content: '';
        position: absolute;
        width: 60px;
        height: 60px;
        background: white;
        border-radius: 50%;
      }

      .score-value,
      .score-label {
        position: relative;
        z-index: 1;
      }

      .score-value {
        font-size: 1.5rem;
        font-weight: bold;
        color: #1e293b;
      }

      .score-label {
        font-size: 0.75rem;
        color: #6b7280;
      }

      .score-info h5 {
        margin: 0 0 0.25rem 0;
        color: #1e293b;
      }

      .key-insights {
        display: flex;
        flex-direction: column;
        gap: 0.75rem;
      }

      .insight-item {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        padding: 0.75rem;
        background: #f8fafc;
        border-radius: 8px;
        border-left: 4px solid #e5e7eb;
      }

      .insight-item.priority-high {
        border-left-color: #ef4444;
      }

      .insight-item.priority-medium {
        border-left-color: #f59e0b;
      }

      .insight-item.priority-low {
        border-left-color: #10b981;
      }

      .insight-text h6 {
        margin: 0 0 0.25rem 0;
        font-size: 0.875rem;
        color: #1e293b;
      }

      .insight-text p {
        margin: 0;
        font-size: 0.75rem;
        color: #6b7280;
      }

      .recommendations {
        padding: 1rem;
        background: #fffbeb;
        border-radius: 8px;
        border: 1px solid #fbbf24;
      }

      .recommendations h6 {
        margin: 0 0 0.5rem 0;
        color: #92400e;
      }

      .recommendation-item {
        display: flex;
        flex-direction: column;
        gap: 0.25rem;
        margin-bottom: 0.5rem;
      }

      .rec-title {
        font-size: 0.875rem;
        font-weight: 500;
        color: #92400e;
      }

      .rec-message {
        font-size: 0.75rem;
        color: #a16207;
      }

      /* AI Insights */
      .ai-insight {
        background: #f0f9ff;
        border: 1px solid #bfdbfe;
        border-radius: 8px;
        padding: 1rem;
        margin-top: 1rem;
      }

      .insight-header {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin-bottom: 0.75rem;
      }

      .insight-title {
        font-weight: 500;
        color: #1e40af;
      }

      .insight-content p {
        margin: 0.25rem 0;
        font-size: 0.875rem;
        color: #1e3a8a;
      }

      /* Receipt Items */
      .receipt-items {
        background: #f8fafc;
        border: 1px solid #e2e8f0;
        border-radius: 8px;
        padding: 1rem;
        margin-top: 1rem;
      }

      .items-header {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        margin-bottom: 0.75rem;
      }

      .items-title {
        font-weight: 500;
        color: #374151;
      }

      .items-list {
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
      }

      .receipt-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 0.5rem;
        background: white;
        border-radius: 4px;
        font-size: 0.875rem;
      }

      /* Animations */
      @keyframes pulse {
        0%, 100% { opacity: 1; }
        50% { opacity: 0.5; }
      }

      @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
      }

      /* Responsive Design */
      @media (max-width: 768px) {
        .ai-transaction-assistant {
          padding: 1rem;
        }

        .assistant-header {
          flex-direction: column;
          gap: 1rem;
          text-align: center;
        }

        .input-methods {
          grid-template-columns: 1fr;
        }

        .form-grid {
          grid-template-columns: 1fr;
        }

        .quick-buttons {
          grid-template-columns: repeat(2, 1fr);
        }

        .form-actions {
          flex-direction: column;
        }

        .financial-score {
          flex-direction: column;
          text-align: center;
        }
      }
    `;
    
    document.head.appendChild(style);

    // Set score circle progress
    setTimeout(() => {
      document.querySelectorAll('.score-circle[data-score]').forEach(circle => {
        const score = parseInt(circle.dataset.score);
        circle.style.setProperty('--score-percent', `${score}%`);
      });
    }, 100);
  }
}

export default AITransactionAssistant;