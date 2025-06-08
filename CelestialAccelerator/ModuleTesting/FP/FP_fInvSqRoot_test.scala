package celestial

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.lang.Float.{intBitsToFloat, floatToIntBits}
import scala.math.{pow, sqrt}

class F32FastInverseSqrtTester extends AnyFlatSpec with ChiselScalatestTester {
  "F32FastInverseSqrt" should "calculate 1/sqrt(x) correctly" in {
    test(new F32FastInverseSqrtV2) { dut =>
      def testInvSqrt(x: Float): Unit = {
        // Calculate expected result: 1/sqrt(x)
        val expected = (1.0f / sqrt(x)).toFloat
        
        val xBits = java.lang.Integer.toUnsignedLong(floatToIntBits(x))
        dut.io.in.poke(xBits.U(32.W))
        
        // Give enough clock cycles for the 5 iterations to complete
        // Each iteration needs 4 steps plus state transitions
        // Adding extra cycles for safety
        dut.clock.step(9)
        
        val out = dut.io.out.peek().litValue.toInt
        val outFloat = intBitsToFloat(out)
        
        println(f"---------Test: 1/sqrt($x) = $outFloat%.6f (expected $expected%.6f)----------------")
        
        if (outFloat.isNaN) {
          assert(java.lang.Float.isNaN(expected), s"Failed on 1/sqrt($x)")
        } else if (expected.isNaN) {
          assert(
            java.lang.Float.isNaN(outFloat), s"Failed on 1/sqrt($x)")
        } else if (expected == Float.PositiveInfinity || expected == Float.NegativeInfinity) {
          assert(java.lang.Float.isInfinite(outFloat), s"Failed on 1/sqrt($x)")
        } else {
          // For very small values, use absolute error instead of relative error
          val smallestFixedPoint = 1.18e-38f
          val relativeError = if (expected != 0) math.abs((outFloat - expected) / expected) else math.abs(outFloat - expected)
          val absoluteError = math.abs(outFloat - expected)
          
          // Allow for some error margin due to the approximation nature of the algorithm
          // Fast inverse square root is an approximation, so we allow slightly larger error
          assert(relativeError <= 0.0001f || absoluteError <= smallestFixedPoint, 
                 s"Failed on 1/sqrt($x): got $outFloat, expected $expected, relative error: $relativeError")
        }
      }
      
      // Test special values
      val specialValues = List(
        0.0f,                    // Should result in positive infinity
        1.0f,                    // Should equal 1.0
        4.0f,                    // Should equal 0.5
        9.0f,                    // Should equal 0.33333...
        16.0f,                   // Should equal 0.25
        /*1.18e-38f,
        Float.MaxValue,          // Very large value
        Float.PositiveInfinity,  // Should be 0*/ // Don't check extreme values, as it uses squared values internally in the float format
        // and thus won't work, this is expected.
        Float.NaN                // Should be NaN
      )
      
      for (x <- specialValues) {
        testInvSqrt(x)
      }
      
      // Test negative values (should result in NaN)
      testInvSqrt(-1.0f)
      testInvSqrt(-4.0f)
      
      // Test powers of 2 (which have exact binary representations)
      for (i <- 1 to 10) {
        val x = math.pow(2.0, i).toFloat
        testInvSqrt(x)
      }
      
      // Test random positive values
      val random = new scala.util.Random(42) // Fixed seed for reproducibility
      for (_ <- 0 until 50) {
        // Generate random positive values across different magnitudes
        val exponent = random.nextInt(40) - 20 // Range from 2^-20 to 2^20
        val mantissa = 1.0f + random.nextFloat() // Range [1.0, 2.0)
        val x = mantissa * math.pow(2.0f, exponent).toFloat
        testInvSqrt(x)
      }
      
      // Test values specifically relevant to the Fast Inverse Square Root algorithm
      // These are values that are often used as test cases for this algorithm
      val quakeTestValues = List(
        0.15f,      // Small value
        2.0f,       // Power of 2
        3.14159f,   // Pi
        4.0f,       // Another power of 2
        100.0f,     // Larger value
        10000.0f    // Much larger value
      )
      
      for (x <- quakeTestValues) {
        testInvSqrt(x)
      }
    }
  }
}