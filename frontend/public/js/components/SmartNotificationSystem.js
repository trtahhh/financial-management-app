// Smart Notifications System
import { formatCurrency } from '../utils/currencyHelpers.js';
import { formatDate, formatRelativeTime } from '../utils/dateHelpers.js';
import { CATEGORY_NAMES } from '../constants/categories.js';

class SmartNotificationSystem {
  constructor(options = {}) {
    this.options = {
      enablePushNotifications: true,
      enableEmailAlerts: false,
      enableSoundAlerts: true,
      enableVibration: true,
      enableBudgetWarnings: true,
      enableAchievementNotifications: true,
      enableReminderSystem: true,
      enableSmartInsights: true,
      notificationChannels: ['browser', 'system'],
      quietHours: { start: 22, end: 7 },
      ...options
    };
    
    this.notifications = [];
    this.reminders = [];
    this.alertRules = [];
    this.notificationHistory = [];
    this.preferences = this.loadPreferences();
    
    this.init();
  }

  async init() {
    await this.requestPermissions();
    this.setupNotificationChannels();
    this.loadAlertRules();
    this.setupReminderSystem();
    this.startBackgroundMonitoring();
    this.registerServiceWorker();
  }

  // Permission Management
  async requestPermissions() {
    if (!('Notification' in window)) {
      console.warn('Trình duyệt không hỗ trợ notifications');
      return false;
    }

    if (Notification.permission === 'granted') {
      return true;
    }

    if (Notification.permission !== 'denied') {
      const permission = await Notification.requestPermission();
      return permission === 'granted';
    }

    return false;
  }

  // Core Notification Functions
  async sendNotification(notification) {
    if (!this.shouldSendNotification(notification)) {
      return false;
    }

    // Apply user preferences
    const processedNotification = this.applyPreferences(notification);
    
    // Send through enabled channels
    const results = await Promise.allSettled([
      this.sendBrowserNotification(processedNotification),
      this.sendSystemNotification(processedNotification),
      this.sendInAppNotification(processedNotification),
      this.sendEmailAlert(processedNotification)
    ]);

    // Store in history
    this.storeNotificationHistory(processedNotification);
    
    // Trigger callbacks
    this.onNotificationSent?.(processedNotification, results);
    
    return true;
  }

  async sendBrowserNotification(notification) {
    if (!this.options.enablePushNotifications || Notification.permission !== 'granted') {
      return false;
    }

    try {
      const browserNotification = new Notification(notification.title, {
        body: notification.message,
        icon: notification.icon || '/favicon.ico',
        badge: '/favicon.ico',
        image: notification.image,
        tag: notification.tag || notification.id,
        requireInteraction: notification.priority === 'high',
        silent: this.isQuietHours(),
        vibrate: this.options.enableVibration ? [200, 100, 200] : undefined,
        data: {
          id: notification.id,
          type: notification.type,
          timestamp: Date.now(),
          actions: notification.actions
        }
      });

      browserNotification.onclick = () => {
        this.handleNotificationClick(notification);
        browserNotification.close();
      };

      // Auto-close after delay (except high priority)
      if (notification.priority !== 'high') {
        setTimeout(() => {
          browserNotification.close();
        }, notification.duration || 5000);
      }

      return true;
    } catch (error) {
      console.error('Error sending browser notification:', error);
      return false;
    }
  }

  async sendSystemNotification(notification) {
    // For Electron apps or PWA with system integration
    if (window.electronAPI) {
      return window.electronAPI.showNotification(notification);
    }
    return false;
  }

  sendInAppNotification(notification) {
    const inAppContainer = this.getOrCreateInAppContainer();
    const notificationEl = this.createInAppNotificationElement(notification);
    
    inAppContainer.appendChild(notificationEl);
    
    // Animate in
    requestAnimationFrame(() => {
      notificationEl.classList.add('show');
    });

    // Auto-remove after duration
    setTimeout(() => {
      this.removeInAppNotification(notificationEl);
    }, notification.duration || 5000);

    return true;
  }

  async sendEmailAlert(notification) {
    if (!this.options.enableEmailAlerts || !notification.emailEnabled) {
      return false;
    }

    // In a real app, this would call your email service
    try {
      const emailData = {
        to: this.preferences.email,
        subject: `[Tài chính] ${notification.title}`,
        body: this.formatEmailBody(notification),
        html: this.formatEmailHtml(notification)
      };

      // Simulate email sending
      console.log('Sending email:', emailData);
      return true;
    } catch (error) {
      console.error('Error sending email:', error);
      return false;
    }
  }

