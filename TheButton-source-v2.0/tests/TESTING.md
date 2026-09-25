# Kontrollid · 2.0

Kontrollitud kohalikus MuMu Android 15 emulaatoris. Päris telefoni ja erinevate tootjate Androidi versioone pole selles keskkonnas kontrollitud.

- APK kompileerimine, DEX-i loomine ning v2/v3 allkirja verifitseerimine.
- URL-i normaliseerimise 20 sisendjuhtu ja navigatsioonipiirangud: `test.ps1`.
- Kohaliku HTTP-veebilehe sisestamine äpi URL-välja ja avamine juhtimisvaates.
- Veebilehe renderdamine avakuva ikoonide taga. MuMu süsteemis puudub elava tausta valija, seega valiti teenus ainult emulaatori shell-testis. Tavapärane telefonis kuvatav taustavalija vajab seadmetesti.
- Lehe JavaScripti kella käimine taustarežiimis.
- Ujuva nupu foreground-teenus ja teavitus Android 15-s; testis anti õigused emulaatori shellist. Esmakordse õiguse küsimise dialoogid vajavad seadmetesti.
- Punase nupuga avamine avakuvalt ja ülemise punase nupuga tagasipöördumine.
- Tekst `Sailiv_tekst_123` säilis tausta/juhtimisvaate vahetusel ning seda sai täiendada tekstiks `Sailiv_tekst_123_UUS`.
- JavaScripti nupu loendur säilis režiimivahetusel (`Vajutusi: 1`).
- Kerimine, kerimisasendi säilimine ja lingi avamine aadressile `/next`.
- Taustas loodud WebView ümbertõstmine juhtimisvaatesse pärast rakenduse uuesti paigaldamist; tekstisisestus töötas ka selles käivitusjärjestuses.
- Testserveri ühenduse katkestamine ja uuesti laadimine näitas püsivat, arusaadavat laadimisveateadet.

## Korratav kohalik veebitest

```powershell
java tests/FixtureServer.java
adb reverse tcp:8765 tcp:8765
```

Sisesta äppi `http://127.0.0.1:8765`. Server kuulab ainult arvuti loopback-aadressil. Ära kasuta seda aadressi päris veebitaustana pärast testserveri sulgemist.

`WallpaperProbe.java` on ainult selle MuMu süsteemiversiooni shelli testiabivahend, kus pole taustavalijat. See ei kuulu rakenduse APK-sse, ei lisa rakendusele õigusi ning kasutab sisemisi Androidi API-sid, mis võivad teistes süsteemiversioonides erineda.

## Veel kontrollida päris telefonis

- Süsteemne taustavalija, overlay-loa andmine/keeldumine ning teavitusluba.
- Ekraaniklaviatuuri paigutus püstises ja rõhtsas asendis.
- Lukustuskuva, ekraani pööramine, akuoptimeerimine ning süsteemi mälusurve.
- Konkreetse kasutaja veebilehed, sisselogimised, HTTPS-vead ja võrgu kadumine.
- Nupu lohistamine, peitmine ning uuesti sisselülitamine erinevate launcheritega.
