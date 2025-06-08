package celestial

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import java.lang.Float

class CelestialTop_testCstmPckt extends AnyFlatSpec with ChiselScalatestTester 
{
  "CelestialTop" should "React as in MMIO" in 
{
test(new CelestialTop(4)) { c =>
    val pckts_to_send : List[Long] = List(
      0x0801234500000000L,
      0x180123453DCCCCCDL,
      0x2001234500000000L,
      0x2801234500000000L,
      0x4801234500000000L,
      0x1000458700000000L,
      0x1001234500000000L,
      0x0000000000000000L
    )
    // Send each of the packets and print the state of the lock
    for (pckt <- pckts_to_send) {
      c.io.dIn.poke(pckt.U)
      println(p"Sending packet: ${pckt.toHexString}")
      
      c.clock.step(1)
    }
}
}





}