  // Specialized Notification Types
  async sendBudgetWarning(category, spent, budget, usage) {
    const warningLevel = this.getBudgetWarningLevel(usage);
    const categoryName = CATEGORY_NAMES[category] || category;
    
    let title, message, priority;
    
    if (usage >= 1.0) {
      title = ' Vượt ngân sách!';
      message = `Đã chi ${formatCurrency(spent)} cho ${categoryName}, vượt ${formatCurrency(spent - budget)}`;
      priority = 'high';
    } else if (usage >= 0.9) {
      title = ' Sắp hết ngân sách';
      message = `Đã chi ${(usage * 100).toFixed(0)}% ngân sách ${categoryName}`;
      priority = 'medium';
    } else if (usage >= 0.8) {
      title = ' Cảnh báo ngân sách';
      message = `Đã chi ${(usage * 100).toFixed(0)}% ngân sách ${categoryName}`;
      priority = 'low';
    }

    if (title) {
      await this.sendNotification({
        id: `budget-warning-${category}`,
        type: 'budget_warning',
        category: 'budget',
        title,
        message,
        priority,
        actions: [
          { action: 'view_budget', title: 'Xem ngân sách' },
          { action: 'adjust_budget', title: 'Điều chỉnh' }
        ],
        data: { category, spent, budget, usage },
        persistent: true
      });
    }
  }

  async sendAchievementNotification(achievement) {
    await this.sendNotification({
      id: `achievement-${achievement.id}`,
      type: 'achievement',
      category: 'gamification',
      title: ' Thành tích mới!',
      message: `Bạn đã đạt được: ${achievement.name}`,
      priority: 'medium',
      duration: 8000,
      actions: [
        { action: 'view_achievements', title: 'Xem thành tích' },
        { action: 'share_achievement', title: 'Chia sẻ' }
      ],
      data: achievement,
      celebratory: true
    });
  }

  async sendSpendingInsight(insight) {
    await this.sendNotification({
      id: `insight-${Date.now()}`,
      type: 'insight',
      category: 'analytics',
      title: ' Thông tin chi tiêu',
      message: insight.message,
      priority: 'low',
      actions: [
        { action: 'view_analytics', title: 'Xem chi tiết' }
      ],
      data: insight,
      educational: true
    });
  }

  async sendReminder(reminder) {
    await this.sendNotification({
      id: `reminder-${reminder.id}`,
      type: 'reminder',
      category: 'reminder',
      title: ' Nhắc nhở',
      message: reminder.message,
      priority: reminder.priority || 'medium',
      actions: [
        { action: 'mark_done', title: 'Đã làm' },
        { action: 'snooze', title: 'Báo lại sau' }
      ],
      data: reminder,
      persistent: true
    });
  }

  // Reminder System
  createReminder(reminderData) {
    const reminder = {
      id: Date.now().toString(),
      title: reminderData.title,
      message: reminderData.message,
      type: reminderData.type, // 'budget_review', 'transaction_entry', 'goal_check', 'custom'
      frequency: reminderData.frequency, // 'once', 'daily', 'weekly', 'monthly'
      scheduledTime: reminderData.scheduledTime,
      nextTrigger: this.calculateNextTrigger(reminderData),
      isEnabled: true,
      createdAt: new Date().toISOString(),
      ...reminderData
    };

    this.reminders.push(reminder);
    this.saveReminders();
    this.scheduleReminder(reminder);
    
    return reminder;
  }

  scheduleReminder(reminder) {
    const now = Date.now();
    const triggerTime = new Date(reminder.nextTrigger).getTime();
    const delay = triggerTime - now;

    if (delay > 0) {
      setTimeout(() => {
        this.triggerReminder(reminder);
      }, Math.min(delay, 2147483647)); // Max setTimeout value
    }
  }

  async triggerReminder(reminder) {
    if (!reminder.isEnabled) return;

    await this.sendReminder(reminder);
    
    // Schedule next occurrence if recurring
    if (reminder.frequency !== 'once') {
      reminder.nextTrigger = this.calculateNextTrigger(reminder);
      this.scheduleReminder(reminder);
      this.saveReminders();
    } else {
      // Mark as completed
      reminder.isEnabled = false;
      this.saveReminders();
    }
  }

