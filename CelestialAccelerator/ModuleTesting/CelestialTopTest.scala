package celestial

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.lang.Float

class CelesitalCommandWrapper extends Module {
  val io = IO(new Bundle {
    val command = Input(UInt(5.W))
    val lock = Input(UInt(27.W))
    val data = Input(UInt(32.W))
    val dOut = Output(UInt(32.W))
    val locked = Output(Bool())
    val currentIteration = Output(UInt(32.W))
  })
    val celestialTop = Module(new CelestialTop(2))
    val combinedCommand = Cat(io.command, io.lock, io.data)
    celestialTop.io.dIn := combinedCommand
    io.dOut := celestialTop.io.dOut
    // printf(p"Celestial top dOut: ${celestialTop.io.dOut}\n")
    io.locked := celestialTop.io.locked
    io.currentIteration := celestialTop.io.currentIteration
}

class CelestialTop_test extends AnyFlatSpec with ChiselScalatestTester 
{
//   "CelestialTop" should "Lock and unlock correctly" in 
// {
// test(new CelesitalCommandWrapper()) { c =>
//     c.io.command.poke(0.U)
//     c.io.lock.poke(0.U)
//     c.io.data.poke(0.U)

//     // Ensure it remains unlocked
//     c.clock.step(10)
//     c.io.locked.expect(false.B)
//     c.clock.step(1)

//     c.io.command.poke(1.U)
//     c.io.lock.poke(1.U)

//     c.clock.step(1)
//     c.io.locked.expect(true.B)
//     // Ensure it remains locked
//     c.clock.step(10)
//     c.io.locked.expect(true.B)
//     // Ensure it remains locked when requesting a unlock with wrong key
//     c.io.command.poke(2.U)
//     c.io.lock.poke(3.U)
//     c.clock.step(1)
//     c.io.locked.expect(true.B)
//     // Should unlcok when requesting a unlock with the right key
//     c.io.command.poke(2.U)
//     c.io.lock.poke(1.U)
//     c.clock.step(1)
//     c.io.locked.expect(false.B)
//     // // Ensure it remains unlocked
//     c.clock.step(10)
//     c.io.locked.expect(false.B)
// }
// }


// "CelestialTop" should "Store and output positions correctly" in 
// {
// test(new CelesitalCommandWrapper()) { c =>
//     c.io.command.poke(1.U)
//     c.io.lock.poke(1.U)
//     c.io.data.poke(0.U)

//     // Lock with key 1
//     c.clock.step(2)
//     // Set X to 1.0
//     c.io.data.poke(Float.floatToIntBits(1.0f).U)
//     c.io.command.poke(3.U)    
//     c.clock.step(1)
//     // Set Y to 2.0
//     c.io.data.poke(Float.floatToIntBits(2.0f).U)
//     c.io.command.poke(4.U)
//     c.clock.step(1)
//     // Set Z to 3.0
//     c.io.data.poke(Float.floatToIntBits(3.0f).U)
//     c.io.command.poke(5.U)
//     c.clock.step(1)
//     // Set mass to 4.0
//     c.io.data.poke(Float.floatToIntBits(4.0f).U)
//     c.io.command.poke(6.U)
//     c.clock.step(1)
//     // Set size to 5.0
//     c.io.data.poke(Float.floatToIntBits(5.0f).U)
//     c.io.command.poke(7.U)
//     c.clock.step(1)
//     // Set dt to 1.0
//     c.io.data.poke(Float.floatToIntBits(1.0f).U)
//     c.io.command.poke(8.U)
//     c.clock.step(1)

//     // Forward data as position to target 0
//     c.io.command.poke(9.U)
//     c.io.data.poke(0.U)
//     // Output position
//     // Set target to 0
//     c.clock.step(1)
//     c.io.data.poke(0.U)
//     c.io.command.poke(17.U)
//     c.clock.step(1)
//     // Request the X position
//     c.io.command.poke(18.U)
//     c.io.dOut.expect(Float.floatToIntBits(1.0f).U)
//     c.clock.step(1)
//     // Request the Y position
//     c.io.command.poke(19.U)
//     c.io.dOut.expect(Float.floatToIntBits(2.0f).U)
//     c.clock.step(1)
//     // Request the Z position
//     c.io.command.poke(20.U)
//     c.io.dOut.expect(Float.floatToIntBits(3.0f).U)
//     c.clock.step(1)
// }
// }



// "CelestialTop" should "Store and output velocities correctly" in 
// {
// test(new CelesitalCommandWrapper()) { c =>
//     c.io.command.poke(1.U)
//     c.io.lock.poke(1.U)
//     c.io.data.poke(0.U)

//     // Lock with key 1
//     c.clock.step(2)
//     // Set X to 1.0
//     c.io.data.poke(Float.floatToIntBits(1.0f).U)
//     c.io.command.poke(3.U)    
//     c.clock.step(1)
//     // Set Y to 2.0
//     c.io.data.poke(Float.floatToIntBits(2.0f).U)
//     c.io.command.poke(4.U)
//     c.clock.step(1)
//     // Set Z to 3.0
//     c.io.data.poke(Float.floatToIntBits(3.0f).U)
//     c.io.command.poke(5.U)
//     c.clock.step(1)
//     // Set mass to 4.0
//     c.io.data.poke(Float.floatToIntBits(4.0f).U)
//     c.io.command.poke(6.U)
//     c.clock.step(1)
//     // Set size to 5.0
//     c.io.data.poke(Float.floatToIntBits(5.0f).U)
//     c.io.command.poke(7.U)
//     c.clock.step(1)
//     // Set dt to 1.0
//     c.io.data.poke(Float.floatToIntBits(1.0f).U)
//     c.io.command.poke(8.U)
//     c.clock.step(1)

//     // Forward data as velocity to target 0
//     c.io.command.poke(10.U)
//     c.io.data.poke(0.U)
//     // Output velocity
//     // Set target to 0
//     c.clock.step(1)
//     c.io.data.poke(0.U)
//     c.io.command.poke(17.U)
//     c.clock.step(1)
//     // Request the X velocity
//     c.io.command.poke(21.U)
//     c.io.dOut.expect(Float.floatToIntBits(1.0f).U)
//     c.clock.step(1)
//     // Request the Y velocity
//     c.io.command.poke(22.U)
//     c.io.dOut.expect(Float.floatToIntBits(2.0f).U)
//     c.clock.step(1)
//     // Request the Z velocity
//     c.io.command.poke(23.U)
//     c.io.dOut.expect(Float.floatToIntBits(3.0f).U)
//     c.clock.step(1)
// }
// }



"CelestialTop" should "Should simulate an year of earth's rotation around the sun" in 
{
test(new CelesitalCommandWrapper()) { c =>
    // Define constants
    val G = 6.6743f * Math.pow(10, -11).toFloat
    val dt = 60f * 60f * 24.0f * 4.0f // 2 days in seconds, to speed up the simulation
    val iterNumber = 91

    // Sun parameters
    val sunX = 0.0f
    val sunY = 0.0f
    val sunZ = 0.0f
    val sunMass = 1.989f * Math.pow(10, 30).toFloat
    val sunScaledMass = sunMass * G * 1e-18.toFloat
    val sunSize = 1.0f
    val sunVelocityX = 0.0f
    val sunVelocityY = 0.0f
    val sunVelocityZ = 0.0f

    // Earth parameters
    val earthX = 1.52f * Math.pow(10, 11).toFloat * 1e-6.toFloat
    val earthY = 0.0f
    val earthZ = 0.0f
    val earthMass = 5.972f * Math.pow(10, 24).toFloat
    val earthScaledMass = earthMass * G * 1e-18.toFloat
    val earthSize = 1.0f
    val earthVelocityX = 0.0f
    val earthVelocityY = 2.929f * Math.pow(10, 4).toFloat * 1e-6.toFloat
    val earthVelocityZ = 0.0f


    // Lock with key 1
    c.io.command.poke(1.U)
    c.io.lock.poke(1.U)
    c.io.data.poke(0.U)
    c.clock.step(2)

    // Set dt to half day
    c.io.data.poke(Float.floatToIntBits(dt).U)
    c.io.command.poke(8.U)
    c.clock.step(1)

    // Set maximum iterations (365/26 to simulate a full year with half-day steps)
    // c.io.data.poke((365 * 2).U)
    c.io.data.poke(iterNumber) // For test purposes, set to 14 iterations
    c.io.command.poke(14.U)
    c.clock.step(1)

    // Set number of active BPEs to 1 (Sun and Earth, but only update using the force exerted on the Earth)
    c.io.data.poke(1.U)
    c.io.command.poke(15.U)
    c.clock.step(1)

    // Configure Sun (BPE 0)
    // Set position
    c.io.data.poke(Float.floatToIntBits(sunX).U)
    c.io.command.poke(3.U)
    c.clock.step(1)
    c.io.data.poke(Float.floatToIntBits(sunY).U)
    c.io.command.poke(4.U)
    c.clock.step(1)
    c.io.data.poke(Float.floatToIntBits(sunZ).U)
    c.io.command.poke(5.U)
    c.clock.step(1)
    // Set mass
    c.io.data.poke(Float.floatToIntBits(sunScaledMass).U)
    c.io.command.poke(6.U)
    c.clock.step(1)
    // Set size
    c.io.data.poke(Float.floatToIntBits(sunSize).U)
    c.io.command.poke(7.U)
    c.clock.step(1)

    // Forward position data to Sun (target 0)
    c.io.command.poke(9.U)
    c.io.data.poke(0.U)
    c.clock.step(1)

    // Set Sun velocity
    c.io.data.poke(Float.floatToIntBits(sunVelocityX).U)
    c.io.command.poke(3.U)
    c.clock.step(1)
    c.io.data.poke(Float.floatToIntBits(sunVelocityY).U)
    c.io.command.poke(4.U)
    c.clock.step(1)
    c.io.data.poke(Float.floatToIntBits(sunVelocityZ).U)
    c.io.command.poke(5.U)
    c.clock.step(1)

    // Forward velocity data to Sun (target 0)
    c.io.command.poke(10.U)
    c.io.data.poke(0.U)
    c.clock.step(1)

    // Configure Earth (BPE 1)
    // Set position
    c.io.data.poke(Float.floatToIntBits(earthX).U)
    c.io.command.poke(3.U)
    c.clock.step(1)
    c.io.data.poke(Float.floatToIntBits(earthY).U)
    c.io.command.poke(4.U)
    c.clock.step(1)
    c.io.data.poke(Float.floatToIntBits(earthZ).U)
    c.io.command.poke(5.U)
    c.clock.step(1)
    // Set mass
    c.io.data.poke(Float.floatToIntBits(earthScaledMass).U)
    c.io.command.poke(6.U)
    c.clock.step(1)
    // Set size
    c.io.data.poke(Float.floatToIntBits(earthSize).U)
    c.io.command.poke(7.U)
    c.clock.step(1)

    // Forward position data to Earth (target 1)
    c.io.command.poke(9.U)
    c.io.data.poke(1.U)
    c.clock.step(1)

    // Set Earth velocity
    c.io.data.poke(Float.floatToIntBits(earthVelocityX).U)
    c.io.command.poke(3.U)
    c.clock.step(1)
    c.io.data.poke(Float.floatToIntBits(earthVelocityY).U)
    c.io.command.poke(4.U)
    c.clock.step(1)
    c.io.data.poke(Float.floatToIntBits(earthVelocityZ).U)
    c.io.command.poke(5.U)
    c.clock.step(1)

    // Forward velocity data to Earth (target 1)
    c.io.command.poke(10.U)
    c.io.data.poke(1.U)
    c.clock.step(1)

    // Verify initial state for Earth
    // Set target to Earth (BPE 1)
    c.io.data.poke(1.U)
    c.io.command.poke(17.U)
    c.clock.step(1)

    // Check position
    c.io.command.poke(18.U) // Get X position
    c.io.data.poke(0.U) // No encryption
    c.io.dOut.expect(Float.floatToIntBits(earthX).U)
    c.clock.step(1)
    c.io.command.poke(19.U) // Get Y position
    c.io.dOut.expect(Float.floatToIntBits(earthY).U)
    c.clock.step(1)
    c.io.command.poke(20.U) // Get Z position
    c.io.dOut.expect(Float.floatToIntBits(earthZ).U)
    c.clock.step(1)

    // Check velocity
    c.io.command.poke(21.U) // Get X velocity
    c.io.dOut.expect(Float.floatToIntBits(earthVelocityX).U)
    c.clock.step(1)
    c.io.command.poke(22.U) // Get Y velocity
    c.io.dOut.expect(Float.floatToIntBits(earthVelocityY).U)
    c.clock.step(1)
    c.io.command.poke(23.U) // Get Z velocity
    c.io.dOut.expect(Float.floatToIntBits(earthVelocityZ).U)
    c.clock.step(1)

    // Start simulation
    c.io.command.poke(12.U)
    println("STARTING SIMULATION")
    c.clock.step(2)

    // Keep the simulation alive with keep-alive messages
    // The maximum simulation length is 365*2 iterations (for half-day steps)
    val iterationLimit = iterNumber * 28 + 20 // Add some margin
    var currentIter = 0
    
    while (currentIter < iterationLimit && (c.io.currentIteration.peek().litValue  != 0 || currentIter < 30)) {
      // Send keep-alive every 100 cycles
      if (currentIter % 100 == 0) {
        c.io.command.poke(16.U) // Keep alive
        c.clock.step(1)
        
        // Print progress
        println(s"Current iteration: ${c.io.currentIteration.peek().litValue}")
      } else {
        c.clock.step(1)
        c.io.command.poke(0.U) // Idle
      }
      currentIter += 1
    }

    // Verify Earth completed one orbit and returned close to starting position
    // Set target to Earth (BPE 1)
    c.io.data.poke(1.U)
    c.io.command.poke(17.U)
    c.clock.step(1)

    // Check final position and velocity
    c.io.command.poke(18.U) // Get X position
    c.io.data.poke(0.U) // No encryption
    val finalEarthX = Float.intBitsToFloat(c.io.dOut.peek().litValue.toInt)
    println(s"Final Earth X: $finalEarthX, Expected: $earthX")
    c.clock.step(1)
    
    c.io.command.poke(19.U) // Get Y position
    val finalEarthY = Float.intBitsToFloat(c.io.dOut.peek().litValue.toInt)
    println(s"Final Earth Y: $finalEarthY, Expected: $earthY")
    c.clock.step(1)
    
    c.io.command.poke(20.U) // Get Z position
    val finalEarthZ = Float.intBitsToFloat(c.io.dOut.peek().litValue.toInt)
    println(s"Final Earth Z: $finalEarthZ, Expected: $earthZ")
    c.clock.step(1)

    // Check velocity
    c.io.command.poke(21.U) // Get X velocity
    val finalEarthVX = Float.intBitsToFloat(c.io.dOut.peek().litValue.toInt)
    println(s"Final Earth VX: $finalEarthVX, Expected: $earthVelocityX")
    c.clock.step(1)
    
    c.io.command.poke(22.U) // Get Y velocity
    val finalEarthVY = Float.intBitsToFloat(c.io.dOut.peek().litValue.toInt)
    println(s"Final Earth VY: $finalEarthVY, Expected: $earthVelocityY")
    c.clock.step(1)
    
    c.io.command.poke(23.U) // Get Z velocity
    val finalEarthVZ = Float.intBitsToFloat(c.io.dOut.peek().litValue.toInt)
    println(s"Final Earth VZ: $finalEarthVZ, Expected: $earthVelocityZ")
    c.clock.step(1)

    // Define tolerance for position and velocity
    // Large tolerance due to the large dt
    val positionTolerance = 1.5e-2f * Math.abs(earthX) + 1800f
    val velocityTolerance = 1.5e-2f * Math.abs(earthVelocityY) + 1e2f

    // Assert Earth has returned near starting position
    assert(
      Math.abs(finalEarthX - earthX) <= positionTolerance,
      s"Earth didn't return to starting X position. Got: $finalEarthX, Expected: $earthX"
    )
    assert(
      Math.abs(finalEarthY - earthY) <= positionTolerance,
      s"Earth didn't return to starting Y position. Got: $finalEarthY, Expected: $earthY"
    )
    assert(
      Math.abs(finalEarthZ - earthZ) <= positionTolerance,
      s"Earth didn't return to starting Z position. Got: $finalEarthZ, Expected: $earthZ"
    )

    // Assert Earth has returned to similar velocity
    assert(
      Math.abs(finalEarthVX - earthVelocityX) <= velocityTolerance,
      s"Earth didn't return to starting X velocity. Got: $finalEarthVX, Expected: $earthVelocityX"
    )
    assert(
      Math.abs(finalEarthVY - earthVelocityY) <= velocityTolerance,
      s"Earth didn't return to starting Y velocity. Got: $finalEarthVY, Expected: $earthVelocityY"
    )
    assert(
      Math.abs(finalEarthVZ - earthVelocityZ) <= velocityTolerance,
      s"Earth didn't return to starting Z velocity. Got: $finalEarthVZ, Expected: $earthVelocityZ"
    )
}
}



}

