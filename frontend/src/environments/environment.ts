export const environment = {
  production: true,
  apiBaseUrl: '/api',
  keycloak: {
    issuer: 'https://auth.timchat.ru/realms/timchat',
    clientId: 'timchat-frontend',
    redirectUri: window.location.origin,
  },
};
