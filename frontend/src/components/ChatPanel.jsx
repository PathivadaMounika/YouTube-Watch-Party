import { useEffect, useRef, useState } from 'react';
import { Send } from 'lucide-react';
import { avatarColorFor, initialFor } from '../utils/avatar';

const QUICK_REACTIONS = ['😂', '😮', '❤️', '👍', '🔥'];

function formatTime(isoString) {
  try {
    return new Date(isoString).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  } catch {
    return '';
  }
}

function ChatPanel({ messages, myUserId, onSend, onReact }) {
  const [input, setInput] = useState('');
  const listRef = useRef(null);

  // Keep the feed scrolled to the latest message as new ones arrive.
  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
  }, [messages.length]);

  function handleSubmit(e) {
    e.preventDefault();
    const trimmed = input.trim();
    if (!trimmed) return;
    onSend(trimmed);
    setInput('');
  }

  return (
    <div className="chat-panel">
      <h3>Chat</h3>

      <div className="chat-messages" ref={listRef}>
        {messages.length === 0 && <p className="chat-empty">No messages yet - say hi!</p>}
        {messages.map((m) => {
          const isMe = m.userId === myUserId;
          return (
            <div key={m.timestamp + m.userId} className={`chat-message ${isMe ? 'mine' : ''}`}>
              <span className="avatar avatar-sm" style={{ background: avatarColorFor(m.username) }}>
                {initialFor(m.username)}
              </span>
              <div className="chat-bubble">
                <div className="chat-bubble-meta">
                  <span className="chat-username">{isMe ? 'You' : m.username}</span>
                  <span className="chat-time">{formatTime(m.timestamp)}</span>
                </div>
                <div className="chat-text">{m.message}</div>
              </div>
            </div>
          );
        })}
      </div>

      <div className="reaction-bar">
        {QUICK_REACTIONS.map((emoji) => (
          <button key={emoji} type="button" onClick={() => onReact(emoji)} title="React">
            {emoji}
          </button>
        ))}
      </div>

      <form className="chat-input-form" onSubmit={handleSubmit}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Send a message..."
          maxLength={500}
        />
        <button type="submit" aria-label="Send">
          <Send size={15} />
        </button>
      </form>
    </div>
  );
}

export default ChatPanel;
