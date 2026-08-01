import { useState } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { Clapperboard, Loader2, LogOut, LogIn } from 'lucide-react';
import { createRoom, joinRoom } from '../api/client';
import { saveIdentity } from '../api/identity';
import { getCurrentUser, logout } from '../api/auth';

function HomePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const currentUser = getCurrentUser();
  const [mode, setMode] = useState('create'); // 'create' | 'join'
  const [roomCode, setRoomCode] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  function handleLogout() {
    logout();
    navigate(0);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);

    if (mode === 'join' && !roomCode.trim()) {
      setError('Please enter a room code.');
      return;
    }

    setLoading(true);
    try {
      const result =
        mode === 'create'
          ? await createRoom()
          : await joinRoom(roomCode.trim().toUpperCase());

      saveIdentity(result.room.roomId, result.you);
      navigate(`/room/${result.room.roomId}`);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <div className="account-bar">
        {currentUser ? (
          <span className="subtitle" style={{ margin: 0 }}>
            Signed in as <strong>{currentUser.username}</strong>{' '}
            <button className="btn-ghost" onClick={handleLogout} type="button" style={{ marginLeft: '0.5rem' }}>
              <LogOut size={13} />
              Log out
            </button>
          </span>
        ) : (
          <span className="subtitle" style={{ margin: 0 }}>
            You'll need an account to create or join a room.
          </span>
        )}
      </div>

      <p className="eyebrow">
        <Clapperboard size={14} strokeWidth={2.5} />
        Watch Party
      </p>
      <h1>Press play together,<br />wherever you are.</h1>
      <p className="subtitle">Sync a YouTube video across everyone in the room, live.</p>

      {location.state?.kicked && (
        <p className="notice">You were removed from that room by the Host.</p>
      )}

      {!currentUser ? (
        <div className="home-card">
          <p className="subtitle" style={{ marginTop: 0 }}>
            Log in or create an account to get started.
          </p>
          <Link to="/login" className="btn-primary" style={{ display: 'flex', justifyContent: 'center', gap: '0.4rem', textDecoration: 'none' }}>
            <LogIn size={16} />
            Log in
          </Link>
          <p className="subtitle" style={{ marginTop: '1rem', marginBottom: 0 }}>
            Don't have an account? <Link to="/register">Sign up</Link>
          </p>
        </div>
      ) : (
        <div className="home-card">
          <div className="mode-tabs">
            <button
              className={mode === 'create' ? 'active' : ''}
              onClick={() => setMode('create')}
              type="button"
            >
              Create room
            </button>
            <button
              className={mode === 'join' ? 'active' : ''}
              onClick={() => setMode('join')}
              type="button"
            >
              Join room
            </button>
          </div>

          <form onSubmit={handleSubmit} className="form">
            {mode === 'join' && (
              <label>
                Room code
                <input
                  value={roomCode}
                  onChange={(e) => setRoomCode(e.target.value)}
                  placeholder="e.g. AB3XQ9"
                  maxLength={6}
                  style={{ textTransform: 'uppercase', fontFamily: 'var(--font-mono)', letterSpacing: '0.08em' }}
                />
              </label>
            )}

            {error && <p className="error">{error}</p>}

            <button className="btn-primary" type="submit" disabled={loading}>
              {loading ? (
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', justifyContent: 'center' }}>
                  <Loader2 size={16} className="spin" /> Please wait...
                </span>
              ) : mode === 'create' ? (
                'Create room'
              ) : (
                'Join room'
              )}
            </button>
          </form>
        </div>
      )}
    </div>
  );
}

export default HomePage;
