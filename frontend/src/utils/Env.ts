export const getEnv = () => {
  const { VITE_API_DOMAIN, VITE_MEDIA_DOMAIN, VITE_PROFILE_DEMO, BASE_URL, ...otherViteConfig } = import.meta.env;

  return {
    API_BASE_URL: `${VITE_API_DOMAIN}/api`,
    MEDIA_BASE_URL: `${VITE_MEDIA_DOMAIN}/media`,
    PROFILE_DEMO: VITE_PROFILE_DEMO === 'true',
    BASE_URL,
    __vite__: otherViteConfig,
  };
};
