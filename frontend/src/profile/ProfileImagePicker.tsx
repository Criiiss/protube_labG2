import type { ChangeEvent } from 'react';

interface ProfileImagePickerProps {
  label: string;
  value: string | null;
  aspectRatio: 'avatar' | 'banner';
  disabled?: boolean;
  onChange: (image: string | null) => void;
  onRemove?: () => void;
}

const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_IMAGE_SIZE = 5 * 1024 * 1024;

function ProfileImagePicker({
  label,
  value,
  aspectRatio,
  disabled = false,
  onChange,
  onRemove,
}: ProfileImagePickerProps) {
  const inputId = `profile-image-${aspectRatio}`;

  const handleFileChange = (event: ChangeEvent<HTMLInputElement>): void => {
    const file = event.target.files?.[0];

    if (!file) {
      return;
    }

    if (!ACCEPTED_IMAGE_TYPES.includes(file.type)) {
      window.alert('Selecciona una imagen JPG, PNG o WebP.');
      event.target.value = '';
      return;
    }

    if (file.size > MAX_IMAGE_SIZE) {
      window.alert('La imagen no puede superar los 5 MB.');
      event.target.value = '';
      return;
    }

    const reader = new FileReader();

    reader.onload = () => {
      if (typeof reader.result === 'string') {
        onChange(reader.result);
      }
    };

    reader.onerror = () => {
      window.alert('No se ha podido leer la imagen seleccionada.');
    };

    reader.readAsDataURL(file);
    event.target.value = '';
  };

  return (
    <div className={`profile-image-picker profile-image-picker--${aspectRatio}`}>
      <span className="profile-image-picker-label">{label}</span>

      <div className="profile-image-preview">
        {value ? (
          <img src={value} alt={`Previsualización de ${label.toLowerCase()}`} />
        ) : (
          <span className="profile-image-empty">Sin imagen</span>
        )}
      </div>

      <div className="profile-image-actions">
        <label className="profile-image-upload-button" htmlFor={inputId} aria-disabled={disabled}>
          {value ? 'Cambiar imagen' : 'Subir imagen'}
        </label>

        <input
          id={inputId}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          hidden
          disabled={disabled}
          onChange={handleFileChange}
        />

        {value && onRemove && (
          <button type="button" className="profile-image-remove-button" disabled={disabled} onClick={() => onRemove()}>
            Eliminar
          </button>
        )}
      </div>

      <small>JPG, PNG o WebP. Máximo 5 MB.</small>
    </div>
  );
}

export default ProfileImagePicker;
