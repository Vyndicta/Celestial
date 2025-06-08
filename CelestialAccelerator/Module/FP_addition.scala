package celestial

import chisel3._
import chisel3.util._
import chisel3.experimental._

class F32Adder extends Module {
  val io = IO(new Bundle {
    val a    = Input(UInt(32.W))
    val b    = Input(UInt(32.W))
    val substracter = Input(Bool())
    val sum  = Output(UInt(32.W))
  })
  
  def unpackFloat(in: UInt): (Bool, UInt, UInt) = {
    val sign     = in(31)
    val exponent = in(30, 23)
    // Add implicit leading 1 to fraction unless exponent and fraction are both zero
    val fraction = Cat(1.U(1.W), in(22, 0)) // Implicit leading 1 for normalized numbers
    val isZero   = (exponent === 0.U) && (in(22, 0) === 0.U)
    val adjustedFraction = Mux(isZero, 0.U(24.W), fraction) // Set fraction to 0 if zero
    // Return sign, exponent, and fraction
    (sign, exponent, adjustedFraction)
  }



  // This version removes the extra space by directly appending bits as a string
    def binStr(x: UInt, width: Int): Printable = {
    var result: Printable = p""
    for (i <- (width - 1) to 0 by -1) {
      result = result + p"${x(i).asUInt}"
    }
    result
  }

  


  val (signA, expA, fracA) = unpackFloat(io.a)
  val (trueSignB, expB, fracB) = unpackFloat(io.b)
  val signB = Mux(io.substracter, !trueSignB, trueSignB) // Flip sign if subtracter is true

  
  val expDiff     = (expA -& expB).asSInt
  val expGreaterA = expDiff >= 0.S

  val alignedFracA = Wire(UInt(24.W))
  val alignedFracB = Wire(UInt(24.W))
  val resultExp    = Wire(UInt(8.W))
  val resultSign   = Wire(Bool())
  
  when (expGreaterA) {
    alignedFracA := fracA
    alignedFracB := fracB >> expDiff.asUInt
    resultExp    := expA
  } .otherwise {
    alignedFracA := fracA >> (-expDiff).asUInt
    alignedFracB := fracB
    resultExp    := expB
  }

  
  // Addition or subtraction based on signs
  val sumRaw = Wire(SInt(25.W))
  val extendedA = Cat(0.U(1.W), alignedFracA) // Add a 25th bit for the carry on
  val extendedB = Cat(0.U(1.W), alignedFracB)
  val absSum = Wire(UInt(25.W))
  when (signA === signB) {
    // printf("Adding...")
    sumRaw := (extendedA + extendedB).asSInt
    // A negative sign indicates a carry on
    absSum := sumRaw.asUInt
    resultSign := signA
    
  } .otherwise {
    sumRaw := (extendedA - extendedB).asSInt
    resultSign := Mux(alignedFracA >= alignedFracB, signA, signB)
    absSum := Mux(sumRaw < 0.S, -sumRaw, sumRaw).asUInt // Todo : make more efficient
  }

  val result = Cat(resultSign, resultExp, absSum(22, 0))


  // === DEBUG PRINTS ===
  // printf(p"\n==== DEBUG INFO ====\n")
  // printf(p"Input A: 0b${binStr(io.a,32)}\n")
  // printf(p"Input B: 0b${binStr(io.b,32)}\n")

  // printf(p"Sign A: $signA, Exp A: 0b${Binary(expA)}, Frac A: 0b${Binary(fracA)}\n")
  // printf(p"Sign B: $signB, Exp B: 0b${Binary(expB)}, Frac B: 0b${Binary(fracB)}\n")
  // printf(p"Exp Diff: $expDiff, Greater A: $expGreaterA\n")
  // printf(p"Aligned Frac A: 0b${Binary(alignedFracA)}, Aligned Frac B: 0b${Binary(alignedFracB)}\n")
  // printf(p"Extended A: 0b${Binary(extendedA)}, Extended B: 0b${Binary(extendedB)}\n")
  // printf(p"Sum Raw: 0b${Binary(sumRaw)}, Abs Sum: 0b${binStr(absSum,25)}\n")

 
  val isZeroA = (expA === 0.U) && (fracA(22, 0) === 0.U)  // Check if A is zero
  val isZeroB = (expB === 0.U) && (fracB(22, 0) === 0.U)  // Check if B is zero
  // printf(p"Is Zero A: $isZeroA, Is Zero B: $isZeroB\n")

  val isNaNA = (expA === 255.U) && !(fracA(22, 0) === 0.U) // Detect if A is NaN
  val isNaNB = (expB === 255.U) && !(fracB(22, 0) === 0.U) // Detect if B is NaN
  // printf(p"Is NaN A: $isNaNA, Is NaN B: $isNaNB\n")

