import { getToken } from './auth';

const BASE_URL = 'http://localhost:8080/api';

async function request(path, options = {}) {
  const token = getToken();
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...options,
  });

  const data = await res.json().catch(() => ({}));

  if (!res.ok) {
    throw new Error(data.error || `Request failed (${res.status})`);
  }

  return data;
}

// Login is required for both of these now - the server derives who you
// are from your JWT rather than trusting a client-supplied name.
export function createRoom() {
  return request('/rooms', { method: 'POST' });
}

export function joinRoom(roomId) {
  return request(`/rooms/${roomId}/join`, { method: 'POST' });
}

export function getRoom(roomId) {
  return request(`/rooms/${roomId}`);
}
