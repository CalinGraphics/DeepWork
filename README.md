# DeepWork

Suite de productivitate **Deep Focus**: aplicație **Android** (Compose) + **companion desktop** (Compose Desktop + Ktor) sincronizate opțional prin **WebSocket** (port **8080**).

## Sumar aplicație

| Componentă | Rol |
|--------------|-----|
| **Timer** | Sesiuni Pomodoro-like, arc progres, integrare cu taskul de focus și sesiuni persistate (Room). |
| **Tasks** | Liste de taskuri, focus pentru timer, XP/streak legate de activitate. |
| **Analytics** | Streak, focus score, realizări (achievements). |
| **Setări** | Durată sesiune (5–120 min), onboarding, legătură către companion. |
| **Companion PC** | Împerechere IP / USB (`adb reverse`), client WebSocket către desktop. |
| **Desktop** | UI companion, server WebSocket, afișare stare sesiune / heatmap / pairing. |

**Tema UI:** fundal întunecat (`#121121`), accente **indigo** `#5C55E8` și **teal** `#00C4D4` (vezi și `docs/design-mockups/`).

**Arhitectură:** proiect modular Gradle — `domain` (modele + use case-uri), `data:local` / `data:remote`, `core:ui` (temă Compose), feature-uri pe verticală (`feature:timer`, `feature:tasks`, …), modul `app` (Hilt, navigare), modul `desktop` (JVM).

---

## Cerințe

- **JDK 17**
- **Android Studio** (sau Android Gradle Plugin compatibil cu proiectul)
- Pentru desktop: același JDK; pe Windows se folosesc task-urile Compose Desktop de mai jos.

---

## Build & run — Android (telefon)

### Din Android Studio

1. Deschide folderul rădăcină al repo-ului (`DeepWork`).
2. Așteaptă **Gradle Sync**.
3. Selectează configurația **Run: `app`**, alege dispozitivul fizic sau emulator → **Run** (▶).

### Din terminal

```powershell
cd path\to\DeepWork
.\gradlew.bat :app:assembleDebug
```

APK debug: `app\build\outputs\apk\debug\app-debug.apk`.

Release (necesită semnare configurată):

```powershell
.\gradlew.bat :app:assembleRelease
```

---

## Build & run — Desktop (PC)

### Rulare rapidă

```powershell
cd path\to\DeepWork
.\gradlew.bat :desktop:run
```

### JAR (Uber JAR pentru OS-ul curent)

```powershell
.\gradlew.bat :desktop:packageUberJarForCurrentOS
```

### Distribuție / installer (OS curent)

```powershell
.\gradlew.bat :desktop:packageDistributionForCurrentOS
```

Output-urile Compose Desktop sunt sub `desktop\build\compose\` (structură generată de plugin).

### Din Android Studio

Panoul **Gradle** → **DeepWork** → **desktop** → **Tasks** → **compose desktop** → `run`.

---

## Telefon ↔ PC (rețea)

1. Pornește **desktop** (`:desktop:run`) — serverul ascultă pe **8080**.
2. **Același Wi‑Fi** sau **USB** cu:
   ```text
   adb reverse tcp:8080 tcp:8080
   ```
3. În aplicația Android, la companion: pentru USB folosește adresa **`127.0.0.1`** după `adb reverse`.

`network_security_config` permite `cleartext` pentru LAN/USB (vezi `app\src\main\res\xml\network_security_config.xml`).

---

## Structură module (rezumat)

```
app/                 # Entry Android, Hilt, NavHost, drawer
core/ui/             # Temă Compose DeepWork (culori, tipografie)
core/common/         # Utilitare comune
domain/              # Modele, repository interfaces, use case-uri
data/local/          # Room, DataStore, implementări repository
data/remote/         # WebSocket client Ktor
feature/timer|tasks|analytics|settings|pc-remote/
desktop/             # Companion Compose Desktop + server Ktor
docs/design-mockups/ # Referințe HTML mockup (nu intră în build)
```

---

## Verificare locală

```powershell
.\gradlew.bat check
```

---

## Licență / notă

Proiect academic; adaptează cheile și endpoint-urile din `build.gradle.kts` / `BuildConfig` înainte de publicare.
