const { createRequire } = require('node:module');
const { readFile, writeFile } = require('node:fs/promises');
const path = require('node:path');
const runtimeRequire = createRequire('C:/Users/23142/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/package.json');
const { chromium } = runtimeRequire('playwright');

(async () => {
  const browser = await chromium.launch({ channel: 'msedge', headless: true });
  const context = await browser.newContext({ viewport: { width: 2040, height: 2200 }, deviceScaleFactor: 0.65 });
  const page = await context.newPage();
  const manifest = JSON.parse(await readFile(path.join(__dirname, 'manifest.json'), 'utf8'));
  const report = [];
  for (const diagram of manifest.diagrams) {
    await page.setContent(await readFile(path.join(__dirname, diagram.file), 'utf8'));
    await page.evaluate(() => document.fonts.ready);
    const failures = await page.evaluate(() => {
      const svg = document.querySelector('svg');
      const errors = [];
      for (const node of svg.querySelectorAll('g[aria-label]')) {
        const rect = node.querySelector('rect');
        const frame = rect.getBBox();
        for (const text of node.querySelectorAll('text')) {
          const b = text.getBBox();
          if (b.x < frame.x || b.x + b.width > frame.x + frame.width || b.y < frame.y || b.y + b.height > frame.y + frame.height) {
            errors.push({ text: text.textContent, issue: 'text outside its node' });
          }
        }
      }
      const ids = [...svg.querySelectorAll('[id]')].map(n => n.id);
      if (ids.length !== new Set(ids).size) errors.push({ issue: 'duplicate ids' });
      return errors;
    });
    await page.locator('svg').screenshot({ path: path.join(__dirname, diagram.file.replace('.svg', '.png')) });
    report.push({ file: diagram.file, failures });
  }
  await writeFile(path.join(__dirname, 'visual-check.json'), JSON.stringify(report, null, 2));
  console.log(JSON.stringify(report));
  await browser.close();
  if (report.some(x => x.failures.length)) process.exitCode = 1;
})().catch(error => { console.error(error); process.exitCode = 1; });
