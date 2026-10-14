import { useState } from 'react';
import './App.css';
import { Header } from './components/home/Header';
import { Sidebar } from './components/home/Sidebar';
import { CategoryBar } from './components/home/CategoryBar';
import { VideoCard } from './components/home/VideoCard';
import { VideoCarousel } from './components/home/VideoCarousel';
import { categories, demoVideos } from './data/home';
import type { Video } from './data/home';

function App() {
  const [expanded, setExpanded] = useState(false);
  const [category, setCategory] = useState('all');
  const [query, setQuery] = useState('');
  const [notice, setNotice] = useState('');
  const filtered = demoVideos.filter(
    (video) =>
      (category === 'all' || video.categoryId === category) &&
      `${video.title} ${video.channel}`.toLocaleLowerCase('es').includes(query.toLocaleLowerCase('es'))
  );
  const isFiltered = category !== 'all' || !!query;
  const selectVideo = (video: Video) =>
    setNotice(`«${video.title}» es una muestra. La reproducción estará disponible con el contenido real.`);
  return (
    <div className={`app-shell ${expanded ? 'menu-expanded' : ''}`}>
      <Header onSearch={setQuery} onAction={setNotice} />
      <Sidebar expanded={expanded} onToggle={() => setExpanded(!expanded)} onAction={setNotice} />
      <div className="content-shell">
        <CategoryBar categories={categories} selected={category} onSelect={setCategory} />
        <main id="main-content">
          <div className="page-intro">
            <div>
              <p className="eyebrow">TU PRÓXIMO DESCUBRIMIENTO</p>
              <h1>
                Algo bueno te espera<span>.</span>
              </h1>
            </div>
            <span className="demo-label">Vista previa · contenido de muestra</span>
          </div>
          {isFiltered ? (
            <section aria-label="Resultados">
              <h2>{query ? `Resultados para «${query}»` : categories.find((item) => item.id === category)?.label}</h2>
              {filtered.length ? (
                <div className="video-grid">
                  {filtered.map((video) => (
                    <VideoCard key={video.id} video={video} onSelect={selectVideo} />
                  ))}
                </div>
              ) : (
                <p className="empty-state">No hay vídeos que coincidan. Prueba otra búsqueda o categoría.</p>
              )}
            </section>
          ) : (
            <>
              <section aria-label="Vídeos para ti">
                <div className="video-grid">
                  {demoVideos.slice(0, 3).map((video) => (
                    <VideoCard key={video.id} video={video} onSelect={selectVideo} />
                  ))}
                </div>
              </section>
              <section className="featured-section">
                <div className="section-heading">
                  <div>
                    <p className="eyebrow">EN EL PUNTO DE MIRA</p>
                    <h2>Descubre tu siguiente favorito</h2>
                  </div>
                  <span>Una selección para explorar</span>
                </div>
                <VideoCarousel videos={demoVideos.slice(3, 11)} onSelect={selectVideo} />
              </section>
              <section>
                <div className="section-heading">
                  <h2>Sigue explorando</h2>
                  <span>Siempre hay algo más que ver</span>
                </div>
                <div className="video-grid">
                  {demoVideos.slice(11, 17).map((video) => (
                    <VideoCard key={video.id} video={video} onSelect={selectVideo} />
                  ))}
                </div>
              </section>
            </>
          )}
        </main>
      </div>
      {notice && (
        <div className="notice" role="status">
          <span>{notice}</span>
          <button aria-label="Cerrar aviso" onClick={() => setNotice('')}>
            ×
          </button>
        </div>
      )}
    </div>
  );
}
export default App;
