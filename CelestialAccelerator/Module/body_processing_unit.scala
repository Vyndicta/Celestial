package celestial

import chisel3._
import chisel3.util._

class BPU extends Module {
  val io = IO(new Bundle {
    val X_in  = Input(UInt(32.W))
    val Y_in  = Input(UInt(32.W))
    val Z_in  = Input(UInt(32.W))

    val m_in  = Input(UInt(32.W))
    val dt    = Input(UInt(32.W))
    val m_slct = Input(UInt(3.W))
    
    val size_in = Input(UInt(32.W))
    
    val X_out = Output(UInt(32.W))
    val Y_out = Output(UInt(32.W))
    val Z_out = Output(UInt(32.W))
    val m_out = Output(UInt(32.W))
  
    val size_out = Output(UInt(32.W))
    val collided = Output(Bool())
  })
  def binStr(x: UInt, width: Int): Printable = {
    var result: Printable = p""
    for (i <- (width - 1) to 0 by -1) {
      result = result + p"${x(i).asUInt}"
    }
    result
  }

  // Used for the collision detection
  def compareFloats(a: UInt, b: UInt): Bool = { //a -> dist, b -> size
    val signA = a(31)
    val signB = b(31)

    val expA = a(30, 23)
    val expB = b(30, 23)

    val mantA = a(22, 0)
    val mantB = b(22, 0)

    val absALess = (expA < expB) || (expA === expB && mantA <= mantB)
    val absAEqual = (expA === expB && mantA === mantB)

    // Handle sign comparison
    val aLessThanB = Wire(Bool())

    when(signA === 0.U && signB === 0.U) {
      // Both positive
      aLessThanB := absALess
    }.elsewhen(signA === 1.U && signB === 1.U) {
      // Both negative (larger exponent = smaller value)
      aLessThanB := !absALess && !absAEqual
    }.elsewhen(signA === 1.U && signB === 0.U) {
      // a is negative, b is positive => a < b
      aLessThanB := true.B
    }.otherwise {
      // a is positive, b is negative => a > b
      aLessThanB := false.B
    }

    aLessThanB
  }

  // Internal states
  val pos_X = RegInit(0.U(32.W))
  val pos_Y = RegInit(0.U(32.W))
  val pos_Z = RegInit(0.U(32.W))

  val mass = RegInit(0.U(32.W))

  val size = RegInit(0.U(32.W))

  io.size_out := size

  val velocity_X = RegInit(0.U(32.W))
  val velocity_Y = RegInit(0.U(32.W))
  val velocity_Z = RegInit(0.U(32.W))


  val fastNegThreeHalfExp = Module(new NegThreeHalfExp())
  val mult = Module(new F32Multiplier())
  val add = Module(new F32Adder())

  val connectFastExpToMultiplier = RegInit(false.B)
  val connectFastExpToSubtractor = RegInit(false.B)

  val collidedReg = RegInit(false.B)
  io.collided := collidedReg
  val counter_wire = WireDefault(0.U(5.W))
  val counter_reg = RegNext(counter_wire) // 0 to 31, used to track the sub state of the BPU
  val reset_counter = RegNext(io.m_slct) =/= io.m_slct // Reset the counter when m_slct changes
  
  // Two variables for the counter to have one that updates instantly; the other is needed to keep track of the state
  counter_wire := Mux(reset_counter || counter_reg >= 23.U, 0.U, counter_reg + 1.U) // Increment the counter when m_slct is not reset
  
  // To avoid losing the cycle that it takes the counter to change, use a wire counter, which is either equal to the counter_int or 0

  // if m_slct == 0, then update velocity, according to the dt and inputs
  // if m_slct == 1, then update position, according to the dt and velocity
  // if m_slct == 2, then set position to X_in, Y_in, Z_in,  mass to m_in, size to size_in
  // if m_slct == 3, then set velocity to X_in, Y_in, Z_in
  // if m_slct == 4, then output velocity in X_out, Y_out, Z_out instead of position
  // if m_slct = 5, reset all the registers, including the collision register 
  // if m_slct == 6, then stand by, do nothing

  // Used to store miscellaneous values
  val temp1 = RegInit(0.U(32.W)) 
  val temp2 = RegInit(0.U(32.W)) 
  val temp3 = RegInit(0.U(32.W)) 

  // Used to store the \vec d
  val tempX = RegInit(2.U(32.W))
  val tempY = RegInit(0.U(32.W))
  val tempZ = RegInit(0.U(32.W))
  
  add.io.substracter := true.B // Work as a add by default

