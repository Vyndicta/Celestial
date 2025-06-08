package celestial

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.lang.Float
import scala.math.sqrt


class CrossBar_test extends AnyFlatSpec with ChiselScalatestTester {
  def binStr(x: UInt, width: Int): Printable = {
    var result: Printable = p""
    for (i <- (width - 1) to 0 by -1) {
      result = result + p"${x(i).asUInt}"
    }
    result
  }
  "BPU" should "Work with earth and sun" in {
    test(new BPE_crossbar(2)) { dut =>
      // 0 = sun
      // 1 = earth

      // Set the sun's position, mass, size and velocity
      val sunX = 0.0f
      val sunY = 0.0f
      val sunZ = 0.0f
      val sunMass = 1.989f * Math.pow(10, 30).toFloat
      val G = 6.6743f * Math.pow(10, -11).toFloat
      val sunScaledMass = sunMass * G * 1e-18.toFloat
      val sunSize = 1.0f // Won't be checking collisions so it doesn't matter
      val sunVelocityX = 0.0f
      val sunVelocityY = 0.0f
      val sunVelocityZ = 0.0f


      val dt = 60f * 60f * 24.0f * 2.0f // Time step of a half day
      dut.io.dt.poke(Float.floatToIntBits(dt).U)

      dut.io.X_in.poke(Float.floatToIntBits(sunX).U)
      dut.io.Y_in.poke(Float.floatToIntBits(sunY).U)
      dut.io.Z_in.poke(Float.floatToIntBits(sunZ).U)
      dut.io.m_in.poke(Float.floatToIntBits(sunScaledMass).U)
      dut.io.size_in.poke(Float.floatToIntBits(sunSize).U)

      dut.io.target.poke(0.U) // 0 = sun
      dut.io.m_slct.poke(2.U) 

      dut.clock.step(1)

      dut.io.X_in.poke(Float.floatToIntBits(sunVelocityX).U)
      dut.io.Y_in.poke(Float.floatToIntBits(sunVelocityY).U)
      dut.io.Z_in.poke(Float.floatToIntBits(sunVelocityZ).U)

      dut.io.m_slct.poke(3.U)
      dut.clock.step(1)

      val earthX = 1.52f * Math.pow(10, 11).toFloat * 1e-6.toFloat
      val earthY = 0.0f
      val earthZ = 0.0f

      val earthMass = 5.972f * Math.pow(10, 24).toFloat
      val earthScaledMass = earthMass * G * 1e-18.toFloat  
      val earthSize = 1.0f // Won't be checking collisions so it doesn't matter
      val earthVelocityX = 0.0f
      val earthVelocityY = 2.929f * Math.pow(10, 4).toFloat * 1e-6.toFloat // Approximate velocity of Earth in m/s at the apoapsis
      val earthVelocityZ = 0.0f
      dut.io.X_in.poke(Float.floatToIntBits(earthX).U)
      dut.io.Y_in.poke(Float.floatToIntBits(earthY).U)
      dut.io.Z_in.poke(Float.floatToIntBits(earthZ).U)
      dut.io.m_in.poke(Float.floatToIntBits(earthScaledMass).U)
      dut.io.size_in.poke(Float.floatToIntBits(earthSize).U)
      dut.io.target.poke(1.U) // 1 = earth
      dut.io.m_slct.poke(2.U) // 2 = set position

      dut.clock.step(1)

      dut.io.X_in.poke(Float.floatToIntBits(earthVelocityX).U)
      dut.io.Y_in.poke(Float.floatToIntBits(earthVelocityY).U)
      dut.io.Z_in.poke(Float.floatToIntBits(earthVelocityZ).U)
      dut.io.m_slct.poke(3.U) // 3 = set velocity

      dut.clock.step(1)

      // Check that the position and velocity of earth are set correctly
      dut.io.m_slct.poke(4.U) // 4 = output velocity
      dut.io.X_out.expect(Float.floatToIntBits(earthVelocityX).U)
      dut.io.Y_out.expect(Float.floatToIntBits(earthVelocityY).U)
      dut.io.Z_out.expect(Float.floatToIntBits(earthVelocityZ).U)

      dut.clock.step(1)

      dut.io.m_slct.poke(6.U) // 5 = output position
      dut.io.X_out.expect(Float.floatToIntBits(earthX).U)
      dut.io.Y_out.expect(Float.floatToIntBits(earthY).U)
      dut.io.Z_out.expect(Float.floatToIntBits(earthZ).U)
      dut.clock.step(1)

      // Alternate between updating position and velocity for half a year, so 180 * 4 quarter days
      for (i <- 0 until (365-1)/2) { //179 ? 
        for (j <- 0 until 4) { // Must keep on poking the same value for 4 cycles
          // Update position
          dut.io.m_slct.poke(1.U) // 1 = update position
          dut.clock.step(1)
        }
        for (j <- 0 until 24) { // Must keep on poking the same value for 24 cycles
          dut.io.m_slct.poke(0.U) // 0 = update velocity
          // Set target to 0 = sun
          dut.io.target.poke(0.U)
          dut.clock.step(1)
        }

      }

      dut.io.m_slct.poke(6.U) // 6  = output position
      dut.io.target.poke(1.U)
      // Check that the position and velocity of earth are set correctly=
      println("Earth position: ")
      println(f"X: ${Float.intBitsToFloat(dut.io.X_out.peek().litValue.toInt)}")
      println(f"Y: ${Float.intBitsToFloat(dut.io.Y_out.peek().litValue.toInt)}")
      println(f"Z: ${Float.intBitsToFloat(dut.io.Z_out.peek().litValue.toInt)}")

      val expectedEarthPositionX = 1.52f * Math.pow(10, 11).toFloat * 1e-6.toFloat
      val expectedEarthPositionY = 0.0f
      val expectedEarthPositionZ = 0.0f
      // Tolerate up to 1e-2 relative error
      val tolerancePos = 1e-2f
      val toleranceAbs = 1800
      assert(
        Math.abs(Float.intBitsToFloat(dut.io.X_out.peek().litValue.toInt) - expectedEarthPositionX) <= tolerancePos * Math.abs(expectedEarthPositionX) + toleranceAbs,
        s"Expected X position: $expectedEarthPositionX, got: ${Float.intBitsToFloat(dut.io.X_out.peek().litValue.toInt)}"
      )
      assert(
        Math.abs(Float.intBitsToFloat(dut.io.Y_out.peek().litValue.toInt) - expectedEarthPositionY) <= tolerancePos * Math.abs(expectedEarthPositionY) + toleranceAbs,
        s"Expected Y position: $expectedEarthPositionY, got: ${Float.intBitsToFloat(dut.io.Y_out.peek().litValue.toInt)}"
      )
      assert(
        Math.abs(Float.intBitsToFloat(dut.io.Z_out.peek().litValue.toInt) - expectedEarthPositionZ) <= tolerancePos * Math.abs(expectedEarthPositionZ) + toleranceAbs,
        s"Expected Z position: $expectedEarthPositionZ, got: ${Float.intBitsToFloat(dut.io.Z_out.peek().litValue.toInt)}"
      )


      dut.clock.step(1)

      dut.io.m_slct.poke(4.U) // 4 = output velocity
      val expectedEearthVelocityX = 0.0f
      val expectedEearthVelocityY = -2.929f * Math.pow(10, 4).toFloat * 1e-6.toFloat
      val expectedEearthVelocityZ = 0.0f
      // Tolerate up to 1e-4 relative error
      val tolerance = 1e-2f
      val toleranceAbsVel = 1e2f
      assert(
        Math.abs(Float.intBitsToFloat(dut.io.X_out.peek().litValue.toInt) - expectedEearthVelocityX) < tolerance * Math.abs(expectedEearthVelocityX) + toleranceAbsVel,
        s"Expected X velocity: $expectedEearthVelocityX, got: ${Float.intBitsToFloat(dut.io.X_out.peek().litValue.toInt)}"
      )
      assert(
        Math.abs(Float.intBitsToFloat(dut.io.Y_out.peek().litValue.toInt) - expectedEearthVelocityY) < tolerance * Math.abs(expectedEearthVelocityY) + toleranceAbsVel,
        s"Expected Y velocity: $expectedEearthVelocityY, got: ${Float.intBitsToFloat(dut.io.Y_out.peek().litValue.toInt)}"
      )
      assert(
        Math.abs(Float.intBitsToFloat(dut.io.Z_out.peek().litValue.toInt) - expectedEearthVelocityZ) < tolerance * Math.abs(expectedEearthVelocityZ) + toleranceAbsVel,
        s"Expected Z velocity: $expectedEearthVelocityZ, got: ${Float.intBitsToFloat(dut.io.Z_out.peek().litValue.toInt)}"
      )
    }
  }


}

