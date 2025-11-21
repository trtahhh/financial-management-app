// Notification Settings UI Component
import SmartNotificationSystem from './SmartNotificationSystem.js';

class NotificationSettings {
  constructor(container, notificationSystem, options = {}) {
    this.container = container;
    this.notificationSystem = notificationSystem;
    this.options = {
      showAdvanced: true,
      allowEmailConfig: true,
      showTestButtons: true,
      ...options
    };
    
    this.preferences = notificationSystem.preferences;
    
    this.init();
  }

  init() {
    this.render();
    this.attachEventListeners();
    this.loadCurrentSettings();
  }

  render() {
    this.container.innerHTML = `
      <div class="notification-settings">
        <div class="settings-header">
          <h2>Cài đặt thông báo</h2>
          <p class="settings-description">
            Tùy chỉnh cách nhận thông báo về tài chính của bạn
          </p>
        </div>

        <div class="settings-sections">
          ${this.renderGeneralSettings()}
          ${this.renderCategorySettings()}
          ${this.renderChannelSettings()}
          ${this.renderQuietHoursSettings()}
          ${this.renderReminderSettings()}
          ${this.renderAdvancedSettings()}
          ${this.renderTestSection()}
        </div>

        <div class="settings-actions">
          <button class="btn-primary" id="btn-save-settings">Lưu cài đặt</button>
          <button class="btn-secondary" id="btn-reset-settings">Khôi phục mặc định</button>
          <button class="btn-secondary" id="btn-export-settings">Xuất cài đặt</button>
        </div>
      </div>
    `;

    this.renderStyles();
  }

