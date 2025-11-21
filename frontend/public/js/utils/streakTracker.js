// Streak Tracking System for Daily Engagement
import { formatDate } from './dateHelpers.js';

class StreakTracker {
  constructor(options = {}) {
    this.options = {
      enableNotifications: true,
      autoSave: true,
      streakBreakGracePeriod: 1, // 1 day grace period
      requireMinimumActions: 1, // Minimum actions per day to count
      ...options
    };
    
    this.streakData = {
      currentStreak: 0,
      longestStreak: 0,
      lastActiveDate: null,
      streakHistory: [],
      dailyActions: new Map(), // date -> action count
      totalActiveDays: 0,
      streakBreaks: 0,
      perfectDays: 0 // Days with exceptional activity
    };
    
    this.dailyGoals = {
      transactions: 1,
      budgetCheck: 1,
      goalReview: 0 // Optional
    };
    
    this.init();
  }

  init() {
    this.loadProgress();
    this.checkStreakStatus();
  }

  // Record daily activity
  recordActivity(type, date = new Date()) {
    const dateKey = this.getDateKey(date);
    const today = this.getDateKey(new Date());
    
    // Initialize daily actions if not exists
    if (!this.streakData.dailyActions.has(dateKey)) {
      this.streakData.dailyActions.set(dateKey, {
        transactions: 0,
        budgetChecks: 0,
        goalReviews: 0,
        totalActions: 0,
        firstAction: date.toISOString(),
        lastAction: date.toISOString()
      });
    }
    
    const dayData = this.streakData.dailyActions.get(dateKey);
    
    // Update action counts
    switch (type) {
      case 'transaction':
        dayData.transactions++;
        break;
      case 'budget_check':
        dayData.budgetChecks++;
        break;
      case 'goal_review':
        dayData.goalReviews++;
        break;
    }
    
    dayData.totalActions++;
    dayData.lastAction = date.toISOString();
    
    // Update streak if this is today's activity
    if (dateKey === today) {
      this.updateStreak(date);
    }
    
    this.saveProgress();
    
    return {
      dayData,
      streakUpdated: dateKey === today,
      currentStreak: this.streakData.currentStreak
    };
  }

  updateStreak(date = new Date()) {
    const dateKey = this.getDateKey(date);
    const yesterday = this.getDateKey(this.getYesterday(date));
    const wasActiveToday = this.isActiveDay(dateKey);
    
    if (!wasActiveToday) {
      return false; // Not enough activity for streak
    }
    
    // First time tracking or continuing from yesterday
    if (!this.streakData.lastActiveDate) {
      this.streakData.currentStreak = 1;
      this.streakData.lastActiveDate = dateKey;
      this.streakData.totalActiveDays = 1;
      this.addStreakEvent('started', 1, date);
    } else {
      const daysSinceLastActive = this.getDaysDifference(this.streakData.lastActiveDate, dateKey);
      
      if (daysSinceLastActive === 1) {
        // Continuing streak
        this.streakData.currentStreak++;
        this.streakData.lastActiveDate = dateKey;
        this.streakData.totalActiveDays++;
        
        // Check for milestone
        if (this.isStreakMilestone(this.streakData.currentStreak)) {
          this.addStreakEvent('milestone', this.streakData.currentStreak, date);
          if (this.options.enableNotifications) {
            this.showStreakNotification(this.streakData.currentStreak);
          }
        }
        
      } else if (daysSinceLastActive === 0) {
        // Same day, just update last active
        this.streakData.lastActiveDate = dateKey;
        
      } else if (daysSinceLastActive <= this.options.streakBreakGracePeriod + 1) {
        // Within grace period, maintain streak but note the gap
        this.streakData.lastActiveDate = dateKey;
        this.streakData.totalActiveDays++;
        
      } else {
        // Streak broken
        if (this.streakData.currentStreak > 0) {
          this.addStreakEvent('broken', this.streakData.currentStreak, date);
          this.streakData.streakBreaks++;
          
          // Update longest streak
          if (this.streakData.currentStreak > this.streakData.longestStreak) {
            this.streakData.longestStreak = this.streakData.currentStreak;
          }
        }
        
        // Start new streak
        this.streakData.currentStreak = 1;
        this.streakData.lastActiveDate = dateKey;
        this.streakData.totalActiveDays++;
        this.addStreakEvent('restarted', 1, date);
      }
    }
    
    // Check for perfect day
    if (this.isPerfectDay(dateKey)) {
      this.streakData.perfectDays++;
    }
    
    // Update longest streak
    if (this.streakData.currentStreak > this.streakData.longestStreak) {
      this.streakData.longestStreak = this.streakData.currentStreak;
    }
    
    this.saveProgress();
    return true;
  }

