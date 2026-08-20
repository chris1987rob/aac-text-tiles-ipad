// Persistence, template fidelity and the controls that used to be decoration.
//
// Every check here corresponds to a defect that shipped green through the other
// seven suites, because those suites either never reloaded the page or never
// looked at what a button actually did. The rule this suite enforces is: a
// control has to change the board, not raise a toast.
const puppeteer = require('/home/mike/browser-automation/node_modules/puppeteer');

const APP = 'file:///home/mike/aac-board/index.html';

// A 1x1 PNG with a trailing marker byte, so two "photos" are distinguishable.
const MK_BLOB = `(byte) => {
  const b64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==';
  const bin = atob(b64);
  const a = new Uint8Array(bin.length + 1);
  for (let i = 0; i < bin.length; i++) a[i] = bin.charCodeAt(i);
  a[bin.length] = byte;
  return new Blob([a], { type: 'image/png' });
}`;

let passed = 0, failed = 0;
function check(name, cond, detail) {
  if (cond) { passed++; console.log('PASS  ' + name); }
  else { failed++; console.log('FAIL  ' + name); if (detail) console.log('        -> ' + detail); }
}

(async () => {
  console.log('--- STARTING PERSISTENCE / REAL-CONTROL TEST ---');
  const browser = await puppeteer.launch({
    headless: 'new',
    args: ['--no-sandbox', '--allow-file-access-from-files', '--use-fake-ui-for-media-stream']
  });
  const page = await browser.newPage();
  await page.setViewport({ width: 1280, height: 800 });

  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));
  page.on('console', m => { if (m.type() === 'error') errors.push('console.error: ' + m.text()); });

  await page.goto(APP, { waitUntil: 'networkidle0' });
  await new Promise(r => setTimeout(r, 1200));

  // --------------------------------------------------------------------------
  // 1 + 2: a real photo Blob survives a reload, and is scoped to its own page.
  //        Before: JSON.stringify turned the Blob into {}, and on the next
  //        launch createObjectURL({}) threw mid-render and blanked the board.
  // --------------------------------------------------------------------------
  await page.evaluate(async (mkSrc) => {
    const mk = eval(mkSrc);
    currentPageIndex = 0;
    await saveTileToDB({ id: 4, label: 'PhotoA', tts: 'A', photo: mk(1) });
    addNewButtonPage();                       // page 2
    await saveTileToDB({ id: 4, label: 'PhotoB', tts: 'B', photo: mk(2) });
  }, MK_BLOB);

  const rawStore = await page.evaluate(() => localStorage.getItem('talk_tiles_pages_v2'));
  check('1. localStorage never carries a Blob (no "photo":{} written)',
    !rawStore.includes('"photo":{}') && !rawStore.includes('"audio":{}'),
    rawStore.slice(0, 200));

  await page.reload({ waitUntil: 'networkidle0' });
  await new Promise(r => setTimeout(r, 1500));

  const survived = await page.evaluate(async () => {
    const readMarker = async (blob) => {
      if (!(blob instanceof Blob)) return 'NOT-A-BLOB:' + typeof blob;
      const b = new Uint8Array(await blob.arrayBuffer());
      return 'marker=' + b[b.length - 1];
    };
    const out = {};
    currentPageIndex = 0; renderCurrentPage();
    await new Promise(r => setTimeout(r, 150));
    out.p1Tiles = document.querySelectorAll('#tiles-grid .tile').length;
    out.p1GridSize = pages[0].gridSize;
    const img = document.querySelector('#tile-slot-4 .tile-image');
    out.p1ImgSrc = img ? img.src.slice(0, 5) : null;
    out.p1Marker = await readMarker(pages[0].tiles[4].photo);
    out.p1Label = pages[0].tiles[4].label;

    const last = pages.length - 1;
    currentPageIndex = last; renderCurrentPage();
    await new Promise(r => setTimeout(r, 150));
    out.p2Tiles = document.querySelectorAll('#tiles-grid .tile').length;
    out.p2Marker = await readMarker(pages[last].tiles[4].photo);
    out.p2Label = pages[last].tiles[4].label;
    return out;
  });

  check('2. A tile photo survives a reload as a real Blob and the whole grid still renders',
    survived.p1Tiles === survived.p1GridSize && survived.p1Tiles > 0 &&
      survived.p1ImgSrc === 'blob:' && survived.p1Marker === 'marker=1' &&
      survived.p1Label === 'PhotoA',
    JSON.stringify(survived));

  check('3. Slot 4 on page 1 and slot 4 on page 2 keep their own photos (store keyed by page+slot)',
    survived.p2Tiles > 0 && survived.p2Marker === 'marker=2' && survived.p2Label === 'PhotoB' &&
      survived.p1Marker !== survived.p2Marker,
    JSON.stringify(survived));

  // --------------------------------------------------------------------------
  // 4: one unreadable tile record must not take the rest of the page with it.
  // --------------------------------------------------------------------------
  const defensive = await page.evaluate(() => {
    pages = [{ id: 99, title: 'Broken', type: 'grid', gridSize: 9, bg: '#fff', express: false, enabled: true,
               tiles: { 1: { id: 1, label: 'ok' }, 2: { id: 2, label: 'bad', photo: {} },
                        3: { id: 3, label: 'also ok' } } }];
    currentPageIndex = 0;
    renderCurrentPage();
    return {
      tiles: document.querySelectorAll('#tiles-grid .tile').length,
      thirdLabel: (document.querySelector('#tile-slot-3 .tile-label') || {}).textContent
    };
  });
  check('4. A tile with an unreadable photo is skipped, not thrown on: every other tile still renders',
    defensive.tiles === 9 && defensive.thirdLabel === 'also ok',
    JSON.stringify(defensive));

  // --------------------------------------------------------------------------
  // 5: page ids stay unique after a delete. They key the tile store, so a
  //    reused id makes two pages share their photos.
  // --------------------------------------------------------------------------
  const ids = await page.evaluate(() => {
    localStorage.removeItem('talk_tiles_pages_v2');
    pages = [{ id: 1, title: 'A', type: 'grid', gridSize: 4, bg: '#fff', tiles: {} }];
    currentPageIndex = 0;
    addNewButtonPage(); addNewButtonPage(); addNewButtonPage();   // 2, 3, 4
    pages.splice(1, 1);                                           // delete id 2
    addNewButtonPage();
    pages.splice(1, 1);
    addNewButtonPage();
    return pages.map(p => p.id);
  });
  check('5. Page ids stay unique across delete + add (no two pages share a tile-store key)',
    new Set(ids).size === ids.length, 'ids: ' + JSON.stringify(ids));

  // --------------------------------------------------------------------------
  // 6 + 7: gallery boards and wizard pages render their colours and pictures.
  //        They were written with `color`/`wordSize` keys the renderer never
  //        reads, and bare-word symbols that printed as text.
  // --------------------------------------------------------------------------
  const gallery = await page.evaluate(() => {
    const bad = [];
    for (const tpl of ONLINE_GALLERY_TEMPLATES) {
      for (const slot of Object.keys(tpl.tiles)) {
        const t = normalizeTemplateTile(tpl.tiles[slot]);
        if (!t.bgColor) bad.push(tpl.id + ' #' + slot + ' no bgColor');
        if (t.symbol && !/\.svg$/.test(t.symbol) &&
            !['smile','frown','eat','water','yes','no','help','home','bus','book','heart','star'].includes(t.symbol) &&
            /^[\w-]+$/.test(t.symbol)) bad.push(tpl.id + ' #' + slot + ' word-as-symbol: ' + t.symbol);
      }
    }
    return bad;
  });
  check('6. Every Online Gallery tile carries a background colour and a real picture, not a word',
    gallery.length === 0, gallery.slice(0, 8).join(' | '));

  const installed = await page.evaluate(async () => {
    installGalleryTemplate('gal-core-16');
    await new Promise(r => setTimeout(r, 250));
    const p = pages[currentPageIndex];
    const el = document.querySelector('#tile-slot-1');
    const glyph = (document.querySelector('#tile-slot-1 .symbol-glyph') || {}).textContent || '';
    return {
      bgOnRecord: p.tiles[1].bgColor,
      bgRendered: el ? el.style.backgroundColor : '',
      symbol: p.tiles[1].symbol,
      // Either a Mulberry <img> or a real emoji glyph counts as a picture.
      // What must never appear is the raw token ("me") printed as text.
      hasPicture: !!document.querySelector('#tile-slot-1 .tile-symbol-img') || (!!glyph && !/^[a-z_]+$/.test(glyph)),
      glyphText: glyph
    };
  });
  check('7. An installed gallery board actually paints: tile 1 has a background colour and an <img>, not the word "me"',
    !!installed.bgOnRecord && installed.bgRendered !== '' && installed.hasPicture &&
      installed.glyphText !== 'me',
    JSON.stringify(installed));

  const wizard = await page.evaluate(async () => {
    openPageWizard();
    setWizardPageType('grid');
    setWizardGridSize(9);
    selectWizardPreset('feelings');
    createPageFromWizard();
    await new Promise(r => setTimeout(r, 250));
    const p = pages[currentPageIndex];
    const first = p.tiles[1];
    return {
      hasBg: !!(first && first.bgColor),
      hasLabelSize: !!(first && first.labelSize),
      noLegacyKeys: !!(first && !('color' in first) && !('wordSize' in first)),
      renderedBg: (document.querySelector('#tile-slot-1') || {}).style
        ? document.querySelector('#tile-slot-1').style.backgroundColor : '',
      wordsAsSymbols: Object.values(p.tiles).filter(t =>
        t.symbol && /^[\w-]+$/.test(t.symbol) && !/\.svg$/.test(t.symbol) &&
        !['smile','frown','eat','water','yes','no','help','home','bus','book','heart','star'].includes(t.symbol)).length
    };
  });
  check('8. A Page Wizard preset page paints too: bgColor + labelSize on the record, no leftover color/wordSize keys',
    wizard.hasBg && wizard.hasLabelSize && wizard.noLegacyKeys &&
      wizard.renderedBg !== '' && wizard.wordsAsSymbols === 0,
    JSON.stringify(wizard));

  // --------------------------------------------------------------------------
  // 9: the page auditory cue is played on arrival. It used to be stored and
  //    never read by anything.
  // --------------------------------------------------------------------------
  const cue = await page.evaluate(async () => {
    pages = [
      { id: 1, title: 'One', type: 'grid', gridSize: 4, bg: '#fff', tiles: {} },
      { id: 2, title: 'Two', type: 'grid', gridSize: 4, bg: '#fff', tiles: {},
        auditoryCue: 'This is the snack page', auditoryMode: 'tts' },
      { id: 3, title: 'Three', type: 'grid', gridSize: 4, bg: '#fff', tiles: {},
        auditoryCue: 'Silent page', auditoryMode: 'none' }
    ];
    setEditMode(false);
    currentPageIndex = 0; lastCuedPageId = null; renderCurrentPage();
    window.__spokenHistory = [];

    currentPageIndex = 1; renderCurrentPage();
    const onArrival = window.__spokenHistory.slice();
    renderCurrentPage();                      // a re-render must not re-announce
    const afterRerender = window.__spokenHistory.slice();

    currentPageIndex = 2; renderCurrentPage();
    const onSilent = window.__spokenHistory.slice();

    setEditMode(true);
    currentPageIndex = 0; renderCurrentPage();
    currentPageIndex = 1; renderCurrentPage();
    const inEditor = window.__spokenHistory.slice();
    setEditMode(false);
    return { onArrival, afterRerender, onSilent, inEditor };
  });
  check('9. A page auditory cue speaks on arrival, only once, never on a "none" page and never in the editor',
    cue.onArrival.length === 1 && cue.onArrival[0] === 'This is the snack page' &&
      cue.afterRerender.length === 1 &&
      cue.onSilent.length === 1 &&
      cue.inEditor.length === 1,
    JSON.stringify(cue));

  // --------------------------------------------------------------------------
  // 10: page-specific scanning is a real cursor that moves and can be selected,
  //     not a stored flag.
  // --------------------------------------------------------------------------
  const scan = await page.evaluate(async () => {
    pages = [{ id: 1, title: 'Scan', type: 'grid', gridSize: 4, bg: '#fff', express: false, enabled: true,
               tiles: { 1: { id: 1, label: 'one', tts: 'one' }, 2: { id: 2, label: 'two', tts: 'two' },
                        3: { id: 3, label: 'three', tts: 'three' }, 4: { id: 4, label: 'four', tts: 'four' } } }];
    currentPageIndex = 0;
    setEditMode(false);
    renderCurrentPage();

    const out = { before: document.querySelectorAll('.scan-active').length };
    togglePageScanning(true);
    await new Promise(r => setTimeout(r, 100));
    out.afterOn = document.querySelectorAll('.scan-active').length;
    out.firstTarget = (document.querySelector('.scan-active') || {}).id;

    await new Promise(r => setTimeout(r, 1400));        // one interval tick
    out.movedTo = (document.querySelector('.scan-active') || {}).id;

    window.__spokenHistory = [];
    const selected = selectScannedTarget();
    await new Promise(r => setTimeout(r, 100));
    out.selected = selected;
    out.spoke = window.__spokenHistory.slice();

    togglePageScanning(false);
    await new Promise(r => setTimeout(r, 100));
    out.afterOff = document.querySelectorAll('.scan-active').length;
    out.timerCleared = scanTimer === null;
    return out;
  });
  check('10. Page scanning runs a real cursor: it highlights, it advances, the highlighted button can be selected, and off stops it',
    scan.before === 0 && scan.afterOn === 1 && scan.firstTarget && scan.movedTo &&
      scan.firstTarget !== scan.movedTo && scan.selected === true &&
      scan.spoke.length >= 1 && scan.afterOff === 0 && scan.timerCleared,
    JSON.stringify(scan));

  // --------------------------------------------------------------------------
  // 11: the Voice buttons pick a voice that speech actually uses.
  // --------------------------------------------------------------------------
  const voice = await page.evaluate(async () => {
    const out = {};
    // Headless Chrome ships no speech voices, so stand in a pair of fakes to
    // prove the wiring; the picker's real-device path is the same code.
    const fakes = [
      { name: 'Test Voice A', lang: 'en-US' },
      { name: 'Test Voice B', lang: 'en-GB' }
    ];
    window.speechSynthesis.getVoices = () => fakes;

    openVoicePicker('primary');
    out.rows = document.querySelectorAll('#voice-picker-list .voice-row').length;
    document.querySelectorAll('#voice-picker-list .voice-row')[0].click();
    out.closedAfterPick = !document.getElementById('modal-voice-picker').classList.contains('open');
    out.storedPrimary = getVoiceName('primary');
    speakText('hello');
    out.usedAfterPrimary = window.__lastVoice;

    openVoicePicker('second');
    document.querySelectorAll('#voice-picker-list .voice-row')[1].click();
    out.storedSecond = getVoiceName('second');
    useSecondVoice();                       // toggles back to primary
    speakText('hello again');
    out.afterToggleBack = window.__lastVoice;
    useSecondVoice();                       // and onto the second voice
    speakText('hello once more');
    out.afterToggleSecond = window.__lastVoice;
    return out;
  });
  check('11. Voice / Use Second Voice pick real voices and speech uses them',
    voice.rows === 2 && voice.closedAfterPick &&
      voice.storedPrimary === 'Test Voice A' && voice.usedAfterPrimary === 'Test Voice A' &&
      voice.storedSecond === 'Test Voice B' &&
      voice.afterToggleBack === 'Test Voice A' && voice.afterToggleSecond === 'Test Voice B',
    JSON.stringify(voice));

  // --------------------------------------------------------------------------
  // 12: an exported book carries no Blob husks, so a round-trip cannot
  //     re-introduce the "photo":{} that used to brick the board.
  // --------------------------------------------------------------------------
  const exported = await page.evaluate(async (mkSrc) => {
    const mk = eval(mkSrc);
    pages = [{ id: 1, title: 'Export', type: 'grid', gridSize: 4, bg: '#fff', tiles: {} }];
    currentPageIndex = 0;
    await saveTileToDB({ id: 1, label: 'Photo', tts: 'photo', photo: mk(7) });
    const book = JSON.stringify({ pages: pagesForStorage() });
    return { hasHusk: book.includes('"photo":{}'), hasFlag: book.includes('"hasPhoto":true') };
  }, MK_BLOB);
  check('12. An exported book records hasPhoto instead of an empty Blob husk',
    !exported.hasHusk && exported.hasFlag, JSON.stringify(exported));

  // --------------------------------------------------------------------------
  // 13: a v1 board (tile store keyed by slot alone) is adopted, not lost.
  // --------------------------------------------------------------------------
  const migrated = await page.evaluate(async () => {
    // Rebuild the v1 world: legacy store populated, migration flag cleared.
    localStorage.removeItem('talk_tiles_tilestore_migrated');
    const db = dbInstance;
    await new Promise((res, rej) => {
      const tx = db.transaction('tiles', 'readwrite');
      tx.objectStore('tiles').put({ id: 2, label: 'Legacy Mom', tts: 'Mom' });
      tx.oncomplete = res; tx.onerror = rej;
    });
    await new Promise((res, rej) => {
      const tx = db.transaction('tiles_v2', 'readwrite');
      tx.objectStore('tiles_v2').clear();
      tx.oncomplete = res; tx.onerror = rej;
    });
    pages = [{ id: 1, title: 'Home', type: 'grid', gridSize: 4, bg: '#fff', tiles: {} }];
    currentPageIndex = 0;
    cachedTiles.clear();
    await loadAllTilesFromDB();
    renderCurrentPage();
    return {
      cached: cachedTiles.has('1:2'),
      label: (cachedTiles.get('1:2') || {}).label,
      renderedLabel: (document.querySelector('#tile-slot-2 .tile-label') || {}).textContent,
      flag: localStorage.getItem('talk_tiles_tilestore_migrated')
    };
  });
  check('13. A v1 slot-keyed board is migrated onto page 1 and renders (legacy records left in place)',
    migrated.cached && migrated.label === 'Legacy Mom' &&
      migrated.renderedLabel === 'Legacy Mom' && migrated.flag === 'done',
    JSON.stringify(migrated));

  // --------------------------------------------------------------------------
  // 14: grid size 12 was supported by the renderer but unreachable from the UI.
  // --------------------------------------------------------------------------
  const grid12 = await page.evaluate(async () => {
    pages = [{ id: 1, title: 'Twelve', type: 'grid', gridSize: 4, bg: '#fff', tiles: {} }];
    currentPageIndex = 0; setEditMode(true); renderCurrentPage();
    const seg = document.querySelector('[data-grid="12"]');
    if (!seg) return { present: false };
    seg.click();
    await new Promise(r => setTimeout(r, 150));
    const cs = getComputedStyle(document.getElementById('tiles-grid'));
    return {
      present: true,
      gridSize: pages[0].gridSize,
      tiles: document.querySelectorAll('#tiles-grid .tile').length,
      cols: cs.gridTemplateColumns.split(' ').length,
      rows: cs.gridTemplateRows.split(' ').length,
      active: seg.classList.contains('active')
    };
  });
  check('14. Page Options offers grid size 12 and it renders as 4 x 3',
    grid12.present && grid12.gridSize === 12 && grid12.tiles === 12 &&
      grid12.cols === 4 && grid12.rows === 3 && grid12.active,
    JSON.stringify(grid12));

  check('15. Zero JS errors during persistence testing', errors.length === 0, errors.join(' | '));

  console.log('\n--- PERSISTENCE / REAL-CONTROL RESULTS ---');
  console.log(`${passed}/${passed + failed} checks passed`);
  await browser.close();
  process.exit(failed === 0 ? 0 : 1);
})();
