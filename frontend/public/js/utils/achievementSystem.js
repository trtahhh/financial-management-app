// Achievement System for Gamification
import { formatCurrency } from '../utils/currencyHelpers.js';
import { CONFIG } from '../constants/config.js';
import { COLORS } from '../constants/colors.js';

class AchievementSystem {
  constructor(options = {}) {
    this.options = {
      enableNotifications: true,
      autoSave: true,
      showProgress: true,
      ...options
    };
    
    this.achievements = new Map();
    this.userProgress = {
      unlockedAchievements: new Set(),
      stats: {
        totalTransactions: 0,
        totalSaved: 0,
        streakDays: 0,
        budgetsCreated: 0,
        goalsCompleted: 0,
        categoriesUsed: new Set(),
        monthsTracked: 0,
        perfectBudgetMonths: 0,
        earlyGoalsCompleted: 0,
        savingsStreaks: 0
      }
    };
    
    this.init();
  }

  init() {
    this.defineAchievements();
    this.loadProgress();
  }

  defineAchievements() {
    const achievements = [
      // Beginner Achievements (Bronze)
      {
        id: 'first-transaction',
        title: 'Bước đầu tiên',
        description: 'Ghi chép giao dịch đầu tiên',
        category: 'beginner',
        tier: 'bronze',
        xpReward: 50,
        condition: (stats) => stats.totalTransactions >= 1,
        icon: '🎯'
      },
      {
        id: 'first-week',
        title: 'Tuần đầu tiên',
        description: 'Ghi chép 7 ngày liên tục',
        category: 'streak',
        tier: 'bronze',
        xpReward: 100,
        condition: (stats) => stats.streakDays >= 7,
        icon: '🗓️'
      },
      {
        id: 'first-budget',
        title: 'Người lập kế hoạch',
        description: 'Tạo ngân sách đầu tiên',
        category: 'planning',
        tier: 'bronze',
        xpReward: 75,
        condition: (stats) => stats.budgetsCreated >= 1,
        icon: '📊'
      },
      {
        id: 'category-explorer',
        title: 'Khám phá danh mục',
        description: 'Sử dụng 5 danh mục khác nhau',
        category: 'variety',
        tier: 'bronze',
        xpReward: 100,
        condition: (stats) => stats.categoriesUsed.size >= 5,
        icon: '🏷️'
      },

      // Intermediate Achievements (Silver)
      {
        id: 'consistent-tracker',
        title: 'Người kiên trì',
        description: 'Ghi chép 30 ngày liên tục',
        category: 'streak',
        tier: 'silver',
        xpReward: 300,
        condition: (stats) => stats.streakDays >= 30,
        icon: '🔥'
      },
      {
        id: 'saver-starter',
        title: 'Bắt đầu tiết kiệm',
        description: 'Tiết kiệm được 1 triệu đồng',
        category: 'savings',
        tier: 'silver',
        xpReward: 200,
        condition: (stats) => stats.totalSaved >= 1000000,
        icon: '💰'
      },
      {
        id: 'budget-master',
        title: 'Chủ ngân sách',
        description: 'Tạo ngân sách cho 8 danh mục',
        category: 'planning',
        tier: 'silver',
        xpReward: 250,
        condition: (stats) => stats.budgetsCreated >= 8,
        icon: '📈'
      },
      {
        id: 'goal-achiever',
        title: 'Người đạt mục tiêu',
        description: 'Hoàn thành 3 mục tiêu tiết kiệm',
        category: 'goals',
        tier: 'silver',
        xpReward: 300,
        condition: (stats) => stats.goalsCompleted >= 3,
        icon: '🎯'
      },
      {
        id: 'monthly-tracker',
        title: 'Theo dõi hàng tháng',
        description: 'Ghi chép liên tục 3 tháng',
        category: 'consistency',
        tier: 'silver',
        xpReward: 400,
        condition: (stats) => stats.monthsTracked >= 3,
        icon: '📅'
      },

      // Advanced Achievements (Gold)
      {
        id: 'dedication-master',
        title: 'Bậc thầy kiên trì',
        description: 'Ghi chép 100 ngày liên tục',
        category: 'streak',
        tier: 'gold',
        xpReward: 800,
        condition: (stats) => stats.streakDays >= 100,
        icon: '🏆'
      },
      {
        id: 'millionaire-saver',
        title: 'Triệu phú tiết kiệm',
        description: 'Tiết kiệm được 10 triệu đồng',
        category: 'savings',
        tier: 'gold',
        xpReward: 600,
        condition: (stats) => stats.totalSaved >= 10000000,
        icon: '💎'
      },
      {
        id: 'perfect-budgeter',
        title: 'Ngân sách hoàn hảo',
        description: 'Không vượt ngân sách 6 tháng liên tục',
        category: 'planning',
        tier: 'gold',
        xpReward: 700,
        condition: (stats) => stats.perfectBudgetMonths >= 6,
        icon: '⚡'
      },
      {
        id: 'early-achiever',
        title: 'Đạt mục tiêu sớm',
        description: 'Hoàn thành 5 mục tiêu trước hạn',
        category: 'goals',
        tier: 'gold',
        xpReward: 500,
        condition: (stats) => stats.earlyGoalsCompleted >= 5,
        icon: '🚀'
      },
      {
        id: 'year-tracker',
        title: 'Theo dõi cả năm',
        description: 'Ghi chép liên tục 12 tháng',
        category: 'consistency',
        tier: 'gold',
        xpReward: 1000,
        condition: (stats) => stats.monthsTracked >= 12,
        icon: '🌟'
      },

      // Master Achievements (Diamond)
      {
        id: 'ultimate-streaker',
        title: 'Kỷ lục kiên trì',
        description: 'Ghi chép 365 ngày liên tục',
        category: 'streak',
        tier: 'diamond',
        xpReward: 2000,
        condition: (stats) => stats.streakDays >= 365,
        icon: '👑'
      },
      {
        id: 'savings-legend',
        title: 'Huyền thoại tiết kiệm',
        description: 'Tiết kiệm được 100 triệu đồng',
        category: 'savings',
        tier: 'diamond',
        xpReward: 1500,
        condition: (stats) => stats.totalSaved >= 100000000,
        icon: '🏰'
      },
      {
        id: 'financial-guru',
        title: 'Bậc thầy tài chính',
        description: 'Mở khóa tất cả thành tích khác',
        category: 'mastery',
        tier: 'diamond',
        xpReward: 3000,
        condition: (stats, achievements) => {
          const totalAchievements = Array.from(achievements.values()).length - 1; // Exclude this achievement
          return achievements.size >= totalAchievements;
        },
        icon: '🎖️'
      }
    ];

    achievements.forEach(achievement => {
      this.achievements.set(achievement.id, achievement);
    });
  }

