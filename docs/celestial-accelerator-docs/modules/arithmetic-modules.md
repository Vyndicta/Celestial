# Arithmetic modules

The body processing units rely on several floating-point arithmetic modules to perform its calculations. These modules are used for various vector operations, position updates, velocity calculations, and specialized mathematical functions.

## Floating point addition/subtraction Module

This module implements both addition and subtraction between two IEEE-754 floating-point numbers. The design choice to combine both operations into a single module was made to optimize resource utilization, as the hardware required for addition and subtraction is very similar.

### Key features

- Implements standard IEEE-754 floating-point arithmetic
- Used extensively in vector operations and in the fast negative three-half exponent algorithm
- Designed to minimize idle time by supporting both operations in a single module

### Usage in the accelerator

The addition/subtraction module is used for various operations including:
- Computing distance vector between celestial bodies (subtraction)
- Summing vector components (addition)
- Position updates by adding velocity components
- Intermediate calculations in the fast negative three-half exponent algorithm
- Summing squared components to calculate distance

## Floating point multiplication module

This module implements multiplication between two IEEE-754 floating-point numbers through a straightforward implementation.

### Key features

- Standard IEEE-754 floating-point multiplication
- Used in both position and velocity update calculations
- Essential for scaling vectors by time step values and masses
- Used in the fast negative three-half exponent calculations

### Usage in the accelerator

The multiplication module is used for:
- Scaling velocity by time step during position updates
- Computing squared distance components
- Multiplying direction vectors by gravitational factors
- Scaling mass values with gravitational constants
- Various intermediate calculations in specialized algorithms

## Module utilization

The arithmetic modules are designed to operate efficiently within the processing pipeline of the Body Processing Units. As shown in the flow utilization table (in the body processing unit's documentation), these modules are carefully scheduled to maximize parallel processing and minimize idle cycles.

