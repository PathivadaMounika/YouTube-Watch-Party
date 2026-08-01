const BASE_URL = 'http://localhost:8080/api';
const STORAGE_KEY = 'watchparty:auth';

async function authRequest(path, body) {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });

  const data = await res.json().catch(() => ({}));

  if (!res.ok) {
    throw new Error(data.error || `Request failed (${res.status})`);
  }

  return data;
}

export function register(username, password) {
  return authRequest('/auth/register', { username, password }).then(saveAuth);
}

export function login(username, password) {
  return authRequest('/auth/login', { username, password }).then(saveAuth);
}

export function logout() {
  localStorage.removeItem(STORAGE_KEY);
}

function saveAuth(authResponse) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(authResponse));
  return authResponse;
}

// The logged-in account, or null if browsing as a guest. Read fresh each
// time rather than cached, so a login/logout in one tab is picked up.
export function getCurrentUser() {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw);
    return { userId: parsed.userId, username: parsed.username };
  } catch {
    return null;
  }
}

export function getToken() {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw).token;
  } catch {
    return null;
  }
}
