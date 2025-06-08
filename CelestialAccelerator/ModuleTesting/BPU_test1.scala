package celestial

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.lang.Float


class BPU_test extends AnyFlatSpec with ChiselScalatestTester {
  "BPU" should "Set position and velocity to input" in {
    test(new BPU) { c =>
      // Test 1 : send in position, mass, and size
      c.io.X_in.poke(10.U)
      c.io.Y_in.poke(20.U)
      c.io.Z_in.poke(30.U)
      c.io.m_in.poke(40.U)
      c.io.m_slct.poke(2.U)
      c.io.size_in.poke(15.U)
      c.clock.step(1)
      c.io.m_slct.poke(6.U)
      c.io.X_out.expect(10.U)
      c.io.Y_out.expect(20.U)
      c.io.Z_out.expect(30.U)
      c.io.m_out.expect(40.U)
      c.io.size_out.expect(15.U)
      c.clock.step(1)
      // Test 2 : set velocity
      c.io.X_in.poke(1.U)
      c.io.Y_in.poke(2.U)
      c.io.Z_in.poke(3.U)
      c.io.m_slct.poke(3.U)
      c.clock.step(1)
      c.io.m_slct.poke(6.U)
      // Should output previously set position
      c.io.X_out.expect(10.U)
      c.io.Y_out.expect(20.U)
      c.io.Z_out.expect(30.U)
    }
  }

