# GravityStaffMode

Plugin Spigot per la gestione della staff mode e del vanish. Include strumenti per moderare i giocatori ed e configurabile in italiano.

Compatibile con server Minecraft dalla 1.8 alla 1.21.11.

## Download

Scarica l'ultima versione dalla pagina [Releases](https://github.com/ImApon877/GravityStaffMode/releases/latest) (`GravityStaffMode.jar`).

## Novita

### 1.1.1
- Permessi con nomi coerenti `gravitystaffmode.<nome>` (vedi sotto). I vecchi `gravityvanish.*` continuano a funzionare.

### 1.1.0
Versione provata con giocatori bot su Paper 1.8.8 e 1.21.11, compreso un crash del server (40/40 controlli superati su entrambe).

- **Inventario dello staff salvato su disco**: se il server crasha con uno staffer in staff mode, l'inventario vero viene restituito.
- **Freeze**: su 1.8 non bloccava niente e dalla 1.9 si attivava e disattivava con lo stesso click. Ora blocca camminata e salti (girare la testa e cadere restano permessi).
- **Invsee in staff mode in sola lettura**: prima gli oggetti presi al giocatore sparivano; niente piu oggetti fantasma nel client.
- **Rientro in staff mode**: gli strumenti tornano anche se un plugin hub svuota l'inventario all'ingresso.
- **/fly** non si spegne piu da solo quando un altro plugin ha dato il permesso di volo.
- **Freeze e uscita**: lo staff viene avvisato se un giocatore freezato esce, con comando opzionale (`freeze.comando-uscita`).
- Gamemode della staff mode configurabile (default `SURVIVAL` invece di `CREATIVE`).
- Dati dei giocatori in `dati.yml`; file `.yml` letti e scritti in UTF-8 con riparazione automatica dei caratteri rovinati su 1.8.

## Funzionalita

- Vanish: nasconde lo staff ai giocatori senza permesso.
- Staff mode: salva inventario, armatura, gamemode e stato del volo, quindi fornisce gli strumenti di moderazione.
- InvSee: apre l'inventario di un giocatore online.
- Freeze: blocca il movimento, le interazioni e i teletrasporti di un giocatore.
- Random TP: teletrasporta a un giocatore online casuale.
- Volo: attivabile solo durante la staff mode.
- I messaggi di ingresso e uscita dei giocatori in vanish possono essere nascosti.
- I messaggi, i prefissi nella tab e le opzioni sono configurabili.

## Installazione

1. Scarica `GravityStaffMode.jar` dalla pagina Releases.
2. Spostalo nella cartella `plugins` del server.
3. Riavvia il server.
4. Modifica `plugins/GravityStaffMode/config.yml` e `messages.yml` se necessario.

## Comandi e permessi

| Comando | Descrizione | Permesso |
| --- | --- | --- |
| `/vanish` | Attiva o disattiva il vanish. | `gravitystaffmode.vanish` |
| `/staffmode` | Attiva o disattiva la staff mode. | `gravitystaffmode.staffmode` |
| `/invsee <giocatore>` | Apre l'inventario di un giocatore. | `gravitystaffmode.invsee` |
| `/randomtp` | Si teletrasporta da un giocatore casuale. | `gravitystaffmode.randomtp` |
| `/fly` | Attiva o disattiva il volo in staff mode. | `gravitystaffmode.fly` |
| `/gravitystaffmode reload` | Ricarica configurazione e messaggi. | `gravitystaffmode.reload` |

| Permesso | Descrizione |
| --- | --- |
| `gravitystaffmode.see` | Consente di vedere i giocatori in vanish. |
| `gravitystaffmode.staffmode` | Consente anche di vedere i giocatori in staff mode. |

Tutti i permessi sono assegnati agli operatori del server per impostazione predefinita. `gravitystaffmode.*` li concede tutti.

I vecchi nomi `gravityvanish.*` (es. `gravityvanish.use`) restano validi: ogni nuovo permesso concede anche il vecchio, quindi i gruppi gia configurati continuano a funzionare.

## Strumenti staff mode

| Strumento | Azione |
| --- | --- |
| Sacca d'inchiostro | Attiva o disattiva il vanish. |
| Bussola | Teletrasporta da un giocatore casuale. |
| Piuma | Attiva o disattiva il volo. |
| Verga di blaze | Blocca o sblocca il giocatore selezionato. |
| Libro | Apre il menu InvSee; clic destro su un giocatore per aprirne l'inventario. |

## Configurazione

Il file `config.yml` consente di nascondere i messaggi di entrata/uscita in vanish e di personalizzare i prefissi della tab:

```yml
nascondi-messaggi-accesso-uscita: true

tab:
  prefisso-staffmode: '&b[S] &r{prefisso}'
  prefisso-vanish: '&7[V] &r{prefisso}'
```

`{prefisso}` conserva il nome nella tab impostato da altri plugin, ad esempio un plugin dei rank.

Altre opzioni:

```yml
# Gamemode usata in staff mode (SURVIVAL, ADVENTURE, CREATIVE, SPECTATOR).
gamemode-staffmode: SURVIVAL

freeze:
  # Comando eseguito dalla console se un giocatore freezato esce. {giocatore} = nome.
  comando-uscita: 'ban {giocatore} Uscito durante il freeze'
```

Gli stati dei giocatori sono salvati in `dati.yml`, gli inventari dello staff in `inventari-staffmode.yml`: non vanno modificati a mano.

## Compilazione

Richiede Java 8 e Maven.

```bash
mvn package
```

Il file compilato viene creato in `target/GravityStaffMode.jar`.

## Struttura progetto

```text
src/main/java/it/gravitymc/gravitystaffmode/
├── GravityStaffModePlugin.java   avvio, reload, collegamenti
├── Messages.java, Permissions.java, Compat.java, Utf8Yaml.java
├── data/       PlayerStates (dati.yml), StaffInventoryStore (inventari-staffmode.yml)
├── manager/    StaffModeManager, VanishManager, FreezeManager, InvseeManager, StaffTools
├── command/    un comando per classe (VanishCommand, StaffModeCommand, ...)
└── listener/   ConnectionListener, StaffModeListener, InventoryListener, FreezeListener
```