  calculateNextTrigger(reminder) {
    const now = new Date();
    const scheduledTime = new Date(reminder.scheduledTime);
    
    let next = new Date(scheduledTime);
    
    switch (reminder.frequency) {
      case 'daily':
        next = new Date(now);
        next.setHours(scheduledTime.getHours(), scheduledTime.getMinutes(), 0, 0);
        if (next <= now) {
          next.setDate(next.getDate() + 1);
        }
        break;
        
      case 'weekly':
        next = new Date(now);
        next.setDate(now.getDate() + (7 - now.getDay() + scheduledTime.getDay()) % 7);
        next.setHours(scheduledTime.getHours(), scheduledTime.getMinutes(), 0, 0);
        if (next <= now) {
          next.setDate(next.getDate() + 7);
        }
        break;
        
      case 'monthly':
        next = new Date(now.getFullYear(), now.getMonth(), scheduledTime.getDate());
        next.setHours(scheduledTime.getHours(), scheduledTime.getMinutes(), 0, 0);
        if (next <= now) {
          next.setMonth(next.getMonth() + 1);
        }
        break;
        
      default: // 'once'
        next = scheduledTime;
    }
    
    return next.toISOString();
  }

  // Smart Insights Generation
  async generateSmartInsights(transactionData, budgetData) {
    const insights = [];
    
    // Spending pattern insights
    const spendingInsights = this.analyzeSpendingPatterns(transactionData);
    insights.push(...spendingInsights);
    
    // Budget insights
    const budgetInsights = this.analyzeBudgetPerformance(budgetData);
    insights.push(...budgetInsights);
    
    // Savings opportunities
    const savingsInsights = this.identifySavingsOpportunities(transactionData);
    insights.push(...savingsInsights);
    
    // Send top insights as notifications
    const topInsights = insights
      .sort((a, b) => b.importance - a.importance)
      .slice(0, 3);
    
    for (const insight of topInsights) {
      if (Math.random() < insight.notificationProbability) {
        await this.sendSpendingInsight(insight);
      }
    }
    
    return insights;
  }

  analyzeSpendingPatterns(transactions) {
    const insights = [];
    
    // Weekend vs weekday spending
    const weekendSpending = transactions.filter(t => {
      const day = new Date(t.date).getDay();
      return day === 0 || day === 6;
    });
    
    const weekdaySpending = transactions.filter(t => {
      const day = new Date(t.date).getDay();
      return day >= 1 && day <= 5;
    });
    
    const weekendAvg = weekendSpending.reduce((sum, t) => sum + Math.abs(t.amount), 0) / Math.max(weekendSpending.length, 1);
    const weekdayAvg = weekdaySpending.reduce((sum, t) => sum + Math.abs(t.amount), 0) / Math.max(weekdaySpending.length, 1);
    
    if (weekendAvg > weekdayAvg * 1.5) {
      insights.push({
        type: 'spending_pattern',
        message: `Bạn chi tiêu cuối tuần nhiều hơn ngày thường ${((weekendAvg / weekdayAvg - 1) * 100).toFixed(0)}%`,
        importance: 7,
        notificationProbability: 0.8,
        actionable: true,
        tips: ['Lập kế hoạch chi tiêu cuối tuần', 'Đặt ngân sách riêng cho cuối tuần']
      });
    }
    
    return insights;
  }

  // UI Components
  getOrCreateInAppContainer() {
    let container = document.getElementById('in-app-notifications');
    if (!container) {
      container = document.createElement('div');
      container.id = 'in-app-notifications';
      container.className = 'in-app-notifications-container';
      document.body.appendChild(container);
    }
    return container;
  }

  createInAppNotificationElement(notification) {
    const el = document.createElement('div');
    el.className = `in-app-notification ${notification.type} ${notification.priority}`;
    el.dataset.notificationId = notification.id;
    
    const icon = this.getNotificationIcon(notification);
    const actions = notification.actions ? this.createActionButtons(notification.actions, notification) : '';
    
    el.innerHTML = `
      <div class="notification-content">
        <div class="notification-icon">${icon}</div>
        <div class="notification-text">
          <div class="notification-title">${notification.title}</div>
          <div class="notification-message">${notification.message}</div>
        </div>
        <button class="notification-close" onclick="this.parentElement.parentElement.remove()">×</button>
      </div>
      ${actions}
    `;
    
    // Add click handler for main notification
    el.addEventListener('click', (e) => {
      if (!e.target.closest('.notification-actions') && !e.target.closest('.notification-close')) {
        this.handleNotificationClick(notification);
      }
    });
    
    return el;
  }