  renderGeneralSettings() {
    return `
      <div class="settings-section">
        <h3>Cài đặt chung</h3>
        
        <div class="setting-group">
          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label">Bật thông báo</label>
              <span class="setting-description">Nhận thông báo từ ứng dụng</span>
            </div>
            <div class="setting-control">
              <label class="toggle">
                <input type="checkbox" id="enable-notifications" checked>
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label">Âm thanh thông báo</label>
              <span class="setting-description">Phát âm thanh khi có thông báo</span>
            </div>
            <div class="setting-control">
              <label class="toggle">
                <input type="checkbox" id="enable-sound" ${this.preferences.soundEnabled ? 'checked' : ''}>
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label">Rung</label>
              <span class="setting-description">Rung thiết bị khi có thông báo</span>
            </div>
            <div class="setting-control">
              <label class="toggle">
                <input type="checkbox" id="enable-vibration" ${this.preferences.vibrationEnabled ? 'checked' : ''}>
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label">Mức độ ưu tiên tối thiểu</label>
              <span class="setting-description">Chỉ nhận thông báo từ mức độ này trở lên</span>
            </div>
            <div class="setting-control">
              <select id="min-priority">
                <option value="low" ${this.preferences.minPriority === 'low' ? 'selected' : ''}>Thấp</option>
                <option value="medium" ${this.preferences.minPriority === 'medium' ? 'selected' : ''}>Trung bình</option>
                <option value="high" ${this.preferences.minPriority === 'high' ? 'selected' : ''}>Cao</option>
              </select>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  renderCategorySettings() {
    return `
      <div class="settings-section">
        <h3>Loại thông báo</h3>
        
        <div class="setting-group">
          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label"> Cảnh báo ngân sách</label>
              <span class="setting-description">Thông báo khi sắp hết hoặc vượt ngân sách</span>
            </div>
            <div class="setting-control">
              <label class="toggle">
                <input type="checkbox" id="category-budget" ${this.preferences.categories.budget ? 'checked' : ''}>
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label"> Thành tích</label>
              <span class="setting-description">Thông báo khi đạt được thành tích mới</span>
            </div>
            <div class="setting-control">
              <label class="toggle">
                <input type="checkbox" id="category-achievement" ${this.preferences.categories.achievement ? 'checked' : ''}>
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label"> Thông tin chi tiêu</label>
              <span class="setting-description">Gợi ý và phân tích thói quen chi tiêu</span>
            </div>
            <div class="setting-control">
              <label class="toggle">
                <input type="checkbox" id="category-insight" ${this.preferences.categories.insight ? 'checked' : ''}>
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label"> Nhắc nhở</label>
              <span class="setting-description">Nhắc nhở về mục tiêu và nhiệm vụ</span>
            </div>
            <div class="setting-control">
              <label class="toggle">
                <input type="checkbox" id="category-reminder" ${this.preferences.categories.reminder ? 'checked' : ''}>
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label"> Báo cáo phân tích</label>
              <span class="setting-description">Báo cáo định kỳ về tình hình tài chính</span>
            </div>
            <div class="setting-control">
              <label class="toggle">
                <input type="checkbox" id="category-analytics" ${this.preferences.categories.analytics ? 'checked' : ''}>
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  renderChannelSettings() {
    return `
      <div class="settings-section">
        <h3>Kênh thông báo</h3>
        
        <div class="setting-group">
          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label">Trình duyệt</label>
              <span class="setting-description">Thông báo desktop từ trình duyệt</span>
            </div>
            <div class="setting-control">
              <label class="toggle">
                <input type="checkbox" id="channel-browser" ${this.preferences.channels.browser ? 'checked' : ''}>
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label">Hệ thống</label>
              <span class="setting-description">Thông báo hệ thống (nếu hỗ trợ)</span>
            </div>
            <div class="setting-control">
              <label class="toggle">
                <input type="checkbox" id="channel-system" ${this.preferences.channels.system ? 'checked' : ''}>
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>

          ${this.options.allowEmailConfig ? `
            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">Email</label>
                <span class="setting-description">Gửi thông báo qua email</span>
              </div>
              <div class="setting-control">
                <label class="toggle">
                  <input type="checkbox" id="channel-email" ${this.preferences.channels.email ? 'checked' : ''}>
                  <span class="toggle-slider"></span>
                </label>
              </div>
            </div>
            
            <div class="setting-item ${!this.preferences.channels.email ? 'disabled' : ''}" id="email-config">
              <div class="setting-info">
                <label class="setting-label">Địa chỉ email</label>
                <span class="setting-description">Email nhận thông báo</span>
              </div>
              <div class="setting-control">
                <input type="email" id="notification-email" value="${this.preferences.email || ''}" 
                       placeholder="your@email.com" ${!this.preferences.channels.email ? 'disabled' : ''}>
              </div>
            </div>
          ` : ''}
        </div>
      </div>
    `;
  }

  renderQuietHoursSettings() {
    return `
      <div class="settings-section">
        <h3>Giờ yên lặng</h3>
        
        <div class="setting-group">
          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label">Bật giờ yên lặng</label>
              <span class="setting-description">Không nhận thông báo trong khung giờ này (trừ khẩn cấp)</span>
            </div>
            <div class="setting-control">
              <label class="toggle">
                <input type="checkbox" id="enable-quiet-hours" ${this.preferences.quietHours.enabled ? 'checked' : ''}>
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>

          <div class="quiet-hours-config ${!this.preferences.quietHours.enabled ? 'disabled' : ''}">
            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">Từ giờ</label>
                <span class="setting-description">Bắt đầu giờ yên lặng</span>
              </div>
              <div class="setting-control">
                <select id="quiet-start" ${!this.preferences.quietHours.enabled ? 'disabled' : ''}>
                  ${Array.from({length: 24}, (_, i) => 
                    `<option value="${i}" ${this.preferences.quietHours.start === i ? 'selected' : ''}>${i.toString().padStart(2, '0')}:00</option>`
                  ).join('')}
                </select>
              </div>
            </div>

            <div class="setting-item">
              <div class="setting-info">
                <label class="setting-label">Đến giờ</label>
                <span class="setting-description">Kết thúc giờ yên lặng</span>
              </div>
              <div class="setting-control">
                <select id="quiet-end" ${!this.preferences.quietHours.enabled ? 'disabled' : ''}>
                  ${Array.from({length: 24}, (_, i) => 
                    `<option value="${i}" ${this.preferences.quietHours.end === i ? 'selected' : ''}>${i.toString().padStart(2, '0')}:00</option>`
                  ).join('')}
                </select>
              </div>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  renderReminderSettings() {
    return `
      <div class="settings-section">
        <h3>Nhắc nhở</h3>
        
        <div class="setting-group">
          <div class="reminder-templates">
            <h4>Nhắc nhở có sẵn</h4>
            
            <div class="reminder-template">
              <div class="template-info">
                <span class="template-name">Nhập giao dịch hàng ngày</span>
                <span class="template-description">Nhắc nhở nhập giao dịch vào cuối ngày</span>
              </div>
              <div class="template-control">
                <label class="toggle">
                  <input type="checkbox" id="reminder-daily-entry">
                  <span class="toggle-slider"></span>
                </label>
                <select id="daily-entry-time" disabled>
                  <option value="18">18:00</option>
                  <option value="19">19:00</option>
                  <option value="20">20:00</option>
                  <option value="21">21:00</option>
                </select>
              </div>
            </div>

            <div class="reminder-template">
              <div class="template-info">
                <span class="template-name">Kiểm tra ngân sách</span>
                <span class="template-description">Nhắc nhở xem lại ngân sách hàng tuần</span>
              </div>
              <div class="template-control">
                <label class="toggle">
                  <input type="checkbox" id="reminder-budget-review">
                  <span class="toggle-slider"></span>
                </label>
                <select id="budget-review-day" disabled>
                  <option value="0">Chủ nhật</option>
                  <option value="1">Thứ 2</option>
                  <option value="6">Thứ 7</option>
                </select>
              </div>
            </div>

            <div class="reminder-template">
              <div class="template-info">
                <span class="template-name">Cập nhật mục tiêu</span>
                <span class="template-description">Nhắc nhở cập nhật tiến độ mục tiêu</span>
              </div>
              <div class="template-control">
                <label class="toggle">
                  <input type="checkbox" id="reminder-goal-update">
                  <span class="toggle-slider"></span>
                </label>
                <select id="goal-update-frequency" disabled>
                  <option value="weekly">Hàng tuần</option>
                  <option value="monthly">Hàng tháng</option>
                </select>
              </div>
            </div>
          </div>

          <div class="custom-reminders">
            <h4>Nhắc nhở tùy chỉnh</h4>
            <button class="btn-add-reminder" id="btn-add-custom-reminder">+ Thêm nhắc nhở</button>
            <div class="custom-reminder-list" id="custom-reminder-list">
              <!-- Custom reminders will be loaded here -->
            </div>
          </div>
        </div>
      </div>
    `;
  }

  renderAdvancedSettings() {
    if (!this.options.showAdvanced) return '';
    
    return `
      <div class="settings-section advanced-section">
        <h3>Cài đặt nâng cao</h3>
        
        <div class="setting-group">
          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label">Tần suất kiểm tra</label>
              <span class="setting-description">Tần suất kiểm tra thông báo tự động</span>
            </div>
            <div class="setting-control">
              <select id="check-frequency">
                <option value="1">1 phút</option>
                <option value="5" selected>5 phút</option>
                <option value="10">10 phút</option>
                <option value="30">30 phút</option>
                <option value="60">1 giờ</option>
              </select>
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label">Giới hạn thông báo</label>
              <span class="setting-description">Số thông báo tối đa trong 1 giờ</span>
            </div>
            <div class="setting-control">
              <input type="number" id="notification-limit" value="10" min="1" max="50">
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label">Lưu lịch sử</label>
              <span class="setting-description">Số ngày lưu lịch sử thông báo</span>
            </div>
            <div class="setting-control">
              <input type="number" id="history-days" value="30" min="1" max="365">
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label">Thông báo thông minh</label>
              <span class="setting-description">Sử dụng AI để cá nhân hóa thông báo</span>
            </div>
            <div class="setting-control">
              <label class="toggle">
                <input type="checkbox" id="smart-notifications" checked>
                <span class="toggle-slider"></span>
              </label>
            </div>
          </div>

          <div class="setting-item">
            <div class="setting-info">
              <label class="setting-label">Xóa lịch sử</label>
              <span class="setting-description">Xóa tất cả lịch sử thông báo</span>
            </div>
            <div class="setting-control">
              <button class="btn-danger" id="btn-clear-history">Xóa lịch sử</button>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  renderTestSection() {
    if (!this.options.showTestButtons) return '';
    
    return `
      <div class="settings-section test-section">
        <h3>Kiểm tra thông báo</h3>
        
        <div class="test-buttons">
          <button class="btn-test" id="test-budget-warning">Test cảnh báo ngân sách</button>
          <button class="btn-test" id="test-achievement">Test thành tích</button>
          <button class="btn-test" id="test-insight">Test thông tin chi tiêu</button>
          <button class="btn-test" id="test-reminder">Test nhắc nhở</button>
        </div>
        
        <div class="permission-status">
          <h4>Trạng thái quyền</h4>
          <div class="status-item">
            <span>Notification API:</span>
            <span class="status" id="notification-permission-status">Đang kiểm tra...</span>
          </div>
          <div class="status-item">
            <span>Service Worker:</span>
            <span class="status" id="sw-status">Đang kiểm tra...</span>
          </div>
        </div>
      </div>
    `;
  }

  loadCurrentSettings() {
    // Load permission status
    this.updatePermissionStatus();
    
    // Load custom reminders
    this.loadCustomReminders();
  }

  async updatePermissionStatus() {
    const notificationStatus = document.getElementById('notification-permission-status');
    const swStatus = document.getElementById('sw-status');
    
    if (notificationStatus) {
      const permission = Notification.permission;
      notificationStatus.textContent = {
        'granted': 'Đã cấp quyền ',
        'denied': 'Đã từ chối ',
        'default': 'Chưa yêu cầu '
      }[permission] || 'Không hỗ trợ ';
      
      notificationStatus.className = `status ${permission}`;
    }
    
    if (swStatus) {
      if ('serviceWorker' in navigator) {
        try {
          const registration = await navigator.serviceWorker.getRegistration();
          swStatus.textContent = registration ? 'Hoạt động ' : 'Chưa cài đặt ';
          swStatus.className = `status ${registration ? 'active' : 'inactive'}`;
        } catch (error) {
          swStatus.textContent = 'Lỗi ';
          swStatus.className = 'status error';
        }
      } else {
        swStatus.textContent = 'Không hỗ trợ ';
        swStatus.className = 'status unsupported';
      }
    }
  }

  loadCustomReminders() {
    const container = document.getElementById('custom-reminder-list');
    if (!container) return;
    
    const customReminders = this.notificationSystem.getActiveReminders()
      .filter(r => r.type === 'custom');
    
    container.innerHTML = customReminders.map(reminder => `
      <div class="custom-reminder-item" data-reminder-id="${reminder.id}">
        <div class="reminder-info">
          <span class="reminder-title">${reminder.title}</span>
          <span class="reminder-schedule">${this.formatReminderSchedule(reminder)}</span>
        </div>
        <div class="reminder-actions">
          <button class="btn-edit-reminder" data-reminder-id="${reminder.id}">Sửa</button>
          <button class="btn-delete-reminder" data-reminder-id="${reminder.id}">Xóa</button>
        </div>
      </div>
    `).join('');
  }

  formatReminderSchedule(reminder) {
    const frequency = {
      'once': 'Một lần',
      'daily': 'Hàng ngày',
      'weekly': 'Hàng tuần', 
      'monthly': 'Hàng tháng'
    }[reminder.frequency] || reminder.frequency;
    
    const time = new Date(reminder.scheduledTime).toLocaleTimeString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit'
    });
    
    return `${frequency} lúc ${time}`;
  }

