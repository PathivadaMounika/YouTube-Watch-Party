// A handful of hand-picked colors from the app's palette (not an
// arbitrary rainbow) so avatars always feel like they belong to the same
// design, no matter whose name hashes to what.
const AVATAR_COLORS = ['#ffb100', '#7c6cff', '#4fb0ff', '#ff8a5c', '#5cd6a9', '#ff5d9e'];

export function avatarColorFor(name) {
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = (hash << 5) - hash + name.charCodeAt(i);
    hash |= 0;
  }
  return AVATAR_COLORS[Math.abs(hash) % AVATAR_COLORS.length];
}

export function initialFor(name) {
  return name?.trim()?.[0]?.toUpperCase() || '?';
}
