import { useState } from 'react';
import { Play } from 'lucide-react';
import { extractYouTubeId } from '../utils/youtube';

function ChangeVideoForm({ onChangeVideo }) {
  const [input, setInput] = useState('');
  const [error, setError] = useState(null);

  function handleSubmit(e) {
    e.preventDefault();
    const videoId = extractYouTubeId(input);
    if (!videoId) {
      setError("Couldn't recognize that as a YouTube URL or video id.");
      return;
    }
    setError(null);
    onChangeVideo(videoId);
    setInput('');
  }

  return (
    <div>
      <form className="change-video-form" onSubmit={handleSubmit}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Paste a YouTube URL..."
        />
        <button type="submit">
          <Play size={15} fill="currentColor" />
          Load
        </button>
      </form>
      {error && <p className="error" style={{ marginBottom: '1rem' }}>{error}</p>}
    </div>
  );
}

export default ChangeVideoForm;
