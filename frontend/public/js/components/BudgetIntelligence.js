// Enhanced Budget Intelligence System
import { formatCurrency } from '../utils/currencyHelpers.js';
import { calculateBudgetProgress } from '../utils/calculationHelpers.js';
import { CATEGORY_NAMES } from '../constants/categories.js';

class BudgetIntelligence {
  constructor(budgetService, transactionService, options = {}) {
    this.budgetService = budgetService;
    this.transactionService = transactionService;
    this.options = {
      enableAI: true,
      enableForecasting: true,
      enableSmartAlerts: true,
      enableRecommendations: true,
      enableCategorySplitting: true,
      enableBudgetTemplates: true,
      ...options
    };
    
    this.historicalData = [];
    this.spendingPatterns = {};
    this.budgetTemplates = [];
    this.alertRules = [];
    
    this.init();
  }

  async init() {
    await this.loadHistoricalData();
    await this.loadBudgetTemplates();
    await this.loadAlertRules();
    this.analyzeSpendingPatterns();
    this.setupSmartAlerts();
  }

  // AI-Powered Budget Recommendations
  async generateBudgetRecommendations(userId, timeframe = 'monthly') {
    const transactions = await this.transactionService.getTransactionsByTimeframe(timeframe);
    const analysis = this.analyzeSpendingBehavior(transactions);
    
    const recommendations = {
      suggestedBudgets: this.calculateSuggestedBudgets(analysis),
      optimizations: this.findOptimizationOpportunities(analysis),
      riskAlerts: this.identifySpendingRisks(analysis),
      savingsOpportunities: this.findSavingsOpportunities(analysis),
      personalizedTips: this.generatePersonalizedTips(analysis),
      confidence: this.calculateRecommendationConfidence(analysis)
    };
    
    return recommendations;
  }

  analyzeSpendingBehavior(transactions) {
    const analysis = {
      totalSpending: 0,
      categoryBreakdown: {},
      spendingTrends: {},
      volatility: {},
      peakSpendingDays: [],
      recurringPatterns: {},
      seasonality: {},
      comparison: {}
    };

    // Category breakdown with statistical analysis
    transactions.forEach(transaction => {
      const category = transaction.category;
      const amount = Math.abs(transaction.amount);
      
      if (!analysis.categoryBreakdown[category]) {
        analysis.categoryBreakdown[category] = {
          total: 0,
          count: 0,
          average: 0,
          median: 0,
          stdDev: 0,
          amounts: []
        };
      }
      
      analysis.categoryBreakdown[category].total += amount;
      analysis.categoryBreakdown[category].count++;
      analysis.categoryBreakdown[category].amounts.push(amount);
      analysis.totalSpending += amount;
    });

    // Calculate statistical measures for each category
    Object.keys(analysis.categoryBreakdown).forEach(category => {
      const data = analysis.categoryBreakdown[category];
      data.average = data.total / data.count;
      data.amounts.sort((a, b) => a - b);
      data.median = this.calculateMedian(data.amounts);
      data.stdDev = this.calculateStandardDeviation(data.amounts, data.average);
      
      // Volatility score (0-1, higher = more volatile)
      analysis.volatility[category] = Math.min(data.stdDev / data.average, 1);
    });

    // Spending trends analysis
    analysis.spendingTrends = this.analyzeSpendingTrends(transactions);
    
    // Peak spending days
    analysis.peakSpendingDays = this.identifyPeakSpendingDays(transactions);
    
    // Recurring patterns
    analysis.recurringPatterns = this.identifyRecurringPatterns(transactions);
    
    // Seasonality analysis
    analysis.seasonality = this.analyzeSeasonality(transactions);
    
    // Comparison with similar users (mock data for now)
    analysis.comparison = this.compareWithPeerGroup(analysis);

    return analysis;
  }

