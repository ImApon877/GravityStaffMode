# GravityStaffMode

Plugin Spigot per la gestione della staff mode e del vanish. Include strumenti per moderare i giocatori ed e configurabile in italiano.

Compatibile con server Minecraft dalla 1.8 alla 1.21.11.

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

## Compilazione

Richiede Java 8 e Maven.

```bash
mvn package
```

Il file compilato viene creato in `target/GravityStaffMode.jar`.
