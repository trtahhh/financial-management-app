// E2E Test Setup
const puppeteer = require('puppeteer');

let browser;
let page;

beforeAll(async () => {
  browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  });
});

afterAll(async () => {
  if (browser) {
    await browser.close();
  }
});

beforeEach(async () => {
  page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 720 });
});

afterEach(async () => {
  if (page) {
    await page.close();
  }
});

global.browser = browser;
global.page = page;