  createActionButtons(actions, notification) {
    if (!actions || actions.length === 0) return '';
    
    return `
      <div class="notification-actions">
        ${actions.map(action => `
          <button class="notification-action" data-action="${action.action}">
            ${action.title}
          </button>
        `).join('')}
      </div>
    `;
  }

  removeInAppNotification(element) {
    element.classList.add('removing');
    setTimeout(() => {
      element.remove();
    }, 300);
  }

  // Notification Management
  shouldSendNotification(notification) {
    // Check quiet hours
    if (this.isQuietHours() && notification.priority !== 'high') {
      return false;
    }
    
    // Check rate limiting
    if (this.isRateLimited(notification)) {
      return false;
    }
    
    // Check user preferences
    if (!this.preferences.categories[notification.category]) {
      return false;
    }
    
    // Check duplicate prevention
    if (this.isDuplicate(notification)) {
      return false;
    }
    
    return true;
  }

  isQuietHours() {
    if (!this.preferences.quietHours.enabled) return false;
    
    const now = new Date();
    const hour = now.getHours();
    const start = this.preferences.quietHours.start;
    const end = this.preferences.quietHours.end;
    
    if (start > end) {
      // Quiet hours cross midnight
      return hour >= start || hour < end;
    } else {
      return hour >= start && hour < end;
    }
  }

  isRateLimited(notification) {
    const recent = this.notificationHistory.filter(n => 
      n.type === notification.type && 
      Date.now() - new Date(n.timestamp).getTime() < 3600000 // 1 hour
    );
    
    const limits = {
      budget_warning: 3,
      achievement: 5,
      insight: 2,
      reminder: 10
    };
    
    return recent.length >= (limits[notification.type] || 5);
  }

  isDuplicate(notification) {
    return this.notificationHistory.some(n => 
      n.id === notification.id || 
      (n.type === notification.type && 
       n.title === notification.title && 
       Date.now() - new Date(n.timestamp).getTime() < 300000) // 5 minutes
    );
  }

  applyPreferences(notification) {
    const prefs = this.preferences;
    
    // Apply sound preference
    if (!prefs.soundEnabled) {
      notification.silent = true;
    }
    
    // Apply vibration preference
    if (!prefs.vibrationEnabled) {
      notification.vibrate = undefined;
    }
    
    // Apply priority filtering
    if (notification.priority === 'low' && prefs.minPriority !== 'low') {
      return null;
    }
    
    return notification;
  }

  // Event Handlers
  handleNotificationClick(notification) {
    // Navigate to relevant section
    switch (notification.type) {
      case 'budget_warning':
        this.onNavigate?.('budgets', { category: notification.data.category });
        break;
      case 'achievement':
        this.onNavigate?.('achievements');
        break;
      case 'insight':
        this.onNavigate?.('analytics');
        break;
      case 'reminder':
        this.handleReminderAction(notification.data, 'view');
        break;
    }
    
    // Mark as read
    this.markAsRead(notification.id);
  }

  handleReminderAction(reminder, action) {
    switch (action) {
      case 'mark_done':
        reminder.isEnabled = false;
        this.saveReminders();
        this.showNotification('Đã đánh dấu hoàn thành', 'success');
        break;
        
      case 'snooze':
        const snoozeTime = new Date();
        snoozeTime.setMinutes(snoozeTime.getMinutes() + 30);
        reminder.nextTrigger = snoozeTime.toISOString();
        this.scheduleReminder(reminder);
        this.saveReminders();
        this.showNotification('Sẽ nhắc lại sau 30 phút', 'info');
        break;
    }
  }

  // Service Worker Integration
  async registerServiceWorker() {
    if ('serviceWorker' in navigator) {
      try {
        const registration = await navigator.serviceWorker.register('/sw-notifications.js');
        console.log('Notification service worker registered');
        
        // Listen for messages from service worker
        navigator.serviceWorker.addEventListener('message', (event) => {
          this.handleServiceWorkerMessage(event.data);
        });
        
      } catch (error) {
        console.error('Failed to register notification service worker:', error);
      }
    }
  }

