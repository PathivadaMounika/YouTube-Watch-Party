// Since Phase 1 is REST-only (no login), we remember "who am I in this
// room" in sessionStorage, keyed by roomId. This is looked up again when
// the WebSocket connects in Phase 2, and cleared if the tab closes.

function key(roomId) {
  return `watchparty:${roomId}`;
}

export function saveIdentity(roomId, you) {
  sessionStorage.setItem(key(roomId), JSON.stringify(you));
}

export function loadIdentity(roomId) {
  const raw = sessionStorage.getItem(key(roomId));
  return raw ? JSON.parse(raw) : null;
}

export function clearIdentity(roomId) {
  sessionStorage.removeItem(key(roomId));
}
