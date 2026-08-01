# Watch Party — v3 (chat, reactions, transfer host)

All 4 phases plus the v2 sync fixes/redesign, plus three of the
assignment's bonus features: live text chat, emoji reactions, and
Host-to-participant host transfer.

## Structure

```
watch-party/
├── backend/    Spring Boot (Java 21, Maven)
└── frontend/   React + Vite (JavaScript)
```

## Backend

Requires Java 21 and Maven. This sandbox couldn't reach Maven Central to
compile-check it, so build it on your own machine first:

```bash
cd backend
mvn spring-boot:run
```

Runs on **http://localhost:8080**. Verify with:

```bash
curl http://localhost:8080/api/health
# {"status":"ok","message":"Watch Party backend is alive"}
```

### Database + accounts (new)

Accounts are backed by MySQL, and **login is now required** to create or
join a room (`/api/rooms/**` rejects unauthenticated requests with a 401).
Room creation/joining always uses the identity from your JWT - the server
no longer trusts a client-supplied username.

Two tables are created automatically on first run:
- `users` - accounts (username, BCrypt password hash)
- `rooms` - one durable record per room: `roomId`, `status` (`ACTIVE` /
  `ENDED`), `hostUserId`, `hostUsername`, `lastVideoId`, `createdAt`,
  `endedAt`. This is separate from the in-memory `Room` used for live
  playback sync - that stays in-memory since it's inherently a real-time
  session, while `rooms` is the durable history/metadata.

1. Install/start MySQL locally, then create the database once:
   ```sql
   CREATE DATABASE watchparty;
   ```
2. Set your credentials in `backend/src/main/resources/application.properties`
   (`spring.datasource.username` / `spring.datasource.password`), or override
   via env vars.
3. Hibernate creates both tables automatically (`spring.jpa.hibernate.ddl-auto=update`).

Auth endpoints:
```bash
POST /api/auth/register   { "username": "...", "password": "..." }  -> { token, userId, username }
POST /api/auth/login      { "username": "...", "password": "..." }  -> { token, userId, username }
```
The frontend stores the returned JWT in `localStorage` and sends it as
`Authorization: Bearer <token>` on API requests. Set `jwt.secret` (a
base64-encoded 256-bit key) via the `JWT_SECRET` env var in real
deployments instead of using the checked-in dev default.

## Frontend

```bash
cd frontend
npm install   # already run once when scaffolding, but harmless to repeat
npm run dev
```

Runs on **http://localhost:5173**. Open it in the browser — it calls the
backend's `/api/health` endpoint and displays the response. If you see
"Could not reach backend", start the backend first.

## What's here right now

### Backend
- `model/` — `Room`, `Participant`, `Role` (HOST / MODERATOR / PARTICIPANT)
- `service/RoomService` — in-memory room registry (`ConcurrentHashMap`),
  generates 6-character room codes, creates rooms, joins participants
- `controller/RoomController`:
  - `POST /api/rooms` `{ username }` -> creates a room, caller becomes Host
  - `GET /api/rooms/{roomId}` -> current room state + participant list
  - `POST /api/rooms/{roomId}/join` `{ username }` -> joins as Participant
- `exception/` — 404 for unknown room codes, 400 for validation errors,
  returned as clean JSON via a `@RestControllerAdvice`
- `dto/` — request/response shapes so we never leak internal model classes
  over the wire

Try it directly once the backend is running:
```bash
curl -X POST http://localhost:8080/api/rooms \
  -H "Content-Type: application/json" \
  -d '{"username":"Mounika"}'
```

### Frontend
- `pages/HomePage` — toggle between Create Room / Join Room, calls the
  backend, then navigates to `/room/:roomId`
- `pages/RoomPage` — loads room state, shows the participant list with
  role badges, "copy invite link", "leave room"
