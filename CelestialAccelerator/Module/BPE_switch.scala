package celestial

import chisel3._
import chisel3.util._
import chisel3.experimental._

class BPE_crossbar(val bpe_nbr: Int) extends Module {
  val io = IO(new Bundle {
    // Target has to be log2(bpe) bits
    val target = Input(UInt(log2Ceil(bpe_nbr).W))
    val X_in = Input(UInt(32.W))
    val Y_in = Input(UInt(32.W))
    val Z_in = Input(UInt(32.W))

    val size_in = Input(UInt(32.W))
    val m_in = Input(UInt(32.W))

    val m_slct = Input(UInt(3.W))

    val dt = Input(UInt(32.W))

    val X_out = Output(UInt(32.W))
    val Y_out = Output(UInt(32.W))
    val Z_out = Output(UInt(32.W))

    val collided = Output(Bool())
    val collision_id = Output(UInt(log2Ceil(bpe_nbr).W))
  })

// For debugging purposes, we can print the binary representation of a UInt
  def binStr(x: UInt, width: Int): Printable = {
    var result: Printable = p""
    for (i <- (width - 1) to 0 by -1) {
      result = result + p"${x(i).asUInt}"
    }
    result
  }
  
    val BPUs_io = for (i <- 0 until bpe_nbr) yield {
        val bpu = Module(new BPU())
        bpu.io
    }

    val X_broadcast = WireDefault(0.U(32.W))
    val Y_broadcast = WireDefault(0.U(32.W))
    val Z_broadcast = WireDefault(0.U(32.W))

    val mass_broadcast = WireDefault(0.U(32.W))
    val size_broadcast = WireDefault(0.U(32.W))

    io.X_out := 0.U
    io.Y_out := 0.U
    io.Z_out := 0.U

    io.collision_id := PriorityEncoder(BPUs_io.map(_.collided))
    // Output 1 if any BPU has a collision
    io.collided := BPUs_io.map(_.collided).reduce(_ || _)

    for (i <- 0 until bpe_nbr) {
        // Default values
        BPUs_io(i).X_in := 0.U
        BPUs_io(i).Y_in := 0.U
        BPUs_io(i).Z_in := 0.U
        BPUs_io(i).size_in := 0.U
        BPUs_io(i).m_in := 0.U
        BPUs_io(i).dt := 0.U
        BPUs_io(i).m_slct := 6.U // 6 = idle
    

        switch(io.m_slct) {
            is(0.U) { // 0 = update velocity
                // -> Broadcast target BPU's coordinate to all other BPUs
                when (io.target === i.U) {
                    // Inputs can remain at default values
                    X_broadcast := BPUs_io(i).X_out
                    Y_broadcast := BPUs_io(i).Y_out
                    Z_broadcast := BPUs_io(i).Z_out

                    mass_broadcast := BPUs_io(i).m_out
                    size_broadcast := BPUs_io(i).size_out
                }.otherwise {
                    BPUs_io(i).X_in := X_broadcast
                    BPUs_io(i).Y_in := Y_broadcast
                    BPUs_io(i).Z_in := Z_broadcast
                    BPUs_io(i).m_in := mass_broadcast
                    BPUs_io(i).size_in := size_broadcast
                    BPUs_io(i).dt := io.dt
                    BPUs_io(i).m_slct := 0.U // 0 = update velocity
                }
            }
            is(1.U) { // 1 = update position
                BPUs_io(i).dt := io.dt
                BPUs_io(i).m_slct := 1.U // 1 = update position
            }
            is(2.U) { // 2 = set position
                when (io.target === i.U) {
                    BPUs_io(i).X_in := io.X_in
                    BPUs_io(i).Y_in := io.Y_in
                    BPUs_io(i).Z_in := io.Z_in
                    BPUs_io(i).size_in := io.size_in
                    BPUs_io(i).m_in := io.m_in
                    BPUs_io(i).m_slct := 2.U // 2 = set position
                }
            }
            is(3.U) { // 3 = set velocity
                when (io.target === i.U) {
                    BPUs_io(i).X_in := io.X_in
                    BPUs_io(i).Y_in := io.Y_in
                    BPUs_io(i).Z_in := io.Z_in
                    BPUs_io(i).m_slct := 3.U // 3 = set velocity
                }
            }
            is(4.U) { // 4 = output velocity of target
                when (io.target === i.U) {
                    BPUs_io(i).m_slct := 4.U // 4 = output velocity

                    io.X_out := BPUs_io(i).X_out
                    io.Y_out := BPUs_io(i).Y_out
                    io.Z_out := BPUs_io(i).Z_out
                }
            }
            is(5.U) { // 5 = reset all BPUs
                BPUs_io(i).m_slct := 5.U 
            }
            is(6.U) { // 6 = output position of target
                when (io.target === i.U) {
                    BPUs_io(i).m_slct := 6.U // 6 = do nothing, but still outputs position

                    io.X_out := BPUs_io(i).X_out
                    io.Y_out := BPUs_io(i).Y_out
                    io.Z_out := BPUs_io(i).Z_out
                }
            }

        }
    }
}