  // Public methods for updating stats
  recordTransaction(transaction) {
    this.userProgress.stats.totalTransactions++;
    this.userProgress.stats.categoriesUsed.add(transaction.category);
    
    if (transaction.type === 'income') {
      // Income doesn't directly add to savings, but affects the calculation
    } else if (transaction.type === 'expense') {
      // Expense reduces potential savings
    }
    
    this.checkAchievements();
    this.saveProgress();
  }

  updateStreak(days) {
    this.userProgress.stats.streakDays = days;
    this.checkAchievements();
    this.saveProgress();
  }

  recordBudgetCreation() {
    this.userProgress.stats.budgetsCreated++;
    this.checkAchievements();
    this.saveProgress();
  }

  recordGoalCompletion(isEarly = false) {
    this.userProgress.stats.goalsCompleted++;
    if (isEarly) {
      this.userProgress.stats.earlyGoalsCompleted++;
    }
    this.checkAchievements();
    this.saveProgress();
  }

  recordMonthlyTracking() {
    this.userProgress.stats.monthsTracked++;
    this.checkAchievements();
    this.saveProgress();
  }

  recordPerfectBudgetMonth() {
    this.userProgress.stats.perfectBudgetMonths++;
    this.checkAchievements();
    this.saveProgress();
  }

  updateSavings(totalSaved) {
    this.userProgress.stats.totalSaved = totalSaved;
    this.checkAchievements();
    this.saveProgress();
  }

