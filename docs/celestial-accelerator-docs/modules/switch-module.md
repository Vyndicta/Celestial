# Switch module

The switch module acts as the central data routing hub of the celestial accelerator, managing the flow of information between the various body processing units (BPUs) and coordinating data sharing during different phases of the simulation.

## Primary function

The switch module connects the relevant outputs to the relevant inputs according to commands from the top module. Its main responsibility is enabling efficient data sharing between body processing units during the simulation process.

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

## Implementation details

### Multiplexing architecture
The switch uses multiplexers to route data efficiently:
- Input multiplexers select which BPU's data to broadcast
- Output demultiplexers distribute data to multiple recipients
- Control logic determines the routing configuration

### Timing coordination
Working with the top module's internal counter:
- Tracks which BPU is currently broadcasting
- Manages the transition between different simulation phases
- Ensures proper synchronization of data flow

## Performance considerations

### Bandwidth optimization
- Designed to minimize data transfer overhead
- Efficient routing reduces communication bottlenecks
- Parallel data paths where possible

### Scalability
- Architecture scales with the number of body processing units
- Configurable through Chisel parameters
- Maintains performance as system size increases

## Integration with simulation phases

### Velocity update phase
- Systematically broadcasts each BPU's position to all others
- Coordinates the O(N) complexity velocity calculations
- Manages the sequential nature of position sharing

### Position update phase
- Handles any necessary data routing for position calculations
- Ensures BPUs have access to required velocity information
- Maintains data flow efficiency during parallel position updates

The switch module is essential for maintaining the accelerator's O(N) complexity advantage, enabling efficient communication patterns that would be much more complex in software implementations.