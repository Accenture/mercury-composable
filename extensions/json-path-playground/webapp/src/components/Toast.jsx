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
