# The Button 2.0 · veebileht Androidi taustaks

Kleebi veebilehe link ning määra see telefoni elavaks taustaks. Punane nupp avab sama veebilehe täisekraanil: saad kerida, linke vajutada ja tekstiväljadele kirjutada. Ülemine punane nupp sulgeb juhtimisvaate ning veeb jääb avakuva ikoonide taha.

## Paigaldamine ja kasutamine

1. Paigalda `downloads/TheButton-v2.0.apk` Androidi telefoni (Android 8.0+). Sama kohaliku testvõtmega allkirjastatud 2.0 saab paigaldada varasema 1.0 peale.
2. Kopeeri brauserist veebilehe link. Ava **The Button → Kleebi kopeeritud link**.
3. Vajuta **Salvesta ja proovi veebilehte**, et kontrollida lehe sobivust.
4. Tule punase nupuga tagasi ning vajuta **Määra veebileht taustaks**. Kinnita valik Androidi taustavalijas.
5. Lülita **Ujuv punane nupp** sisse. Luba rakendusel teiste rakenduste peal kuvamine ning lülita nupp pärast seadete juurest naasmist sisse.
6. Vajuta avakuval punast nuppu, et veebilehte kasutada. Juhtimisvaate punane nupp viib tagasi eelmisele ekraanile.

Ühe vajutuse vaiketegevus on **Veeb sisse / välja**, topeltvajutus laadib lehe uuesti ja pikk vajutus peidab nupu. **Juhtimine** võimaldab sidumisi, suurust ja läbipaistvust muuta. Nuppu saab lohistada ning äpist või teavitusest välja lülitada. Peidetud nupu saab äpist uuesti sisse lülitada. Juhtimisvaates asendab ujuvat nuppu alati nähtav ülemine sulgemisnupp.

Lingita saab proovida sisseehitatud võrguühenduseta näidislehte. Pärast selle valimist saab ka näidise taustaks määrata.

## Kuidas see töötab

`LiveWallpaper` loob privaatse virtuaalse ekraani ja `Presentation`-akna, mille WebView renderdub otse Androidi taustapinna sisse. See on veebileht, mitte veebilehest tehtud pilt. Tavalises taustarežiimis saab avakuva ikoone kasutada.

Tekstisisestuseks avatakse `WebActivity`: Androidi fookustatav täisekraaniaken. `WebSession` liigutab sama WebView'd tausta ja juhtimisvaate vahel, säilitades tavalisel režiimivahetusel lehe JavaScripti oleku, vormisisu ning kerimise. Tausta ja juhtimisvaate erinev kõrgus võib responsive-lehe paigutust muuta.

Peidetud lehe töö ja taimerid peatatakse. Kui lehte kaks minutit kusagil ei kuvata, vabastatakse WebView mälu; järgmine avamine taastab navigatsiooniajaloo ja laadib dokumendi uuesti. Vormide ja JavaScripti ajutine olek ei säili pärast seda, protsessi lõpetamist või telefoni taaskäivitamist. Salvestatud algne URL ja nupu seadistus säilivad. Pärast telefoni taaskäivitamist tuleb ujuv nupp äpist uuesti sisse lülitada.

## Ühilduvus

- Vajalikud on elavate taustade tugi ja Android System WebView. Tootja võib piirata ujuvaid nuppe ning taustateenuseid.
- Lehe tugi sõltub veebist. Mõned sisselogimised, hüpikaknad, DRM-video ja brauseri erifunktsioonid ei pruugi WebView's töötada.
- Failide allalaadimine/üleslaadimine, kaamera, mikrofon ning asukohaload pole selles versioonis toetatud. Nende jaoks kasuta tavalist brauserit.
- HTTP ja HTTPS aadressid on lubatud; ilma protokollita domeenile lisatakse HTTPS. Sertifikaadivigu ei eirata. Kohalike failide, `content://` ja natiivse JavaScripti silla kaudu seadmele ligipääsu ei anta.
- Veebileht saab tavalise veebipäringu ning võib kasutada küpsiseid ja kohalikku veebisalvestust. Rakendus ei saada linki eraldi analüütikateenusele. Brauseri sisselogimised ei kandu automaatselt sellesse WebView'sse.

## Ehitamine

Native Java projekt, compile/target SDK 35, AGP 8.8.2. Ava kaust Android Studios ning kasuta ühilduvat Gradle 8.10.2 installatsiooni ja JDK 17+. Gradle wrapper pole projektiga kaasas.

Windowsis saab ehitada otse ametlike Android SDK tööriistadega:

```powershell
.\build-apk.ps1 -JdkPath 'C:\Program Files\Java\jdk-21' -PlatformPath '<SDK>\platforms\android-35' -BuildToolsPath '<SDK>\build-tools\35.0.0'
.\test.ps1
.\package-downloads.ps1
```

Algses töökaustas kasutab ehitusskript vaikimisi juba alla laaditud `.tools` SDK-faile. Teise arvutisse kopeerides anna enda SDK asukohad. Ehituse väljund on `app/build/manual/TheButton-debug.apk`. Pakendamine loob APK ning lähtekoodi ZIP-i `downloads` kausta; SDK, build-vahemälu ja allkirjastamisvõti ZIP-i ei kuulu.

APK on kohaliku testvõtmega allkirjastatud debug-versioon. Play poe jaoks tuleb teha eraldi väljalase ja täita foreground-teenuse avaldamise nõuded.

## Kontrollid

Vt [TESTING.md](tests/TESTING.md). URL-i kontrolli JVM-testid asuvad `tests/ee/thebutton/WebAddressTest.java` failis. `tests/fixture.html` ja `tests/FixtureServer.java` annavad kohaliku veebilehe sisestuse, JavaScripti, kerimise ja navigatsiooni kontrollimiseks.

Tehnilised alusdokumendid: [Android WebView](https://developer.android.com/develop/ui/views/layout/webapps/webview), [WallpaperService](https://developer.android.com/reference/android/service/wallpaper/WallpaperService), [Presentation](https://developer.android.com/reference/android/app/Presentation), [DisplayManager](https://developer.android.com/reference/android/hardware/display/DisplayManager).
