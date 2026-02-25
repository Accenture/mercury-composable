import { NavLink } from 'react-router-dom';
import { PLAYGROUND_CONFIGS } from '../config/playgrounds';
import { useWebSocketContext } from '../contexts/WebSocketContext';
import { useToast } from '../hooks/useToast';
import ConnectionBar from './ConnectionBar/ConnectionBar';
import styles from './Navigation.module.css';

export default function Navigation() {
  // Tool links are derived from config — add a new playground in config/playgrounds.js,
  // not here.
  const toolLinks = PLAYGROUND_CONFIGS.map((cfg) => ({ to: cfg.path, label: cfg.label }));

  const externalLinks = [
    { href: '/info', label: 'INFO' },
    { href: '/info/lib', label: 'LIBRARIES' },
    { href: '/info/routes', label: 'SERVICES' },
    { href: '/health', label: 'HEALTH' },
    { href: '/env', label: 'ENVIRONMENT' },
  ];

  // Shared WebSocket context — lets us show the live status of *every* playground,
  // not just the currently-active route.
  const ctx = useWebSocketContext();
  const { addToast } = useToast();

  const wsUrl = (wsPath: string) =>
    import.meta.env.DEV
      ? `ws://localhost:3000${wsPath}`
      : `ws://${window.location.host}${wsPath}`;

  return (
    <nav className={styles.nav}>
      {/* One connection bar per playground — all visible simultaneously */}
      <div className={styles.connectionSection}>
        {PLAYGROUND_CONFIGS.map((cfg) => {
          const { phase } = ctx.getSlot(cfg.wsPath);
          return (
            <ConnectionBar
              key={cfg.wsPath}
              label={cfg.label}
              connected={phase === 'connected'}
              connecting={phase === 'connecting'}
              url={wsUrl(cfg.wsPath)}
              onConnect={() => ctx.connect(cfg.wsPath, addToast)}
              onDisconnect={() => ctx.disconnect(cfg.wsPath)}
            />
          );
        })}
      </div>
      <div className={styles.navSection}>
        <div className={styles.navLabel}>Tools:</div>
        <div className={styles.navLinks}>
          {toolLinks.map(link => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`
              }
            >
              <span className={styles.navText}>{link.label}</span>
            </NavLink>
          ))}
        </div>
      </div>
      <div className={styles.navSection}>
        <div className={styles.navLabel}>Quick Links:</div>
        <div className={styles.navLinks}>
          {externalLinks.map(link => (
            <a
              key={link.href}
              href={link.href}
              className={styles.navLink}
              target="_blank"
              rel="noopener noreferrer"
            >
              <span className={styles.navText}>{link.label}</span>
            </a>
          ))}
        </div>
      </div>
    </nav>
  );
}
