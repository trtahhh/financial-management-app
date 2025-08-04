/**
 * 🔗 INTEGRATION SYSTEM - Hệ thống liên kết các chức năng
 * Quản lý tất cả các liên kết và tác động giữa các module
 */

// Global variables
const INTEGRATION_CONFIG = {
  API_BASE: 'http://localhost:8080/api',
  USER_ID: 1,
  NOTIFICATIONS_ENABLED: true,
  AUTO_REFRESH_DASHBOARD: true
};

/**
 * 🔄 CROSS-MODULE NOTIFICATIONS - Thông báo liên module
 */
class IntegrationNotifications {
  static show(message, type = 'info', duration = 5000) {
    if (!INTEGRATION_CONFIG.NOTIFICATIONS_ENABLED) return;
    
    const notification = document.createElement('div');
    notification.className = `alert alert-${type} alert-dismissible fade show integration-notification`;
    notification.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 9999;
      min-width: 350px;
      max-width: 500px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    `;
    
    notification.innerHTML = `
      <div class="d-flex align-items-center">
        <i class="fas ${this.getIcon(type)} me-2"></i>
        <div class="flex-grow-1">${message}</div>
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
      </div>
    `;
    
    document.body.appendChild(notification);
    
    // Auto remove
    setTimeout(() => {
      if (notification.parentNode) {
        notification.remove();
      }
    }, duration);
  }
  
  static getIcon(type) {
    const icons = {
      success: 'fa-check-circle',
      warning: 'fa-exclamation-triangle',
      danger: 'fa-times-circle',
      info: 'fa-info-circle'
    };
    return icons[type] || 'fa-info-circle';
  }
  
  static showTransactionImpact(transaction, impacts) {
    let message = `<strong>Giao dịch ${transaction.type === 'THU' ? 'thu nhập' : 'chi tiêu'} đã được lưu!</strong><br>`;
    
    if (impacts.budgetUpdate) {
      message += `💳 ${impacts.budgetUpdate}<br>`;
    }
    if (impacts.goalUpdate) {
      message += `🎯 ${impacts.goalUpdate}<br>`;
    }
    if (impacts.walletUpdate) {
      message += `💰 ${impacts.walletUpdate}`;
    }
    
    this.show(message, 'success', 7000);
  }
  
  static showBudgetAlert(categoryName, percentage, exceeded = false) {
    const type = exceeded ? 'danger' : 'warning';
    const icon = exceeded ? '🚨' : '⚠️';
    const message = `${icon} <strong>${categoryName}</strong><br>Đã sử dụng ${percentage}% ngân sách${exceeded ? ' - Đã vượt giới hạn!' : ''}`;
    
    this.show(message, type, 10000);
  }
}

/**
 * 🔗 BUDGET INTEGRATION - Tích hợp ngân sách
 */
class BudgetIntegration {
  static async updateUsageFromTransaction(transaction) {
    if (transaction.type !== 'CHI') return null;
    
    try {
      const response = await fetch(`${INTEGRATION_CONFIG.API_BASE}/budgets/updateUsage`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('authToken') || ''}`
        },
        body: JSON.stringify({
          categoryId: transaction.categoryId,
          amount: transaction.amount,
          userId: INTEGRATION_CONFIG.USER_ID,
          month: new Date(transaction.date).getMonth() + 1,
          year: new Date(transaction.date).getFullYear()
        })
      });
      
      if (response.ok) {
        const result = await response.json();
        console.log("✅ Budget usage updated:", result);
        
        // Check for budget alerts
        this.checkBudgetAlert(transaction.categoryId, transaction.amount);
        
        return `Ngân sách ${result.categoryName} đã cập nhật (${result.usagePercent}% đã sử dụng)`;
      }
    } catch (error) {
      console.error("❌ Failed to update budget usage:", error);
    }
    
    return null;
  }
  
  static async checkBudgetAlert(categoryId, amount) {
    try {
      const response = await fetch(`${INTEGRATION_CONFIG.API_BASE}/budgets/check/${categoryId}?userId=${INTEGRATION_CONFIG.USER_ID}&amount=${amount}`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('authToken') || ''}`
        }
      });
      
      if (response.ok) {
        const alertData = await response.json();
        
        if (alertData.exceeded || alertData.nearLimit) {
          IntegrationNotifications.showBudgetAlert(
            alertData.categoryName, 
            alertData.percentage, 
            alertData.exceeded
          );
        }
      }
    } catch (error) {
      console.error("❌ Failed to check budget alert:", error);
    }
  }
  
  static async getCurrentUsage(categoryId, month, year) {
    try {
      const response = await fetch(`${INTEGRATION_CONFIG.API_BASE}/budgets/usage/${categoryId}?userId=${INTEGRATION_CONFIG.USER_ID}&month=${month}&year=${year}`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('authToken') || ''}`
        }
      });
      
      if (response.ok) {
        return await response.json();
      }
    } catch (error) {
      console.error("❌ Failed to get budget usage:", error);
    }
    
    return null;
  }
}

