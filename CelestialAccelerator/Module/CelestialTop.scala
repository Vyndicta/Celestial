package celestial

import chisel3._
import chisel3.util._
import chisel3.experimental._

class CelestialTop(val BPE_num: Int) extends Module {
  val io = IO(new Bundle {
  val dOut = Output(UInt(32.W))
  val locked = Output(Bool())
  val currentIteration = Output(UInt(32.W))
  val dIn = Input(UInt(64.W))
  })

// For debugging purposes, to print the binary representation of a UInt
  def binStr(x: UInt, width: Int): Printable = {
    var result: Printable = p""
    for (i <- (width - 1) to 0 by -1) {
      result = result + p"${x(i).asUInt}"
    }
    result
  }
  val bp_switch = Module(new BPE_switch(BPE_num))


  val command = io.dIn(63, 59) // 32 possible commands
  val key = io.dIn(58, 32) // 27 bits for key
  val data = io.dIn(31, 0) // 32 bits for data.

  val lock_key = RegInit(0.U(27.W))

  val unlock_timeout = 100000.U(14.W) // Timeout for unlocking, in cycles
  val last_valid_pckt_received_cnt = RegInit(0.U(14.W))

  val dt = RegInit(0.U(32.W))
  val m = RegInit(0.U(32.W))
  val size = RegInit(0.U(32.W))
  
  val X = RegInit(0.U(32.W))
  val Y = RegInit(0.U(32.W))
  val Z = RegInit(0.U(32.W))

  val numberActiveBPE = RegInit(0.U(log2Ceil(BPE_num).W)) // To update only using the BPE holding data

  io.locked := (lock_key =/= 0.U) // Unlock if key is set to 0

  defaultValues() // Set default values for the BPE switch, can be modified below depending on the command

  val s_idle :: sRunning :: Nil = Enum(2)

  val state = RegInit(s_idle)

  val internal_counter = RegInit(0.U(log2Ceil(BPE_num+1).W))
  // To give the BPEs enough cycles to update
  val substate_cntr = RegInit(0.U(5.W))

  val last_pckt_was_keep_alive = RegInit(false.B)
  // Used to ensure the register isn't simply left on keep alive. Requires new keep alive packet to keep it alive
  last_pckt_was_keep_alive := (command === 16.U) // Keep alive command

  val stop_when_collision = RegInit(false.B)

  val currentIteration = RegInit(0.U(32.W))
  val max_iterations = RegInit(1000000.U) // Maximum number of iterations
  io.currentIteration := currentIteration


  // Only used when data is being outputted, not used when sending data in the BPEs
  val target = RegInit(0.U(log2Ceil(BPE_num).W)) // Target BPE to send data to

  // Increment last_valid_pckt_received_cnt, reset lock if it gets above a threshold
  when (io.locked === true.B) {
    last_valid_pckt_received_cnt := last_valid_pckt_received_cnt + 1.U
    when (last_valid_pckt_received_cnt === 1000000.U) {
      unlock() // The user process might have crashed, so we unlock
    }
  }

  //Init wire at false
  val request_valid = WireDefault(false.B)
  request_valid := (key === lock_key) 

  switch (state) {
    is (s_idle) {
      idle_state()
    }
    is (sRunning) {
      running_state()
    }
  }

  def running_state(): Unit = {
    // Iterate over each of the active BPEs, tell them to broadcast their data
    // Once it is done, update position and return to BPE nbr 0
    when (request_valid) {
      val reach_end = (internal_counter === numberActiveBPE)
      when (reach_end) {
        update_position()
      } .otherwise {
        update_velocity()
      }

      // Handle collision detection
      when (bp_switch.io.collided === true.B && stop_when_collision === true.B) {
        // Stop the simulation if a collision is detected
        state := s_idle
      }

      switch (command) {
        // Only need to handle stop simulation, keep alive, and unlock in the running state
        is (1.U) { // Unlock
          unlock()
        }
        is (13.U) { // Stop simulation
          state := s_idle
        }
        is (16.U) { // Keep alive
          handle_keep_alive()
        }
      }
    }
  }

