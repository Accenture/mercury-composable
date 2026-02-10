# JSON-Path Playground - UI Enhancement Specification

## Project Overview
Enhance the existing React-based JSON-Path Playground with a polished, user-friendly interface while maintaining minimal complexity.

## Design Principles
- **Minimal Complexity**: Use existing React features, avoid unnecessary libraries
- **Progressive Enhancement**: Build on current structure without major rewrites
- **Maintainability**: Keep components simple and focused
- **Accessibility**: Ensure keyboard navigation and screen reader support

---

## Phase 1: Core Visual Improvements (Priority: HIGH)

### 1.1 Modern Layout & Card Design
**Files to modify**: `JsonPathPlayground.module.css`

**Changes**:
- Implement card-based sections with subtle shadows
- Add a two-column grid layout (input/controls left, console right on desktop)
- Responsive: stack vertically on mobile (< 768px)

**CSS Variables to add**:
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

### 1.2 Connection Status Enhancement
**Component**: `JsonPathPlayground.jsx`

**Implementation**:
```jsx
// Add new component
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
```

**CSS additions**:
- Animated pulse effect for connected status dot
- Green (#10b981) for connected, Red (#ef4444) for disconnected

---

## Phase 2: Enhanced Console (Priority: HIGH)

### 2.1 Message Type Parsing & Styling
**Component**: `JsonPathPlayground.jsx`

**New utility function**:
```javascript
const parseMessage = (msg) => {
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
```

**New component**:
```jsx
const ConsoleMessage = ({ message }) => {
  const parsed = parseMessage(message);
  const icon = {
    info: 'ℹ️',
    error: '❌',
    ping: '🔄',
    welcome: '👋',
    raw: '📝'
  }[parsed.type] || '•';

  return (
    <div className={`${styles.consoleMessage} ${styles[`type-${parsed.type}`]}`}>
      <span className={styles.messageIcon}>{icon}</span>
      <span className={styles.messageContent}>{parsed.message}</span>
      {parsed.time && (
        <span className={styles.messageTime}>{parsed.time}</span>
      )}
    </div>
  );
};
```

### 2.2 Console Controls
**Features**:
- Auto-scroll toggle button
- Clear console button
- Copy all button
- Scroll to bottom button (appears when not at bottom)

**State additions**:
```javascript
const [autoScroll, setAutoScroll] = useState(true);
const consoleRef = useRef(null);
```

---

## Phase 3: Input Enhancements (Priority: MEDIUM)

### 3.1 JSON/XML Validation
**New utility functions**:
```javascript
const validateJSON = (text) => {
  try {
    JSON.parse(text);
    return { valid: true, error: null };
  } catch (e) {
    return { valid: false, error: e.message };
  }
};

const formatJSON = (text) => {
  try {
    const parsed = JSON.parse(text);
    return JSON.stringify(parsed, null, 2);
  } catch {
    return text;
  }
};
```

**UI additions**:
- Validation indicator (checkmark/x icon) next to textarea
- "Format" button to prettify JSON
- Character counter showing `{current}/{MAX_BUFFER}`

### 3.2 Sample Data Quick Load
**Component addition**:
```jsx
const SAMPLE_DATA = {
  simple: '{"name": "John", "age": 30}',
  nested: '{"user": {"name": "Jane", "address": {"city": "NYC"}}}',
  array: '[{"id": 1}, {"id": 2}, {"id": 3}]'
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
```

---

## Phase 4: Better Feedback & UX (Priority: MEDIUM)

### 4.1 Toast Notifications
**New component**: `Toast.jsx`

```jsx
import { useState, useEffect } from 'react';
import styles from './Toast.module.css';

export const useToast = () => {
  const [toasts, setToasts] = useState([]);

  const addToast = (message, type = 'info') => {
    const id = Date.now();
    setToasts(prev => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== id));
    }, 3000);
  };

  return { toasts, addToast };
};

export const ToastContainer = ({ toasts }) => (
  <div className={styles.toastContainer}>
    {toasts.map(toast => (
      <div key={toast.id} className={`${styles.toast} ${styles[toast.type]}`}>
        {toast.message}
      </div>
    ))}
  </div>
);
```

**Usage**: Replace inline `addMessage` calls for user actions with toast notifications

### 4.2 Loading States
**State addition**:
```javascript
const [connecting, setConnecting] = useState(false);
```

**Implementation**:
- Show spinner on "Start" button while connecting
- Disable button during connection attempt

---

## Phase 5: Command History UI (Priority: LOW)

### 5.1 Visual History Selector
**Component**:
```jsx
const CommandHistory = ({ history, onSelect, visible }) => {
  if (!visible || history.length === 0) return null;
  
  return (
    <div className={styles.historyDropdown}>
      <div className={styles.historyHeader}>Recent Commands</div>
      {history.slice(0, 10).map((cmd, idx) => (
        <button
          key={idx}
          className={styles.historyItem}
          onClick={() => onSelect(cmd)}
        >
          {cmd}
        </button>
      ))}
    </div>
  );
};
```

**Trigger**: Show on Ctrl+Space or click on history icon

---

## Phase 6: Persistence & Session Management (Priority: LOW)

### 6.1 LocalStorage Integration
**New hook**: `useLocalStorage.js`

```javascript
import { useState, useEffect } from 'react';

export const useLocalStorage = (key, initialValue) => {
  const [value, setValue] = useState(() => {
    try {
      const item = window.localStorage.getItem(key);
      return item ? JSON.parse(item) : initialValue;
    } catch {
      return initialValue;
    }
  });

  useEffect(() => {
    try {
      window.localStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
      console.error('Error saving to localStorage:', error);
    }
  }, [key, value]);

  return [value, setValue];
};
```

**Usage**:
```javascript
const [history, setHistory] = useLocalStorage('jsonpath-history', []);
const [lastPayload, setLastPayload] = useLocalStorage('jsonpath-payload', '');
```

---

## Phase 7: Custom Hooks for Clean Architecture (Priority: MEDIUM)

### 7.1 WebSocket Hook
**New file**: `hooks/useWebSocket.js`

```javascript
import { useState, useEffect, useRef, useCallback } from 'react';

export const useWebSocket = (url) => {
  const [connected, setConnected] = useState(false);
  const [messages, setMessages] = useState([]);
  const wsRef = useRef(null);
  const pingIntervalRef = useRef(null);

  const connect = useCallback(() => {
    // Connection logic extracted from component
  }, [url]);

  const disconnect = useCallback(() => {
    // Disconnection logic
  }, []);

  const send = useCallback((message) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(message);
    }
  }, []);

  useEffect(() => {
    return () => {
      disconnect();
    };
  }, [disconnect]);

  return { connected, messages, connect, disconnect, send };
};
```

---

## Component Structure (Final)

```
src/
├── components/
│   ├── JsonPathPlayground.jsx          (Main container)
│   ├── JsonPathPlayground.module.css
│   ├── ConnectionStatus.jsx            (Status indicator)
│   ├── ConnectionStatus.module.css
│   ├── ConsolePanel.jsx                (Console output)
│   ├── ConsolePanel.module.css
│   ├── ConsoleMessage.jsx              (Individual message)
│   ├── InputPanel.jsx                  (Command & payload inputs)
│   ├── InputPanel.module.css
│   ├── Toast.jsx                       (Notifications)
│   └── Toast.module.css
├── hooks/
│   ├── useWebSocket.js                 (WebSocket logic)
│   ├── useToast.js                     (Toast notifications)
│   └── useLocalStorage.js              (Persistence)
├── utils/
│   ├── messageParser.js                (Parse message types)
│   ├── validators.js                   (JSON/XML validation)
│   └── formatters.js                   (Format JSON/XML)
└── App.jsx
```

---

## CSS Architecture

### Global Tokens
All components use CSS variables defined in `index.css` for consistency.

### Module Pattern
Each component has its own `.module.css` for scoped styles.

### Responsive Breakpoints
- Mobile: < 768px
- Tablet: 768px - 1024px
- Desktop: > 1024px

---

## Accessibility Checklist

- [ ] All interactive elements keyboard accessible
- [ ] Focus indicators visible
- [ ] ARIA labels on all buttons
- [ ] Status updates announced to screen readers
- [ ] Color contrast ratio meets WCAG AA standards
- [ ] Skip to console/Skip to input links

---

## Performance Optimizations

### Implemented
1. **useCallback** for event handlers
2. **useMemo** for computed values (filtered messages)
3. **Virtual scrolling** for large message lists (if > 1000 messages)
4. **Debounced validation** for payload input (300ms)

### Code Example
```javascript
const filteredMessages = useMemo(() => {
  return messages.filter(msg => {
    // Filter logic
  });
}, [messages, filterType]);

const handlePayloadChange = useCallback(
  debounce((value) => {
    setPayload(value);
    validateJSON(value);
  }, 300),
  []
);
```

---

## Testing Considerations

### Manual Testing Checklist
- [ ] Connect/Disconnect flow works
- [ ] Command history navigation (up/down arrows)
- [ ] Payload validation displays correctly
- [ ] Console auto-scroll works
- [ ] Toast notifications appear and dismiss
- [ ] LocalStorage persists across refreshes
- [ ] Responsive layout on mobile
- [ ] Keyboard navigation throughout

### Error Scenarios
- [ ] WebSocket connection fails
- [ ] Invalid JSON in payload
- [ ] Payload exceeds MAX_BUFFER
- [ ] Network disconnection while connected

---

## Implementation Order (Recommended)

1. **Phase 1**: Visual improvements & layout (foundation)
2. **Phase 2**: Enhanced console (core feature)
3. **Phase 7**: Extract WebSocket hook (architecture cleanup)
4. **Phase 3**: Input enhancements (usability)
5. **Phase 4**: Toast notifications (feedback)
6. **Phase 6**: LocalStorage (persistence)
7. **Phase 5**: Command history UI (nice-to-have)

---

## Dependencies

### Current
- React 18+
- Vite

### To Add (Optional)
None required - using vanilla React and CSS

---

## Browser Support
- Chrome/Edge: Last 2 versions
- Firefox: Last 2 versions
- Safari: Last 2 versions
- Mobile Safari/Chrome: Last 2 versions

---

## Migration Notes

### Breaking Changes
None - all changes are additive and backward compatible

### State Management
No external library needed - React hooks sufficient for this complexity level

### Backward Compatibility
- Existing WebSocket protocol unchanged
- All current features preserved
- Console message format remains compatible

---

## Future Enhancements (Out of Scope)

- Command autocomplete with AI suggestions
- Export console to CSV/JSON file
- Syntax highlighting in console (would require library)
- Dark mode (adds complexity, defer to v2)
- Multi-tab support for different connections
- Command templates/snippets manager

---

## File Size Impact Estimate

- Current bundle: ~50KB
- Estimated after changes: ~65KB (30% increase)
- Acceptable for feature set added

---

## Notes for Future Developers

1. **Console Performance**: If message count exceeds 1000, consider implementing virtual scrolling using `react-window`
2. **Theme System**: CSS variables are already set up for easy dark mode addition later
3. **i18n Ready**: String literals can be extracted to a constants file for internationalization
4. **API Evolution**: WebSocket hook designed to support multiple connections if needed in future

---

*Last Updated: February 10, 2026*
*Version: 1.0*