  attachEventListeners() {
    // Save settings
    document.getElementById('btn-save-settings')?.addEventListener('click', () => {
      this.saveSettings();
    });
    
    // Reset settings
    document.getElementById('btn-reset-settings')?.addEventListener('click', () => {
      this.resetSettings();
    });
    
    // Export settings
    document.getElementById('btn-export-settings')?.addEventListener('click', () => {
      this.exportSettings();
    });
    
    // Email toggle
    document.getElementById('channel-email')?.addEventListener('change', (e) => {
      const emailConfig = document.getElementById('email-config');
      const emailInput = document.getElementById('notification-email');
      
      if (e.target.checked) {
        emailConfig?.classList.remove('disabled');
        emailInput?.removeAttribute('disabled');
      } else {
        emailConfig?.classList.add('disabled');
        emailInput?.setAttribute('disabled', '');
      }
    });
    
    // Quiet hours toggle
    document.getElementById('enable-quiet-hours')?.addEventListener('change', (e) => {
      const config = document.querySelector('.quiet-hours-config');
      const selects = config?.querySelectorAll('select');
      
      if (e.target.checked) {
        config?.classList.remove('disabled');
        selects?.forEach(select => select.removeAttribute('disabled'));
      } else {
        config?.classList.add('disabled');
        selects?.forEach(select => select.setAttribute('disabled', ''));
      }
    });
    
    // Reminder template toggles
    document.querySelectorAll('.reminder-template input[type="checkbox"]').forEach(checkbox => {
      checkbox.addEventListener('change', (e) => {
        const select = e.target.closest('.reminder-template').querySelector('select');
        if (select) {
          select.disabled = !e.target.checked;
        }
      });
    });
    
    // Add custom reminder
    document.getElementById('btn-add-custom-reminder')?.addEventListener('click', () => {
      this.showAddReminderModal();
    });
    
    // Test buttons
    if (this.options.showTestButtons) {
      document.getElementById('test-budget-warning')?.addEventListener('click', () => {
        this.testNotification('budget_warning');
      });
      
      document.getElementById('test-achievement')?.addEventListener('click', () => {
        this.testNotification('achievement');
      });
      
      document.getElementById('test-insight')?.addEventListener('click', () => {
        this.testNotification('insight');
      });
      
      document.getElementById('test-reminder')?.addEventListener('click', () => {
        this.testNotification('reminder');
      });
    }
    
    // Clear history
    document.getElementById('btn-clear-history')?.addEventListener('click', () => {
      this.clearHistory();
    });
  }

