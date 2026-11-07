# THE ALIBI — GEMINI API CONFIGURATION GUIDE

## 1. Environment Variables
Configure the following environment variables on the Ktor server or deployment container:

| Variable | Description | Default Value |
|---|---|---|
| `GEMINI_API_KEY` | Google Gemini API Key | *(Required for live cloud generation)* |
| `GEMINI_TEXT_MODEL` | Gemini model for case architecture | `gemini-2.5-flash` |
| `GEMINI_IMAGE_MODEL` | Gemini / Imagen model for visual generation | `imagen-3.0-generate-002` |
| `PORT` | Ktor backend server HTTP port | `8080` |

> **Security Note:** The Gemini API key must NEVER be embedded inside Android client source code or APK assets. It is strictly stored in server environment variables.

---

## 2. Running the Ktor Backend
From the project root:
```bash
export GEMINI_API_KEY="your_actual_api_key_here"
cd backend/server
gradle run
```
The server will boot on `http://0.0.0.0:8080`.

---

## 3. Running the Android Application
```bash
gradle assembleDebug
```
The Android client connects to `http://10.0.2.2:8080` when running in the Android emulator, and includes an embedded procedural mystery engine so that single-device developer test mode is 100% playable even without an external server!
