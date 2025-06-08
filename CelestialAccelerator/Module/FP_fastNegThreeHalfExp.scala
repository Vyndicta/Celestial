package celestial

import chisel3._
import chisel3.util._

class NegThreeHalfExpInitial extends Module {
  val io = IO(new Bundle {
    val in  = Input(UInt(32.W))
    val out = Output(UInt(32.W))
  })
  def binStr(x: UInt, width: Int): Printable = {
    var result: Printable = p""
    for (i <- (width - 1) to 0 by -1) {
      result = result + p"${x(i).asUInt}"
    }
    result
  }

  // Constants
  val magicNumber = "h9EADA9A8".U(32.W)

  // Approximation
  val floatAsInt = io.in
  val firstApprox = magicNumber - ((3.U * floatAsInt) >> 1.U) // 3/2 * x
  io.out := firstApprox
}



class NegThreeHalfExpRefine extends Module {
  val io = IO(new Bundle {
    val inExpThree = Input(UInt(32.W)) // original input x
    val approx = Input(UInt(32.W)) // guess
    val out = Output(UInt(32.W))
    
    val mulA = Output(UInt(32.W)) 
    val mulB = Output(UInt(32.W)) 
    val mulOut = Input(UInt(32.W))

    val subA = Output(UInt(32.W))
    val subB = Output(UInt(32.W))
    val subOut = Input(UInt(32.W))

    val rst = Input(Bool())
  })

  // Debug helper function
  def binStr(x: UInt, width: Int): Printable = {
    var result: Printable = p""
    for (i <- (width - 1) to 0 by -1) {
      result = result + p"${x(i).asUInt}"
    }
    result
  }

  // Default assignments to prevent uninitialized refs
  io.mulA := 0.U
  io.mulB := 0.U
  io.subA := 0.U
  io.subB := 0.U

  val onePointFive = "h3FC00000".U(32.W)

  val temp = Reg(UInt(32.W))

  val s1 :: s2 :: s3 :: s4 :: s5 :: Nil = Enum(5)
  val state = RegInit(s1)

  switch(state) {
    is(s1) {
      io.mulA := io.approx
      io.mulB := io.inExpThree
      temp := io.mulOut
      state := s2
    }
    is(s2) {
      io.mulA := temp
      io.mulB := io.approx
      temp := io.mulOut
      state := s3
    }
    is(s3) {
      io.subA := onePointFive
      io.subB := Cat(0.U(1.W), temp(30, 23) - 1.U, temp(22, 0)) // divide by 2
      temp := io.subOut
      state := s4
    }
    is(s4) {
      io.mulA := io.approx
      io.mulB := temp
      state := s5
      temp := io.mulOut
    }
    is(s5) {
      // Hold until new approx comes
    }
  }

  when(io.rst) {
    state := s1
  }

  // Output logic
  //io.out := Cat(0.U(1.W), io.mulOut(30, 0))
  // Add a mux to output the result only when the state is s5
  when (state === s4) {
    io.out := Cat(0.U(1.W), io.mulOut(30, 0))
  }. elsewhen (state === s5) {
    io.out := Cat(0.U(1.W), temp(30, 0))
  }. otherwise {
   io.out := 0.U(32.W)
  }

  // print("========================NegThreeHalfExpRefine============================\n")
  // printf(p"inExpThree: ${binStr(io.inExpThree, 32)}\n")
  // printf(p"Approx: ${binStr(io.approx, 32)}\n")
  // printf(p"State: ${state}\n")
  // print("====================================================\n")
}


class NegThreeHalfExp extends Module {
    val io = IO(new Bundle {
    val in  = Input(UInt(32.W))
    val rst = Input(Bool())
    val out = Output(UInt(32.W))
    
    
    val mulA = Output(UInt(32.W)) 
    val mulB = Output(UInt(32.W)) 
    val mulOut = Input(UInt(32.W))

    val subA = Output(UInt(32.W))
    val subB = Output(UInt(32.W))
    val subOut = Input(UInt(32.W))
  })
  def binStr(x: UInt, width: Int): Printable = {
    var result: Printable = p""
    for (i <- (width - 1) to 0 by -1) {
      result = result + p"${x(i).asUInt}"
    }
    result
  }
  

  val initial = Module(new NegThreeHalfExpInitial())
  val refine = Module(new NegThreeHalfExpRefine())

  val sign = io.in(31)
  val exponent = io.in(30, 23)
  val fraction = io.in(22, 0)
  
