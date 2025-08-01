# Celestial top module
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
The celestial top module serves as the main interface between the rocket core and the accelerator. It handles communication, coordinates the simulation process, and manages the overall control flow of the n-body simulation.

## Communication protocol

The communication between the top module and the rocket core is accomplished through 64-bit packets sent to a data_in register. Each packet follows this structure:

| Field      | Bits    | Size     | Description                                |
|------------|---------|----------|--------------------------------------------|
| Command    | 63-59   | 5 bits   | Specifies the action for the accelerator   |
| Lock key   | 58-32   | 27 bits  | Security mechanism for authorized requests |
| Data       | 31-0    | 32 bits  | Values relevant to the command             |

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

## Implementation

Overall, the top module simply translates the commands from the MMIO into commands for the switch module, stores XYZ data and forwards them when required as well as connecting the switch's output to the MMIO output when required.

### Output Commands and Bit-Flip Mask

The `outputX`, `outputY`, `outputZ`, `outputdX`, `outputdY`, and `outputdZ` commands are used to retrieve the state of a celestial body from a specified Body Processing Unit (BPU). To use these commands, you must first select the target BPU using the `setTarget (stand alone)` command (CMD 17).

A key security feature of the output commands is the use of a **bit-flip mask**. When requesting data, the core must provide a 32-bit random value in the `Data` field of the packet. The accelerator then performs a bitwise XOR operation between this mask and the requested data (e.g., the X coordinate) before sending it back.

`output_data = actual_data XOR bit_flip_mask`

To reconstruct the original data, the core must perform a second XOR operation with the same mask:

`actual_data = output_data XOR bit_flip_mask`

This mechanism prevents a malicious actor from passively snooping on the communication bus to read the simulation data. Without the correct bit-flip mask, the intercepted data is meaningless.

### Simulation Control

-   **`startSimulation` (12):** Begins the n-body simulation. The accelerator will run for the number of iterations specified by `setTargetIterationNbr`.
-   **`stopSimulation` (13):** Halts the simulation prematurely.
-   **`setTargetIterationNbr` (14):** Sets the total number of time steps for the simulation.
-   **`setNbrActivePEs` (15):** Configures the number of BPUs to be used in the simulation, allowing for simulations with fewer than the maximum number of bodies.
-   **`keepAlive` (16):** Resets the inactivity timer to prevent the accelerator from automatically unlocking. This is useful during long periods of data setup or analysis.
-   **`outputCollisionID` (24):** If a collision is detected and the `stopInCaseOfCollision` flag is set, this command retrieves the ID of the BPU whose body was involved in the collision.

## Usage

Refer to the example C codes.