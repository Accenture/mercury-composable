/*
 * Mercury API Playground - the controls around Swagger UI.
 *
 * Sources of an OpenAPI document, in the order the header offers them:
 *   - a URL typed into the field (also ?url=... on this page's address),
 *   - a MiniGraph application in dev mode (host, port, graph id or session id -> /api/openapi/...),
 *   - a document bundled with this application or placed in its api.playground.apps folder
 *     (GET /api/specs lists them, GET /api/specs/{name} serves one),
 *   - a YAML or JSON file opened with the button or dropped anywhere on the page.
 *
 * Swagger UI does the rendering: a URL goes through its DownloadUrl plugin (updateUrl + download)
 * and a file's text through updateSpec, which parses YAML or JSON itself. The last MiniGraph
 * settings are remembered in this browser only (localStorage, best effort).
 */
(function () {
  'use strict';

  var SPECS_LIST = 'api/specs';
  var SPECS_FILE = 'api/specs/';
  var DEFAULT_DOCUMENT = 'demo.yaml';
  var MAX_FILE_BYTES = 10 * 1024 * 1024;
  var STORAGE_KEY = 'mercury-api-playground.minigraph';

  var ui = SwaggerUIBundle({
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [SwaggerUIBundle.presets.apis],
    plugins: [SwaggerUIBundle.plugins.DownloadUrl],
    layout: 'BaseLayout',
    queryConfigEnabled: false,
    displayRequestDuration: true,
    tryItOutEnabled: true
  });

  var $ = function (id) { return document.getElementById(id); };
  var status = $('status');
  var urlInput = $('spec-url');
  var bundledSelect = $('bundled-select');
  var dialog = $('minigraph-dialog');

  function setStatus(text, isError) {
    status.textContent = text;
    status.classList.toggle('mp-error', !!isError);
  }

  function absolute(relative) {
    return new URL(relative, window.location.href).toString();
  }

  // Load a document by URL - Swagger UI fetches it (CORS applies) and reports its own errors.
  function loadUrl(url, label) {
    ui.specActions.updateUrl(url);
    ui.specActions.download(url);
    urlInput.value = url;
    setStatus('Loaded from ' + (label || url));
  }

  function loadText(text, label) {
    ui.specActions.updateUrl('');
    ui.specActions.updateSpec(text);
    urlInput.value = '';
    setStatus('Loaded ' + label + ' - a file is a point-in-time copy; a URL is always current');
  }

  // --- URL field ---------------------------------------------------------------------------
  $('url-form').addEventListener('submit', function (event) {
    event.preventDefault();
    var url = urlInput.value.trim();
    if (!url) {
      setStatus('Enter the URL of an OpenAPI document, or drop a file on the page', true);
      return;
    }
    bundledSelect.value = '';
    loadUrl(url);
  });

  // --- bundled documents -------------------------------------------------------------------
  function loadBundled(name) {
    loadUrl(absolute(SPECS_FILE + encodeURIComponent(name)), 'the bundled document ' + name);
  }

  fetch(absolute(SPECS_LIST), { headers: { Accept: 'application/json' } })
    .then(function (response) { return response.ok ? response.json() : { list: [] }; })
    .then(function (result) {
      var names = Array.isArray(result.list) ? result.list : [];
      names.forEach(function (name) {
        var option = document.createElement('option');
        option.value = name;
        option.textContent = name;
        bundledSelect.appendChild(option);
      });
      var requested = new URLSearchParams(window.location.search).get('url');
      if (requested) {
        loadUrl(requested);
      } else if (names.indexOf(DEFAULT_DOCUMENT) >= 0) {
        bundledSelect.value = DEFAULT_DOCUMENT;
        loadBundled(DEFAULT_DOCUMENT);
      } else if (names.length > 0) {
        bundledSelect.value = names[0];
        loadBundled(names[0]);
      } else {
        setStatus('No bundled document - enter a URL, open the MiniGraph dialog or drop a file');
      }
    })
    .catch(function () {
      setStatus('The bundled document list is not available - enter a URL or drop a file', true);
    });

  bundledSelect.addEventListener('change', function () {
    if (bundledSelect.value) {
      loadBundled(bundledSelect.value);
    }
  });

  // --- files: the button and drag-and-drop -------------------------------------------------
  function acceptFile(file) {
    if (!file) {
      return;
    }
    if (!/\.(ya?ml|json)$/i.test(file.name)) {
      setStatus('Not loaded: ' + file.name + ' - an OpenAPI document is a .yaml, .yml or .json file', true);
      return;
    }
    if (file.size > MAX_FILE_BYTES) {
      setStatus('Not loaded: ' + file.name + ' is larger than 10 MB', true);
      return;
    }
    var reader = new FileReader();
    reader.onload = function () {
      bundledSelect.value = '';
      loadText(String(reader.result), 'the file ' + file.name);
    };
    reader.onerror = function () {
      setStatus('Could not read ' + file.name, true);
    };
    reader.readAsText(file);
  }

  $('file-button').addEventListener('click', function () { $('file-input').click(); });
  $('file-input').addEventListener('change', function (event) {
    acceptFile(event.target.files && event.target.files[0]);
    event.target.value = '';
  });

  var overlay = $('drop-overlay');
  var dragDepth = 0;
  function hasFiles(event) {
    var types = event.dataTransfer && event.dataTransfer.types;
    return !!types && Array.prototype.indexOf.call(types, 'Files') >= 0;
  }
  document.addEventListener('dragenter', function (event) {
    if (!hasFiles(event)) { return; }
    event.preventDefault();
    dragDepth++;
    overlay.hidden = false;
  });
  document.addEventListener('dragover', function (event) {
    if (!hasFiles(event)) { return; }
    event.preventDefault();
    event.dataTransfer.dropEffect = 'copy';
  });
  document.addEventListener('dragleave', function (event) {
    if (!hasFiles(event)) { return; }
    dragDepth = Math.max(0, dragDepth - 1);
    if (dragDepth === 0) { overlay.hidden = true; }
  });
  document.addEventListener('drop', function (event) {
    if (!hasFiles(event)) { return; }
    event.preventDefault();
    dragDepth = 0;
    overlay.hidden = true;
    acceptFile(event.dataTransfer.files[0]);
  });

  // --- MiniGraph dialog --------------------------------------------------------------------
  var host = $('mg-host');
  var port = $('mg-port');
  var identifier = $('mg-id');
  var preview = $('mg-preview');

  function kind() {
    var checked = document.querySelector('input[name="mg-kind"]:checked');
    return checked ? checked.value : 'graph';
  }

  function minigraphUrl() {
    var h = host.value.trim() || '127.0.0.1';
    var p = port.value.trim() || '8085';
    var id = identifier.value.trim();
    var path = kind() === 'session' ? '/api/openapi/session/' : '/api/openapi/';
    return 'http://' + h + ':' + p + path + encodeURIComponent(id || (kind() === 'session' ? '{session-id}' : '{graph-id}'));
  }

  function refreshPreview() {
    preview.textContent = decodeURIComponent(minigraphUrl());
  }

  function remember() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        host: host.value, port: port.value, kind: kind(), id: identifier.value
      }));
    } catch (e) { /* storage unavailable - nothing to remember */ }
  }

  function recall() {
    try {
      var saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
      if (saved) {
        if (saved.host) { host.value = saved.host; }
        if (saved.port) { port.value = saved.port; }
        if (saved.id) { identifier.value = saved.id; }
        var radio = document.querySelector('input[name="mg-kind"][value="' + (saved.kind || 'graph') + '"]');
        if (radio) { radio.checked = true; }
      }
    } catch (e) { /* storage unavailable or corrupt - keep the defaults */ }
  }

  ['input', 'change'].forEach(function (type) {
    $('minigraph-form').addEventListener(type, refreshPreview);
  });

  $('minigraph-button').addEventListener('click', function () {
    recall();
    refreshPreview();
    if (typeof dialog.showModal === 'function') {
      dialog.showModal();
    } else {
      dialog.setAttribute('open', '');
    }
    identifier.focus();
    identifier.select();
  });

  $('mg-cancel').addEventListener('click', function () { dialog.close('cancel'); });

  dialog.addEventListener('close', function () {
    if (dialog.returnValue !== 'load') {
      return;
    }
    if (!identifier.value.trim()) {
      setStatus('Enter a graph id or a session id', true);
      return;
    }
    remember();
    bundledSelect.value = '';
    var url = minigraphUrl();
    loadUrl(url, 'MiniGraph ' + (kind() === 'session' ? 'session ' : 'graph ') + identifier.value.trim() + ' at ' + url);
  });

  window.ui = ui;
})();