  checkStreakStatus() {
    if (!this.streakData.lastActiveDate) {
      return;
    }
    
    const today = this.getDateKey(new Date());
    const daysSinceLastActive = this.getDaysDifference(this.streakData.lastActiveDate, today);
    
    // Check if streak should be broken
    if (daysSinceLastActive > this.options.streakBreakGracePeriod + 1) {
      if (this.streakData.currentStreak > 0) {
        // Streak broken due to inactivity
        this.addStreakEvent('broken_inactive', this.streakData.currentStreak, new Date());
        this.streakData.streakBreaks++;
        
        if (this.streakData.currentStreak > this.streakData.longestStreak) {
          this.streakData.longestStreak = this.streakData.currentStreak;
        }
        
        this.streakData.currentStreak = 0;
        this.saveProgress();
      }
    }
  }

  isActiveDay(dateKey) {
    const dayData = this.streakData.dailyActions.get(dateKey);
    if (!dayData) return false;
    
    return dayData.totalActions >= this.options.requireMinimumActions &&
           dayData.transactions >= this.dailyGoals.transactions;
  }

  isPerfectDay(dateKey) {
    const dayData = this.streakData.dailyActions.get(dateKey);
    if (!dayData) return false;
    
    return dayData.transactions >= 3 && // Multiple transactions
           dayData.budgetChecks >= 1 &&  // Checked budget
           dayData.totalActions >= 5;    // High activity
  }

  isStreakMilestone(streak) {
    const milestones = [7, 14, 30, 60, 100, 200, 365];
    return milestones.includes(streak);
  }

  addStreakEvent(type, streak, date) {
    this.streakData.streakHistory.push({
      type,
      streak,
      date: date.toISOString(),
      timestamp: Date.now()
    });
    
    // Keep only last 100 events
    if (this.streakData.streakHistory.length > 100) {
      this.streakData.streakHistory = this.streakData.streakHistory.slice(-100);
    }
  }

  showStreakNotification(streak) {
    const milestoneTexts = {
      7: 'Tuần đầu tiên hoàn thành!',
      14: 'Hai tuần kiên trì!',
      30: 'Một tháng tuyệt vời!',
      60: 'Hai tháng không ngừng nghỉ!',
      100: 'Một trăm ngày phi thường!',
      200: 'Hai trăm ngày đáng kinh ngạc!',
      365: 'Một năm hoàn hảo! Bạn là huyền thoại!'
    };
    
    const text = milestoneTexts[streak] || `${streak} ngày liên tục!`;
    
    const notification = document.createElement('div');
    notification.className = 'streak-notification';
    notification.innerHTML = `
      <div class="streak-notification-content">
        <div class="streak-fire"></div>
        <div class="streak-details">
          <h3 class="streak-title">Chuỗi ngày ${streak}!</h3>
          <p class="streak-text">${text}</p>
          <div class="streak-progress">
            <div class="progress-dots">
              ${this.generateProgressDots(streak)}
            </div>
          </div>
        </div>
        <button class="streak-close" onclick="this.parentElement.parentElement.remove()">×</button>
      </div>
    `;

    document.body.appendChild(notification);

    setTimeout(() => {
      if (notification.parentElement) {
        notification.remove();
      }
    }, 6000);

    requestAnimationFrame(() => {
      notification.classList.add('show');
    });
  }

  generateProgressDots(streak) {
    const dots = [];
    const maxDots = 10;
    const dotsToShow = Math.min(streak, maxDots);
    
    for (let i = 0; i < dotsToShow; i++) {
      dots.push('<div class="progress-dot active"></div>');
    }
    
    if (streak > maxDots) {
      dots.push(`<span class="progress-more">+${streak - maxDots}</span>`);
    }
    
    return dots.join('');
  }

  // Getters for UI
  getCurrentStreak() {
    return this.streakData.currentStreak;
  }

  getLongestStreak() {
    return this.streakData.longestStreak;
  }

  getTotalActiveDays() {
    return this.streakData.totalActiveDays;
  }

  getStreakBreaks() {
    return this.streakData.streakBreaks;
  }

  getPerfectDays() {
    return this.streakData.perfectDays;
  }

  getLastActiveDate() {
    return this.streakData.lastActiveDate;
  }

  getDayActivity(date = new Date()) {
    const dateKey = this.getDateKey(date);
    return this.streakData.dailyActions.get(dateKey) || {
      transactions: 0,
      budgetChecks: 0,
      goalReviews: 0,
      totalActions: 0
    };
  }

  getWeekActivity(startDate = null) {
    const start = startDate || this.getStartOfWeek(new Date());
    const weekData = [];
    
    for (let i = 0; i < 7; i++) {
      const date = new Date(start);
      date.setDate(start.getDate() + i);
      const dateKey = this.getDateKey(date);
      
      weekData.push({
        date: dateKey,
        dayName: date.toLocaleDateString('vi-VN', { weekday: 'short' }),
        activity: this.getDayActivity(date),
        isActive: this.isActiveDay(dateKey),
        isPerfect: this.isPerfectDay(dateKey)
      });
    }
    
    return weekData;
  }

