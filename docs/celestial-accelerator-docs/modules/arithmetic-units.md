# Arithmetic units

The arithmetic units in the celestial accelerator play a crucial role in performing the necessary calculations for updating the positions and velocities of celestial bodies during simulations. These units are designed to handle various mathematical operations efficiently, ensuring that the accelerator can process data quickly and accurately.

## Functions of the arithmetic units

1. **Position update calculations**:
   - The arithmetic units are responsible for calculating the new positions of celestial bodies based on their current velocities and the time step of the simulation. The position update is performed using the formula:
     \[
     \vec{p}_{n+1} = \vec{p}_{n} + \vec{v}_{n} \cdot dt
     \]
   - This requires multiple arithmetic operations, including multiplications and additions, which the units execute in parallel to optimize performance.

2. **Velocity update calculations**:
   - The units also compute the velocity updates based on gravitational interactions between bodies. The velocity update is influenced by the mass of the bodies and their relative positions, which involves more complex calculations.

3. **Collision detection**:
   - In addition to position and velocity updates, the arithmetic units assist in collision detection by calculating the distances between bodies and determining if they are on a collision course. This is essential for simulations involving multiple celestial objects.

## Design and implementation

- The arithmetic units are designed to operate in parallel, allowing for simultaneous calculations. This parallelism is key to achieving the desired performance improvements in the accelerator.
- The units utilize optimized algorithms, such as the fast negative three-half exponent, to reduce the number of cycles required for certain calculations, particularly during velocity updates.

## Conclusion

The arithmetic units are a fundamental component of the celestial accelerator, enabling efficient and accurate computations necessary for simulating the dynamics of celestial bodies. Their design focuses on maximizing performance while ensuring the reliability of the simulation results.