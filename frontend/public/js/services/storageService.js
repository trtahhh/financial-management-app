// Local storage service for offline functionality
import { CONFIG } from '../constants/config.js';

class StorageService {
  constructor() {
    this.prefix = 'finance_app_';
    this.keys = CONFIG.STORAGE_KEYS;
  }

  // Generate prefixed key
  getKey(key) {
    return `${this.prefix}${key}`;
  }

  // Generic get method
  get(key, defaultValue = null) {
    try {
      const item = localStorage.getItem(this.getKey(key));
      return item ? JSON.parse(item) : defaultValue;
    } catch (error) {
      console.error(`Failed to get item from storage: ${key}`, error);
      return defaultValue;
    }
  }

  // Generic set method
  set(key, value) {
    try {
      localStorage.setItem(this.getKey(key), JSON.stringify(value));
      return true;
    } catch (error) {
      console.error(`Failed to set item in storage: ${key}`, error);
      return false;
    }
  }

  // Remove item
  remove(key) {
    try {
      localStorage.removeItem(this.getKey(key));
      return true;
    } catch (error) {
      console.error(`Failed to remove item from storage: ${key}`, error);
      return false;
    }
  }

  // Clear all app data
  clear() {
    try {
      const keys = Object.keys(localStorage);
      keys.forEach(key => {
        if (key.startsWith(this.prefix)) {
          localStorage.removeItem(key);
        }
      });
      return true;
    } catch (error) {
      console.error('Failed to clear storage', error);
      return false;
    }
  }

  // Get storage usage info
  getStorageInfo() {
    let total = 0;
    let used = 0;
    
    try {
      // Calculate used space
      for (let key in localStorage) {
        if (localStorage.hasOwnProperty(key)) {
          used += localStorage[key].length + key.length;
        }
      }

      // Estimate total space (usually 5-10MB)
      total = 5 * 1024 * 1024; // 5MB default estimate

      return {
        used,
        total,
        percentage: (used / total) * 100,
        available: total - used
      };
    } catch (error) {
      console.error('Failed to get storage info', error);
      return { used: 0, total: 0, percentage: 0, available: 0 };
    }
  }

  // Specific methods for cached data
  
  // Transactions cache
  getCachedTransactions() {
    return this.get(this.keys.CACHED_TRANSACTIONS, []);
  }

  setCachedTransactions(transactions) {
    return this.set(this.keys.CACHED_TRANSACTIONS, transactions);
  }

  addCachedTransaction(transaction) {
    const cached = this.getCachedTransactions();
    cached.push({ ...transaction, _cached: true, _timestamp: Date.now() });
    return this.setCachedTransactions(cached);
  }

  updateCachedTransaction(id, updates) {
    const cached = this.getCachedTransactions();
    const index = cached.findIndex(t => t.id === id);
    
    if (index >= 0) {
      cached[index] = { ...cached[index], ...updates, _timestamp: Date.now() };
      return this.setCachedTransactions(cached);
    }
    
    return false;
  }

  removeCachedTransaction(id) {
    const cached = this.getCachedTransactions();
    const filtered = cached.filter(t => t.id !== id);
    return this.setCachedTransactions(filtered);
  }

  // Budgets cache
  getCachedBudgets() {
    return this.get(this.keys.CACHED_BUDGETS, []);
  }

  setCachedBudgets(budgets) {
    return this.set(this.keys.CACHED_BUDGETS, budgets);
  }

  // Goals cache
  getCachedGoals() {
    return this.get(this.keys.CACHED_GOALS, []);
  }

  setCachedGoals(goals) {
    return this.set(this.keys.CACHED_GOALS, goals);
  }

  // User preferences
  getUserPreferences() {
    return this.get(this.keys.USER_PREFERENCES, {
      theme: 'light',
      currency: 'VND',
      language: 'vi',
      notifications: true,
      autoSync: true,
      defaultCategory: 'other',
      defaultPaymentMethod: 'cash'
    });
  }

  setUserPreferences(preferences) {
    const current = this.getUserPreferences();
    return this.set(this.keys.USER_PREFERENCES, { ...current, ...preferences });
  }

  // Sync status tracking
  getSyncStatus() {
    return this.get('sync_status', {
      lastSync: null,
      pendingTransactions: 0,
      pendingBudgets: 0,
      pendingGoals: 0,
      isOnline: navigator.onLine
    });
  }

  setSyncStatus(status) {
    const current = this.getSyncStatus();
    return this.set('sync_status', { ...current, ...status });
  }

  // Offline queue management
  getOfflineQueue() {
    return this.get('offline_queue', []);
  }

  addToOfflineQueue(action) {
    const queue = this.getOfflineQueue();
    const queueItem = {
      id: Date.now().toString(),
      action,
      timestamp: new Date().toISOString(),
      retries: 0
    };
    
    queue.push(queueItem);
    return this.set('offline_queue', queue);
  }

  removeFromOfflineQueue(id) {
    const queue = this.getOfflineQueue();
    const filtered = queue.filter(item => item.id !== id);
    return this.set('offline_queue', filtered);
  }

  clearOfflineQueue() {
    return this.set('offline_queue', []);
  }

  // Export data for backup
  exportData() {
    const data = {
      transactions: this.getCachedTransactions(),
      budgets: this.getCachedBudgets(),
      goals: this.getCachedGoals(),
      preferences: this.getUserPreferences(),
      exportDate: new Date().toISOString(),
      version: '1.0.0'
    };

    return data;
  }

  // Import data from backup
  importData(data) {
    try {
      if (data.transactions) this.setCachedTransactions(data.transactions);
      if (data.budgets) this.setCachedBudgets(data.budgets);
      if (data.goals) this.setCachedGoals(data.goals);
      if (data.preferences) this.setUserPreferences(data.preferences);
      
      return true;
    } catch (error) {
      console.error('Failed to import data', error);
      return false;
    }
  }
}

// Create singleton instance
export const storageService = new StorageService();
export default StorageService;