  calculateSuggestedBudgets(analysis) {
    const suggestions = {};
    
    Object.keys(analysis.categoryBreakdown).forEach(category => {
      const data = analysis.categoryBreakdown[category];
      const volatility = analysis.volatility[category] || 0;
      
      // Base suggestion on historical average + buffer for volatility
      const baseAmount = data.average * 30; // Monthly budget
      const volatilityBuffer = baseAmount * (volatility * 0.3);
      const trendAdjustment = this.getTrendAdjustment(category, analysis.spendingTrends);
      
      suggestions[category] = {
        recommended: Math.round(baseAmount + volatilityBuffer + trendAdjustment),
        min: Math.round(data.median * 30),
        max: Math.round(baseAmount * 1.5),
        confidence: this.calculateCategoryConfidence(data, volatility),
        reasoning: this.generateBudgetReasoning(category, data, volatility, trendAdjustment)
      };
    });
    
    return suggestions;
  }

  generateBudgetReasoning(category, data, volatility, trendAdjustment) {
    const reasons = [];
    
    if (data.count < 5) {
      reasons.push('Dữ liệu còn ít, gợi ý có thể chưa chính xác');
    }
    
    if (volatility > 0.5) {
      reasons.push('Chi tiêu không ổn định, nên dự phòng thêm');
    } else if (volatility < 0.2) {
      reasons.push('Chi tiêu ổn định, có thể tin tương vào con số này');
    }
    
    if (trendAdjustment > 0) {
      reasons.push('Xu hướng tăng chi tiêu, nên tăng ngân sách');
    } else if (trendAdjustment < 0) {
      reasons.push('Xu hướng giảm chi tiêu, có thể giảm ngân sách');
    }
    
    const categoryName = CATEGORY_NAMES[category] || category;
    return `Dành cho ${categoryName}: ${reasons.join(', ') || 'Phù hợp với thói quen hiện tại'}`;
  }

  // Smart Forecasting
  async generateSpendingForecast(category, timeframe = 'month') {
    const transactions = await this.transactionService.getCategoryTransactions(category);
    const forecast = {
      prediction: 0,
      confidence: 0,
      factors: [],
      scenarios: {
        optimistic: 0,
        realistic: 0,
        pessimistic: 0
      },
      recommendations: []
    };

    if (transactions.length < 10) {
      forecast.confidence = 0.3;
      forecast.prediction = this.calculateSimpleForecast(transactions);
    } else {
      // Advanced forecasting with trend analysis
      const { prediction, confidence } = this.calculateAdvancedForecast(transactions, timeframe);
      forecast.prediction = prediction;
      forecast.confidence = confidence;
    }

    // Generate scenarios
    const variance = forecast.prediction * 0.2;
    forecast.scenarios = {
      optimistic: Math.max(0, forecast.prediction - variance),
      realistic: forecast.prediction,
      pessimistic: forecast.prediction + variance
    };

    // Add influencing factors
    forecast.factors = this.identifyForecastFactors(category, transactions);
    
    // Generate recommendations
    forecast.recommendations = this.generateForecastRecommendations(category, forecast);

    return forecast;
  }

  calculateAdvancedForecast(transactions, timeframe) {
    // Implement time series forecasting
    const monthlyData = this.aggregateByMonth(transactions);
    
    if (monthlyData.length < 3) {
      return { prediction: this.calculateSimpleForecast(transactions), confidence: 0.4 };
    }

    // Linear trend analysis
    const trend = this.calculateLinearTrend(monthlyData);
    
    // Seasonal adjustment
    const seasonalFactor = this.getSeasonalFactor(new Date().getMonth(), monthlyData);
    
    // Base prediction on trend + seasonal adjustment
    const lastValue = monthlyData[monthlyData.length - 1].amount;
    const prediction = lastValue + trend.slope + (trend.slope * seasonalFactor);
    
    // Confidence based on R-squared and data points
    const confidence = Math.min(0.9, trend.rSquared * (monthlyData.length / 12));
    
    return { prediction: Math.max(0, prediction), confidence };
  }

