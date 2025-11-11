-- Insert initial achievements
INSERT INTO achievements (code, name, description, icon, category, points, tier, criteria, is_active, created_at, updated_at) VALUES
-- Transaction achievements
('FIRST_TRANSACTION', 'First Step', 'Record your first transaction', '💰', 'transaction', 10, 'bronze', '{"type": "transaction_count", "count": 1}', 1, NOW(), NOW()),
('TRANSACTION_ROOKIE', 'Transaction Rookie', 'Record 10 transactions', '📝', 'transaction', 25, 'bronze', '{"type": "transaction_count", "count": 10}', 1, NOW(), NOW()),
('TRANSACTION_PRO', 'Transaction Pro', 'Record 50 transactions', '📊', 'transaction', 50, 'silver', '{"type": "transaction_count", "count": 50}', 1, NOW(), NOW()),
('TRANSACTION_MASTER', 'Transaction Master', 'Record 100 transactions', '🏆', 'transaction', 100, 'gold', '{"type": "transaction_count", "count": 100}', 1, NOW(), NOW()),
('TRANSACTION_LEGEND', 'Transaction Legend', 'Record 500 transactions', '👑', 'transaction', 200, 'platinum', '{"type": "transaction_count", "count": 500}', 1, NOW(), NOW()),

-- Budget achievements
('BUDGET_BEGINNER', 'Budget Beginner', 'Create your first budget', '📋', 'budget', 15, 'bronze', '{"type": "budget_created"}', 1, NOW(), NOW()),
('BUDGET_PLANNER', 'Budget Planner', 'Create 5 budgets', '📅', 'budget', 30, 'silver', '{"type": "budget_created", "count": 5}', 1, NOW(), NOW()),
('BUDGET_MASTER', 'Budget Master', 'Successfully stick to budget for 3 months', '💼', 'budget', 100, 'gold', '{"type": "budget_adherence", "months": 3}', 1, NOW(), NOW()),

-- Goal achievements
('GOAL_SETTER', 'Goal Setter', 'Create your first financial goal', '🎯', 'goal', 15, 'bronze', '{"type": "goal_created"}', 1, NOW(), NOW()),
('GOAL_ACHIEVER', 'Goal Achiever', 'Complete your first goal', '✅', 'goal', 50, 'silver', '{"type": "goal_achieved", "count": 1}', 1, NOW(), NOW()),
('GOAL_CHAMPION', 'Goal Champion', 'Complete 5 financial goals', '🏅', 'goal', 150, 'gold', '{"type": "goal_achieved", "count": 5}', 1, NOW(), NOW()),

-- Saving achievements
('SAVING_STARTER', 'Saving Starter', 'Save your first 100,000 VND', '🐷', 'saving', 20, 'bronze', '{"type": "total_savings", "amount": 100000}', 1, NOW(), NOW()),
('SAVING_HERO', 'Saving Hero', 'Save 1,000,000 VND', '💪', 'saving', 50, 'silver', '{"type": "total_savings", "amount": 1000000}', 1, NOW(), NOW()),
('SAVING_CHAMPION', 'Saving Champion', 'Save 5,000,000 VND', '🌟', 'saving', 100, 'gold', '{"type": "total_savings", "amount": 5000000}', 1, NOW(), NOW()),
('SAVING_LEGEND', 'Saving Legend', 'Save 10,000,000 VND', '💎', 'saving', 200, 'platinum', '{"type": "total_savings", "amount": 10000000}', 1, NOW(), NOW()),

-- Streak achievements
('STREAK_BEGINNER', 'Consistent Beginner', 'Track expenses for 7 consecutive days', '🔥', 'streak', 25, 'bronze', '{"type": "saving_streak", "days": 7}', 1, NOW(), NOW()),
('STREAK_DEDICATED', 'Dedicated Tracker', 'Track expenses for 30 consecutive days', '⚡', 'streak', 75, 'silver', '{"type": "saving_streak", "days": 30}', 1, NOW(), NOW()),
('STREAK_MASTER', 'Streak Master', 'Track expenses for 90 consecutive days', '🌪️', 'streak', 150, 'gold', '{"type": "saving_streak", "days": 90}', 1, NOW(), NOW()),

-- Category-specific achievements
('FOOD_CONSCIOUS', 'Food Conscious', 'Track 20 food expenses', '🍜', 'category', 30, 'bronze', '{"type": "spending_category", "category": "Food & Drink", "count": 20}', 1, NOW(), NOW()),
('EDUCATION_INVESTOR', 'Education Investor', 'Track 10 education expenses', '📚', 'category', 40, 'silver', '{"type": "spending_category", "category": "Education", "count": 10}', 1, NOW(), NOW()),
('ENTERTAINMENT_MANAGER', 'Entertainment Manager', 'Track 15 entertainment expenses', '🎮', 'category', 30, 'bronze', '{"type": "spending_category", "category": "Entertainment", "count": 15}', 1, NOW(), NOW());

-- Insert initial challenges
INSERT INTO challenges (code, name, description, type, category, target_value, reward_points, start_date, end_date, is_active, created_at, updated_at) VALUES
-- Weekly challenges
('WEEKLY_10_TRANSACTIONS', 'Weekly Transaction Goal', 'Record 10 transactions this week', 'weekly', 'transactions', 10, 50, DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY), INTERVAL 6 DAY), 1, NOW(), NOW()),
('WEEKLY_BUDGET_ADHERENCE', 'Weekly Budget Master', 'Stay within budget for the week', 'weekly', 'budget_adherence', 90, 75, DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY), INTERVAL 6 DAY), 1, NOW(), NOW()),
('WEEKLY_SAVING', 'Weekly Saver', 'Save 200,000 VND this week', 'weekly', 'savings', 200000, 60, DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL WEEKDAY(NOW()) DAY), INTERVAL 6 DAY), 1, NOW(), NOW()),

-- Monthly challenges
('MONTHLY_50_TRANSACTIONS', 'Monthly Transaction Champion', 'Record 50 transactions this month', 'monthly', 'transactions', 50, 150, DATE_FORMAT(NOW(), '%Y-%m-01'), LAST_DAY(NOW()), 1, NOW(), NOW()),
('MONTHLY_BUDGET_MASTER', 'Monthly Budget Expert', 'Maintain 95% budget adherence for the month', 'monthly', 'budget_adherence', 95, 200, DATE_FORMAT(NOW(), '%Y-%m-01'), LAST_DAY(NOW()), 1, NOW(), NOW()),
('MONTHLY_SAVING_GOAL', 'Monthly Saving Hero', 'Save 1,000,000 VND this month', 'monthly', 'savings', 1000000, 250, DATE_FORMAT(NOW(), '%Y-%m-01'), LAST_DAY(NOW()), 1, NOW(), NOW()),
('MONTHLY_STREAK', 'Monthly Consistency', 'Maintain 30-day streak this month', 'monthly', 'streak', 30, 300, DATE_FORMAT(NOW(), '%Y-%m-01'), LAST_DAY(NOW()), 1, NOW(), NOW());
