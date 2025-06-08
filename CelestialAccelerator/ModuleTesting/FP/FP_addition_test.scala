package celestial

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.lang.Float.intBitsToFloat
import java.lang.Float.floatToIntBits

class F32AdderTester extends AnyFlatSpec with ChiselScalatestTester {
  "F32Adder" should "add floats correctly" in {
    test(new F32Adder) { dut =>
      def testAdd(a: Float, b: Float): Unit = {
      val expected = a + b
      val aBits = java.lang.Integer.toUnsignedLong(floatToIntBits(a))
      val bBits = java.lang.Integer.toUnsignedLong(floatToIntBits(b))
      val expectedBits = java.lang.Integer.toUnsignedLong(floatToIntBits(expected))

      dut.io.a.poke(aBits.U(32.W))
      dut.io.b.poke(bBits.U(32.W))
      dut.io.substracter.poke(false.B)
      dut.clock.step(3)
      

      val out = dut.io.sum.peek().litValue.toInt
      val outFloat = intBitsToFloat(out)

      println(f"Test: $a + $b = $outFloat%.6f (expected $expected%.6f)")
      if (outFloat.isNaN) {
        assert(java.lang.Float.isNaN(expected), s"Failed on $a + $b")
      } else if (expected.isNaN) {
        assert(java.lang.Float.isNaN(outFloat), s"Failed on $a + $b")
      } else if (expected == Float.PositiveInfinity) {
        assert(java.lang.Float.isInfinite(outFloat), s"Failed on $a + $b")
      } else if (expected == Float.NegativeInfinity) {
        assert(java.lang.Float.isInfinite(outFloat), s"Failed on $a + $b")
      } else
      {
        // Tolerate 0.001% error
        assert(math.abs(outFloat - expected) <= math.abs(expected) * 0.00001f, s"Failed on $a + $b: got $outFloat, expected $expected")
      }
    }


      val corner_case_numbers = List(
        0.0f, // Zero
        1.0f, // One
        -1.0f, // Negative one
        Float.MinValue, // Smallest positive float
        Float.MaxValue, // Largest positive float
        Float.NegativeInfinity, // Negative infinity
        Float.PositiveInfinity, // Positive infinity
        Float.NaN // Not a number
      )
      // Test addition of corner case numbers
      for (i <- corner_case_numbers.indices) {
        for (j <- i until corner_case_numbers.length) {
          val a = corner_case_numbers(i)
          val b = corner_case_numbers(j)
          testAdd(a, b)
        }
      }
      // Test addition of random floats
      val random = new scala.util.Random
      for (_ <- 0 until 10) {
        val a = random.nextFloat() * 1000 - 500 // Random float between -500 and 500
        val b = random.nextFloat() * 1000 - 500 // Random float between -500 and 500
        testAdd(a, b)
      }
      // Test addition of large numbers
      val largeNumbers = List(
        1e10f, // Large positive float
        -1e10f, // Large negative float
        1e20f, // Very large positive float
        -1e20f // Very large negative float
      )
      for (i <- largeNumbers.indices) {
        for (j <- i until largeNumbers.length) {
          val a = largeNumbers(i)
          val b = largeNumbers(j)
          testAdd(a, b)
        }
      }
      // Test addition of small numbers
      val smallNumbers = List(
        1e-10f, // Small positive float
        -1e-10f, // Small negative float
        1e-20f, // Very small positive float
        -1e-20f // Very small negative float
      )
      for (i <- smallNumbers.indices) {
        for (j <- i until smallNumbers.length) {
          val a = smallNumbers(i)
          val b = smallNumbers(j)
          testAdd(a, b)
        }
      }
      // Test addition of mixed numbers
      val mixedNumbers = List(
        1e10f, // Large positive float
        -1e10f, // Large negative float
        1e-10f, // Small positive float
        -1e-10f // Small negative float
      )
      for (i <- mixedNumbers.indices) {
        for (j <- i until mixedNumbers.length) {
          val a = mixedNumbers(i)
          val b = mixedNumbers(j)
          testAdd(a, b)
        }
      }
      // Test random numbers of extreme range
      for (_ <- 0 until 100) {
        val a = random.nextFloat() * 1e38f - 1e38f // Random float between -1e38 and 1e38
        val b = random.nextFloat() * 1e38f - 1e38f // Random float between -1e38 and 1e38
        testAdd(a, b)
      }
    }
  }
    "F32Adder" should "substracts floats correctly" in {
    test(new F32Adder) { dut =>
      def testAdd(a: Float, b: Float): Unit = {
      val expected = a - b
      val aBits = java.lang.Integer.toUnsignedLong(floatToIntBits(a))
      val bBits = java.lang.Integer.toUnsignedLong(floatToIntBits(b))
      val expectedBits = java.lang.Integer.toUnsignedLong(floatToIntBits(expected))

      dut.io.a.poke(aBits.U(32.W))
      dut.io.b.poke(bBits.U(32.W))
      dut.io.substracter.poke(true.B)
      dut.clock.step(3)
      

      val out = dut.io.sum.peek().litValue.toInt
      val outFloat = intBitsToFloat(out)

      println(f"Test: $a - $b = $outFloat%.6f (expected $expected%.6f)")
      if (outFloat.isNaN) {
        assert(java.lang.Float.isNaN(expected), s"Failed on $a + $b")
      } else if (expected.isNaN) {
        assert(java.lang.Float.isNaN(outFloat), s"Failed on $a + $b")
      } else if (expected == Float.PositiveInfinity) {
        assert(java.lang.Float.isInfinite(outFloat), s"Failed on $a + $b")
      } else if (expected == Float.NegativeInfinity) {
        assert(java.lang.Float.isInfinite(outFloat), s"Failed on $a + $b")
      } else
      {
        // Tolerate 0.001% error
        assert(math.abs(outFloat - expected) <= math.abs(expected) * 0.00001f, s"Failed on $a + $b: got $outFloat, expected $expected")
      }
    }
     val corner_case_numbers = List(
        0.0f, // Zero
        1.0f, // One
        -1.0f, // Negative one
        Float.MinValue, // Smallest positive float
        Float.MaxValue, // Largest positive float
        Float.NegativeInfinity, // Negative infinity
        Float.PositiveInfinity, // Positive infinity
        Float.NaN // Not a number
      )
      // Test addition of corner case numbers
      for (i <- corner_case_numbers.indices) {
        for (j <- i until corner_case_numbers.length) {
          val a = corner_case_numbers(i)
          val b = corner_case_numbers(j)
          testAdd(a, b)
        }
      }
      // Test addition of random floats
      val random = new scala.util.Random
      for (_ <- 0 until 10) {
        val a = random.nextFloat() * 1000 - 500 // Random float between -500 and 500
        val b = random.nextFloat() * 1000 - 500 // Random float between -500 and 500
        testAdd(a, b)
      }
      // Test addition of large numbers
      val largeNumbers = List(
        1e10f, // Large positive float
        -1e10f, // Large negative float
        1e20f, // Very large positive float
        -1e20f // Very large negative float
      )
      for (i <- largeNumbers.indices) {
        for (j <- i until largeNumbers.length) {
          val a = largeNumbers(i)
          val b = largeNumbers(j)
          testAdd(a, b)
        }
      }
      // Test addition of small numbers
      val smallNumbers = List(
        1e-10f, // Small positive float
        -1e-10f, // Small negative float
        1e-20f, // Very small positive float
        -1e-20f // Very small negative float
      )
      for (i <- smallNumbers.indices) {
        for (j <- i until smallNumbers.length) {
          val a = smallNumbers(i)
          val b = smallNumbers(j)
          testAdd(a, b)
        }
      }
      // Test addition of mixed numbers
      val mixedNumbers = List(
        1e10f, // Large positive float
        -1e10f, // Large negative float
        1e-10f, // Small positive float
        -1e-10f // Small negative float
      )
      for (i <- mixedNumbers.indices) {
        for (j <- i until mixedNumbers.length) {
          val a = mixedNumbers(i)
          val b = mixedNumbers(j)
          testAdd(a, b)
        }
      }
      // Test random numbers of extreme range
      for (_ <- 0 until 100) {
        val a = random.nextFloat() * 1e38f - 1e38f // Random float between -1e38 and 1e38
        val b = random.nextFloat() * 1e38f - 1e38f // Random float between -1e38 and 1e38
        testAdd(a, b)
      }
    }
  }
}