  // Category Splitting Intelligence
  async suggestCategorySplit(category, transactions) {
    if (!this.options.enableCategorySplitting) return null;
    
    const analysis = this.analyzeTransactionDescriptions(transactions);
    const clusters = this.clusterTransactions(transactions, analysis);
    
    if (clusters.length <= 1) return null;
    
    const suggestions = clusters.map(cluster => ({
      newCategory: this.suggestCategoryName(cluster),
      transactions: cluster.transactions,
      totalAmount: cluster.totalAmount,
      averageAmount: cluster.averageAmount,
      description: cluster.commonTerms.join(', '),
      confidence: cluster.confidence
    }));
    
    return {
      originalCategory: category,
      suggestedSplits: suggestions,
      potentialSavings: this.calculateSplitBenefits(suggestions),
      implementation: {
        effort: this.calculateImplementationEffort(suggestions),
        impact: this.calculateSplitImpact(suggestions)
      }
    };
  }

  clusterTransactions(transactions, analysis) {
    // Simple clustering based on description keywords and amount ranges
    const clusters = [];
    const processedTransactions = new Set();
    
    // Group by common keywords
    analysis.keywords.forEach(keyword => {
      const matchingTransactions = transactions.filter(t => 
        !processedTransactions.has(t.id) && 
        t.description.toLowerCase().includes(keyword.toLowerCase())
      );
      
      if (matchingTransactions.length >= 3) {
        clusters.push({
          type: 'keyword',
          identifier: keyword,
          transactions: matchingTransactions,
          totalAmount: matchingTransactions.reduce((sum, t) => sum + Math.abs(t.amount), 0),
          averageAmount: matchingTransactions.reduce((sum, t) => sum + Math.abs(t.amount), 0) / matchingTransactions.length,
          commonTerms: [keyword],
          confidence: matchingTransactions.length / transactions.length
        });
        
        matchingTransactions.forEach(t => processedTransactions.add(t.id));
      }
    });
    
    // Group by amount ranges
    const remainingTransactions = transactions.filter(t => !processedTransactions.has(t.id));
    const amountClusters = this.clusterByAmount(remainingTransactions);
    
    clusters.push(...amountClusters);
    
    return clusters.filter(c => c.transactions.length >= 3);
  }

  // Smart Alert System
  async setupSmartAlerts() {
    if (!this.options.enableSmartAlerts) return;
    
    // Default alert rules
    const defaultRules = [
      {
        id: 'budget_80_percent',
        type: 'budget_threshold',
        threshold: 0.8,
        message: 'Đã chi 80% ngân sách danh mục {category}',
        severity: 'warning',
        frequency: 'once_per_period'
      },
      {
        id: 'budget_exceeded',
        type: 'budget_threshold',
        threshold: 1.0,
        message: 'Đã vượt ngân sách danh mục {category}',
        severity: 'critical',
        frequency: 'immediate'
      },
      {
        id: 'unusual_spending',
        type: 'anomaly_detection',
        threshold: 2.0, // 2 standard deviations
        message: 'Chi tiêu bất thường ở danh mục {category}: {amount}',
        severity: 'info',
        frequency: 'daily'
      },
      {
        id: 'recurring_missed',
        type: 'recurring_transaction',
        threshold: 1, // 1 day late
        message: 'Giao dịch định kỳ "{description}" đã trễ {days} ngày',
        severity: 'warning',
        frequency: 'daily'
      }
    ];
    
    this.alertRules = [...this.alertRules, ...defaultRules];
    
    // Schedule alert checks
    this.scheduleAlertChecks();
  }

  async checkAlerts() {
    const alerts = [];
    
    for (const rule of this.alertRules) {
      try {
        const ruleAlerts = await this.evaluateAlertRule(rule);
        alerts.push(...ruleAlerts);
      } catch (error) {
        console.error('Error evaluating alert rule:', rule.id, error);
      }
    }
    
    // Process and display alerts
    if (alerts.length > 0) {
      this.processAlerts(alerts);
    }
    
    return alerts;
  }

