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

## Usage

Refer to the example C codes.