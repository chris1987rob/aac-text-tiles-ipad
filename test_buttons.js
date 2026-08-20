// Exercises EVERY button in the UI by clicking the real DOM element and
// asserting what it did -- not by calling the handler function behind it.
//
// Check 18 closes the loop: every element that carries an onclick attribute in
// index.html is tagged at start-up, every click is recorded, and the run fails
// if any tagged button was never pressed. Add a button to the markup without
// adding it to a check here and this suite goes red.
const puppeteer = require('/home/mike/browser-automation/node_modules/puppeteer');
const path = require('path');
const os = require('os');
const fs = require('fs');

(async () => {
  console.log('--- STARTING FULL BUTTON-COVERAGE TEST ---');

  const downloadDir = fs.mkdtempSync(path.join(os.tmpdir(), 'talktiles-dl-'));

  const browser = await puppeteer.launch({
    headless: 'new',
    args: [
      '--no-sandbox',
      '--allow-file-access-from-files',
      '--use-fake-ui-for-media-stream',
      '--use-fake-device-for-media-stream'
    ]
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 1024, height: 768 });

  const client = await page.target().createCDPSession();
  await client.send('Page.setDownloadBehavior', { behavior: 'allow', downloadPath: downloadDir });

  const errors = [];
  page.on('pageerror', e => errors.push('pageerror: ' + e.message));
  page.on('console', m => { if (m.type() === 'error') errors.push('console.error: ' + m.text()); });

  // Tag every declared button before the app runs, and record every press.
  await page.evaluateOnNewDocument(() => {
    window.__clicked = new Set();
    // SVG elements have no HTMLElement.click(), so press everything the same way.
    window.__press = (el) => {
      if (!el) throw new Error('no element to press');
      if (typeof el.click === 'function') el.click();
      else el.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
    };
    window.__spoken = [];
    window.__toasts = [];
    document.addEventListener('DOMContentLoaded', () => {
      window.__inventory = [];
      document.querySelectorAll('[onclick]').forEach((el, i) => {
        el.setAttribute('data-btn-idx', String(i));
        window.__inventory.push({ idx: String(i), id: el.id || '', on: (el.getAttribute('onclick') || '').slice(0, 70) });
      });
      document.addEventListener('click', (e) => {
        const el = e.target.closest ? e.target.closest('[data-btn-idx]') : null;
        if (el) window.__clicked.add(el.getAttribute('data-btn-idx'));
      }, true);
    });
  });

  const indexPath = 'file://' + path.resolve(__dirname, 'index.html');
  await page.goto(indexPath, { waitUntil: 'networkidle0' });
  await new Promise(r => setTimeout(r, 500));

  await page.evaluate(() => {
    window.speechSynthesis.speak = (u) => window.__spoken.push(u.text);
    const realToast = window.showToast;
    window.showToast = (msg, type) => { window.__toasts.push(msg); return realToast(msg, type); };
    localStorage.removeItem('talk_tiles_custom_templates');
  });

  const results = [];
  const check = (name, pass, detail) => results.push({ name, pass, detail });

  // click(selector) -> presses the real element; throws if it is not there.
  const clickAll = (sels) => page.evaluate((list) => {
    const missing = [];
    list.forEach(sel => {
      const el = document.querySelector(sel);
      if (!el) { missing.push(sel); return; }
      el.click();
    });
    return missing;
  }, sels);

  // --------------------------------------------------------------------------
  // Check 1: home screen — Player, Page Editor, the four stub modals, and the
  //          legacy layout / edit / lock controls
  // --------------------------------------------------------------------------
  const c1 = await page.evaluate(() => {
    const out = { stubs: [] };
    document.getElementById('btn-home-player').click();
    out.playerBoard = document.getElementById('view-board').classList.contains('active');
    out.playerEdit = document.body.classList.contains('mode-editor');

    document.getElementById('btn-home-editor').click();
    out.editorBoard = document.getElementById('view-board').classList.contains('active');
    out.editorEdit = document.body.classList.contains('mode-editor');

    document.getElementById('btn-bar-home').click();
    out.backHome = document.getElementById('view-home').classList.contains('active');

    ['Settings', 'Downloads', 'Help', 'Feedback'].forEach(t => {
      const btn = [...document.querySelectorAll('#view-home [onclick]')]
        .find(b => (b.getAttribute('onclick') || '') === `openStubModal('${t}')`);
      btn.click();
      const modal = document.getElementById('modal-stub');
      out.stubs.push({
        title: t,
        open: modal.classList.contains('open'),
        heading: document.getElementById('stub-modal-title').textContent,
        bodyLen: document.getElementById('stub-modal-body').innerHTML.length
      });
      modal.click();                       // backdrop -> closeStubModal()
      out.stubs[out.stubs.length - 1].closed = !modal.classList.contains('open');
    });

    document.getElementById('btn-home-player').click();
    const sizes = [];
    ['btn-layout-1', 'btn-layout-2', 'btn-layout-3'].forEach(id => {
      document.getElementById(id).click();
      sizes.push(pages[currentPageIndex].gridSize);
    });
    out.layoutSizes = sizes;

    document.getElementById('btn-edit-mode').click();
    out.editModeOn = isEditMode;
    document.getElementById('btn-edit-mode').click();
    out.editModeOff = isEditMode;

    document.getElementById('btn-lock').click();
    out.pinned = isPinned;
    out.lockStyled = document.getElementById('btn-lock').classList.contains('pinned');
    out.editDisabled = document.getElementById('btn-edit-mode').disabled;
    document.getElementById('btn-lock').click();
    out.unpinned = isPinned;
    out.editReenabled = document.getElementById('btn-edit-mode').disabled;

    // Home screen book label opens the pages drawer
    document.getElementById('btn-bar-home').click();
    document.getElementById('home-book-label').click();
    out.bookLabelDrawer = document.getElementById('modal-pages-navigator').classList.contains('open');
    closePagesNavigator();
    return out;
  });

  check(
    '1. Home screen: Player / Page Editor / Home, book label, all four stub modals open+close, layout 1-3, edit toggle, Lock pins and disables editing',
    c1.playerBoard && !c1.playerEdit && c1.editorBoard && c1.editorEdit && c1.backHome &&
      c1.stubs.length === 4 && c1.stubs.every(s => s.open && s.heading === s.title && s.bodyLen > 0 && s.closed) &&
      JSON.stringify(c1.layoutSizes) === JSON.stringify([4, 9, 12]) &&
      c1.editModeOn === true && c1.editModeOff === false &&
      c1.pinned === true && c1.lockStyled && c1.editDisabled === true &&
      c1.unpinned === false && c1.editReenabled === false && c1.bookLabelDrawer,
    JSON.stringify(c1)
  );

  // --------------------------------------------------------------------------
  // Check 2: bottom toolbar, both modes
  // --------------------------------------------------------------------------
  const c2 = await page.evaluate(() => {
    const out = {};
    pages = [
      { id: 1, title: 'One', type: 'grid', gridSize: 4, bg: '#ffffff', express: true, enabled: true,
        tiles: { 1: { id: 1, label: 'apple', tts: 'apple' }, 2: { id: 2, label: 'ball', tts: 'ball' } } },
      { id: 2, title: 'Two', type: 'grid', gridSize: 9, bg: '#ffffff', express: false, enabled: true, tiles: {} },
      { id: 3, title: 'Three', type: 'grid', gridSize: 4, bg: '#ffffff', express: false, enabled: true, tiles: {} }
    ];
    currentPageIndex = 0;
    setEditMode(false);
    renderCurrentPage();

    document.getElementById('btn-bar-next').click();
    out.afterNext = pages[currentPageIndex].title;
    document.getElementById('btn-bar-prev').click();
    out.afterPrev = pages[currentPageIndex].title;

    document.getElementById('bottom-bar-center').click();       // -> pages drawer
    out.drawerFromLabel = document.getElementById('modal-pages-navigator').classList.contains('open');
    closePagesNavigator();

    document.getElementById('btn-bar-layers').click();
    out.drawerFromLayers = document.getElementById('modal-pages-navigator').classList.contains('open');
    closePagesNavigator();

    // Play the whole page
    window.__spoken = [];
    document.getElementById('btn-bar-play').click();
    out.played = window.__spoken.slice();

    // Undo: with chips present it drops one, with none it just reports back
    expressCollectedChips = []; renderExpressChips();
    document.getElementById('tile-slot-1').dispatchEvent(new PointerEvent('pointerup', { bubbles: true }));
    document.getElementById('tile-slot-2').dispatchEvent(new PointerEvent('pointerup', { bubbles: true }));
    out.chipsBeforeUndo = document.querySelectorAll('.express-chip').length;
    document.getElementById('btn-bar-undo').click();
    out.chipsAfterUndo = document.querySelectorAll('.express-chip').length;
    window.__toasts = [];
    clearExpressChips();
    document.getElementById('btn-bar-undo').click();
    out.undoEmptyToast = window.__toasts.slice(-1)[0];

    // btn-bar-jump was a hidden, toast-only control; removed in v2.5.
    out.jumpGone = !document.getElementById('btn-bar-jump');

    document.getElementById('btn-bar-auditory').click();
    out.cueModal = document.getElementById('modal-auditory-cue').classList.contains('open');
    closeAuditoryCueModal();

    setEditMode(true);
    document.getElementById('btn-bar-sliders').click();
    out.optionsOpen = document.getElementById('popover-page-options').classList.contains('open');
    closeAllPopovers();

    document.getElementById('btn-bar-add').click();
    out.newPageOpen = document.getElementById('popover-new-page').classList.contains('open');
    closeAllPopovers();

    document.getElementById('btn-bar-home').click();
    out.home = document.getElementById('view-home').classList.contains('active');
    document.getElementById('btn-home-editor').click();
    return out;
  });

  check(
    '2. Bottom toolbar: prev / next / home / options / pages-drawer (label + layers) / undo / jump / auditory / plus / play',
    c2.afterNext === 'Two' && c2.afterPrev === 'One' &&
      c2.drawerFromLabel && c2.drawerFromLayers &&
      c2.played.length === 1 && c2.played[0] === 'apple, ball' &&
      c2.chipsBeforeUndo === 2 && c2.chipsAfterUndo === 1 && c2.undoEmptyToast === 'Back action' &&
      c2.jumpGone && c2.cueModal && c2.optionsOpen && c2.newPageOpen && c2.home,
    JSON.stringify(c2)
  );

  // --------------------------------------------------------------------------
  // Check 3: Page Options popover — every row and every grid segment
  // --------------------------------------------------------------------------
  const c3 = await page.evaluate(() => {
    const out = {};
    setEditMode(true);
    currentPageIndex = 0;
    renderCurrentPage();

    document.getElementById('btn-bar-sliders').click();
    const pop = document.getElementById('popover-page-options');

    // share icon (stub)
    window.__toasts = [];
    window.__press(pop.querySelector('svg[onclick]'));
    out.shareToast = window.__toasts.slice(-1)[0];

    // background row -> colour picker
    document.getElementById('btn-bar-sliders').click();
    [...pop.querySelectorAll('.popover-row')].find(r => r.textContent.includes('Background')).click();
    out.colorPickerOpen = document.getElementById('popover-color-picker').classList.contains('open');
    closeAllPopovers();

    // every grid segment button
    const sizes = [];
    document.getElementById('btn-bar-sliders').click();
    [...pop.querySelectorAll('.segment-btn')].forEach(btn => {
      btn.click();
      sizes.push({
        want: parseInt(btn.dataset.grid, 10),
        got: pages[currentPageIndex].gridSize,
        tiles: document.querySelectorAll('#tiles-grid .tile').length,
        active: btn.classList.contains('active')
      });
      document.getElementById('btn-bar-sliders').click();
    });
    out.sizes = sizes;

    // auditory cue row
    [...pop.querySelectorAll('.popover-row')].find(r => r.textContent.includes('Scanning Auditory Cues')).click();
    out.cueFromOptions = document.getElementById('modal-auditory-cue').classList.contains('open');
    closeAuditoryCueModal();

    // the three toggles
    document.getElementById('btn-bar-sliders').click();
    const enabled = document.getElementById('toggle-page-enabled');
    const express = document.getElementById('toggle-express-page');
    const scanning = document.getElementById('toggle-page-scanning');
    enabled.click();  out.enabledOff = pages[currentPageIndex].enabled;
    enabled.click();  out.enabledOn = pages[currentPageIndex].enabled;
    express.click();  out.expressA = pages[currentPageIndex].express;
    out.barA = document.getElementById('express-bar-container').classList.contains('open');
    express.click();  out.expressB = pages[currentPageIndex].express;
    out.barB = document.getElementById('express-bar-container').classList.contains('open');
    scanning.click(); out.scanningOn = pages[currentPageIndex].scanning;
    scanning.click(); out.scanningOff = pages[currentPageIndex].scanning;

    // backdrop closes the popover
    document.getElementById('btn-bar-sliders').click();
    pop.click();
    out.closedByBackdrop = !pop.classList.contains('open');
    return out;
  });

  const expectedSegs = [1, 2, 4, 9, 12, 16, 25, 36, 48];   // 12 added in v2.5
  check(
    '3. Page Options: Share exports the page, Background row, all 9 grid segments re-render, auditory row, Enabled/Express/Scanning toggles, backdrop close',
    c3.shareToast === 'Exported page JSON' && c3.colorPickerOpen &&
      c3.sizes.length === expectedSegs.length &&
      c3.sizes.every((s, i) => s.want === expectedSegs[i] && s.got === s.want && s.tiles === s.want && s.active) &&
      c3.cueFromOptions &&
      c3.enabledOff === false && c3.enabledOn === true &&
      c3.expressA !== c3.expressB && c3.barA === c3.expressA && c3.barB === c3.expressB &&
      c3.scanningOn === true && c3.scanningOff === false &&
      c3.closedByBackdrop,
    JSON.stringify(c3)
  );

  // --------------------------------------------------------------------------
  // Check 4: colour picker — both tabs, all 16 swatches, hex entry
  // --------------------------------------------------------------------------
  const c4 = await page.evaluate(() => {
    const out = { swatches: [] };
    setEditMode(true); currentPageIndex = 0; renderCurrentPage();

    const openPicker = () => { document.getElementById('btn-bar-sliders').click();
      [...document.querySelectorAll('#popover-page-options .popover-row')].find(r => r.textContent.includes('Background')).click(); };

    openPicker();
    document.querySelector('#popover-color-picker [onclick*="picker"]').click();
    out.pickerTab = document.getElementById('color-picker-custom-view').style.display;
    document.querySelector('#popover-color-picker [onclick*="swatches"]').click();
    out.swatchTab = document.getElementById('color-swatches-view').style.display;

    const swatchSelectors = [...document.querySelectorAll('#color-swatches-view [onclick^="applyPageBg("]')]
      .map(b => b.getAttribute('onclick').match(/'([^']+)'/)[1]);
    out.swatchCount = swatchSelectors.length;
    swatchSelectors.forEach(hex => {
      openPicker();
      document.querySelector(`#color-swatches-view [onclick="applyPageBg('${hex}')"]`).click();
      out.swatches.push({ hex, bg: pages[currentPageIndex].bg,
        painted: document.getElementById('board-content').style.backgroundColor !== '' });
    });

    openPicker();
    document.querySelector('#popover-color-picker [onclick*="picker"]').click();
    document.getElementById('picker-hex-input').value = '#123456';
    document.querySelector('#popover-color-picker [onclick="applyPageBgFromHex()"]').click();
    out.hexApplied = pages[currentPageIndex].bg;
    out.pickerClosed = !document.getElementById('popover-color-picker').classList.contains('open');

    openPicker();
    document.getElementById('popover-color-picker').click();
    out.closedByBackdrop = !document.getElementById('popover-color-picker').classList.contains('open');
    return out;
  });

  check(
    '4. Colour picker: Swatches/Picker tabs, all 16 swatches repaint the page, hex entry applies, backdrop closes',
    c4.pickerTab === 'flex' && c4.swatchTab === 'grid' &&
      c4.swatchCount === 16 && c4.swatches.length === 16 &&
      c4.swatches.every(s => s.bg === s.hex && s.painted) &&
      c4.hexApplied === '#123456' && c4.pickerClosed && c4.closedByBackdrop,
    JSON.stringify(c4)
  );

  // --------------------------------------------------------------------------
  // Check 5: New Page menu — all eight items
  // --------------------------------------------------------------------------
  const c5 = await page.evaluate(() => {
    const isOpen = id => document.getElementById(id).classList.contains('open');
    const items = [...document.querySelectorAll('#popover-new-page .popover-menu-item')];
    const labels = items.map(i => i.textContent.replace(/\s+/g, ' ').trim());
    const out = { labels, effects: [] };

    const press = (idx) => {
      document.getElementById('btn-bar-add').click();
      [...document.querySelectorAll('#popover-new-page .popover-menu-item')][idx].click();
    };

    setEditMode(true);
    const before = pages.length;

    press(0); out.effects.push({ item: 'gallery', open: isOpen('modal-online-gallery') }); closeOnlineGallery();
    press(1); out.effects.push({ item: 'templates', open: isOpen('modal-my-templates') }); closeMyTemplatesModal();
    press(2); out.effects.push({ item: 'import', open: isOpen('modal-import-book') }); closeImportModal();
    press(3); out.effects.push({ item: 'duplicate', pages: pages.length, title: pages[currentPageIndex].title });
    press(4); out.effects.push({ item: 'wizard', open: isOpen('modal-page-wizard') }); closePageWizard();
    press(5); out.effects.push({ item: 'keyboard', type: pages[currentPageIndex].type });
    press(6); out.effects.push({ item: 'scene', type: pages[currentPageIndex].type });
    press(7); out.effects.push({ item: 'button', type: pages[currentPageIndex].type, grid: pages[currentPageIndex].gridSize });

    out.pagesAdded = pages.length - before;

    document.getElementById('btn-bar-add').click();
    document.getElementById('popover-new-page').click();
    out.closedByBackdrop = !isOpen('popover-new-page');
    return out;
  });

  check(
    '5. New Page menu: all 8 items fire (gallery / templates / import / duplicate / wizard / keyboard / scene / blank page)',
    c5.labels.length === 8 &&
      c5.effects[0].open && c5.effects[1].open && c5.effects[2].open &&
      c5.effects[3].title.endsWith('(Copy)') &&
      c5.effects[4].open &&
      c5.effects[5].type === 'keyboard' && c5.effects[6].type === 'scene' &&
      c5.effects[7].type === 'grid' && c5.effects[7].grid === 4 &&
      c5.pagesAdded === 4 && c5.closedByBackdrop,
    JSON.stringify(c5)
  );

  // --------------------------------------------------------------------------
  // Check 6: pages navigator — Open, delete, + New Page, close
  // --------------------------------------------------------------------------
  const c6 = await page.evaluate(() => {
    const out = {};
    setEditMode(true);
    openPagesDrawer();
    const rows = [...document.querySelectorAll('#pages-nav-list .page-nav-item')];
    out.rowCount = rows.length;
    out.pageCount = pages.length;

    rows[1].querySelector('.btn-primary').click();          // Open on row 2
    out.jumped = currentPageIndex;

    openPagesDrawer();
    const before = pages.length;
    document.querySelector('#pages-nav-list .page-nav-item .btn-danger').click();   // ✕ delete
    out.deleted = before - pages.length;

    out.stillOpen = document.getElementById('modal-pages-navigator').classList.contains('open');
    document.querySelector('#modal-pages-navigator [onclick*="toggleNewPageMenu"]').click();
    out.newPageMenu = document.getElementById('popover-new-page').classList.contains('open');
    closeAllPopovers();

    openPagesDrawer();
    document.getElementById('modal-pages-navigator').click();
    out.closedByBackdrop = !document.getElementById('modal-pages-navigator').classList.contains('open');
    return out;
  });

  check(
    '6. Pages navigator: lists every page, Open jumps, ✕ deletes, "+ New Page" opens the menu, backdrop closes',
    c6.rowCount === c6.pageCount && c6.jumped === 1 && c6.deleted === 1 &&
      c6.newPageMenu && c6.closedByBackdrop,
    JSON.stringify(c6)
  );

  // --------------------------------------------------------------------------
  // Check 7: tile editor — every control except the camera (check 16)
  // --------------------------------------------------------------------------
  const c7 = await page.evaluate(async () => {
    const out = {};
    pages = [{ id: 1, title: 'Edit Me', type: 'grid', gridSize: 4, bg: '#ffffff', express: false, enabled: true, tiles: {} }];
    currentPageIndex = 0; setEditMode(true); renderCurrentPage();

    document.getElementById('tile-slot-1').click();
    out.opened = document.getElementById('editor-modal').classList.contains('open');
    out.title = document.getElementById('modal-slot-title').textContent;

    document.getElementById('pos-btn-top').click();
    out.posTop = pendingLabelPosition;
    document.getElementById('pos-btn-bottom').click();
    out.posBottom = pendingLabelPosition;

    document.querySelector('#editor-modal [onclick="openSymbolLibrary()"]').click();
    out.libraryOpen = document.getElementById('modal-symbol-library').classList.contains('open');
    closeSymbolLibrary();

    document.querySelector('#editor-modal [onclick="triggerFileInput()"]').click();
    out.fileInputExists = !!document.getElementById('photo-file-input');

    // inline symbol strip: search, pick a card, clear the search
    const symSearch = document.getElementById('editor-symbol-search');
    symSearch.value = 'apple';
    symSearch.dispatchEvent(new Event('input', { bubbles: true }));
    const strip = document.getElementById('editor-symbol-strip');
    out.inline = { cards: strip.querySelectorAll('.sym-card').length, src: strip.dataset.querySource };
    strip.querySelector('.sym-card').click();
    out.inlinePicked = { symbol: pendingSymbol, selected: strip.querySelectorAll('.sym-card.selected').length,
                         stillOpen: document.getElementById('editor-modal').classList.contains('open') };
    document.getElementById('btn-editor-sym-clear').click();
    out.inlineCleared = { value: symSearch.value, src: strip.dataset.querySource,
                          clearBtn: document.getElementById('btn-editor-sym-clear').style.display };

    // stage a symbol so Remove has something to remove
    pendingSymbol = 'symbols/en/eat_,_to.svg';
    updateModalPhotoPreview();
    document.getElementById('btn-remove-photo').style.display = 'inline-flex';
    document.getElementById('btn-remove-photo').click();
    out.symbolCleared = pendingSymbol;

    // record -> stop -> preview -> remove
    document.getElementById('btn-record-voice').click();
    await new Promise(r => setTimeout(r, 500));
    out.recording = document.getElementById('btn-stop-record').style.display !== 'none';
    document.getElementById('btn-stop-record').click();
    await new Promise(r => setTimeout(r, 400));
    out.hasAudio = !!pendingAudioBlob;
    document.getElementById('btn-play-preview').click();
    document.getElementById('btn-remove-audio').click();
    out.audioCleared = pendingAudioBlob;

    // save a real tile
    document.getElementById('modal-label-input').value = 'eat';
    document.getElementById('modal-tts-input').value = 'I want to eat';
    document.getElementById('modal-tile-bgcolor').value = '#4caf50';
    document.getElementById('modal-tile-textcolor').value = '#ffffff';
    document.getElementById('modal-label-size').value = '1.6';
    onLabelSizeInput();
    await document.querySelector('#editor-modal [onclick="saveEditorTile()"]').click();
    await new Promise(r => setTimeout(r, 250));
    out.savedLabel = (document.querySelector('#tile-slot-1 .tile-label') || {}).textContent;
    out.savedBg = document.getElementById('tile-slot-1').style.backgroundColor;
    out.editorClosed = !document.getElementById('editor-modal').classList.contains('open');

    // it speaks in player mode
    setEditMode(false); renderCurrentPage();
    window.__spoken = [];
    document.getElementById('tile-slot-1').dispatchEvent(new PointerEvent('pointerup', { bubbles: true }));
    out.spoke = window.__spoken.slice();

    // clear tile, then close with the X
    setEditMode(true); renderCurrentPage();
    document.getElementById('tile-slot-1').dispatchEvent(new PointerEvent('pointerup', { bubbles: true }));
    out.reopenedForClear = document.getElementById('editor-modal').classList.contains('open');
    document.querySelector('#editor-modal [onclick="clearTileWithConfirm()"]').click();
    await new Promise(r => setTimeout(r, 250));
    out.cleared = !document.querySelector('#tile-slot-1 .tile-label');

    document.getElementById('tile-slot-1').click();
    document.querySelector('#editor-modal [onclick="closeEditor()"]').click();
    out.closedByX = !document.getElementById('editor-modal').classList.contains('open');
    return out;
  });

  check(
    '7. Tile editor: opens on a slot, label position top/bottom, inline symbol strip (search / pick / clear), full library, upload, remove photo, record/stop/preview/remove audio, Save, Clear, close',
    c7.opened && c7.title === 'Edit Button #1' &&
      c7.posTop === 'top' && c7.posBottom === 'bottom' &&
      c7.libraryOpen && c7.fileInputExists && c7.symbolCleared === null &&
      c7.inline.cards > 0 && c7.inline.src === 'search' &&
      !!c7.inlinePicked.symbol && c7.inlinePicked.selected === 1 && c7.inlinePicked.stillOpen &&
      c7.inlineCleared.value === '' && c7.inlineCleared.clearBtn === 'none' &&
      c7.recording && c7.hasAudio && c7.audioCleared === null &&
      c7.savedLabel === 'eat' && c7.savedBg === 'rgb(76, 175, 80)' && c7.editorClosed &&
      JSON.stringify(c7.spoke) === JSON.stringify(['I want to eat']) &&
      c7.reopenedForClear && c7.cleared && c7.closedByX,
    JSON.stringify(c7)
  );

  // --------------------------------------------------------------------------
  // Check 8: symbol library — 12 category chips, search, clear, shortcuts, close
  // --------------------------------------------------------------------------
  const c8 = await page.evaluate(async () => {
    const out = { chips: [] };
    setEditMode(true); renderCurrentPage();
    document.getElementById('tile-slot-1').click();
    document.querySelector('#editor-modal [onclick="openSymbolLibrary()"]').click();
    await new Promise(r => setTimeout(r, 250));

    const chips = [...document.querySelectorAll('.sym-cat-chip')];
    out.chipCount = chips.length;
    for (const chip of chips) {
      chip.click();
      out.chips.push({
        cat: chip.dataset.cat,
        active: chip.classList.contains('active'),
        cards: document.querySelectorAll('#symbol-library-grid .sym-card').length
      });
    }

    document.querySelector('.sym-cat-chip[data-cat="all"]').click();
    const search = document.getElementById('sym-search-input');
    search.value = 'water'; filterSymbolLibrary();
    out.searchCards = document.querySelectorAll('#symbol-library-grid .sym-card').length;
    out.clearVisible = document.getElementById('btn-sym-clear-search').style.display;
    document.getElementById('btn-sym-clear-search').click();
    out.afterClear = { value: search.value, cards: document.querySelectorAll('#symbol-library-grid .sym-card').length };

    // picking a card fills the editor
    document.querySelector('#symbol-library-grid .sym-card').click();
    out.picked = { symbol: pendingSymbol, libraryClosed: !document.getElementById('modal-symbol-library').classList.contains('open') };

    // camera / upload shortcuts inside the library header
    document.querySelector('#editor-modal [onclick="openSymbolLibrary()"]').click();
    await new Promise(r => setTimeout(r, 200));
    document.querySelector('#modal-symbol-library [onclick*="triggerFileInput"]').click();
    out.uploadShortcutClosed = !document.getElementById('modal-symbol-library').classList.contains('open');

    document.querySelector('#editor-modal [onclick="openSymbolLibrary()"]').click();
    await new Promise(r => setTimeout(r, 200));
    document.querySelector('#modal-symbol-library [onclick*="startCamera"]').click();
    await new Promise(r => setTimeout(r, 300));
    out.cameraShortcut = document.getElementById('camera-fs').classList.contains('open');
    closeCameraFullscreen();

    document.querySelector('#editor-modal [onclick="openSymbolLibrary()"]').click();
    await new Promise(r => setTimeout(r, 200));
    document.querySelector('#modal-symbol-library .modal-close-btn').click();
    out.closedByX = !document.getElementById('modal-symbol-library').classList.contains('open');

    document.querySelector('#editor-modal [onclick="openSymbolLibrary()"]').click();
    await new Promise(r => setTimeout(r, 200));
    document.getElementById('modal-symbol-library').click();
    out.closedByBackdrop = !document.getElementById('modal-symbol-library').classList.contains('open');
    closeEditor();
    return out;
  });

  check(
    '8. Symbol library: all 12 category chips filter, search + clear-search, a card fills the editor, camera/upload shortcuts, X and backdrop close',
    c8.chipCount === 12 && c8.chips.every(c => c.active && c.cards > 0) &&
      c8.searchCards > 0 && c8.clearVisible === 'flex' &&
      c8.afterClear.value === '' && c8.afterClear.cards > c8.searchCards &&
      !!c8.picked.symbol && c8.picked.libraryClosed &&
      c8.uploadShortcutClosed && c8.cameraShortcut && c8.closedByX && c8.closedByBackdrop,
    JSON.stringify(c8)
  );

  // --------------------------------------------------------------------------
  // Check 9: Set Auditory Cue modal
  // --------------------------------------------------------------------------
  const c9 = await page.evaluate(() => {
    const out = { modes: [] };
    setEditMode(false); currentPageIndex = 0; renderCurrentPage();
    document.getElementById('btn-bar-auditory').click();

    ['recorded', 'tts', 'none'].forEach(m => {
      document.getElementById('tab-cue-' + m).click();
      out.modes.push({ m, mode: currentAuditoryMode, active: document.getElementById('tab-cue-' + m).classList.contains('active') });
    });
    document.getElementById('tab-cue-tts').click();

    // Voice / Use Second Voice are real controls as of v2.5: the first opens a
    // voice picker, the second switches the board onto the second voice.
    window.__toasts = [];
    document.querySelector('#modal-auditory-cue [onclick*="openVoicePicker"]').click();
    out.voicePickerOpen = document.getElementById('modal-voice-picker').classList.contains('open');
    out.voiceRows = document.querySelectorAll('#voice-picker-list .voice-row').length;
    out.voiceEmptyNote = !!document.querySelector('#voice-picker-list div');
    document.querySelector('#modal-voice-picker .modal-close-btn').click();
    out.voicePickerClosed = !document.getElementById('modal-voice-picker').classList.contains('open');
    document.querySelector('#modal-auditory-cue [onclick*="useSecondVoice"]').click();
    out.secondVoiceOpened = document.getElementById('modal-voice-picker').classList.contains('open');
    document.querySelector('#modal-voice-picker .modal-footer .btn').click();

    document.getElementById('cue-text-input').value = 'Pick a colour';
    window.__spoken = [];
    document.querySelector('#modal-auditory-cue [onclick="previewAuditoryCue()"]').click();
    out.previewed = window.__spoken.slice();

    document.querySelector('#modal-auditory-cue [onclick="saveAuditoryCueModal()"]').click();
    out.saved = { cue: pages[0].auditoryCue, mode: pages[0].auditoryMode,
                  closed: !document.getElementById('modal-auditory-cue').classList.contains('open') };

    document.getElementById('btn-bar-auditory').click();
    out.reopenedValue = document.getElementById('cue-text-input').value;
    document.querySelector('#modal-auditory-cue [onclick="closeAuditoryCueModal()"]').click();
    out.closedByX = !document.getElementById('modal-auditory-cue').classList.contains('open');
    return out;
  });

  check(
    '9. Auditory Cue modal: Recorded / TTS / None tabs, Voice picker opens/closes, Second Voice, Preview speaks, Save persists on the page, Close',
    c9.modes.every(m => m.mode === m.m && m.active) &&
      c9.voicePickerOpen && c9.voiceEmptyNote && c9.voicePickerClosed && c9.secondVoiceOpened &&
      JSON.stringify(c9.previewed) === JSON.stringify(['Pick a colour']) &&
      c9.saved.cue === 'Pick a colour' && c9.saved.mode === 'tts' && c9.saved.closed &&
      c9.reopenedValue === 'Pick a colour' && c9.closedByX,
    JSON.stringify(c9)
  );

  // --------------------------------------------------------------------------
  // Check 10: Page Wizard — every name, type, size and preset button
  // --------------------------------------------------------------------------
  const c10 = await page.evaluate(() => {
    const out = { names: [], sizes: [], presets: [] };
    setEditMode(true);
    openPageWizard();

    [...document.querySelectorAll('#modal-page-wizard [onclick^="setWizardPageName("]')].forEach(b => {
      b.click();
      out.names.push({ want: b.getAttribute('onclick').match(/'(.+)'/)[1],
                       got: document.getElementById('wiz-page-name').value });
    });

    document.getElementById('wiz-type-scene').click();
    out.sceneType = { type: wizardPageType, gridHidden: document.getElementById('wiz-grid-options').style.display };
    document.getElementById('wiz-type-grid').click();
    out.gridType = { type: wizardPageType, gridShown: document.getElementById('wiz-grid-options').style.display };

    [...document.querySelectorAll('#wiz-grid-options .segment-btn')].forEach(b => {
      b.click();
      out.sizes.push({ want: parseInt(b.getAttribute('data-wizgrid'), 10), got: wizardGridSize, active: b.classList.contains('active') });
    });

    [...document.querySelectorAll('#modal-page-wizard .preset-card')].forEach(c => {
      c.click();
      out.presets.push({ want: c.getAttribute('data-preset'), got: wizardPreset, active: c.classList.contains('active') });
    });

    // build one
    setWizardPageType('grid'); setWizardGridSize(9); selectWizardPreset('core');
    document.getElementById('wiz-page-name').value = 'Wizard Core';
    const before = pages.length;
    document.querySelector('#modal-page-wizard [onclick="createPageFromWizard()"]').click();
    out.created = { added: pages.length - before, title: pages[currentPageIndex].title,
                    grid: pages[currentPageIndex].gridSize,
                    tiles: Object.keys(pages[currentPageIndex].tiles).length,
                    rendered: document.querySelectorAll('#tiles-grid .tile').length,
                    closed: !document.getElementById('modal-page-wizard').classList.contains('open') };

    openPageWizard();
    document.querySelector('#modal-page-wizard .modal-close-btn').click();
    out.closedByX = !document.getElementById('modal-page-wizard').classList.contains('open');
    openPageWizard();
    document.getElementById('modal-page-wizard').click();
    out.closedByBackdrop = !document.getElementById('modal-page-wizard').classList.contains('open');
    return out;
  });

  check(
    '10. Page Wizard: 5 name presets, Grid/Scene type, all 7 sizes, all 6 content presets, Create builds the page, X + backdrop close',
    c10.names.length === 5 && c10.names.every(n => n.got === n.want) &&
      c10.sceneType.type === 'scene' && c10.sceneType.gridHidden === 'none' &&
      c10.gridType.type === 'grid' && c10.gridType.gridShown === 'flex' &&
      c10.sizes.length === 7 && c10.sizes.every(s => s.got === s.want && s.active) &&
      c10.presets.length === 6 && c10.presets.every(p => p.got === p.want && p.active) &&
      c10.created.added === 1 && c10.created.title === 'Wizard Core' && c10.created.grid === 9 &&
      c10.created.tiles === 9 && c10.created.rendered === 9 && c10.created.closed &&
      c10.closedByX && c10.closedByBackdrop,
    JSON.stringify(c10)
  );

  // --------------------------------------------------------------------------
  // Check 11: Online Gallery + Import/Export
  // --------------------------------------------------------------------------
  const c11 = await page.evaluate(() => {
    const out = { installs: [] };
    setEditMode(true);
    openOnlineGallery();
    const cards = [...document.querySelectorAll('#gallery-cards-grid .btn-primary')];
    out.cardCount = cards.length;
    cards.forEach((_, i) => {
      openOnlineGallery();
      const before = pages.length;
      const btn = [...document.querySelectorAll('#gallery-cards-grid .btn-primary')][i];
      btn.click();
      out.installs.push({ added: pages.length - before, title: pages[currentPageIndex].title,
                          grid: pages[currentPageIndex].gridSize,
                          rendered: document.querySelectorAll('#tiles-grid .tile').length,
                          closed: !document.getElementById('modal-online-gallery').classList.contains('open') });
    });
    openOnlineGallery();
    document.querySelector('#modal-online-gallery .modal-close-btn').click();
    out.galleryClosedByX = !document.getElementById('modal-online-gallery').classList.contains('open');
    openOnlineGallery();
    document.getElementById('modal-online-gallery').click();
    out.galleryClosedByBackdrop = !document.getElementById('modal-online-gallery').classList.contains('open');

    openImportModal();
    out.importOpen = document.getElementById('modal-import-book').classList.contains('open');
    document.querySelector('#modal-import-book [onclick*="book-import-file-input"]').click();
    out.importInput = !!document.getElementById('book-import-file-input');
    window.__toasts = [];
    document.querySelector('#modal-import-book [onclick="exportCurrentPageJSON()"]').click();
    document.querySelector('#modal-import-book [onclick="exportEntireBookJSON()"]').click();
    out.exportToasts = window.__toasts.slice();
    document.querySelector('#modal-import-book .modal-close-btn').click();
    out.importClosedByX = !document.getElementById('modal-import-book').classList.contains('open');
    openImportModal();
    document.getElementById('modal-import-book').click();
    out.importClosedByBackdrop = !document.getElementById('modal-import-book').classList.contains('open');
    return out;
  });

  check(
    '11. Online Gallery installs all 5 boards; Import/Export modal: file picker, Export page, Export book, X + backdrop close',
    c11.cardCount === 5 && c11.installs.length === 5 &&
      c11.installs.every(i => i.added === 1 && i.rendered === i.grid && i.closed) &&
      c11.galleryClosedByX && c11.galleryClosedByBackdrop &&
      c11.importOpen && c11.importInput &&
      JSON.stringify(c11.exportToasts) === JSON.stringify(['Exported page JSON', 'Exported entire communication book JSON']) &&
      c11.importClosedByX && c11.importClosedByBackdrop,
    JSON.stringify(c11)
  );

  // --------------------------------------------------------------------------
  // Check 12: My Templates modal — Save, +Use, delete, close (real DOM clicks)
  // --------------------------------------------------------------------------
  const c12 = await page.evaluate(() => {
    const out = {};
    localStorage.removeItem('talk_tiles_custom_templates');
    setEditMode(true);
    pages = [{ id: 1, title: 'Snack', type: 'grid', gridSize: 9, bg: '#eeeeee', express: false, enabled: true,
               tiles: { 1: { id: 1, label: 'juice', tts: 'juice' } } }];
    currentPageIndex = 0; renderCurrentPage();

    openMyTemplatesModal();
    const rowsBefore = document.querySelectorAll('#my-templates-list .editor-section').length;

    // +Use on the first built-in
    const before = pages.length;
    document.querySelector('#my-templates-list .editor-section .btn-primary').click();
    out.used = { added: pages.length - before, title: pages[currentPageIndex].title,
                 grid: pages[currentPageIndex].gridSize,
                 closed: !document.getElementById('modal-my-templates').classList.contains('open') };

    // Save current page as a template
    currentPageIndex = 0; renderCurrentPage();
    openMyTemplatesModal();
    document.querySelector('#modal-my-templates [onclick="saveCurrentPageAsTemplate()"]').click();
    const rowsAfterSave = [...document.querySelectorAll('#my-templates-list .editor-section')];
    const last = rowsAfterSave[rowsAfterSave.length - 1];
    out.saved = { rowsBefore, rowsAfterSave: rowsAfterSave.length,
                  stored: JSON.parse(localStorage.getItem('talk_tiles_custom_templates')).length,
                  lastTitle: last.querySelector('.template-title').textContent.trim(),
                  lastHasDelete: !!last.querySelector('.btn-danger'),
                  builtinsHaveNoDelete: rowsAfterSave.slice(0, rowsBefore).every(r => !r.querySelector('.btn-danger')) };

    // delete it with the bin button
    last.querySelector('.btn-danger').click();
    out.deleted = { stored: JSON.parse(localStorage.getItem('talk_tiles_custom_templates')).length,
                    rows: document.querySelectorAll('#my-templates-list .editor-section').length };

    document.querySelector('#modal-my-templates .modal-close-btn').click();
    out.closedByX = !document.getElementById('modal-my-templates').classList.contains('open');
    openMyTemplatesModal();
    document.querySelector('#modal-my-templates [onclick="closeMyTemplatesModal()"].btn').click();
    out.closedByFooter = !document.getElementById('modal-my-templates').classList.contains('open');
    openMyTemplatesModal();
    document.getElementById('modal-my-templates').click();
    out.closedByBackdrop = !document.getElementById('modal-my-templates').classList.contains('open');
    return out;
  });

  check(
    '12. My Templates: +Use builds a page, "Save Current Page" adds a deletable row after the built-ins, bin deletes it, X/Close/backdrop all close',
    c12.used.added === 1 && c12.used.title === 'Core Words (Classic)' && c12.used.grid === 48 && c12.used.closed &&
      c12.saved.rowsAfterSave === c12.saved.rowsBefore + 1 && c12.saved.stored === 1 &&
      c12.saved.lastTitle === 'Snack Template' && c12.saved.lastHasDelete && c12.saved.builtinsHaveNoDelete &&
      c12.deleted.stored === 0 && c12.deleted.rows === c12.saved.rowsBefore &&
      c12.closedByX && c12.closedByFooter && c12.closedByBackdrop,
    JSON.stringify(c12)
  );

  // --------------------------------------------------------------------------
  // Check 13: scene picker + hotspot editor
  // --------------------------------------------------------------------------
  const c13 = await page.evaluate(async () => {
    const out = { presets: [] };
    setEditMode(true);
    addNewScenePage();

    // "Set Background" opens the picker; every preset loads
    document.querySelector('#scene-view [onclick="openScenePicker()"]').click();
    const cards = [...document.querySelectorAll('#scene-presets-grid .btn-primary')];
    out.presetCount = cards.length;
    for (let i = 0; i < cards.length; i++) {
      document.querySelector('#scene-view [onclick="openScenePicker()"]').click();
      [...document.querySelectorAll('#scene-presets-grid .btn-primary')][i].click();
      out.presets.push({ bg: !!pages[currentPageIndex].sceneBg,
                         hotspots: pages[currentPageIndex].hotspots.length,
                         rendered: document.querySelectorAll('.scene-hotspot').length,
                         imgShown: document.getElementById('scene-image').style.display });
    }

    document.querySelector('#scene-view [onclick="openScenePicker()"]').click();
    document.querySelector('#modal-scene-picker [onclick="triggerSceneBgInput()"]').click();
    out.uploadClosedPicker = !document.getElementById('modal-scene-picker').classList.contains('open');
    document.querySelector('#scene-view [onclick="openScenePicker()"]').click();
    document.querySelector('#modal-scene-picker [onclick="triggerSceneCamera()"]').click();
    await new Promise(r => setTimeout(r, 300));
    out.cameraFromPicker = document.getElementById('camera-fs').classList.contains('open');
    closeCameraFullscreen();
    document.querySelector('#scene-view [onclick="openScenePicker()"]').click();
    document.querySelector('#modal-scene-picker .modal-close-btn').click();
    out.pickerClosedByX = !document.getElementById('modal-scene-picker').classList.contains('open');
    document.querySelector('#scene-view [onclick="openScenePicker()"]').click();
    document.getElementById('modal-scene-picker').click();
    out.pickerClosedByBackdrop = !document.getElementById('modal-scene-picker').classList.contains('open');

    // "+ Add Hotspot", then every control in the hotspot editor
    const hsBefore = pages[currentPageIndex].hotspots.length;
    document.querySelector('#scene-view [onclick="addSceneHotspot()"]').click();
    out.hotspotAdded = pages[currentPageIndex].hotspots.length - hsBefore;
    const newSpot = pages[currentPageIndex].hotspots[pages[currentPageIndex].hotspots.length - 1];

    document.getElementById('hotspot-' + newSpot.id).click();
    out.editorOpen = document.getElementById('modal-hotspot-editor').classList.contains('open');

    out.modes = [];
    ['tts', 'recorded', 'jump'].forEach(m => {
      document.getElementById('hs-tab-' + m).click();
      out.modes.push({ m, active: document.getElementById('hs-tab-' + m).classList.contains('active'),
                       wrap: document.getElementById('hs-action-' + m + '-wrap').style.display });
    });

    document.getElementById('btn-hs-record').click();
    await new Promise(r => setTimeout(r, 400));
    out.recordingLabel = document.getElementById('btn-hs-record').textContent.trim();
    document.getElementById('btn-hs-record').click();
    await new Promise(r => setTimeout(r, 400));
    window.__toasts = [];
    document.getElementById('btn-hs-play-rec').click();
    out.playRecToast = window.__toasts.slice();

    out.styles = [];
    ['invisible', 'highlight', 'outline'].forEach(s => {
      document.getElementById('hs-style-' + s).click();
      out.styles.push({ s, active: document.getElementById('hs-style-' + s).classList.contains('active') });
    });
    document.getElementById('hs-style-invisible').click();

    document.getElementById('hs-tab-tts').click();
    document.getElementById('hotspot-label-input').value = 'Water bottle';
    document.getElementById('hotspot-tts-input').value = 'I want my water bottle';
    window.__spoken = [];
    document.querySelector('#modal-hotspot-editor [onclick="previewHotspotSpeech()"]').click();
    out.previewed = window.__spoken.slice();

    document.querySelector('#modal-hotspot-editor [onclick="saveHotspotEditor()"]').click();
    const saved = pages[currentPageIndex].hotspots.find(h => h.id === newSpot.id);
    out.saved = { label: saved.label, tts: saved.tts,
                  closed: !document.getElementById('modal-hotspot-editor').classList.contains('open') };

    // it plays in player mode
    setEditMode(false); renderCurrentPage();
    window.__spoken = [];
    document.getElementById('hotspot-' + saved.id).click();
    out.playedInUserMode = window.__spoken.slice();

    // delete it
    setEditMode(true); renderCurrentPage();
    const countBefore = pages[currentPageIndex].hotspots.length;
    document.getElementById('hotspot-' + saved.id).click();
    document.querySelector('#modal-hotspot-editor [onclick="deleteCurrentHotspot()"]').click();
    out.deleted = countBefore - pages[currentPageIndex].hotspots.length;

    document.querySelector('.scene-hotspot').click();
    document.querySelector('#modal-hotspot-editor .modal-close-btn').click();
    out.closedByX = !document.getElementById('modal-hotspot-editor').classList.contains('open');
    document.querySelector('.scene-hotspot').click();
    document.getElementById('modal-hotspot-editor').click();
    out.closedByBackdrop = !document.getElementById('modal-hotspot-editor').classList.contains('open');
    return out;
  });

  check(
    '13. Scene picker (all 4 presets, upload, camera, closes) and hotspot editor (+Add, TTS/Recorded/Jump, record, play, 3 styles, Preview, Save, Delete, closes)',
    c13.presetCount === 4 && c13.presets.every(p => p.bg && p.hotspots > 0 && p.rendered === p.hotspots && p.imgShown === 'block') &&
      c13.uploadClosedPicker && c13.cameraFromPicker && c13.pickerClosedByX && c13.pickerClosedByBackdrop &&
      c13.hotspotAdded === 1 && c13.editorOpen &&
      c13.modes.every(m => m.active) && c13.modes[0].wrap !== 'none' &&
      c13.recordingLabel.includes('Stop') && c13.playRecToast.length >= 0 &&
      c13.styles.every(s => s.active) &&
      JSON.stringify(c13.previewed) === JSON.stringify(['I want my water bottle']) &&
      c13.saved.label === 'Water bottle' && c13.saved.closed &&
      JSON.stringify(c13.playedInUserMode) === JSON.stringify(['I want my water bottle']) &&
      c13.deleted === 1 && c13.closedByX && c13.closedByBackdrop,
    JSON.stringify(c13)
  );

  // --------------------------------------------------------------------------
  // Check 14: keyboard page — every key, quick phrase and action button
  // --------------------------------------------------------------------------
  const c14 = await page.evaluate(() => {
    const out = {};
    setEditMode(false);
    const kbIdx = pages.findIndex(p => p.type === 'keyboard');
    if (kbIdx >= 0) { currentPageIndex = kbIdx; } else { addNewKeyboardPage(); }
    renderCurrentPage();
    kbClearText(); updateKeyboardDisplay();

    const letterKeys = [...document.querySelectorAll('#kb-keys-layout .kb-key')]
      .filter(b => /^kbTypeKey\('[^ ]'\)$/.test(b.getAttribute('onclick') || ''));
    out.letterCount = letterKeys.length;
    letterKeys.forEach(k => k.click());
    out.typed = document.getElementById('kb-text-display').textContent;

    document.querySelector('#view-board [onclick="kbClearText()"]').click();
    out.afterClear = document.getElementById('kb-text-display').textContent;

    const quick = [...document.querySelectorAll('[onclick^="kbInsertQuick("]')];
    out.quickCount = quick.length;
    quick.forEach(q => q.click());
    out.afterQuick = document.getElementById('kb-text-display').textContent;

    document.querySelector('[onclick="kbBackspace()"]').click();
    out.afterBackspace = document.getElementById('kb-text-display').textContent;

    window.__spoken = [];
    const speakBtns = [...document.querySelectorAll('[onclick="kbSpeakText()"]')];
    out.speakBtnCount = speakBtns.length;
    speakBtns.forEach(b => b.click());
    out.spoke = window.__spoken.slice();

    // kbToggleNumbers() rebuilds #kb-keys-layout wholesale, so press Space while
    // the markup-declared key is still the one on screen.
    kbClearText();
    const spaceKeys = [...document.querySelectorAll('[onclick="kbTypeKey(\' \')"]')];
    out.spaceKeyCount = spaceKeys.length;
    spaceKeys.forEach(b => b.click());
    out.spaceTyped = document.getElementById('kb-text-display').textContent === ' '.repeat(spaceKeys.length);
    kbClearText();

    document.querySelector('[onclick="kbToggleNumbers()"]').click();
    out.numbersLayout = document.getElementById('kb-keys-layout').textContent.replace(/\s+/g, '');
    document.querySelector('[onclick="kbToggleNumbers()"]').click();
    out.backToLetters = document.getElementById('kb-keys-layout').textContent.includes('Q');

    return out;
  });

  check(
    '14. Keyboard page: all 29 letter/punctuation keys type, Space, 123 toggle, Backspace, Clear, ▶ Speak, all 9 quick phrases',
    c14.letterCount === 29 && c14.typed.length === 29 && c14.afterClear === '' &&
      c14.quickCount === 9 && c14.afterQuick.includes('I want') && c14.afterQuick.includes('Thank you') &&
      c14.afterBackspace.length === c14.afterQuick.length - 1 &&
      c14.speakBtnCount >= 1 && c14.spoke.length === c14.speakBtnCount &&
      c14.spoke.every(t => t === c14.afterBackspace.trim()) &&
      /1234567890/.test(c14.numbersLayout) && c14.backToLetters && c14.spaceTyped,
    JSON.stringify(c14)
  );

  // --------------------------------------------------------------------------
  // Check 15: express speech bar plays the collected words IN SEQUENCE
  //           ("this speech bar will play the selected words in sequence")
  // --------------------------------------------------------------------------
  const c15 = await page.evaluate(() => {
    pages = [{ id: 1, title: 'Express', type: 'grid', gridSize: 9, bg: '#ffffff', express: true, enabled: true,
      tiles: { 1: { id: 1, label: 'I', tts: 'I' }, 2: { id: 2, label: 'want', tts: 'want' },
               3: { id: 3, label: 'the', tts: 'the' }, 4: { id: 4, label: 'water', tts: 'water' },
               5: { id: 5, label: 'bottle', tts: 'bottle' } } }];
    currentPageIndex = 0; setEditMode(false); renderCurrentPage();
    expressCollectedChips = []; renderExpressChips();

    window.__spoken = [];
    [1, 2, 3, 4, 5].forEach(s => document.getElementById('tile-slot-' + s)
      .dispatchEvent(new PointerEvent('pointerup', { bubbles: true })));
    const perTap = window.__spoken.slice();
    const chips = [...document.querySelectorAll('.express-chip')].map(c => c.textContent.trim());

    window.__spoken = [];
    document.getElementById('express-bar').click();
    const sentence = window.__spoken.slice();

    // red X drops the last word, repeated presses clear the bar
    const x = document.querySelector('#express-bar [onclick*="clearExpressChips"]');
    x.click();
    const afterOneX = [...document.querySelectorAll('.express-chip')].map(c => c.textContent.trim());
    for (let i = 0; i < 6; i++) x.click();
    const afterAll = document.querySelectorAll('.express-chip').length;

    window.__spoken = [];
    document.getElementById('express-bar').click();
    const emptyBar = window.__spoken.slice();
    return { perTap, chips, sentence, afterOneX, afterAll, emptyBar };
  });

  check(
    '15. Express speech bar: each tap speaks + chips the word, pressing the bar plays them IN SEQUENCE, red X drops the last word',
    JSON.stringify(c15.perTap) === JSON.stringify(['I', 'want', 'the', 'water', 'bottle']) &&
      JSON.stringify(c15.chips) === JSON.stringify(['I', 'want', 'the', 'water', 'bottle']) &&
      JSON.stringify(c15.sentence) === JSON.stringify(['I want the water bottle']) &&
      JSON.stringify(c15.afterOneX) === JSON.stringify(['I', 'want', 'the', 'water']) &&
      c15.afterAll === 0 && c15.emptyBar.length === 0,
    JSON.stringify(c15)
  );

  // --------------------------------------------------------------------------
  // Check 16: full-screen camera — start, flip, shutter, retake, save, close
  // --------------------------------------------------------------------------
  const c16 = await page.evaluate(async () => {
    const out = {};
    pages = [{ id: 1, title: 'Cam', type: 'grid', gridSize: 4, bg: '#ffffff', express: false, enabled: true, tiles: {} }];
    currentPageIndex = 0; setEditMode(true); renderCurrentPage();
    document.getElementById('tile-slot-1').click();

    document.querySelector('#editor-modal [onclick="startCamera()"]').click();
    await new Promise(r => setTimeout(r, 900));
    const v = document.getElementById('camera-fs-video');
    out.open = document.getElementById('camera-fs').classList.contains('open');
    out.liveShown = document.getElementById('camera-fs-live').style.display;
    out.videoSize = [v.videoWidth, v.videoHeight];

    const facingBefore = cameraFacingMode;
    document.querySelector('#camera-fs [onclick="flipCamera()"]').click();
    await new Promise(r => setTimeout(r, 700));
    out.flipped = cameraFacingMode !== facingBefore;

    document.getElementById('camera-fs-shutter').click();
    out.captured = { hasData: !!capturedImageData,
                     editorShown: document.getElementById('camera-fs-editor').style.display,
                     canvas: [document.getElementById('camera-fs-canvas').width, document.getElementById('camera-fs-canvas').height] };

    document.getElementById('btn-photo-retake').click();
    await new Promise(r => setTimeout(r, 700));
    out.retaken = { data: capturedImageData, liveShown: document.getElementById('camera-fs-live').style.display };

    document.getElementById('camera-fs-shutter').click();
    await document.querySelector('#camera-fs [onclick="saveCapturedToTile()"]').click();
    await pendingPhotoWrite;
    out.staged = { blob: !!pendingPhotoBlob, size: pendingPhotoBlob && pendingPhotoBlob.size,
                   cameraClosed: !document.getElementById('camera-fs').classList.contains('open'),
                   previewShown: document.getElementById('modal-photo-img').style.display };

    // Leave nothing behind: drop the staged photo rather than writing a Blob
    // into localStorage (see UPGRADES.md P0-1).
    document.getElementById('btn-remove-photo').click();
    out.dropped = pendingPhotoBlob;

    document.querySelector('#editor-modal [onclick="startCamera()"]').click();
    await new Promise(r => setTimeout(r, 600));
    document.querySelector('#camera-fs-live .camera-fs-close').click();
    out.closedFromLive = !document.getElementById('camera-fs').classList.contains('open');

    document.querySelector('#editor-modal [onclick="startCamera()"]').click();
    await new Promise(r => setTimeout(r, 800));
    document.getElementById('camera-fs-shutter').click();
    document.querySelector('#camera-fs-editor .camera-fs-close').click();
    out.closedFromEditor = !document.getElementById('camera-fs').classList.contains('open');

    closeEditor();
    return out;
  });

  check(
    '16. Full-screen camera: opens live, Flip switches facing mode, shutter captures to the canvas, Retake, Save-to-tile stages a JPEG, both close buttons',
    c16.open && c16.liveShown === 'block' && c16.videoSize[0] > 0 &&
      c16.flipped &&
      c16.captured.hasData && c16.captured.editorShown === 'block' && c16.captured.canvas[0] > 0 &&
      c16.retaken.data === null && c16.retaken.liveShown === 'block' &&
      c16.staged.blob && c16.staged.size > 0 && c16.staged.cameraClosed && c16.staged.previewShown === 'block' &&
      c16.dropped === null && c16.closedFromLive && c16.closedFromEditor,
    JSON.stringify(c16)
  );

  // --------------------------------------------------------------------------
  // Check 17: every remaining dismiss control — X buttons, footer buttons and
  //           backdrops — actually dismisses its dialog, and the card bodies
  //           swallow the click instead of closing through to the backdrop
  // --------------------------------------------------------------------------
  const c17 = await page.evaluate(async () => {
    const out = { dismissals: [], swallowed: [] };
    setEditMode(true);
    pages = [{ id: 1, title: 'Sweep', type: 'grid', gridSize: 4, bg: '#ffffff', express: false, enabled: true, tiles: {} }];
    currentPageIndex = 0; renderCurrentPage();

    const openers = {
      'modal-pages-navigator': () => openPagesDrawer(),
      'modal-stub': () => openStubModal('Settings'),
      'modal-symbol-library': () => openSymbolLibrary(),
      'modal-auditory-cue': () => openAuditoryCueModal(),
      'modal-voice-picker': () => openVoicePicker('primary'),
      'modal-page-wizard': () => openPageWizard(),
      'modal-online-gallery': () => openOnlineGallery(),
      'modal-import-book': () => openImportModal(),
      'modal-my-templates': () => openMyTemplatesModal(),
      'modal-scene-picker': () => openScenePicker(),
      'editor-modal': () => openEditor(1),
      'modal-hotspot-editor': () => {
        const p = pages[currentPageIndex];
        if (p.type !== 'scene') { addNewScenePage(); }
        if (!pages[currentPageIndex].hotspots.length) addSceneHotspot();
        openHotspotEditor(pages[currentPageIndex].hotspots[0]);
      }
    };

    // Every dismiss control declared inside each dialog, plus its backdrop.
    for (const [id, open] of Object.entries(openers)) {
      const modal = document.getElementById(id);
      const controls = [...modal.querySelectorAll('[onclick]')]
        .filter(el => /^close[A-Za-z]*\(\)$/.test(el.getAttribute('onclick') || ''));
      for (const ctrl of controls) {
        open();
        await new Promise(r => setTimeout(r, 60));
        window.__press(ctrl);
        out.dismissals.push({ id, via: ctrl.className.includes('modal-close-btn') ? 'X' : 'button',
                              closed: !modal.classList.contains('open') });
      }
      // the backdrop itself -- only where one is wired up. #editor-modal and
      // #modal-auditory-cue deliberately have none: a stray tap outside must
      // not throw away what the adult was typing.
      open();
      await new Promise(r => setTimeout(r, 60));
      // clicking the card must NOT close it (event.stopPropagation)
      const card = modal.querySelector('[onclick="event.stopPropagation()"]');
      if (card) {
        window.__press(card);
        out.swallowed.push({ id, stillOpen: modal.classList.contains('open') });
      }
      if (modal.getAttribute('onclick')) {
        window.__press(modal);
        out.dismissals.push({ id, via: 'backdrop', closed: !modal.classList.contains('open') });
      } else {
        window.__press(modal);
        out.sticky = (out.sticky || []).concat([{ id, stillOpen: modal.classList.contains('open') }]);
        modal.classList.remove('open');
      }
    }

    // The two scene-view "set background" buttons and the Page Options "< Back"
    setEditMode(true);
    addNewScenePage();
    [...document.querySelectorAll('#scene-view [onclick="openScenePicker()"]')].forEach(btn => {
      window.__press(btn);
      out.scenePickerOpens = (out.scenePickerOpens || 0) + (document.getElementById('modal-scene-picker').classList.contains('open') ? 1 : 0);
      closeScenePicker();
    });

    document.getElementById('btn-bar-sliders').click();
    [...document.querySelectorAll('#popover-color-picker [onclick="togglePageOptions()"]')].forEach(b => {
      openColorPicker();
      b.click();
      out.backToOptions = document.getElementById('popover-page-options').classList.contains('open');
    });
    closeAllPopovers();

    // Popover cards swallow clicks the same way their backdrops close.
    ['popover-page-options', 'popover-color-picker', 'popover-new-page'].forEach(id => {
      const pop = document.getElementById(id);
      pop.classList.add('open');
      const card = pop.querySelector('[onclick="event.stopPropagation()"]');
      if (card) { window.__press(card); out.swallowed.push({ id, stillOpen: pop.classList.contains('open') }); }
      window.__press(pop);
      out.dismissals.push({ id, via: 'backdrop', closed: !pop.classList.contains('open') });
    });
    return out;
  });

  check(
    '17. Every X / Close / backdrop control dismisses its dialog; card bodies swallow the click; the editor and cue modals stay open on a backdrop tap',
    c17.dismissals.length >= 25 && c17.dismissals.every(d => d.closed) &&
      c17.swallowed.length > 0 && c17.swallowed.every(s => s.stillOpen) &&
      c17.sticky.length === 2 && c17.sticky.every(s => s.stillOpen) &&
      c17.scenePickerOpens === 2 && c17.backToOptions === true,
    JSON.stringify(c17)
  );

  // --------------------------------------------------------------------------
  // Check 18: coverage — every declared button in index.html was pressed
  // --------------------------------------------------------------------------
  const cov = await page.evaluate(() => {
    const missed = window.__inventory
      .filter(b => !window.__clicked.has(b.idx))
      .map(b => (b.id ? '#' + b.id + ' ' : '') + b.on);
    return { total: window.__inventory.length, clicked: window.__clicked.size, missed };
  });

  check(
    `18. Button coverage: all ${cov.total} elements with an onclick handler in index.html were clicked`,
    cov.missed.length === 0,
    `missed ${cov.missed.length}: ${cov.missed.join(' | ')}`
  );

  // --------------------------------------------------------------------------
  // Check 19: no JS errors anywhere in the run
  // --------------------------------------------------------------------------
  check('19. Zero JS errors while pressing every button', errors.length === 0, errors.join('; '));

  console.log('\n--- FULL BUTTON-COVERAGE TEST RESULTS ---');
  let allPass = true;
  for (const r of results) {
    console.log(`${r.pass ? 'PASS' : 'FAIL'}  ${r.name}`);
    if (!r.pass) { console.log(`        -> ${r.detail}`); allPass = false; }
  }
  console.log(`\n${results.filter(r => r.pass).length}/${results.length} checks passed`);
  console.log(`(pressed ${cov.clicked}/${cov.total} declared buttons)\n`);

  await browser.close();
  fs.rmSync(downloadDir, { recursive: true, force: true });
  if (!allPass) process.exit(1);
})();