- `api/client.js` — small fetch wrapper for the three endpoints above
- `api/identity.js` — remembers "who am I in this room" in
  `sessionStorage` (no auth yet — that's a later bonus), keyed by roomId

### Try the full flow
1. Start backend (STS) and frontend (`npm run dev`)
2. Open `http://localhost:5173`, enter a name, click **Create Room** ->
   you land on `/room/XXXXXX` as Host
3. Copy that URL, open it in a private/incognito window, use **Join Room**
   with the same code and a different name -> you join as Participant
4. Both windows show the same participant list when you refresh

## Next (Phase 2)

WebSocket connection (STOMP + SockJS) — presence (`user_joined`/`user_left`)
broadcast live to everyone in the room, replacing the "refresh to see
updates" behavior from Phase 1.

---

## Phase 2 — how it works

**Flow:** create/join a room over REST like before (this still hands back
your `userId`/`role`) → the moment you land on the room page, the frontend
opens a WebSocket, subscribes to that room's topic, and sends a "join"
frame carrying the `userId` it already has. The server ties your WebSocket
session to that participant and broadcasts the full room state to everyone
subscribed. When your tab closes (or you click Leave), the server detects
the disconnect, removes you from the room, and broadcasts the update again
— everyone else's participant list drops you automatically.

### Backend additions
- `config/WebSocketConfig` — enables STOMP over SockJS at `/ws`, broker
  prefix `/topic` (server → clients), app prefix `/app` (clients → server)
- `controller/RoomWebSocketController` — handles `@MessageMapping("/room/{roomId}/join")`:
  validates the participant exists (via `RoomService`), maps the WS session
  to them, broadcasts state
- `listener/WebSocketEventListener` — reacts to `SessionDisconnectEvent`,
  removes the participant, broadcasts state
- `service/WebSocketSessionRegistry` — maps `sessionId -> {roomId, userId}`
  so the disconnect handler knows who just left
- `service/RoomBroadcaster` — single place that pushes `RoomResponse` to
  `/topic/room/{roomId}`, used by both the join handler and the disconnect
  listener so they can't drift out of sync

We broadcast the *entire* room state on every change rather than granular
`user_joined`/`user_left` event types — simpler to reason about and the
frontend just replaces its local state wholesale. Fine-grained event types
are a natural next refinement once playback events (Phase 3) need it.

### Frontend additions
- `hooks/useRoomSocket.js` — opens the STOMP client, subscribes to the
  room's topic, sends the join frame, exposes `{ room, connected }`
- `pages/RoomPage` — uses the hook for live state, falls back to the
  one-time REST fetch while the socket is still connecting; shows a small
  green/gray dot next to your name for connection status

### Try it
1. Start backend (STS) and frontend (`npm run dev`)
2. Create a room in one window, join it from a second (incognito) window
3. **Without refreshing**, the first window's participant list should
   update the instant the second window joins
4. Close the second window (or click **Leave**) — the first window's list
   should drop that participant within a couple seconds, live

## Next (Phase 3)

Playback sync: YouTube IFrame player wired up, `play`/`pause`/`seek`/
`change_video` events sent over the same WebSocket connection, role
enforcement so only Host/Moderator can control playback.

---

## Phase 3 — how it works

### The core problem: echo loops

When the server broadcasts a `sync_state` update, it goes to **everyone**
in the room, including whoever just triggered it. If we naively call
`player.playVideo()` every time a broadcast says "playing", that call
itself fires the player's `onStateChange` event again, which our code
would then re-send to the server, which broadcasts again... an infinite
loop (or at minimum stutter/flicker).

**Fix:** a short "suppression window" (`SUPPRESS_WINDOW_MS` in
`YouTubePlayer.jsx`, currently 700ms). Right before we programmatically
call `playVideo()`/`pauseVideo()`/`seekTo()` in response to a server
broadcast, we start the suppression window. Any player events that fire
while it's active are treated as "that was us, not the user" and are not
re-sent to the server.

### The second problem: detecting seeks

YouTube's IFrame API has no `onSeek` event. We poll `getCurrentTime()`
every 500ms (`POLL_INTERVAL_MS`) and compare it against what we'd expect
given the last known position and elapsed time. A gap bigger than
`DRIFT_THRESHOLD_SECONDS` (1.5s) means the user dragged the seek bar (or
a sync correction is needed) - handled the same way whether the video is
playing or paused.

### Backend additions
- `RoomWebSocketController` — new handlers: `/room/{roomId}/play`,
  `/pause`, `/seek`, `/change-video`. Each just mutates `Room`'s playback
  state and broadcasts the full state via the same `RoomBroadcaster` from
  Phase 2 - no new broadcast mechanism needed
- No permission checks yet (anyone can control playback) - Phase 4 adds
  that validation into these same handlers

### Frontend additions
- `utils/loadYouTubeApi.js` — loads the YouTube IFrame API script once
  (it calls a global callback, not a promise, so this wraps it in one)
- `utils/youtube.js` — parses a pasted YouTube URL (watch/youtu.be/embed/
  shorts) or bare video id into just the id
- `components/YouTubePlayer.jsx` — the player itself, with the
  suppression-window pattern described above
- `components/ChangeVideoForm.jsx` — paste a link, sends `change-video`
- `pages/RoomPage` — renders the player when `room.videoId` is set, wires
  its callbacks to `sendAction()` from the WebSocket hook

### Try it
1. Start backend (STS) and frontend (`npm run dev`)
2. Create a room, paste a YouTube link into the box, click **Load**
3. Open a second (incognito) window, join the room - it should load the
   same video at the same position automatically
4. Press play/pause/seek in one window - the other should follow within
   about a second, with no flicker or fighting between the two
5. Paste a different link in either window - both switch together

## Next (Phase 4)

Role enforcement: reject `play`/`pause`/`seek`/`change-video` from a plain
Participant on the backend, disable those controls in the UI for
Participants, and add `assign_role`/`remove_participant` so the Host can
promote people or remove them.

---

## Phase 4 — how it works

### The core idea: the backend is the real gate, the UI is a courtesy

Every playback handler now looks up *who* sent the message (via the
session -> participant mapping set up in Phase 2's join handler) and
checks their role before touching any state. If they're not allowed, we
don't broadcast anything - we just send a private "denied" notice back to
that one sender. The frontend also hides/disables the relevant controls
for Participants, but that's just UX - even if someone bypassed the UI
(e.g. via browser devtools), the backend check is what actually protects
the room.

### Sending a message to just one person

Rejections and "you were removed" notices need to reach exactly one
session, not the whole room. Since this app has no login, there's no
authenticated `Principal` to address a user by - so `NotificationSender`
uses the WebSocket session id itself as the "user" identifier with
Spring's `convertAndSendToUser()`. This is a documented fallback Spring
provides for anonymous WebSocket sessions, and the frontend subscribes to
`/user/queue/notifications` to receive it.

### Backend additions
- `service/RoleService` — single place defining "who can control
  playback" (Host/Moderator) and "who is Host" - avoids duplicating that
  logic across four separate handlers
- `service/NotificationSender` — sends a private message to one session
- `RoomWebSocketController` — every playback handler now calls
  `requirePlaybackPermission()` first; two new handlers:
  - `/room/{roomId}/assign-role` - Host-only, sets a participant's role
    to Moderator or Participant (not Host - that's a distinct, more
    sensitive "transfer host" feature we're leaving as a bonus)
  - `/room/{roomId}/remove-participant` - Host-only, removes someone and
    sends them a private `REMOVED_FROM_ROOM` notice
- `WebSocketSessionRegistry.findSessionId()` - reverse lookup so we can
  find a *specific* participant's session to notify them when kicked

### Frontend additions
- `useRoomSocket` now also subscribes to `/user/queue/notifications` and
  exposes the latest `notification`
- `RoomPage`:
  - shows a red toast for `ACTION_DENIED` (auto-dismisses after ~3.5s)
  - on `REMOVED_FROM_ROOM`, clears identity and navigates home with a
    banner explaining what happened
  - hides the "paste a video" form entirely for non-privileged users
  - overlays a semi-transparent, click-blocking layer on the YouTube
    player for Participants (their native player controls are still
    physically there, this just stops clicks reaching them - a courtesy
    layer, since the backend is the real enforcement)
  - Host sees "Make Moderator"/"Make Participant" and "Remove" buttons
    next to every other participant in the list

### Try it
1. Start backend (STS) and frontend (`npm run dev`)
2. Create a room as Host, join from a second (incognito) window as a
   plain Participant
3. In the Participant window: the video-change form is gone, and a lock
   overlay sits over the player - clicking it does nothing
4. In the Host window: click **Make Moderator** next to the Participant's
   name - their window should now show full controls (form + no overlay)
5. Demote them back to Participant, then click **Remove** - their window
   should immediately kick them back to the home page with a "removed by
   Host" banner, and the Host's participant list drops them

## Next (later / bonus ideas)

Transfer host, persistent rooms (survive server restart), authentication,
text chat, reactions, horizontal scaling with Redis Pub/Sub - see the
Bonus Ideas section of the original assignment for the full list.

---

## v2 — bug fixes from real testing

Three issues found by actually running the app with multiple people at
once, each with a real underlying cause:

### 1. New joiners (and anyone joining a room already playing) start behind
`Room.currentTime` was only ever a **snapshot** taken at the moment of
the last play/pause/seek/change-video event - it never advanced on its
own between events. Someone who joined 90 seconds after the Host hit
play would get handed that stale snapshot and start 90 seconds behind.

**Fix:** `Room` now also tracks `lastUpdatedAtMillis` (server clock, set
by `touchPlaybackTimestamp()` in every playback handler). `RoomResponse`
computes a **live-adjusted** position: if the room is playing, it adds
however many real seconds have elapsed since that snapshot was taken. So
`GET /api/rooms/{roomId}` and every broadcast always reflect where
playback actually is *right now*, not where it was last time someone
touched a control.

On top of that, a `PlaybackHeartbeat` (`@Scheduled`, every 4 seconds)
re-broadcasts this live-adjusted state for any room currently playing.
This means even a client that's still buffering when they first join
gets a correction shortly after - and it self-heals repeatedly, rather
than only once. This is also what fixes the "more participants = more
lag" symptom: each newly-joined video has to buffer from scratch (that
delay is inherent to embedding YouTube and can't be fully eliminated),
but now nobody stays behind for more than a few seconds.

### 2. A late joiner's video plays even when the room is paused
A quirk of the YouTube IFrame API: calling `seekTo()` on a freshly
created player can kick off playback **even with `autoplay: 0` set**.
The original `onReady` handler seeked to the room's current position but
never explicitly re-asserted "and pause" afterward, so a joiner arriving
while the Host had the video paused would see it start playing on their
screen regardless.

**Fix:** `onReady` in `YouTubePlayer.jsx` now explicitly calls
`playVideo()` or `pauseVideo()` right after seeking, based on the room's
actual `playing` state - matching the same pattern already used
elsewhere in the component for later updates.

### 3. Visual redesign
Went from the functional-but-plain v1 UI to a "movie night" theme:
- **Palette:** near-black indigo background, warm marquee-amber accent
  for primary actions/Host, a velvet-violet secondary for Moderator,
  green reserved for the live-connection indicator (the one place a
  color needs to carry real meaning)
- **Type:** Space Grotesk for display/headings, Inter for body text,
  JetBrains Mono for the room code and role labels
- **Signature element:** the room code as a dashed-border "ticket stub"
  chip - a nod to the movie-going theme instead of a generic pill/badge
- Participant avatars are colored circles with the first letter of their
  name (deterministic per-name color, not random each render)
- Icons via `lucide-react` throughout (copy/leave/lock/crown/shield/etc)
  instead of plain text buttons

### Try it
1. Extract this zip, backend: refresh/re-import in STS and re-run;
   frontend: `npm install` (picks up the new `lucide-react` dependency)
   then `npm run dev`
2. Create a room, start a video playing, then join from a second
   (incognito) window a good 30+ seconds later - it should land close to
   the Host's actual position, not back near where it started
3. Pause the video as Host, then join from a third window - it should
   load paused, not playing
4. General visual check - room code ticket chip, colored avatars, icon
   buttons throughout

---

## v3 — chat, reactions, transfer host

Three bonus features from the assignment, each reusing existing
architecture rather than introducing anything new.

### Chat and reactions
- New backend handlers `/room/{roomId}/chat` and `/room/{roomId}/reaction`
  - no role restriction - anyone can chat/react
  - a reaction is really the same message shape as a chat message; the
    only difference is a `type` field and (client-side) how it's rendered
  - broadcast on their own topic (`/topic/room/{roomId}/chat`), separate
    from room/playback state, so the two don't get tangled together
- Not persisted - chat history is gone on refresh, which is fine for a
  live watch-party feature (this mirrors the "ephemeral" framing the
  assignment itself gives reactions)
- Frontend: `ChatPanel` renders the message feed + input + a row of quick
  reaction buttons (😂 😮 ❤️ 👍 🔥); `FloatingReactions` renders incoming
  reactions as a brief emoji burst that animates up and fades over the
  video, so reactions are felt in the moment rather than cluttering the
  chat feed

### Transfer host
- New handler `/room/{roomId}/transfer-host`, Host-only (same permission
  pattern as `assign-role`/`remove-participant`)
- The outgoing Host becomes a Moderator rather than dropping straight to
  Participant - a softer landing, and they keep playback control
- Frontend: a "Make Host" button next to each participant in the Host's
  controls, with a confirmation prompt since it immediately gives up your
  own Host status

### Try it
1. Open a room in two windows (different names)
2. Send a chat message from one - it should appear in both windows'
   chat panel, right-aligned/highlighted for whoever sent it
3. Click a reaction emoji - it should float up and fade over the video
   in **both** windows
4. As Host, click **Make Host** next to the other participant, confirm -
   their window should immediately gain Host controls (video form
   unlocked, promote/remove buttons appear on others), and yours should
   drop to Moderator (still able to control playback, but no participant
   management buttons on yourself)
