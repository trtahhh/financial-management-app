// Patch để fix balance display từ wallet thực tế
// Hàm lấy current balance từ endpoint mới

function fetchCurrentBalance() {
    const token = localStorage.getItem('accessToken');
    const url = 'http://localhost:8080/api/dashboard/current-balance';
    
    console.log(" Fetching current balance from:", url);
    
    const headers = {
        'Content-Type': 'application/json'
    };
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    
    return fetch(url, {
        method: 'GET',
        headers: headers,
        credentials: 'include',
        mode: 'cors'
    })
    .then(res => {
        console.log(" Current balance response status:", res.status);
        if (!res.ok) {
            return res.text().then(text => {
                console.error("Current balance error:", text);
                throw new Error(`HTTP ${res.status}: ${text}`);
            });
        }
        return res.json();
    })
    .then(data => {
        console.log(" Current balance data received:", data);
        return data;
    })
    .catch(err => {
        console.error(" Current balance fetch failed:", err);
        throw err;
    });
}

// Override updateDashboardUI to use current balance
const originalUpdateDashboardUI = window.updateDashboardUI;
window.updateDashboardUI = function(data) {
    console.log(" updateDashboardUI intercepted with patch");
    
    // Gọi hàm gốc
    originalUpdateDashboardUI.call(this, data);
    
    // Sau đó update balance từ current-balance endpoint
    fetchCurrentBalance()
        .then(balanceData => {
            const totalBalance = balanceData.totalBalance || 0;
            console.log(" Patched balance display with current wallet balance:", totalBalance);
            const balanceEl = document.getElementById('balance');
            if (balanceEl) {
                balanceEl.textContent = totalBalance.toLocaleString('vi-VN') + ' VND';
            }
        })
        .catch(err => {
            console.warn(" Patch: Failed to get current balance, keeping original:", err);
        });
};

console.log(" balance-patch.js loaded - currentBalance display patch active");
