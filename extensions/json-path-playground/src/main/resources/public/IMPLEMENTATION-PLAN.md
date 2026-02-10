# JSON-Path Playground - Implementation Plan

## Overview
This plan breaks down the UI enhancement specification into concrete, sequential tasks that can be executed systematically.

---

## Pre-Implementation Setup

### Task 0: Environment Preparation
- [ ] Review current codebase structure
- [ ] Ensure dev server is running (`npm run dev`)
- [ ] Create feature branch if not already done
- [ ] Backup current working state (git commit)

**Estimated Time**: 5 minutes

---

## Phase 1: Foundation & Visual Polish (2-3 hours)

### Task 1.1: Add CSS Variables to Global Styles
**File**: `src/index.css`

**Action**: Add CSS custom properties at the top of the file
```css
:root {
  --primary-color: #2563eb;
  --primary-hover: #1d4ed8;
  --success-color: #10b981;
  --warning-color: #f59e0b;
  --danger-color: #ef4444;
  --bg-primary: #ffffff;
  --bg-secondary: #f8fafc;
  --bg-dark: #1e293b;
  --border-color: #e2e8f0;
  --text-primary: #0f172a;
  --text-secondary: #64748b;
  --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
  --shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.1);
  --radius: 0.5rem;
}
```

**Test**: Verify variables are accessible in DevTools

**Estimated Time**: 5 minutes

---

### Task 1.2: Refactor Main Container Layout
**File**: `src/components/JsonPathPlayground.module.css`

**Actions**:
1. Update `.container` to use CSS Grid for two-column layout
2. Add card styling with shadows
3. Create responsive breakpoint for mobile (< 768px stacks vertically)
4. Update button styles to use new color variables

**CSS Structure**:
```css
.container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.5rem;
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
}

.leftPanel,
.rightPanel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.card {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius);
  padding: 1.5rem;
  box-shadow: var(--shadow-sm);
}

@media (max-width: 768px) {
  .container {
    grid-template-columns: 1fr;
  }
}
```

**Test**: Layout responds correctly on desktop and mobile

**Estimated Time**: 30 minutes

---

### Task 1.3: Create Connection Status Component
**File**: `src/components/JsonPathPlayground.jsx`

**Actions**:
1. Add `ConnectionStatus` component function inside the main component
2. Replace existing status bar div with new component
3. Add corresponding CSS styles

**Code to Add**:
```jsx
const ConnectionStatus = ({ connected, url }) => (
  <div className={styles.statusCard}>
    <div className={styles.statusIndicator}>
      <span className={`${styles.statusDot} ${connected ? styles.connected : styles.disconnected}`} />
      <span className={styles.statusText}>
        {connected ? 'Connected' : 'Disconnected'}
      </span>
    </div>
    <span className={styles.statusUrl}>{url}</span>
  </div>
);

// Usage in main component:
<ConnectionStatus connected={connected} url={wsUrl.current} />
```

**CSS to Add** (in `JsonPathPlayground.module.css`):
```css
.statusCard {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  background: var(--bg-secondary);
  border-radius: var(--radius);
  border: 1px solid var(--border-color);
}

.statusIndicator {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.statusDot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;
}

.statusDot.connected {
  background: var(--success-color);
  animation: pulse 2s infinite;
}

.statusDot.disconnected {
  background: var(--danger-color);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.statusText {
  font-weight: 600;
  color: var(--text-primary);
}

.statusUrl {
  font-family: monospace;
  font-size: 0.875rem;
  color: var(--text-secondary);
}
```

**Test**: Status indicator shows correct color and animation

**Estimated Time**: 30 minutes

---

### Task 1.4: Update Button Styles
**File**: `src/components/JsonPathPlayground.module.css`

**Actions**:
1. Update button colors to use CSS variables
2. Add icons to buttons (using Unicode or text)
3. Improve hover/active states

**CSS Updates**:
```css
.button {
  padding: 0.75rem 1.5rem;
  font-size: 1rem;
  font-weight: 500;
  border: none;
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.2s ease;
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.buttonPrimary {
  background-color: var(--primary-color);
  color: white;
}

.buttonPrimary:hover:not(:disabled) {
  background-color: var(--primary-hover);
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.buttonWarning {
  background-color: var(--warning-color);
  color: var(--text-primary);
}

.buttonWarning:hover:not(:disabled) {
  background-color: #e08e0b;
  transform: translateY(-1px);
  box-shadow: var(--shadow-md);
}

.button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
```

