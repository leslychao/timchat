export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api',
  keycloak: {
    issuer: 'http://localhost:8180/realms/timchat',
    clientId: 'timchat-frontend',
    redirectUri: 'http://localhost:4200',
  },
};
