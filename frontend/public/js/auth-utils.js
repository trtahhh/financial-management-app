// Utility functions for authenticated API calls
const TOKEN_PRIMARY_KEY = 'accessToken';
const TOKEN_LEGACY_KEY = 'authToken';

function syncStoredTokens() {
  try {
    const primary = localStorage.getItem(TOKEN_PRIMARY_KEY);
    const legacy = localStorage.getItem(TOKEN_LEGACY_KEY);

    if (primary && !legacy) {
      localStorage.setItem(TOKEN_LEGACY_KEY, primary);
    } else if (!primary && legacy) {
      localStorage.setItem(TOKEN_PRIMARY_KEY, legacy);
    }
  } catch (error) {
    console.warn('Unable to sync auth tokens:', error);
  }
}

syncStoredTokens();

function storeAuthToken(token) {
  if (!token) return;
  localStorage.setItem(TOKEN_PRIMARY_KEY, token);
  localStorage.setItem(TOKEN_LEGACY_KEY, token);
}

function clearStoredTokens() {
  localStorage.removeItem(TOKEN_PRIMARY_KEY);
  localStorage.removeItem(TOKEN_LEGACY_KEY);
}

function getAuthToken() {
  return localStorage.getItem(TOKEN_PRIMARY_KEY) || localStorage.getItem(TOKEN_LEGACY_KEY);
}

function getAuthHeaders() {
  const token = getAuthToken();
  const headers = {
    'Content-Type': 'application/json'
  };

  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }

  return headers;
}

function checkAuth() {
  const token = getAuthToken();
  if (!token) {
    window.location.href = '/login';
    return false;
  }
  return true;
}

function logout(redirectTo = '/login') {
  clearStoredTokens();
  window.location.href = redirectTo;
}

async function authenticatedFetch(url, options = {}) {
  if (!checkAuth()) return;
  
  const defaultOptions = {
    headers: getAuthHeaders()
  };
  
  // Merge options
  const mergedOptions = {
    ...defaultOptions,
    ...options,
    headers: {
      ...defaultOptions.headers,
      ...(options.headers || {})
    }
  };
  
  try {
    const response = await fetch(url, mergedOptions);
    
    if (response.status === 401) {
      // Token expired or invalid
      clearStoredTokens();
      window.location.href = '/login';
      return;
    }
    
    return response;
  } catch (error) {
    console.error('API call failed:', error);
    throw error;
  }
}

// Function to normalize image URL (handle both old and new formats)
function normalizeImageUrl(imageUrl) {
  if (!imageUrl) return null;
  
  // Convert old format /api/files/uploads/xxx to new format /uploads/xxx
  if (imageUrl.startsWith('/api/files/uploads/')) {
    return imageUrl.replace('/api/files/uploads/', '/uploads/');
  }
  
  return imageUrl;
}

// Function to update avatar across all pages
function updateGlobalAvatar(imageUrl) {
  const normalizedUrl = normalizeImageUrl(imageUrl);
  
  // Update sidebar avatar
  const sidebarAvatar = document.querySelector('#userAvatar');
  if (sidebarAvatar) {
    sidebarAvatar.src = normalizedUrl || 'https://via.placeholder.com/40/ffffff/198754?text=U';
  }
  
  // Update profile page avatar if exists
  const profileImage = document.getElementById('profileImage');
  if (profileImage) {
    profileImage.src = normalizedUrl || 'https://via.placeholder.com/150/198754/ffffff?text=User';
  }
}

// Function to update user info globally 
function updateGlobalUserInfo(userData) {
  // Update sidebar user name
  const sidebarUserName = document.querySelector('#userName');
  if (sidebarUserName) {
    sidebarUserName.textContent = userData.fullName || userData.username || 'Người dùng';
  }
  
  // Update sidebar user role
  const sidebarUserRole = document.querySelector('#userRole');
  if (sidebarUserRole) {
    sidebarUserRole.textContent = userData.role || 'USER';
  }
  
  // Update avatar
  updateGlobalAvatar(userData.imageUrl);
}

// Export for use in other files
window.authUtils = {
  getAuthToken,
  getAuthHeaders,
  checkAuth,
  authenticatedFetch,
  logout,
  normalizeImageUrl,
  updateGlobalAvatar,
  updateGlobalUserInfo,
  syncStoredTokens,
  storeAuthToken,
  clearStoredTokens
};