**Test**: Buttons have smooth hover effects and proper disabled states

**Estimated Time**: 20 minutes

---

### Task 1.5: Restructure JSX Layout
**File**: `src/components/JsonPathPlayground.jsx`

**Actions**:
1. Wrap header and links in a header section (full width)
2. Create left panel for inputs and controls
3. Create right panel for console
4. Update className assignments

**JSX Structure**:
```jsx
return (
  <div className={styles.wrapper}>
    <header className={styles.header}>
      <h1 className={styles.title}>JSON-Path Playground</h1>
      <nav className={styles.linkSection}>
        {/* existing links */}
      </nav>
    </header>

    <div className={styles.container}>
      <div className={styles.leftPanel}>
        <ConnectionStatus connected={connected} url={wsUrl.current} />
        
        <div className={styles.card}>
          {/* Command input */}
        </div>
        
        <div className={styles.card}>
          {/* Payload textarea */}
        </div>
        
        <div className={styles.buttonGroup}>
          {/* Buttons */}
        </div>
      </div>

      <div className={styles.rightPanel}>
        {showConsole && (
          <div className={styles.card}>
            {/* Console */}
          </div>
        )}
      </div>
    </div>
  </div>
);
```

**Test**: Layout displays correctly in two columns on desktop

**Estimated Time**: 30 minutes

---

**Phase 1 Checkpoint**: Commit changes with message "feat: implement modern layout and visual polish"

---

## Phase 2: Enhanced Console (2-3 hours)

### Task 2.1: Create Message Parser Utility
**New File**: `src/utils/messageParser.js`

**Actions**:
1. Create utils directory
2. Create messageParser.js file
3. Implement parseMessage function

**Code**:
```javascript
export const parseMessage = (msg) => {
  try {
    const parsed = JSON.parse(msg);
    return {
      type: parsed.type || 'info',
      message: parsed.message || msg,
      time: parsed.time,
      raw: msg
    };
  } catch {
    return {
      type: 'raw',
      message: msg,
      time: null,
      raw: msg
    };
  }
};

export const getMessageIcon = (type) => {
  const icons = {
    info: 'ℹ️',
    error: '❌',
    ping: '🔄',
    welcome: '👋',
    raw: '📝'
  };
  return icons[type] || '•';
};
```

**Test**: Import and test with sample messages

**Estimated Time**: 15 minutes

---

### Task 2.2: Create ConsoleMessage Component
**File**: `src/components/JsonPathPlayground.jsx`

**Actions**:
1. Add ConsoleMessage component function
2. Import parseMessage and getMessageIcon utilities

**Code**:
```jsx
import { parseMessage, getMessageIcon } from '../utils/messageParser';

const ConsoleMessage = ({ message }) => {
  const parsed = parseMessage(message);
  const icon = getMessageIcon(parsed.type);

  return (
    <div className={`${styles.consoleMessage} ${styles[`messageType-${parsed.type}`]}`}>
      <span className={styles.messageIcon}>{icon}</span>
      <span className={styles.messageContent}>{parsed.message}</span>
      {parsed.time && (
        <span className={styles.messageTime}>{parsed.time}</span>
      )}
    </div>
  );
};
```

**Test**: Messages render with icons

**Estimated Time**: 20 minutes

---

### Task 2.3: Update Console Rendering
**File**: `src/components/JsonPathPlayground.jsx`

**Actions**:
1. Replace console pre element with mapped ConsoleMessage components
2. Add consoleRef for scroll control

**Code Update**:
```jsx
const consoleRef = useRef(null);

// In JSX:
<div className={styles.console} ref={consoleRef}>
  {messages.map((msg, idx) => (
    <ConsoleMessage key={idx} message={msg} />
  ))}
  {messages.length === 0 && (
    <div className={styles.emptyConsole}>
      No messages yet. Click "Start" to connect.
    </div>
  )}
</div>
```

**Test**: Messages display with proper formatting

**Estimated Time**: 20 minutes

---

### Task 2.4: Add Console Message Styles
**File**: `src/components/JsonPathPlayground.module.css`

