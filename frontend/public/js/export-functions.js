/**
 * Export và Utility Functions cho Financial Management App
 * Chứa các hàm export CSV, PDF, print, share...
 */

// ==================== DASHBOARD EXPORTS ====================

/**
 * Export Dashboard Data as CSV/PDF
 */
function exportDashboard() {
    const format = prompt('Chọn định dạng xuất:\n1 - CSV\n2 - PDF\n3 - Excel', '1');
    
    if (format === '1') {
        exportDashboardCSV();
    } else if (format === '2') {
        exportDashboardPDF();
    } else if (format === '3') {
        alert('Tính năng Excel đang được phát triển');
    }
}

/**
 * Export Dashboard as CSV
 */
function exportDashboardCSV() {
    try {
        // Collect dashboard data
        const totalIncome = document.getElementById('totalIncome')?.textContent || '0';
        const totalExpense = document.getElementById('totalExpense')?.textContent || '0';
        const balance = document.getElementById('balance')?.textContent || '0';
        
        const dateFrom = document.getElementById('dash-date-from')?.value || 'N/A';
        const dateTo = document.getElementById('dash-date-to')?.value || 'N/A';
        
        // Create CSV content
        let csv = 'Dashboard Report - Finance AI\n\n';
        csv += 'Thời gian:,' + new Date().toLocaleString('vi-VN') + '\n';
        csv += 'Từ ngày:,' + dateFrom + '\n';
        csv += 'Đến ngày:,' + dateTo + '\n\n';
        csv += 'Chỉ số,Giá trị\n';
        csv += 'Tổng Thu,' + totalIncome + '\n';
        csv += 'Tổng Chi,' + totalExpense + '\n';
        csv += 'Số Dư,' + balance + '\n';
        
        // Download CSV
        downloadCSV(csv, 'dashboard-report-' + new Date().toISOString().split('T')[0] + '.csv');
        
        showNotification('Đã xuất báo cáo Dashboard thành công!', 'success');
    } catch (error) {
        console.error('Error exporting dashboard:', error);
        alert('Lỗi khi xuất báo cáo: ' + error.message);
    }
}

/**
 * Export Dashboard as PDF
 */
function exportDashboardPDF() {
    alert('Tính năng xuất PDF đang được phát triển.\nVui lòng sử dụng tính năng In để lưu dưới dạng PDF.');
}

/**
 * Print Dashboard
 */
function printDashboard() {
    // Create print-friendly version
    const printContent = `
        <html>
        <head>
            <title>Dashboard Report - Finance AI</title>
            <style>
                body { font-family: Arial, sans-serif; margin: 20px; }
                h1 { color: #198754; text-align: center; }
                .stats { display: flex; justify-content: space-around; margin: 30px 0; }
                .stat-box { text-align: center; padding: 20px; border: 1px solid #ddd; border-radius: 8px; }
                .stat-label { color: #666; font-size: 14px; }
                .stat-value { font-size: 28px; font-weight: bold; margin-top: 10px; color: #333; }
                .footer { text-align: center; margin-top: 50px; color: #999; font-size: 12px; }
            </style>
        </head>
        <body>
            <h1>📊 Dashboard Report</h1>
            <p style="text-align: center; color: #666;">Ngày xuất: ${new Date().toLocaleString('vi-VN')}</p>
            
            <div class="stats">
                <div class="stat-box">
                    <div class="stat-label">Tổng Thu</div>
                    <div class="stat-value" style="color: #198754;">${document.getElementById('totalIncome')?.textContent || '0'}</div>
                </div>
                <div class="stat-box">
                    <div class="stat-label">Tổng Chi</div>
                    <div class="stat-value" style="color: #dc3545;">${document.getElementById('totalExpense')?.textContent || '0'}</div>
                </div>
                <div class="stat-box">
                    <div class="stat-label">Số Dư</div>
                    <div class="stat-value" style="color: #0dcaf0;">${document.getElementById('balance')?.textContent || '0'}</div>
                </div>
            </div>
            
            <div class="footer">
                Finance AI - Quản lý tài chính thông minh<br>
                © 2025 All rights reserved
            </div>
        </body>
        </html>
    `;
    
    const printWindow = window.open('', '_blank');
    printWindow.document.write(printContent);
    printWindow.document.close();
    printWindow.print();
}

// ==================== TRANSACTIONS EXPORTS ====================

/**
 * Export Transactions as CSV
 */
