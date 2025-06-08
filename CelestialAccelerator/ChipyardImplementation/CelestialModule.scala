class CelestialTopIO extends Bundle {
  val dOut = Output(UInt(32.W))
  val locked = Output(Bool())
  val currentIteration = Output(UInt(32.W))
  val dIn = Input(UInt(64.W))
}

case class CelestialParams(
  BPE_num: Int
)

trait CelestialModule extends HasRegMap {
  val io: CelestialTopIO

  implicit val p: Parameters
  def params: CelestialParams
  val clock: Clock

  val dIn = Reg(UInt(64.W))
  val dOut = Wire(UInt(32.W))
  val locked = Wire(Bool())
  val currentIteration = Wire(UInt(32.W))

  val impl = Module(new CelestialTop(params.BPE_num))
  impl.io.dIn := dIn

  dOut := impl.io.dOut
  locked := impl.io.locked
  currentIteration := impl.io.currentIteration

  val status = Cat(locked, 0.U(31.W))

  regmap(
    0x00 -> Seq(
      RegField.r(32, status)),
    0x04 -> Seq(
      RegField.w(64, dIn)),
    0x0C -> Seq(
      RegField.r(32, dOut)),
    0x10 -> Seq(
      RegField.r(32, currentIteration))
  )
}

class CelestialTL(params: CelestialParams, beatBytes: Int)(implicit p: Parameters)
  extends TLRegisterRouter(
    params.address, "celestial", Seq("ucbbar,celestial"),
    beatBytes = beatBytes)(
      new TLRegBundle(params, _) with CelestialTopIO)(
      new TLRegModule(params, _, _) with CelestialModule)


trait CanHavePeripheryCelestial { this: BaseSubsystem =>
  private val portName = "celestial"

  val celestial_locked = p(CelestialKey) match {
    case Some(params) => {
      val celestial = pbus {
        LazyModule(new CelestialTL(params, pbus.beatBytes)(p))
      }
      pbus.coupleTo(portName) {
        celestial.node :=
          TLFragmenter(pbus.beatBytes, pbus.blockBytes) := _
      }

      val pbus_io = pbus { InModuleBody {
        val locked = IO(Output(Bool()))
        locked := celestial.module.io.locked
        locked
      }}

      val top_locked = InModuleBody {
        val locked = IO(Output(Bool())).suggestName("celestial_locked")
        locked := pbus_io
        locked
      }

      Some(top_locked)
    }
    case None => None
  }
}

class WithCelestial(BPE_num: Int = 4) extends Config((site, here, up) => {
  case CelestialKey => {
    Some(CelestialParams(
      address = 0x4000,
      BPE_num = BPE_num
    ))
  }
})
