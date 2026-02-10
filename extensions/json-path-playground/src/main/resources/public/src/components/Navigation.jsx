import styles from './Navigation.module.css';

export default function Navigation() {
  const links = [
    { href: '/info', label: 'Info', icon: 'ℹ️' },
    { href: '/info/lib', label: 'Libraries', icon: '📚' },
    { href: '/info/routes', label: 'Services', icon: '🔀' },
    { href: '/health', label: 'Health', icon: '💚' },
    { href: '/env', label: 'Environment', icon: '⚙️' }
  ];

  return (
    <nav className={styles.nav}>
      <div className={styles.navLabel}>Quick Links:</div>
      <div className={styles.navLinks}>
        {links.map(link => (
          <a 
            key={link.href} 
            href={link.href} 
            className={styles.navLink}
            target="_blank"
            rel="noopener noreferrer"
          >
            <span className={styles.navIcon}>{link.icon}</span>
            <span className={styles.navText}>{link.label}</span>
          </a>
        ))}
      </div>
    </nav>
  );
}