  def update_position(): Unit = {
    // printf(p"Updating position\n")
    // Update the position of the BPEs
    bp_switch.io.m_slct := 1.U // 1 = update position
    substate_cntr := substate_cntr + 1.U
    when (substate_cntr === 3.U) { // 4 cycles of wait, as takes one cycle to update
      substate_cntr := 0.U
      internal_counter := 0.U
      
      // + 2 as it takes 1 cycle to update a register, and must finish at one below the max iteration number as it is non inclusive
      when (currentIteration + 1.U === max_iterations) {
        // Stop the simulation if the maximum number of iterations is reached
        state := s_idle
        currentIteration := 0.U
      }
      .otherwise {
        currentIteration := currentIteration + 1.U
      }
    }
  }

  // Update the velocity of the BPEs
  def update_velocity(): Unit = {
    bp_switch.io.m_slct := 0.U 
    bp_switch.io.target := internal_counter
    substate_cntr := substate_cntr + 1.U
    when (substate_cntr === 22.U) {
      substate_cntr := 0.U
      internal_counter := internal_counter + 1.U
    }
  }
  

  def idle_state(): Unit = {
    when (command === 1.U) {
      attemptLock()
    }

    // Only process the other type of commands if the request is valid
    when (request_valid) {
      switch (command) {
        is (2.U) { // Unlock
          unlock()
        }
        is (3.U) { // Set X
          X := data
        }
        is (4.U) { // Set Y
          Y := data
        }
        is (5.U) { // Set Z
          Z := data
        }
        is (6.U) { // Set mass
          m := data
        }
        is (7.U) { // Set size
          size := data
        }
        is (8.U) { // Set dt
          dt := data
        }
        is (9.U) { // Set target, forward data as position
          val truncated_data = data(log2Ceil(BPE_num), 0)
          bp_switch.io.target := truncated_data
          forwardData()
          bp_switch.io.m_slct := 2.U // 2 = set position
        }
        is (10.U) { // Set target, forward data as velocity
          val truncated_data = data(log2Ceil(BPE_num), 0)
          bp_switch.io.target := truncated_data
          forwardData()
          bp_switch.io.m_slct := 3.U // 3 = set velocity
        }
        is (11.U) { // Set tstop_when_collision
          stop_when_collision := data(0) // 1 = stop when collision
        }
        is (12.U) { // Start simulation
        // Start at the number of active BPEs, so that it starts with a position update instead of a velocity update
          internal_counter := numberActiveBPE
          currentIteration := 0.U
          state := sRunning
        }
        is (13.U) { // Stop simulation
          // No need to do anything, as must be handled from the running state, not here
        }
        is (14.U) { // Set max iteration number
          max_iterations := data        
        }
        is (15.U) { // Set number of active BPEs
          numberActiveBPE := data(log2Ceil(BPE_num), 0)
        }
        is (16.U) { // Keep alive
          handle_keep_alive()
        }
        is (17.U) { // Set target
          target := data(log2Ceil(BPE_num), 0)        
        }
        is (18.U) { // Output the target BPE's X position
          bp_switch.io.target := target
          bp_switch.io.m_slct := 6.U // 6 = output position
          val X_out = bp_switch.io.X_out
          val bit_flip_mask = data // Used to encrypt the data
          val X_out_encrypted = X_out ^ bit_flip_mask
          io.dOut := X_out_encrypted
        }
        is (19.U) { // Output the target BPE's Y position
          bp_switch.io.target := target
          bp_switch.io.m_slct := 6.U // 6 = output position
          val Y_out = bp_switch.io.Y_out
          val bit_flip_mask = data // Used to encrypt the data
          val Y_out_encrypted = Y_out ^ bit_flip_mask
          io.dOut := Y_out_encrypted
        }
        is (20.U) { // Output the target BPE's Z position
          bp_switch.io.target := target
          bp_switch.io.m_slct := 6.U // 6 = output velocity
          val Z_out = bp_switch.io.Z_out
          val bit_flip_mask = data // Used to encrypt the data
          val Z_out_encrypted = Z_out ^ bit_flip_mask
          io.dOut := Z_out_encrypted
        }
        is (21.U) { // Output the target BPE's dX
          bp_switch.io.target := target
          bp_switch.io.m_slct := 4.U // 4 = output velocity
          val X_out = bp_switch.io.X_out
          val bit_flip_mask = data // Used to encrypt the data
          val X_out_encrypted = X_out ^ bit_flip_mask
          io.dOut := X_out_encrypted
        }
        is (22.U) { // Output the target BPE's dY
          bp_switch.io.target := target
          bp_switch.io.m_slct := 4.U // 4 = output velocity
          val Y_out = bp_switch.io.Y_out
          val bit_flip_mask = data // Used to encrypt the data
          val Y_out_encrypted = Y_out ^ bit_flip_mask
          io.dOut := Y_out_encrypted
        }
        is (23.U) { // Output the target BPE's dZ
          bp_switch.io.target := target
          bp_switch.io.m_slct := 4.U // 4 = output velocity
          val Z_out = bp_switch.io.Z_out
          val bit_flip_mask = data // Used to encrypt the data
          val Z_out_encrypted = Z_out ^ bit_flip_mask
          io.dOut := Z_out_encrypted
        }
        is (24.U) { // Output the ID of the BPE that collided
          val collision_id = bp_switch.io.collision_id
          val bit_flip_mask = data // Used to encrypt the data
          val extended_collision_id = Cat(0.U((32-log2Ceil(BPE_num)).W), collision_id) // Extend to 64 bits
          io.dOut := extended_collision_id ^ bit_flip_mask
        }
        // No other commands are implemented
      }
    }
  }

