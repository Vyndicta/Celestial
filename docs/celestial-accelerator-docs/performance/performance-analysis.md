# Performance Analysis of the Celestial Accelerator

The performance analysis of the celestial accelerator provides insights into its efficiency and effectiveness in simulating the n-body problem. This document outlines the methodology used for performance evaluation, the results obtained, and the implications of these results.

## Methodology

To assess the performance of the accelerator, simulations were conducted with varying numbers of celestial bodies (2, 4, 6, and 8). The simulations were executed using C code that models a year of solar system movement with a daily time step. Two scenarios were compared:

1. **Without Accelerator**: The simulation was run without the accelerator, and potential collisions were not computed to provide a baseline for comparison.
2. **With Accelerator**: The simulation was run utilizing the accelerator, which computes the number of clock cycles required for both setup and execution.

## Results

The results of the performance analysis are summarized below:

- **Clock Cycles**: The number of clock cycles required to run the simulation was recorded for both scenarios. It was observed that the clock cycles required without the accelerator increased exponentially with the number of bodies, while the clock cycles with the accelerator scaled linearly.
  
- **Speedup**: The speedup granted by the accelerator was calculated as a percentage increase in performance. The results indicated a significant improvement, particularly as the number of bodies increased.

### Key Findings

- The number of clock cycles required for the simulation without the accelerator grows exponentially, while the accelerator maintains a linear growth due to its dedicated processing units for each body.
- The analytical model for clock cycles with the accelerator was confirmed, matching the expected performance metrics.
- Even in the worst-case scenario with only 2 bodies, the optimized velocity update flow provided a speed increase of 626%.

## Conclusion

The performance analysis demonstrates that the celestial accelerator significantly enhances the efficiency of n-body simulations. By leveraging dedicated processing units and optimized algorithms, the accelerator achieves substantial speedups, making it a valuable tool for simulating complex celestial dynamics. The results underscore the importance of hardware acceleration in computational astrophysics and related fields.