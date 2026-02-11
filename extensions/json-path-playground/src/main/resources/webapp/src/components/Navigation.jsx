import styles from './Navigation.module.css';

export default function Navigation() {
  const links = [
    { href: '/info', label: 'INFO' },
    { href: '/info/lib', label: 'LIBRARIES'},
    { href: '/info/routes', label: 'SERVICES'},
    { href: '/health', label: 'HEALTH'},
    { href: '/env', label: 'ENVIRONMENT'}
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
            {link.icon && <span className={styles.navIcon}>{link.icon}</span>}
            <span className={styles.navText}>{link.label}</span>
          </a>
        ))}
      </div>
    </nav>
  );
}
