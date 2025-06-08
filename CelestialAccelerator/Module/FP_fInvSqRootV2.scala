package celestial

import chisel3._
import chisel3.util._

class F32InvSqrtInitial extends Module {
  val io = IO(new Bundle {
    val in  = Input(UInt(32.W))
    val out = Output(UInt(32.W))
  })

  // Constants
  val magicNumber = "h5F375A86".U(32.W)

  // Approximation
  val floatAsInt = io.in
  val firstApprox = magicNumber - (floatAsInt >> 1)
  io.out := firstApprox
}



class F32InvSqrtRefine(numberForDebug: Integer)  extends Module {
  val io = IO(new Bundle {
    val in     = Input(UInt(32.W)) // original input x
    val approx = Input(UInt(32.W)) // guess
    val out    = Output(UInt(32.W))
  })
  // Debug helper function
  def binStr(x: UInt, width: Int): Printable = {
    var result: Printable = p""
    for (i <- (width - 1) to 0 by -1) {
      result = result + p"${x(i).asUInt}"
    }
    result
  }

  val mul = Module(new F32Multiplier())
  val sub = Module(new F32Adder())
  
  sub.io.substracter := true.B
  val done = RegInit(false.B)

  // Default assignments to prevent uninitialized refs
  mul.io.a := 0.U
  mul.io.b := 0.U
  sub.io.a := 0.U
  sub.io.b := 0.U

  val onePointFive = "h3FC00000".U(32.W)
  val xHalf = Cat(0.U(1.W), io.in(30, 23) - 1.U, io.in(22, 0)) // x * 0.5


  val resultReg = RegInit(0.U(32.W))

  val tempYsq = Reg(UInt(32.W))
  val tempXHYsq = Reg(UInt(32.W))
  val temp3 = Reg(UInt(32.W))

  val s1 :: s2 :: s3 :: s4 :: nextIter :: Nil = Enum(5)
  val state = RegInit(s1)

  val y = RegInit(0.U(32.W))

  switch(state) {
    is(s1) {
      done := false.B
      mul.io.a := io.approx;
      mul.io.b := io.approx
      tempYsq := mul.io.out
      state := s2
    }
    is(s2) {
      mul.io.a := xHalf; mul.io.b := tempYsq
      tempXHYsq := mul.io.out
      state := s3
    }
    is(s3) {
      sub.io.a := onePointFive; sub.io.b := tempXHYsq
      temp3 := sub.io.sum
      state := s4
    }
    is(s4) {
      mul.io.a := io.approx; mul.io.b := temp3
      resultReg := Cat(0.U(1.W), mul.io.out(30, 0)) // force sign = 0
      state := nextIter
      done := true.B
    }
  }
  // Reset state when the input changes
  val prevInput = RegNext(io.approx) // Check when approx changes and not in as io.in is sent directly in the top module, and thus the
  // second refinement would not be triggered
  when(io.approx =/= prevInput) {
    state := s1
  }
  
  // printf("====================================================\n")
  // printf(p"Debug ${numberForDebug} Io input: ${binStr(io.in, 32)}\n")
  // printf(p"Debug ${numberForDebug} Initial Approx: ${binStr(io.approx, 32)}\n")
  // printf(p"Debug ${numberForDebug} tempYsq: ${binStr(tempYsq, 32)}\n")
  // printf(p"Debug ${numberForDebug} tempXHYsq: ${binStr(tempXHYsq, 32)}\n")
  // printf(p"Debug ${numberForDebug} temp3: ${binStr(temp3, 32)}\n")
  // printf(p"Debug ${numberForDebug} resultReg: ${binStr(resultReg, 32)}\n")
  // printf(p"Debug ${numberForDebug} done: ${done}\n")
  // printf(p"Debug ${numberForDebug} io.out: ${binStr(io.out, 32)}\n")
  // printf(p"Debug ${numberForDebug} State: ${state}\n")
  // printf("====================================================\n")
  // Output the result
  io.out := Mux(state === s4, Cat(0.U(1.W), mul.io.out(30, 0)), resultReg)
}

class F32FastInverseSqrtV2 extends Module {
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


  val initial = Module(new F32InvSqrtInitial())
  val refine1 = Module(new F32InvSqrtRefine(0))
  val refine2 = Module(new F32InvSqrtRefine(1))

  val sign = io.in(31)
  val exponent = io.in(30, 23)
  val fraction = io.in(22, 0)

  val isZero = (exponent === 0.U) && (fraction === 0.U)
  val isInf = (exponent === 255.U) && (fraction === 0.U)
  val isNaN = (exponent === 255.U) && (fraction =/= 0.U)
  val isNegative = sign === 1.U && !isZero


  // Default assignments to avoid inferred latches
  initial.io.in := 0.U
  refine1.io.in := 0.U
  refine1.io.approx := 0.U
  refine2.io.in := 0.U
  refine2.io.approx := 0.U

  when (isNegative || isNaN) {
    io.out  := "h7FC00000".U // NaN
  } .elsewhen (isZero) {
    io.out  := "h7F800000".U // +Inf
  } .elsewhen (isInf) {
    io.out  := 0.U // 1/sqrt(Inf) = 0
  } .otherwise {
    initial.io.in := io.in

    refine1.io.in := io.in
    refine1.io.approx := initial.io.out

    refine2.io.in := io.in
    refine2.io.approx := refine1.io.out

    io.out := refine2.io.out

    printf("====================================================\n")
    printf(p"Io input: ${binStr(io.in, 32)}\n")
    printf(p"Initial Approx: ${binStr(initial.io.out, 32)}\n")
    printf(p"Refine1 Approx: ${binStr(refine1.io.out, 32)}\n")
    printf(p"Refine2 Approx: ${binStr(refine2.io.out, 32)}\n")
    // printf(p"result: ${binStr(result, 32)}\n")
    // Print the current clock cycle
    printf("====================================================\n")
  }

  // Wire io.out to the result of refine2 to get the final output faster
  // io.out := refine2.io.out
}

