# UML Diagrams

---

## 1. Class Diagram

```mermaid
classDiagram
    class GardenSimulationAPI {
        +initializeGarden()
        +getPlants() Map~String,Object~
        +rain(int)
        +temperature(int)
        +parasite(String)
        +getState()
        +getEngine() SimulationEngine
    }

    class SimulationEngine {
        -Garden garden
        -List~GardenModule~ modules
        -GardenConfigLoader configLoader
        -GardenLogger logger
        -SimulationContext context
        -Path configPath
        +initialize()
        +initializeWith(Map~PlantType,Integer~)
        +submitEvent(GardenEvent)
        +advanceOneDay()
        +tickAutonomous()
        +snapshot() GardenSnapshot
        +logCurrentState()
        +addPlant(PlantType)
        +removePlant(String) bool
        +removeDeadPlants() int
        +getPlantDefinitions() Map
        +getRecentLogEntries() List
        +getRecentUserLogEntries() List
        +getLogPath() Path
    }

    class SimulationContext {
        -GardenLogger logger
        -int day
        +getDay() int
        +advanceDay()
        +resetDay()
        +log(String, String, String, String, Garden, String)
    }

    class Garden {
        -List~Plant~ plants
        -int soilNutrients
        -int ambientTemperature
        -int lastObservedTemperature
        -int lastEventRawTemperature
        +addPlant(Plant)
        +removePlant(Plant) bool
        +removePlantByName(String) bool
        +getPlants() List
        +getAlivePlants() List
        +getDeadPlants() List
        +countAliveByType() Map
        +changeSoilNutrients(int)
        +setAmbientTemperature(int)
        +getLastObservedTemperature() int
        +getLastEventRawTemperature() int
    }

    class Plant {
        -int id
        -PlantType type
        -int health
        -int waterLevel
        -PlantStatus status
        -Map~String,Integer~ parasiteAge
        -String deathReason
        -int boardSlot
        +getBoardSlot() int
        +setBoardSlot(int)
        +infest(String) bool
        +ageParasitesAndGetTreatmentReady() List
        +treatParasite(String) bool
        +damage(int, String)
        +heal(int)
        +addWater(int)
        +consumeDailyWater()
        +evaluateWaterStress()
        +evaluateTemperature(int)
        +applyParasiteDamage()
    }

    class PlantType {
        <<enumeration>>
        ROSE
        TOMATO
        LETTUCE
        CACTUS
        SUNFLOWER
        BASIL
        PEPPER
        LAVENDER
        -String displayName
        -int waterRequirement
        -int minTemperature
        -int maxTemperature
        -List~String~ parasites
        -int recoveryRate
        +fromName(String) PlantType
    }

    class PlantStatus {
        <<enumeration>>
        HEALTHY
        STRESSED
        INFESTED
        RECOVERING
        DEAD
    }

    class GardenModule {
        <<interface>>
        +getName() String
        +handleEvent(Garden, GardenEvent, SimulationContext)
        +dailyUpdate(Garden, SimulationContext)
    }

    class WateringSystem {
        +handleEvent(Garden, GardenEvent, SimulationContext)
        +dailyUpdate(Garden, SimulationContext)
    }
    class TemperatureControlSystem {
        +handleEvent(Garden, GardenEvent, SimulationContext)
        +dailyUpdate(Garden, SimulationContext)
    }
    class PestControlSystem {
        +handleEvent(Garden, GardenEvent, SimulationContext)
        +dailyUpdate(Garden, SimulationContext)
    }
    class FertilizerSystem {
        +handleEvent(Garden, GardenEvent, SimulationContext)
        +dailyUpdate(Garden, SimulationContext)
    }

    class GardenEvent {
        <<interface>>
        +name() String
        +value() String
    }
    class RainEvent { +amount int }
    class TemperatureEvent { +temperature int }
    class ParasiteEvent { +parasite String }
    class ManualEvent { +eventName String }

    class GardenLogger {
        -Path logPath
        +reset()
        +log(int, String, String, String, String, Garden, String)
        +recentEntries() List~LogEntry~
        +recentUserEntries() List~LogEntry~
        +getLogPath() Path
    }

    class GardenConfigLoader {
        +load(Path) Garden
    }

    class GardenSnapshot {
        +day int
        +alivePlants int
        +deadPlants int
        +soilNutrients int
        +ambientTemperature int
        +outsideTemperature int
        +plants List~PlantView~
    }

    class PlantView {
        +name String
        +type String
        +health int
        +waterLevel int
        +status String
        +activeParasites String
        +deathReason String
        +slotIndex int
    }

    GardenSimulationAPI --> SimulationEngine
    SimulationEngine --> Garden
    SimulationEngine --> GardenLogger
    SimulationEngine --> SimulationContext
    SimulationEngine --> GardenConfigLoader
    SimulationEngine --> "0..*" GardenModule
    SimulationEngine --> GardenEvent
    SimulationContext --> GardenLogger
    Garden --> "0..*" Plant
    Plant --> PlantType
    Plant --> PlantStatus
    GardenModule <|.. WateringSystem
    GardenModule <|.. TemperatureControlSystem
    GardenModule <|.. PestControlSystem
    GardenModule <|.. FertilizerSystem
    GardenEvent <|.. RainEvent
    GardenEvent <|.. TemperatureEvent
    GardenEvent <|.. ParasiteEvent
    GardenEvent <|.. ManualEvent
    SimulationEngine ..> GardenSnapshot : creates
    GardenSnapshot --> "0..*" PlantView
```

