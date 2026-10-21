export type PrivacyVisibility = 'PUBLIC' | 'PRIVATE';
export interface PublicProfile {
    userId: number;
    username: string;
    displayName: string | null;
    biography: string | null;
    avatarUrl: string | null;
    bannerUrl: string | null;
    createdAt: string;
}

export interface MyProfile extends PublicProfile {
    subscriptionsVisibility: PrivacyVisibility;
    likedVideosVisibility: PrivacyVisibility;
}

export interface ProfileFormData{
    displayName: string;
    username: string;
    biography: string;
    avatarUrl: string | null;
    bannerUrl: string | null;
}

export interface UpdateProfileRequest{
    displayName: string;
    biography: string | null;
    avatarUrl: string | null;
    bannerUrl: string | null;
}

export interface ChangeUsernameRequest{
    username: string;
}
//todo: videocount subcount y subscriptions hay que implementarlo mas tarde, queda prep
export interface ProfileStats{
    videosCount: number;
    subscribersCount: number;
    subscriptionsCount: number;
}