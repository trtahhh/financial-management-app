const express = require('express');
const path = require('path');
const expressLayouts = require('express-ejs-layouts');
const { createProxyMiddleware } = require('http-proxy-middleware');

const app = express();
const PORT = process.env.PORT || 3000;

// ⚙️ View engine + layout
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'ejs');
app.use(expressLayouts);
app.set('layout', 'layout');

// 📂 Static files
app.use(express.static(path.join(__dirname, 'public')));

// 📜 Log tất cả request
app.use((req, res, next) => {
  console.log(`➡ ${req.method} ${req.originalUrl}`);
  next();
});

// 🔁 Proxy API sang backend Spring Boot (http://localhost:8080)
app.use(
  '/api',
  createProxyMiddleware({
    target: 'http://localhost:8080',
    changeOrigin: true,
    logLevel: 'debug',          // thêm log chi tiết
    onProxyReq: (proxyReq, req, res) => {
      console.log(`➡️ Proxying ${req.method} ${req.originalUrl} → ${proxyReq.protocol}//${proxyReq.host}${proxyReq.path}`);
    },
    onProxyRes: (proxyRes, req, res) => {
      console.log(`✅ Backend response: ${proxyRes.statusCode} for ${req.originalUrl}`);
    },
    onError: (err, req, res) => {
      console.error('❌ Proxy error:', err);
      res.status(500).json({ code: 500, message: 'Proxy failed', detail: err.message });
    }
  })
);

// 📂 Proxy uploads folder để serve static files từ backend
app.use(
  '/uploads',
  createProxyMiddleware({
    target: 'http://localhost:8080',
    changeOrigin: true,
    pathRewrite: {
      '^/uploads': '/api/files/uploads'
    },
    logLevel: 'debug',
    onProxyReq: (proxyReq, req, res) => {
      console.log(`📷 Proxying file: ${req.originalUrl} → ${proxyReq.path}`);
    }
  })
);

// 🌐 Các routes render view
// Trang public (không cần đăng nhập)
app.get('/', (req, res) => res.render('home', { layout: false }));
app.get('/home', (req, res) => res.render('home', { layout: false }));
app.get('/login', (req, res) => res.render('login', { layout: false }));
app.get('/register', (req, res) => res.render('register', { layout: false }));

// Trang cần đăng nhập (sử dụng layout có sidebar)
app.get('/dashboard', (req, res) => res.render('dashboard', { title: 'Thống kê tài chính' }));
app.get('/profile', (req, res) => res.render('profile', { title: 'Hồ sơ cá nhân' }));
app.get('/wallets', (req, res) => res.render('wallets'));
app.get('/categories', (req, res) => res.render('categories'));
app.get('/transactions', (req, res) => res.render('transactions'));
app.get('/budgets', (req, res) => res.render('budgets'));
app.get('/goals', (req, res) => res.render('goals'));
app.get('/chat', (req, res) => res.render('chat'));

// 🚀 Start server
app.listen(PORT, () => console.log(`✅ Frontend running: http://localhost:${PORT}`));
