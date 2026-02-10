# Development Guide

## Quick Start

### Development Mode (Hot Reload)

**Terminal 1 - Start Spring Boot Backend:**
```bash
# From project root: /Users/germs/Documents/mercury-composable/extensions/json-path-playground
java -jar target/json-path-playground-4.3.62.jar
```

**Terminal 2 - Start Vite Dev Server:**
```bash
# From: /Users/germs/Documents/mercury-composable/extensions/json-path-playground/src/main/resources/public
npm run dev
```

**Access the app:** http://localhost:3000

All WebSocket and API requests are automatically proxied from port 3000 → 8085.

---

## Production Mode

### Option A: Manual Build

```bash
# From: src/main/resources/public
npm run build
npm run copy:dist

# From project root
mvn clean package
java -jar target/json-path-playground-4.3.62.jar
```

**Access the app:** http://localhost:8085

### Option B: Automated with Maven (Recommended)

Add the `frontend-maven-plugin` to your `pom.xml` (see README.md for configuration), then:

```bash
# From project root - builds everything
mvn clean package
java -jar target/json-path-playground-4.3.62.jar
```

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│  Development Mode                               │
├─────────────────────────────────────────────────┤
│                                                 │
│  Browser → localhost:3000 (Vite Dev Server)    │
│                    ↓ (proxy)                    │
│             localhost:8085 (Spring Boot)        │
│                                                 │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│  Production Mode                                │
├─────────────────────────────────────────────────┤
│                                                 │
│  Browser → localhost:8085 (Spring Boot)        │
│            ↓ (serves static files from public/) │
│       React App (built bundle)                  │
│                                                 │
└─────────────────────────────────────────────────┘
```

---

## Ports

- **8085** - Spring Boot backend (always runs here)
- **3000** - Vite dev server (development only)

---

## Common Commands

| Command | Description |
|---------|-------------|
| `npm run dev` | Start Vite dev server with HMR |
| `npm run build` | Build production bundle to `dist/` |
| `npm run build:prod` | Build and copy to public folder |
| `npm run preview` | Preview production build locally |

---

## Troubleshooting

### WebSocket connection fails in development
- Ensure Spring Boot is running on port 8085
- Check the proxy configuration in `vite.config.js`

### Static files not loading in production
- Make sure you ran `npm run build` and copied `dist/*` to the public folder
- Rebuild with `mvn clean package`

### Port 3000 or 8085 already in use
- Kill the process: `lsof -ti:3000 | xargs kill -9`
- Or change the port in `vite.config.js` or `application.properties`