  "BPU" should "Update velocity as expected for various parameters" in {
    test(new BPU) { dut =>

      // Define sets of parameters to try
      val testCases = Seq(
        (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
        (1.0, 2.0, 3.0, 0.1, -0.2, 0.3, 2.0, 2.0, 4.0, 5.0, 1.5, 1.5, 0.5),
        (-1.0, -1.0, -1.0, -0.5, 0.5, 0.0, 0.5, 1.0, -2.0, -2.0, 1.0, 1.0, 2.0),
        (5.0, 5.0, 5.0, 1.0, 1.0, 1.0, 10.0, 1.0, 6.0, 5.0, 2.0, 2.0, 0.1)
        // Add more test cases as needed
      )
      // Format: (startPosX, startPosY, startPosZ, startVelX, startVelY, startVelZ, mass1, startSize, secondPosX, secondPosY, secondMass, secondSize, dt)

      for ((startPosX, startPosY, startPosZ, startVelX, startVelY, startVelZ, mass1, startSize, secondPosX, secondPosY, secondMass, secondSize, dt) <- testCases) {

        println(s"Testing with startPos=($startPosX, $startPosY, $startPosZ), startVel=($startVelX, $startVelY, $startVelZ), mass1=$mass1, startSize=$startSize, secondPos=($secondPosX, $secondPosY), secondMass=$secondMass, secondSize=$secondSize, dt=$dt")

        val startPosXBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(startPosX.toFloat))
        val startPosYBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(startPosY.toFloat))
        val startPosZBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(startPosZ.toFloat))

        val startVelocityXBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(startVelX.toFloat))
        val startVelocityYBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(startVelY.toFloat))
        val startVelocityZBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(startVelZ.toFloat))

        val mass1Bits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(mass1.toFloat))
        val startSizeBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(startSize.toFloat))
        val dtBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(dt.toFloat))

        val secondObjectMassBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(secondMass.toFloat))
        val secondObjectPosXBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(secondPosX.toFloat))
        val secondObjectPosYBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(secondPosY.toFloat))
        val secondObjectPosZBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(0.0f)) // fixed Z for second object
        val secondObjectSizeBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(secondSize.toFloat))

        val distanceX = secondPosX - startPosX
        val distanceY = secondPosY - startPosY
        val distanceZ = 0.0 - startPosZ

        val distanceSquared = distanceX * distanceX + distanceY * distanceY + distanceZ * distanceZ
        val distance = math.sqrt(distanceSquared)

        val distanceXN = distanceX / distance
        val distanceYN = distanceY / distance
        val distanceZN = distanceZ / distance

        val multiplier = secondMass * dt / distanceSquared

        val expectedVelocityX = startVelX + distanceXN * multiplier
        val expectedVelocityY = startVelY + distanceYN * multiplier
        val expectedVelocityZ = startVelZ + distanceZN * multiplier

        // Step 1: Set initial position, mass, size
        dut.io.m_slct.poke(2.U)
        dut.io.X_in.poke(startPosXBits.U)
        dut.io.Y_in.poke(startPosYBits.U)
        dut.io.Z_in.poke(startPosZBits.U)
        dut.io.m_in.poke(mass1Bits.U)
        dut.io.size_in.poke(startSizeBits.U)
        dut.io.dt.poke(dtBits.U)
        dut.clock.step(1)

        // Step 2: Set initial velocity
        dut.io.m_slct.poke(3.U)
        dut.io.X_in.poke(startVelocityXBits.U)
        dut.io.Y_in.poke(startVelocityYBits.U)
        dut.io.Z_in.poke(startVelocityZBits.U)
        dut.clock.step(1)

        // Step 3: Velocity update
        dut.io.m_slct.poke(0.U)
        dut.io.X_in.poke(secondObjectPosXBits.U)
        dut.io.Y_in.poke(secondObjectPosYBits.U)
        dut.io.Z_in.poke(secondObjectPosZBits.U)
        dut.io.m_in.poke(secondObjectMassBits.U)
        dut.io.size_in.poke(secondObjectSizeBits.U)

        dut.clock.step(24)

        // Step 4: Check the updated velocity
        dut.io.m_slct.poke(4.U)

        val actualVelocityX = dut.io.X_out.peek().litValue.toInt
        val actualVelocityY = dut.io.Y_out.peek().litValue.toInt
        val actualVelocityZ = dut.io.Z_out.peek().litValue.toInt

        val actualVelocityXFloat = Float.intBitsToFloat(actualVelocityX)
        val actualVelocityYFloat = Float.intBitsToFloat(actualVelocityY)
        val actualVelocityZFloat = Float.intBitsToFloat(actualVelocityZ)

        // If distance < sizeStart + sizeSecond, then we expect a collision
        val expectCollision = (math.abs(distance) < (startSize + secondSize))
        // println("Size start: " + startSize)
        // println("Size second: " + secondSize)
        // println("Distance: " + distance)
        // println("Expect collision: " + expectCollision)
        dut.io.collided.expect(expectCollision.B)

        println(f"Expected Velocity: ($expectedVelocityX%.6f, $expectedVelocityY%.6f, $expectedVelocityZ%.6f)")
        println(f"Actual   Velocity: ($actualVelocityXFloat%.6f, $actualVelocityYFloat%.6f, $actualVelocityZFloat%.6f)")

        val tolerance = 1e-5

        assert(math.abs(actualVelocityXFloat - expectedVelocityX) <= math.abs(expectedVelocityX) * tolerance, s"Failed on velocity X: got $actualVelocityXFloat, expected $expectedVelocityX")
        assert(math.abs(actualVelocityYFloat - expectedVelocityY) <= math.abs(expectedVelocityY) * tolerance, s"Failed on velocity Y: got $actualVelocityYFloat, expected $expectedVelocityY")
        assert(math.abs(actualVelocityZFloat - expectedVelocityZ) <= math.abs(expectedVelocityZ) * tolerance, s"Failed on velocity Z: got $actualVelocityZFloat, expected $expectedVelocityZ")
      }
    }
  }

  "BPU" should "Update position as expected for various parameters" in {
  test(new BPU) { dut =>

    val testCases = Seq(
      (0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 1.0),
      (5.0, -3.0, 2.0, 0.5, 1.0, -0.5, 0.1),
      (-2.0, 4.0, 1.0, -1.0, -1.0, 0.0, 2.0),
      (10.0, 10.0, 10.0, 5.0, 5.0, 5.0, 0.5)
      // (startPosX, startPosY, startPosZ, velX, velY, velZ, dt)
    )

    for ((startPosX, startPosY, startPosZ, velocityX, velocityY, velocityZ, dt) <- testCases) {

      println(s"Testing position update with startPos=($startPosX, $startPosY, $startPosZ), velocity=($velocityX, $velocityY, $velocityZ), dt=$dt")

      val startPosXBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(startPosX.toFloat))
      val startPosYBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(startPosY.toFloat))
      val startPosZBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(startPosZ.toFloat))

      val velocityXBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(velocityX.toFloat))
      val velocityYBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(velocityY.toFloat))
      val velocityZBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(velocityZ.toFloat))

      val dtBits = java.lang.Integer.toUnsignedLong(Float.floatToIntBits(dt.toFloat))

      // Expected new position = old position + velocity * dt
      val expectedPosX = startPosX + velocityX * dt
      val expectedPosY = startPosY + velocityY * dt
      val expectedPosZ = startPosZ + velocityZ * dt

      // Step 1: Set initial position
      dut.io.m_slct.poke(2.U)
      dut.io.X_in.poke(startPosXBits.U)
      dut.io.Y_in.poke(startPosYBits.U)
      dut.io.Z_in.poke(startPosZBits.U)
      dut.io.dt.poke(dtBits.U)
      dut.clock.step(1)

      // Step 2: Set initial velocity
      dut.io.m_slct.poke(3.U)
      dut.io.X_in.poke(velocityXBits.U)
      dut.io.Y_in.poke(velocityYBits.U)
      dut.io.Z_in.poke(velocityZBits.U)
      dut.clock.step(1)

      // Step 3: Request position update
      dut.io.m_slct.poke(1.U)
      dut.clock.step(5) // Wait 4 cycles for the position to update

      // Step 4: Read back the updated position
      val actualPosX = dut.io.X_out.peek().litValue.toInt
      val actualPosY = dut.io.Y_out.peek().litValue.toInt
      val actualPosZ = dut.io.Z_out.peek().litValue.toInt

      val actualPosXFloat = Float.intBitsToFloat(actualPosX)
      val actualPosYFloat = Float.intBitsToFloat(actualPosY)
      val actualPosZFloat = Float.intBitsToFloat(actualPosZ)

      val tolerance = 1e-5

      assert(math.abs(actualPosXFloat - expectedPosX) <= math.abs(expectedPosX) * tolerance, s"Failed on X: got $actualPosXFloat, expected $expectedPosX")
      assert(math.abs(actualPosYFloat - expectedPosY) <= math.abs(expectedPosY) * tolerance, s"Failed on Y: got $actualPosYFloat, expected $expectedPosY")
      assert(math.abs(actualPosZFloat - expectedPosZ) <= math.abs(expectedPosZ) * tolerance, s"Failed on Z: got $actualPosZFloat, expected $expectedPosZ")
    }
  }
}

}

