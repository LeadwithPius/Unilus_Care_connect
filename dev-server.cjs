const http = require('http');
const fs = require('fs');
const path = require('path');

const root = path.resolve('Frontend_Material');
const backend = 'http://localhost:8080';

const mimeTypes = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.webmanifest': 'application/manifest+json'
};

const server = http.createServer((req, res) => {
  const url = new URL(req.url, 'http://localhost:5173');

  if (url.pathname === '/api' || url.pathname.startsWith('/api/')) {
    const options = {
      hostname: 'localhost',
      port: 8080,
      path: url.pathname + url.search,
      method: req.method,
      headers: { ...req.headers, host: 'localhost:8080' }
    };

    const proxy = http.request(options, backendRes => {
      res.writeHead(backendRes.statusCode, backendRes.headers);
      backendRes.pipe(res);
    });

    proxy.on('error', error => {
      console.error('Backend proxy error:', error.message);
      if (!res.headersSent) res.writeHead(502);
      res.end('Backend unavailable. Check that Spring Boot is running on port 8080.');
    });

    req.pipe(proxy);
    return;
  }

  let pathname;
  try {
    pathname = decodeURIComponent(url.pathname);
  } catch {
    res.writeHead(400);
    res.end('Invalid URL');
    return;
  }

  if (pathname === '/') pathname = '/index.html';

  const file = path.resolve(root, '.' + pathname);

  if (file !== root && !file.startsWith(root + path.sep)) {
    res.writeHead(403);
    res.end('Forbidden');
    return;
  }

  fs.readFile(file, (error, content) => {
    if (error) {
      res.writeHead(error.code === 'ENOENT' ? 404 : 500);
      res.end(error.code === 'ENOENT' ? 'Page not found' : 'Server error');
      return;
    }

    res.writeHead(200, {
      'Content-Type': mimeTypes[path.extname(file).toLowerCase()] || 'application/octet-stream'
    });
    res.end(content);
  });
});

server.listen(5173, '127.0.0.1', () => {
  console.log('CareConnect frontend: http://localhost:5173');
  console.log('API proxy: http://localhost:5173/api -> http://localhost:8080/api');
});
