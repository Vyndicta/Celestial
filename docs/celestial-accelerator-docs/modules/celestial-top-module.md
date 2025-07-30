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

The accelerator supports the following commands:

| CMD (DEC) | CMD (BIN) | Name                      | Data             | Description                                                                                             |
|-----------|-----------|---------------------------|------------------|---------------------------------------------------------------------------------------------------------|
| 0         | 00000     | no request / Idle         | –                | No operation requested                                                                                  |
| 1         | 00001     | lock                      | –                | Lock the accelerator with the lock key in the packet                                                    |
| 2         | 00010     | unlock                    | –                | Unlock the accelerator                                                                                  |
| 3         | 00011     | setX                      | X value          | Set the X register                                                                                      |
| 4         | 00100     | setY                      | Y value          | Set the Y register                                                                                      |
| 5         | 00101     | setZ                      | Z value          | Set the Z register                                                                                      |
| 6         | 00110     | setM                      | Mass value       | Set the mass register                                                                                   |
| 7         | 00111     | setS                      | Size             | Set the size register                                                                                   |
| 8         | 01000     | setDt                     | Δt               | Set the simulation's time step                                                                          |
| 9         | 01001     | forwardData(position)     | Target           | Forward the XYZ registers as position, mass and size data to target body processing unit                |
| 10        | 01010     | forwardData(velocity)     | Target           | Forward the XYZ registers as velocity to the target body processing unit                                |
| 11        | 01011     | stopInCaseOfCollision     | Bool             | Choose whether to stop the simulation on collision (active HIGH)                                        |
| 12        | 01100     | startSimulation           | –                | Begin simulation run                                                                                    |
| 13        | 01101     | stopSimulation            | –                | Stop simulation run                                                                                     |
| 14        | 01110     | setTargetIterationNbr     | Iteration number | Set number of iteration in a simulation                                                                 |
| 15        | 01111     | setNbrActivePEs           | Number of PEs    | Set number of active processing elements                                                                |
| 16        | 10000     | keepAlive                 | –                | Ensure the accelerator doesn't unlock due to inactivity                                                 |
| 17        | 10001     | setTarget (stand alone)   | Target ID        | Select the target, used by the output functions                                                         |
| 18        | 10010     | outputX                   | Bit flip mask    | Output X coordinate. The target body processing unit must be specified previously using command 17.     |
| 19        | 10011     | outputY                   | Bit flip mask    | Output Y coordinate. The target body processing unit must be specified previously using command 17.     |
| 20        | 10100     | outputZ                   | Bit flip mask    | Output Z coordinate. The target body processing unit must be specified previously using command 17.     |
| 21        | 10101     | outputdX                  | Bit flip mask    | Output velocity in X. The target body processing unit must be specified previously using command 17.    |
| 22        | 10110     | outputdY                  | Bit flip mask    | Output velocity in Y. The target body processing unit must be specified previously using command 17.    |
| 23        | 10111     | outputdZ                  | Bit flip mask    | Output velocity in Z. The target body processing unit must be specified previously using command 17.    |
| 24        | 11000     | outputCollisionID         | –                | Output ID of the body that collided                                                                     |
| 25-31     | -         | -                         | –                | Not implemented                                                                                         |

## Usage

Refer to the example C codes.