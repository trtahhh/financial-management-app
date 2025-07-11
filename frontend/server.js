const express = require('express');
const path    = require('path');
const layouts = require('express-ejs-layouts');
const axios   = require('axios');
const app  = express();
const PORT = process.env.PORT || 3000;
const API  = process.env.API_URL || 'http://localhost:8080/api'; // backend

app.set('views', path.join(__dirname,'views'));
app.set('view engine', 'ejs');
app.use(express.static(path.join(__dirname,'public')));
app.use(layouts);            
app.set('layout','layout');

app.get('/', async (req,res)=>{
  try {
    const chart = await axios.get(`${API}/dashboard/summary`);
    res.render('dashboard',{title:'Dashboard', data: chart.data});
  } catch(e){
    res.render('dashboard',{title:'Dashboard', data:{}});
  }
});

app.get('/login', (req,res)=> res.render('login',{title:'Login'}));

app.listen(PORT,()=>console.log(`Frontend → http://localhost:${PORT}`));
