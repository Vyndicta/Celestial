# Performance analysis of the celestial accelerator

As the speed increase changes with the number of bodies simulated, different scenarios with 2, 4, 6
and 8 bodies were used to provide a good overview. A C code running a simulation corresponding
to a year of the solar system’s movement with a time step of one day was used to make the comparison.

To make the comparison more representative of the average use case, the implementation without
the accelerator did not compute the possible collisions. The C code ran the simulation both with and
without the use of the accelerator to get the number of clock cycles required in both cases.

As the C commands can take a long time to execute, relative the speed at which the accelerator
works, the number of clock cycles measured by the C code for the accelerator is not exact : the
accelerator can finish a few cycles before the C code realises it. The precise number was extracted
from the waveforms using GTKwave. It would also have been possible to use longer simulations to
render this delay even smaller relative to the simulation time, but such a solution would have made
the simulation time unpractical. The results are presented below:

![The performances](../assets/PerfGraph.png)

As expected, the number of clock cycles to run the simulation without the accelerator increases
exponentially. As the accelerator has one processing unit per body, it scales linearly. The number of
clock cycles matches the analytical model exactly : n_clkacc = n_iter ∗ (23 ∗ n + 4) − n ∗ 23, with n
the number of bodies in the simulation. The −n ∗ 23 appears because at the first iteration, there is only a position update but no velocity update.

The acceleration speedup increases linearly with the number of bodies, which is expected from
comparing a O(n) and O(n2) operation. Even for 2 bodies only, the worst case scenario, the optimised
velocity update flows brings a 626% speed increase.