// Date manipulation and formatting helpers
export const formatDate = (date, format = 'DD/MM/YYYY') => {
  if (!date) return '';
  
  const d = new Date(date);
  if (isNaN(d.getTime())) return '';
  
  const day = String(d.getDate()).padStart(2, '0');
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const year = d.getFullYear();
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  
  switch (format) {
    case 'DD/MM/YYYY':
      return `${day}/${month}/${year}`;
    case 'MM/DD/YYYY':
      return `${month}/${day}/${year}`;
    case 'YYYY-MM-DD':
      return `${year}-${month}-${day}`;
    case 'DD/MM/YYYY HH:mm':
      return `${day}/${month}/${year} ${hours}:${minutes}`;
    case 'HH:mm':
      return `${hours}:${minutes}`;
    default:
      return `${day}/${month}/${year}`;
  }
};

// Get start of month
export const getStartOfMonth = (date = new Date()) => {
  const d = new Date(date);
  return new Date(d.getFullYear(), d.getMonth(), 1);
};

// Get end of month
export const getEndOfMonth = (date = new Date()) => {
  const d = new Date(date);
  return new Date(d.getFullYear(), d.getMonth() + 1, 0, 23, 59, 59);
};

// Get start of week (Monday)
export const getStartOfWeek = (date = new Date()) => {
  const d = new Date(date);
  const day = d.getDay();
  const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Adjust when day is Sunday
  return new Date(d.setDate(diff));
};

// Get days in month
export const getDaysInMonth = (date = new Date()) => {
  const d = new Date(date);
  return new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate();
};

// Calculate days between dates
export const getDaysBetween = (date1, date2) => {
  const oneDay = 24 * 60 * 60 * 1000; // hours*minutes*seconds*milliseconds
  const firstDate = new Date(date1);
  const secondDate = new Date(date2);
  
  return Math.round(Math.abs((firstDate - secondDate) / oneDay));
};

// Calculate days since date
export const getDaysSince = (date) => {
  return getDaysBetween(new Date(), date);
};

// Check if date is today
export const isToday = (date) => {
  const today = new Date();
  const checkDate = new Date(date);
  
  return today.getDate() === checkDate.getDate() &&
         today.getMonth() === checkDate.getMonth() &&
         today.getFullYear() === checkDate.getFullYear();
};

// Check if date is this month
export const isThisMonth = (date) => {
  const today = new Date();
  const checkDate = new Date(date);
  
  return today.getMonth() === checkDate.getMonth() &&
         today.getFullYear() === checkDate.getFullYear();
};

// Get relative time (2 hours ago, yesterday, etc.)
export const getRelativeTime = (date) => {
  const now = new Date();
  const targetDate = new Date(date);
  const diffInSeconds = Math.floor((now - targetDate) / 1000);
  
  if (diffInSeconds < 60) return 'Vừa xong';
  
  const diffInMinutes = Math.floor(diffInSeconds / 60);
  if (diffInMinutes < 60) return `${diffInMinutes} phút trước`;
  
  const diffInHours = Math.floor(diffInMinutes / 60);
  if (diffInHours < 24) return `${diffInHours} giờ trước`;
  
  const diffInDays = Math.floor(diffInHours / 24);
  if (diffInDays === 1) return 'Hôm qua';
  if (diffInDays < 7) return `${diffInDays} ngày trước`;
  
  const diffInWeeks = Math.floor(diffInDays / 7);
  if (diffInWeeks === 1) return 'Tuần trước';
  if (diffInWeeks < 4) return `${diffInWeeks} tuần trước`;
  
  const diffInMonths = Math.floor(diffInDays / 30);
  if (diffInMonths === 1) return 'Tháng trước';
  if (diffInMonths < 12) return `${diffInMonths} tháng trước`;
  
  const diffInYears = Math.floor(diffInDays / 365);
  return `${diffInYears} năm trước`;
};

// Generate date ranges for reports
export const getDateRanges = () => {
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);
  
  return {
    today: {
      start: new Date(today.getFullYear(), today.getMonth(), today.getDate()),
      end: new Date(today.getFullYear(), today.getMonth(), today.getDate(), 23, 59, 59),
      label: 'Hôm nay'
    },
    yesterday: {
      start: new Date(yesterday.getFullYear(), yesterday.getMonth(), yesterday.getDate()),
      end: new Date(yesterday.getFullYear(), yesterday.getMonth(), yesterday.getDate(), 23, 59, 59),
      label: 'Hôm qua'
    },
    thisWeek: {
      start: getStartOfWeek(today),
      end: today,
      label: 'Tuần này'
    },
    thisMonth: {
      start: getStartOfMonth(today),
      end: getEndOfMonth(today),
      label: 'Tháng này'
    },
    lastMonth: {
      start: getStartOfMonth(new Date(today.getFullYear(), today.getMonth() - 1, 1)),
      end: getEndOfMonth(new Date(today.getFullYear(), today.getMonth() - 1, 1)),
      label: 'Tháng trước'
    },
    last30Days: {
      start: new Date(today.getFullYear(), today.getMonth(), today.getDate() - 30),
      end: today,
      label: '30 ngày qua'
    },
    last90Days: {
      start: new Date(today.getFullYear(), today.getMonth(), today.getDate() - 90),
      end: today,
      label: '90 ngày qua'
    }
  };
};