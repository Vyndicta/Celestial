package celestial

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.lang.Float.{intBitsToFloat, floatToIntBits}
import scala.math.{pow, sqrt}

class F32FastNegThreeHalfRefinerTesterWrapper extends Module {
  val io = IO(new Bundle {
    val xExpThree = Input(UInt(32.W))
    val approx = Input(UInt(32.W))
    val refinedOut = Output(UInt(32.W))
    val rst = Input(Bool())
  })

  val refiner = Module(new NegThreeHalfExpRefine)
  val add = Module(new F32Adder())
  val mul = Module(new F32Multiplier())

  // Default dummy connections (safe for FIRRTL)
  refiner.io.inExpThree := io.xExpThree
  refiner.io.approx := io.approx

  mul.io.a := refiner.io.mulA
  mul.io.b := refiner.io.mulB

  add.io.substracter := true.B

  add.io.a := refiner.io.subA
  add.io.b := refiner.io.subB

  refiner.io.mulOut := mul.io.out
  refiner.io.subOut := add.io.sum
  refiner.io.rst := io.rst

  io.refinedOut := refiner.io.out
}


class F32FastNegThreeHalfRefinerTester extends AnyFlatSpec with ChiselScalatestTester {
  "F32FastInverseSqrt" should "calculate x^3/2 correctly" in {
    test(new F32FastNegThreeHalfRefinerTesterWrapper) { dut =>
      def testNegThreeHalf(x: Float): Unit = {
        val y = (1.0f / pow(x, 1.5)).toFloat * 1.02f
        val expected_refined_value = y*(3-x*x*x*y*y)/2.0

        val xE3Bits = java.lang.Integer.toUnsignedLong(floatToIntBits(x*x*x))
        val yBits = java.lang.Integer.toUnsignedLong(floatToIntBits(y))

        dut.io.xExpThree.poke(xE3Bits.U)
        dut.io.approx.poke(yBits.U)
        dut.io.rst.poke(true.B)
        dut.clock.step(1)
        dut.io.rst.poke(false.B)

        dut.clock.step(3)
        val out = dut.io.refinedOut.peek().litValue.toInt
        val outFloat = intBitsToFloat(out)
        
        println(f"---------Test: 1/sqrt($x) = $outFloat%.6f (expected $expected_refined_value%.6f)----------------")

      }

      val specialValues = List(1.0f, 4.0f, 9.0f, 16.0f)
      for (x <- specialValues) {
        testNegThreeHalf(x)
      }
    }
  }
}