# Extended Persistence-Based Conway’s Game of Life

## Panoramica

Il sistema modella una versione estesa del [Game of Life (GOL)](https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life) come una serie di configurazioni cellulari in evoluzione. Il progetto fornisce un'implementazione base del GOL, e il suo scopo è estenderla.

# Cell

Nel gioco base fornito, le cell seguono le regole standard del GOL. L'implementazione estesa arricchisce questo modello di base introducendo tipi di cell specializzati con comportamenti metabolici distinti. Alcune cell specializzate tollerano condizioni di vicinato diverse, prosperando in isolamento o in ambienti affollati. Inoltre, le cell scambiano energia con i vicini, rappresentando punti vita o risorse che influenzano la loro evoluzione.

# Board

La board base consiste in una griglia che contiene un insieme di tile, dove ogni tile contiene una cell. Nella sua forma estesa, la board ha tile interattivi, capaci di assorbire o donare energia alle cell. Queste interazioni possono influenzare i comportamenti delle cell, ed è necessario eseguire analisi per valutare le proprietà e gli stati in evoluzione della board.

# Game

Nella sua versione base, il gioco opera simulando l’evoluzione standard del GOL, gestendo le interazioni tra cell in base agli stati dei vicini a partire da una configurazione iniziale. Tutte le tile contengono una cell che rimane la stessa durante la simulazione, alternando il suo stato tra viva e morta. L’implementazione estesa introduce eventi globali che influenzano tutte le tile e le cell sulla board in generazioni specifiche. Questi eventi, che includono scenari differenti, alterano radicalmente lo stato della board e le dinamiche delle cell. All'inizio di ogni generazione, lo stato di aliveness del vicinato viene calcolato per valutare il prossimo stato di aliveness di ciascuna cell. Sebbene il nuovo stato di aliveness e mood della cell sia applicato solo nella generazione successiva, i cambiamenti ai life points hanno effetto immediato.

# Persistenza

Lo stato del GOL esteso deve essere memorizzato persistentemente e recuperabile utilizzando `JPA`/`Hibernate` con un database in-memory `H2`. Le entità vengono salvate e possono essere ricaricate successivamente, consentendo l'ispezione o la ripresa della simulazione a una generazione specifica.

---

## Requisiti Dettagliati

Il progetto Extended GOL fornisce fin dall'inizio un'implementazione del GOL base da estendere.

### Codice fornito

Classi GOL:

- `ExtendedGameOfLife`: classe facade in cui i metodi richiesti per il testing sono lasciati come stub.
- `Cell`: rappresenta una cell in una generazione.
- `Tile`: rappresenta una tile sulla board.
- `Board`: rappresenta una board di gioco semplice e statica.
- `Game`: definisce il contesto della simulazione.
- `Generation`: rappresentazione dello stato di gioco.
- `Interactable` e `Evolvable`: interfacce da implementare da alcune entità.
- `CellType`, `CellMood` e `EventType`: `enum` che includono le tipologie previste di tipi cell, mood cell e tipi evento nel gioco esteso.
- `Coord`: oggetto valore `JPA` embeddable e hashable che incapsula coordinate intere `(x, y)`, con override di `equals()` e `hashCode()` per l'utilizzo come chiave in mappe e set.
- `ExtendedGameOfLife`: classe di eccezione personalizzata per l'estensione GOL.

- Persistenza:

  - `JPAUtil`: utility basata su singleton per gestire l'`EntityManagerFactory`
  - `GenericExtGOLRepository`: classe di repository generica da implementare per classi repository specifiche.

- Configurazione:
  - `persistence.xml`: configura Hibernate con database `H2` in memoria.
  - `pom.xml`: include le dipendenze per `Hibernate ORM`, `JPA`, `H2` e `JUnit 4`.

---

## R1 Cell

### Comportamento base

Nel gioco base fornito, le cell seguono le regole classiche del GOL:

- **Sopravvivenza:** una cell viva con due o tre vicini vivi sopravvive alla generazione successiva.
- **Morte per sottopopolazione:** una cell viva con meno di due vicini vivi muore.
- **Morte per sovrappopolazione:** una cell viva con più di tre vicini vivi muore.
- **Rinascita:** una cell morta torna viva quando ha esattamente tre vicini vivi (indipendentemente dal suo `cellType`).

La board è finita: le cell agli angoli hanno 3 vicini, quelle ai bordi 5, e le cell centrali 8.