  // Handling the mult & add modules sharing
  // counter_wire =/= 0.U because it takes one cycle to update connectFastExpToMultiplier, but we never want to share the mult with the fastExp module at the first cycle
  when (connectFastExpToMultiplier && counter_wire =/= 0.U) {
    mult.io.a := fastNegThreeHalfExp.io.mulA
    mult.io.b := fastNegThreeHalfExp.io.mulB
  }. otherwise {
    mult.io.a := 0.U // Default value to avoid uninitialized ref
    mult.io.b := 0.U
  }
  
  fastNegThreeHalfExp.io.mulOut := mult.io.out

  fastNegThreeHalfExp.io.rst := false.B // Default value to avoid uninitialized ref
  fastNegThreeHalfExp.io.in := 0.U // Default value to avoid uninitialized ref

  when (connectFastExpToSubtractor && counter_wire =/= 0.U) {
    add.io.a := fastNegThreeHalfExp.io.subA
    add.io.b := fastNegThreeHalfExp.io.subB
  }. otherwise {
    add.io.a := 0.U // Default value to avoid uninitialized ref
    add.io.b := 0.U
  }
  fastNegThreeHalfExp.io.subOut := add.io.sum

  def updateVelocity(): Unit = {

    // printf("====================================================\n")
    // printf(p"Update velocity\n")
    // printf(p"counter_wire: ${binStr(counter_wire, 5)}\n")
    // printf(p"Temp1: ${binStr(temp1, 32)}\n")
    // printf(p"Temp2: ${binStr(temp2, 32)}\n")  
    // printf(p"Temp3: ${binStr(temp3, 32)}\n")
    // printf(p"TempX: ${binStr(tempX, 32)}\n")
    // printf(p"TempY: ${binStr(tempY, 32)}\n")
    // printf(p"TempZ: ${binStr(tempZ, 32)}\n")
    // printf(p"Velocity X: ${binStr(velocity_X, 32)}\n")
    // printf(p"Velocity Y: ${binStr(velocity_Y, 32)}\n")
    // printf(p"Velocity Z: ${binStr(velocity_Z, 32)}\n")
    // printf("====================================================\n")
    // printf(p"Update velocity\n")

    switch(counter_wire) {

      is(0.U) {
    // printf("====================================================\n")
    //     printf(p"Current positionX: ${binStr(pos_X, 32)}\n")
    //     printf(p"Current positionY: ${binStr(pos_Y, 32)}\n")
    //     printf(p"Current positionZ: ${binStr(pos_Z, 32)}\n")
    //     // printf("Velocity update starting up\n")
    //     printf(p"Current velocityX: ${binStr(velocity_X, 32)}\n")
    //     printf(p"Current velocityY: ${binStr(velocity_Y, 32)}\n")
    //     printf(p"Current velocityZ: ${binStr(velocity_Z, 32)}\n")
    // printf("====================================================\n")

        // printf(p"Collision register state: $collidedReg")
        connectFastExpToMultiplier := false.B
        connectFastExpToSubtractor := false.B

        // Start computing m1 * m2, and \vec d
        // mult.io.a := mass
        // mult.io.b := io.m_in
        // temp1 := mult.io.out // Store m1 * m2 in temp1
        temp1 := io.m_in // Because we want the acceleration, not the force, will have to clean up later, I can probably remove one of the temps

        add.io.a := io.X_in
        add.io.b := pos_X
        tempX := add.io.sum
      }

      is(1.U) {
        // Compute \vec d
        add.io.a := io.Y_in
        add.io.b := pos_Y
        tempY := add.io.sum
        // printf(p"dy: ${binStr(add.io.sum, 32)}\n")

        // Start computing ||d||^2
        mult.io.a := tempX
        mult.io.b := tempX
        // printf(p"dX^2: ${binStr(mult.io.out, 32)}\n")
        temp2 := mult.io.out // Store d_x^2 in temp2
      }

      is(2.U) {
        // Compute \vec d
        add.io.a := io.Z_in
        add.io.b := pos_Z
        tempZ := add.io.sum
        // Start computing ||d||^2
        mult.io.a := tempY
        mult.io.b := tempY
        // printf(p"dY^2: ${binStr(mult.io.out, 32)}\n")
        temp3 := mult.io.out // Store d_y^2 in temp3
      }

      is(3.U) {
        // Compute ||d||^2
        mult.io.a := tempZ
        mult.io.b := tempZ
        temp2 := mult.io.out // Store d_z^2 in temp2

        // Compute d_x^2 + d_y^2 
        add.io.substracter := false.B
        add.io.a := temp2
        add.io.b := temp3
        temp3 := add.io.sum // Store d_x^2 + d_y^2 in temp3
        // printf(p"dZ^2: ${binStr(mult.io.out, 32)}\n")	
        // printf(p"dx^2 + dy^2: ${binStr(add.io.sum, 32)}\n")
        connectFastExpToMultiplier := true.B // The exp will start at the next cycle, at it needs the multiplier from the start
      }

      is (4.U) {
        // Compute ||d||^2
        add.io.substracter := false.B
        add.io.a := temp2
        add.io.b := temp3
        temp2 := add.io.sum // Store ||d||^2 in temp2
        // printf(p"dx^2 + dy^2 + dz^2: ${binStr(add.io.sum, 32)}\n")
        fastNegThreeHalfExp.io.in := add.io.sum
        fastNegThreeHalfExp.io.rst := true.B // Reset the fastExp module
      }

      is (5.U) {
        add.io.substracter := false.B 
        add.io.a := size
        add.io.b := io.size_in
        temp3 := add.io.sum // Store size + size_in in temp3
        fastNegThreeHalfExp.io.in := temp2
        // printf(p"Input of fastExp: ${binStr(fastNegThreeHalfExp.io.in, 32)}\n")
        fastNegThreeHalfExp.io.rst := false.B 
        connectFastExpToSubtractor := true.B
      }
      is (6.U) {
        fastNegThreeHalfExp.io.in := temp2
      }
      is (7.U) {
        fastNegThreeHalfExp.io.in := temp2
        connectFastExpToMultiplier := false.B // Shouldn't be needed at the next cycle
      }
      is (8.U) {
        fastNegThreeHalfExp.io.in := temp2
        // Multiply m2 by dt
        mult.io.a := io.dt // dt
        mult.io.b := temp1 // m2 * dt
        temp1 := mult.io.out // Storem2 * dt in temp1
        connectFastExpToMultiplier := true.B
      }
      is (9.U) {
        fastNegThreeHalfExp.io.in := temp2
      }
      is (10.U) {
        fastNegThreeHalfExp.io.in := temp2
      }
      is (11.U) {
        fastNegThreeHalfExp.io.in := temp2
        connectFastExpToMultiplier := false.B
      }
      is (12.U) {
        fastNegThreeHalfExp.io.in := temp2
        mult.io.a := temp3 // size1 + size2
        mult.io.b := temp3
        temp3 := mult.io.out // Store (size1 + size2)^2 in temp3, to compare with the distance^2
        connectFastExpToMultiplier := true.B
      }
      is (13.U) {
        fastNegThreeHalfExp.io.in := temp2
        // Compare temp3, which is (size1 + size2)^2, with temp2 (||d||^2 )
        collidedReg := Mux(compareFloats(temp2, temp3), true.B, collidedReg) 
      }
      is (14.U) {
        fastNegThreeHalfExp.io.in := temp2
      }
      is (15.U) {
        fastNegThreeHalfExp.io.in := temp2
      }
      is (16.U) {
        fastNegThreeHalfExp.io.in := temp2
      }
      is (17.U) {
        fastNegThreeHalfExp.io.in := temp2
        temp3 := fastNegThreeHalfExp.io.out
        connectFastExpToSubtractor := false.B
        connectFastExpToMultiplier := false.B
      }
      is (18.U) {
        mult.io.a := temp1 // m2 * dt
        mult.io.b := temp3 // 1 / (d^3)
        // printf(p" m1*m2 : ${binStr(temp1, 32)}\n")
        // printf(p" 1/(d^3): ${binStr(temp3, 32)}\n")
        temp2 := mult.io.out
      }
      is (19.U) {
        // It is possible to connect the output of the multiplier to the input of the adder, 
        // but 
        // 1 : It would likely force to slow down a clk cycle
        // 2 : it creates a combinartorial loop inside the three half exp module, which would force some corner cases to be handled
        mult.io.a := tempX // d_x
        mult.io.b := temp2 // dt*m2/(d^3)
        // printf(p" m1*m2/(d^3): ${binStr(temp2, 32)}\n")
        // printf(p" dx: ${binStr(tempX, 32)}\n")
        temp3 := mult.io.out
      }
      is (20.U) {
        add.io.substracter := false.B
        add.io.a := temp3
        add.io.b := velocity_X
        velocity_X := add.io.sum // Update the x velocity   
        // printf(p"Delta velocityX: ${binStr(temp3, 32)}\n")

        mult.io.a := tempY // d_y
        mult.io.b := temp2 // dt*m2/(d^3)
        temp3 := mult.io.out
      }
      is (21.U) {     
        add.io.substracter := false.B
        add.io.a := temp3
        add.io.b := velocity_Y
        velocity_Y := add.io.sum 

        
        mult.io.a := tempZ // d_z
        mult.io.b := temp2 // m1*m2/(d^3)
        temp3 := mult.io.out
      }
      is (22.U) {        
        add.io.substracter := false.B
        add.io.a := temp3
        add.io.b := velocity_Z
        velocity_Z := add.io.sum // Update the z velocity  
        // printf("Velocity update finished\n")
        // printf(p"New velocityX: ${binStr(velocity_X, 32)}\n")
        // printf(p"New velocityY: ${binStr(velocity_Y, 32)}\n")
        // printf(p"New velocityZ: ${binStr(velocity_Z, 32)}\n")
      }
      
      // is (23.U) { }       

    }
  }

