const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const http = require('node:http');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { chromium } = require('playwright');

async function main() {
  const destination = path.resolve(__dirname, '../../build/gui-demo');
  await fs.mkdir(destination, { recursive: true });
  const root = path.resolve(__dirname, '../..');
  const server = http.createServer(async (request, response) => {
    const requested = decodeURIComponent(new URL(request.url, 'http://localhost').pathname);
    const file = path.resolve(root, '.' + requested);
    if (!file.startsWith(root + path.sep)) {
      response.writeHead(403).end();
      return;
    }
    try {
      const content = await fs.readFile(file);
      const types = { '.png': 'image/png', '.js': 'text/javascript', '.json': 'application/json', '.html': 'text/html' };
      response.setHeader('Content-Type', (types[path.extname(file)] || 'text/plain') + '; charset=utf-8');
      response.end(content);
    } catch {
      response.writeHead(404).end();
    }
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  let browser;
  try {
    browser = await chromium.launch({ channel: 'chrome', headless: true });
    const page = await browser.newPage({ viewport: { width: 1344, height: 1064 } });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.goto(`http://127.0.0.1:${server.address().port}/docs/gui/tab-layout-demo.html`);
    await page.waitForFunction(() => window.demo);
    const initial = await page.evaluate(() => ({
      ready: window.demo.ready,
      error: window.demo.error,
      language: document.documentElement.lang,
      pages: [...document.querySelectorAll('canvas')].map(canvas => canvas.dataset.page),
      bounds: window.demo.textBounds
    }));
    assert.equal(initial.ready, true, initial.error);
    assert.equal(initial.language, 'en');
    assert.deepEqual(initial.pages, ['0', '1', '2', '3']);
    for (const bounds of initial.bounds) {
      assert.ok(bounds.x >= 0 && bounds.y >= 0, bounds.value);
      assert.ok(bounds.x + bounds.width <= 320 && bounds.y + bounds.height <= 214, bounds.value);
    }

    const first = page.locator('figure').first();
    for (const [index, label] of ['Profile', 'Techniques', 'Cultivation', 'Info'].entries()) {
      const button = first.getByRole('button', { name: label, exact: true });
      await button.click();
      assert.equal(await first.locator('canvas').getAttribute('data-page'), String(index));
      assert.equal(await button.getAttribute('aria-pressed'), 'true');
      const accessibleDescription = await first.locator('canvas').getAttribute('aria-label');
      assert.ok(accessibleDescription.includes(index === 0 ? 'player preview only here' : 'no player model'));
    }
    const keyboardButton = first.getByRole('button', { name: 'Profile', exact: true });
    await keyboardButton.focus();
    await page.keyboard.press('Enter');
    assert.equal(await first.locator('canvas').getAttribute('data-page'), '0');
    await page.getByRole('button', { name: 'Show all four tabs' }).click();
    await page.getByRole('button', { name: 'Show all four tabs' }).evaluate(button => button.blur());
    await page.screenshot({ path: path.join(destination, 'onglets-desktop.png'), fullPage: true });

    const pixels = await first.locator('canvas').evaluate(canvas => {
      const data = canvas.getContext('2d').getImageData(0, 0, 320, 214).data;
      let opaque = 0;
      let dark = 0;
      for (let index = 0; index < data.length; index += 4) {
        if (data[index + 3] === 255) opaque++;
        if (data[index] < 70 && data[index + 1] < 70 && data[index + 2] < 70) dark++;
      }
      return { opaque, dark };
    });
    assert.ok(pixels.opaque > 50000 && pixels.dark > 5000, 'La preview ne doit pas etre vide');

    await page.setViewportSize({ width: 390, height: 844 });
    const mobile = await page.evaluate(() => ({
      overflow: document.documentElement.scrollWidth > innerWidth,
      canvases: [...document.querySelectorAll('canvas')].map(canvas => {
        const rect = canvas.getBoundingClientRect();
        return { left: rect.left, right: rect.right, width: rect.width };
      })
    }));
    assert.equal(mobile.overflow, false);
    for (const rect of mobile.canvases) {
      assert.ok(rect.left >= 0 && rect.right <= 390 && rect.width > 0);
    }
    await page.screenshot({ path: path.join(destination, 'onglets-mobile.png'), fullPage: true });
    const localPreview = await browser.newPage();
    await localPreview.goto(pathToFileURL(path.join(__dirname, 'tab-layout-demo.html')).href);
    await localPreview.waitForFunction(() => window.demo);
    assert.equal(await localPreview.evaluate(() => window.demo.ready), true, 'Local HTML preview must load');
    assert.deepEqual(errors, []);
    console.log(JSON.stringify({ result: 'PASS', pages: 4, textBounds: initial.bounds.length,
      language: 'English', navigation: 'mouse and keyboard', mobileOverflow: false,
      localFilePreview: true, browserErrors: 0, destination }, null, 2));
  } finally {
    await browser?.close();
    await new Promise(resolve => server.close(resolve));
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
