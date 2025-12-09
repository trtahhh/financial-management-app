// Level System for User Progression
import { CONFIG } from '../constants/config.js';
import { COLORS } from '../constants/colors.js';

class LevelSystem {
  constructor(options = {}) {
    this.options = {
      enableNotifications: true,
      autoSave: true,
      baseXPRequired: 100,
      xpMultiplier: 1.2,
      maxLevel: 50,
      ...options
    };
    
    this.userLevel = {
      currentLevel: 1,
      currentXP: 0,
      totalXP: 0,
      levelHistory: []
    };
    
    this.xpSources = {
      // Daily activities
      firstTransactionOfDay: 25,
      streakDay: 15,
      perfectDay: 50, // No overspending
      
      // Transaction activities
      addTransaction: 5,
      addDetailedTransaction: 10, // With description and category
      quickTransaction: 15, // Under 10 seconds
      
      // Budget activities
      createBudget: 30,
      stayInBudget: 20,
      underBudgetMonth: 100,
      
      // Goal activities
      createGoal: 40,
      reachGoalMilestone: 50,
      completeGoal: 200,
      completeGoalEarly: 300,
      
      // Achievement activities
      unlockAchievement: 0, // Variable based on achievement tier
      
      // Social/sharing activities
      shareAchievement: 25,
      helpOthers: 30,
      
      // Consistency bonuses
      weeklyStreak: 100,
      monthlyStreak: 500,
      perfectWeek: 150,
      perfectMonth: 750
    };
    
    this.levelTiers = this.generateLevelTiers();
    this.init();
  }

  init() {
    this.loadProgress();
    this.calculateLevel();
  }

  generateLevelTiers() {
    const tiers = [];
    let totalXP = 0;
    
    for (let level = 1; level <= this.options.maxLevel; level++) {
      const xpRequired = Math.floor(
        this.options.baseXPRequired * Math.pow(this.options.xpMultiplier, level - 1)
      );
      
      totalXP += xpRequired;
      
      tiers.push({
        level,
        xpRequired,
        totalXPRequired: totalXP,
        title: this.getLevelTitle(level),
        rewards: this.getLevelRewards(level),
        color: this.getLevelColor(level)
      });
    }
    
    return tiers;
  }

  getLevelTitle(level) {
    if (level >= 50) return 'Bậc Thầy Tài Chính';
    if (level >= 40) return 'Chuyên Gia Quản Lý';
    if (level >= 30) return 'Nhà Đầu Tư Thông Minh';
    if (level >= 25) return 'Người Tiết Kiệm Xuất Sắc';
    if (level >= 20) return 'Quản Lý Tài Chính Giỏi';
    if (level >= 15) return 'Người Lập Kế Hoạch';
    if (level >= 10) return 'Theo Dõi Kiên Trì';
    if (level >= 5) return 'Người Mới Bắt Đầu';
    return 'Tân Binh';
  }

  getLevelRewards(level) {
    const rewards = [];
    
    // Feature unlocks
    if (level === 3) rewards.push('Mở khóa: Ngân sách nâng cao');
    if (level === 5) rewards.push('Mở khóa: Mục tiêu tiết kiệm');
    if (level === 8) rewards.push('Mở khóa: Báo cáo chi tiết');
    if (level === 10) rewards.push('Mở khóa: Dự đoán chi tiêu');
    if (level === 15) rewards.push('Mở khóa: Đầu tư cơ bản');
    if (level === 20) rewards.push('Mở khóa: Phân tích xu hướng');
    if (level === 25) rewards.push('Mở khóa: Tư vấn tài chính AI');
    if (level === 30) rewards.push('Mở khóa: Portfolio tracking');
    
    // Bonus rewards every 5 levels
    if (level % 5 === 0) {
      rewards.push('Tăng giới hạn mục tiêu');
      rewards.push('Tăng số lượng nhắc nhở');
    }
    
    // Special milestones
    if (level === 10) rewards.push('Huy hiệu Bạc');
    if (level === 20) rewards.push('Huy hiệu Vàng');
    if (level === 30) rewards.push('Huy hiệu Bạch Kim');
    if (level === 50) rewards.push('Huy hiệu Thạch Anh');
    
    return rewards;
  }

  getLevelColor(level) {
    if (level >= 30) return '#8B5CF6'; // Purple
    if (level >= 20) return '#F59E0B'; // Gold
    if (level >= 10) return '#6B7280'; // Silver
    return '#10B981'; // Green (Bronze)
  }

