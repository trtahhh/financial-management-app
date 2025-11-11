/**
 * SMART NAVIGATION SYSTEM - Hệ thống điều hướng thông minh
 * Giúp người dùng di chuyển giữa các trang liên quan
 */

class SmartNavigation {
 static init() {
 this.addQuickActions();
 this.addContextualLinks();
 this.addShortcuts();
 }
 
 /**
 * Thêm quick actions vào mỗi trang
 */
 static addQuickActions() {
 const currentPage = this.getCurrentPage();
 const quickActionsHtml = this.getQuickActionsForPage(currentPage);
 
 if (quickActionsHtml) {
 const container = document.createElement('div');
 container.className = 'quick-actions-container';
 container.style.cssText = `
 position: fixed;
 bottom: 20px;
 right: 20px;
 z-index: 1000;
 `;
 container.innerHTML = quickActionsHtml;
 document.body.appendChild(container);
 }
 }
 
 /**
 * Lấy trang hiện tại
 */
 static getCurrentPage() {
 const path = window.location.pathname;
 if (path.includes('dashboard')) return 'dashboard';
 if (path.includes('transactions')) return 'transactions';
 if (path.includes('budgets')) return 'budgets';
 if (path.includes('goals')) return 'goals';
 if (path.includes('wallets')) return 'wallets';
 if (path.includes('categories')) return 'categories';
 if (path.includes('chat')) return 'chat';
 return 'home';
 }
 
 /**
 * Lấy quick actions cho từng trang
 */
 static getQuickActionsForPage(page) {
 const actions = {
 dashboard: `
 <div class="btn-group-vertical" role="group">
 </div>
 `,
 transactions: `
 <div class="btn-group-vertical" role="group">
 </div>
 `,
 budgets: `
 <div class="btn-group-vertical" role="group">
 </div>
 `,
 goals: `
 <div class="btn-group-vertical" role="group">
 </div>
 `,
 wallets: `
 <div class="btn-group-vertical" role="group">
 </div>
 `
 };
 
 return actions[page] || null;
 }
 
 /**
 * Thêm contextual links vào nội dung trang
 */
 static addContextualLinks() {
 const page = this.getCurrentPage();
 
 switch (page) {
 case 'dashboard':
 this.addDashboardLinks();
 break;
 case 'transactions':
 this.addTransactionLinks();
 break;
 case 'budgets':
 this.addBudgetLinks();
 break;
 }
 }
 
 /**
 * Thêm links cho dashboard
 */
 static addDashboardLinks() {
 // Add links to budget usage percentages
 setTimeout(() => {
 const budgetElements = document.querySelectorAll('[data-budget-category]');
 budgetElements.forEach(el => {
 el.style.cursor = 'pointer';
 el.setAttribute('title', 'Click để xem chi tiết ngân sách');
 el.addEventListener('click', () => {
 const categoryId = el.dataset.budgetCategory;
 this.goTo(`/budgets?category=${categoryId}`);
 });
 });
 
 // Add links to goal progress
 const goalElements = document.querySelectorAll('[data-goal-id]');
 goalElements.forEach(el => {
 el.style.cursor = 'pointer';
 el.setAttribute('title', 'Click để xem chi tiết mục tiêu');
 el.addEventListener('click', () => {
 const goalId = el.dataset.goalId;
 this.goTo(`/goals?goal=${goalId}`);
 });
 });
 }, 1000);
 }
 
 /**
 * Thêm links cho transactions
 */
 static addTransactionLinks() {
 setTimeout(() => {
 // Add category links
 const categoryElements = document.querySelectorAll('.transaction-category');
 categoryElements.forEach(el => {
 el.style.cursor = 'pointer';
 el.setAttribute('title', 'Click để xem ngân sách danh mục này');
 el.addEventListener('click', (e) => {
 e.stopPropagation();
 const categoryName = el.textContent.trim();
 this.goTo(`/budgets?search=${encodeURIComponent(categoryName)}`);
 });
 });
 }, 1000);
 }
 
 /**
 * Thêm links cho budgets
 */
 static addBudgetLinks() {
 setTimeout(() => {
 // Add transaction view links to budget rows
 const budgetRows = document.querySelectorAll('[data-budget-id]');
 budgetRows.forEach(row => {
 const button = document.createElement('button');
 button.className = 'btn btn-sm btn-outline-primary';
 button.innerHTML = '<i class="fas fa-list"></i> Xem giao dịch';
 button.onclick = () => {
 const categoryId = row.dataset.categoryId;
 this.goTo(`/transactions?category=${categoryId}`);
 };
 
 const actionsCell = row.querySelector('.actions-cell') || row.lastElementChild;
 if (actionsCell) {
 actionsCell.appendChild(button);
 }
 });
 }, 1000);
 }
 
 /**
 * Thêm keyboard shortcuts
 */
 static addShortcuts() {
 document.addEventListener('keydown', (e) => {
 // Ctrl/Cmd + key combinations
 if (e.ctrlKey || e.metaKey) {
 switch (e.key) {
 case 'd':
 e.preventDefault();
 this.goTo('/dashboard');
 break;
 case 't':
 e.preventDefault();
 this.goTo('/transactions');
 break;
 case 'b':
 e.preventDefault();
 this.goTo('/budgets');
 break;
 case 'g':
 e.preventDefault();
 this.goTo('/goals');
 break;
 case 'w':
 e.preventDefault();
 this.goTo('/wallets');
 break;
 case 'n':
 e.preventDefault();
 this.openQuickAdd();
 break;
 }
 }
 });
 
 // Show shortcuts help
 this.addShortcutsHelp();
 }
 
