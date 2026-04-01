# DeepWork

**Text scurt pentru GitHub (About / descriere repo):**  
*Aplicație Android pentru sesiuni de focus (Pomodoro), taskuri, statistici și un companion pe PC legat prin rețea locală — Kotlin, Compose, Room.*

---

DeepWork e o aplicație de productivitate pe care am construit-o ca să țin sesiuni de lucru concentrate: timer, listă de taskuri, câteva statistici (streak, XP, heatmap) și, dacă vrei, un ferestrău pe laptop care se leagă de telefon prin Wi‑Fi sau USB. Pe telefon folosești interfața principală; pe PC rulează un „companion” care ascultă pe portul 8080 și arată starea sesiunii. Poți folosi și gesturi (accelerometru + giroscop) ca să declanșezi acțiuni fără să tot apeși pe ecran.

Interfața e întunecată, cu accente indigo și teal; proiectul e împărțit pe module (domain, date locale/remote, feature-uri separate pentru timer, tasks, analytics, setări, pc-remote).

---

## Ce poți face în app

- **Timer** — sesiuni cu durată reglabilă (5–120 min), pauză, reluare, sesiuni salvate local.
- **Tasks** — adaugi taskuri, le bifezi, alegi unul ca focus pentru timer.
- **Analytics** — streak, focus score, XP, heatmap pe zile, realizări.
- **Setări** — durata implicită a sesiunii, onboarding, legătură spre companion.
- **Companion PC** — introduci IP-ul laptopului și te conectezi la serverul WebSocket; poți folosi și USB cu `adb reverse` dacă e nevoie.
- **Desktop** — aplicație Compose Desktop pe Windows (sau JVM) cu același branding, server Ktor pe `/deepwork`.

---

## Ce ai nevoie instalat

- **JDK 17**
- **Android Studio** (sau un mediu compatibil cu Gradle-ul din proiect)
- Pentru companionul desktop: același JDK; pe Windows comenzile sunt de mai jos.

---

## Android — build și APK

**Din Android Studio:** deschizi folderul `DeepWork`, aștepți sync, apoi Run pe modulul `app` (telefon sau emulator).

**Din terminal (PowerShell), în folderul proiectului:**

```powershell
cd D:\Facultate\DeepWork
.\gradlew.bat :app:assembleDebug
```

APK debug: `app\build\outputs\apk\debug\app-debug.apk` — îl poți copia pe telefon sau instala cu `adb install -r app\build\outputs\apk\debug\app-debug.apk` dacă telefonul e conectat.

---

## Desktop — cum pornești companionul pe laptop

```powershell
cd D:\Facultate\DeepWork
.\gradlew.bat :desktop:run
```

Lasă fereastra pornită; serverul ascultă pe **8080**. În interfața desktop ar trebui să vezi status de tip „server ready” sau URL de pairing.

Alte variante (JAR, pachet): `packageUberJarForCurrentOS` / `packageDistributionForCurrentOS` — vezi task-urile Compose Desktop în Gradle.

---

## Telefon + laptop, fără cablu (Wi‑Fi) — pașii pe care îi faci tu

Asta e fluxul când vrei să scoți cablulul USB și să folosești telefonul din cameră, cu gesturi (giroscop / accelerometru), iar laptopul stă pe birou cu companionul pornit.

1. **Instalezi aplicația pe telefon** o singură dată (din Android Studio Run, sau `adb install` cu cablul, sau copiezi APK-ul pe telefon). După ce e instalată, **nu mai ai nevoie de cablu** pentru folosirea normală.

2. **Laptopul și telefonul pe același Wi‑Fi** — același rețea (ex. „Acasă” pe ambele), nu „Guest” pe unul și principal pe altul dacă acestea sunt izolate.

3. **Pe laptop** pornești companionul: `.\gradlew.bat :desktop:run` și îl lași deschis.

4. **Află IP-ul laptopului** în acea rețea: în PowerShell rulezi `ipconfig` și caută la **Wireless LAN adapter Wi‑Fi** câmpul **IPv4 Address** (ex. `192.168.1.7`). Dacă IP-ul se schimbă după ce te reconectezi la Wi‑Fi, îl verifici din nou.

5. **Firewall Windows:** dacă telefonul nu se conectează, permite trafic **inbound** pe portul **8080** pentru rețea privată, sau testezi o dată cu firewall-ul oprit doar ca să vezi dacă asta era problema.

6. **Pe telefon** deschizi DeepWork → meniu (drawer) → **Companion PC** (sau din Setări butonul de împerechere). În câmp pui **doar IP-ul** (ex. `192.168.1.7`), fără `http://`, apoi **Conectează**.

7. **Timer** — pornești o sesiune; gesturile (telefon cu fața în jos, shake, răsuciri după giroscop etc.) sunt procesate pe telefon și, dacă ești conectat, se trimit și către desktop.

8. **Dacă nu merge:** verifică că IP-ul e corect, că desktopul rulează, că portul 8080 nu e blocat; varianta cu **USB** rămâne pentru debugging: `adb reverse tcp:8080 tcp:8080` și în app adresa **`127.0.0.1`**.

**Notă tehnică:** WebSocket-ul merge pe `ws://IP:8080/deepwork`.

---

## Senzori (telefon fizic)

Gestiunile sunt în `data/local/.../SensorRepositoryImpl.kt`: accelerometru (față în jos, shake, înclinare) și giroscop pentru răsuciri și rotație ~360°. Emulatorul adesea nu are giroscop; pentru demo folosește telefonul real.

---

## Client WebSocket pe Android

Motorul Ktor folosește **OkHttp** (`ktor-client-okhttp`), nu `ktor-client-android`, pentru că engine-ul Android nu suportă WebSocket.

---

## Structură module (pe scurt)

```
app/                 # Android, Hilt, navigare
core/ui/             # Temă Compose
domain/              # Modele, use case-uri
data/local/          # Room, DataStore
data/remote/         # Client WebSocket
feature/             # timer, tasks, analytics, settings, pc-remote
desktop/             # Companion JVM + server Ktor
docs/                # mockup-uri, prezentări
```

---

## Verificare

```powershell
.\gradlew.bat check
```

---

Proiect făcut pentru universitate; înainte de publicare comercială ai roti cheile, endpoint-urile și ai folosi WSS în loc de cleartext pe rețea.
