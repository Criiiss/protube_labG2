import type { ProfileStats as ProfileStatsData } from './types';

interface ProfileStatsProps {
  stats: ProfileStatsData;
}
const formatCount = (value: number): string =>
  new Intl.NumberFormat('es-ES', { notation: 'compact', maximumFractionDigits: 1 }).format(value);

function ProfileStats({ stats }: ProfileStatsProps) {
  return (
    <section className="profile-stats" aria-label="Estadísticas del canal">
      <div className="profile-stat">
        <strong>{formatCount(stats.subscriptionsCount)}</strong>
        <span>suscriptores</span>
      </div>
      <div className="profile-stat">
        <strong>{formatCount(stats.videosCount)}</strong>
        <span>vídeos</span>
      </div>
    </section>
  );
}
export default ProfileStats;