**Actions**:
1. Update console container styles
2. Add styles for ConsoleMessage
3. Add message type-specific styles

**CSS**:
```css
.console {
  background-color: var(--bg-dark);
  color: #e2e8f0;
  padding: 1rem;
  border-radius: var(--radius);
  min-height: 300px;
  max-height: 600px;
  overflow-y: auto;
  font-family: 'Courier New', Courier, monospace;
  font-size: 0.875rem;
}

.consoleMessage {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 0.75rem;
  padding: 0.5rem;
  margin-bottom: 0.25rem;
  border-radius: 0.25rem;
  transition: background-color 0.15s;
}

.consoleMessage:hover {
  background-color: rgba(255, 255, 255, 0.05);
}

.messageIcon {
  font-size: 1rem;
}

.messageContent {
  word-wrap: break-word;
  color: #e2e8f0;
}

.messageTime {
  font-size: 0.75rem;
  color: #94a3b8;
  white-space: nowrap;
}

.messageType-error .messageContent {
  color: #fca5a5;
}

.messageType-info .messageContent {
  color: #93c5fd;
}

.emptyConsole {
  color: #64748b;
  text-align: center;
  padding: 2rem;
}
```

**Test**: Console messages have distinct styling by type

**Estimated Time**: 25 minutes

---

### Task 2.5: Add Console Controls
**File**: `src/components/JsonPathPlayground.jsx`

**Actions**:
1. Add state for auto-scroll
2. Create console controls bar
3. Implement scroll behavior
4. Add copy and clear functions

**Code**:
```jsx
const [autoScroll, setAutoScroll] = useState(true);

// Auto-scroll effect
useEffect(() => {
  if (autoScroll && consoleRef.current) {
    consoleRef.current.scrollTop = consoleRef.current.scrollHeight;
  }
}, [messages, autoScroll]);

const handleCopyConsole = () => {
  const text = messages.join('\n');
  navigator.clipboard.writeText(text);
  // Show feedback (will add toast in Phase 4)
  alert('Console copied to clipboard!');
};

const handleClearConsole = () => {
  setMessages([]);
};

// JSX for console controls:
<div className={styles.consoleHeader}>
  <span className={styles.consoleTitle}>Console Output</span>
  <div className={styles.consoleControls}>
    <button
      className={styles.controlButton}
      onClick={() => setAutoScroll(!autoScroll)}
      title={autoScroll ? 'Disable auto-scroll' : 'Enable auto-scroll'}
    >
      {autoScroll ? '⏸️' : '▶️'}
    </button>
    <button
      className={styles.controlButton}
      onClick={handleCopyConsole}
      title="Copy console output"
    >
      📋
    </button>
    <button
      className={styles.controlButton}
      onClick={handleClearConsole}
      title="Clear console"
    >
      🗑️
    </button>
  </div>
</div>
```

**CSS**:
```css
.consoleHeader {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid var(--border-color);
}

.consoleTitle {
  font-weight: 600;
  color: var(--text-primary);
}

.consoleControls {
  display: flex;
  gap: 0.5rem;
}

.controlButton {
  background: transparent;
  border: 1px solid var(--border-color);
  border-radius: 0.25rem;
  padding: 0.25rem 0.5rem;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.2s;
}

.controlButton:hover {
  background: var(--bg-secondary);
  border-color: var(--primary-color);
}
```

**Test**: Console controls work correctly

**Estimated Time**: 40 minutes

---

**Phase 2 Checkpoint**: Commit changes with message "feat: enhance console with message parsing and controls"

---

## Phase 3: Input Enhancements (1.5-2 hours)

### Task 3.1: Create Validator Utilities
**New File**: `src/utils/validators.js`

**Code**:
```javascript
export const validateJSON = (text) => {
  if (!text.trim()) {
    return { valid: true, error: null };
  }
  try {
    JSON.parse(text);
    return { valid: true, error: null };
  } catch (e) {
    return { valid: false, error: e.message };
  }
};

export const formatJSON = (text) => {
  try {
    const parsed = JSON.parse(text);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return text;
  }
};
```

**Test**: Test with valid and invalid JSON

**Estimated Time**: 10 minutes

---

### Task 3.2: Add Payload Validation State
**File**: `src/components/JsonPathPlayground.jsx`

**Actions**:
1. Import validators
2. Add validation state
3. Add validation effect

