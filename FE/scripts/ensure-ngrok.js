// @ts-check
const http = require('http');
const { spawn } = require('child_process');

const NGROK_DOMAIN = 'https://obstetrically-nontangible-rubi.ngrok-free.dev';
const TARGET_PORT = 8080;

function checkNgrokRunning() {
  return new Promise((resolve) => {
    const req = http.get('http://127.0.0.1:4040/api/tunnels', (res) => {
      let data = '';
      res.on('data', (chunk) => (data += chunk));
      res.on('end', () => {
        try {
          const json = JSON.parse(data);
          const hasTunnel = json.tunnels && json.tunnels.length > 0;
          resolve(hasTunnel);
        } catch {
          resolve(false);
        }
      });
    });

    req.on('error', () => resolve(false));
    req.setTimeout(1500, () => {
      req.destroy();
      resolve(false);
    });
  });
}

async function ensureNgrok() {
  const isRunning = await checkNgrokRunning();
  if (isRunning) {
    console.log(`\x1b[32m [Ngrok] Tunnel is already active at: ${NGROK_DOMAIN} -> http://localhost:${TARGET_PORT}\x1b[0m`);
    return;
  }

  console.log(`\x1b[33m [Ngrok] Starting tunnel: ${NGROK_DOMAIN} -> http://localhost:${TARGET_PORT}...\x1b[0m`);

  try {
    const proc = spawn('ngrok', ['http', String(TARGET_PORT), '--url', NGROK_DOMAIN], {
      detached: true,
      stdio: 'ignore',
      shell: true,
    });

    proc.unref();

    await new Promise((r) => setTimeout(r, 2000));
    const verified = await checkNgrokRunning();
    if (verified) {
      console.log(`\x1b[32m [Ngrok] Tunnel connected successfully!\x1b[0m`);
      console.log(`\x1b[36m Webhook SePay: ${NGROK_DOMAIN}/api/v1/public/payments/webhook/sepay\x1b[0m`);
    } else {
      console.log(`\x1b[33mℹ [Ngrok] Tunnel command launched in background.\x1b[0m`);
    }
  } catch (err) {
    console.warn(`\x1b[31m [Ngrok] Unable to auto-start ngrok: ${err.message}\x1b[0m`);
  }
}

ensureNgrok();
