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
        +submitEvent(GardenEvent)
        +advanceOneDay()
        +snapshot() GardenSnapshot
        +logCurrentState()
        +addPlant(PlantType)
        +getPlantDefinitions() Map
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
        +addPlant(Plant)
        +getPlants() List
        +getAlivePlants() List
        +getDeadPlants() List
        +countAliveByType() Map
        +changeSoilNutrients(int)
        +setAmbientTemperature(int)
    }

    class Plant {
        -int id
        -PlantType type
        -int health
        -int waterLevel
        -PlantStatus status
        -Map~String,Integer~ parasiteAge
        -String deathReason
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
        +plants List~PlantView~
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
