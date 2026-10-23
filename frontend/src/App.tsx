import { useEffect, useState } from 'react';
import './App.css';
import './features.css';
import { Header } from './components/home/Header';
import { Sidebar } from './components/home/Sidebar';
import { CategoryBar } from './components/home/CategoryBar';
import { VideoCard } from './components/home/VideoCard';
import type { VideoAction } from './components/home/VideoCard';
import { VideoCarousel } from './components/home/VideoCarousel';
import { categories, demoVideos } from './data/home';
import type { Video } from './data/home';
import { useSession } from './auth/useSession';
import { AuthPage } from './auth/AuthPage';
import { ProfilePage } from './profile/ProfilePage';
import { useLibrary } from './home/useLibrary';
import { useCatalog } from './home/useCatalog';
import './profile/profile.css';
import ProfilePage from './profile/ProfilePage';
import { useAllVideos } from './useAllVideos';

type AppView = 'videos' | 'profile';
type ProfileMode = 'own' | 'public';

function App() {
  const [expanded, setExpanded] = useState(false);
  const [category, setCategory] = useState('all');
  const [query, setQuery] = useState('');
  const [notice, setNotice] = useState('');
  const [location, setLocation] = useState(window.location.hash || '#/');
  const [sampleMode, setSampleMode] = useState(
    () => new URLSearchParams(window.location.hash.split('?')[1]).get('sample') === '1'
  );
  const [limit, setLimit] = useState(12);
  const [sort, setSort] = useState('recommended');
  const session = useSession();
  const { library, toggle, visit } = useLibrary(session.user?.id);
  const catalog = useCatalog();
  const route = location.slice(1).split('?')[0] || '/';
  const params = new URLSearchParams(location.split('?')[1]);
  useEffect(() => {
    const update = () => {
      setLocation(window.location.hash || '#/');
      setLimit(12);
      window.scrollTo(0, 0);
    };
    window.addEventListener('hashchange', update);
    return () => window.removeEventListener('hashchange', update);
  }, []);
  const videos = sampleMode ? demoVideos : catalog.videos;
  let filtered = videos.filter(
    (video) =>
      !library.hidden.includes(video.id) &&
      (category === 'all' || video.categoryId === category) &&
      `${video.title} ${video.channel}`.toLocaleLowerCase('es').includes(query.toLocaleLowerCase('es'))
  );
  const collections: Record<string, string[]> = {
    '/library': library.saved,
    '/later': library.later,
    '/liked': library.liked,
    '/history': library.history,
  };
  const names: Record<string, string> = {
    '/library': 'Tu biblioteca',
    '/later': 'Ver más tarde',
    '/liked': 'Vídeos que te gustan',
    '/history': 'Tu historial',
    '/subscriptions': 'Tus suscripciones',
    '/channel': params.get('name') || 'Canal',
  };
  if (collections[route]) filtered = filtered.filter((video) => collections[route].includes(video.id));
  if (route === '/subscriptions') filtered = filtered.filter((video) => library.subscriptions.includes(video.channel));
  if (route === '/channel') filtered = filtered.filter((video) => video.channel === params.get('name'));
  if (route === '/channel' && params.get('video'))
    filtered = filtered.filter((video) => video.id === params.get('video'));
  if (route === '/history') filtered.sort((a, b) => library.history.indexOf(a.id) - library.history.indexOf(b.id));
  else if (sort === 'popular')
    filtered.sort((a, b) => (b.viewCount || parseFloat(b.views)) - (a.viewCount || parseFloat(a.views)));
  else if (sort === 'recent')
    filtered.sort((a, b) => (b.createdAt || b.id).localeCompare(a.createdAt || a.id, undefined, { numeric: true }));
  const selectVideo = (video: Video) => {
    if (session.user) visit(video.id);
    setNotice('La reproducción pertenece a la página de vídeo y aún no está implementada.');
  };
  async function action(type: VideoAction, video: Video) {
    if (type === 'share') {
      const link = `${window.location.origin}${window.location.pathname}#/channel?name=${encodeURIComponent(video.channel)}&video=${encodeURIComponent(video.id)}${sampleMode ? '&sample=1' : ''}`;
      try {
        await navigator.clipboard.writeText(link);
        setNotice('Enlace copiado.');
      } catch {
        setNotice(`Enlace para compartir: ${link}`);
      }
      return;
    }
    if (!session.user) {
      window.location.hash = '/login';
      return;
    }
    toggle(type, type === 'subscriptions' ? video.channel : video.id);
    setNotice('Preferencia actualizada en este navegador.');
  }
  const cards = (items: Video[]) => (
    <div className="video-grid">
      {items.map((video) => (
        <VideoCard
          key={video.id}
          video={video}
          onSelect={selectVideo}
          onAction={(type, item) => void action(type, item)}
          selectedActions={Object.entries(library)
            .filter(([key, ids]) => ids.includes(key === 'subscriptions' ? video.channel : video.id))
            .map(([key]) => key)}
        />
      ))}
  const [view, setView] = useState<AppView>('profile');
  const [profileMode, setProfileMode] = useState<ProfileMode>('own');

  return (
    <div className="App">
      <nav className="app-navigation" aria-label="Navegación principal">
        <img src="/protube-logo-removebg-preview.png" className="app-navigation-logo" alt="ProTube" />

        <div className="app-navigation-actions">
          <button type="button" className={view === 'videos' ? 'active' : ''} onClick={() => setView('videos')}>
            Vídeos
          </button>

          <button type="button" className={view === 'profile' ? 'active' : ''} onClick={() => setView('profile')}>
            Perfiles
          </button>
        </div>

        {view === 'profile' && (
          <div className="app-navigation-actions">
            <button
              type="button"
              className={profileMode === 'own' ? 'active' : ''}
              onClick={() => setProfileMode('own')}
            >
              Mi canal
            </button>

            <button
              type="button"
              className={profileMode === 'public' ? 'active' : ''}
              onClick={() => setProfileMode('public')}
            >
              Perfil ajeno
            </button>
          </div>
        )}
      </nav>

      {view === 'profile' &&
        (profileMode === 'own' ? <ProfilePage userId={1} /> : <ProfilePage username="otro_creador" />)}

      {view === 'videos' && <ContentApp />}
    </div>
  );
  const authMode = route.slice(1);
  const isAuthPage = ['login', 'register', 'forgot', 'reset', 'verify', 'resend', 'google-register'].includes(authMode);
  const privatePage = ['/profile', '/settings', '/library', '/later', '/liked', '/history', '/subscriptions'].includes(
    route
  );
  const showListing = route !== '/' || category !== 'all' || !!query;
  let content;
  if (isAuthPage) content = <AuthPage key={authMode} mode={authMode} onLogin={session.setUser} />;
  else if (privatePage && session.loading) content = <p role="status">Comprobando tu sesión…</p>;
  else if (privatePage && !session.user)
    content = (
      <div className="empty-state">
        <h1>Este espacio es para ti.</h1>
        <p>Inicia sesión para acceder a tu cuenta y a tus listas.</p>
        <a className="primary-button" href="#/login">
          Iniciar sesión
        </a>
      </div>
    );
  else if ((route === '/profile' || route === '/settings') && session.user)
    content = <ProfilePage key={session.user.id} userId={session.user.id} onUpdated={() => void session.refresh()} />;
  else if (route === '/terms')
    content = (
      <section className="account-page">
        <p className="eyebrow">PROTUBE · PROYECTO ACADÉMICO</p>
        <h1>Términos de uso</h1>
        <div className="auth-card">
          <p>
            Esta versión de protube es un proyecto académico. Al crear una cuenta te comprometes a utilizarla de forma
            responsable, respetar a otros usuarios y compartir únicamente contenido para el que tengas permiso.
          </p>
          <p>
            El nombre, usuario, email y fecha de nacimiento se utilizan para gestionar tu cuenta y comprobar la edad
            mínima. La contraseña se almacena cifrada mediante un hash. No compartas tu contraseña con otras personas.
          </p>
          <p>
            Las listas de la home se guardan en este navegador. Esta versión no garantiza la conservación de los datos y
            no debe utilizarse para almacenar información sensible.
          </p>
          <p>
            Antes de abrir el servicio al público, el responsable del proyecto debe completar y revisar sus condiciones
            y política de privacidad.
          </p>
          <a href="#/register">Volver al registro →</a>
        </div>
      </section>
    );
  else if (route === '/studio')
    content = (
      <div className="empty-state">
        <h1>Tu Studio.</h1>
        <p>La subida y gestión de vídeos se conectará con el apartado de publicación de contenido.</p>
        <a href="#/">Volver al inicio →</a>
      </div>
    );
  else
    content = (
      <>
        <div className="page-intro">
          <div>
            <p className="eyebrow">TU PRÓXIMO DESCUBRIMIENTO</p>
            <h1>
              {names[route] || 'Algo bueno te espera'}
              <span>.</span>
            </h1>
          </div>
          <button
            className="text-button"
            onClick={() => {
              setSampleMode(!sampleMode);
              setLimit(12);
            }}
          >
            {sampleMode ? 'Ver catálogo real' : 'Explorar contenido de muestra'}
          </button>
        </div>
        <div className="catalog-toolbar">
          <label>
            Ordenar por{' '}
            <select
              value={sort}
              onChange={(e) => {
                setSort(e.target.value);
                setLimit(12);
              }}
            >
              <option value="recommended">Recomendados</option>
              <option value="recent">Más recientes</option>
              <option value="popular">Más populares</option>
            </select>
          </label>
          <span className="demo-label">
            {sampleMode ? 'Contenido de muestra' : 'Catálogo'}
            {session.user ? ' · listas guardadas en este navegador' : ''}
          </span>
        </div>
        {catalog.error && !sampleMode && (
          <p className="form-message" role="status">
            {catalog.error}
          </p>
        )}
        {catalog.loading && !sampleMode ? (
          <div className="video-grid" aria-label="Cargando vídeos" role="status">
            {Array.from({ length: 6 }, (_, index) => (
              <div key={index} className="video-skeleton">
                <div />
                <span />
                <span />
              </div>
            ))}
          </div>
        ) : !filtered.length ? (
          <div className="empty-state">
            <h2>{query ? 'No hay coincidencias' : 'Todavía no hay vídeos aquí'}</h2>
            <p>
              {query
                ? 'Prueba otra búsqueda o categoría.'
                : 'Puedes explorar las muestras mientras llega el contenido real.'}
            </p>
            {!sampleMode && (
              <button className="primary-button" onClick={() => setSampleMode(true)}>
                Ver muestras
              </button>
            )}
          </div>
        ) : showListing || sort !== 'recommended' ? (
          <section aria-label="Resultados">
            {query && <h2>Resultados para «{query}»</h2>}
            {cards(filtered.slice(0, limit))}
            {filtered.length > limit && (
              <button className="load-more" onClick={() => setLimit(limit + 12)}>
                Cargar más vídeos
              </button>
            )}
          </section>
        ) : (
          <>
            <section aria-label="Vídeos para ti">{cards(filtered.slice(0, 3))}</section>
            {filtered.length > 3 && (
              <section className="featured-section">
                <div className="section-heading">
                  <div>
                    <p className="eyebrow">EN EL PUNTO DE MIRA</p>
                    <h2>Descubre tu siguiente favorito</h2>
                  </div>
                  <span>Una selección para explorar</span>
                </div>
                <VideoCarousel
                  videos={filtered.slice(3, 11)}
                  onSelect={selectVideo}
                  onAction={(type, video) => void action(type, video)}
                />
              </section>
            )}
            {filtered.length > 11 && (
              <section>
                <div className="section-heading">
                  <h2>Sigue explorando</h2>
                  <span>Siempre hay algo más que ver</span>
                </div>
                {cards(filtered.slice(11, 17 + Math.max(0, limit - 12)))}
                {filtered.length > 17 + Math.max(0, limit - 12) && (
                  <button className="load-more" onClick={() => setLimit(limit + 12)}>
                    Cargar más vídeos
                  </button>
                )}
              </section>
            )}
          </>
        )}
      </>
    );
  return (
    <div className={`app-shell ${expanded ? 'menu-expanded' : ''}`}>
      <a
        className="skip-link"
        href="#main-content"
        onClick={(event) => {
          event.preventDefault();
          document.getElementById('main-content')?.focus();
        }}
      >
        Saltar al contenido
      </a>
      <Header
        onSearch={(value) => {
          setQuery(value);
          setLimit(12);
          if (route !== '/') window.location.hash = '/';
        }}
        onAction={setNotice}
        user={session.user}
        sessionLoading={session.loading}
        onLogout={() => void session.logout().catch((error) => setNotice(error.message))}
      />
      <Sidebar
        expanded={expanded}
        onToggle={() => setExpanded(!expanded)}
        subscriptions={session.user ? library.subscriptions : []}
        route={route}
      />
      <div className="content-shell">
        {!isAuthPage && !['/profile', '/settings', '/terms'].includes(route) && (
          <CategoryBar
            categories={categories}
            selected={category}
            onSelect={(value) => {
              setCategory(value);
              setLimit(12);
            }}
          />
        )}
        <main id="main-content" tabIndex={-1}>
          {content}
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
