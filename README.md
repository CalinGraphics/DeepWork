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

## Telefon ↔ PC (același Wi‑Fi) — pași concreți

1. **Pe PC (Windows)** pornește companionul desktop (server WebSocket pe **8080**):
   ```powershell
   cd path\to\DeepWork
   .\gradlew.bat :desktop:run
   ```
   Lasă fereastra deschisă; în UI ar trebui să vezi status de tip „Server ready” / URL de pairing.

2. **Află IP-ul PC-ului în aceeași rețea Wi‑Fi** ca telefonul:
   - Deschide **PowerShell** sau **cmd** și rulează: `ipconfig`
   - Caută adaptorul **Wi‑Fi** (Wireless LAN) și notează **IPv4 Address** (ex. `192.168.0.42`).

3. **Verifică rețeaua:** telefonul și PC-ul trebuie să fie pe **același SSID** (aceeași rețea Wi‑Fi), nu „Guest” pe unul și principal pe altul, dacă acestea sunt izolate.

4. **Firewall Windows** (dacă nu se conectează): permite conexiuni **inbound** pe portul **8080** pentru rețea privată, sau testează o dată cu firewall-ul oprit doar ca diagnostic.

5. **Pe Android:** pornește aplicația **DeepWork** → meniu (drawer) → **Companion PC** (sau din Setări butonul de împerechere).

6. În câmpul de adresă introduci **IP-ul de la pasul 2** (ex. `192.168.0.42`) și apeși **Conectează**. Nu pune `http://` în față; clientul folosește WebSocket către `ws://IP:8080/deepwork`.

7. **Dacă tot nu merge:** ping între dispozitive (din PC: `ping IP_telefon` dacă răspunde la ping) sau încearcă varianta **USB** cu:
   ```text
   adb reverse tcp:8080 tcp:8080
   ```
   și în app adresa **`127.0.0.1`**.

`network_security_config` permite trafic **cleartext** pentru LAN/USB (vezi `app\src\main\res\xml\network_security_config.xml`) — util la proiect academic; pentru producție ai folosi **WSS**.

---

## Senzori pe telefon (gesturi)

Implementare: `data/local/.../SensorRepositoryImpl.kt`.

- **Accelerometru:** față în jos, shake, înclinare (pitch).
- **Giroscop** (`Sensor.TYPE_GYROSCOPE`): răsuciri stânga/dreapta pe axa Z și acumulare pentru ~**360°** într-o singură mișcare de rotire.
- Emulatorul uneori **nu expune giroscop**; testează pe **telefon fizic** pentru gesturi complete.

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
