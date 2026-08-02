#  YouTube Watch Party

A real-time YouTube Watch Party application that allows multiple users to watch YouTube videos together with synchronized playback, live chat, and room management.

##  Live Demo

### Frontend
https://you-tube-watch-party-sand.vercel.app

### Backend API
https://youtube-watch-party-api-production-256b.up.railway.app

### Health Check
https://youtube-watch-party-api-production-256b.up.railway.app/api/health

---

# Features

- User Registration & Login (JWT Authentication)
- Guest Mode
- Create Watch Rooms
- Join Existing Rooms
- Real-Time Video Synchronization
- Play / Pause / Seek Synchronization
- Live Chat
- Participant List Updates
- Host Controls
- Room Invite Code
- Responsive UI
- WebSocket (STOMP + SockJS) Communication
- REST API Backend
- Persistent MySQL Database

---

# Tech Stack

## Frontend

- React
- Vite
- JavaScript
- CSS
- STOMP.js
- SockJS

## Backend

- Java
- Spring Boot
- Spring Security
- Spring WebSocket
- Spring Data JPA
- Hibernate
- JWT Authentication

## Database

- MySQL (Railway)

## Deployment

- Frontend: Vercel
- Backend: Railway
- Database: Railway MySQL

---

# Project Structure

```
watch-party-v6
│
├── frontend
│   ├── src
│   ├── public
│   └── package.json
│
├── backend
│   ├── src
│   ├── pom.xml
│   └── Dockerfile
│
└── README.md
```

---

# Local Setup

## Clone Repository

```bash
git clone https://github.com/Pathivada Mounika/YouTube-Watch-Party.git

cd YouTube-Watch-Party
```

---

# Backend Setup

Navigate to backend

```bash
cd backend
```

Configure the following environment variables:

```
DB_URL=
DB_USERNAME=
DB_PASSWORD=
JWT_SECRET=
JWT_EXPIRATION_MINUTES=60
```

Run

```bash
mvn spring-boot:run
```

Backend runs on

```
http://localhost:8080
```

---

# Frontend Setup

Navigate to frontend

```bash
cd frontend
```

Install dependencies

```bash
npm install
```

Start development server

```bash
npm run dev
```

Frontend runs on

```
http://localhost:5173
```

---

# Environment Variables

## Backend

```
DB_URL
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION_MINUTES
```

---

# API Endpoints

## Authentication

```
POST /api/auth/register
POST /api/auth/login
```

## Room

```
POST /api/rooms
POST /api/rooms/join
GET /api/rooms/{roomId}
```

## Health

```
GET /api/health
```

---

# WebSocket Endpoint

```
/ws
```

Message Prefix

```
/app
```

Topic Prefix

```
/topic
```

---

# Deployment

Frontend

- Vercel

Backend

- Railway

Database

- Railway MySQL

---

# Screenshots

Register Page
<img width="1363" height="617" alt="Screenshot from 2026-08-02 13-51-15" src="https://github.com/user-attachments/assets/58d33760-5742-4b43-b103-feea6e9c4e57" />

Login Page
<img width="1363" height="617" alt="Screenshot from 2026-08-02 13-51-30" src="https://github.com/user-attachments/assets/eb11ea12-5f21-41e3-90cd-ebeaeb6e5f4b" />

Dashboard page
<img width="1363" height="617" alt="Screenshot from 2026-08-02 13-51-42" src="https://github.com/user-attachments/assets/be29b6cd-7156-4a45-8711-e333bb17b698" />

chat and emojis
<img width="1363" height="617" alt="Screenshot from 2026-08-02 13-51-54" src="https://github.com/user-attachments/assets/49e3d17d-9563-4cba-a091-e039571d1ed1" />

watch page
<img width="1363" height="617" alt="Screenshot from 2026-08-02 13-53-52" src="https://github.com/user-attachments/assets/a68a5d0f-9181-4e66-b871-70402a06eea7" />

Synchronization
<img width="868" height="477" alt="Screenshot from 2026-08-02 13-59-00" src="https://github.com/user-attachments/assets/1667ce03-dd34-461a-b117-e6fe7c52313e" />



---

# Future Improvements

- Voice Chat
- Video Chat
- Emoji Reactions
- Playlist Support
- Video Queue
- Screen Sharing
- Notifications
- Dark/Light Theme

---

# Author

**Pathiavada Mounika**

GitHub:
https://github.com/PathivadaMounika

---

# License

This project is developed for educational and portfolio purposes.