  // Achievement checking
  checkAchievements() {
    const newAchievements = [];
    
    for (const [id, achievement] of this.achievements.entries()) {
      if (!this.userProgress.unlockedAchievements.has(id)) {
        if (achievement.condition(this.userProgress.stats, this.userProgress.unlockedAchievements)) {
          this.unlockAchievement(id);
          newAchievements.push(achievement);
        }
      }
    }
    
    return newAchievements;
  }

  unlockAchievement(achievementId) {
    const achievement = this.achievements.get(achievementId);
    if (!achievement) return false;

    this.userProgress.unlockedAchievements.add(achievementId);
    
    if (this.options.enableNotifications) {
      this.showAchievementNotification(achievement);
    }
    
    this.onAchievementUnlocked?.(achievement);
    return true;
  }

  showAchievementNotification(achievement) {
    // Create achievement notification
    const notification = document.createElement('div');
    notification.className = 'achievement-notification';
    notification.innerHTML = `
      <div class="achievement-notification-content">
        <div class="achievement-icon ${achievement.tier}">
          ${achievement.icon}
        </div>
        <div class="achievement-details">
          <h4 class="achievement-title">Thành tích mới!</h4>
          <p class="achievement-name">${achievement.title}</p>
          <p class="achievement-desc">${achievement.description}</p>
          <p class="achievement-xp">+${achievement.xpReward} XP</p>
        </div>
        <button class="achievement-close" onclick="this.parentElement.parentElement.remove()">×</button>
      </div>
    `;

    // Add to page
    document.body.appendChild(notification);

    // Auto remove after 5 seconds
    setTimeout(() => {
      if (notification.parentElement) {
        notification.remove();
      }
    }, 5000);

    // Add animation
    requestAnimationFrame(() => {
      notification.classList.add('show');
    });
  }

  // Getters for UI
  getUnlockedAchievements() {
    return Array.from(this.userProgress.unlockedAchievements)
      .map(id => this.achievements.get(id))
      .filter(Boolean);
  }

  getProgressAchievements() {
    const achievements = [];
    
    for (const [id, achievement] of this.achievements.entries()) {
      const isUnlocked = this.userProgress.unlockedAchievements.has(id);
      const progress = this.calculateProgress(achievement);
      
      achievements.push({
        ...achievement,
        isUnlocked,
        progress: Math.min(progress, 100)
      });
    }
    
    return achievements.sort((a, b) => {
      // Sort by: unlocked status, then by progress, then by tier
      if (a.isUnlocked !== b.isUnlocked) {
        return a.isUnlocked ? -1 : 1;
      }
      if (a.progress !== b.progress) {
        return b.progress - a.progress;
      }
      
      const tierOrder = { bronze: 0, silver: 1, gold: 2, diamond: 3 };
      return tierOrder[a.tier] - tierOrder[b.tier];
    });
  }

  calculateProgress(achievement) {
    const stats = this.userProgress.stats;
    
    switch (achievement.id) {
      case 'first-transaction':
        return Math.min((stats.totalTransactions / 1) * 100, 100);
        
      case 'first-week':
        return Math.min((stats.streakDays / 7) * 100, 100);
        
      case 'consistent-tracker':
        return Math.min((stats.streakDays / 30) * 100, 100);
        
      case 'dedication-master':
        return Math.min((stats.streakDays / 100) * 100, 100);
        
      case 'ultimate-streaker':
        return Math.min((stats.streakDays / 365) * 100, 100);
        
      case 'first-budget':
        return Math.min((stats.budgetsCreated / 1) * 100, 100);
        
      case 'budget-master':
        return Math.min((stats.budgetsCreated / 8) * 100, 100);
        
      case 'category-explorer':
        return Math.min((stats.categoriesUsed.size / 5) * 100, 100);
        
      case 'saver-starter':
        return Math.min((stats.totalSaved / 1000000) * 100, 100);
        
      case 'millionaire-saver':
        return Math.min((stats.totalSaved / 10000000) * 100, 100);
        
      case 'savings-legend':
        return Math.min((stats.totalSaved / 100000000) * 100, 100);
        
      case 'goal-achiever':
        return Math.min((stats.goalsCompleted / 3) * 100, 100);
        
      case 'early-achiever':
        return Math.min((stats.earlyGoalsCompleted / 5) * 100, 100);
        
      case 'monthly-tracker':
        return Math.min((stats.monthsTracked / 3) * 100, 100);
        
      case 'year-tracker':
        return Math.min((stats.monthsTracked / 12) * 100, 100);
        
      case 'perfect-budgeter':
        return Math.min((stats.perfectBudgetMonths / 6) * 100, 100);
        
      case 'financial-guru':
        const totalAchievements = this.achievements.size - 1; // Exclude this achievement
        return Math.min((this.userProgress.unlockedAchievements.size / totalAchievements) * 100, 100);
        
      default:
        return 0;
    }
  }

