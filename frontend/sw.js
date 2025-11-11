// Service Worker for Financial Management App
// Provides offline capability and caching

const CACHE_NAME = 'financial-app-v1.0.0';
const STATIC_CACHE = 'financial-static-v1';
const DATA_CACHE = 'financial-data-v1';

// Files to cache for offline use
const STATIC_FILES = [
    '/',
    '/index.html',
    '/css/styles.css',
    '/css/gamification.css',
    '/js/app.js',
    '/js/constants/categories.js',
    '/js/constants/config.js',
    '/js/constants/colors.js',
    '/js/utils/currencyHelpers.js',
    '/js/utils/dateHelpers.js',
    '/js/utils/validators.js',
    '/js/utils/calculationHelpers.js',
    '/js/utils/achievementSystem.js',
    '/js/utils/levelSystem.js',
    '/js/utils/streakTracker.js',
    '/js/services/apiService.js',
    '/js/services/transactionService.js',
    '/js/services/storageService.js',
    '/js/components/HomeScreen.js',
    '/js/components/TransactionList.js',
    '/js/components/QuickAddTransaction.js',
    '/js/components/BudgetOverview.js',
    '/js/components/CategoryCard.js',
    '/js/components/ProgressBar.js'
];

// API endpoints to cache
const API_ENDPOINTS = [
    '/api/transactions',
    '/api/budgets',
    '/api/goals',
    '/api/categories',
    '/api/user/profile'
];

// Install event - cache static files
self.addEventListener('install', (event) => {
    console.log('🔧 Service Worker installing...');
    
    event.waitUntil(
        Promise.all([
            // Cache static files
            caches.open(STATIC_CACHE).then((cache) => {
                console.log('📦 Caching static files...');
                return cache.addAll(STATIC_FILES.map(url => new Request(url, {
                    cache: 'reload'
                })));
            }),
            
            // Initialize data cache
            caches.open(DATA_CACHE).then((cache) => {
                console.log('📊 Initializing data cache...');
                return cache;
            })
        ]).then(() => {
            console.log('✅ Service Worker installed successfully');
            // Skip waiting to activate immediately
            return self.skipWaiting();
        }).catch((error) => {
            console.error('❌ Service Worker installation failed:', error);
        })
    );
});

// Activate event - clean up old caches
self.addEventListener('activate', (event) => {
    console.log('🚀 Service Worker activating...');
    
    event.waitUntil(
        Promise.all([
            // Clean up old caches
            caches.keys().then((cacheNames) => {
                return Promise.all(
                    cacheNames.map((cacheName) => {
                        if (cacheName !== STATIC_CACHE && 
                            cacheName !== DATA_CACHE && 
                            cacheName !== CACHE_NAME) {
                            console.log('🗑️ Deleting old cache:', cacheName);
                            return caches.delete(cacheName);
                        }
                    })
                );
            }),
            
            // Claim all clients
            self.clients.claim()
        ]).then(() => {
            console.log('✅ Service Worker activated successfully');
        }).catch((error) => {
            console.error('❌ Service Worker activation failed:', error);
        })
    );
});

// Fetch event - handle requests with caching strategy
self.addEventListener('fetch', (event) => {
    const { request } = event;
    const url = new URL(request.url);
    
    // Handle different types of requests
    if (request.method === 'GET') {
        if (isStaticFile(url.pathname)) {
            // Static files: Cache First strategy
            event.respondWith(handleStaticFile(request));
        } else if (isAPIRequest(url.pathname)) {
            // API requests: Network First with cache fallback
            event.respondWith(handleAPIRequest(request));
        } else {
            // Other requests: Network First
            event.respondWith(handleNetworkFirst(request));
        }
    } else {
        // Non-GET requests: Network only with offline queue
        event.respondWith(handleNetworkOnly(request));
    }
});