/**
 * 🎯 GOALS INTEGRATION - Tích hợp mục tiêu
 */
class GoalsIntegration {
  static async updateProgressFromTransaction(transaction) {
    if (transaction.type !== 'THU') return null;
    
    try {
      const response = await fetch(`${INTEGRATION_CONFIG.API_BASE}/goals/updateProgress`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('authToken') || ''}`
        },
        body: JSON.stringify({
          userId: INTEGRATION_CONFIG.USER_ID,
          amount: transaction.amount,
          date: transaction.date
        })
      });
      
      if (response.ok) {
        const result = await response.json();
        console.log("✅ Goals progress updated:", result);
        
        if (result.goalCompleted) {
          IntegrationNotifications.show(
            `🎉 <strong>Chúc mừng!</strong><br>Bạn đã hoàn thành mục tiêu: ${result.goalName}`,
            'success',
            10000
          );
        }
        
        return `Tiến độ mục tiêu đã cập nhật (+${transaction.amount.toLocaleString('vi-VN')}đ)`;
      }
    } catch (error) {
      console.error("❌ Failed to update goals progress:", error);
    }
    
    return null;
  }
  
  static async getActiveGoals() {
    try {
      const response = await fetch(`${INTEGRATION_CONFIG.API_BASE}/goals?userId=${INTEGRATION_CONFIG.USER_ID}&status=active`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('authToken') || ''}`
        }
      });
      
      if (response.ok) {
        return await response.json();
      }
    } catch (error) {
      console.error("❌ Failed to get active goals:", error);
    }
    
    return [];
  }
}

/**
 * 💰 WALLET INTEGRATION - Tích hợp ví
 */
class WalletIntegration {
  static async updateBalanceFromTransaction(transaction) {
    const balanceChange = transaction.type === 'THU' ? transaction.amount : -transaction.amount;
    
    try {
      const response = await fetch(`${INTEGRATION_CONFIG.API_BASE}/wallets/updateBalance`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('authToken') || ''}`
        },
        body: JSON.stringify({
          userId: INTEGRATION_CONFIG.USER_ID,
          balanceChange: balanceChange,
          transactionId: transaction.id,
          walletId: transaction.walletId || 1 // Default wallet
        })
      });
      
      if (response.ok) {
        const result = await response.json();
        console.log("✅ Wallet balance updated:", result);
        
        if (result.newBalance < 0) {
          IntegrationNotifications.show(
            `⚠️ <strong>Cảnh báo!</strong><br>Số dư ví ${result.walletName} đã âm: ${result.newBalance.toLocaleString('vi-VN')}đ`,
            'warning',
            8000
          );
        }
        
        return `Số dư ví cập nhật: ${result.newBalance.toLocaleString('vi-VN')}đ`;
      }
    } catch (error) {
      console.error("❌ Failed to update wallet balance:", error);
    }
    
    return null;
  }
  
  static async getCurrentBalance(walletId = null) {
    const url = walletId 
      ? `${INTEGRATION_CONFIG.API_BASE}/wallets/${walletId}/balance`
      : `${INTEGRATION_CONFIG.API_BASE}/wallets/total-balance?userId=${INTEGRATION_CONFIG.USER_ID}`;
    
    try {
      const response = await fetch(url, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('authToken') || ''}`
        }
      });
      
      if (response.ok) {
        return await response.json();
      }
    } catch (error) {
      console.error("❌ Failed to get wallet balance:", error);
    }
    
    return null;
  }
}

