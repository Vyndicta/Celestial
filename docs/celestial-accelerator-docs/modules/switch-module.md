# Switch module
<style>
table {
  border-collapse: collapse;
  width: 70%;
  margin-bottom: 1em;
}

th, td {
  border: 1px solid #ddd;
  padding: 4px;
  text-align: left;
}

tr:nth-child(even) {
  background-color: #333;
}

th {
  background-color: #444;
}
</style>
The Switch Module handles the communication between the top module and all Body Processing Units (BPUs). It functions as a centralized routing hub that manages data flow between components during different phases of the n-body simulation.

## Purpose and functionality

The primary purpose of the Switch Module is to:

1. **Route simulation data** between components based on control signals
2. **Coordinate computational phases** across multiple BPUs
3. **Collect and forward status information** on collision detection

## Architecture

The Switch Module connects to each BPU through a standardized interface and manages data transfers using a target selection mechanism. It instantiates and coordinates multiple BPUs, creating a scalable architecture that can handle a variable number of celestial bodies.


### Key components

- **Target selection register**: Identifies which BPU is currently being addressed
- **Broadcast mechanism**: Wires that distribute one BPU's data to all others
- **Mode selection logic**: Determines the current operation mode for each BPU
- **Collision detection system**: Priority encoder to identify and report collisions

## Operation modes

The Switch Module operates in several distinct modes controlled by a 3-bit selection signal (`m_slct`). Each mode configures a specific pattern of data flow between components:

<div class="table-wrapper" markdown="block">

| **CMD (DEC)** | **CMD (BIN)** | **Name** | **Description** |
|:-------------:|:-------------:|:---------|:----------------|
| 0 | 000 | Velocity update | Broadcasts the position and mass of the target BPU to all other BPUs, setting them to velocity update mode. The target BPU's data is used as the source for the broadcast. |
| 1 | 001 | Update position | Sets all BPUs to position update mode and forwards the time step value to all units for synchronized position calculations. |
| 2 | 010 | Set position | Forwards the X, Y, Z, mass and size registers from the top module to the targeted BPU, setting it to coordinate setup mode while all other BPUs remain idle. |
| 3 | 011 | Set velocity | Forwards the X, Y and Z registers from the top module to the targeted BPU, setting it to velocity setup mode while all other BPUs remain idle. |
| 4 | 100 | Output velocity | Forwards the velocity components (X, Y, Z) from the targeted BPU to the top module. All other BPUs remain in idle state. |
| 5 | 101 | Reset all | Commands all BPUs to reset their internal registers, clearing all stored data. |
| 6 | 110 | Output position | Forwards the position components (X, Y, Z) from the targeted BPU to the top module. All other BPUs remain in idle state. |
| 7 | 111 | Idle | No data transfer occurs; all units remain in their current state. |

</div>

## Implementation details

### Input/output interfaces

The Switch Module exposes the following interfaces:

- **Target selection**: A register storing the ID of the currently targeted BPU
- **Data inputs**: X, Y, Z coordinates, mass, and size values from the top module
- **Control inputs**: Mode selection and time step values
- **Data outputs**: X, Y, Z values from the selected BPU for reading back
- **Status outputs**: Collision detection signals and collision ID information

### Broadcast mechanism

During velocity updates (mode 0), the switch module implements an efficient broadcast mechanism:

1. When a BPU is selected as the target, its position and mass data are read into broadcast wires
2. All other BPUs receive this broadcast data for their velocity calculations
3. This allows each BPU to compute gravitational interactions with all other bodies

This broadcasting approach enables efficient n-body simulations by ensuring each body has access to the position data of every other body, while minimizing data transfer overhead.

### Collision detection

The Switch Module coordinates collision detection across all BPUs:

- Each BPU reports collisions through a dedicated signal
- The module uses a priority encoder to identify which BPU detected a collision
- Collision information (occurrence and ID) is then reported back to the top module

The collision detection is implemented with a priority encoder that identifies the first BPU reporting a collision, and a reduction function that combines all collision signals to indicate whether any collision has occurred:

```scala
io.collision_id := PriorityEncoder(BPUs_io.map(_.collided))
io.collided := BPUs_io.map(_.collided).reduce(_ || _)
```

## Implementation details

### Input/output interfaces

The Switch Module exposes the following interfaces:

- **Target selection**: A register storing the ID of the currently targeted BPU
- **Data inputs**: X, Y, Z coordinates, mass, and size values from the top module
- **Control inputs**: Mode selection and time step values
- **Data outputs**: X, Y, Z values from the selected BPU for reading back
- **Status outputs**: Collision detection signals and collision ID information

### Broadcast mechanism

During velocity updates (mode 0), the switch module implements an efficient broadcast mechanism:

1. When a BPU is selected as the target, its position and mass data are read into broadcast wires
2. All other BPUs receive this broadcast data for their velocity calculations
3. This allows each BPU to compute gravitational interactions with all other bodies

This broadcasting approach enables efficient n-body simulations by ensuring each body has access to the position data of every other body, while minimizing data transfer overhead.

### Collision detection

The Switch Module coordinates collision detection across all BPUs:

- Each BPU reports collisions through a dedicated signal
- The module uses a priority encoder to identify which BPU detected a collision
- Collision information (occurrence and ID) is then reported back to the top module

The collision detection is implemented with a priority encoder that identifies the first BPU reporting a collision, and a reduction function that combines all collision signals to indicate whether any collision has occurred:

```scala
io.collision_id := PriorityEncoder(BPUs_io.map(_.collided))
io.collided := BPUs_io.map(_.collided).reduce(_ || _)
```