// Handle static file requests (CSS, JS, images)
async function handleStaticFile(request) {
    try {
        const cache = await caches.open(STATIC_CACHE);
        
        // Try cache first
        const cachedResponse = await cache.match(request);
        if (cachedResponse) {
            // Check if we should update in background
            fetchAndUpdateCache(request, cache);
            return cachedResponse;
        }
        
        // Not in cache, fetch from network
        const networkResponse = await fetch(request);
        if (networkResponse.ok) {
            cache.put(request, networkResponse.clone());
        }
        return networkResponse;
        
    } catch (error) {
        console.error('Static file request failed:', error);
        
        // Try cache as fallback
        const cache = await caches.open(STATIC_CACHE);
        const cachedResponse = await cache.match(request);
        if (cachedResponse) {
            return cachedResponse;
        }
        
        // Return offline page for HTML requests
        if (request.headers.get('accept').includes('text/html')) {
            return getOfflinePage();
        }
        
        throw error;
    }
}

// Handle API requests with network first strategy
async function handleAPIRequest(request) {
    try {
        const cache = await caches.open(DATA_CACHE);
        
        // Try network first
        const networkResponse = await fetch(request);
        
        if (networkResponse.ok) {
            // Cache successful responses
            cache.put(request, networkResponse.clone());
            return networkResponse;
        } else {
            throw new Error(`Network response not ok: ${networkResponse.status}`);
        }
        
    } catch (error) {
        console.log('Network request failed, trying cache:', error);
        
        // Fallback to cache
        const cache = await caches.open(DATA_CACHE);
        const cachedResponse = await cache.match(request);
        
        if (cachedResponse) {
            // Add offline indicator header
            const response = cachedResponse.clone();
            response.headers.set('X-From-Cache', 'true');
            return response;
        }
        
        // Return empty response for failed API requests
        return new Response(JSON.stringify({
            error: 'Offline - no cached data available',
            offline: true
        }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' }
        });
    }
}

// Handle other requests with network first
async function handleNetworkFirst(request) {
    try {
        return await fetch(request);
    } catch (error) {
        // For HTML requests, return cached version or offline page
        if (request.headers.get('accept').includes('text/html')) {
            const cache = await caches.open(STATIC_CACHE);
            const cachedResponse = await cache.match('/index.html');
            return cachedResponse || getOfflinePage();
        }
        
        throw error;
    }
}

// Handle network-only requests (POST, PUT, DELETE)
async function handleNetworkOnly(request) {
    try {
        return await fetch(request);
    } catch (error) {
        // For offline POST/PUT/DELETE, store in offline queue
        if (isAPIRequest(new URL(request.url).pathname)) {
            await storeOfflineRequest(request);
            
            return new Response(JSON.stringify({
                success: true,
                offline: true,
                message: 'Request queued for when online'
            }), {
                status: 200,
                headers: { 'Content-Type': 'application/json' }
            });
        }
        
        throw error;
    }
}

// Background fetch and cache update
async function fetchAndUpdateCache(request, cache) {
    try {
        const response = await fetch(request);
        if (response.ok) {
            cache.put(request, response.clone());
        }
    } catch (error) {
        // Ignore background update errors
        console.log('Background cache update failed:', error);
    }
}

// Store offline requests for later sync
async function storeOfflineRequest(request) {
    const offlineQueue = await getOfflineQueue();
    
    const requestData = {
        url: request.url,
        method: request.method,
        headers: Object.fromEntries(request.headers.entries()),
        body: request.method !== 'GET' ? await request.text() : null,
        timestamp: Date.now()
    };
    
    offlineQueue.push(requestData);
    await setOfflineQueue(offlineQueue);
    
    console.log('📤 Stored offline request:', requestData);
}

// Get offline queue from storage
async function getOfflineQueue() {
    try {
        const cache = await caches.open(DATA_CACHE);
        const response = await cache.match('/offline-queue');
        
        if (response) {
            return await response.json();
        }
    } catch (error) {
        console.log('Failed to get offline queue:', error);
    }
    
    return [];
}

// Store offline queue
async function setOfflineQueue(queue) {
    try {
        const cache = await caches.open(DATA_CACHE);
        const response = new Response(JSON.stringify(queue), {
            headers: { 'Content-Type': 'application/json' }
        });
        
        await cache.put('/offline-queue', response);
    } catch (error) {
        console.error('Failed to store offline queue:', error);
    }
}