**Code**:
```jsx
import { validateJSON, formatJSON } from '../utils/validators';

const [payloadValidation, setPayloadValidation] = useState({ valid: true, error: null });

// Add effect to validate on payload change
useEffect(() => {
  if (payload) {
    const validation = validateJSON(payload);
    setPayloadValidation(validation);
  } else {
    setPayloadValidation({ valid: true, error: null });
  }
}, [payload]);

const handleFormatPayload = () => {
  const formatted = formatJSON(payload);
  setPayload(formatted);
};
```

**Test**: Validation updates on input change

**Estimated Time**: 15 minutes

---

### Task 3.3: Add Validation UI Indicators
**File**: `src/components/JsonPathPlayground.jsx`

**Actions**:
1. Add validation icon next to textarea
2. Add character counter
3. Add format button

**JSX**:
```jsx
<div className={styles.inputGroup}>
  <div className={styles.labelRow}>
    <label htmlFor="payload" className={styles.label}>JSON/XML Payload</label>
    <div className={styles.payloadControls}>
      <span className={styles.charCounter}>
        {payload.length} / {MAX_BUFFER}
      </span>
      {payload && (
        <span className={styles.validationIcon}>
          {payloadValidation.valid ? '✅' : '❌'}
        </span>
      )}
      <button
        className={styles.formatButton}
        onClick={handleFormatPayload}
        disabled={!payload || !payloadValidation.valid}
      >
        Format
      </button>
    </div>
  </div>
  <textarea
    id="payload"
    className={`${styles.textarea} ${!payloadValidation.valid ? styles.textareaError : ''}`}
    rows="8"
    placeholder="Paste your JSON/XML payload here"
    value={payload}
    onChange={(e) => setPayload(e.target.value)}
  />
  {!payloadValidation.valid && (
    <div className={styles.errorMessage}>{payloadValidation.error}</div>
  )}
</div>
```

**CSS**:
```css
.labelRow {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.payloadControls {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.charCounter {
  font-size: 0.875rem;
  color: var(--text-secondary);
  font-family: monospace;
}

.validationIcon {
  font-size: 1rem;
}

.formatButton {
  padding: 0.25rem 0.75rem;
  font-size: 0.875rem;
  background: var(--primary-color);
  color: white;
  border: none;
  border-radius: 0.25rem;
  cursor: pointer;
  transition: background 0.2s;
}

.formatButton:hover:not(:disabled) {
  background: var(--primary-hover);
}

.formatButton:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.textareaError {
  border-color: var(--danger-color);
}

.errorMessage {
  color: var(--danger-color);
  font-size: 0.875rem;
  margin-top: 0.5rem;
}
```

**Test**: Validation indicators display correctly

**Estimated Time**: 30 minutes

---

### Task 3.4: Add Sample Data Quick Load
**File**: `src/components/JsonPathPlayground.jsx`

**Actions**:
1. Define sample data constant
2. Create SampleButtons component
3. Add to UI

**Code**:
```jsx
const SAMPLE_DATA = {
  simple: JSON.stringify({ name: "John Doe", age: 30, city: "New York" }, null, 2),
  nested: JSON.stringify({
    user: {
      name: "Jane Smith",
      profile: {
        email: "jane@example.com",
        address: { city: "San Francisco", country: "USA" }
      }
    }
  }, null, 2),
  array: JSON.stringify([
    { id: 1, name: "Item 1", status: "active" },
    { id: 2, name: "Item 2", status: "pending" },
    { id: 3, name: "Item 3", status: "inactive" }
  ], null, 2)
};

const SampleButtons = ({ onLoad }) => (
  <div className={styles.sampleButtons}>
    <span className={styles.sampleLabel}>Quick load:</span>
    {Object.keys(SAMPLE_DATA).map(key => (
      <button
        key={key}
        className={styles.sampleButton}
        onClick={() => onLoad(SAMPLE_DATA[key])}
      >
        {key}
      </button>
    ))}
  </div>
);

// Add below textarea:
<SampleButtons onLoad={setPayload} />
```

**CSS**:
```css
.sampleButtons {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  flex-wrap: wrap;
}

.sampleLabel {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.sampleButton {
  padding: 0.25rem 0.75rem;
  font-size: 0.75rem;
  background: var(--bg-secondary);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  border-radius: 0.25rem;
  cursor: pointer;
  transition: all 0.2s;
}

.sampleButton:hover {
  background: var(--primary-color);
  color: white;
  border-color: var(--primary-color);
}
```

