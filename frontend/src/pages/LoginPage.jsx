import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Clapperboard, Loader2 } from 'lucide-react';
import { login } from '../api/auth';

function LoginPage() {
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);

    if (!username.trim() || !password) {
      setError('Please enter a username and password.');
      return;
    }

    setLoading(true);
    try {
      await login(username.trim(), password);
      navigate('/');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <p className="eyebrow">
        <Clapperboard size={14} strokeWidth={2.5} />
        Watch Party
      </p>
      <h1>Welcome back.</h1>
      <p className="subtitle">Log in to use your account when creating or joining rooms.</p>

      <div className="home-card">
        <form onSubmit={handleSubmit} className="form">
          <label>
            Username
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="e.g. Mounika"
              maxLength={30}
              autoComplete="username"
            />
          </label>

          <label>
            Password
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              autoComplete="current-password"
            />
          </label>

          {error && <p className="error">{error}</p>}

          <button className="btn-primary" type="submit" disabled={loading}>
            {loading ? (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', justifyContent: 'center' }}>
                <Loader2 size={16} className="spin" /> Logging in...
              </span>
            ) : (
              'Log in'
            )}
          </button>
        </form>
      </div>

      <p className="subtitle" style={{ marginTop: '1.5rem' }}>
        Don't have an account? <Link to="/register">Sign up</Link>
      </p>
      <p className="subtitle">
        <Link to="/">Continue as a guest instead</Link>
      </p>
    </div>
  );
}

export default LoginPage;