---

## 2. Plant State Diagram

Tracks the lifecycle of every `Plant` object from birth to death.

```mermaid
stateDiagram-v2
    [*] --> HEALTHY : new Plant(type)

    HEALTHY --> STRESSED : health drops below 70\n(dehydration / heat / cold)
    HEALTHY --> INFESTED : infest(parasite) called\n& plant is susceptible

    STRESSED --> HEALTHY : heal() called\nhealth ≥ 85
    STRESSED --> RECOVERING : heal() called\nhealth 70–84
    STRESSED --> INFESTED : infest(parasite) called
    STRESSED --> DEAD : health reaches 0

    INFESTED --> RECOVERING : treatParasite() called\n& parasiteAge ≥ TREATMENT_DAYS\n& health ≥ 70
    INFESTED --> STRESSED : treatParasite() called\n& health < 70
    INFESTED --> DEAD : health reaches 0\n(ongoing parasite damage)

    RECOVERING --> HEALTHY : heal() called\nhealth ≥ 85
    RECOVERING --> STRESSED : further damage
    RECOVERING --> DEAD : health reaches 0

    DEAD --> [*]

    note right of INFESTED
        Parasites apply damage every
        daily-update cycle.
        Treatment removes parasite
        but does NOT restore health.
    end note
```

---

## 3. Use Case Diagram

```mermaid
flowchart LR
    GS(["🖥️ Grading Script"])
    HG(["👤 Human Gardener"])

    subgraph System ["Computerized Garden System"]
        UC1["Initialize Garden"]
        UC2["Simulate Rain"]
        UC3["Set Temperature"]
        UC4["Trigger Parasite"]
        UC5["Get Garden State"]
        UC6["Add Plant Manually"]
        UC7["Advance Day"]
        UC8["View Live Dashboard"]
        UC9["Reset Garden"]
        UC10["Remove Plant(s)"]
        UC11["Remove All Dead"]
        UC12["Open Log File"]
    end

    GS --> UC1
    GS --> UC2
    GS --> UC3
    GS --> UC4
    GS --> UC5

    HG --> UC1
    HG --> UC2
    HG --> UC3
    HG --> UC4
    HG --> UC5
    HG --> UC6
    HG --> UC7
    HG --> UC8
    HG --> UC9
    HG --> UC10
    HG --> UC11
    HG --> UC12
```

---

## 4. Sequence Diagram — Event Processing

```mermaid
sequenceDiagram
    participant Script as Grading Script / UI
    participant API as GardenSimulationAPI
    participant Engine as SimulationEngine
    participant Modules as GardenModule(s)
    participant Garden as Garden / Plants
    participant Log as GardenLogger

    Script->>API: rain(25) / temperature(95) / parasite("aphid")
    API->>Engine: submitEvent(event)
    Engine->>Log: EVENT_RECEIVED (day N)

    loop handleEvent — each module
        Engine->>Modules: handleEvent(garden, event, ctx)
        Modules->>Garden: update plant/env state
        Modules->>Log: module-specific action log
    end

    loop dailyUpdate — each module
        Engine->>Modules: dailyUpdate(garden, ctx)
        Modules->>Garden: aging, treatment, irrigation, fertilizer
        Modules->>Log: daily module summary
    end

    Engine->>Garden: evaluateWaterStress(); consumeDailyWater()
    Engine->>Log: DAY_COMPLETED (alive/dead counts)
```

---

## 5. Sequence Diagram — getState()

