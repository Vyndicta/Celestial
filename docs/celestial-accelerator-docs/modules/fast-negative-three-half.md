# Fast Negative Three-Half Exponent

As detailed in the body processing unit's documentation, the velocity update flow relies on computing $$x^{-3/2}$$. While it would be possible to implement the fast inverse square root algorithm (which computes $$x^{-1/2}$$) and then cube the result, a custom algorithm dedicated to $$x^{-3/2}$$ can yield better results, depending on the precision requirements.

This document first explains the well-known fast inverse square root algorithm, details the custom fast negative three-half exponent algorithm and then compare the two methods in terms of precision and clock cycles necessary.

## The Fast Inverse Square Root Algorithm

The fast inverse square root algorithm is a well known method for approximating $$y = 1/\sqrt{x}$$.

### Initial Estimate

The algorithm leverages the binary representation of IEEE-754 floating-point numbers. A positive float $$x$$ can be written as:

$$
x = (1 + \frac{M_x}{N}) \cdot 2^{E_x-B}
$$

where $$M_x$$ is the mantissa, $$E_x$$ is the exponent, $$N=2^{23}$$, and $$B$$ is the exponent bias (127 for single precision).

If we interpret the 32-bit representation of $$x$$ as an integer $$I_x$$, we get the relation:

$$
I_x = E_x \cdot N + M_x
$$

The core idea starts by taking the base-2 logarithm of the target equation $$y = x^{-1/2}$$:

$$
ln(y) = -\frac{1}{2} ln(x)
$$

The first order Taylor approximation of ln(1+x) is x. Then, since due to the floating point representation, x is within $$[0,1($$, this approximation can be refined by adding a constant value, yielding:
$ln(1+z) \approx z + \sigma$ (where $\sigma \approx 0.057304$ is a constant chosen to minimize error over the range $[0,1)$).
This can be used on the integer previous equation with $x$ and $y$:

$$
I_y \approx \frac{3N}{2}(B - \sigma) - \frac{I_x}{2}
$$

The term $$\frac{3N}{2}(B - \sigma)$$ is known as the "magic number". For $$\sigma = 0.057304$$, this constant is approximately $$1.597 \times 10^9$$, which is `0x5F34FF64` in hexadecimal.

This gives a remarkably good first estimate for $$I_y$$ using only integer and bit-shifting operations:
`I_y = 0x5F34FF64 - (I_x >> 1)`

### Refining the Estimate

The initial estimate can be improved using the Newton-Raphson method for root-finding. We seek a root of the function $$f(y) = \frac{1}{y^2} - x$$. The iterative refinement formula is:

$$
y_{n+1} = y_n \left( \frac{3}{2} - \frac{x y_n^2}{2} \right)
$$

Each iteration of this formula significantly improves the precision of the result.

## The Fast Negative Three-Half Algorithm

This algorithm adapts the principles of the fast inverse square root to directly compute $$y = x^{-3/2}$$.

### Initial Estimate

The derivation is similar. We start with the log equation:

$$
ln(y) = -\frac{3}{2} ln(x)
$$

Following the same substitution and approximation steps, we arrive at a new relationship between the integer representations:

$$
I_y \approx \frac{5N}{2}(B - \sigma) - \frac{3I_x}{2}
$$

The magic number for this calculation, $$\frac{5N}{2}(B - \sigma)$$, is `0x9EADA9A8`. The initial estimate is therefore:
`I_y = 0x9EADA9A8 - (3 * I_x) >> 1`

### Refining the Estimate

To refine the estimate, we again use the Newton-Raphson method. The most suitable function for this problem is $$f(y) = -x^3 + \frac{1}{y^2}$$. This choice avoids costly division operations in the refinement step.

The first-order (d=1, standard Newton-Raphson) refinement formula is:

$$
y_{n+1} = \frac{y_n(3 - x^3 y_n^2)}{2}
$$

The second-order (d=2) Householder's method gives a more complex formula:

$$
y_{n+1} = \frac{y_n(3x^6y^4 - 10x^3y^2 + 15)}{8}
$$

## Comparison and Conclusion

To evaluate the best approach for computing $x^{-3/2}$, we compare three methods:
1.  **Cubed Fast Inverse SqRt**: Compute $x^{-1/2}$ and cube the result.
2.  **Fast Neg. 3/2 (d=1)**: Use the custom algorithm with the first-order refinement.
3.  **Fast Neg. 3/2 (d=2)**: Use the custom algorithm with the second-order refinement.

The performance is compared based on computational cost (in cycles) and average relative error.

| Method                               | Refinement Iterations | Cost (Cycles) | Avg. Relative Error |
| ------------------------------------ | --------------------- | ------------- | ------------------- |
| Cubed Fast Inverse SqRt              | 0                     | 3             | 0.0504              |
|                                      | 1                     | 8             | 0.0018              |
|                                      | 2                     | 12            | 4.18e-06            |
|                                      | 3                     | 16            | 3.65e-08            |
| **Fast Neg. 3/2 (d=1)**              | **0**                 | **1**         | **0.0343**          |
|                                      | **1**                 | **7**         | **0.0026**          |
|                                      | **2**                 | **11**        | **2.11e-05**        |
|                                      | **3**                 | **15**        | **3.74e-08**        |
| Fast Neg. 3/2 (d=2)                  | 1                     | 12            | 2.55e-04            |
|                                      | 2                     | 20            | 4.78e-08            |

The second-order (d=2) refinement method proves to be inefficient, offering poor precision for its high computational cost.

The choice is between the cubed fast inverse square root and the custom fast negative three-half algorithm with first-order refinement. For high-precision applications, an error threshold of 1e-6 is desirable. This leaves two viable options:
-   Cubed Fast Inverse SqRt with 3 iterations.
-   Fast Neg. 3/2 (d=1) with 3 iterations.

The **Fast Negative Three-Half algorithm with 3 refinement iterations** was chosen for the hardware implementation. It requires 15 cycles, a ~7% improvement over the 16 cycles of the alternative, while the precision is only slightly lower. This trade-off was deemed favorable for the accelerator's velocity update flow.

## References

1.  McEniry, C. (2007). *The mathematics behind the fast inverse square root function code*.
2.  Moroz, L. V., Walczyk, C. J., Hrynchyshyn, A., Holimath, V., & Cieśliński, J. L. (2018). Fast calculation of inverse square root with the use of magic constant – analytical approach. *Applied mathematics and computation*, 316, 245-255.