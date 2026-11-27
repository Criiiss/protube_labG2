const dayMonthYear = new Intl.DateTimeFormat('es-ES', { day: '2-digit', month: '2-digit', year: 'numeric' });

/** Publication date of a video as dd/mm/yyyy falls back to the text the API sent. */
export function formatVideoDate(video: { createdAt?: string; published?: string }): string {
  const time = video.createdAt ? Date.parse(video.createdAt) : NaN;
  return Number.isNaN(time) ? video.published || '' : dayMonthYear.format(time);
}
