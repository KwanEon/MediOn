import { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { AlertCircle, X } from 'lucide-react';

interface ConfirmationModalProps {
  open: boolean;
  title: string;
  message: string;
  confirmLabel: string;
  loading?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

function ConfirmationModal({
  open,
  title,
  message,
  confirmLabel,
  loading = false,
  onConfirm,
  onCancel
}: ConfirmationModalProps) {
  const cancelButtonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) {
      return;
    }

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    cancelButtonRef.current?.focus();

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !loading) {
        onCancel();
      }
    }

    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [loading, onCancel, open]);

  if (!open) {
    return null;
  }

  return createPortal(
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !loading) {
          onCancel();
        }
      }}
    >
      <section
        className="confirmation-modal"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="confirmation-modal-title"
        aria-describedby="confirmation-modal-message"
      >
        <button
          className="modal-close-button"
          type="button"
          aria-label="닫기"
          disabled={loading}
          onClick={onCancel}
        >
          <X size={19} />
        </button>
        <div className="confirmation-modal-icon"><AlertCircle size={25} /></div>
        <h2 id="confirmation-modal-title">{title}</h2>
        <p id="confirmation-modal-message">{message}</p>
        <div className="modal-actions">
          <button ref={cancelButtonRef} type="button" disabled={loading} onClick={onCancel}>
            취소
          </button>
          <button className="is-danger" type="button" disabled={loading} onClick={onConfirm}>
            {loading ? '처리 중...' : confirmLabel}
          </button>
        </div>
      </section>
    </div>,
    document.body
  );
}

export default ConfirmationModal;
