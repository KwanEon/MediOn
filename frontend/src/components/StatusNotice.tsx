import { Database, RefreshCw } from 'lucide-react';
import { useMedicalSearchStore } from '../store/useMedicalSearchStore';

function StatusNotice() {
  const error = useMedicalSearchStore((state) => state.error);
  const previewMode = useMedicalSearchStore((state) => state.previewMode);
  const loadNearbyInstitutions = useMedicalSearchStore((state) => state.loadNearbyInstitutions);

  if (!error) {
    return null;
  }

  return (
    <div className={previewMode ? 'notice preview-notice' : 'notice'} role="status">
      <Database size={17} />
      <span>{error}</span>
      <button type="button" onClick={loadNearbyInstitutions} aria-label="다시 연결" title="다시 연결">
        <RefreshCw size={16} />
      </button>
    </div>
  );
}

export default StatusNotice;