  handleServiceWorkerMessage(data) {
    switch (data.type) {
      case 'notification_click':
        this.handleNotificationClick(data.notification);
        break;
      case 'notification_action':
        this.handleNotificationAction(data.notification, data.action);
        break;
    }
  }

  // Preferences Management
  loadPreferences() {
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
      minPriority: 'low', // low, medium, high
      email: null
    };
    
    const saved = localStorage.getItem('notification_preferences');
    return saved ? { ...defaults, ...JSON.parse(saved) } : defaults;
  }

  updatePreferences(newPreferences) {
    this.preferences = { ...this.preferences, ...newPreferences };
    localStorage.setItem('notification_preferences', JSON.stringify(this.preferences));
    this.onPreferencesUpdated?.(this.preferences);
  }

  // Data Persistence
  storeNotificationHistory(notification) {
    this.notificationHistory.unshift({
      ...notification,
      timestamp: new Date().toISOString(),
      read: false
    });
    
    // Keep only last 100 notifications
    this.notificationHistory = this.notificationHistory.slice(0, 100);
    
    localStorage.setItem('notification_history', JSON.stringify(this.notificationHistory));
  }

  saveReminders() {
    localStorage.setItem('notification_reminders', JSON.stringify(this.reminders));
  }

  loadReminders() {
    const saved = localStorage.getItem('notification_reminders');
    this.reminders = saved ? JSON.parse(saved) : [];
    
    // Reschedule active reminders
    this.reminders.filter(r => r.isEnabled).forEach(r => {
      this.scheduleReminder(r);
    });
  }

  // Background Monitoring
  startBackgroundMonitoring() {
    // Check for conditions that should trigger notifications
    setInterval(() => {
      this.checkBudgetAlerts();
      this.checkSpendingAnomalies();
      this.checkGoalProgress();
    }, 5 * 60 * 1000); // Check every 5 minutes
  }

  async checkBudgetAlerts() {
    // This would integrate with your budget service
    // Example implementation:
    try {
      const budgets = await this.getBudgetData?.();
      if (!budgets) return;
      
      budgets.forEach(budget => {
        const usage = budget.spent / budget.amount;
        if (usage >= 0.8) {
          this.sendBudgetWarning(budget.category, budget.spent, budget.amount, usage);
        }
      });
    } catch (error) {
      console.error('Error checking budget alerts:', error);
    }
  }

  // Utility Methods
  getNotificationIcon(notification) {
    const icons = {
      budget_warning: '',
      achievement: '',
      insight: '',
      reminder: '',
      system: '',
      error: '',
      success: ''
    };
    
    return icons[notification.type] || icons.system;
  }

  getBudgetWarningLevel(usage) {
    if (usage >= 1.0) return 'critical';
    if (usage >= 0.9) return 'high';
    if (usage >= 0.8) return 'medium';
    return 'low';
  }

  formatEmailBody(notification) {
    return `
${notification.title}

${notification.message}

---
Thời gian: ${formatDate(new Date())}
Loại: ${notification.type}
${notification.data ? `Dữ liệu: ${JSON.stringify(notification.data, null, 2)}` : ''}

Ứng dụng Quản lý Tài chính
    `.trim();
  }

  formatEmailHtml(notification) {
    // Would return formatted HTML email template
    return this.formatEmailBody(notification).replace(/\n/g, '<br>');
  }

  markAsRead(notificationId) {
    const notification = this.notificationHistory.find(n => n.id === notificationId);
    if (notification) {
      notification.read = true;
      localStorage.setItem('notification_history', JSON.stringify(this.notificationHistory));
    }
  }

  showNotification(message, type = 'info') {
    // Integration with existing notification system
    if (window.showNotification) {
      window.showNotification(message, type);
    }
  }

  // Public API
  getNotificationHistory() {
    return this.notificationHistory;
  }

  getActiveReminders() {
    return this.reminders.filter(r => r.isEnabled);
  }

  clearHistory() {
    this.notificationHistory = [];
    localStorage.removeItem('notification_history');
  }

  // Event handlers (to be set by parent app)
  onNotificationSent = null;
  onNavigate = null;
  onPreferencesUpdated = null;
  getBudgetData = null;
}

export default SmartNotificationSystem;