  async evaluateAlertRule(rule) {
    const alerts = [];
    
    switch (rule.type) {
      case 'budget_threshold':
        alerts.push(...await this.checkBudgetThresholds(rule));
        break;
        
      case 'anomaly_detection':
        alerts.push(...await this.checkSpendingAnomalies(rule));
        break;
        
      case 'recurring_transaction':
        alerts.push(...await this.checkRecurringTransactions(rule));
        break;
        
      case 'custom':
        alerts.push(...await this.evaluateCustomRule(rule));
        break;
    }
    
    return alerts;
  }

  // Budget Templates System
  async createBudgetTemplate(name, budgets, metadata = {}) {
    const template = {
      id: Date.now().toString(),
      name,
      budgets: budgets.map(b => ({
        category: b.category,
        amount: b.amount,
        percentage: b.percentage || null
      })),
      metadata: {
        totalAmount: budgets.reduce((sum, b) => sum + b.amount, 0),
        categoryCount: budgets.length,
        targetAudience: metadata.targetAudience || 'general',
        difficulty: metadata.difficulty || 'medium',
        description: metadata.description || '',
        tags: metadata.tags || [],
        ...metadata
      },
      createdAt: new Date().toISOString(),
      usage: {
        timesUsed: 0,
        lastUsed: null,
        avgRating: 0,
        reviews: []
      }
    };
    
    this.budgetTemplates.push(template);
    await this.saveBudgetTemplates();
    
    return template;
  }

  async getRecommendedTemplates(userProfile) {
    const recommendations = [];
    
    // Filter templates based on user profile
    const candidates = this.budgetTemplates.filter(template => {
      if (userProfile.targetAudience && template.metadata.targetAudience !== 'general') {
        return template.metadata.targetAudience === userProfile.targetAudience;
      }
      return true;
    });
    
    // Score templates based on user preferences and historical data
    for (const template of candidates) {
      const score = await this.calculateTemplateScore(template, userProfile);
      if (score > 0.3) {
        recommendations.push({
          template,
          score,
          matchReasons: this.getTemplateMatchReasons(template, userProfile)
        });
      }
    }
    
    // Sort by score
    recommendations.sort((a, b) => b.score - a.score);
    
    return recommendations.slice(0, 10);
  }

  async calculateTemplateScore(template, userProfile) {
    let score = 0;
    
    // Base score from usage statistics
    if (template.usage.timesUsed > 0) {
      score += Math.min(0.3, template.usage.avgRating / 5 * 0.3);
      score += Math.min(0.2, template.usage.timesUsed / 100 * 0.2);
    }
    
    // Category match score
    const userCategories = userProfile.topCategories || [];
    const templateCategories = template.budgets.map(b => b.category);
    const categoryOverlap = userCategories.filter(c => templateCategories.includes(c)).length;
    score += (categoryOverlap / Math.max(userCategories.length, templateCategories.length)) * 0.3;
    
    // Budget size compatibility
    const userTotalBudget = userProfile.totalBudget || 0;
    const templateTotal = template.metadata.totalAmount;
    if (userTotalBudget > 0) {
      const sizeDiff = Math.abs(userTotalBudget - templateTotal) / userTotalBudget;
      score += Math.max(0, (1 - sizeDiff) * 0.2);
    }
    
    return Math.min(1, score);
  }

  // Enhanced Analytics
  generateSpendingInsights(transactions, timeframe = 'month') {
    const insights = {
      summary: this.generateSpendingSummary(transactions),
      patterns: this.identifySpendingPatterns(transactions),
      comparisons: this.generateComparisons(transactions, timeframe),
      predictions: this.generatePredictions(transactions),
      recommendations: this.generateActionableRecommendations(transactions),
      achievements: this.identifyFinancialAchievements(transactions)
    };
    
    return insights;
  }