  // XP Management
  addXP(source, amount = null, context = {}) {
    let xpGained = amount || this.xpSources[source] || 0;
    
    // Apply multipliers based on context
    xpGained = this.applyXPMultipliers(xpGained, source, context);
    
    const oldLevel = this.userLevel.currentLevel;
    this.userLevel.currentXP += xpGained;
    this.userLevel.totalXP += xpGained;
    
    const newLevel = this.calculateLevel();
    
    // Check for level up
    if (newLevel > oldLevel) {
      this.onLevelUp(oldLevel, newLevel, xpGained);
    } else if (xpGained > 0) {
      this.onXPGained(xpGained, source, context);
    }
    
    this.saveProgress();
    return xpGained;
  }

  applyXPMultipliers(baseXP, source, context) {
    let multiplier = 1;
    
    // Streak multipliers
    if (context.streakDays >= 30) multiplier += 0.5;
    else if (context.streakDays >= 14) multiplier += 0.3;
    else if (context.streakDays >= 7) multiplier += 0.1;
    
    // Perfect behavior multipliers
    if (context.isPerfectDay) multiplier += 0.2;
    if (context.isUnderBudget) multiplier += 0.15;
    if (context.isQuickEntry && context.timeSpent < 10) multiplier += 0.25;
    
    // Level-based diminishing returns for basic activities
    if (['addTransaction', 'firstTransactionOfDay'].includes(source)) {
      if (this.userLevel.currentLevel > 20) multiplier *= 0.8;
      else if (this.userLevel.currentLevel > 10) multiplier *= 0.9;
    }
    
    return Math.floor(baseXP * multiplier);
  }

  calculateLevel() {
    for (let i = 0; i < this.levelTiers.length; i++) {
      const tier = this.levelTiers[i];
      if (this.userLevel.totalXP < tier.totalXPRequired) {
        this.userLevel.currentLevel = tier.level;
        return tier.level;
      }
    }
    
    // Max level reached
    this.userLevel.currentLevel = this.options.maxLevel;
    return this.options.maxLevel;
  }

  onLevelUp(oldLevel, newLevel, xpGained) {
    const levelData = this.levelTiers[newLevel - 1];
    
    // Record level up
    this.userLevel.levelHistory.push({
      level: newLevel,
      timestamp: new Date().toISOString(),
      xpGained
    });
    
    if (this.options.enableNotifications) {
      this.showLevelUpNotification(oldLevel, newLevel, levelData);
    }
    
    this.onLevelUpCallback?.(oldLevel, newLevel, levelData);
  }

  onXPGained(xpGained, source, context) {
    if (this.options.enableNotifications && xpGained >= 50) {
      this.showXPNotification(xpGained, source);
    }
    
    this.onXPGainedCallback?.(xpGained, source, context);
  }

  showLevelUpNotification(oldLevel, newLevel, levelData) {
    const notification = document.createElement('div');
    notification.className = 'level-up-notification';
    notification.innerHTML = `
      <div class="level-up-content">
        <div class="level-up-header">
          <div class="level-icon" style="background-color: ${levelData.color}">
            ${newLevel}
          </div>
          <div class="level-info">
            <h3 class="level-up-title">Lên cấp!</h3>
            <p class="level-up-subtitle">Cấp ${newLevel} - ${levelData.title}</p>
          </div>
        </div>
        
        ${levelData.rewards.length > 0 ? `
          <div class="level-rewards">
            <h4>Phần thưởng mới:</h4>
            <ul>
              ${levelData.rewards.map(reward => `<li>${reward}</li>`).join('')}
            </ul>
          </div>
        ` : ''}
        
        <button class="level-close" onclick="this.parentElement.parentElement.remove()">
          Tuyệt vời!
        </button>
      </div>
    `;

    document.body.appendChild(notification);

    // Auto remove after 8 seconds
    setTimeout(() => {
      if (notification.parentElement) {
        notification.remove();
      }
    }, 8000);

    // Add animation
    requestAnimationFrame(() => {
      notification.classList.add('show');
    });
  }

  showXPNotification(xpGained, source) {
    const notification = document.createElement('div');
    notification.className = 'xp-notification';
    notification.innerHTML = `
      <div class="xp-content">
        <span class="xp-amount">+${xpGained} XP</span>
        <span class="xp-source">${this.getXPSourceText(source)}</span>
      </div>
    `;

    document.body.appendChild(notification);

    // Auto remove after 3 seconds
    setTimeout(() => {
      if (notification.parentElement) {
        notification.remove();
      }
    }, 3000);

    // Add animation
    requestAnimationFrame(() => {
      notification.classList.add('show');
    });
  }