**Test**: Sample buttons load data correctly

**Estimated Time**: 25 minutes

---

**Phase 3 Checkpoint**: Commit changes with message "feat: add input validation and sample data loading"

---

## Phase 4: Toast Notifications (1 hour)

### Task 4.1: Create Toast Hook
**New File**: `src/hooks/useToast.js`

**Code**:
```javascript
import { useState } from 'react';

export const useToast = () => {
  const [toasts, setToasts] = useState([]);

  const addToast = (message, type = 'info') => {
    const id = Date.now() + Math.random();
    setToasts(prev => [...prev, { id, message, type }]);
    
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 3000);
  };

  const removeToast = (id) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  };

  return { toasts, addToast, removeToast };
};
```

**Test**: Import and test hook

**Estimated Time**: 10 minutes

---

### Task 4.2: Create Toast Component
**New File**: `src/components/Toast.jsx`

**Code**:
```jsx
import styles from './Toast.module.css';

export const ToastContainer = ({ toasts, onRemove }) => {
  if (toasts.length === 0) return null;

  return (
    <div className={styles.toastContainer}>
      {toasts.map(toast => (
        <div
          key={toast.id}
          className={`${styles.toast} ${styles[toast.type]}`}
          onClick={() => onRemove(toast.id)}
        >
          <span className={styles.toastIcon}>
            {toast.type === 'success' && '✅'}
            {toast.type === 'error' && '❌'}
            {toast.type === 'info' && 'ℹ️'}
          </span>
          <span className={styles.toastMessage}>{toast.message}</span>
        </div>
      ))}
    </div>
  );
};
```

**Test**: Component renders toasts

**Estimated Time**: 15 minutes

---

### Task 4.3: Create Toast Styles
**New File**: `src/components/Toast.module.css`

**Code**:
```css
.toastContainer {
  position: fixed;
  top: 1rem;
  right: 1rem;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-width: 400px;
}

.toast {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  background: white;
  border-radius: var(--radius);
  box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1);
  cursor: pointer;
  animation: slideIn 0.3s ease;
  border-left: 4px solid;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.toast.success {
  border-left-color: var(--success-color);
}

.toast.error {
  border-left-color: var(--danger-color);
}

.toast.info {
  border-left-color: var(--primary-color);
}

.toastIcon {
  font-size: 1.25rem;
  flex-shrink: 0;
}

.toastMessage {
  color: var(--text-primary);
  font-size: 0.875rem;
  flex: 1;
}

@media (max-width: 768px) {
  .toastContainer {
    left: 1rem;
    right: 1rem;
    max-width: none;
  }
}
```

**Test**: Toast animations work

**Estimated Time**: 15 minutes

---

### Task 4.4: Integrate Toast into Main Component
**File**: `src/components/JsonPathPlayground.jsx`

**Actions**:
1. Import useToast hook and ToastContainer
2. Replace alert() calls with toast notifications
3. Update user feedback for actions

**Code**:
```jsx
import { useToast } from '../hooks/useToast';
import { ToastContainer } from './Toast';

// In component:
const { toasts, addToast, removeToast } = useToast();

// Replace alert in handleCopyConsole:
const handleCopyConsole = () => {
  const text = messages.join('\n');
  navigator.clipboard.writeText(text);
  addToast('Console copied to clipboard!', 'success');
};

// Add toast for connection events:
ws.onopen = () => {
  addMessage(eventWithTimestamp("info", "connected"));
  setConnected(true);
  addToast('Connected to WebSocket', 'success');
  ws.send(JSON.stringify({ type: 'welcome' }));
  pingIntervalRef.current = setInterval(keepAlive, PING_INTERVAL);
};

ws.onclose = (evt) => {
  setConnected(false);
  if (pingIntervalRef.current) clearInterval(pingIntervalRef.current);
  addMessage(eventWithTimestamp("info", `disconnected - (${evt.code}) ${evt.reason}`));
  addToast('Disconnected from WebSocket', 'info');
  wsRef.current = null;
};

// Add to JSX:
<ToastContainer toasts={toasts} onRemove={removeToast} />
```

