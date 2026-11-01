const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function setupProxy(app) {
  app.use(
    ['/api', '/uploads'],
    createProxyMiddleware({
      target: process.env.API_URL || 'http://localhost:8080',
      changeOrigin: true,
    }),
  );
};