  getXPSourceText(source) {
    const sourceTexts = {
      firstTransactionOfDay: 'Giao dịch đầu tiên trong ngày',
      streakDay: 'Duy trì chuỗi ngày',
      perfectDay: 'Ngày hoàn hảo',
      addTransaction: 'Thêm giao dịch',
      addDetailedTransaction: 'Giao dịch chi tiết',
      quickTransaction: 'Giao dịch nhanh',
      createBudget: 'Tạo ngân sách',
      stayInBudget: 'Không vượt ngân sách',
      underBudgetMonth: 'Tháng tiết kiệm',
      createGoal: 'Tạo mục tiêu',
      reachGoalMilestone: 'Đạt mốc mục tiêu',
      completeGoal: 'Hoàn thành mục tiêu',
      completeGoalEarly: 'Hoàn thành sớm',
      weeklyStreak: 'Chuỗi tuần',
      monthlyStreak: 'Chuỗi tháng',
      perfectWeek: 'Tuần hoàn hảo',
      perfectMonth: 'Tháng hoàn hảo'
    };
    
    return sourceTexts[source] || source;
  }

  // Getters for UI
  getCurrentLevel() {
    return this.userLevel.currentLevel;
  }

  getCurrentXP() {
    return this.userLevel.currentXP;
  }

  getTotalXP() {
    return this.userLevel.totalXP;
  }

  getXPForNextLevel() {
    if (this.userLevel.currentLevel >= this.options.maxLevel) {
      return 0;
    }
    
    const currentTier = this.levelTiers[this.userLevel.currentLevel - 1];
    const nextTier = this.levelTiers[this.userLevel.currentLevel];
    
    return nextTier.totalXPRequired - this.userLevel.totalXP;
  }

  getXPProgress() {
    if (this.userLevel.currentLevel >= this.options.maxLevel) {
      return 100;
    }
    
    const currentTier = this.levelTiers[this.userLevel.currentLevel - 1];
    const nextTier = this.levelTiers[this.userLevel.currentLevel];
    
    const currentLevelXP = this.userLevel.totalXP - (currentTier.totalXPRequired - currentTier.xpRequired);
    const xpNeededForNext = nextTier.xpRequired;
    
    return (currentLevelXP / xpNeededForNext) * 100;
  }

  getLevelData(level = null) {
    const targetLevel = level || this.userLevel.currentLevel;
    return this.levelTiers[targetLevel - 1];
  }

  getLevelHistory() {
    return [...this.userLevel.levelHistory];
  }

  getNextLevelRewards() {
    if (this.userLevel.currentLevel >= this.options.maxLevel) {
      return [];
    }
    
    return this.levelTiers[this.userLevel.currentLevel].rewards;
  }

  // Statistics
  getAverageXPPerDay() {
    if (this.userLevel.levelHistory.length === 0) {
      return 0;
    }
    
    const firstLevel = new Date(this.userLevel.levelHistory[0].timestamp);
    const now = new Date();
    const daysActive = Math.max(1, Math.ceil((now - firstLevel) / (1000 * 60 * 60 * 24)));
    
    return Math.floor(this.userLevel.totalXP / daysActive);
  }

  getTimeToNextLevel() {
    const xpNeeded = this.getXPForNextLevel();
    const averageXPPerDay = this.getAverageXPPerDay();
    
    if (averageXPPerDay === 0) {
      return null;
    }
    
    return Math.ceil(xpNeeded / averageXPPerDay);
  }

  // Data persistence
  saveProgress() {
    if (!this.options.autoSave) return;
    
    const data = {
      currentLevel: this.userLevel.currentLevel,
      currentXP: this.userLevel.currentXP,
      totalXP: this.userLevel.totalXP,
      levelHistory: this.userLevel.levelHistory
    };
    
    localStorage.setItem('level_progress', JSON.stringify(data));
  }

  loadProgress() {
    try {
      const data = localStorage.getItem('level_progress');
      if (data) {
        const parsed = JSON.parse(data);
        this.userLevel = {
          currentLevel: parsed.currentLevel || 1,
          currentXP: parsed.currentXP || 0,
          totalXP: parsed.totalXP || 0,
          levelHistory: parsed.levelHistory || []
        };
      }
    } catch (error) {
      console.error('Failed to load level progress:', error);
    }
  }

  resetProgress() {
    this.userLevel = {
      currentLevel: 1,
      currentXP: 0,
      totalXP: 0,
      levelHistory: []
    };
    this.saveProgress();
  }

  // Export/Import
  exportProgress() {
    return {
      version: '1.0',
      timestamp: new Date().toISOString(),
      data: { ...this.userLevel }
    };
  }

  importProgress(backup) {
    try {
      if (backup.version === '1.0' && backup.data) {
        this.userLevel = { ...backup.data };
        this.calculateLevel();
        this.saveProgress();
        return true;
      }
    } catch (error) {
      console.error('Failed to import level progress:', error);
    }
    return false;
  }
}

export default LevelSystem;