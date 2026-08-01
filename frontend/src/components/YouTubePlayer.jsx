import { useEffect, useRef } from 'react';
import { loadYouTubeIframeApi } from '../utils/loadYouTubeApi';

// How much drift (in seconds) we tolerate before forcing a correction.
// Small drift is normal (network latency, polling granularity) and
// self-corrects; only bigger gaps mean an actual seek/join happened.
const DRIFT_THRESHOLD_SECONDS = 1.5;

// After we apply a remote update (or send one of our own), ignore
// player events for this long. This is what stops the echo loop
// described in the write-up: without it, our own seekTo()/playVideo()
// calls would immediately re-fire onStateChange and get re-sent to the
// server as if a human had just clicked something.
const SUPPRESS_WINDOW_MS = 700;

// How often we poll the player's currentTime to detect a manual seek.
// The YouTube IFrame API has no dedicated "onSeek" event, so this is
// the standard workaround: compare the actual position against what we
// expect given the last known position + elapsed time.
const POLL_INTERVAL_MS = 500;

function YouTubePlayer({ videoId, playing, currentTime, onPlay, onPause, onSeek }) {
  const containerRef = useRef(null);
  const playerRef = useRef(null);
  const loadedVideoIdRef = useRef(null);
  const suppressUntilRef = useRef(0);
  const lastKnownTimeRef = useRef(0);
  const pollRef = useRef(null);

  function suppress() {
    suppressUntilRef.current = Date.now() + SUPPRESS_WINDOW_MS;
  }

  function isSuppressed() {
    return Date.now() < suppressUntilRef.current;
  }

  // Create the player once on mount.
  useEffect(() => {
    let cancelled = false;

    loadYouTubeIframeApi().then((YT) => {
      if (cancelled || !containerRef.current) return;

      playerRef.current = new YT.Player(containerRef.current, {
        videoId,
        playerVars: { autoplay: playing ? 1 : 0 },
        events: {
          onReady: () => {
            loadedVideoIdRef.current = videoId;
            lastKnownTimeRef.current = currentTime || 0;
            suppress();
            if (currentTime) {
              playerRef.current.seekTo(currentTime, true);
            }
            // Explicitly enforce play/pause rather than trusting the
            // autoplay playerVar alone - seekTo() can kick off playback
            // on a freshly created player even with autoplay off, which
            // would otherwise leave a newly-joined viewer's video
            // playing when the room is actually paused.
            if (playing) {
              playerRef.current.playVideo();
            } else {
              playerRef.current.pauseVideo();
            }
          },
          onStateChange: handleStateChange,
        },
      });
    });

    pollRef.current = setInterval(pollForSeek, POLL_INTERVAL_MS);

    return () => {
      cancelled = true;
      clearInterval(pollRef.current);
      playerRef.current?.destroy?.();
      playerRef.current = null;
    };
    // Intentionally empty deps - the player is created once. Later prop
    // changes are applied via the effects below rather than recreating it.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // React to the video changing (a change_video broadcast came in).
  useEffect(() => {
    const player = playerRef.current;
    if (!player || !videoId) return;
    if (loadedVideoIdRef.current === videoId) return;

    suppress();
    loadedVideoIdRef.current = videoId;
    lastKnownTimeRef.current = currentTime || 0;
    player.loadVideoById({ videoId, startSeconds: currentTime || 0 });
    if (!playing) {
      // loadVideoById always starts playing - pause immediately if the
      // room's state says paused.
      player.pauseVideo();
    }
  }, [videoId]);

  // React to play/pause/seek state broadcast from the server (could be
  // our own echoed action, or someone else's).
  useEffect(() => {
    const player = playerRef.current;
    if (!player || typeof player.getCurrentTime !== 'function') return;
    if (loadedVideoIdRef.current !== videoId) return; // handled by the effect above instead

    const actualTime = player.getCurrentTime();
    const drift = Math.abs(actualTime - currentTime);

    if (drift > DRIFT_THRESHOLD_SECONDS) {
      suppress();
      player.seekTo(currentTime, true);
      lastKnownTimeRef.current = currentTime;
    }

    const state = player.getPlayerState(); // 1 = playing, 2 = paused
    if (playing && state !== 1) {
      suppress();
      player.playVideo();
    } else if (!playing && state !== 2) {
      suppress();
      player.pauseVideo();
    }
  }, [playing, currentTime, videoId]);

  function handleStateChange(event) {
    if (isSuppressed()) return;

    const player = playerRef.current;
    if (!player) return;

    const time = player.getCurrentTime();
    lastKnownTimeRef.current = time;

    if (event.data === window.YT.PlayerState.PLAYING) {
      onPlay(time);
    } else if (event.data === window.YT.PlayerState.PAUSED) {
      onPause(time);
    }
  }

  function pollForSeek() {
    const player = playerRef.current;
    if (!player || typeof player.getCurrentTime !== 'function') return;
    if (isSuppressed()) return;

    const state = player.getPlayerState();
    if (state !== 1 && state !== 2) return; // only meaningful while playing/paused

    const now = player.getCurrentTime();
    const elapsedSeconds = POLL_INTERVAL_MS / 1000;
    const expected = lastKnownTimeRef.current + (state === 1 ? elapsedSeconds : 0);
    const jump = Math.abs(now - expected);

    if (jump > DRIFT_THRESHOLD_SECONDS) {
      // Bigger jump than elapsed time explains - the user dragged the
      // seek bar (or used keyboard shortcuts to skip).
      onSeek(now);
    }

    lastKnownTimeRef.current = now;
  }

  return <div className="youtube-player" ref={containerRef} />;
}

export default YouTubePlayer;
