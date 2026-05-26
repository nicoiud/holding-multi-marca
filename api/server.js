const express = require('express');
const fs = require('fs');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;
const API_SECRET = process.env.API_SECRET || '';
const NEWS_FILE = path.join(__dirname, 'news.json');
const MAX_NEWS = 500;

app.use(express.json());
app.use(express.static(path.join(__dirname, '../web')));

function loadNews() {
  if (!fs.existsSync(NEWS_FILE)) return [];
  try {
    return JSON.parse(fs.readFileSync(NEWS_FILE, 'utf8'));
  } catch {
    return [];
  }
}

function saveNews(news) {
  fs.writeFileSync(NEWS_FILE, JSON.stringify(news, null, 2));
}

function authMiddleware(req, res, next) {
  if (!API_SECRET) return next();
  const token = req.headers['x-api-secret'];
  if (token !== API_SECRET) return res.status(401).json({ error: 'Unauthorized' });
  next();
}

// n8n publica una noticia nueva
app.post('/api/news', authMiddleware, (req, res) => {
  const { titulo, url, fuente, categoria, score, resumen, analisis, fecha } = req.body;

  if (!titulo || !url || !categoria) {
    return res.status(400).json({ error: 'titulo, url y categoria son requeridos' });
  }

  const news = loadNews();

  if (news.some(n => n.url === url)) {
    return res.status(409).json({ error: 'Noticia ya existe' });
  }

  const item = {
    id: Date.now().toString(),
    titulo,
    url,
    fuente: fuente || 'Desconocida',
    categoria,
    score: score || 0,
    resumen: resumen || '',
    analisis: analisis || null,
    fecha: fecha || new Date().toISOString(),
  };

  news.unshift(item);

  // mantener solo las últimas MAX_NEWS noticias
  if (news.length > MAX_NEWS) news.splice(MAX_NEWS);

  saveNews(news);
  res.status(201).json(item);
});

// Frontend obtiene noticias
app.get('/api/news', (req, res) => {
  const { categoria, limit = 50, offset = 0 } = req.query;
  let news = loadNews();

  if (categoria) {
    news = news.filter(n => n.categoria === categoria);
  }

  const total = news.length;
  const page = news.slice(Number(offset), Number(offset) + Number(limit));

  res.json({ total, offset: Number(offset), limit: Number(limit), items: page });
});

app.get('/api/stats', (req, res) => {
  const news = loadNews();
  const ia = news.filter(n => n.categoria === 'IA').length;
  const tech = news.filter(n => n.categoria === 'Tech').length;
  res.json({ total: news.length, IA: ia, Tech: tech });
});

app.listen(PORT, () => {
  console.log(`API corriendo en http://localhost:${PORT}`);
});
