// Service Worker for Smart Notifications
const CACHE_NAME = 'financial-app-notifications-v1';
const NOTIFICATION_TAG = 'financial-app';

// Install event
self.addEventListener('install', (event) => {
  console.log('Notification service worker installed');
  self.skipWaiting();
});

// Activate event
self.addEventListener('activate', (event) => {
  console.log('Notification service worker activated');
  event.waitUntil(self.clients.claim());
});

// Background sync for notifications
self.addEventListener('sync', (event) => {
  if (event.tag === 'background-notification-check') {
    event.waitUntil(checkForNotifications());
  }
});

// Push notification handler
self.addEventListener('push', (event) => {
  console.log('Push notification received');
  
  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch (error) {
    console.error('Error parsing push data:', error);
  }
  
  const options = {
    body: data.message || 'Bạn có thông báo mới',
    icon: '/icons/notification-icon.png',
    badge: '/icons/badge-icon.png',
    tag: data.tag || NOTIFICATION_TAG,
    requireInteraction: data.requireInteraction || false,
    actions: data.actions || [],
    data: data.data || {},
    timestamp: Date.now(),
    silent: data.silent || false,
    vibrate: data.vibrate || [200, 100, 200]
  };
  
  event.waitUntil(
    self.registration.showNotification(data.title || 'Quản lý Tài chính', options)
  );
});

// Notification click handler
self.addEventListener('notificationclick', (event) => {
  console.log('Notification clicked:', event);
  
  const notification = event.notification;
  const action = event.action;
  const data = notification.data || {};
  
  notification.close();
  
  // Handle action buttons
  if (action) {
    handleNotificationAction(notification, action, data);
  } else {
    // Handle main notification click
    handleNotificationClick(notification, data);
  }
});

// Notification close handler
self.addEventListener('notificationclose', (event) => {
  console.log('Notification closed:', event.notification.data);
  
  // Send analytics about notification dismissal
  sendMessageToClient({
    type: 'notification_dismissed',
    notificationId: event.notification.data.id
  });
});

// Handle notification click
async function handleNotificationClick(notification, data) {
  const urlToOpen = getUrlForNotification(data);
  
  // Try to find an existing window/tab with our app
  const windowClients = await self.clients.matchAll({
    type: 'window',
    includeUncontrolled: true
  });
  
  let clientToFocus = null;
  
  // Look for existing tab with our app
  for (let client of windowClients) {
    if (client.url.includes(self.location.origin)) {
      clientToFocus = client;
      break;
    }
  }
  
  if (clientToFocus) {
    // Focus existing tab and navigate
    await clientToFocus.focus();
    await clientToFocus.navigate(urlToOpen);
    
    // Send message to client
    sendMessageToClient({
      type: 'notification_click',
      notification: data,
      action: null
    }, clientToFocus);
  } else {
    // Open new tab
    await self.clients.openWindow(urlToOpen);
  }
}

// Handle notification action buttons
async function handleNotificationAction(notification, action, data) {
  console.log('Notification action:', action, data);
  
  switch (action) {
    case 'view_budget':
      await handleNotificationClick(notification, {
        ...data,
        targetPage: 'budgets',
        targetCategory: data.category
      });
      break;
      
    case 'adjust_budget':
      await handleNotificationClick(notification, {
        ...data,
        targetPage: 'budgets',
        targetAction: 'edit',
        targetCategory: data.category
      });
      break;
      
    case 'view_achievements':
      await handleNotificationClick(notification, {
        ...data,
        targetPage: 'achievements'
      });
      break;
      
    case 'share_achievement':
      await shareAchievement(data);
      break;
      
    case 'view_analytics':
      await handleNotificationClick(notification, {
        ...data,
        targetPage: 'analytics'
      });
      break;
      
    case 'mark_done':
      await markReminderDone(data);
      break;
      
    case 'snooze':
      await snoozeReminder(data);
      break;
      
    case 'quick_add':
      await handleNotificationClick(notification, {
        ...data,
        targetPage: 'transactions',
        targetAction: 'add'
      });
      break;
      
    default:
      console.log('Unknown action:', action);
  }
  
  // Send action to client
  sendMessageToClient({
    type: 'notification_action',
    notification: data,
    action: action
  });
}

// Get URL for notification type
function getUrlForNotification(data) {
  const baseUrl = self.location.origin;
  
  if (data.targetPage) {
    let url = `${baseUrl}/#/${data.targetPage}`;
    
    if (data.targetCategory) {
      url += `?category=${data.targetCategory}`;
    }
    
    if (data.targetAction) {
      url += `${url.includes('?') ? '&' : '?'}action=${data.targetAction}`;
    }
    
    return url;
  }
  
  // Default routing based on notification type
  switch (data.type) {
    case 'budget_warning':
      return `${baseUrl}/#/budgets?category=${data.category}`;
    case 'achievement':
      return `${baseUrl}/#/achievements`;
    case 'insight':
      return `${baseUrl}/#/analytics`;
    case 'reminder':
      return `${baseUrl}/#/dashboard`;
    default:
      return baseUrl;
  }
}

// Background notification check
async function checkForNotifications() {
  console.log('Checking for background notifications...');
  
  try {
    // In a real app, this would fetch from your API
    const response = await fetch('/api/notifications/check', {
      method: 'GET',
      headers: {
        'Authorization': await getAuthToken()
      }
    });
    
    if (response.ok) {
      const notifications = await response.json();
      
      for (const notification of notifications) {
        await self.registration.showNotification(notification.title, {
          body: notification.message,
          icon: '/icons/notification-icon.png',
          badge: '/icons/badge-icon.png',
          tag: notification.tag || NOTIFICATION_TAG,
          data: notification.data,
          actions: notification.actions || []
        });
      }
    }
  } catch (error) {
    console.error('Error checking for notifications:', error);
  }
}