function exportTransactions() {
    try {
        // Get all transaction rows from the table
        const rows = document.querySelectorAll('#transaction-tbody tr');
        
        if (rows.length === 0) {
            alert('Không có giao dịch nào để xuất!');
            return;
        }
        
        // Create CSV header
        let csv = 'Ngày,Danh Mục,Mô Tả,Ví,Số Tiền,Loại\n';
        
        // Add each transaction
        rows.forEach(row => {
            const cells = row.querySelectorAll('td');
            if (cells.length >= 6) {
                const date = cells[1]?.textContent.trim() || '';
                const category = cells[2]?.textContent.trim() || '';
                const description = cells[3]?.textContent.trim().replace(/,/g, ' ') || '';
                const wallet = cells[4]?.textContent.trim() || '';
                const amount = cells[5]?.textContent.trim() || '';
                
                csv += `${date},"${category}","${description}","${wallet}","${amount}"\n`;
            }
        });
        
        // Download CSV
        downloadCSV(csv, 'transactions-' + new Date().toISOString().split('T')[0] + '.csv');
        
        showNotification('Đã xuất ' + rows.length + ' giao dịch thành công!', 'success');
    } catch (error) {
        console.error('Error exporting transactions:', error);
        alert('Lỗi khi xuất giao dịch: ' + error.message);
    }
}

/**
 * Bulk delete selected transactions
 */
function bulkDeleteTransactions() {
    const selectedCheckboxes = document.querySelectorAll('input[type="checkbox"]:checked:not(#selectAll)');
    
    if (selectedCheckboxes.length === 0) {
        alert('Vui lòng chọn ít nhất một giao dịch để xóa!');
        return;
    }
    
    if (!confirm(`Bạn có chắc muốn xóa ${selectedCheckboxes.length} giao dịch đã chọn?`)) {
        return;
    }
    
    // Get transaction IDs
    const transactionIds = Array.from(selectedCheckboxes).map(cb => {
        const row = cb.closest('tr');
        return row?.dataset.transactionId || row?.dataset.id;
    }).filter(id => id);
    
    if (transactionIds.length === 0) {
        alert('Không tìm thấy ID giao dịch để xóa!');
        return;
    }
    
    // Call API to delete (implementation would go here)
    console.log('Deleting transaction IDs:', transactionIds);
    alert(`Tính năng xóa nhiều đang được phát triển.\nSẽ xóa ${transactionIds.length} giao dịch.`);
}

// ==================== BUDGET EXPORTS ====================

/**
 * Export Budget Report
 */
function exportBudgetReport() {
    try {
        const period = document.getElementById('budget-period')?.value || new Date().toISOString().slice(0, 7);
        
        // Create CSV
        let csv = 'Budget Report - Finance AI\n\n';
        csv += 'Kỳ:,' + period + '\n';
        csv += 'Ngày xuất:,' + new Date().toLocaleString('vi-VN') + '\n\n';
        csv += 'Danh Mục,Ngân Sách,Đã Chi,Còn Lại,Tỷ Lệ\n';
        
        // Get budget cards
        const budgetCards = document.querySelectorAll('.budget-card');
        budgetCards.forEach(card => {
            const category = card.querySelector('.budget-category')?.textContent.trim() || '';
            const amount = card.querySelector('.budget-amount')?.textContent.trim() || '0';
            const spent = card.querySelector('.budget-spent')?.textContent.trim() || '0';
            const remaining = card.querySelector('.budget-remaining')?.textContent.trim() || '0';
            const percentage = card.querySelector('.budget-percentage')?.textContent.trim() || '0%';
            
            csv += `"${category}","${amount}","${spent}","${remaining}",${percentage}\n`;
        });
        
        downloadCSV(csv, 'budget-report-' + period + '.csv');
        showNotification('Đã xuất báo cáo ngân sách thành công!', 'success');
    } catch (error) {
        console.error('Error exporting budget report:', error);
        alert('Lỗi khi xuất báo cáo: ' + error.message);
    }
}

// ==================== GOALS EXPORTS ====================

/**
 * Export Goals Progress
 */
function exportGoalsProgress() {
    try {
        // Create CSV
        let csv = 'Goals Progress Report - Finance AI\n\n';
        csv += 'Ngày xuất:,' + new Date().toLocaleString('vi-VN') + '\n\n';
        csv += 'Mục Tiêu,Mục Tiêu,Hiện Tại,Còn Lại,Tiến Độ,Deadline,Trạng Thái\n';
        
        // Get goal cards
        const goalCards = document.querySelectorAll('.goal-card');
        goalCards.forEach(card => {
            const name = card.querySelector('.goal-name')?.textContent.trim() || '';
            const target = card.querySelector('.goal-target')?.textContent.trim() || '0';
            const current = card.querySelector('.goal-current')?.textContent.trim() || '0';
            const remaining = card.querySelector('.goal-remaining')?.textContent.trim() || '0';
            const progress = card.querySelector('.goal-progress')?.textContent.trim() || '0%';
            const deadline = card.querySelector('.goal-deadline')?.textContent.trim() || '';
            const status = card.querySelector('.goal-status')?.textContent.trim() || '';
            
            csv += `"${name}","${target}","${current}","${remaining}",${progress},"${deadline}","${status}"\n`;
        });
        
        downloadCSV(csv, 'goals-progress-' + new Date().toISOString().split('T')[0] + '.csv');
        showNotification('Đã xuất tiến độ mục tiêu thành công!', 'success');
    } catch (error) {
        console.error('Error exporting goals:', error);
        alert('Lỗi khi xuất tiến độ: ' + error.message);
    }
}

