import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import styles from './MarkdownPreview.module.css';

interface MarkdownPreviewProps {
  message: string | null;
}

export default function MarkdownPreview({ message }: MarkdownPreviewProps) {
  return (
    <div className={styles.previewRoot}>
      <div className={styles.previewBody}>
        {message === null ? (
          <div className={styles.emptyPreview}>
            No preview yet. Send a 'help' or 'describe' command and/or click a plain-text message in the
            Console to view it here.
          </div>
        ) : (
          <div className={styles.markdownContent}>
            <ReactMarkdown remarkPlugins={[remarkGfm]}>
              {message}
            </ReactMarkdown>
          </div>
        )}
      </div>
    </div>
  );
}