  getTierStats() {
    const stats = {
      bronze: { unlocked: 0, total: 0 },
      silver: { unlocked: 0, total: 0 },
      gold: { unlocked: 0, total: 0 },
      diamond: { unlocked: 0, total: 0 }
    };

    for (const achievement of this.achievements.values()) {
      stats[achievement.tier].total++;
      if (this.userProgress.unlockedAchievements.has(achievement.id)) {
        stats[achievement.tier].unlocked++;
      }
    }

    return stats;
  }

  getTotalXPEarned() {
    let totalXP = 0;
    
    for (const achievementId of this.userProgress.unlockedAchievements) {
      const achievement = this.achievements.get(achievementId);
      if (achievement) {
        totalXP += achievement.xpReward;
      }
    }
    
    return totalXP;
  }

  getCompletionPercentage() {
    return (this.userProgress.unlockedAchievements.size / this.achievements.size) * 100;
  }

  // Data persistence
  saveProgress() {
    if (!this.options.autoSave) return;
    
    const data = {
      unlockedAchievements: Array.from(this.userProgress.unlockedAchievements),
      stats: {
        ...this.userProgress.stats,
        categoriesUsed: Array.from(this.userProgress.stats.categoriesUsed)
      }
    };
    
    localStorage.setItem('achievement_progress', JSON.stringify(data));
  }

  loadProgress() {
    try {
      const data = localStorage.getItem('achievement_progress');
      if (data) {
        const parsed = JSON.parse(data);
        this.userProgress.unlockedAchievements = new Set(parsed.unlockedAchievements || []);
        this.userProgress.stats = {
          ...this.userProgress.stats,
          ...parsed.stats,
          categoriesUsed: new Set(parsed.stats?.categoriesUsed || [])
        };
      }
    } catch (error) {
      console.error('Failed to load achievement progress:', error);
    }
  }

  resetProgress() {
    this.userProgress = {
      unlockedAchievements: new Set(),
      stats: {
        totalTransactions: 0,
        totalSaved: 0,
        streakDays: 0,
        budgetsCreated: 0,
        goalsCompleted: 0,
        categoriesUsed: new Set(),
        monthsTracked: 0,
        perfectBudgetMonths: 0,
        earlyGoalsCompleted: 0,
        savingsStreaks: 0
      }
    };
    this.saveProgress();
  }

  // Export/Import for backup
  exportProgress() {
    return {
      version: '1.0',
      timestamp: new Date().toISOString(),
      data: {
        unlockedAchievements: Array.from(this.userProgress.unlockedAchievements),
        stats: {
          ...this.userProgress.stats,
          categoriesUsed: Array.from(this.userProgress.stats.categoriesUsed)
        }
      }
    };
  }

  importProgress(backup) {
    try {
      if (backup.version === '1.0' && backup.data) {
        this.userProgress.unlockedAchievements = new Set(backup.data.unlockedAchievements || []);
        this.userProgress.stats = {
          ...this.userProgress.stats,
          ...backup.data.stats,
          categoriesUsed: new Set(backup.data.stats?.categoriesUsed || [])
        };
        this.saveProgress();
        return true;
      }
    } catch (error) {
      console.error('Failed to import achievement progress:', error);
    }
    return false;
  }
}

export default AchievementSystem;