Per modellare le interazioni con altre cell e con l'ambiente, le cell devono implementare un'interfaccia chiamata `Evolvable`. Il progetto fornisce la versione base delle cell tramite la classe `Cell`, che implementa `Evolvable` e contiene un override del metodo `evolve()` che supporta le regole classiche del GOL.

### Comportamenti estesi

A ogni generazione, il metodo `evolve()` aggiorna i `lifePoints` della cell in base al vicinato e alle interazioni.

- La morte li decrementa di uno
- Le condizioni di sopravvivenza li incrementano di uno
- La rinascita li resetta a 0

Oltre al rispetto delle regole classiche del GOL, una cell deve avere `lifePoints` maggiori o uguali a 0 per poter essere viva. Tuttavia, le cell in condizioni letali secondo il GOL muoiono anche se hanno livelli energetici positivi. Le cell morte non aggiornano i propri `lifePoints`.

Ogni `Tile` ha un attributo `lifePointModifier`, con valore di default `0`, e implementa l'interfaccia `Interactable`. Le `Tile` possono avere impatti differenti sui `lifePoints` della `Cell`, a seconda del valore di `lifePointModifier`: se positivo aggiungono il valore corrente ai `lifePoints` della cell, se negativo sottraggono, e se zero non hanno effetto. Le interazioni con la tile impattano la cell all'inizio di ogni generazione.

#### Tipi di cell specializzate

Ogni cell ha un attributo `lifePoints`, che rappresenta il livello di energia della cell. La classe base `Cell` ha questo attributo impostato al valore di default `0`.  
Il GOL esteso implementa tre diversi tipi di cell come sottoclassi della classe `Cell`:

- `Highlander`: può sopravvivere per tre generazioni consecutive in condizioni letali secondo il GOL.
- `Loner`: prospera in isolamento, spostando la soglia inferiore di sopravvivenza a un solo vicino.
- `Social`: sposta la soglia superiore di sopravvivenza fino a un massimo di 8 vicini.

Tutte le cell hanno un attributo `cellType`, e le cell base sono marcate come `BASIC`.

#### Vampiri e guaritori

Ogni `Cell` può avere tre mood: `NAIVE`, `HEALER`, o `VAMPIRE`. La sequenza delle interazioni delle `Cell` segue un ordine fisso, partendo dall’angolo in alto a sinistra della board e procedendo da sinistra a destra, riga per riga. Le interazioni sono possibili solo tra `Cell` vive. Quando due cell interagiscono, implementando `Interactable`, a seconda dei rispettivi mood, si verificano risultati differenti:

- `HEALER` + `NAIVE`: il `HEALER` genera 1 `lifePoint` per il `NAIVE`.
- `HEALER` + `HEALER`: non succede nulla.
- `HEALER` + `VAMPIRE`: il `VAMPIRE` assorbe 1 `lifePoint` dal `HEALER`.
- `VAMPIRE` + `VAMPIRE`: non succede nulla.
- `VAMPIRE` + `NAIVE`: il `VAMPIRE` assorbe 1 `lifePoint` dal `NAIVE` e lo trasforma in `VAMPIRE`.
- `NAIVE` + `NAIVE`: non succede nulla.

Un `VAMPIRE` morde un non-vampiro solo se quest’ultimo ha attualmente `lifePoints ≥ 0`. Il morso altera istantaneamente i `lifePoints` del bersaglio, mentre la morte e il cambiamento del mood (cioè la trasformazione in `VAMPIRE`) avvengono come sempre nella generazione successiva.

Tutte le cell hanno un attributo `cellMood`. Il mood può cambiare più volte per la stessa cell, a seconda delle sue interazioni con altre `Cell` sulla `Board` e degli eventi che si verificano durante una `Game`.

#### Persistenza

Tutte le classi derivate da `Cell`, così come la classe base, devono essere annotate come entità `JPA`, assicurando che l'intera gerarchia delle cell possa essere salvata e caricata tramite i repository `JPA`. La classe di repository generica fornita `GenericExtGOLRepository<E,I>` deve essere implementata parametrizzandola per `Cell` per fornire le operazioni di base e un metodo `load` in modo che lo stato delle cell sia memorizzato persistentemente e recuperabile tramite `JPA`. Per esempio:

```java
public class CellRepository  extends GenericExtGOLRepository<Cell, Long> {
    public CellRepository()  { super(Cell.class);  }
}
```

