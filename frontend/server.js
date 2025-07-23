const express = require('express');
const path = require('path');
const layouts = require('express-ejs-layouts');
const axios = require('axios');
const { createProxyMiddleware } = require('http-proxy-middleware');
const session = require('express-session');
const flash = require('connect-flash');
const multer = require('multer');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;
const API = process.env.API_URL || 'http://localhost:8080/api';

app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'ejs');
app.use(layouts);
app.set('layout', 'layout');
app.use(express.static(path.join(__dirname, 'public')));
app.use(express.urlencoded({ extended: true }));
app.use(express.json());
app.use(session({ secret: 'secret', resave: false, saveUninitialized: true }));
app.use(flash());

// Cấu hình multer cho upload file
const storage = multer.diskStorage({
  destination: function (req, file, cb) {
    const uploadDir = path.join(__dirname, 'public', 'uploads');
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    cb(null, uploadDir);
  },
  filename: function (req, file, cb) {
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
    cb(null, 'avatar-' + uniqueSuffix + path.extname(file.originalname));
  }
});

const upload = multer({ 
  storage: storage,
  limits: {
    fileSize: 5 * 1024 * 1024 // 5MB limit
  },
  fileFilter: function (req, file, cb) {
    if (file.mimetype.startsWith('image/')) {
      cb(null, true);
    } else {
      cb(new Error('Chỉ cho phép upload file ảnh!'), false);
    }
  }
});

// Middleware log lỗi
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).render('error', { title: 'Error', error: err.message });
});

// Truyền session và path vào view cho mọi route
app.use((req, res, next) => {
  res.locals.session = req.session;
  res.locals.path = req.path;
  next();
});