// ==================== HEALTH SCORE EXPORTS ====================

/**
 * Export Health Score Report
 */
function exportHealthReport() {
    try {
        const score = document.getElementById('health-score')?.textContent || '0';
        const rating = document.getElementById('health-rating')?.textContent || '';
        
        // Create CSV
        let csv = 'Financial Health Report - Finance AI\n\n';
        csv += 'Ngày đánh giá:,' + new Date().toLocaleString('vi-VN') + '\n';
        csv += 'Điểm số:,' + score + '\n';
        csv += 'Xếp hạng:,' + rating + '\n\n';
        csv += 'Danh Mục,Điểm,Nhận Xét\n';
        
        // Get category scores
        const categories = document.querySelectorAll('.category-score');
        categories.forEach(cat => {
            const name = cat.querySelector('.category-name')?.textContent.trim() || '';
            const catScore = cat.querySelector('.category-value')?.textContent.trim() || '0';
            const comment = cat.querySelector('.category-comment')?.textContent.trim().replace(/,/g, ' ') || '';
            
            csv += `"${name}",${catScore},"${comment}"\n`;
        });
        
        downloadCSV(csv, 'health-report-' + new Date().toISOString().split('T')[0] + '.csv');
        showNotification('Đã xuất báo cáo sức khỏe tài chính!', 'success');
    } catch (error) {
        console.error('Error exporting health report:', error);
        alert('Lỗi khi xuất báo cáo: ' + error.message);
    }
}

/**
 * Share Health Score
 */
function shareHealthScore() {
    const score = document.getElementById('health-score')?.textContent || '0';
    const rating = document.getElementById('health-rating')?.textContent || '';
    
    const shareText = `Điểm sức khỏe tài chính của tôi: ${score}/100 - ${rating} 📊\n\nQuản lý tài chính thông minh với Finance AI!`;
    
    if (navigator.share) {
        navigator.share({
            title: 'Điểm Sức Khỏe Tài Chính',
            text: shareText,
            url: window.location.href
        }).then(() => {
            showNotification('Đã chia sẻ thành công!', 'success');
        }).catch(err => {
            console.log('Share cancelled or failed:', err);
        });
    } else {
        // Fallback: Copy to clipboard
        navigator.clipboard.writeText(shareText).then(() => {
            alert('Đã copy nội dung vào clipboard!\n\n' + shareText);
        }).catch(err => {
            alert('Không thể chia sẻ: ' + err.message);
        });
    }
}

/**
 * Refresh Health Score
 */
function refreshHealthScore() {
    const btn = event?.target?.closest('button');
    const icon = btn?.querySelector('i');
    
    if (icon) {
        icon.classList.add('fa-spin');
    }
    
    // Call API to recalculate health score
    if (typeof loadFinancialHealthScore === 'function') {
        loadFinancialHealthScore();
    } else {
        alert('Đang tải lại dữ liệu...');
    }
    
    setTimeout(() => {
        if (icon) {
            icon.classList.remove('fa-spin');
        }
    }, 1500);
}

// ==================== PLANNING EXPORTS ====================

/**
 * Export Planning Report
 */
function exportPlanReport() {
    alert('Tính năng xuất kế hoạch đang được phát triển.\nBạn có thể in trang để lưu dưới dạng PDF.');
}

/**
 * Compare Plans
 */
function comparePlans() {
    alert('Tính năng so sánh kế hoạch đang được phát triển.');
}

// ==================== UTILITY FUNCTIONS ====================

/**
 * Download CSV File
 */
function downloadCSV(csvContent, filename) {
    // Add BOM for UTF-8
    const BOM = '\uFEFF';
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

/**
 * Show notification message
 */
function showNotification(message, type = 'info') {
    // Try to use existing notification system
    if (typeof showToast === 'function') {
        showToast(message, type);
        return;
    }
    
    // Fallback: Create simple notification
    const notification = document.createElement('div');
    notification.className = `alert alert-${type === 'success' ? 'success' : 'info'} position-fixed top-0 end-0 m-3`;
    notification.style.zIndex = '9999';
    notification.style.minWidth = '300px';
    notification.innerHTML = `
        <div class="d-flex align-items-center">
            <i class="bi bi-${type === 'success' ? 'check-circle' : 'info-circle'} me-2"></i>
            <div>${message}</div>
        </div>
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.remove();
    }, 3000);
}

// Export functions for use in other modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        exportDashboard,
        exportTransactions,
        bulkDeleteTransactions,
        exportBudgetReport,
        exportGoalsProgress,
        exportHealthReport,
        shareHealthScore,
        refreshHealthScore,
        exportPlanReport,
        comparePlans,
        printDashboard
    };
}
