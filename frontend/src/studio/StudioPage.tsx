import { useEffect, useRef, useState } from 'react';
import { ApiError } from '../auth/api';
import { categories } from '../data/home';
import {
  sendBinary,
  statusText,
  studioCancel,
  studioEdit,
  studioFinish,
  studioItem,
  studioLimits,
  studioList,
  studioStart,
  studioUrl,
} from './api';
import type { Item, Limits, Metadata, Visibility } from './api';
import { fileChecksum, readDuration, transfer } from './upload';
import './studio.css';

const defaults: Metadata = {
  title: '',
  description: '',
  tags: [],
  category: 'all',
  language: 'es',
  ageRestricted: false,
  commentsAllowed: true,
  visibility: 'PRIVATE',
  publishAt: null,
  draft: false,
  thumbnail: 'auto-1',
};
const localDate = (value: string | null) => {
  if (!value) return '';
  const date = new Date(value);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};
const displayError = (error: unknown) =>
  error instanceof Error ? error.message : 'No se pudo completar la operación.';
const busyStatus = (item: Item) => item.status === 'QUEUED' || item.status === 'PROCESSING';

export function StudioPage() {
  const [items, setItems] = useState<Item[]>([]);
  const [limits, setLimits] = useState<Limits>();
  const [metadata, setMetadata] = useState<Metadata>(defaults);
  const [tagText, setTagText] = useState('');
  const [schedule, setSchedule] = useState('');
  const [file, setFile] = useState<File>();
  const [image, setImage] = useState<File>();
  const [selected, setSelected] = useState<Item>();
  const [busy, setBusy] = useState(false);
  const [dragging, setDragging] = useState(false);
  const [message, setMessage] = useState('');
  const [notice, setNotice] = useState('');
  const previous = useRef(new Map<string, Item['status']>());
  const transferController = useRef<AbortController | null>(null);
  const selectionRequest = useRef(0);
  const uploadId = useRef<string | undefined>(undefined);
  const cancellation = useRef(false);

  function handleError(error: unknown) {
    if (error instanceof ApiError && error.status === 401) window.location.hash = '/login?next=studio';
    else setMessage(displayError(error));
  }

  useEffect(() => {
    let mounted = true;
    const refresh = async () => {
      try {
        const data = await studioList();
        if (!mounted) return;
        const finished = data.filter((item) => {
          const old = previous.current.get(item.id);
          return (old === 'PROCESSING' || old === 'QUEUED') && (item.status === 'READY' || item.status === 'ERROR');
        });
        if (finished.length)
          setNotice(finished.map((item) => `${item.metadata.title}: ${statusText[item.status]}`).join(' · '));
        previous.current = new Map(data.map((item) => [item.id, item.status]));
        setItems(data);
      } catch (error) {
        if (mounted) handleError(error);
      }
    };
    void refresh();
    void studioLimits()
      .then((data) => {
        if (mounted) setLimits(data);
      })
      .catch((error) => {
        if (mounted) handleError(error);
      });
    const timer = window.setInterval(() => void refresh(), 2500);
    return () => {
      mounted = false;
      clearInterval(timer);
      transferController.current?.abort();
    };
  }, []);

  const current = items.find((item) => item.id === selected?.id) || selected;
  function update(item: Item) {
    setSelected(item);
    setItems((old) => [item, ...old.filter((other) => other.id !== item.id)]);
  }
  function select(item?: Item) {
    selectionRequest.current++;
    setSelected(item);
    setMetadata(item?.metadata || defaults);
    setTagText(item?.metadata.tags.join(', ') || '');
    setSchedule(localDate(item?.metadata.publishAt));
    setFile(undefined);
    setImage(undefined);
    setMessage('');
    uploadId.current = item?.id;
  }
  function change<K extends keyof Metadata>(key: K, value: Metadata[K]) {
    setMetadata((old) => ({ ...old, [key]: value }));
  }
  async function choose(candidate?: File) {
    if (!candidate || !limits || busy) return;
    const request = ++selectionRequest.current;
    setMessage('');
    setFile(undefined);
    if (!/\.(mp4|mov|webm)$/i.test(candidate.name)) {
      setMessage('Selecciona un vídeo MP4, MOV o WebM.');
      return;
    }
    if (!candidate.size || candidate.size > limits.maxBytes) {
      setMessage('El archivo está vacío o supera el tamaño permitido.');
      return;
    }
    const duration = await readDuration(candidate);
    if (request !== selectionRequest.current) return;
    if (duration !== undefined && duration > limits.maxSeconds) {
      setMessage('El vídeo supera la duración máxima.');
      return;
    }
    setFile(candidate);
    setMetadata((old) => ({ ...old, title: old.title || candidate.name.replace(/\.[^.]+$/, '').slice(0, 100) }));
  }
  function formData(draft: boolean): Metadata {
    const title = metadata.title.trim();
    if (!title) throw new Error('Introduce un título.');
    if (schedule && !draft && metadata.visibility !== 'PUBLIC')
      throw new Error('Selecciona Público para programar la publicación.');
    const publishAt = schedule && !draft ? new Date(schedule).toISOString() : null;
    const tags = tagText
      .split(',')
      .map((tag) => tag.trim())
      .filter(Boolean);
    return { ...metadata, title, tags, draft, publishAt };
  }

  async function save(draft: boolean) {
    if (busy || !limits) return;
    setMessage('');
    setNotice('');
    setBusy(true);
    cancellation.current = false;
    const controller = new AbortController();
    transferController.current = controller;
    try {
      let data = formData(draft);
      let item = current;
      if (file && (!item || item.status === 'UPLOADING')) {
        const checksum = await fileChecksum(file);
        controller.signal.throwIfAborted();
        if (item && checksum !== item.checksum)
          throw new Error('Selecciona el mismo archivo original para reanudar la subida.');
        item = item
          ? await studioItem(item.id)
          : await studioStart(file.name, file.size, checksum, { ...data, thumbnail: 'auto-1' });
        uploadId.current = item.id;
        update(item);
      }
      if (!item) throw new Error('Selecciona un archivo de vídeo.');
      controller.signal.throwIfAborted();
      if (image) {
        await sendBinary(`/videos/${item.id}/thumbnail`, image, controller.signal);
        data = { ...data, thumbnail: 'custom' };
      }
      item = await studioEdit(item.id, data);
      update(item);
      setMetadata(item.metadata);
      setImage(undefined);
      if (item.status === 'UPLOADING' && (file || item.receivedBytes === item.totalBytes)) {
        if (file) await transfer(file, item, limits.chunkBytes, controller.signal, update);
        controller.signal.throwIfAborted();
        item = await studioFinish(item.id);
        update(item);
        setFile(undefined);
        setNotice('Subida completa. Te avisaremos cuando termine el procesamiento.');
      } else setNotice(draft ? 'Borrador guardado.' : 'Cambios guardados.');
    } catch (error) {
      if (!controller.signal.aborted) handleError(error);
    } finally {
      if (cancellation.current && uploadId.current) {
        try {
          await studioCancel(uploadId.current);
          const id = uploadId.current;
          setItems((old) => old.filter((item) => item.id !== id));
          select();
          setNotice('Subida cancelada.');
        } catch (error) {
          handleError(error);
        }
      }
      if (cancellation.current && !uploadId.current) {
        select();
        setNotice('Subida cancelada.');
      }
      transferController.current = null;
      setBusy(false);
    }
  }

  async function cancel() {
    if (transferController.current) {
      cancellation.current = true;
      transferController.current.abort();
      return;
    }
    if (!current) {
      select();
      return;
    }
    try {
      await studioCancel(current.id);
      setItems((old) => old.filter((item) => item.id !== current.id));
      select();
      setNotice('Subida cancelada.');
    } catch (error) {
      handleError(error);
    }
  }

  const mayUpload = !current || current.status === 'UPLOADING';
  const progress = current ? Math.floor((current.receivedBytes / current.totalBytes) * 100) : 0;
  return (
    <section className="studio-page">
      <div className="page-intro">
        <div>
          <p className="eyebrow">TU CONTENIDO</p>
          <h1>Studio</h1>
        </div>
        <button className="primary-button" disabled={busy} onClick={() => select()}>
          Nuevo vídeo
        </button>
      </div>
      {message && (
        <p className="form-message" role="alert">
          {message}
        </p>
      )}
      {notice && (
        <p className="form-message" role="status">
          {notice}
        </p>
      )}
      <div className="studio-layout">
        <form
          className="auth-card studio-form"
          onSubmit={(event) => {
            event.preventDefault();
            void save(metadata.draft);
          }}
        >
          <h2>{current ? 'Editar vídeo' : 'Subir vídeo'}</h2>
          {mayUpload && (
            <label
              className={`studio-drop ${dragging ? 'dragging' : ''}`}
              onDragOver={(event) => {
                event.preventDefault();
                if (!busy) setDragging(true);
              }}
              onDragLeave={() => setDragging(false)}
              onDrop={(event) => {
                event.preventDefault();
                setDragging(false);
                void choose(event.dataTransfer.files[0]);
              }}
            >
              <strong>
                {file?.name ||
                  (current ? 'Selecciona el archivo original para reanudar' : 'Selecciona o arrastra un vídeo')}
              </strong>
              <span>
                MP4, MOV o WebM{' '}
                {limits &&
                  `· ${Math.floor(limits.maxBytes / 1024 ** 2)} MB · ${Math.floor(limits.maxSeconds / 60)} min máx.`}
              </span>
              <input
                type="file"
                accept=".mp4,.mov,.webm"
                disabled={busy || !limits}
                onChange={(event) => {
                  void choose(event.target.files?.[0]);
                  event.target.value = '';
                }}
              />
            </label>
          )}
          {current && (
            <div className="studio-progress">
              <span>
                {statusText[current.status]} {current.status === 'UPLOADING' && `· ${progress}%`}
              </span>
              {current.status === 'UPLOADING' && (
                <progress value={progress} max={100} aria-label="Progreso de subida" />
              )}
              {current.failure && <p role="alert">{current.failure}</p>}
            </div>
          )}
          <fieldset disabled={busy}>
            <label className="form-field">
              Título
              <input
                required
                maxLength={100}
                value={metadata.title}
                onChange={(e) => change('title', e.target.value)}
              />
            </label>
            <label className="form-field">
              Descripción
              <textarea
                rows={4}
                maxLength={5000}
                value={metadata.description}
                onChange={(e) => change('description', e.target.value)}
              />
            </label>
            <label className="form-field">
              Etiquetas (separadas por comas)
              <input value={tagText} onChange={(e) => setTagText(e.target.value)} />
            </label>
            <div className="studio-fields">
              <label className="form-field">
                Categoría
                <select value={metadata.category} onChange={(e) => change('category', e.target.value)}>
                  {categories.map(({ id, label }) => (
                    <option key={id} value={id}>
                      {id === 'all' ? 'Sin categoría' : label}
                    </option>
                  ))}
                </select>
              </label>
              <label className="form-field">
                Idioma
                <select value={metadata.language} onChange={(e) => change('language', e.target.value)}>
                  <option value="es">Español</option>
                  <option value="en">Inglés</option>
                  <option value="ca">Catalán</option>
                  <option value="fr">Francés</option>
                  <option value="und">Sin especificar</option>
                </select>
              </label>
            </div>
            <label className="check-field">
              <input
                type="checkbox"
                checked={metadata.ageRestricted}
                onChange={(e) => change('ageRestricted', e.target.checked)}
              />{' '}
              Solo mayores de 18 años
            </label>
            <label className="check-field">
              <input
                type="checkbox"
                checked={metadata.commentsAllowed}
                onChange={(e) => change('commentsAllowed', e.target.checked)}
              />{' '}
              Permitir comentarios
            </label>
            <h3>Miniatura</h3>
            <p className="form-hint">Las tres opciones automáticas aparecerán al terminar el procesamiento.</p>
            {!!current?.thumbnails.length && (
              <div className="studio-thumbnails">
                {current.thumbnails.map((name) => (
                  <label key={name}>
                    <img
                      src={studioUrl(`/videos/${current.id}/thumbnail/${name}.jpg`)}
                      alt={name === 'custom' ? 'Personalizada' : `Opción ${name.slice(-1)}`}
                    />
                    <input
                      type="radio"
                      name="thumbnail"
                      checked={metadata.thumbnail === name && !image}
                      onChange={() => {
                        setImage(undefined);
                        change('thumbnail', name);
                      }}
                    />
                  </label>
                ))}
              </div>
            )}
            <label className="form-field">
              Miniatura personalizada · JPG/PNG · 16:9 · máximo 5 MB
              <input
                type="file"
                accept="image/jpeg,image/png"
                onChange={(e) => {
                  const next = e.target.files?.[0];
                  setMessage('');
                  setImage(undefined);
                  if (next && limits && next.size > limits.imageBytes)
                    setMessage('La miniatura debe pesar hasta 5 MB.');
                  else setImage(next);
                }}
              />
            </label>
            <label className="form-field">
              Visibilidad
              <select
                value={metadata.visibility}
                onChange={(e) => {
                  change('visibility', e.target.value as Visibility);
                  if (e.target.value !== 'PUBLIC') setSchedule('');
                }}
              >
                <option value="PRIVATE">Privado</option>
                <option value="PUBLIC">Público</option>
                <option value="UNLISTED">No listado</option>
              </select>
            </label>
            {metadata.visibility === 'PUBLIC' && (
              <label className="form-field">
                Programar publicación (hora local)
                <input type="datetime-local" value={schedule} onChange={(e) => setSchedule(e.target.value)} />
              </label>
            )}
          </fieldset>
          <div className="studio-actions">
            <button className="primary-button" disabled={busy || !limits}>
              {busy
                ? 'Un momento…'
                : file && mayUpload
                  ? current
                    ? 'Reanudar subida'
                    : schedule
                      ? 'Subir y programar'
                      : 'Subir vídeo'
                  : 'Guardar cambios'}
            </button>
            <button
              type="button"
              className="text-button"
              disabled={busy || !limits}
              onClick={() => void save(!metadata.draft)}
            >
              {metadata.draft
                ? schedule
                  ? 'Programar borrador'
                  : 'Aplicar visibilidad al borrador'
                : 'Guardar borrador'}
            </button>
            {(mayUpload || current?.status === 'QUEUED' || current?.status === 'ERROR') &&
              (busy || current || file) && (
                <button type="button" className="text-button" onClick={() => void cancel()}>
                  Cancelar subida
                </button>
              )}
          </div>
        </form>
        <aside className="auth-card studio-list">
          <h2>Mis vídeos</h2>
          {!items.length && <p className="form-hint">Tus subidas aparecerán aquí.</p>}
          {items.map((item) => (
            <article className="studio-list-item" key={item.id}>
              <button className="text-button" disabled={busy} onClick={() => select(item)}>
                {item.metadata.title}
              </button>
              <span>
                {statusText[item.status]}
                {item.metadata.draft && ' · Borrador'}
                {item.metadata.publishAt && new Date(item.metadata.publishAt) > new Date() && ' · Programado'}
              </span>
              {busyStatus(item) && <small>Se procesa en segundo plano.</small>}
              {item.status === 'ERROR' && <small>{item.failure}</small>}
              {item.status === 'READY' && <a href={`#/watch?id=${item.id}`}>Ver vídeo</a>}
            </article>
          ))}
        </aside>
      </div>
    </section>
  );
}
