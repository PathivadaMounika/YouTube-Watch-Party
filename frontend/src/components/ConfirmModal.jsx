import { useEffect, useRef } from 'react';

/**
 * Reusable confirmation modal - replaces window.confirm with something
 * that matches the app's theme. Controlled component: renders nothing
 * when `open` is false.
 */
function ConfirmModal({
  open,
  title,
  message,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  tone = 'default', // 'default' | 'danger'
  onConfirm,
  onCancel,
}) {
  const confirmBtnRef = useRef(null);

  // Focus the confirm button when the modal opens, and let Escape cancel.
  useEffect(() => {
    if (!open) return;

    confirmBtnRef.current?.focus();

    function handleKeyDown(e) {
      if (e.key === 'Escape') onCancel();
    }
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, onCancel]);

  if (!open) return null;

  return (
    <div className="modal-backdrop" onClick={onCancel}>
      <div
        className="modal-card"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="modal-title"
        aria-describedby="modal-message"
        onClick={(e) => e.stopPropagation()}
      >
        <h3 id="modal-title" className="modal-title">{title}</h3>
        <p id="modal-message" className="modal-message">{message}</p>
        <div className="modal-actions">
          <button className="btn-ghost" onClick={onCancel}>
            {cancelLabel}
          </button>
          <button
            ref={confirmBtnRef}
            className={tone === 'danger' ? 'btn-primary danger' : 'btn-primary'}
            onClick={onConfirm}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

export default ConfirmModal;
