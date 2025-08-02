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

// 🌐 Các routes render view
app.get('/', (req, res) => res.redirect('/dashboard'));
app.get('/dashboard', (req, res) => res.render('dashboard', { title: 'Thống kê tài chính' }));
app.get('/wallets', (req, res) => res.render('wallets'));
app.get('/categories', (req, res) => res.render('categories'));
app.get('/transactions', (req, res) => res.render('transactions'));
app.get('/chat', (req, res) => res.render('chat'));
app.get('/goals', (req, res) => res.render('goals'));
app.get('/budgets', (req, res) => res.render('budgets'));
app.get('/login', (req, res) => res.render('login', { title: 'Đăng nhập' }));

// 🚀 Start server
app.listen(PORT, () => console.log(`✅ Frontend running: http://localhost:${PORT}`));
