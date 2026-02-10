import { useState, useEffect, useRef } from 'react';
import styles from './JsonPathPlayground.module.css';
import { parseMessage, getMessageIcon } from '../utils/messageParser';
import { validateJSON, formatJSON } from '../utils/validators';
import { useToast } from '../hooks/useToast';
import { useLocalStorage } from '../hooks/useLocalStorage';
import { ToastContainer } from './Toast';

const MAX_ITEMS = 30;
const MAX_BUFFER = 64000;
const PING_INTERVAL = 30000;

// Sample data for quick loading
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

// Determine WebSocket URL based on environment
const getWebSocketURL = () => {
  if (import.meta.env.DEV) {
    // Development mode - proxy will handle this
    return `ws://localhost:3000/ws/json/path`;
  }
  // Production - use the current host
  return `ws://${window.location.host}/ws/json/path`;
};

export default function JsonPathPlayground() {
  // --- UI & Application State ---
  const [messages, setMessages] = useState([]);
  const [connected, setConnected] = useState(false);
  const [showConsole, setShowConsole] = useState(false);
  
  // Input states
  const [command, setCommand] = useState('');
  const [payload, setPayload] = useLocalStorage('jsonpath-last-payload', '');
  const [payloadValidation, setPayloadValidation] = useState({ valid: true, error: null });
  
  // Command history state
  const [history, setHistory] = useLocalStorage('jsonpath-command-history', []);
  const [historyIndex, setHistoryIndex] = useState(-1);
  const [autoScroll, setAutoScroll] = useState(true);

  // Toast notifications
  const { toasts, addToast, removeToast } = useToast();

  // --- Mutable References ---
  const wsRef = useRef(null);
  const pingIntervalRef = useRef(null);
  const wsUrl = useRef(getWebSocketURL());
  const consoleRef = useRef(null);

  // --- Console Message Component ---
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

  // --- Connection Status Component ---
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

  // --- Cleanup on Unmount ---
  useEffect(() => {
    return () => {
      if (wsRef.current) wsRef.current.close();
      if (pingIntervalRef.current) clearInterval(pingIntervalRef.current);
    };
  }, []);

  // --- Auto-scroll Effect ---
  useEffect(() => {
    if (autoScroll && consoleRef.current) {
      consoleRef.current.scrollTop = consoleRef.current.scrollHeight;
    }
  }, [messages, autoScroll]);

  // --- Payload Validation Effect ---
  useEffect(() => {
    if (payload) {
      const validation = validateJSON(payload);
      setPayloadValidation(validation);
    } else {
      setPayloadValidation({ valid: true, error: null });
    }
  }, [payload]);

  // --- Helper Functions ---
  const getTimestamp = () => {
    const s = new Date().toString();
    const gmt = s.indexOf('GMT');
    return gmt > 0 ? s.substring(0, gmt).trim() : s;
  };

  const eventWithTimestamp = (type, message) => {
    return JSON.stringify({ type, message, time: getTimestamp() });
  };

  const addMessage = (newMsg) => {
    setMessages((prev) => {
      const updated = [newMsg, ...prev];
      if (updated.length > MAX_ITEMS) updated.pop();
      return updated;
    });
  };

  const keepAlive = () => {
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(eventWithTimestamp("ping", "keep alive"));
    }
  };

  // --- Connection Logic ---
  const connectToEdge = () => {
    if (!window.WebSocket) {
      addToast('WebSocket not supported by your browser', 'error');
      return;
    }

    if (connected) {
      addToast('Already connected', 'error');
      return;
    }

    setShowConsole(true);
    const ws = new WebSocket(wsUrl.current);
    wsRef.current = ws;

    ws.onopen = () => {
      addMessage(eventWithTimestamp("info", "connected"));
      setConnected(true);
      addToast('Connected to WebSocket', 'success');
      ws.send(JSON.stringify({ type: 'welcome' }));
      pingIntervalRef.current = setInterval(keepAlive, PING_INTERVAL);
    };

    ws.onmessage = (evt) => {
      if (!evt.data.startsWith('{"type":"ping"')) {
        addMessage(evt.data);
      }
    };

    ws.onclose = (evt) => {
      setConnected(false);
      if (pingIntervalRef.current) clearInterval(pingIntervalRef.current);
      addMessage(eventWithTimestamp("info", `disconnected - (${evt.code}) ${evt.reason}`));
      addToast('Disconnected from WebSocket', 'info');
      wsRef.current = null;
    };
  };

  const disconnectFromEdge = () => {
    if (wsRef.current) {
      wsRef.current.close();
    } else {
      addMessage(eventWithTimestamp("error", "already disconnected"));
    }
  };

  // --- Console Control Handlers ---
  const handleCopyConsole = () => {
    const text = messages.join('\n');
    navigator.clipboard.writeText(text);
    addToast('Console copied to clipboard!', 'success');
  };

  const handleClearConsole = () => {
    setMessages([]);
    addToast('Console cleared', 'info');
  };

  const handleFormatPayload = () => {
    const formatted = formatJSON(payload);
    setPayload(formatted);
  };

  // --- Sample Buttons Component ---
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

  // --- Event Handlers ---
  const handleKeyDown = (e) => {
    if (!connected) return;

    if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (history.length > 0 && historyIndex < history.length - 1) {
        const newIndex = historyIndex + 1;
        setHistoryIndex(newIndex);
        setCommand(history[newIndex]);
      }
    } else if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (historyIndex > 0) {
        const newIndex = historyIndex - 1;
        setHistoryIndex(newIndex);
        setCommand(history[newIndex]);
      } else if (historyIndex === 0) {
        setHistoryIndex(-1);
        setCommand('');
      }
    } else if (e.key === 'Enter') {
      e.preventDefault();
      const text = command.trim();
      
      if (text.length > 0) {
        wsRef.current.send(text);
        
        // Update history
        setHistory((prev) => [text, ...prev].slice(0, MAX_ITEMS));
        setHistoryIndex(-1);

        // Handle special 'load' command
        if (text === 'load') {
          if (payload.length === 0) {
            addMessage("ERROR: please paste JSON/XML payload in input text area");
          } else if (payload.length > MAX_BUFFER) {
            addMessage(`ERROR: please reduce JSON/XML payload size. It must be less than ${MAX_BUFFER} characters`);
          } else {
            wsRef.current.send(payload);
          }
        }
      }
      setCommand(''); // Clear input after sending
    }
  };

  return (
    <div className={styles.wrapper}>
      <ToastContainer toasts={toasts} onRemove={removeToast} />
      
      <header className={styles.header}>
        <h1 className={styles.title}>JSON-Path Playground</h1>
        
        <nav className={styles.linkSection}>
          <a href="/info" className={styles.link}>INFO endpoint</a>
          <a href="/info/lib" className={styles.link}>Library dependency list</a>
          <a href="/info/routes" className={styles.link}>Service list</a>
          <a href="/health" className={styles.link}>Health endpoint</a>
          <a href="/env" className={styles.link}>Environment endpoint</a>
        </nav>
      </header>

      <div className={styles.container}>
        <div className={styles.leftPanel}>
          <ConnectionStatus connected={connected} url={wsUrl.current} />
          
          <div className={styles.card}>
            <div className={styles.inputGroup}>
              <label htmlFor="command" className={styles.label}>Command</label>
              <input
                id="command"
                type="text"
                className={styles.input}
                placeholder="Enter your test message once it is connected"
                value={command}
                onChange={(e) => setCommand(e.target.value)}
                onKeyDown={handleKeyDown}
                disabled={!connected}
              />
            </div>
          </div>
          
          <div className={styles.card}>
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
              <SampleButtons onLoad={setPayload} />
            </div>
          </div>
          
          <div className={styles.buttonGroup}>
            {!connected && (
              <button className={`${styles.button} ${styles.buttonPrimary}`} onClick={connectToEdge}>
                Start
              </button>
            )}
            {connected && (
              <button className={`${styles.button} ${styles.buttonWarning}`} onClick={disconnectFromEdge}>
                Stop Service
              </button>
            )}
            {showConsole && !connected && (
              <button className={`${styles.button} ${styles.buttonWarning}`} onClick={() => setShowConsole(false)}>
                Clear & Hide Console
              </button>
            )}
          </div>
        </div>

        <div className={styles.rightPanel}>
          {showConsole && (
            <div className={styles.card}>
              <div className={styles.consoleHeader}>
                <span className={styles.consoleTitle}>Console Output</span>
                <div className={styles.consoleControls}>
                  <button
                    className={styles.controlButton}
                    onClick={() => setAutoScroll(!autoScroll)}
                    title={autoScroll ? 'Disable auto-scroll' : 'Enable auto-scroll'}
                    aria-label={autoScroll ? 'Disable auto-scroll' : 'Enable auto-scroll'}
                  >
                    {autoScroll ? '⏸️' : '▶️'}
                  </button>
                  <button
                    className={styles.controlButton}
                    onClick={handleCopyConsole}
                    title="Copy console output"
                    aria-label="Copy console output to clipboard"
                  >
                    📋
                  </button>
                  <button
                    className={styles.controlButton}
                    onClick={handleClearConsole}
                    title="Clear console"
                    aria-label="Clear console"
                  >
                    🗑️
                  </button>
                </div>
              </div>
              <div className={styles.console} ref={consoleRef} role="log" aria-live="polite">
                {messages.map((msg, idx) => (
                  <ConsoleMessage key={idx} message={msg} />
                ))}
                {messages.length === 0 && (
                  <div className={styles.emptyConsole}>
                    No messages yet. Click "Start" to connect.
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
