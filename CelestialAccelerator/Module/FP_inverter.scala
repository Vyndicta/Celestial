package celestial
// This module was not used in the end, though it works
import chisel3._
import chisel3.util._

class FP32Inverter extends Module {
  val io = IO(new Bundle {
    val in  = Input(UInt(32.W))
    val out = Output(UInt(32.W))
  })

  // Constants
  val bias = 127.U // As per IEEE 754 standard, the bias for single-precision floats is 127

  // Unpack input
  def unpackFloat(in: UInt): (UInt, UInt, UInt) = {
    val sign     = in(31)
    val exponent_with_bias = in(30, 23)
    val fraction = in(22, 0)
    (sign, exponent_with_bias, fraction)
  }
      def binStr(x: UInt, width: Int): Printable = {
    var result: Printable = p""
    for (i <- (width - 1) to 0 by -1) {
      result = result + p"${x(i).asUInt}"
    }
    result
  }


  val (sign, exponent_with_bias, fraction) = unpackFloat(io.in)

  // Special cases
  val isZero = (exponent_with_bias === 0.U) && (fraction === 0.U)
  val isInf  = (exponent_with_bias === 255.U) && (fraction === 0.U)
  val isNaN  = (exponent_with_bias === 255.U) && (fraction =/= 0.U)

  // Add implicit leading 1 for normalized numbers, unless input is zero
  val mantissa = Mux(exponent_with_bias === 0.U, Cat(0.U(1.W), fraction), Cat(1.U(1.W), fraction)) // 24 bits

  // Placeholder for output
  val result = Wire(UInt(32.W))
  result := 0.U

  when (isZero) {
    result := Cat(sign, 255.U(8.W), 0.U(23.W)) // Set exponent to 255 (inf) and fraction to 0
  }.elsewhen (isNaN) {
    result := "h7FC00000".U // Return NaN
  }.elsewhen (isInf) {
    result := 0.U // 1/inf = 0
  } .otherwise {
    // If we actually computed 1 / x, it would have to be in floating points.
    // To avoid such an expensive operation, we can use the following:
    // 1/m = 2^N / m * 2^(-N) 
    // This way, we can do the division in fixed point
    // 1L << 23 makes it 24 bit long, thus matching the implicit leading 1 of the mantissa,
    // thus seen as a one.
    // Then, N = 24, to maintain the 24 bits of precision.
    // Which leads to (1L << 47)
    val N = 24

    val reciprocal = (1L << (23 + N)).U / mantissa // 1 / m, with 47 bits
    // Extract upper 24 bits as the new mantissa (simple truncation)
    // Equivalent to shifting right by 23 bits
    // x = m×2^e -> 1/ (m×2^(e)) =  1/m * 2^(-e) = 1/m * 2^(bias-e-bias) 
    // -> new_e = bias - e_with_bias
    // new_e_with_bias = new_e + bias = bias - e_with_bias + bias = 2*bias - e_with_bias = bias << 1 - e_with_bias    
    val invExponent_with_bias = (bias << 1).asUInt - exponent_with_bias 

    // Normalize if needed
    // Count leading zeros from MSB
    val leadingZeros = PriorityEncoder(reciprocal.asBools.reverse) - 1.U
    val shiftedReciprocal = reciprocal << leadingZeros

    val normMantissa = shiftedReciprocal(45, 23) // Upper 24 bits (1 implicit + 23)
    val shiftedExp = (invExponent_with_bias - (leadingZeros - N.U + 2.U))(7, 0) // Adjust exponent by leading zeros and N
    val shiftValue = leadingZeros - N.U + 2.U
    val normExponent = Mux(invExponent_with_bias < shiftValue, invExponent_with_bias, shiftedExp) // If already zero, keep it zero

    val finalSign = sign // Inversion doesn't change the sign

    result := Cat(finalSign, normExponent, normMantissa)

    // Debug prints
    printf("========Debug Info:=============\n")
    printf(p"Input binary: ${binStr(io.in, 32)}\n")
    printf(p"sign : 0b${binStr(sign, 1)}, exponent: 0b${binStr(exponent_with_bias, 8)}, fraction: 0b${binStr(fraction, 23)}\n")
    printf("isZero: %d, isInf: %d, isNaN: %d\n", isZero, isInf, isNaN)
    printf(p"Reciprocal: ${binStr(reciprocal, 47)}\n")
    printf("Leading zeros: %d\n", leadingZeros)
    
    printf(p"normMantissa: ${binStr(normMantissa, 23)}\n")
    printf(p"invExponent_with_bias: ${binStr(invExponent_with_bias, 8)}\n")
    printf(p"normExponent: ${binStr(normExponent, 8)}\n")
    printf("Result: %d\n", result)
    printf("Result (hex): %x\n", result)    
    printf(p"Result (binary): ${binStr(result, 32)}\n")
  }

  io.out := result
}
