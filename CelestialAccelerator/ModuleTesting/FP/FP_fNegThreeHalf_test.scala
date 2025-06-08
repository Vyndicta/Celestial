package celestial

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.lang.Float.{intBitsToFloat, floatToIntBits}
import scala.math.pow
class F32FastNegThreeHalfFullTesterTesterWrapper extends Module {
  val io = IO(new Bundle {
    val in  = Input(UInt(32.W))
    val rst = Input(Bool())
    val out = Output(UInt(32.W))
  })
  
  // Create an adder, a multiplier, and a neg three half exp module
  val adder = Module(new F32Adder())
  val multiplier = Module(new F32Multiplier())
  val negThreeHalfExp = Module(new NegThreeHalfExp)

  // Connect the neg three half exp module to the adder and multiplier
  negThreeHalfExp.io.in := io.in
  negThreeHalfExp.io.rst := io.rst
  multiplier.io.a := negThreeHalfExp.io.mulA
  multiplier.io.b := negThreeHalfExp.io.mulB
  adder.io.a := negThreeHalfExp.io.subA
  adder.io.b := negThreeHalfExp.io.subB
  adder.io.substracter := true.B
  negThreeHalfExp.io.mulOut := multiplier.io.out
  negThreeHalfExp.io.subOut := adder.io.sum
  io.out := negThreeHalfExp.io.out
}

class F32FastNegThreeHalfFullTester extends AnyFlatSpec with ChiselScalatestTester {
  "F32FastNegThreeHalf" should "calculate x^(-3/2) correctly" in {
    test(new F32FastNegThreeHalfFullTesterTesterWrapper) { dut =>
      def testNegThreeHalf(x: Float): Unit = {
        val expected = (1.0f / pow(x, 1.5)).toFloat

        val xBits = java.lang.Integer.toUnsignedLong(floatToIntBits(x))
        
        dut.io.rst.poke(true.B)
        dut.io.in.poke(xBits.U)
        dut.clock.step(1)
        dut.io.rst.poke(false.B)

        // Wait for sufficient clock cycles for three refinements + initial work
        dut.clock.step(14)

        val out = dut.io.out.peek().litValue.toInt
        println(f"out: ${out.toBinaryString} (${out.toHexString})")
        val outFloat = intBitsToFloat(out)

        println(f"---------Test: ($x)^(-3/2) = $outFloat%.6f (expected $expected%.6f)---------")

        if (outFloat.isNaN) {
          assert(java.lang.Float.isNaN(expected), s"Failed on x^(-3/2) for $x")
        } else if (expected.isNaN) {
          assert(java.lang.Float.isNaN(outFloat), s"Failed on x^(-3/2) for $x")
        } else if (expected == Float.PositiveInfinity || expected == Float.NegativeInfinity) {
          assert(java.lang.Float.isInfinite(outFloat), s"Failed on x^(-3/2) for $x")
        } else {
          val smallestFixedPoint = 1.18e-38f
          val relativeError = if (expected != 0) math.abs((outFloat - expected) / expected) else math.abs(outFloat - expected)
          val absoluteError = math.abs(outFloat - expected)

          assert(relativeError <= 0.001f || absoluteError <= smallestFixedPoint, 
            s"Failed on ($x)^(-3/2): got $outFloat, expected $expected, relative error: $relativeError")
        }
        
      }

      // Special test values
      val specialValues = List(
        0.0f,                   // Should return +Inf
        1.0f,                   // Should return 1.0
        4.0f,                   // Should return 0.5
        9.0f,                   // Should return ~0.33333
        16.0f,                  // Should return 0.25
        Float.NaN               // Should return NaN
      )

      for (x <- specialValues) {
        testNegThreeHalf(x)
      }

      // Test negative inputs
      testNegThreeHalf(-1.0f)
      testNegThreeHalf(-4.0f)

      // Test random positive floats
      val random = new scala.util.Random(123)
      for (_ <- 0 until 30) {
        val exponent = random.nextInt(30) - 15
        val mantissa = 1.0f + random.nextFloat()
        val x = mantissa * math.pow(2.0f, exponent).toFloat
        testNegThreeHalf(x)
      }

      // Values inspired by Quake fast inverse sqrt
      val quakeValues = List(
        0.15f, 2.0f, 3.14159f, 4.0f, 100.0f, 10000.0f
      )

      for (x <- quakeValues) {
        testNegThreeHalf(x)
      }
    }
  }
}