**Test**: Toasts appear for user actions

**Estimated Time**: 20 minutes

---

**Phase 4 Checkpoint**: Commit changes with message "feat: add toast notification system"

---

## Phase 5: LocalStorage Persistence (45 minutes)

### Task 5.1: Create LocalStorage Hook
**New File**: `src/hooks/useLocalStorage.js`

**Code**:
```javascript
import { useState, useEffect } from 'react';

export const useLocalStorage = (key, initialValue) => {
  const [value, setValue] = useState(() => {
    try {
      const item = window.localStorage.getItem(key);
      return item ? JSON.parse(item) : initialValue;
    } catch (error) {
      console.error(`Error reading localStorage key "${key}":`, error);
      return initialValue;
    }
  });

  useEffect(() => {
    try {
      window.localStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
      console.error(`Error setting localStorage key "${key}":`, error);
    }
  }, [key, value]);

  return [value, setValue];
};
```

**Test**: Hook persists data across page reloads

**Estimated Time**: 15 minutes

---

### Task 5.2: Integrate LocalStorage for History
**File**: `src/components/JsonPathPlayground.jsx`

**Actions**:
1. Import useLocalStorage
2. Replace useState for history with useLocalStorage
3. Test persistence

**Code**:
```jsx
import { useLocalStorage } from '../hooks/useLocalStorage';

// Replace:
// const [history, setHistory] = useState([]);
// With:
const [history, setHistory] = useLocalStorage('jsonpath-command-history', []);
```

**Test**: Command history persists across reloads

**Estimated Time**: 10 minutes

---

### Task 5.3: Integrate LocalStorage for Last Payload
**File**: `src/components/JsonPathPlayground.jsx`

**Actions**:
1. Use useLocalStorage for payload initial value
2. Update payload setter

**Code**:
```jsx
// Replace:
// const [payload, setPayload] = useState('');
// With:
const [payload, setPayload] = useLocalStorage('jsonpath-last-payload', '');
```

**Test**: Last payload persists across reloads

**Estimated Time**: 10 minutes

---

### Task 5.4: Add Clear Storage Option
**File**: `src/components/JsonPathPlayground.jsx`

**Actions**:
1. Add function to clear localStorage
2. Add button in settings or help section

**Code**:
```jsx
const handleClearStorage = () => {
  if (confirm('Clear all saved data (history and last payload)?')) {
    localStorage.removeItem('jsonpath-command-history');
    localStorage.removeItem('jsonpath-last-payload');
    setHistory([]);
    setPayload('');
    addToast('Storage cleared', 'info');
  }
};

// Add button somewhere appropriate (maybe in a settings dropdown):
<button className={styles.link} onClick={handleClearStorage}>
  Clear saved data
</button>
```

**Test**: Clear storage works correctly

**Estimated Time**: 10 minutes

---

**Phase 5 Checkpoint**: Commit changes with message "feat: add localStorage persistence for history and payload"

---

## Phase 6: Polish & Refinements (1 hour)

### Task 6.1: Add Loading States
**File**: `src/components/JsonPathPlayground.jsx`

**Actions**:
1. Add connecting state
2. Show loading indicator on connect button

**Code**:
```jsx
const [connecting, setConnecting] = useState(false);

const connectToEdge = () => {
  if (!window.WebSocket) {
    addToast('WebSocket not supported by your browser', 'error');
    return;
  }

  if (connected) {
    addToast('Already connected', 'error');
    return;
  }

  setConnecting(true);
  setShowConsole(true);
  const ws = new WebSocket(wsUrl.current);
  wsRef.current = ws;

  ws.onopen = () => {
    setConnecting(false);
    addMessage(eventWithTimestamp("info", "connected"));
    setConnected(true);
    addToast('Connected to WebSocket', 'success');
    ws.send(JSON.stringify({ type: 'welcome' }));
    pingIntervalRef.current = setInterval(keepAlive, PING_INTERVAL);
  };

  ws.onerror = () => {
    setConnecting(false);
  };

  // Update button:
  <button
    className={`${styles.button} ${styles.buttonPrimary}`}
    onClick={connectToEdge}
    disabled={connecting}
  >
    {connecting ? 'Connecting...' : 'Start'}
  </button>
};
```

**Test**: Loading state shows correctly

**Estimated Time**: 15 minutes

