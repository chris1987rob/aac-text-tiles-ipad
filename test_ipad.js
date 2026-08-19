const puppeteer = require('/home/mike/browser-automation/node_modules/puppeteer');

(async () => {
  console.log('=== Running AAC Text Tiles iPad Verification Suite ===');
  
  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox', '--allow-file-access-from-files']
  });

  const page = await browser.newPage();
  
  // Emulate Apple iPad in Landscape (1024x768 @ 2x Retina)
  await page.setViewport({
    width: 1024,
    height: 768,
    deviceScaleFactor: 2,
    isMobile: true,
    hasTouch: true
  });

  const jsErrors = [];
  page.on('pageerror', err => jsErrors.push(err.message));

  await page.goto('file:///home/mike/aac-text-tiles-ipad/index.html', { waitUntil: 'networkidle0' });
  await new Promise(r => setTimeout(r, 500));

  const results = await page.evaluate(async () => {
    const checks = [];

    // Check 1: PWA Manifest and Apple Meta
    const manifestLink = document.querySelector('link[rel="manifest"]');
    const appleIcon = document.querySelector('link[rel="apple-touch-icon"]');
    const appleCapable = document.querySelector('meta[name="apple-mobile-web-app-capable"]');
    checks.push({
      name: 'iPad PWA Manifest & Apple Meta Tags',
      pass: manifestLink && appleIcon && appleCapable && appleCapable.content === 'yes'
    });

    // Check 2: Mulberry Symbol Catalog (3,400+ indexed)
    const hasSymbols = typeof AAC_OFFICIAL_SYMBOLS !== 'undefined' && AAC_OFFICIAL_SYMBOLS.length > 3400;
    checks.push({
      name: 'Mulberry AAC Symbol Library (3,400+ indexed vector SVGs)',
      pass: hasSymbols
    });

    // Check 3: Standard Communication Grids
    switchToBoardView(false);
    const initialTiles = document.querySelectorAll('#tiles-grid .tile').length;
    checks.push({
      name: 'Standard Grid Player Mode',
      pass: initialTiles >= 2
    });

    // Check 4: Express Sentence Bar
    const expressBar = document.getElementById('express-bar-container');
    checks.push({
      name: 'Express Sentence Builder Bar',
      pass: expressBar !== null
    });

    // Check 5: Visual Scene Display & Hotspots
    addNewScenePage();
    selectScenePreset('living-room');
    const hotspotsCount = document.querySelectorAll('#scene-hotspots-container .scene-hotspot').length;
    checks.push({
      name: 'Visual Scene Display with Hotspot Presets',
      pass: hotspotsCount === 5
    });

    // Check 6: Talking Keyboard Page
    addNewKeyboardPage();
    kbTypeKey('H');
    kbTypeKey('E');
    kbTypeKey('L');
    kbTypeKey('L');
    kbTypeKey('O');
    kbSpeakText();
    const typed = document.getElementById('kb-text-display').textContent;
    checks.push({
      name: 'Talking Keyboard Page with Speech Output',
      pass: typed.includes('HELLO') && window.__lastSpoken === 'HELLO'
    });

    // Check 7: Page Wizard
    openPageWizard();
    const wizOpen = document.getElementById('modal-page-wizard').classList.contains('open');
    closePageWizard();
    checks.push({
      name: 'Page Creation Wizard Modal',
      pass: wizOpen
    });

    // Check 8: Online Gallery
    openOnlineGallery();
    const galOpen = document.getElementById('modal-online-gallery').classList.contains('open');
    closeOnlineGallery();
    checks.push({
      name: 'Online AAC Board Gallery',
      pass: galOpen
    });

    // Check 9: Hotspot Editor Modal
    openHotspotEditor(pages[currentPageIndex].hotspots ? pages[currentPageIndex].hotspots[0] : { id: 1, label: 'Test' });
    const hsModalOpen = document.getElementById('modal-hotspot-editor').classList.contains('open');
    closeHotspotEditor();
    checks.push({
      name: 'Visual Scene Hotspot Editor Modal',
      pass: hsModalOpen
    });

    return checks;
  });

  await browser.close();

  console.log('\n--- IPAD VERIFICATION RESULTS ---');
  let allPass = true;
  results.forEach((c, i) => {
    const status = c.pass ? 'PASS' : 'FAIL';
    if (!c.pass) allPass = false;
    console.log(`${status}  ${i + 1}. ${c.name}`);
  });

  const noErrors = jsErrors.length === 0;
  console.log(`${noErrors ? 'PASS' : 'FAIL'}  Zero JS Errors during run (${jsErrors.length} errors)`);
  if (!noErrors) {
    allPass = false;
    console.error('JS Errors:', jsErrors);
  }

  console.log(`\n${results.length + 1}/${results.length + 1} checks evaluated. Final status: ${allPass ? 'ALL PASS!' : 'FAILURES DETECTED'}`);

  if (!allPass) process.exit(1);
})();
