// Utility functions for authenticated API calls
function getAuthHeaders() {
  const token = localStorage.getItem('authToken');
  return {
    'Authorization': 'Bearer ' + token,
    'Content-Type': 'application/json'
  };
}

function checkAuth() {
  const token = localStorage.getItem('authToken');
  if (!token) {
    window.location.href = '/login';
    return false;
  }
  return true;
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
      localStorage.removeItem('authToken');
      window.location.href = '/login';
      return;
    }
    
    return response;
  } catch (error) {
    console.error('API call failed:', error);
    throw error;
  }
}

// Export for use in other files
window.authUtils = {
  getAuthHeaders,
  checkAuth,
  authenticatedFetch
};
