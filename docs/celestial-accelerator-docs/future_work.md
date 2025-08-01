# Future Work

While the Celestial Accelerator successfully meets its primary objectives of providing significant speedup for n-body simulations, there are several potential areas for further improvement and enhancement in future iterations.

## Precision and Range Improvements

### Double Precision Support
Currently, the accelerator uses single-precision floating-point (32-bit) calculations. This forces the use of a pre-scaling multiplication for a solar system simulation, to ensure the computation of 1/d^3 doesn't go over what a float can do. Implementing double-precision (64-bit) floating-point operations would:
- Increase the numerical range of simulations, removing the need for such a scaler
- Improve calculation precision for sensitive simulations
- Allow for more accurate long-running simulations where precision errors can accumulate

### Custom Floating-Point Format
The requirements for celestial simulations are unique due to the vast distances between celestial objects. A specialized floating-point format could be developed with:
- Modified exponent bias tailored to astronomical scales
- Optimized bit allocation between mantissa and exponent
- Potentially faster calculations due to hardware-specific optimizations
But it would make the communication with the C code more cumbersome. 

## Security Enhancements

Building upon the existing [security measures](security/security-risks.md), several improvements could be implemented:

- **Advanced Brute Force Protection**: The current 27-bit key could be vulnerable to brute force attacks during long-running simulations. A potential solution would be to clear all sensitive data and output a warning when a certain number of invalid commands are detected.
- **Dynamic Key Management**: Implementing key rotation or more sophisticated authentication mechanisms.
- **Intrusion Detection**: More sophisticated detection of potential attacks with appropriate responses.

## Performance Optimizations

### Concurrent Simulation Support
Adding support for multiple concurrent simulations would be beneficial when:
- A simulation uses only a subset of the available Body Processing Units (BPUs)
- Different users need to run independent simulations with smaller body counts
- Comparing multiple simulation scenarios in parallel

### Support for simulation with more bodies

Add some multiplexing mechanism to support simulations where the number of bodies is greater than the number of available Body Processing Units (BPU).

### Broadcasting Optimization
Currently, when a BPU broadcasts its position to other units during the velocity update phase, it remains idle. Modifying this procedure could:
- Save approximately 23 cycles per simulation step
- Nearly double the throuput for the worst case scenario, 2 bodies

## Hardware implementation

### Neg Three Half refinement optimization
The effective frequency is heavily bottlenecked by the negative three half exponent's refinement module. Some troubleshooting should be able to fix this issue, by dividing one of the steps in two.

### FPGA-Specific Optimizations
Leveraging specific FPGA features could further enhance performance:
- DSP block utilization for critical mathematical operations
- Memory architecture optimizations
- Clock domain optimizations for different modules



## Integration Capabilities

### API Enhancements
Expanding the programming interface to support:
- More simulation parameters and configurations
- Advanced initialization methods
- Better integration with scientific computing frameworks

### Visualization Support
Direct integration with visualization tools could provide:
- Real-time simulation monitoring
- Interactive parameter adjustments
- Improved debugging capabilities

## Conclusion

Though the accelerator is fully functional, there are numerous possible improvements, in terms of usability, precison and performance.