// Process offline queue when back online
async function processOfflineQueue() {
    const queue = await getOfflineQueue();
    const processed = [];
    
    for (const requestData of queue) {
        try {
            const request = new Request(requestData.url, {
                method: requestData.method,
                headers: requestData.headers,
                body: requestData.body
            });
            
            const response = await fetch(request);
            
            if (response.ok) {
                processed.push(requestData);
                console.log('✅ Processed offline request:', requestData.url);
            } else {
                console.warn('⚠️ Failed to process offline request:', response.status);
            }
        } catch (error) {
            console.error('❌ Error processing offline request:', error);
        }
    }
    
    // Remove processed requests from queue
    const remainingQueue = queue.filter(item => !processed.includes(item));
    await setOfflineQueue(remainingQueue);
    
    // Notify clients about sync completion
    self.clients.matchAll().then(clients => {
        clients.forEach(client => {
            client.postMessage({
                type: 'OFFLINE_SYNC_COMPLETE',
                processed: processed.length,
                remaining: remainingQueue.length
            });
        });
    });
}

// Helper functions
function isStaticFile(pathname) {
    return STATIC_FILES.some(file => file === pathname) ||
           pathname.includes('.css') ||
           pathname.includes('.js') ||
           pathname.includes('.png') ||
           pathname.includes('.jpg') ||
           pathname.includes('.ico') ||
           pathname.includes('.svg');
}

function isAPIRequest(pathname) {
    return pathname.startsWith('/api/') ||
           API_ENDPOINTS.some(endpoint => pathname.startsWith(endpoint));
}

function getOfflinePage() {
    return new Response(`
        <!DOCTYPE html>
        <html lang="vi">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Offline - Quản Lý Tài Chính</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    background: #f8fafc;
                    color: #1a202c;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    min-height: 100vh;
                    margin: 0;
                    text-align: center;
                    padding: 1rem;
                }
                .offline-container {
                    max-width: 400px;
                }
                .offline-icon {
                    font-size: 4rem;
                    margin-bottom: 1rem;
                }
                .offline-title {
                    font-size: 1.5rem;
                    font-weight: 700;
                    margin-bottom: 0.5rem;
                }
                .offline-message {
                    color: #718096;
                    margin-bottom: 2rem;
                }
                .retry-btn {
                    background: #667eea;
                    color: white;
                    border: none;
                    padding: 0.75rem 1.5rem;
                    border-radius: 8px;
                    cursor: pointer;
                    font-weight: 600;
                }
                .retry-btn:hover {
                    background: #5a67d8;
                }
            </style>
        </head>
        <body>
            <div class="offline-container">
                <div class="offline-icon">📱</div>
                <h1 class="offline-title">Chế độ Offline</h1>
                <p class="offline-message">
                    Bạn đang không có kết nối internet. Ứng dụng sẽ hoạt động với dữ liệu đã lưu trữ.
                </p>
                <button class="retry-btn" onclick="location.reload()">
                    Thử lại
                </button>
            </div>
            
            <script>
                // Auto-retry when back online
                window.addEventListener('online', () => {
                    location.reload();
                });
            </script>
        </body>
        </html>
    `, {
        headers: { 'Content-Type': 'text/html' }
    });
}

// Listen for messages from main thread
self.addEventListener('message', (event) => {
    const { type, data } = event.data;
    
    switch (type) {
        case 'SYNC_OFFLINE_QUEUE':
            processOfflineQueue();
            break;
            
        case 'CLEAR_CACHE':
            clearAllCaches();
            break;
            
        case 'UPDATE_CACHE':
            updateCache(data);
            break;
            
        default:
            console.log('Unknown message type:', type);
    }
});

// Clear all caches
async function clearAllCaches() {
    const cacheNames = await caches.keys();
    await Promise.all(cacheNames.map(name => caches.delete(name)));
    console.log('🗑️ All caches cleared');
}

// Update specific cache
async function updateCache(data) {
    if (data.type === 'static') {
        const cache = await caches.open(STATIC_CACHE);
        await cache.addAll(STATIC_FILES);
    } else if (data.type === 'data') {
        // Clear data cache to force fresh requests
        await caches.delete(DATA_CACHE);
    }
}

// Handle online/offline events
self.addEventListener('online', () => {
    console.log('📶 Back online - processing offline queue');
    processOfflineQueue();
});

self.addEventListener('offline', () => {
    console.log('📱 Gone offline - requests will be queued');
});

console.log('🔧 Service Worker loaded successfully');