const express = require('express');
const path = require('path');
const expressLayouts = require('express-ejs-layouts');
const { createProxyMiddleware } = require('http-proxy-middleware');

const app = express();
const PORT = process.env.PORT || 3000;

// View engine + layout
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'ejs');
app.use(expressLayouts);
app.set('layout', 'layout');

// Static files - IMPORTANT: Must be BEFORE route handlers
app.use('/css', express.static(path.join(__dirname, 'public', 'css')));
app.use('/js', express.static(path.join(__dirname, 'public', 'js')));
app.use('/public', express.static(path.join(__dirname, 'public')));
app.use(express.static(path.join(__dirname, 'public'))); // Fallback

// Security headers for SPA
app.use((req, res, next) => {
  res.setHeader('X-Content-Type-Options', 'nosniff');
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('X-XSS-Protection', '1; mode=block');
  next();
});

// Log tất cả request
app.use((req, res, next) => {
 console.log(` ${req.method} ${req.originalUrl}`);
 next();
});

// Proxy API sang backend Spring Boot (http://localhost:8080)
app.use(
 '/api',
 createProxyMiddleware({
 target: 'http://localhost:8080',
 changeOrigin: true,
 logLevel: 'warn', // production log level
 onProxyReq: (proxyReq, req, res) => {
 console.log(` Proxying ${req.method} ${req.originalUrl} ${proxyReq.protocol}//${proxyReq.host}${proxyReq.path}`);
 },
 onProxyRes: (proxyRes, req, res) => {
 console.log(` Backend response: ${proxyRes.statusCode} for ${req.originalUrl}`);
 },
 onError: (err, req, res) => {
 console.error(' Proxy error:', err);
 res.status(500).json({ code: 500, message: 'Proxy failed', detail: err.message });
 }
 })
);

// Proxy uploads folder để serve static files từ backend
app.use(
 '/uploads',
 createProxyMiddleware({
 target: 'http://localhost:8080',
 changeOrigin: true,
 pathRewrite: function(path, req) {
   return path.replace(/^\/uploads/, '/api/files/uploads');
 },
 logLevel: 'warn',
 onProxyReq: (proxyReq, req, res) => {
 console.log(` Proxying file: ${req.originalUrl} ${proxyReq.path}`);
 }
 })
);

// Các routes render view
// Trang public (không cần đăng nhập)
app.get('/', (req, res) => res.render('home', { layout: false }));
app.get('/home', (req, res) => res.render('home', { layout: false }));
app.get('/login', (req, res) => res.render('login', { layout: false }));
app.get('/register', (req, res) => res.render('register', { layout: false }));
app.get('/reset-password', (req, res) => res.render('reset-password', { layout: false }));

// Route xác thực email
app.get('/verify-email', (req, res) => {
 const token = req.query.token;
 if (!token) {
 return res.render('error', { 
 layout: false, 
 message: 'Token xác thực không hợp lệ!',
 error: 'Missing verification token'
 });
 }
 
 res.render('verify-email', { 
 layout: false, 
 token: token,
 title: 'Xác thực Email'
 });
});

// Trang cần đăng nhập (sử dụng layout có sidebar)
app.get('/dashboard', (req, res) => res.render('dashboard', { title: 'Thống kê tài chính' }));
app.get('/profile', (req, res) => res.render('profile', { title: 'Hồ sơ cá nhân' }));
app.get('/wallets', (req, res) => res.render('wallets'));
app.get('/categories', (req, res) => res.render('categories'));
app.get('/transactions', (req, res) => res.render('transactions'));
app.get('/budgets', (req, res) => res.render('budgets'));
app.get('/goals', (req, res) => res.render('goals'));
app.get('/planning', (req, res) => res.render('planning', { title: 'Kế Hoạch Tài Chính AI' }));
app.get('/chat', (req, res) => res.render('chat'));

// Health check endpoint
app.get('/health', (req, res) => {
 res.json({ 
   status: 'ok', 
   timestamp: new Date().toISOString(),
   version: '1.0.0'
 });
});

// API status endpoint  
app.get('/api-status', (req, res) => {
 res.json({
   frontend: 'ok',
   backend: 'checking...',
   timestamp: new Date().toISOString()
 });
});

// EJS Routes
app.get('/', (req, res) => {
  res.render('home', { layout: false, title: 'Finance AI - Quản lý tài chính thông minh' });
});

app.get('/login', (req, res) => {
  res.render('login', { title: 'Đăng nhập' });
});

app.get('/register', (req, res) => {
  res.render('register', { title: 'Đăng ký' });
});

app.get('/verify-email', (req, res) => {
  res.render('verify-email', { title: 'Xác thực email' });
});

app.get('/reset-password', (req, res) => {
  res.render('reset-password', { title: 'Đặt lại mật khẩu' });
});

app.get('/dashboard', (req, res) => {
  res.render('dashboard', { title: 'Tổng quan' });
});

app.get('/transactions', (req, res) => {
  res.render('transactions', { title: 'Giao dịch' });
});

app.get('/budgets', (req, res) => {
  res.render('budgets', { title: 'Ngân sách' });
});

app.get('/goals', (req, res) => {
  res.render('goals', { title: 'Mục tiêu' });
});

app.get('/wallets', (req, res) => {
  res.render('wallets', { title: 'Ví tiền' });
});

app.get('/categories', (req, res) => {
  res.render('categories', { title: 'Danh mục' });
});

app.get('/profile', (req, res) => {
  res.render('profile', { title: 'Hồ sơ' });
});

app.get('/chat', (req, res) => {
  res.render('chat', { title: 'Chat AI' });
});

// Start server
app.listen(PORT, () => {
  console.log(`🚀 Frontend server running: http://localhost:${PORT}`);
  console.log(`� EJS Mode: Serving templates from views/`);
  console.log(`⚡ Health check: http://localhost:${PORT}/health`);
});