  async saveSettings() {
    try {
      const newPreferences = this.gatherSettings();
      
      // Validate settings
      const validation = this.validateSettings(newPreferences);
      if (!validation.isValid) {
        this.showError(validation.message);
        return;
      }
      
      // Update preferences
      this.notificationSystem.updatePreferences(newPreferences);
      
      // Create/update template reminders
      await this.updateTemplateReminders();
      
      this.showSuccess('Cài đặt đã được lưu');
      
    } catch (error) {
      console.error('Error saving settings:', error);
      this.showError('Có lỗi khi lưu cài đặt');
    }
  }

  gatherSettings() {
    return {
      categories: {
        budget: document.getElementById('category-budget')?.checked || false,
        achievement: document.getElementById('category-achievement')?.checked || false,
        insight: document.getElementById('category-insight')?.checked || false,
        reminder: document.getElementById('category-reminder')?.checked || false,
        analytics: document.getElementById('category-analytics')?.checked || false
      },
      channels: {
        browser: document.getElementById('channel-browser')?.checked || false,
        system: document.getElementById('channel-system')?.checked || false,
        email: document.getElementById('channel-email')?.checked || false
      },
      quietHours: {
        enabled: document.getElementById('enable-quiet-hours')?.checked || false,
        start: parseInt(document.getElementById('quiet-start')?.value) || 22,
        end: parseInt(document.getElementById('quiet-end')?.value) || 7
      },
      soundEnabled: document.getElementById('enable-sound')?.checked || false,
      vibrationEnabled: document.getElementById('enable-vibration')?.checked || false,
      minPriority: document.getElementById('min-priority')?.value || 'low',
      email: document.getElementById('notification-email')?.value || null
    };
  }

