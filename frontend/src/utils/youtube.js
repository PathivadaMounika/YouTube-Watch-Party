/**
 * Accepts a full YouTube URL (watch, youtu.be, embed, shorts) or a bare
 * 11-character video id, and returns just the id. Returns null if it
 * can't figure it out, so the caller can show a friendly error instead
 * of silently loading nothing.
 */
export function extractYouTubeId(input) {
  const trimmed = input.trim();
  if (!trimmed) return null;

  // Already looks like a bare video id.
  if (/^[a-zA-Z0-9_-]{11}$/.test(trimmed)) {
    return trimmed;
  }

  try {
    const url = new URL(trimmed);

    if (url.hostname.includes('youtu.be')) {
      return url.pathname.slice(1) || null;
    }

    if (url.hostname.includes('youtube.com')) {
      if (url.pathname === '/watch') {
        return url.searchParams.get('v');
      }
      if (url.pathname.startsWith('/embed/')) {
        return url.pathname.split('/embed/')[1];
      }
      if (url.pathname.startsWith('/shorts/')) {
        return url.pathname.split('/shorts/')[1];
      }
    }
  } catch {
    // Not a valid URL and not a bare id - fall through to null.
  }

  return null;
}