Ogni repository deve implementare il metodo load(...) (e qualsiasi query personalizzata) in modo che lo stato completo della board e del gioco possa essere salvato e ricaricato tramite JPA.

---

## R2 Board

### Comportamenti base

La board è una griglia a dimensioni fisse (`M×N`), corrispondente a `M*N` istanze di oggetti `Tile` semplici con coordinate `x` e `y`, ognuna delle quali contiene una singola `Cell`.

### Comportamenti estesi

A ogni generazione, il metodo `evolve()` aggiorna i `lifePoints` della `Cell` in base al suo vicinato e alle interazioni.

- La morte li decrementa di uno
- Le condizioni di sopravvivenza li incrementano di uno
- La rinascita li resetta 0

Oltre al rispetto delle regole classiche del GOL, una cell deve avere `lifePoints` maggiori o uguali a 0 per essere viva. Tuttavia, le cell in condizioni di morte secondo il GOL muoiono anche se hanno livelli energetici positivi. Le cell morte non aggiornano i `lifePoints` altrimenti.

Ogni `Tile` ha un valore `lifePointModifier`, di default `0`, e implementa l'interfaccia `Interactable`. Le `Tile` possono avere impatti diversi sui `lifePoints` della `Cell`, a seconda del valore di `lifePointModifier`: se positivo, aggiungono il valore corrente ai `lifePoints` della cell; se negativo, lo sottraggono; se è zero, non hanno effetto. Le interazioni con la tile impattano la cell all'inizio di ogni generazione.

#### Visualizzazione

La `Board` supporta una visualizzazione basata su stringa tramite il metodo `visualize()`. Ogni `Tile` che ospita una `Cell` morta è rappresentata con uno `0`, mentre le `Cell` vive sono rappresentate con:

- `Cell`: `C`
- `Highlander`: `H`
- `Loner`: `L`
- `Social`: `S`

Questo metodo produce visualizzazioni nel seguente formato:

```
0C00H
L00CS
0H000
C0000
```

### Metodi analitici

La classe `Board` fornisce i seguenti metodi di analisi. Ognuno accetta un'istanza di `Generation` (o un intervallo di generazioni) e restituisce le informazioni richieste:

| Firma del Metodo                                                                         | Descrizione                                                                                                                                     |
| ---------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| `public Integer countCells(Generation gen)`                                              | Restituisce il **numero totale** di cell vive in `gen`.                                                                                         |
| `public Cell getHighestEnergyCell(Generation gen)`                                       | Trova la **singola** cell viva con il maggior numero di `lifePoints`, scegliendo quella più in alto a sinistra in caso di parità.               |
| `public Map<Integer, List<Cell>> getCellsByEnergyLevel(Generation gen)`                  | Raggruppa le **cell vive** per il loro livello attuale di `lifePoints`.                                                                         |
| `public Map<CellType, Integer> countCellsByType(Generation gen)`                         | Conta le cell vive **per** `CellType`. Suggerimento: usare query personalizzate nel repository dedicato.                                        |
| `public List<Cell> topEnergyCells(Generation gen, int n)`                                | Restituisce le **prime `n`** cell vive ordinate per `lifePoints` decrescenti.                                                                   |
| `public Map<Integer, List<Cell>> groupByAliveNeighborCount(Generation gen)`              | Raggruppa le cell vive per il **numero di vicini vivi**.                                                                                        |
| `public IntSummaryStatistics energyStatistics(Generation gen)`                           | Calcola statistiche riassuntive (`count`, `min`, `max`, `sum`, `average`) sui `lifePoints` delle cell vive.                                     |
| `public Map<Integer, IntSummaryStatistics> getTimeSeriesStats(int fromStep, int toStep)` | Restituisce una **serie temporale** di statistiche energetiche, calcolate solo per le cell vive, per ogni generazione da `fromStep` a `toStep`. |

#### Persistenza

La configurazione completa della `Board` deve essere interamente persistente: tutte le entità devono essere annotate con gli opportuni mapping di ID e relazioni.  
La classe di repository generica `GenericExtGOLRepository<E,I>` fornita deve essere implementata parametrizzandola per `Board`, per fornire le operazioni base e un metodo `load` in modo che lo stato della board, incluse le posizioni e gli stati completi delle `Cell` e delle `Tile`, sia memorizzato e recuperabile tramite `JPA`. Per esempio:

