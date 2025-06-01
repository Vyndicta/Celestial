# Object processing unit

The object processing unit (also called body processing unit or BPU) is the core computational element of the celestial accelerator. Each unit is responsible for managing one celestial body and performing all necessary calculations for that body's motion and interactions.

## Main functions

The processing unit performs three primary tasks:

1. **Position update**: Calculate new position based on current velocity and time step
2. **Data output**: Provide position or velocity data to other units
3. **Velocity update**: Compute new velocity based on gravitational interactions

## Architecture overview

Each object processing unit contains four main sub-modules:

### Master module
- Holds the body's position, velocity, and mass
- Coordinates all other sub-modules
- Executes requested tasks based on control signals

### Fast negative three-half exponent module
- Implements optimized algorithm for velocity calculations
- Essential for computing gravitational accelerations efficiently
- Based on modified fast inverse square root algorithm

### Arithmetic units
- Two parallel arithmetic units for multiplication and addition
- Shared between different sub-modules to minimize area
- Enable parallel computation for improved performance

### Output multiplexer
- Selects between position and velocity output
- Controlled by the setup signal from the top module
- Enables data sharing during different simulation phases

## Position update process

The position update follows the equation:
```
p_{n+1} = p_n + v_n * dt
```

This process takes 4 cycles using the two arithmetic units in parallel:
1. **Cycle 1**: Multiply time step with X velocity, store in buffer
2. **Cycle 2**: Add buffer to X position, multiply time step with Y velocity
3. **Cycle 3**: Add buffer to Y position, multiply time step with Z velocity  
4. **Cycle 4**: Add buffer to Z position

## Velocity update process

The velocity update is more complex, requiring computation of gravitational forces from all other bodies. The simplified equation (with mass factored out) is:

```
v_{1,n+1} = v_{1,n} + dt * m_2 * G * (unit_direction_vector / distance²)
```

This requires:
- Distance calculation between bodies
- Normalization of direction vector
- Force magnitude computation
- Velocity component updates

## Key features

- **Scalability**: Number of units is configurable as a Chisel parameter
- **Efficiency**: Shared arithmetic resources minimize hardware overhead
- **Parallelism**: Multiple units operate simultaneously for O(N) complexity
- **Collision detection**: Built-in capability to detect potential collisions during velocity updates