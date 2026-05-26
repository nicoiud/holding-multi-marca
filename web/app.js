const API = '/api/news';
let currentCat = 'all';

async function fetchStats() {
  try {
    const res = await fetch('/api/stats');
    const data = await res.json();
    document.getElementById('stats').innerHTML = `
      <div class="stat-chip">Total: <strong>${data.total}</strong></div>
      <div class="stat-chip">🤖 IA: <strong>${data.IA}</strong></div>
      <div class="stat-chip">💻 Tech: <strong>${data.Tech}</strong></div>
    `;
  } catch {}
}

async function fetchNews(cat) {
  const url = cat === 'all' ? `${API}?limit=100` : `${API}?categoria=${cat}&limit=100`;
  const res = await fetch(url);
  if (!res.ok) throw new Error('Error al cargar noticias');
  const data = await res.json();
  return data.items;
}

function scoreClass(score) {
  if (score >= 8) return 'score-high';
  if (score >= 6) return 'score-mid';
  return 'score-low';
}

function nivelClass(nivel) {
  if (nivel === 'alto')  return 'nivel-alto';
  if (nivel === 'medio') return 'nivel-medio';
  return 'nivel-bajo';
}

function nivelLabel(nivel, importante) {
  if (!importante) return '— Sin análisis de importancia';
  const emoji = nivel === 'alto' ? '⚡' : nivel === 'medio' ? '📌' : '💤';
  const label = nivel === 'alto' ? 'IMPORTANTE' : nivel === 'medio' ? 'RELEVANTE' : 'MENOR';
  return `${emoji} <span class="${nivelClass(nivel)}">${label}</span>`;
}

function timeAgo(isoDate) {
  const diff = Date.now() - new Date(isoDate).getTime();
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'ahora mismo';
  if (mins < 60) return `hace ${mins} min`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `hace ${hrs} h`;
  return `hace ${Math.floor(hrs / 24)} d`;
}

function renderCard(item) {
  const hasAnalisis = item.analisis && item.analisis.por_que;
  const analisisHTML = hasAnalisis ? `
    <button class="analisis-toggle" onclick="toggleAnalisis(this)">
      🔍 Ver análisis editorial ▾
    </button>
    <div class="analisis-panel">
      <div>${nivelLabel(item.analisis.nivel, item.analisis.importante)}</div>
      <div class="analisis-label">Por qué importa</div>
      <div>${item.analisis.por_que}</div>
      ${item.analisis.impacto ? `
        <div class="analisis-label">Impacto</div>
        <div>${item.analisis.impacto}</div>
      ` : ''}
    </div>
  ` : '';

  return `
    <article class="card">
      <div class="card-header">
        <span class="cat-badge ${item.categoria}">${item.categoria === 'IA' ? '🤖 IA' : '💻 Tech'}</span>
        <h2 class="card-title">
          <a href="${item.url}" target="_blank" rel="noopener">${item.titulo}</a>
        </h2>
      </div>
      <div class="card-meta">
        <span>${item.fuente}</span>
        <span>${timeAgo(item.fecha)}</span>
        <span><span class="score-dot ${scoreClass(item.score)}"></span>Score ${item.score}/10</span>
      </div>
      ${item.resumen ? `<p class="resumen">${item.resumen}</p>` : ''}
      ${analisisHTML}
    </article>
  `;
}

function toggleAnalisis(btn) {
  const panel = btn.nextElementSibling;
  panel.classList.toggle('open');
  btn.textContent = panel.classList.contains('open')
    ? '🔍 Ocultar análisis editorial ▴'
    : '🔍 Ver análisis editorial ▾';
}

async function render(cat) {
  const feed = document.getElementById('feed');
  const empty = document.getElementById('empty');
  const loading = document.getElementById('loading');

  feed.innerHTML = '';
  empty.classList.add('hidden');
  loading.classList.remove('hidden');

  try {
    const news = await fetchNews(cat);
    loading.classList.add('hidden');
    if (news.length === 0) {
      empty.classList.remove('hidden');
      return;
    }
    feed.innerHTML = news.map(renderCard).join('');
  } catch (err) {
    loading.textContent = 'Error al cargar noticias. Reintentando en 30s...';
    setTimeout(() => render(cat), 30000);
  }
}

document.querySelectorAll('.tab').forEach(tab => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    tab.classList.add('active');
    currentCat = tab.dataset.cat;
    render(currentCat);
  });
});

fetchStats();
render(currentCat);

// recargar cada 5 minutos
setInterval(() => {
  fetchStats();
  render(currentCat);
}, 5 * 60 * 1000);
