package celestial

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.lang.Float.{intBitsToFloat, floatToIntBits}

class F32InverterTester extends AnyFlatSpec with ChiselScalatestTester {
  "F32Inverter" should "compute 1/x correctly" in {
    test(new FP32Inverter) { dut =>
      def testInv(x: Float): Unit = {
        val expected = 1.0f / x
        val aBits = java.lang.Integer.toUnsignedLong(floatToIntBits(x))
        val expectedBits = java.lang.Integer.toUnsignedLong(floatToIntBits(expected))

        dut.io.in.poke(aBits.U(32.W))
        dut.clock.step(1)
        val out = dut.io.out.peek().litValue.toInt
        val outFloat = intBitsToFloat(out)
        println(f"----------------------Test: 1/$x = $outFloat%.6f (expected $expected%.6f)----------------------")
        if (outFloat.isNaN) {
          assert(java.lang.Float.isNaN(expected), s"Failed on 1/$x")
        } else if (expected.isNaN) {
          assert(java.lang.Float.isNaN(outFloat), s"Failed on 1/$x")
        } else if (expected == Float.PositiveInfinity) {
          assert(java.lang.Float.isInfinite(outFloat), s"Failed on 1/$x")
        } else if (expected == Float.NegativeInfinity) {
          assert(java.lang.Float.isInfinite(outFloat), s"Failed on 1/$x")
        } else {
          val smallestFixedPoint = 1.18e-38f
          assert(math.abs(outFloat - expected) <= math.abs(expected) * 0.0001f || math.abs(outFloat - expected) <= smallestFixedPoint, s"Failed on 1/$x: got $outFloat, expected $expected")
        }
      }
      testInv(1.0f) 
      testInv(4.1832361E9f) // Test with a large positive number      

      //testInv(0.0f) // Test with zero

      val rand = new scala.util.Random
      for (_ <- 0 until 500) {
        val v = rand.nextFloat() * 1e10f - 5e9f
        if (v != 0.0f) testInv(v)
      }

      val testValues = List(
        1.0f,
        -1.0f,
        2.0f,
        -2.0f,
        0.5f,
        -0.5f,
        0.0f,
        -0.0f,
        Float.MaxValue,
        Float.MinValue,
        Float.PositiveInfinity,
        Float.NegativeInfinity,
        Float.NaN
      )

      for (value <- testValues) {
        testInv(value)
      }

    }
  }
}