  def updatePosition(): Unit = {
    switch (counter_wire) {
      is(0.U) {
        // In case it was interrupted during another operation
        connectFastExpToMultiplier := false.B
        connectFastExpToSubtractor := false.B 
        
        mult.io.a := io.dt
        mult.io.b := velocity_X
        temp1 := mult.io.out
      }
      is(1.U) {
        mult.io.a := io.dt
        mult.io.b := velocity_Y
        temp1 := mult.io.out

        add.io.substracter := false.B
        add.io.a := temp1 // dt * velocityX
        add.io.b := pos_X
        pos_X := add.io.sum
      }
      is(2.U) {
        mult.io.a := io.dt
        mult.io.b := velocity_Z
        temp1 := mult.io.out

        add.io.substracter := false.B
        add.io.a := temp1 // dt * velocityX
        add.io.b := pos_Y
        pos_Y := add.io.sum
      }
      is(3.U) {
        add.io.substracter := false.B
        add.io.a := temp1 // dt * velocityX
        add.io.b := pos_Z
        pos_Z := add.io.sum
      }
    }
  }

  switch(io.m_slct) {
    is(0.U) { 
      updateVelocity()
    }
    is(1.U) { // Update position
      updatePosition() 
      
    // printf("====================================================\n")
    // printf(p"Update velocity\n")
    // printf(p"counter_wire: ${binStr(counter_wire, 5)}\n")
    // printf(p"Temp1: ${binStr(temp1, 32)}\n")
    // printf(p"Temp2: ${binStr(temp2, 32)}\n")  
    // printf(p"Temp3: ${binStr(temp3, 32)}\n")
    // printf(p"TempX: ${binStr(tempX, 32)}\n")
    // printf(p"TempY: ${binStr(tempY, 32)}\n")
    // printf(p"TempZ: ${binStr(tempZ, 32)}\n")
    // printf(p"Velocity X: ${binStr(velocity_X, 32)}\n")
    // printf(p"Velocity Y: ${binStr(velocity_Y, 32)}\n")
    // printf(p"Velocity Z: ${binStr(velocity_Z, 32)}\n")
    // printf(p"Position X: ${binStr(pos_X, 32)}\n")
    // printf(p"Position Y: ${binStr(pos_Y, 32)}\n")
    // printf(p"Position Z: ${binStr(pos_Z, 32)}\n")

    // printf("====================================================\n")
    }
    is(2.U) { // Set position, mass and size
      pos_X := io.X_in
      pos_Y := io.Y_in
      pos_Z := io.Z_in
      mass  := io.m_in
      size  := io.size_in
      // Reset collision register
      collidedReg := false.B
    }
    is(3.U) { // Set velocity
      velocity_X := io.X_in
      velocity_Y := io.Y_in
      velocity_Z := io.Z_in
    }
    is(4.U) { // Output velocity instead of position
      // Already handled in the output section
    }
    is(5.U) { 
      // Reset all registers
      collidedReg := false.B
      pos_X := 0.U
      pos_Y := 0.U 
      pos_Z := 0.U
      mass := 0.U
      size := 0.U
      velocity_X := 0.U
      velocity_Y := 0.U
      velocity_Z := 0.U
    }
    is (6.U) {
      // Do nothing
    }
  }

  when(io.m_slct === 4.U) {
    // Output velocity instead of position
    io.X_out := velocity_X
    io.Y_out := velocity_Y
    io.Z_out := velocity_Z
  }.otherwise {
    // Output position and mass
    io.X_out := pos_X
    io.Y_out := pos_Y
    io.Z_out := pos_Z
  }

  // Output mass
  io.m_out := mass



}
