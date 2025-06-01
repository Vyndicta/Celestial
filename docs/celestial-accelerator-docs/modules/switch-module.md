# Switch module

The switch module acts as the central data routing hub of the celestial accelerator, managing the flow of information between the various body processing units (BPUs) and coordinating data sharing during different phases of the simulation.

## Key operations

### Position broadcasting
During the velocity update phase, each body processing unit needs to know the positions of all other bodies to compute gravitational interactions. The switch module:
- Receives position data from one BPU at a time
- Broadcasts this position to all other BPUs
- Cycles through each BPU systematically
- Ensures all units receive the necessary position information

### Data flow coordination
The switch module manages different types of data flow:
- **Position sharing**: Broadcasting positions during velocity calculations
- **Velocity sharing**: Distributing velocity data when needed
- **Control signal routing**: Forwarding control commands to appropriate units
- **Status information**: Collecting and routing status data back to the top module

