import { NavLink } from 'react-router-dom';
import { PLAYGROUND_CONFIGS } from '../config/playgrounds';
import styles from './Navigation.module.css';

interface NavigationProps {
  connectionBar?: React.ReactNode;
}

export default function Navigation({ connectionBar }: NavigationProps) {
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

  return (
    <nav className={styles.nav}>
      {connectionBar && (
        <div className={styles.connectionSection}>
          {connectionBar}
        </div>
      )}
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
