const target = process.env['API_BASE_URL'] ?? 'http://localhost:8080';

module.exports = {
  '/api':      { target, secure: false, changeOrigin: true },
  '/actuator': { target, secure: false, changeOrigin: true },
};
