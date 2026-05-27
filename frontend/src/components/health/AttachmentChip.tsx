import { Chip } from '@mui/material';
import { fileApi } from '../../api/fileApi';
import type { Attachment } from '../../types/healthRecord';
import type { FileCategory } from '../../types/file';

const CATEGORY_ICON: Record<FileCategory, string> = {
  AVATAR: '🖼️',
  STOOL_IMAGE: '🖼️',
  HEALTH_REPORT: '📄',
  HEALTH_IMAGE: '🖼️',
  ULTRASOUND_VIDEO: '🎬',
};

interface AttachmentChipProps {
  attachment: Attachment;
  onRemove?: () => void;
}

export function AttachmentChip({ attachment, onRemove }: AttachmentChipProps) {
  const open = async () => {
    try {
      const { url } = await fileApi.signedViewUrl(attachment.fileId);
      window.open(url, '_blank', 'noopener,noreferrer');
    } catch {
      // swallow — the chip stays interactive; user can try again
    }
  };

  return (
    <Chip
      label={`${CATEGORY_ICON[attachment.category]} ${attachment.originalFilename}`}
      onClick={open}
      onDelete={onRemove}
      variant="outlined"
      sx={{ maxWidth: 280 }}
    />
  );
}
