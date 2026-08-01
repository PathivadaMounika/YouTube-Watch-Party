import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Link2, LogOut, Lock, Crown, Shield, X, ArrowUp, ArrowDown, Repeat } from 'lucide-react';
import { getRoom } from '../api/client';
import { loadIdentity, saveIdentity, clearIdentity } from '../api/identity';
import { useRoomSocket } from '../hooks/useRoomSocket';
import YouTubePlayer from '../components/YouTubePlayer';
import ChangeVideoForm from '../components/ChangeVideoForm';
import ChatPanel from '../components/ChatPanel';
import FloatingReactions from '../components/FloatingReactions';
import ConfirmModal from '../components/ConfirmModal';
import { avatarColorFor, initialFor } from '../utils/avatar';

function RoomPage() {
  const { roomId } = useParams();
  const navigate = useNavigate();
  const [initialRoom, setInitialRoom] = useState(null);
  const [error, setError] = useState(null);
  const [toast, setToast] = useState(null);
  const [copied, setCopied] = useState(false);
  const [chatMessages, setChatMessages] = useState([]);
  const [floatingReactions, setFloatingReactions] = useState([]);
  const [pendingHostTransfer, setPendingHostTransfer] = useState(null);
  const storedIdentity = loadIdentity(roomId);

  // Live room state arrives over the WebSocket once connected/joined.
  // Until then, we fall back to the one-time REST fetch below.
  const { room: liveRoom, connected, sendAction, notification, clearNotification, removed, chatEvent } =
    useRoomSocket(roomId, storedIdentity?.userId);
  const room = liveRoom || initialRoom;

  // "you" should reflect your CURRENT role, not just the role you joined
  // with. sessionStorage only ever gets written once at create/join time -
  // if the Host promotes/demotes you afterwards, the only place that
  // shows up live is the room's own participants list. So we look
  // ourselves up there first, falling back to the stored identity only
  // if the room hasn't loaded yet.
  const you = room
    ? room.participants.find((p) => p.userId === storedIdentity?.userId) || storedIdentity
    : storedIdentity;

  useEffect(() => {
    if (!storedIdentity) {
      navigate('/');
      return;
    }

    getRoom(roomId)
      .then(setInitialRoom)
      .catch((err) => setError(err.message));
  }, [roomId]);

  // Keep sessionStorage's copy of "who am I" up to date so a page
  // refresh doesn't revert to a stale role.
  useEffect(() => {
    if (you && storedIdentity && you.role !== storedIdentity.role) {
      saveIdentity(roomId, you);
    }
  }, [you?.role]);

  // Reliable removal detection: if the broadcast we receive ever shows
  // us missing from the participant list, we were removed. This doesn't
  // depend on the private per-session notification channel below, which
  // can be unreliable for anonymous (non-logged-in) WebSocket sessions.
  useEffect(() => {
    if (removed) {
      clearIdentity(roomId);
      navigate('/', { state: { kicked: true } });
    }
  }, [removed]);

  // React to private notifications: currently just "action denied"
  // toasts. (Room removal is handled above via the more reliable
  // broadcast-based check instead.)
  useEffect(() => {
    if (!notification) return;

    if (notification.type === 'REMOVED_FROM_ROOM') {
      // Handled by the `removed` effect above - avoid double-navigating.
      return;
    }

    setToast(notification.message);
    const timer = setTimeout(() => setToast(null), 3500);
    clearNotification();
    return () => clearTimeout(timer);
  }, [notification]);

  // Route incoming chat-topic events: CHAT messages go into the feed,
  // REACTION events spawn a brief floating animation over the player.
  useEffect(() => {
    if (!chatEvent) return;

    if (chatEvent.type === 'CHAT') {
      setChatMessages((prev) => [...prev.slice(-99), chatEvent]);
      return;
    }

    if (chatEvent.type === 'REACTION') {
      const id = chatEvent._receivedAt + Math.random();
      const leftPercent = 15 + Math.random() * 70;
      setFloatingReactions((prev) => [...prev, { id, emoji: chatEvent.message, leftPercent }]);
      setTimeout(() => {
        setFloatingReactions((prev) => prev.filter((r) => r.id !== id));
      }, 1800);
    }
  }, [chatEvent]);

  const canControlPlayback = you?.role === 'HOST' || you?.role === 'MODERATOR';
  const isHost = you?.role === 'HOST';

  function copyInviteLink() {
    navigator.clipboard.writeText(window.location.href);
    setCopied(true);
    setTimeout(() => setCopied(false), 1800);
  }

  function leaveRoom() {
    clearIdentity(roomId);
    navigate('/');
  }

  function handleChangeVideo(videoId) {
    sendAction('change-video', { videoId });
  }

  function handlePlay(time) {
    sendAction('play', { time });
  }

  function handlePause(time) {
    sendAction('pause', { time });
  }

  function handleSeek(time) {
    sendAction('seek', { time });
  }

  function promote(userId) {
    sendAction('assign-role', { userId, role: 'MODERATOR' });
  }

  function demote(userId) {
    sendAction('assign-role', { userId, role: 'PARTICIPANT' });
  }

  function removeParticipant(userId) {
    sendAction('remove-participant', { userId });
  }

  function transferHost(userId, username) {
    setPendingHostTransfer({ userId, username });
  }

  function confirmTransferHost() {
    sendAction('transfer-host', { userId: pendingHostTransfer.userId });
    setPendingHostTransfer(null);
  }

  function cancelTransferHost() {
    setPendingHostTransfer(null);
  }

  function sendChat(message) {
    sendAction('chat', { message });
  }

  function sendReaction(emoji) {
    sendAction('reaction', { message: emoji });
  }

  if (error) {
    return (
      <div className="page">
        <h2>Couldn't load room</h2>
        <p className="error">{error}</p>
        <button className="btn-primary" onClick={() => navigate('/')} style={{ marginTop: '1rem' }}>
          Back home
        </button>
      </div>
    );
  }

  if (!room) {
    return (
      <div className="page">
        <p className="subtitle">Loading room...</p>
      </div>
    );
  }

  return (
    <div className="page room-page">
      {toast && <div className="toast">{toast}</div>}

      <ConfirmModal
        open={!!pendingHostTransfer}
        title="Transfer host?"
        message={
          pendingHostTransfer
            ? `Make ${pendingHostTransfer.username} the new Host? You'll become a Moderator.`
            : ''
        }
        confirmLabel="Make Host"
        cancelLabel="Cancel"
        onConfirm={confirmTransferHost}
        onCancel={cancelTransferHost}
      />

      <header className="room-header">
        <div className="room-header-left">
          <div className="ticket">
            <span className="ticket-code">{room.roomId}</span>
          </div>
          <div className="room-meta">
            <span className="room-you-line">
              {you.username} · {you.role.charAt(0) + you.role.slice(1).toLowerCase()}
            </span>
            <span className={`live-pill ${connected ? 'live' : ''}`}>
              <span className="dot" />
              {connected ? 'Live' : 'Connecting'}
            </span>
          </div>
        </div>
        <div className="room-header-actions">
          <button className="btn-ghost" onClick={copyInviteLink}>
            <Link2 size={15} />
            {copied ? 'Copied!' : 'Copy invite'}
          </button>
          <button className="btn-ghost danger" onClick={leaveRoom}>
            <LogOut size={15} />
            Leave
          </button>
        </div>
      </header>

      {room.videoId ? (
        <div className="player-wrapper">
          <YouTubePlayer
            videoId={room.videoId}
            playing={room.playing}
            currentTime={room.currentTime}
            onPlay={handlePlay}
            onPause={handlePause}
            onSeek={handleSeek}
          />
          <FloatingReactions reactions={floatingReactions} />
          {!canControlPlayback && (
            <div className="player-lock-overlay" title="Only the Host or a Moderator can control playback">
              <span>
                <Lock size={13} />
                Only Host/Moderator can control playback
              </span>
            </div>
          )}
        </div>
      ) : (
        <div className="video-placeholder">
          <p>No video selected yet.</p>
          <p className="hint">
            {canControlPlayback
              ? 'Paste a YouTube link below to get started.'
              : 'Waiting for the Host or a Moderator to choose a video.'}
          </p>
        </div>
      )}

      {canControlPlayback && <ChangeVideoForm onChangeVideo={handleChangeVideo} />}

      <div className="room-columns">
        <section>
          <h3>Participants ({room.participants.length})</h3>
          <ul className="participant-list">
            {room.participants.map((p) => (
              <li key={p.userId}>
                <span className="avatar" style={{ background: avatarColorFor(p.username) }}>
                  {initialFor(p.username)}
                </span>
                <span className="participant-name">{p.username}</span>
                <span className="participant-right">
                  <span className={`role-badge role-${p.role.toLowerCase()}`}>
                    {p.role === 'HOST' && <Crown size={11} />}
                    {p.role === 'MODERATOR' && <Shield size={11} />}
                    {p.role}
                  </span>
                  {isHost && p.userId !== you.userId && (
                    <span className="host-controls">
                      {p.role === 'PARTICIPANT' ? (
                        <button onClick={() => promote(p.userId)}>
                          <ArrowUp size={12} />
                          Moderator
                        </button>
                      ) : (
                        <button onClick={() => demote(p.userId)}>
                          <ArrowDown size={12} />
                          Participant
                        </button>
                      )}
                      <button onClick={() => transferHost(p.userId, p.username)}>
                        <Repeat size={12} />
                        Make Host
                      </button>
                      <button className="danger" onClick={() => removeParticipant(p.userId)}>
                        <X size={12} />
                        Remove
                      </button>
                    </span>
                  )}
                </span>
              </li>
            ))}
          </ul>
        </section>

        <ChatPanel
          messages={chatMessages}
          myUserId={you.userId}
          onSend={sendChat}
          onReact={sendReaction}
        />
      </div>
    </div>
  );
}

export default RoomPage;
