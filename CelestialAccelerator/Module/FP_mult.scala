package celestial

import chisel3._
import chisel3.util._

class F32Multiplier extends Module {
  val io = IO(new Bundle {
    val a   = Input(UInt(32.W))
    val b   = Input(UInt(32.W))
    val out = Output(UInt(32.W))
  })

  val bias = 127.U(8.W)

  def unpackFloat(in: UInt): (Bool, UInt, UInt) = {
    val sign     = in(31)
    val exponent = in(30, 23)
    val fraction = in(22, 0)
    val isZero   = (exponent === 0.U) && (fraction === 0.U)
    val mantissa = Mux(exponent === 0.U, Cat(0.U(1.W), fraction), Cat(1.U(1.W), fraction))
    (sign, exponent, Mux(isZero, 0.U(24.W), mantissa))
  }
  
  def binStr(x: UInt, width: Int): Printable = {
    var result: Printable = p""
    for (i <- (width - 1) to 0 by -1) {
      result = result + p"${x(i).asUInt}"
    }
    result
  }

  val (signA, expA, mantA) = unpackFloat(io.a)
  val (signB, expB, mantB) = unpackFloat(io.b)

  val signRes = signA ^ signB

  //val expSum = expA +& expB - bias
  val expSum = Mux(expA +& expB > bias, expA +& expB - bias, 0.U(8.W))
  val mantProduct = (mantA * mantB)(47, 0)

  val leadingOne = PriorityEncoder(mantProduct.asBools.reverse)
  val normalizedMantissa = (mantProduct << (leadingOne))(47, 0)
  val finalMantissa = normalizedMantissa(46, 24)
  val adjustedExp = (expSum - leadingOne + 1.U)
  val truncated_adjustedExp = adjustedExp(7, 0)
  val isNaNA = (expA === 255.U) && (mantA(22, 0) =/= 0.U)
  val isNaNB = (expB === 255.U) && (mantB(22, 0) =/= 0.U)
  val isInfA = (expA === 255.U) && (mantA(22, 0) === 0.U)
  val isInfB = (expB === 255.U) && (mantB(22, 0) === 0.U)
  val isZeroA = (expA === 0.U) && (mantA(22, 0) === 0.U)
  val isZeroB = (expB === 0.U) && (mantB(22, 0) === 0.U)

  val result = WireDefault(0.U(32.W))

  when (isNaNA || isNaNB || (isInfA && isZeroB) || (isInfB && isZeroA)) {
    result := "h7FC00000".U // NaN
  } .elsewhen (isInfA || isInfB) {
    result := Cat(signRes, 255.U(8.W), 0.U(23.W)) // Infinity
  } .elsewhen (isZeroA || isZeroB) {
    result := 0.U // Zero
  } .otherwise {
    when (adjustedExp >= 255.U) {
      result := Cat(signRes, 255.U(8.W), 0.U(23.W)) // Overflow -> Infinity
    } .elsewhen (adjustedExp < 1.U) {
      // Underflow
      result := 0.U
    } .otherwise {
      result := Cat(signRes, truncated_adjustedExp, finalMantissa(22, 0))
    }
  }

  io.out := result

  // Debugging information
  // Print the two inputs in binary
  // printf("=====================Debug info=====================\n")
  // printf(p"Input A: ${binStr(io.a, 32)}\n")
  // printf(p"Input B: ${binStr(io.b, 32)}\n")
  // printf(p"Sign A: ${signA}, Exponent A: ${expA}, Mantissa A: ${binStr(mantA, 24)}\n")
  // printf(p"Sign B: ${signB}, Exponent B: ${expB}, Mantissa B: ${binStr(mantB, 24)}\n")
  // printf(p"mantProduct : ${binStr(mantProduct, 48)}\n")
  // printf(p"Sign Result: ${signRes}, expSum : ${expSum}, Exponent Result: ${adjustedExp}, Mantissa Result: ${binStr(finalMantissa, 23)}\n")
  // printf(p"Leading one : ${leadingOne}\n")
  // printf(p"Normalized Mantissa: ${binStr(normalizedMantissa, 48)}\n")
  // printf(p"Adjusted Exponent: ${adjustedExp}\n")
  // printf(p"Final Result: ${binStr(result, 32)}\n")

}