/**
 * 🔄 MAIN INTEGRATION ORCHESTRATOR - Điều phối chính
 */
class FinancialIntegration {
  static async processTransaction(transaction, isNew = true) {
    console.log("🔗 Processing transaction integration:", transaction);
    
    const impacts = {
      budgetUpdate: null,
      goalUpdate: null,
      walletUpdate: null
    };
    
    // Process all integrations in parallel
    const promises = [];
    
    if (transaction.type === 'CHI' && transaction.categoryId) {
      promises.push(
        BudgetIntegration.updateUsageFromTransaction(transaction)
          .then(result => impacts.budgetUpdate = result)
      );
    }
    
    if (transaction.type === 'THU') {
      promises.push(
        GoalsIntegration.updateProgressFromTransaction(transaction)
          .then(result => impacts.goalUpdate = result)
      );
    }
    
    promises.push(
      WalletIntegration.updateBalanceFromTransaction(transaction)
        .then(result => impacts.walletUpdate = result)
    );
    
    try {
      await Promise.all(promises);
      
      // Show comprehensive notification
      if (isNew) {
        IntegrationNotifications.showTransactionImpact(transaction, impacts);
      }
      
      // Auto refresh dashboard if enabled
      if (INTEGRATION_CONFIG.AUTO_REFRESH_DASHBOARD && 
          window.location.pathname.includes('dashboard')) {
        setTimeout(() => {
          if (typeof updateDashboard === 'function') {
            updateDashboard();
          }
        }, 1000);
      }
      
      return impacts;
    } catch (error) {
      console.error("🚨 Integration processing failed:", error);
      IntegrationNotifications.show(
        "Có lỗi xảy ra khi cập nhật dữ liệu liên quan. Vui lòng kiểm tra lại.",
        'warning'
      );
      return impacts;
    }
  }
  
  static async getIntegratedDashboardData(month, year) {
    console.log("📊 Fetching integrated dashboard data...");
    
    try {
      const promises = [
        fetch(`${INTEGRATION_CONFIG.API_BASE}/statistics/summary?userId=${INTEGRATION_CONFIG.USER_ID}&month=${month}&year=${year}`),
        fetch(`${INTEGRATION_CONFIG.API_BASE}/transactions?userId=${INTEGRATION_CONFIG.USER_ID}`),
        fetch(`${INTEGRATION_CONFIG.API_BASE}/categories?userId=${INTEGRATION_CONFIG.USER_ID}`),
        fetch(`${INTEGRATION_CONFIG.API_BASE}/budgets?userId=${INTEGRATION_CONFIG.USER_ID}&month=${month}&year=${year}`),
        GoalsIntegration.getActiveGoals(),
        WalletIntegration.getCurrentBalance()
      ];
      
      const results = await Promise.all(promises);
      
      const [statsRes, transactionsRes, categoriesRes, budgetsRes] = results;
      const [goals, walletBalance] = results.slice(4);
      
      const data = {
        statistics: statsRes.ok ? await statsRes.json() : {},
        transactions: transactionsRes.ok ? await transactionsRes.json() : [],
        categories: categoriesRes.ok ? await categoriesRes.json() : [],
        budgets: budgetsRes.ok ? await budgetsRes.json() : [],
        goals: goals || [],
        walletBalance: walletBalance || { balance: 0 }
      };
      
      console.log("✅ Integrated dashboard data loaded:", data);
      return data;
    } catch (error) {
      console.error("🚨 Failed to load integrated dashboard data:", error);
      throw error;
    }
  }
}

// 🌐 GLOBAL INTEGRATION HOOKS - Hooks tích hợp toàn cục

// Hook vào transaction saving
window.addEventListener('transactionSaved', (event) => {
  FinancialIntegration.processTransaction(event.detail.transaction, true);
});

// Hook vào transaction editing
window.addEventListener('transactionUpdated', (event) => {
  FinancialIntegration.processTransaction(event.detail.transaction, false);
});

// Global utility function
window.FinancialIntegration = FinancialIntegration;
window.IntegrationNotifications = IntegrationNotifications;

console.log("🔗 Financial Integration System loaded successfully!");