  val connectRefinerToMulAndSub = RegInit(true.B)
  val isZero = (exponent === 0.U) && (fraction === 0.U)
  val isInf = (exponent === 255.U) && (fraction === 0.U)
  val isNaN = (exponent === 255.U) && (fraction =/= 0.U)
  val isNegative = sign === 1.U && !isZero

  // Default assignments to avoid inferred latches
  
  initial.io.in := io.in
  refine.io.inExpThree := 0.U
  refine.io.approx := 0.U
  refine.io.rst := false.B
  
  val count = RegInit(0.U(4.W)) // 4 bits to count up to 16
  val xEThree = RegInit(0.U(32.W))
  val temp = RegInit(0.U(32.W))

  // Set the default value of the outputs
  io.mulA := 0.U
  io.mulB := 0.U
  io.subA := 0.U
  io.subB := 0.U

  io.out := 0.U(32.W)


  when (connectRefinerToMulAndSub) {
    io.mulA := refine.io.mulA
    io.mulB := refine.io.mulB
    io.subA := refine.io.subA
    io.subB := refine.io.subB
  } 
  // Connect the output of the multiplier and subtracter to the refiner
  refine.io.mulOut := io.mulOut
  refine.io.subOut := io.subOut

  when ((isNegative || isNaN) && io.rst === false.B) {
    io.out  := "h7FC00000".U // NaN
  } .elsewhen (isZero && io.rst === false.B) {
    io.out  := "h7F800000".U // +Inf
  } .elsewhen (isInf && io.rst === false.B) {
    io.out  := 0.U // 1/sqrt(Inf) = 0
  } .otherwise {
    refine.io.inExpThree := xEThree

    when (count === 12.U)
    {
      io.out := refine.io.out
    }.elsewhen (count >= 13.U) {
      io.out := temp
    }.otherwise {
      io.out := 0.U(32.W)
    }

    when (io.rst)
    {
      // Already start the computation when reseting. 
      // The refine step uses x^3, so we need to compute x^2 first, which is then multiplied by x to get x^3 at count 0
        io.mulA := io.in
        io.mulB := io.in
        temp := io.mulOut
        connectRefinerToMulAndSub := false.B
    }

    // Switch over the counter to determine which refinement to use
    switch (count) {
      is(0.U) {
        // Finish the computation of x^3 before the first refinement
        io.mulA := temp
        io.mulB := io.in
        xEThree := io.mulOut
        refine.io.rst := true.B
        connectRefinerToMulAndSub := true.B
      }
      is(1.U) {
        // First refinement
        refine.io.approx := initial.io.out
        // Wire the inputs to the multiplier and subtracter
        refine.io.rst := false.B
      }
      is(2.U) {
        // Wait for the first refinement to finish (4 cycles)
        refine.io.approx := initial.io.out
      }
      is(3.U) {
        refine.io.approx := initial.io.out
      }
      is(4.U) {
        refine.io.approx := initial.io.out
        temp := refine.io.out
        refine.io.rst := true.B
      }
      is(5.U) {
        // Second refinement
        refine.io.approx := temp  
        refine.io.rst := false.B
      }
      is(6.U) {
        refine.io.approx := temp
      }
      is(7.U) {
        // Wait for the second refinement to finish (4 cycles)
        refine.io.approx := temp
      }  
      is(8.U) {
        refine.io.rst := true.B
        temp := refine.io.out
        refine.io.approx := temp
      }
      is(9.U) {
        refine.io.approx := temp
        refine.io.rst := false.B
      }
      is(10.U) {
        refine.io.approx := temp
      }
      is(11.U) {
        // Third refinement
        refine.io.approx := temp
      }
      is(12.U) {
        temp := refine.io.out
        refine.io.approx := temp
      }
      

    }

    // printf("======NetThreeExp==============================================\n")
    // printf(p"Counter :  ${count}\n")
    // printf(p"Temp: ${binStr(temp, 32)} \n")
    // printf(p"Io input: ${binStr(io.in, 32)}\n")
    // printf(p"Initial Approx: ${binStr(initial.io.out, 32)}\n")
    // printf(p"input^3: ${binStr(xEThree, 32)}\n")
    // printf(p"Refine Approx: ${binStr(refine.io.out, 32)}\n")
    // printf(p"Output: ${binStr(io.out, 32)}\n")
    // printf("====================================================\n")
  }

  // Increase counter on each clock cycle, reset to 0 when input changes
  when (io.rst) {
    count := 0.U
  } .otherwise {
    // Increase unless already at max value
    when (count < 31.U) {
      count := count + 1.U
    }
  }

  // Wire io.out to the result of refine2 to get the final output faster
  // io.out := refine2.io.out
}