  val isInfA = (expA === 255.U) && (fracA(22, 0) === 0.U) // Detect if A is Inf
  val isInfB = (expB === 255.U) && (fracB(22, 0) === 0.U) // Detect if B is Inf
  // printf(p"Is Inf A: $isInfA, Is Inf B: $isInfB\n")

  when(isNaNA || isNaNB) {
    // printf(p"NaN detected, setting to NaN\n")
    io.sum := Cat(resultSign, 255.U(8.W), 1.U(23.W)) // Set to NaN
  }. elsewhen(isInfA || isInfB) {
    // If the signs are the same, return infinity
    // If the signs are different, return NaN
    when (isInfA && !isInfB) {
      // printf(p"Infinity A detected\n")
      // Set to same infinity as A
      io.sum := Cat(resultSign, 255.U(8.W), 0.U(23.W)) // Set to infinity
    } .elsewhen (isInfB && !isInfA) {
      // printf(p"Infinity B detected\n")
      // Set to same infinity as B
      io.sum := Cat(resultSign, 255.U(8.W), 0.U(23.W)) // Set to infinity
    }. otherwise { //     when (isInfA && isInfB) 
      when (signB === signA) {
        // If the signs are the same, return infinity
        // printf(p"Converging infinity detected, setting to infinity\n")
        io.sum := Cat(resultSign, 255.U(8.W), 0.U(23.W)) // Set to infinity
      } .otherwise {
        // printf(p"Diverging infinity detected, setting to NaN\n")
        io.sum := Cat(resultSign, 255.U(8.W), 1.U(23.W)) // Set to NaN
      }
    }
 

  } .elsewhen(isZeroA && isZeroB) {
    io.sum := 0.U  // Both inputs are zero, so the sum is zero
  } .elsewhen(isZeroA) {
    // printf(p"Zero A detected, setting to B\n")
    io.sum := Cat(signB, io.b(30,0)) // To handle the sign change if substracter mode
  } .elsewhen(isZeroB) {
    // printf(p"Zero B detected, setting to A\n")
    io.sum := io.a  // If B is zero, result is A
  } .otherwise { // None are zero
      // Normalize the result, in case during a subtraction the result no longer has a leading 1
    // Find the position of the leading 1
    // Use bool in reverse order
    // Priority returns the bit position of the least-significant high bit of the input -> reverse
    val leadingZeros = PriorityEncoder(absSum.asBools.reverse) // Find the leading zeroes
    // printf(p"Leading Zeros: $leadingZeros\n")
    // Shift the result to normalize
    val normalizedFrac = (absSum << leadingZeros)(24, 1) // Shift out the leading zeroes, and keep the 24 MSB
    val normalizedExp = (resultExp - leadingZeros + 1.U)(7, 0) // +1 to account for the 25th carry on bit that was added, then gets removed as a leading zero
    // printf(p"Normalized Frac: 0b${binStr(normalizedFrac, 24)}\n")    
    // printf(p"Remaining part of the frac in output: 0b${binStr(normalizedFrac(22, 0), 22)}\n")
    // printf(p"Result Exp: 0b${binStr(resultExp, 8)}\n")
    // printf(p"Normalized Exp: 0b${binStr(normalizedExp, 8)}\n")
    // Handle going to infinity
    when ((resultExp === 255.U || normalizedExp  === 255.U) && normalizedFrac =/= 0.U) {
      // printf(p"Overflow detected, setting to infinity\n")
      io.sum := Cat(resultSign, 255.U(8.W), 0.U(23.W)) // Set to infinity
    } .elsewhen (normalizedFrac === 0.U) {
      // printf(p"Underflow detected, setting to zero\n")
      io.sum := 0.U // Set to zero
    } .otherwise {
      // printf(p"Normal case, setting to normal value\n")
      io.sum := Cat(resultSign, normalizedExp, normalizedFrac(22, 0))
    }

  }
  

  // printf(p"Output (io.sum): 0b${binStr(io.sum, 32)}\n")
  //printf(p"Output (io.sum): 0b" + Binary(io.sum.pad(32)) + p"\n")
  // Minimal debug print
  // printf("=================Adder===================================\n")
  // printf(p"Input A: 0b${binStr(io.a,32)}\n")
  // printf(p"Input B: 0b${binStr(io.b,32)}\n")
  // printf(p"Substracter: ${io.substracter}\n")
  // printf(p"Output: 0b${binStr(io.sum,32)}\n")
  // printf("====================================================\n")

}