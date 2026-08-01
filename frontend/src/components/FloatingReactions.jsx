/**
 * Renders a handful of active reaction emojis that float up and fade
 * over the video. The parent (RoomPage) owns the list and is
 * responsible for pruning old entries after they've had time to
 * animate out - this component just lays out whatever it's given.
 */
function FloatingReactions({ reactions }) {
  return (
    <div className="floating-reactions">
      {reactions.map((r) => (
        <span
          key={r.id}
          className="floating-reaction"
          style={{ left: `${r.leftPercent}%` }}
        >
          {r.emoji}
        </span>
      ))}
    </div>
  );
}

export default FloatingReactions;