  def handle_keep_alive(): Unit = {
    when (last_pckt_was_keep_alive === false.B) {
      last_valid_pckt_received_cnt := 0.U
      last_pckt_was_keep_alive := true.B
    } 
  }

  def attemptLock(): Unit = {
    // println("Attempting to lock")
    when (lock_key === 0.U) {
      // println("Locking")
      lock_key := key
    }
  }

  def unlock(): Unit = {
    lock_key := 0.U
    last_valid_pckt_received_cnt := 0.U
    emptyData() // To ensure the data won't leak
  }


  def defaultValues(): Unit = {
    bp_switch.io.X_in := 0.U
    bp_switch.io.Y_in := 0.U
    bp_switch.io.Z_in := 0.U
    bp_switch.io.size_in := 0.U
    bp_switch.io.m_in := 0.U
    bp_switch.io.dt := dt
    bp_switch.io.m_slct := 7.U // 7 = idle
    bp_switch.io.target := 0.U
    // Output NaN
    val NaN = 0x7FC00000.U
    io.dOut := NaN
  }

  def forwardData(): Unit = {
    bp_switch.io.X_in := X
    bp_switch.io.Y_in := Y
    bp_switch.io.Z_in := Z
    bp_switch.io.size_in := size
    bp_switch.io.m_in := m
    bp_switch.io.dt := dt
  }

  def emptyData(): Unit = {
    bp_switch.io.m_slct := 5.U // 5 = reset all BPUs
    // Also remove all data inside of this module
    dt := 0.U
    m := 0.U
    size := 0.U
    X := 0.U
    Y := 0.U
    Z := 0.U
    numberActiveBPE := 0.U
    bp_switch.io.X_in := 0.U
    bp_switch.io.Y_in := 0.U
    bp_switch.io.Z_in := 0.U
    bp_switch.io.size_in := 0.U
    bp_switch.io.m_in := 0.U
    bp_switch.io.dt := 0.U
    state := s_idle
    internal_counter := 0.U
    substate_cntr := 0.U
    last_valid_pckt_received_cnt := 0.U
  }

  // printf(p"----------------------\n")
  // printf(p"Command: ${binStr(command, 5)}\n")
  // printf(p"Key: ${binStr(key, 27)}\n")
  // printf(p"Data: ${binStr(data, 32)}\n")
  // printf(p"Lock key: ${binStr(lock_key, 27)}\n")
  // printf(p"Locked : ${io.locked}\n")
  // printf(p"Command: ${binStr(command, 5)}\n")
  // printf(p"Key: ${binStr(key, 27)}\n")
  // printf(p"Data: ${binStr(data, 32)}\n")
  // printf(p"X: ${binStr(X, 32)}\n")
  // printf(p"Y: ${binStr(Y, 32)}\n")
  // printf(p"Z: ${binStr(Z, 32)}\n")
  // printf(p"dOut: ${binStr(io.dOut, 32)}\n")
  // printf(p"----------------------\n")
  // printf(p"State: ${state}\n")
  // printf(p"Iteration: ${currentIteration}\n")
  // printf(p"internal_counter: ${internal_counter}\n")
  // printf(p"substate_cntr: ${substate_cntr}\n")


}