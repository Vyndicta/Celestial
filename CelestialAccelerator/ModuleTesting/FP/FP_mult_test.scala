package celestial

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.lang.Float.{intBitsToFloat, floatToIntBits}

class F32MultiplierTester extends AnyFlatSpec with ChiselScalatestTester {
  "F32Multiplier" should "multiply floats correctly" in {
    test(new F32Multiplier) { dut =>
      def testMul(a: Float, b: Float): Unit = {
        val expected = a * b
        val aBits = java.lang.Integer.toUnsignedLong(floatToIntBits(a))
        val bBits = java.lang.Integer.toUnsignedLong(floatToIntBits(b))

        dut.io.a.poke(aBits.U(32.W))
        dut.io.b.poke(bBits.U(32.W))
        dut.clock.step(1)

        val out = dut.io.out.peek().litValue.toInt
        val outFloat = intBitsToFloat(out)

        println(f"---------Test: $a * $b = $outFloat%.6f (expected $expected%.6f)----------------")

        if (outFloat.isNaN) {
          assert(java.lang.Float.isNaN(expected), s"Failed on $a * $b")
        } else if (expected.isNaN) {
          assert(java.lang.Float.isNaN(outFloat), s"Failed on $a * $b")
        } else if (expected == Float.PositiveInfinity || expected == Float.NegativeInfinity) {
          assert(java.lang.Float.isInfinite(outFloat), s"Failed on $a * $b")
        } else {
          val smallestFixedPoint = 1.18e-38f
          assert(math.abs(outFloat - expected) <= math.abs(expected) * 0.0001f || math.abs(outFloat - expected) <= smallestFixedPoint, s"Failed on $a * $b: got $outFloat, expected $expected")
        }
      }

      val specialValues = List(
        0.0f, -0.0f,
        1.0f, -1.0f,
        Float.MinValue, Float.MaxValue,
        Float.NaN,
        Float.PositiveInfinity, Float.NegativeInfinity
      )

      for (i <- specialValues.indices; j <- i until specialValues.length) {
        val a = specialValues(i)
        val b = specialValues(j)
        testMul(a, b)
      }

      val random = new scala.util.Random
      for (_ <- 0 until 100) {
        val a = random.nextFloat() * 1e10f - 5e9f
        val b = random.nextFloat() * 1e10f - 5e9f
        testMul(a, b)
      }

      val extremeValues = List(
        1e-30f, -1e-30f,
        1e30f, -1e30f
      )
      for (i <- extremeValues.indices; j <- i until extremeValues.length) {
        testMul(extremeValues(i), extremeValues(j))
      }
    }
  }
}
