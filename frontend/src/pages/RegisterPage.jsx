import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Clapperboard, Loader2 } from 'lucide-react';
import { register } from '../api/auth';

function RegisterPage() {
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
    if (password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }

    setLoading(true);
    try {
      await register(username.trim(), password);
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
      <h1>Create an account.</h1>
      <p className="subtitle">Optional - you can still join rooms as a guest without one.</p>

      <div className="home-card">
        <form onSubmit={handleSubmit} className="form">
          <label>
            Username
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Letters, numbers, underscores"
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
              placeholder="At least 8 characters"
              autoComplete="new-password"
            />
          </label>

          {error && <p className="error">{error}</p>}

          <button className="btn-primary" type="submit" disabled={loading}>
            {loading ? (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.4rem', justifyContent: 'center' }}>
                <Loader2 size={16} className="spin" /> Creating account...
              </span>
            ) : (
              'Sign up'
            )}
          </button>
        </form>
      </div>

      <p className="subtitle" style={{ marginTop: '1.5rem' }}>
        Already have an account? <Link to="/login">Log in</Link>
      </p>
    </div>
  );
}

export default RegisterPage;
