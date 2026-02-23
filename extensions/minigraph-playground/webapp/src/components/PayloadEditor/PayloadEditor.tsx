import SampleButtons from './SampleButtons';
import styles from './PayloadEditor.module.css';
import { type ValidationResult } from '../../utils/validators';
import { MAX_BUFFER } from '../../config/playgrounds';

interface PayloadEditorProps {
  payload:    string;
  onChange:   (value: string) => void;
  validation: ValidationResult;
  onFormat:   () => void;
}

export default function PayloadEditor({ payload, onChange, validation, onFormat }: PayloadEditorProps) {
  return (
    <div className={styles.card}>
      <div className={styles.inputGroup}>
        <div className={styles.labelRow}>
          <label htmlFor="payload" className={styles.label}>JSON/XML Payload</label>
          <div className={styles.payloadControls}>
            <span className={styles.charCounter}>{payload.length} / {MAX_BUFFER}</span>
            {payload && validation.type && (
              <span className={styles.typeIndicator}>{validation.type.toUpperCase()}</span>
            )}
            {payload && (
              <span className={styles.validationIcon}>{validation.valid ? '✅' : '❌'}</span>
            )}
            <button
              className={styles.formatButton}
              onClick={onFormat}
              disabled={!payload || validation.type !== 'json'}
              title={validation.type === 'xml' ? 'Format only available for JSON' : 'Format JSON'}
            >
              Format
            </button>
          </div>
        </div>

        <textarea
          id="payload"
          className={`${styles.textarea} ${!validation.valid ? styles.textareaError : ''}`}
          rows={8}
          placeholder="Paste your JSON/XML payload here"
          value={payload}
          onChange={(e) => onChange(e.target.value)}
        />

        {!validation.valid && (
          <div className={styles.errorMessage}>{validation.error}</div>
        )}

        <SampleButtons onLoad={onChange} />
      </div>
    </div>
  );
}