// Middleware để xử lý CORS và headers
app.use((req, res, next) => {
  res.header('Access-Control-Allow-Origin', '*');
  res.header('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.header('Access-Control-Allow-Headers', 'Origin, X-Requested-With, Content-Type, Accept, Authorization');
  
  if (req.method === 'OPTIONS') {
    res.sendStatus(200);
  } else {
    next();
  }
});

const requireAuth = (req, res, next) => {
  console.log('requireAuth - Session token:', req.session?.token ? 'exists' : 'null');
  if (!req.session.token) {
    console.log('requireAuth - No token, redirecting to login');
    return res.redirect('/login');
  }
  console.log('requireAuth - Token found, proceeding');
  next();
};

app.get('/login', (req, res) =>
  res.render('login', { title: 'Login', error: req.flash('error') })
);

app.post('/login', async (req, res) => {
  try {
    console.log('Login attempt for:', req.body.email);
    const resp = await axios.post(`${API}/auth/login`, req.body);
    console.log('Login successful, token received');
    req.session.token = resp.data.token;
    console.log('Session token saved:', req.session.token ? 'yes' : 'no');
    res.redirect('/dashboard');
  } catch (e) {
    console.error('Login failed:', e.response?.data || e.message);
    let msg = 'Đăng nhập thất bại';
    if (e.response && e.response.data && e.response.data.error) msg = e.response.data.error;
    req.flash('error', msg);
    res.redirect('/login');
  }
});

app.get('/register', (req, res) =>
  res.render('register', { title: 'Register', error: req.flash('error') })
);

app.post('/register', async (req, res) => {
  try {
    console.log('Register data:', req.body); // Debug log
    await axios.post(`${API}/auth/register`, req.body);
    res.redirect('/login');
  } catch (e) {
    console.error('Register error:', e.response?.data || e.message); // Debug log
    let msg = 'Đăng ký thất bại';
    if (e.response && e.response.data && e.response.data.error) msg = e.response.data.error;
    req.flash('error', msg);
    res.redirect('/register');
  }
});

app.post('/logout', (req, res) => {
  req.session.destroy(() => {
    res.redirect('/login');
  });
});

// Dashboard endpoint
app.get('/dashboard', requireAuth, async (req, res) => {
  console.log('Dashboard request - Session token:', req.session?.token ? 'exists' : 'null');
  try {
    const resp = await axios.get(`${API}/dashboard`, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    console.log('Dashboard API call successful');
    res.render('dashboard', { title: 'Dashboard', data: resp.data, error: null });
  } catch (e) {
    console.error('Dashboard error:', e.response?.data || e.message);
    res.render('dashboard', { title: 'Dashboard', data: {}, error: 'Không tải được dữ liệu' });
  }
});

app.get('/', (req, res) => {
  res.render('home', { title: 'Trang chủ' });
});


app.get('/budgets', requireAuth, async (req, res) => {
  try {
    const resp = await axios.get(`${API}/budgets`, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    res.render('budgets', { title: 'Budgets', budgets: resp.data, error: null });
  } catch (e) {
    console.error('Budgets error:', e.response?.data || e.message);
    res.render('budgets', { title: 'Budgets', budgets: [], error: 'Không tải được ngân sách' });
  }
});

app.get('/transactions', requireAuth, (req, res) => {
  res.render('transactions', { 
    title: 'Quản lý giao dịch',
    user: req.session.user,
    error: req.query.error || null,
    success: req.query.success || null
  });
});

// Transaction API endpoints
app.get('/api/transactions', requireAuth, async (req, res) => {
  try {
    console.log('Transactions API - Making direct call to backend');
    console.log('Transactions API - Query params:', req.query);
    const resp = await axios.get(`${API}/transactions`, {
      headers: { Authorization: `Bearer ${req.session.token}` },
      params: req.query
    });
    console.log('Transactions API - Call successful, data count:', resp.data?.length || 0);
    res.json(resp.data);
  } catch (e) {
    console.error('Transaction fetch error:', e.response?.data || e.message);
    res.status(400).json({ error: e.response?.data?.error || 'Failed to fetch transactions' });
  }
});

app.get('/api/transactions/:id', requireAuth, async (req, res) => {
  try {
    const resp = await axios.get(`${API}/transactions/${req.params.id}`, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    res.json(resp.data);
  } catch (e) {
    console.error('Transaction fetch error:', e.response?.data || e.message);
    res.status(400).json({ error: e.response?.data?.error || 'Failed to fetch transaction' });
  }
});

app.post('/api/transactions', requireAuth, async (req, res) => {
  try {
    const resp = await axios.post(`${API}/transactions`, req.body, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    res.json(resp.data);
  } catch (e) {
    console.error('Transaction create error:', e.response?.data || e.message);
    res.status(400).json({ error: e.response?.data?.error || 'Failed to create transaction' });
  }
});

app.put('/api/transactions/:id', requireAuth, async (req, res) => {
  try {
    const resp = await axios.put(`${API}/transactions/${req.params.id}`, req.body, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    res.json(resp.data);
  } catch (e) {
    console.error('Transaction update error:', e.response?.data || e.message);
    res.status(400).json({ error: e.response?.data?.error || 'Failed to update transaction' });
  }
});

app.delete('/api/transactions/:id', requireAuth, async (req, res) => {
  try {
    const resp = await axios.delete(`${API}/transactions/${req.params.id}`, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    res.json(resp.data);
  } catch (e) {
    console.error('Transaction delete error:', e.response?.data || e.message);
    res.status(400).json({ error: e.response?.data?.error || 'Failed to delete transaction' });
  }
});

// Budget API endpoints
app.get('/api/budgets', requireAuth, async (req, res) => {
  try {
    console.log('Budgets API - Making direct call to backend');
    const resp = await axios.get(`${API}/budgets`, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    console.log('Budgets API - Call successful');
    res.json(resp.data);
  } catch (e) {
    console.error('Budgets API error:', e.response?.data || e.message);
    res.status(400).json({ error: e.response?.data?.error || 'Failed to fetch budgets' });
  }
});

app.post('/api/budgets', requireAuth, async (req, res) => {
  try {
    const resp = await axios.post(`${API}/budgets`, req.body, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    res.json(resp.data);
  } catch (e) {
    res.status(400).json({ error: e.response?.data?.error || 'Failed to create budget' });
  }
});

app.put('/api/budgets/:id', requireAuth, async (req, res) => {
  try {
    const resp = await axios.put(`${API}/budgets/${req.params.id}`, req.body, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    res.json(resp.data);
  } catch (e) {
    res.status(400).json({ error: e.response?.data?.error || 'Failed to update budget' });
  }
});

app.delete('/api/budgets/:id', requireAuth, async (req, res) => {
  try {
    const resp = await axios.delete(`${API}/budgets/${req.params.id}`, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    res.json(resp.data);
  } catch (e) {
    res.status(400).json({ error: e.response?.data?.error || 'Failed to delete budget' });
  }
});

// Wallet API endpoints
app.get('/api/wallets', requireAuth, async (req, res) => {
  try {
    console.log('Wallets API - Making direct call to backend');
    const resp = await axios.get(`${API}/wallets`, {
      headers: { Authorization: `Bearer ${req.session.token}` },
      params: req.query
    });
    console.log('Wallets API - Call successful');
    res.json(resp.data);
  } catch (e) {
    console.error('Wallets API error:', e.response?.data || e.message);
    res.status(400).json({ error: e.response?.data?.error || 'Failed to fetch wallets' });
  }
});

app.post('/api/wallets', requireAuth, async (req, res) => {
  try {
    const resp = await axios.post(`${API}/wallets`, req.body, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    res.json(resp.data);
  } catch (e) {
    res.status(400).json({ error: e.response?.data?.error || 'Failed to create wallet' });
  }
});

app.put('/api/wallets/:id', requireAuth, async (req, res) => {
  try {
    const resp = await axios.put(`${API}/wallets/${req.params.id}`, req.body, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    res.json(resp.data);
  } catch (e) {
    res.status(400).json({ error: e.response?.data?.error || 'Failed to update wallet' });
  }
});

app.delete('/api/wallets/:id', requireAuth, async (req, res) => {
  try {
    const resp = await axios.delete(`${API}/wallets/${req.params.id}`, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    res.json(resp.data);
  } catch (e) {
    res.status(400).json({ error: e.response?.data?.error || 'Failed to delete wallet' });
  }
});

// Category API endpoints
app.get('/api/categories', requireAuth, async (req, res) => {
  try {
    console.log('Categories API - Making direct call to backend');
    const resp = await axios.get(`${API}/categories`, {
      headers: { Authorization: `Bearer ${req.session.token}` },
      params: req.query
    });
    console.log('Categories API - Call successful');
    res.json(resp.data);
  } catch (e) {
    console.error('Categories API error:', e.response?.data || e.message);
    res.status(400).json({ error: e.response?.data?.error || 'Failed to fetch categories' });
  }
});

app.post('/api/categories', requireAuth, async (req, res) => {
  try {
    const resp = await axios.post(`${API}/categories`, req.body, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    res.json(resp.data);
  } catch (e) {
    res.status(400).json({ error: e.response?.data?.error || 'Failed to create category' });
  }
});

app.put('/api/categories/:id', requireAuth, async (req, res) => {
  try {
    const resp = await axios.put(`${API}/categories/${req.params.id}`, req.body, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    res.json(resp.data);
  } catch (e) {
    res.status(400).json({ error: e.response?.data?.error || 'Failed to update category' });
  }
});

app.delete('/api/categories/:id', requireAuth, async (req, res) => {
  try {
    const resp = await axios.delete(`${API}/categories/${req.params.id}`, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    res.json(resp.data);
  } catch (e) {
    res.status(400).json({ error: e.response?.data?.error || 'Failed to delete category' });
  }
});

// Dashboard API endpoint
app.get('/api/dashboard', requireAuth, async (req, res) => {
  try {
    console.log('Dashboard API - Making direct call to backend');
    const resp = await axios.get(`${API}/dashboard`, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    console.log('Dashboard API - Call successful');
    res.json(resp.data);
  } catch (e) {
    console.error('Dashboard API error:', e.response?.data || e.message);
    res.status(400).json({ error: e.response?.data?.error || 'Failed to load dashboard data' });
  }
});

app.get('/api/dashboard/financial-summary', requireAuth, async (req, res) => {
  try {
    const resp = await axios.get(`${API}/dashboard/financial-summary`, {
      headers: { Authorization: `Bearer ${req.session.token}` },
      params: req.query
    });
    res.json(resp.data);
  } catch (e) {
    res.status(400).json({ error: e.response?.data?.error || 'Failed to fetch financial summary' });
  }
});

app.get('/api/dashboard/monthly-statistics', requireAuth, async (req, res) => {
  try {
    const resp = await axios.get(`${API}/dashboard/monthly-statistics`, {
      headers: { Authorization: `Bearer ${req.session.token}` },
      params: req.query
    });
    res.json(resp.data);
  } catch (e) {
    res.status(400).json({ error: e.response?.data?.error || 'Failed to fetch monthly statistics' });
  }
});

app.get('/api/dashboard/yearly-statistics', requireAuth, async (req, res) => {
  try {
    const resp = await axios.get(`${API}/dashboard/yearly-statistics`, {
      headers: { Authorization: `Bearer ${req.session.token}` },
      params: req.query
    });
    res.json(resp.data);
  } catch (e) {
    res.status(400).json({ error: e.response?.data?.error || 'Failed to fetch yearly statistics' });
  }
});

app.get('/profile', requireAuth, async (req, res) => {
  try {
    const resp = await axios.get(`${API}/profile`, { 
      headers: { Authorization: `Bearer ${req.session.token}` } 
    });
    
    // Format birthday để hiển thị trong input date
    let userData = resp.data;
    if (userData.birthday) {
      const birthday = new Date(userData.birthday);
      userData.birthday = birthday.toISOString().split('T')[0]; // Format: YYYY-MM-DD
    }
    
    // Đảm bảo gender có giá trị mặc định
    if (!userData.gender) {
      userData.gender = '';
    }
    
    // Đảm bảo imageUrl có giá trị hợp lệ
    if (!userData.imageUrl || userData.imageUrl.trim() === '') {
      userData.imageUrl = null;
    }
    
    res.render('profile', { 
      title: 'Hồ sơ', 
      user: userData, 
      error: req.flash('error'), 
      success: req.flash('success') 
    });
  } catch (e) {
    console.error('Profile error:', e.response?.data || e.message);
    req.flash('error', 'Không tải được hồ sơ: ' + (e.response?.data?.error || e.message));
    res.redirect('/dashboard');
  }
});

app.put('/profile', requireAuth, async (req, res) => {
  try {
    const resp = await axios.put(`${API}/profile`, req.body, { 
      headers: { Authorization: `Bearer ${req.session.token}` } 
    });
    res.json({ message: resp.data.message || 'Cập nhật thành công!' });
  } catch (e) {
    console.error('Profile update error:', e.response?.data || e.message);
    res.status(400).json({ 
      error: e.response?.data?.error || 'Cập nhật thất bại!' 
    });
  }
});

// Upload avatar endpoint
app.post('/upload-avatar', requireAuth, upload.single('avatar'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ success: false, error: 'Không có file được upload' });
    }

    // Tạo URL cho file đã upload
    const imageUrl = `/uploads/${req.file.filename}`;
    
    // Cập nhật avatar URL vào profile
    const resp = await axios.put(`${API}/profile`, { imageUrl }, { 
      headers: { Authorization: `Bearer ${req.session.token}` } 
    });
    
    res.json({ 
      success: true, 
      imageUrl: imageUrl,
      message: 'Upload avatar thành công!' 
    });
  } catch (e) {
    console.error('Avatar upload error:', e.response?.data || e.message);
    res.status(400).json({ 
      success: false,
      error: e.response?.data?.error || 'Upload avatar thất bại!' 
    });
  }
});

// Delete avatar endpoint
app.post('/delete-avatar', requireAuth, async (req, res) => {
  try {
    // Cập nhật avatar URL thành null
    const resp = await axios.put(`${API}/profile`, { imageUrl: null }, { 
      headers: { Authorization: `Bearer ${req.session.token}` } 
    });
    
    res.json({ 
      success: true,
      message: 'Xóa avatar thành công!' 
    });
  } catch (e) {
    console.error('Avatar delete error:', e.response?.data || e.message);
    res.status(400).json({ 
      success: false,
      error: e.response?.data?.error || 'Xóa avatar thất bại!' 
    });
  }
});

// Test proxy endpoint
app.get('/api/test/proxy', requireAuth, async (req, res) => {
  try {
    console.log('Proxy test - Making direct API call');
    const resp = await axios.get(`${API}/test/current-user`, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    console.log('Proxy test - API call successful');
    res.json({ success: true, data: resp.data });
  } catch (e) {
    console.error('Proxy test - API call failed:', e.response?.data || e.message);
    res.status(400).json({ success: false, error: e.response?.data?.error || e.message });
  }
});

// Test session endpoint
app.get('/api/test/session', requireAuth, (req, res) => {
  console.log('Session test - Token exists:', !!req.session.token);
  console.log('Session test - Token length:', req.session.token ? req.session.token.length : 0);
  console.log('Session test - Token preview:', req.session.token ? req.session.token.substring(0, 20) + '...' : 'null');
  
  res.json({ 
    hasToken: !!req.session.token,
    tokenLength: req.session.token ? req.session.token.length : 0,
    tokenPreview: req.session.token ? req.session.token.substring(0, 20) + '...' : 'null'
  });
});

// Test data API endpoint
app.post('/api/test/sample-data', requireAuth, async (req, res) => {
  try {
    console.log('Test data API - Making direct call to backend');
    const resp = await axios.post(`${API}/test/sample-data`, {}, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    console.log('Test data API - Call successful');
    res.json(resp.data);
  } catch (e) {
    console.error('Test data error:', e.response?.data || e.message);
    res.status(400).json({ error: e.response?.data?.error || 'Failed to create test data' });
  }
});

// Get current user endpoint
app.get('/api/test/current-user', requireAuth, async (req, res) => {
  try {
    console.log('Current user API - Making direct call to backend');
    const resp = await axios.get(`${API}/test/current-user`, {
      headers: { Authorization: `Bearer ${req.session.token}` }
    });
    console.log('Current user API - Call successful');
    res.json(resp.data);
  } catch (e) {
    console.error('Current user error:', e.response?.data || e.message);
    res.status(400).json({ error: e.response?.data?.error || 'Failed to get current user' });
  }
});

app.listen(PORT, () => console.log(`Frontend → http://localhost:${PORT}`));