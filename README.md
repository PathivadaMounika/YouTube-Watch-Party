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

You can add screenshots of:

- Login Page
- Register Page
- Dashboard
- Watch Room
- Live Chat
- Multiple Participants

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