```java
public class BoardRepository  extends GenericExtGOLRepository<Board, Long> {
    public BoardRepository()  { super(Board.class);  }
}
```

Ogni repository deve implementare il metodo load(...) (e qualunque query personalizzata) in modo che lo stato completo della board e del gioco possa essere salvato e ricaricato tramite JPA.

## R3 Game

### Comportamenti base

La `Game` orchestra l’evoluzione della `Board` attraverso le `Generation` usando le regole standard del GOL e il comportamento base delle cell.  
Si parte impostando la configurazione iniziale della `Board` e si gestisce l’evoluzione fino al raggiungimento di un numero target di `Generation`. La routine prevede:

1. **Rilevamento vicini**: all'inizializzazione, viene analizzata la `Board` per determinare i vicini di ciascuna cell.
2. **Valutazione del vicinato**: la `Game` fa sì che ogni `Cell` valuti lo stato di aliveness dei suoi vicini alla generazione corrente.
3. **Evoluzione**: la `Game` imposta il nuovo stato di ogni `Cell` secondo le regole GOL per la generazione successiva, in base alla valutazione del vicinato.

### Comportamenti estesi

A ogni generazione, il metodo `evolve()` aggiorna i `lifePoints` della `Cell` secondo il vicinato e le interazioni.

- La morte li decrementa di 1
- Le condizioni di sopravvivenza li incrementano di 1
- La rinascita li resetta a 0

Oltre alle regole classiche del GOL, una cell deve avere `lifePoints` ≥ 0 per essere considerata viva. Tuttavia, una cell in condizioni letali secondo il GOL muore anche se ha energia positiva. Le cell morte non aggiornano i `lifePoints` in altro modo.

Ogni `Tile` ha un attributo `lifePointModifier` (default: `0`) e implementa l’interfaccia `Interactable`. Le `Tile` influenzano i `lifePoints` della `Cell` come segue:

- Se `lifePointModifier` > 0: aggiungono valore
- Se < 0: sottraggono
- Se = 0: nessun effetto

Queste interazioni si applicano all’inizio di ogni generazione.

#### Eventi

La `Game` estesa può attivare eventi globali che impostano lo stato di tutte le `Tile` sulla `Board` per una singola `Generation`. Gli eventi disponibili sono:

- `Cataclysm`: tutte le `Tile` azzerano i `lifePoints` della `Cell` che ospitano.
- `Famine`: tutte le `Tile` assorbono esattamente 1 `lifePoint` dalla `Cell` che ospitano.
- `Bloom`: tutte le `Tile` danno esattamente 2 `lifePoints` alle `Cell` che ospitano.
- `Blood Moon`: i `VAMPIRE` ottengono la capacità di trasformare gli `HEALER` in `VAMPIRE` con il loro morso.
- `Sanctuary`: tutte le `Tile` danno 1 `lifePoint` agli `HEALER`, e tutti i `VAMPIRE` vengono trasformati immediatamente in `NAIVE`.

L'evento `Sanctuary` è l'unico caso che prevede un cambiamento di mood istantaneo delle cell, all'inzio della `Generation` prima di ogni interazione delle cell.
Ogni evento ha effetto solo su una singola generazione. Ogni generazione può contenere al massimo un evento. Gli eventi impattano tutte le `Tile` all’inizio della generazione, influenzando l’evoluzione della `Cell` associata.

#### Persistenza

Il livello di persistenza deve catturare ogni snapshot di `Generation` (layout della board e stati delle cell) insieme alle relative entità `Game`.  
Al caricamento, `loadEvents()` è responsabile della query e del riaggancio di ciascun evento alla `Generation` corrispondente nella `Game`, ricostruendo così la timeline completa e permettendo replay o ispezione.  
La classe generica fornita `GenericExtGOLRepository<E,I>` deve essere implementata parametrizzandola per `Game`, per fornire operazioni base e un metodo `load` in modo che lo stato della `Game`, incluse tutte le sue `Generation` e i relativi eventi, sia persistente e recuperabile tramite `JPA`. Esempio:

```java
public class GameRepository  extends GenericExtGOLRepository<Game, Long> {
    public GameRepository()  { super(Game.class);  }
}
```

Ogni repository deve implementare il metodo load(...) (e qualsiasi query personalizzata) per assicurare che lo stato completo della Board e della Game possa essere salvato e ricaricato tramite JPA.