// Action handlers
async function shareAchievement(data) {
  if (navigator.share) {
    try {
      await navigator.share({
        title: ' Tôi vừa đạt được thành tích mới!',
        text: `Tôi vừa đạt được: ${data.name} trong ứng dụng quản lý tài chính!`,
        url: self.location.origin
      });
    } catch (error) {
      console.log('Share cancelled or failed:', error);
    }
  } else {
    // Fallback to copying to clipboard
    await navigator.clipboard.writeText(
      ` Tôi vừa đạt được: ${data.name} trong ứng dụng quản lý tài chính! ${self.location.origin}`
    );
  }
}

async function markReminderDone(data) {
  try {
    await fetch('/api/reminders/mark-done', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': await getAuthToken()
      },
      body: JSON.stringify({ reminderId: data.id })
    });
    
    // Show confirmation notification
    await self.registration.showNotification(' Đã hoàn thành', {
      body: 'Nhắc nhở đã được đánh dấu hoàn thành',
      tag: 'reminder-done',
      requireInteraction: false
    });
    
  } catch (error) {
    console.error('Error marking reminder as done:', error);
  }
}

async function snoozeReminder(data) {
  try {
    await fetch('/api/reminders/snooze', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': await getAuthToken()
      },
      body: JSON.stringify({ 
        reminderId: data.id,
        snoozeMinutes: 30
      })
    });
    
    // Show confirmation notification
    await self.registration.showNotification('⏰ Đã báo lại', {
      body: 'Sẽ nhắc lại sau 30 phút',
      tag: 'reminder-snoozed',
      requireInteraction: false
    });
    
  } catch (error) {
    console.error('Error snoozing reminder:', error);
  }
}

// Utility functions
async function getAuthToken() {
  // In a real app, get auth token from IndexedDB or other storage
  return 'mock-auth-token';
}

async function sendMessageToClient(message, client = null) {
  if (client) {
    client.postMessage(message);
  } else {
    // Send to all clients
    const clients = await self.clients.matchAll();
    clients.forEach(client => {
      client.postMessage(message);
    });
  }
}

// Periodic notification scheduling
async function schedulePeriodicNotifications() {
  // Register background sync for periodic checks
  if ('sync' in self.registration) {
    try {
      await self.registration.sync.register('background-notification-check');
    } catch (error) {
      console.error('Background sync registration failed:', error);
    }
  }
}

// Budget alert helpers
async function checkBudgetAlerts() {
  try {
    const response = await fetch('/api/budgets/check-alerts', {
      method: 'GET',
      headers: {
        'Authorization': await getAuthToken()
      }
    });
    
    if (response.ok) {
      const alerts = await response.json();
      
      for (const alert of alerts) {
        await self.registration.showNotification(alert.title, {
          body: alert.message,
          icon: '/icons/warning-icon.png',
          tag: `budget-alert-${alert.category}`,
          requireInteraction: alert.priority === 'high',
          actions: [
            { action: 'view_budget', title: 'Xem ngân sách' },
            { action: 'adjust_budget', title: 'Điều chỉnh' }
          ],
          data: alert
        });
      }
    }
  } catch (error) {
    console.error('Error checking budget alerts:', error);
  }
}

// Goal reminder helpers
async function checkGoalReminders() {
  try {
    const response = await fetch('/api/goals/check-reminders', {
      method: 'GET',
      headers: {
        'Authorization': await getAuthToken()
      }
    });
    
    if (response.ok) {
      const reminders = await response.json();
      
      for (const reminder of reminders) {
        await self.registration.showNotification(' Nhắc nhở mục tiêu', {
          body: reminder.message,
          icon: '/icons/goal-icon.png',
          tag: `goal-reminder-${reminder.goalId}`,
          actions: [
            { action: 'view_goals', title: 'Xem mục tiêu' },
            { action: 'update_progress', title: 'Cập nhật tiến độ' }
          ],
          data: reminder
        });
      }
    }
  } catch (error) {
    console.error('Error checking goal reminders:', error);
  }
}

// Spending insights
async function generateSpendingInsights() {
  try {
    const response = await fetch('/api/analytics/insights', {
      method: 'GET',
      headers: {
        'Authorization': await getAuthToken()
      }
    });
    
    if (response.ok) {
      const insights = await response.json();
      
      // Only show the most important insight
      const topInsight = insights
        .sort((a, b) => b.importance - a.importance)[0];
      
      if (topInsight && topInsight.showNotification) {
        await self.registration.showNotification(' Thông tin chi tiêu', {
          body: topInsight.message,
          icon: '/icons/insight-icon.png',
          tag: 'spending-insight',
          actions: [
            { action: 'view_analytics', title: 'Xem chi tiết' }
          ],
          data: topInsight
        });
      }
    }
  } catch (error) {
    console.error('Error generating insights:', error);
  }
}

// Initialize periodic checks
self.addEventListener('activate', (event) => {
  event.waitUntil(schedulePeriodicNotifications());
});

// Handle app update notifications
self.addEventListener('message', (event) => {
  if (event.data.type === 'app_updated') {
    self.registration.showNotification(' Ứng dụng đã được cập nhật', {
      body: 'Khởi động lại để sử dụng tính năng mới',
      icon: '/icons/update-icon.png',
      tag: 'app-update',
      requireInteraction: true,
      actions: [
        { action: 'reload_app', title: 'Khởi động lại' }
      ]
    });
  }
});

// Debug logging for development
if (self.location.hostname === 'localhost') {
  console.log('Notification service worker loaded in development mode');
}