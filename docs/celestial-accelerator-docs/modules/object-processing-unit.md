# Object Processing Unit

The Object Processing Unit (OPU) is a critical component of the celestial accelerator, responsible for managing the simulation of celestial bodies. It performs essential tasks related to the position and velocity updates of these bodies, ensuring accurate and efficient computations during the simulation process.

## Tasks Performed by the Object Processing Unit

1. **Position Update**: 
   - The OPU updates the position of a celestial body based on its current velocity and the time step of the simulation. This is achieved using the following equation:
     \[
     \vec{p}_{n+1} = \vec{p}_{n} + \vec{v}_{n} \cdot dt
     \]
   - The update process requires three multiplications and three additions, which can be executed in parallel using the arithmetic units available in the accelerator.

2. **Velocity Update**: 
   - The OPU computes the new velocity of the celestial body by considering the gravitational forces exerted by other bodies. This involves calculating the acceleration based on the mass and distance between bodies, which is optimized using the fast negative three-half exponent algorithm.

3. **Output Selection**: 
   - Depending on the input signal, the OPU can output either the updated velocity or position of the celestial body. This is managed through a multiplexer (Mux) that selects the appropriate output based on the current operation.

## Sub-Modules of the Object Processing Unit

The OPU is composed of several sub-modules that work together to perform its tasks efficiently:

- **Master Module**: 
  - This module holds the essential parameters of the celestial body, including its position, velocity, and mass. It coordinates the operations of the other sub-modules to execute the required tasks.

- **Fast Negative Three-Half Exponent Module**: 
  - This specialized module is crucial for optimizing the velocity update process. It implements a modified version of the fast inverse square root algorithm, allowing for rapid computation of gravitational effects with minimal cycles.

## Performance Considerations

The OPU is designed to minimize the number of clock cycles required for updates. The position update can be completed in four cycles, while the velocity update, including collision detection, can be performed in as few as 22 cycles. This efficiency is achieved through parallel processing and optimized algorithms, making the OPU a vital component in simulating large systems of celestial bodies effectively.