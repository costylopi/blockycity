# Block City — mod Minecraft Java în stil GTA (Fabric, MC 1.21.1)

Mod nou, în director separat. **Nu modifică** `~/.minecraft` și nu atinge fișierele jocului.
Toate texturile sunt originale (generate de `tools/gen_assets.py`); nu există nimic din GTA V.

## Ce conține
- **Mașini** conducibile (W/S accelerare/frână-marșarier, A/D viraj, click dreapta = urcare, Shift = coborâre), 2 locuri, cățărare automată de 1 bloc, lovire pietoni, HP + explozie.
- **Pietoni** (AI de bază: se plimbă, se uită la jucător, fug când sunt loviți), apar pe trotuare.
- **Wanted 0–5 stele** + **poliție** (țintește doar jucători căutați; corp la corp + foc la distanță; dispare când wanted scade).
- **Pistol** (hitscan, fără muniție), **Car Key** (spawnează o mașină).
- **Minimap rotitor** (stânga-jos), stele wanted (dreapta-sus), vitezometru, cameră third-person automată.
- **Generator de oraș** din blocuri vanilla: străzi cu marcaje, trotuare, clădiri, felinare.
- **Multiplayer**: funcționează pe server dedicat/LAN (fizica mașinii rulează pe clientul șoferului, ca la barcă; wanted e sincronizat prin pachet Fabric).

## Cerințe
- JDK **21** (cu `javac`, nu doar JRE) • Gradle **8.10+** • conexiune la internet la primul build.

## Build (pas cu pas)
1. `cd blockcity`
2. Dacă nu ai wrapper: `gradle wrapper --gradle-version 8.10.2` (o singură dată), apoi folosește `./gradlew` (Windows: `gradlew.bat`).
3. `./gradlew build` → jar-ul apare în `build/libs/blockcity-0.1.0.jar` (nu folosi `-sources`).
4. Test fără să-ți atingi instalarea: `./gradlew runClient` — pornește Minecraft într-un director separat `./run`.
5. (Opțional) regenerezi texturile: `python3 tools/gen_assets.py`.

## Instalare într-un profil separat (instalarea existentă rămâne neatinsă)
1. Instalează **Fabric Loader** pentru 1.21.1 din https://fabricmc.net/use/installer/ — în installer bifează *Create profile* și, în launcher, la profilul nou setează **Game directory** = un folder nou, ex. `~/mc-blockcity`.
2. Pune în `~/mc-blockcity/mods/`: `blockcity-0.1.0.jar` + **Fabric API 0.115.0+1.21.1** (sau versiunea 1.21.1 curentă).
3. Pornește profilul. Configul apare în `~/mc-blockcity/config/blockcity.json`.
4. Pe server dedicat: aceleași două jar-uri în `mods/` (clienții au nevoie de ele și ei).

## Primele 5 minute în joc (test treptat)
1. Lume nouă Creative → `/blockcity city` (sau `/blockcity city 4`) — generează orașul în jurul tău (OP nivel 2).
2. Din tabul **Tools** ia **Car Key** și **Pistol**; click dreapta pe asfalt cu cheia, sau folosește mașinile din oraș.
3. Click dreapta pe mașină = urci. Verifică: accelerare, viraj, marșarier, Shift = cobori.
4. Lovește un pieton (sau `/blockcity wanted 2`) → apar polițiști; minimap-ul arată puncte roșii/albastre.
5. Aștepți ~30 s fără crime → stelele scad.

## Configurare — `config/blockcity.json`
Viteză/accelerare/viraj/HP mașină, damage/rază/cooldown pistol, wanted on/off, timp de scădere, număr maxim poliție, pietoni on/off și număr, third-person la intrare, minimap on/off + mărime + zoom, mărimea orașului. Repornește jocul după modificări.

## Limitări cunoscute (v0.1)
- Neverificat în Minecraft real în momentul livrării (vezi mesajul de livrare): posibile mici erori de API de corectat la primul `gradle build`.
- Clădirile sunt cochilii goale (fără etaje/scări); pietonii nu urmează strict trotuarele; poliția nu are mașini.
- Nu există bani, misiuni, magazine; „iluminare/umbre realiste" = cele vanilla (pentru shadere folosește Iris + un shaderpack).
- Animații: cele vanilla de umanoid (mers, braț cu pistol); roțile mașinii nu se rotesc.
- Pe server, fizica mașinii e „client-authoritative" (acceptabil pentru prietenii pe LAN; nu e anti-cheat).
