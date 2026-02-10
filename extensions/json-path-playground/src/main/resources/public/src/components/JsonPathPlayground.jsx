import { useState, useEffect, useRef } from 'react';
import styles from './JsonPathPlayground.module.css';

const MAX_ITEMS = 30;
const MAX_BUFFER = 64000;
const PING_INTERVAL = 30000;

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
  const [payload, setPayload] = useState('');
  
  // Command history state
  const [history, setHistory] = useState([]);
  const [historyIndex, setHistoryIndex] = useState(-1);

  // --- Mutable References ---
  const wsRef = useRef(null);
  const pingIntervalRef = useRef(null);
  const wsUrl = useRef(getWebSocketURL());

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
      addMessage("WebSocket NOT supported by your Browser");
      return;
    }

    if (connected) {
      addMessage(eventWithTimestamp("error", "already connected"));
      return;
    }

    setShowConsole(true);
    const ws = new WebSocket(wsUrl.current);
    wsRef.current = ws;

    ws.onopen = () => {
      addMessage(eventWithTimestamp("info", "connected"));
      setConnected(true);
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
              <label htmlFor="payload" className={styles.label}>JSON/XML</label>
              <textarea
                id="payload"
                className={styles.textarea}
                rows="8"
                placeholder="Paste your JSON/XML payload here"
                value={payload}
                onChange={(e) => setPayload(e.target.value)}
              />
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
              <pre className={styles.console}>
                {messages.join('\n')}
              </pre>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
