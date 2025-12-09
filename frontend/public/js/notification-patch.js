// Patch để fix notification message undefined
console.log(" 🔧 notification-patch.js loaded");

// Override displayNotifications function để xử lý undefined message
const originalDisplayNotifications = window.displayNotifications;
window.displayNotifications = function(notifications) {
    console.log(" 🔧 displayNotifications intercepted - fixing undefined messages");
    console.log(" 🔧 Notifications data:", notifications);
    
    const container = document.getElementById('notificationList');
    const markAllBtn = document.getElementById('markAllBtn');
    
    if (!container) {
        console.warn(" ⚠️ notificationList container not found");
        return;
    }
    
    if (!notifications || notifications.length === 0) {
        container.innerHTML = ` 
            <li class="text-center py-3 text-muted">
                <i class="fas fa-bell-slash"></i><br>
                Không có thông báo mới
            </li>
        `;
        if (markAllBtn) markAllBtn.style.display = 'none';
        console.log(" 🔧 No notifications - showing empty state");
        return;
    }
    
    if (markAllBtn) markAllBtn.style.display = 'inline';
    
    container.innerHTML = notifications.map((notif, index) => {
        // FIX: Handle undefined title and message
        const title = notif.title || notif.type || 'Thông báo';
        const message = notif.message || notif.title || 'Bạn có thông báo mới';
        
        const icon = getNotificationIcon(notif.type);
        const time = formatTimeAgo(notif.createdAt);
        const color = getNotificationColor(notif.type);
        
        console.log(` 🔧 Notification ${index}:`, { title, message, icon, time, color });
        
        return `
            <li>
                <a class="dropdown-item py-2 ${notif.isRead ? 'text-muted' : ''}" href="#" onclick="markAsReadAndNavigate(${notif.id}, '${notif.link || '#'}'); return false;">
                    <div class="d-flex align-items-start">
                        <div class="me-2 text-${color}">
                            <i class="${icon} fa-fw"></i>
                        </div>
                        <div class="flex-grow-1 small">
                            <div class="fw-bold">${title}</div>
                            <div class="text-muted small">${message}</div>
                            <div class="text-muted" style="font-size: 0.75rem;">
                                <i class="far fa-clock"></i> ${time}
                            </div>
                        </div>
                        ${!notif.isRead ? '<span class="badge bg-primary rounded-circle" style="width: 8px; height: 8px;"></span>' : ''}
                    </div>
                </a>
            </li>
            <li><hr class="dropdown-divider m-0"></li>
        `;
    }).join('');
    
    console.log(` 🔧 Rendered ${notifications.length} notifications`);
};

console.log(" 🔧 notification-patch.js: displayNotifications override applied");