---

### Task 6.2: Improve Accessibility
**File**: `src/components/JsonPathPlayground.jsx`

**Actions**:
1. Add ARIA labels to all buttons
2. Add role attributes where needed
3. Ensure focus indicators are visible

**Code Updates**:
```jsx
<button
  className={styles.controlButton}
  onClick={handleCopyConsole}
  title="Copy console output"
  aria-label="Copy console output to clipboard"
>
  📋
</button>

<div className={styles.console} ref={consoleRef} role="log" aria-live="polite">
  {/* messages */}
</div>
```

**CSS**:
```css
*:focus-visible {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
}
```

**Test**: Keyboard navigation works throughout app

**Estimated Time**: 20 minutes

---

### Task 6.3: Add Help/Documentation Section
**File**: `src/components/JsonPathPlayground.jsx`

**Actions**:
1. Create collapsible help section
2. Add command reference
3. Add keyboard shortcuts

**Code**:
```jsx
const [showHelp, setShowHelp] = useState(false);

const HelpSection = () => (
  <div className={styles.helpSection}>
    <h3>Available Commands</h3>
    <ul>
      <li><code>load</code> - Load the JSON/XML payload from the textarea</li>
      <li><code>help</code> - Show available commands</li>
      <li><code>$.path.to.field</code> - JSON-Path expression to query data</li>
    </ul>
    <h3>Keyboard Shortcuts</h3>
    <ul>
      <li><kbd>↑</kbd> <kbd>↓</kbd> - Navigate command history</li>
      <li><kbd>Enter</kbd> - Send command</li>
    </ul>
  </div>
);

// Add toggle button in header:
<button className={styles.link} onClick={() => setShowHelp(!showHelp)}>
  {showHelp ? 'Hide Help' : 'Show Help'}
</button>

{showHelp && <HelpSection />}
```

**Test**: Help section displays correctly

**Estimated Time**: 25 minutes

---

**Phase 6 Checkpoint**: Commit changes with message "feat: add loading states, accessibility improvements, and help section"

---

## Final Steps

### Task 7.1: Responsive Testing
**Actions**:
1. Test on mobile viewport (< 768px)
2. Test on tablet viewport (768px - 1024px)
3. Test on desktop viewport (> 1024px)
4. Fix any responsive issues

**Estimated Time**: 20 minutes

---

### Task 7.2: Cross-Browser Testing
**Actions**:
1. Test in Chrome
2. Test in Firefox
3. Test in Safari
4. Fix any compatibility issues

**Estimated Time**: 15 minutes

---

### Task 7.3: Performance Check
**Actions**:
1. Check bundle size with `npm run build`
2. Test with large message volumes (> 100 messages)
3. Verify no memory leaks (DevTools Performance tab)

**Estimated Time**: 15 minutes

---

### Task 7.4: Code Cleanup
**Actions**:
1. Remove console.log statements
2. Remove unused imports
3. Ensure consistent formatting
4. Add JSDoc comments to utility functions

**Estimated Time**: 20 minutes

---

### Task 7.5: Final Documentation
**Actions**:
1. Update README.md with new features
2. Add screenshots (optional)
3. Document any environment variables or configuration

**Estimated Time**: 30 minutes

---

**Final Checkpoint**: Commit all changes with message "chore: final polish and documentation"

---

## Total Time Estimate
- Phase 1: 2-3 hours
- Phase 2: 2-3 hours
- Phase 3: 1.5-2 hours
- Phase 4: 1 hour
- Phase 5: 45 minutes
- Phase 6: 1 hour
- Final Steps: 1.5 hours

**Total: 10-13 hours**

---

## Success Criteria
- [ ] All phases completed
- [ ] No console errors
- [ ] Responsive on all screen sizes
- [ ] Works in Chrome, Firefox, Safari
- [ ] Accessibility standards met
- [ ] LocalStorage persistence works
- [ ] Toast notifications appear appropriately
- [ ] Console messages are properly formatted
- [ ] Input validation works correctly
- [ ] All existing functionality preserved

---

## Rollback Plan
If issues arise:
1. Each phase is committed separately
2. Can revert to previous checkpoint with `git reset --hard <commit-hash>`
3. Feature branch can be abandoned if needed

---

*Created: February 10, 2026*
*Ready for implementation*
