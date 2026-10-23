import { useCallback, useEffect, useState } from 'react';
import {
  changeUsername,
  getMyProfile,
  getPublicProfile,
  removeAvatar,
  removeBanner,
  updateProfile,
} from './profileApi';

import type { ChangeUsernameRequest, MyProfile, PublicProfile, UpdateProfileRequest } from './types';

type Profile = MyProfile | PublicProfile;
interface UseProfileOptions {
  userId?: number;
  username?: string;
}

interface UseProfileResult {
  profile: Profile | null;
  isOwner: boolean;
  loading: boolean;
  saving: boolean;
  error: string | null;
  refresh: () => Promise<void>;
  saveProfile: (data: UpdateProfileRequest) => Promise<void>;
  saveUsername: (data: ChangeUsernameRequest) => Promise<void>;
  deleteAvatar: () => Promise<void>;
  deleteBanner: () => Promise<void>;
}

const getErrorMessage = (error: unknown): string => {
  if (error instanceof Error && error.message) return error.message;
  return 'No se ha podido completar la operación';
};

export const useProfile = ({ userId, username }: UseProfileOptions): UseProfileResult => {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isOwner = userId !== undefined;
  const refresh = useCallback(async () => {
    if (userId === undefined && !username) {
      setError('No se ha indicado ningún perfil');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const loadedProfile =
        userId !== undefined ? await getMyProfile(userId) : await getPublicProfile(username as string);
      setProfile(loadedProfile);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [userId, username]);
  useEffect(() => {
    void refresh();
  }, [refresh]);

  const saveProfile = async (data: UpdateProfileRequest) => {
    if (userId === undefined) throw new Error('Solo el propietario puede actualizar este perfil');
    setSaving(true);
    setError(null);

    try {
      const updatedProfile = await updateProfile(userId, data);
      setProfile(updatedProfile);
    } catch (requestError) {
      const message = getErrorMessage(requestError);
      setError(message);
      throw new Error(message);
    } finally {
      setSaving(false);
    }
  };

  const saveUsername = async (data: ChangeUsernameRequest) => {
    if (userId === undefined) throw new Error('Solo el propietario puede cambiar su nombre de usuario');
    setSaving(true);
    setError(null);

    try {
      const updatedProfile = await changeUsername(userId, data);
      setProfile(updatedProfile);
    } catch (requestError) {
      const message = getErrorMessage(requestError);
      setError(message);
      throw new Error(message);
    } finally {
      setSaving(false);
    }
  };

  const deleteAvatar = async () => {
    if (userId === undefined) throw new Error('Solo el propietario puede eliminar su avatar');
    setSaving(true);
    setError(null);

    try {
      const updatedProfile = await removeAvatar(userId);
      setProfile(updatedProfile);
    } catch (requestError) {
      const message = getErrorMessage(requestError);
      setError(message);
      throw new Error(message);
    } finally {
      setSaving(false);
    }
  };

  const deleteBanner = async () => {
    if (userId === undefined) throw new Error('Solo el propietario puede eliminar su banner');
    setSaving(true);
    setError(null);

    try {
      const updatedProfile = await removeBanner(userId);
      setProfile(updatedProfile);
    } catch (requestError) {
      const message = getErrorMessage(requestError);
      setError(message);
      throw new Error(message);
    } finally {
      setSaving(false);
    }
  };

  return {
    profile,
    isOwner,
    loading,
    saving,
    error,
    refresh,
    saveProfile,
    saveUsername,
    deleteAvatar,
    deleteBanner,
  };
};