  validateSettings(settings) {
    if (settings.channels.email && !settings.email) {
      return {
        isValid: false,
        message: 'Vui lòng nhập địa chỉ email'
      };
    }
    
    if (settings.email && !this.isValidEmail(settings.email)) {
      return {
        isValid: false,
        message: 'Địa chỉ email không hợp lệ'
      };
    }
    
    return { isValid: true };
  }

  isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
  }

  async updateTemplateReminders() {
    // Daily entry reminder
    if (document.getElementById('reminder-daily-entry')?.checked) {
      const time = parseInt(document.getElementById('daily-entry-time')?.value) || 18;
      await this.createTemplateReminder('daily-entry', {
        title: 'Nhập giao dịch',
        message: 'Đừng quên nhập các giao dịch hôm nay',
        frequency: 'daily',
        scheduledTime: this.createTimeFromHour(time)
      });
    }
    
    // Budget review reminder
    if (document.getElementById('reminder-budget-review')?.checked) {
      const day = parseInt(document.getElementById('budget-review-day')?.value) || 0;
      await this.createTemplateReminder('budget-review', {
        title: 'Kiểm tra ngân sách',
        message: 'Hãy xem lại ngân sách tuần này',
        frequency: 'weekly',
        scheduledTime: this.createTimeFromWeekday(day, 9)
      });
    }
    
    // Goal update reminder
    if (document.getElementById('reminder-goal-update')?.checked) {
      const frequency = document.getElementById('goal-update-frequency')?.value || 'weekly';
      await this.createTemplateReminder('goal-update', {
        title: 'Cập nhật mục tiêu',
        message: 'Hãy cập nhật tiến độ các mục tiêu của bạn',
        frequency: frequency,
        scheduledTime: frequency === 'weekly' 
          ? this.createTimeFromWeekday(0, 10)  // Sunday 10AM
          : this.createTimeFromDay(1, 10)     // 1st of month 10AM
      });
    }
  }

  async createTemplateReminder(templateId, reminderData) {
    // Remove existing template reminder
    const existing = this.notificationSystem.reminders.find(r => r.templateId === templateId);
    if (existing) {
      existing.isEnabled = false;
    }
    
    // Create new reminder
    this.notificationSystem.createReminder({
      ...reminderData,
      type: 'template',
      templateId: templateId
    });
  }

  createTimeFromHour(hour) {
    const date = new Date();
    date.setHours(hour, 0, 0, 0);
    return date.toISOString();
  }

  createTimeFromWeekday(weekday, hour) {
    const date = new Date();
    const currentDay = date.getDay();
    const daysToAdd = (weekday - currentDay + 7) % 7;
    date.setDate(date.getDate() + daysToAdd);
    date.setHours(hour, 0, 0, 0);
    return date.toISOString();
  }

  createTimeFromDay(dayOfMonth, hour) {
    const date = new Date();
    date.setDate(dayOfMonth);
    date.setHours(hour, 0, 0, 0);
    if (date <= new Date()) {
      date.setMonth(date.getMonth() + 1);
    }
    return date.toISOString();
  }

  async testNotification(type) {
    const testNotifications = {
      budget_warning: {
        title: ' Test cảnh báo ngân sách',
        message: 'Đây là thông báo test cho cảnh báo ngân sách',
        type: 'budget_warning',
        priority: 'medium'
      },
      achievement: {
        title: ' Test thành tích',
        message: 'Bạn đã test thành công tính năng thông báo!',
        type: 'achievement',
        priority: 'medium'
      },
      insight: {
        title: ' Test thông tin',
        message: 'Đây là một gợi ý test về chi tiêu của bạn',
        type: 'insight',
        priority: 'low'
      },
      reminder: {
        title: ' Test nhắc nhở',
        message: 'Đây là thông báo nhắc nhở test',
        type: 'reminder',
        priority: 'medium'
      }
    };
    
    const notification = testNotifications[type];
    if (notification) {
      await this.notificationSystem.sendNotification({
        id: `test-${type}-${Date.now()}`,
        ...notification
      });
    }
  }

  resetSettings() {
    if (confirm('Bạn có chắc muốn khôi phục cài đặt mặc định?')) {
      // Reset to defaults
      const defaults = {
        categories: {
          budget: true,
          achievement: true,
          insight: true,
          reminder: true,
          analytics: false
        },
        channels: {
          browser: true,
          system: false,
          email: false
        },
        quietHours: {
          enabled: true,
          start: 22,
          end: 7
        },
        soundEnabled: true,
        vibrationEnabled: true,
        minPriority: 'low',
        email: null
      };
      
      this.notificationSystem.updatePreferences(defaults);
      this.render(); // Re-render with defaults
      this.showSuccess('Đã khôi phục cài đặt mặc định');
    }
  }

  exportSettings() {
    const settings = {
      preferences: this.notificationSystem.preferences,
      reminders: this.notificationSystem.getActiveReminders(),
      exportDate: new Date().toISOString(),
      version: '1.0'
    };
    
    const blob = new Blob([JSON.stringify(settings, null, 2)], {
      type: 'application/json'
    });
    
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `notification-settings-${new Date().toISOString().split('T')[0]}.json`;
    a.click();
    
    URL.revokeObjectURL(url);
    this.showSuccess('Đã xuất cài đặt');
  }

  clearHistory() {
    if (confirm('Bạn có chắc muốn xóa tất cả lịch sử thông báo?')) {
      this.notificationSystem.clearHistory();
      this.showSuccess('Đã xóa lịch sử thông báo');
    }
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
    if (document.getElementById('notification-settings-styles')) return;

    const style = document.createElement('style');
    style.id = 'notification-settings-styles';
    style.textContent = `
      .notification-settings {
        max-width: 800px;
        margin: 0 auto;
        padding: 2rem;
      }

      .settings-header {
        text-align: center;
        margin-bottom: 2rem;
      }

      .settings-header h2 {
        margin: 0 0 0.5rem 0;
        color: #111827;
        font-size: 1.875rem;
      }

      .settings-description {
        margin: 0;
        color: #6b7280;
        font-size: 1rem;
      }

      .settings-sections {
        display: flex;
        flex-direction: column;
        gap: 2rem;
        margin-bottom: 2rem;
      }

      .settings-section {
        background: white;
        border-radius: 12px;
        padding: 1.5rem;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        border: 1px solid #e5e7eb;
      }

      .settings-section h3 {
        margin: 0 0 1.5rem 0;
        color: #111827;
        font-size: 1.25rem;
        font-weight: 600;
      }

      .setting-group {
        display: flex;
        flex-direction: column;
        gap: 1rem;
      }

      .setting-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1rem;
        background: #f9fafb;
        border-radius: 8px;
        transition: opacity 0.2s;
      }

      .setting-item.disabled {
        opacity: 0.5;
      }

      .setting-info {
        flex: 1;
      }

      .setting-label {
        display: block;
        font-weight: 500;
        color: #111827;
        margin-bottom: 0.25rem;
      }

      .setting-description {
        font-size: 0.875rem;
        color: #6b7280;
      }

      .setting-control {
        display: flex;
        align-items: center;
        gap: 0.5rem;
      }

      /* Toggle Switch */
      .toggle {
        position: relative;
        display: inline-block;
        width: 44px;
        height: 24px;
      }

      .toggle input {
        opacity: 0;
        width: 0;
        height: 0;
      }

      .toggle-slider {
        position: absolute;
        cursor: pointer;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background-color: #d1d5db;
        transition: 0.3s;
        border-radius: 24px;
      }

      .toggle-slider:before {
        position: absolute;
        content: "";
        height: 18px;
        width: 18px;
        left: 3px;
        bottom: 3px;
        background-color: white;
        transition: 0.3s;
        border-radius: 50%;
      }

      .toggle input:checked + .toggle-slider {
        background-color: #3b82f6;
      }

      .toggle input:checked + .toggle-slider:before {
        transform: translateX(20px);
      }

      /* Form Controls */
      .setting-control select,
      .setting-control input[type="email"],
      .setting-control input[type="number"] {
        padding: 0.5rem;
        border: 1px solid #d1d5db;
        border-radius: 6px;
        font-size: 0.875rem;
        background: white;
      }

      .setting-control select:disabled,
      .setting-control input:disabled {
        background: #f3f4f6;
        color: #9ca3af;
        cursor: not-allowed;
      }

      /* Quiet Hours */
      .quiet-hours-config.disabled .setting-item {
        opacity: 0.5;
      }

      /* Reminder Templates */
      .reminder-templates h4 {
        margin: 0 0 1rem 0;
        color: #374151;
        font-size: 1rem;
      }

      .reminder-template {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1rem;
        border: 1px solid #e5e7eb;
        border-radius: 8px;
        background: white;
      }

      .template-info {
        flex: 1;
      }

      .template-name {
        display: block;
        font-weight: 500;
        color: #111827;
        margin-bottom: 0.25rem;
      }

      .template-description {
        font-size: 0.875rem;
        color: #6b7280;
      }

      .template-control {
        display: flex;
        align-items: center;
        gap: 0.75rem;
      }

      /* Custom Reminders */
      .custom-reminders {
        margin-top: 2rem;
      }

      .btn-add-reminder {
        width: 100%;
        padding: 0.75rem;
        border: 2px dashed #d1d5db;
        background: none;
        border-radius: 8px;
        color: #6b7280;
        cursor: pointer;
        font-size: 0.875rem;
        transition: all 0.2s;
      }

      .btn-add-reminder:hover {
        border-color: #3b82f6;
        color: #3b82f6;
      }

      .custom-reminder-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1rem;
        border: 1px solid #e5e7eb;
        border-radius: 8px;
        background: white;
        margin-top: 0.5rem;
      }

      .reminder-info {
        flex: 1;
      }

      .reminder-title {
        display: block;
        font-weight: 500;
        color: #111827;
        margin-bottom: 0.25rem;
      }

      .reminder-schedule {
        font-size: 0.875rem;
        color: #6b7280;
      }

      .reminder-actions {
        display: flex;
        gap: 0.5rem;
      }

      .btn-edit-reminder,
      .btn-delete-reminder {
        padding: 0.25rem 0.75rem;
        border: 1px solid #d1d5db;
        border-radius: 4px;
        background: white;
        cursor: pointer;
        font-size: 0.75rem;
      }

      .btn-delete-reminder:hover {
        background: #fef2f2;
        border-color: #fecaca;
        color: #dc2626;
      }

      /* Test Section */
      .test-buttons {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
        gap: 1rem;
        margin-bottom: 1.5rem;
      }

      .btn-test {
        padding: 0.75rem 1rem;
        border: 1px solid #3b82f6;
        border-radius: 8px;
        background: white;
        color: #3b82f6;
        cursor: pointer;
        font-size: 0.875rem;
        transition: all 0.2s;
      }

      .btn-test:hover {
        background: #3b82f6;
        color: white;
      }

      .permission-status h4 {
        margin: 0 0 0.75rem 0;
        color: #374151;
        font-size: 1rem;
      }

      .status-item {
        display: flex;
        justify-content: space-between;
        padding: 0.5rem 0;
        border-bottom: 1px solid #f3f4f6;
      }

      .status-item:last-child {
        border-bottom: none;
      }

      .status.granted,
      .status.active {
        color: #10b981;
      }

      .status.denied,
      .status.inactive,
      .status.error {
        color: #ef4444;
      }

      .status.default,
      .status.unsupported {
        color: #f59e0b;
      }

      /* Action Buttons */
      .settings-actions {
        display: flex;
        gap: 1rem;
        justify-content: center;
        flex-wrap: wrap;
      }

      .btn-primary,
      .btn-secondary,
      .btn-danger {
        padding: 0.75rem 1.5rem;
        border-radius: 8px;
        cursor: pointer;
        font-size: 0.875rem;
        font-weight: 500;
        transition: all 0.2s;
      }

      .btn-primary {
        background: #3b82f6;
        color: white;
        border: 1px solid #3b82f6;
      }

      .btn-primary:hover {
        background: #2563eb;
        border-color: #2563eb;
      }

      .btn-secondary {
        background: white;
        color: #374151;
        border: 1px solid #d1d5db;
      }

      .btn-secondary:hover {
        background: #f3f4f6;
      }

      .btn-danger {
        background: #ef4444;
        color: white;
        border: 1px solid #ef4444;
      }

      .btn-danger:hover {
        background: #dc2626;
        border-color: #dc2626;
      }

      /* Responsive Design */
      @media (max-width: 768px) {
        .notification-settings {
          padding: 1rem;
        }

        .setting-item {
          flex-direction: column;
          align-items: stretch;
          gap: 1rem;
        }

        .reminder-template,
        .custom-reminder-item {
          flex-direction: column;
          align-items: stretch;
          gap: 1rem;
        }

        .template-control,
        .reminder-actions {
          justify-content: flex-end;
        }

        .test-buttons {
          grid-template-columns: 1fr;
        }

        .settings-actions {
          flex-direction: column;
        }
      }

      @media (max-width: 480px) {
        .settings-header h2 {
          font-size: 1.5rem;
        }

        .settings-section {
          padding: 1rem;
        }

        .setting-item {
          padding: 0.75rem;
        }
      }
    `;
    
    document.head.appendChild(style);
  }
}

export default NotificationSettings;