 /**
 * Hiển thị help cho shortcuts
 */
 static addShortcutsHelp() {
 const helpButton = document.createElement('button');
 helpButton.className = 'btn btn-sm btn-outline-secondary shortcuts-help';
 helpButton.innerHTML = '<i class="fas fa-keyboard"></i>';
 helpButton.style.cssText = `
 position: fixed;
 bottom: 20px;
 left: 20px;
 z-index: 1000;
 `;
 helpButton.setAttribute('title', 'Phím tắt (Ctrl+D: Dashboard, Ctrl+T: Transactions, Ctrl+B: Budgets, Ctrl+G: Goals, Ctrl+N: Thêm nhanh)');
 
 document.body.appendChild(helpButton);
 }
 
 /**
 * Navigation utilities
 */
 static goTo(path) {
 window.location.href = path;
 }
 
 static openTransactionModal() {
 const modal = document.getElementById('transactionModal');
 if (modal) {
 const bsModal = new bootstrap.Modal(modal);
 bsModal.show();
 } else {
 this.goTo('/transactions');
 }
 }
 
 static openBudgetModal() {
 const modal = document.getElementById('budget-modal');
 if (modal) {
 const bsModal = new bootstrap.Modal(modal);
 bsModal.show();
 } else {
 this.goTo('/budgets');
 }
 }
 
 static openGoalModal() {
 const modal = document.getElementById('goal-modal');
 if (modal) {
 const bsModal = new bootstrap.Modal(modal);
 bsModal.show();
 } else {
 this.goTo('/goals');
 }
 }
 
 static openWalletModal() {
 const modal = document.getElementById('wallet-modal');
 if (modal) {
 const bsModal = new bootstrap.Modal(modal);
 bsModal.show();
 } else {
 this.goTo('/wallets');
 }
 }
 
 static openQuickAdd() {
 const page = this.getCurrentPage();
 
 switch (page) {
 case 'dashboard':
 case 'transactions':
 this.openTransactionModal();
 break;
 case 'budgets':
 this.openBudgetModal();
 break;
 case 'goals':
 this.openGoalModal();
 break;
 case 'wallets':
 this.openWalletModal();
 break;
 default:
 this.goTo('/transactions');
 }
 }
}

/**
 * BREADCRUMB SYSTEM - Hệ thống breadcrumb thông minh
 */
class SmartBreadcrumb {
 static init() {
 // this.addBreadcrumb(); // Disabled to avoid breadcrumb duplication
 }
 
 static addBreadcrumb() {
 const breadcrumbData = this.getBreadcrumbData();
 if (!breadcrumbData.length) return;
 
 const breadcrumbHtml = `
 <nav aria-label="breadcrumb" class="mb-3">
 <ol class="breadcrumb">
 ${breadcrumbData.map(item => `
 <li class="breadcrumb-item ${item.active ? 'active' : ''}">
 ${item.active ? item.name : `<a href="${item.url}" class="text-success">${item.name}</a>`}
 </li>
 `).join('')}
 </ol>
 </nav>
 `;
 
 const mainContent = document.querySelector('main.container-fluid');
 if (mainContent) {
 mainContent.insertAdjacentHTML('afterbegin', breadcrumbHtml);
 }
 }
 
 static getBreadcrumbData() {
 const path = window.location.pathname;
 const params = new URLSearchParams(window.location.search);
 
 const breadcrumbs = [
 { name: 'Trang chủ', url: '/dashboard' }
 ];
 
 if (path.includes('dashboard')) {
 breadcrumbs.push({ name: 'Dashboard', active: true });
 } else if (path.includes('transactions')) {
 breadcrumbs.push({ name: 'Giao dịch', active: true });
 if (params.get('category')) {
 breadcrumbs.push({ name: `Danh mục: ${params.get('category')}`, active: true });
 }
 } else if (path.includes('budgets')) {
 breadcrumbs.push({ name: 'Ngân sách', active: true });
 if (params.get('category')) {
 breadcrumbs.push({ name: `Danh mục: ${params.get('category')}`, active: true });
 }
 } else if (path.includes('goals')) {
 breadcrumbs.push({ name: 'Mục tiêu', active: true });
 } else if (path.includes('wallets')) {
 breadcrumbs.push({ name: 'Ví', active: true });
 } else if (path.includes('categories')) {
 breadcrumbs.push({ name: 'Danh mục', active: true });
 } else if (path.includes('chat')) {
 breadcrumbs.push({ name: 'AI Chat', active: true });
 }
 
 return breadcrumbs;
 }
}

// Initialize when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
 SmartNavigation.init();
 // SmartBreadcrumb.init(); // Disabled to avoid breadcrumb duplication
});

// Make available globally
window.SmartNavigation = SmartNavigation;
window.SmartBreadcrumb = SmartBreadcrumb;

console.log(" Smart Navigation System loaded!");
