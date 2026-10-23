import { useState } from 'react';
import './App.css';
import './profile/profile.css';
import ProfilePage from './profile/ProfilePage';
import { useAllVideos } from './useAllVideos';

type AppView = 'videos' | 'profile';

function App() {
  const [view, setView] = useState<AppView>('profile');

  return (
    <div className="App">
      <nav className="app-navigation" aria-label="Navegación principal">
        <img src="/protube-logo-removebg-preview.png" className="app-navigation-logo" alt="ProTube" />

        <div className="app-navigation-actions">
          <button type="button" className={view === 'videos' ? 'active' : ''} onClick={() => setView('videos')}>
            Vídeos
          </button>

          <button type="button" className={view === 'profile' ? 'active' : ''} onClick={() => setView('profile')}>
            Mi canal
          </button>
        </div>
      </nav>

      {view === 'profile' ? <ProfilePage userId={1} /> : <ContentApp />}
    </div>
  );
}

function ContentApp() {
  const { loading, message, value } = useAllVideos();

  switch (loading) {
    case 'loading':
      return (
        <main className="legacy-content">
          <p>Loading...</p>
        </main>
      );

    case 'error':
      return (
        <main className="legacy-content">
          <h3>Error</h3>
          <p>{message}</p>
        </main>
      );

    case 'success':
      return (
        <main className="legacy-content">
          <strong>Videos availables:</strong>
          <ul>
            {value.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </main>
      );

    default:
      return (
        <main className="legacy-content">
          <p>Idle...</p>
        </main>
      );
  }
}

export default App;