  getMonthActivity(month = null, year = null) {
    const now = new Date();
    const targetMonth = month !== null ? month : now.getMonth();
    const targetYear = year !== null ? year : now.getFullYear();
    
    const monthData = [];
    const daysInMonth = new Date(targetYear, targetMonth + 1, 0).getDate();
    
    for (let day = 1; day <= daysInMonth; day++) {
      const date = new Date(targetYear, targetMonth, day);
      const dateKey = this.getDateKey(date);
      
      monthData.push({
        date: dateKey,
        day: day,
        activity: this.getDayActivity(date),
        isActive: this.isActiveDay(dateKey),
        isPerfect: this.isPerfectDay(dateKey)
      });
    }
    
    return monthData;
  }

  getStreakStats() {
    return {
      current: this.streakData.currentStreak,
      longest: this.streakData.longestStreak,
      totalActiveDays: this.streakData.totalActiveDays,
      streakBreaks: this.streakData.streakBreaks,
      perfectDays: this.streakData.perfectDays,
      consistency: this.getConsistencyPercentage(),
      averageActivityPerDay: this.getAverageActivityPerDay()
    };
  }

  getStreakHistory(limit = 20) {
    return this.streakData.streakHistory
      .slice(-limit)
      .reverse()
      .map(event => ({
        ...event,
        date: new Date(event.date),
        description: this.getStreakEventDescription(event)
      }));
  }

  getStreakEventDescription(event) {
    const descriptions = {
      started: `Bắt đầu chuỗi ngày với ${event.streak} ngày`,
      milestone: `Đạt cột mốc ${event.streak} ngày!`,
      broken: `Chuỗi ${event.streak} ngày bị gián đoạn`,
      broken_inactive: `Chuỗi ${event.streak} ngày kết thúc do không hoạt động`,
      restarted: `Bắt đầu chuỗi mới`
    };
    
    return descriptions[event.type] || `Sự kiện chuỗi: ${event.type}`;
  }

  getConsistencyPercentage() {
    if (this.streakData.streakHistory.length === 0) return 0;
    
    const firstEvent = this.streakData.streakHistory[0];
    const daysSinceStart = this.getDaysDifference(
      this.getDateKey(new Date(firstEvent.date)),
      this.getDateKey(new Date())
    );
    
    return daysSinceStart > 0 ? (this.streakData.totalActiveDays / daysSinceStart) * 100 : 0;
  }

  getAverageActivityPerDay() {
    if (this.streakData.totalActiveDays === 0) return 0;
    
    let totalActions = 0;
    for (const dayData of this.streakData.dailyActions.values()) {
      totalActions += dayData.totalActions;
    }
    
    return totalActions / this.streakData.totalActiveDays;
  }

  // Utility methods
  getDateKey(date) {
    return formatDate(date, 'yyyy-MM-dd');
  }

  getYesterday(date = new Date()) {
    const yesterday = new Date(date);
    yesterday.setDate(date.getDate() - 1);
    return yesterday;
  }

  getStartOfWeek(date) {
    const start = new Date(date);
    const day = start.getDay();
    const diff = start.getDate() - day + (day === 0 ? -6 : 1); // Monday as start
    return new Date(start.setDate(diff));
  }

  getDaysDifference(startDateKey, endDateKey) {
    const start = new Date(startDateKey);
    const end = new Date(endDateKey);
    const diffTime = end - start;
    return Math.floor(diffTime / (1000 * 60 * 60 * 24));
  }

  // Data persistence
  saveProgress() {
    if (!this.options.autoSave) return;
    
    const data = {
      ...this.streakData,
      dailyActions: Object.fromEntries(this.streakData.dailyActions)
    };
    
    localStorage.setItem('streak_progress', JSON.stringify(data));
  }

  loadProgress() {
    try {
      const data = localStorage.getItem('streak_progress');
      if (data) {
        const parsed = JSON.parse(data);
        this.streakData = {
          currentStreak: parsed.currentStreak || 0,
          longestStreak: parsed.longestStreak || 0,
          lastActiveDate: parsed.lastActiveDate,
          streakHistory: parsed.streakHistory || [],
          dailyActions: new Map(Object.entries(parsed.dailyActions || {})),
          totalActiveDays: parsed.totalActiveDays || 0,
          streakBreaks: parsed.streakBreaks || 0,
          perfectDays: parsed.perfectDays || 0
        };
      }
    } catch (error) {
      console.error('Failed to load streak progress:', error);
    }
  }

  resetProgress() {
    this.streakData = {
      currentStreak: 0,
      longestStreak: 0,
      lastActiveDate: null,
      streakHistory: [],
      dailyActions: new Map(),
      totalActiveDays: 0,
      streakBreaks: 0,
      perfectDays: 0
    };
    this.saveProgress();
  }

  // Export/Import
  exportProgress() {
    return {
      version: '1.0',
      timestamp: new Date().toISOString(),
      data: {
        ...this.streakData,
        dailyActions: Object.fromEntries(this.streakData.dailyActions)
      }
    };
  }

  importProgress(backup) {
    try {
      if (backup.version === '1.0' && backup.data) {
        this.streakData = {
          ...backup.data,
          dailyActions: new Map(Object.entries(backup.data.dailyActions || {}))
        };
        this.saveProgress();
        return true;
      }
    } catch (error) {
      console.error('Failed to import streak progress:', error);
    }
    return false;
  }
}

export default StreakTracker;