  generateSpendingSummary(transactions) {
    const total = transactions.reduce((sum, t) => sum + Math.abs(t.amount), 0);
    const avgPerDay = total / 30;
    const avgPerTransaction = total / transactions.length;
    
    const categoryTotals = {};
    transactions.forEach(t => {
      const cat = t.category;
      categoryTotals[cat] = (categoryTotals[cat] || 0) + Math.abs(t.amount);
    });
    
    const topCategory = Object.entries(categoryTotals)
      .sort((a, b) => b[1] - a[1])[0];
    
    return {
      totalSpent: total,
      transactionCount: transactions.length,
      averagePerDay: avgPerDay,
      averagePerTransaction: avgPerTransaction,
      topCategory: topCategory ? {
        name: CATEGORY_NAMES[topCategory[0]] || topCategory[0],
        amount: topCategory[1],
        percentage: (topCategory[1] / total) * 100
      } : null,
      categoryBreakdown: categoryTotals
    };
  }

  identifySpendingPatterns(transactions) {
    return {
      peakDays: this.findPeakSpendingDays(transactions),
      timePatterns: this.analyzeTimePatterns(transactions),
      amountPatterns: this.analyzeAmountPatterns(transactions),
      categoryPatterns: this.analyzeCategoryPatterns(transactions),
      seasonality: this.analyzeSeasonality(transactions)
    };
  }

  // Utility Methods
  calculateMedian(arr) {
    const sorted = [...arr].sort((a, b) => a - b);
    const mid = Math.floor(sorted.length / 2);
    return sorted.length % 2 === 0 
      ? (sorted[mid - 1] + sorted[mid]) / 2 
      : sorted[mid];
  }

  calculateStandardDeviation(arr, mean) {
    const variance = arr.reduce((sum, val) => sum + Math.pow(val - mean, 2), 0) / arr.length;
    return Math.sqrt(variance);
  }

  async saveData() {
    await this.saveBudgetTemplates();
    await this.saveAlertRules();
    await this.saveSpendingPatterns();
  }

  async saveBudgetTemplates() {
    localStorage.setItem('budget_templates', JSON.stringify(this.budgetTemplates));
  }

  async loadBudgetTemplates() {
    const saved = localStorage.getItem('budget_templates');
    this.budgetTemplates = saved ? JSON.parse(saved) : [];
  }

  async saveAlertRules() {
    localStorage.setItem('alert_rules', JSON.stringify(this.alertRules));
  }

  async loadAlertRules() {
    const saved = localStorage.getItem('alert_rules');
    if (saved) {
      this.alertRules = JSON.parse(saved);
    }
  }

  async loadHistoricalData() {
    // Load and process historical transaction data
    const transactions = await this.transactionService.getAllTransactions();
    this.historicalData = transactions;
  }

  scheduleAlertChecks() {
    // Check alerts every hour
    setInterval(() => {
      this.checkAlerts();
    }, 60 * 60 * 1000);
    
    // Initial check
    setTimeout(() => this.checkAlerts(), 1000);
  }

  processAlerts(alerts) {
    alerts.forEach(alert => {
      // Send notification
      if (window.showNotification) {
        window.showNotification(alert.message, alert.severity);
      }
      
      // Log alert
      console.log(`Alert: ${alert.message}`, alert);
      
      // Store alert for history
      this.storeAlert(alert);
    });
  }

  storeAlert(alert) {
    const alerts = JSON.parse(localStorage.getItem('alert_history') || '[]');
    alerts.unshift({
      ...alert,
      timestamp: new Date().toISOString()
    });
    
    // Keep only last 100 alerts
    alerts.splice(100);
    
    localStorage.setItem('alert_history', JSON.stringify(alerts));
  }
}

export default BudgetIntelligence;