```mermaid
sequenceDiagram
    participant Script as Grading Script
    participant API as GardenSimulationAPI
    participant Engine as SimulationEngine
    participant Garden as Garden
    participant Log as GardenLogger

    Script->>API: getState()
    API->>Engine: logCurrentState()
    Engine->>Garden: getAlivePlants()
    Engine->>Garden: getDeadPlants()
    Engine->>Log: GARDEN_SNAPSHOT (summary)
    loop each alive plant
        Engine->>Log: PLANT_STATUS (type, health, water, status, parasites)
    end
    loop each dead plant
        Engine->>Log: PLANT_DEAD (type, death_reason)
    end
```

---

## 6. Activity Diagram — Daily Simulation Cycle

The control flow of a single `submitEvent(...)` call — the heart of every simulated day,
whether triggered by an API call, a manual "Advance Day" click, or the autonomous timer.

```mermaid
flowchart TD
    A([Event submitted]) --> B{Garden initialized?}
    B -- No --> B1[initialize from garden_config.json]
    B -- Yes --> C
    B1 --> C[advanceDay: day counter += 1]
    C --> D[Log EVENT_RECEIVED]
    D --> E[Each alive plant consumes daily water]
    E --> F[Log DAILY_WATER_USE]

    F --> G[/For each module: handleEvent/]
    G --> H{Module threw?}
    H -- Yes --> H1[Log MODULE_ERROR, continue]
    H -- No --> I[Module updated plant / env state]
    H1 --> J
    I --> J{More modules?}
    J -- Yes --> G
    J -- No --> K[/For each module: dailyUpdate/]

    K --> L[Aging, treatment, irrigation, fertilizer]
    L --> M[Each alive plant: evaluateWaterStress]
    M --> N[Build living status mix + deaths-by-cause]
    N --> O[Log DAY_COMPLETED summary]
    O --> P([Day ends; temperature resets to 72°F])
```

---

## 7. Object Diagram — Runtime Snapshot

A concrete instant on day 4: one healthy Cactus, one infested Rose mid-treatment, and one
dead Lettuce whose board slot has been freed. Illustrates the live object graph the engine
holds and the immutable `GardenSnapshot` the UI renders from.

```mermaid
flowchart TB
    engine["engine : SimulationEngine<br/>day = 4"]
    garden["garden : Garden<br/>soilNutrients = 71<br/>ambientTemperature = 72<br/>lastObservedTemperature = 100"]
    p1["cactus3 : Plant<br/>type = CACTUS<br/>health = 100<br/>status = HEALTHY<br/>boardSlot = 0"]
    p2["rose1 : Plant<br/>type = ROSE<br/>health = 64<br/>status = INFESTED<br/>parasiteAge = {aphid:1}<br/>boardSlot = 1"]
    p3["lettuce2 : Plant<br/>type = LETTUCE<br/>health = 0<br/>status = DEAD<br/>deathReason = 'heat stress'<br/>boardSlot = 2"]
    water["watering : WateringSystem"]
    temp["climate : TemperatureControlSystem"]
    pest["pest : PestControlSystem"]
    fert["fertilizer : FertilizerSystem"]
    snap["snapshot : GardenSnapshot<br/>day = 4, alive = 2, dead = 1"]

    engine --> garden
    engine --> water
    engine --> temp
    engine --> pest
    engine --> fert
    engine -. creates .-> snap
    garden --> p1
    garden --> p2
    garden --> p3
```

---

## 8. Communication Diagram — Event Processing

The same interaction as the event-processing sequence diagram, drawn as a communication
(collaboration) diagram to emphasize object links and the numbered message order.

```mermaid
flowchart LR
    Script(["Grading Script / UI"])
    API["api : GardenSimulationAPI"]
    Engine["engine : SimulationEngine"]
    Ctx["ctx : SimulationContext"]
    Modules["module : GardenModule"]
    Garden["garden : Garden"]
    Log["logger : GardenLogger"]

    Script -->|"1: rain / temperature / parasite"| API
    API -->|"2: submitEvent(event)"| Engine
    Engine -->|"3: advanceDay()"| Ctx
    Engine -->|"4: consumeDailyWater()"| Garden
    Engine -->|"5: handleEvent(garden, event, ctx)"| Modules
    Modules -->|"6: update state"| Garden
    Engine -->|"7: dailyUpdate(garden, ctx)"| Modules
    Engine -->|"8: evaluateWaterStress()"| Garden
    Ctx -->|"9: log(...)"| Log
    Modules -->|"10: log(...)"| Log
```
