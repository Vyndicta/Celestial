# Object processing unit

The object processing unit (also called body processing unit or BPU) is the core computational element of the celestial accelerator. Each unit is responsible for managing one celestial body and performing all necessary calculations for that body's motion and interactions.

## Main functions

The processing unit performs three primary tasks:

1. **Position update**: Calculate new position based on current velocity and time step
2. **Data output**: Provide position or velocity data to other units
3. **Velocity update**: Compute new velocity based on gravitational interactions
4. **Accepting input data**: Store the input data, such as coordinates of the celestial body

## Architecture overview

Each object processing unit contains four main sub-modules:

### Master module
- Holds the body's position, velocity, and mass
- Coordinates all other sub-modules
- Executes requested tasks based on control signals

### Fast negative three-half exponent module
- Implements optimized algorithm for $$^-3/2$$
- Based on modified fast inverse square root algorithm
