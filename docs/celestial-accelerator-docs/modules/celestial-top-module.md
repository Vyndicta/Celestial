# Celestial top module

The celestial top module serves as the main interface between the rocket core and the accelerator. It handles communication, coordinates the simulation process, and manages the overall control flow of the n-body simulation.

## Communication protocol

The communication between the top module and the rocket core is accomplished through 64-bit packets sent to a data_in register. Each packet follows this structure:

| Field | Size | Description |
|-------|------|-------------|
| Command | 8 bits | Specifies the action for the accelerator |
| Lock key | 8 bits | Security mechanism for authorized requests |
| Data | 48 bits | Values relevant to the command |

## Supported commands

The accelerator supports various commands for different operations:

- **Setup commands**: Initialize simulation parameters
- **Position update commands**: Trigger position calculations
- **Velocity update commands**: Initiate velocity computations
- **Collision detection commands**: Check for potential collisions
- **Status query commands**: Retrieve simulation state

## Key responsibilities

### Iteration management
- Controls the simulation time steps
- Manages the overall simulation loop
- Coordinates between different processing phases

### Module coordination
- Orchestrates communication between body processing units
- Controls the switch module for data routing
- Synchronizes position and velocity update phases

### Security features
- Implements lock key verification to prevent unauthorized access
- Protects against simulation tampering
- Ensures data integrity during processing

## Implementation details

The top module maintains several important state variables:
- Current iteration counter
- Time step value
- Number of active bodies
- Simulation status flags

It also manages the control signals that coordinate the different phases of the simulation, ensuring proper sequencing of position updates, velocity calculations, and collision detection.