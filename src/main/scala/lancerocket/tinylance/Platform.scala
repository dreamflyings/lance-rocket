// See LICENSE.SiFive for license details.
// See LICENSE.HORIE_Tetsuya for license details.
package lancerocket.tinylance

import Chisel._

import freechips.rocketchip.config._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util.ResetCatchAndSync
import freechips.rocketchip.system._

import sifive.blocks.devices.mockaon._
import sifive.blocks.devices.gpio._
import sifive.blocks.devices.uart._
import sifive.blocks.devices.seg7._
import sifive.blocks.devices.pinctrl._

//-------------------------------------------------------------------------
// PinGen
//-------------------------------------------------------------------------

object PinGen {
  def apply(): BasePin =  {
    val pin = new BasePin()
    pin
  }
}

//-------------------------------------------------------------------------
// TinyLancePlatformIO
//-------------------------------------------------------------------------

class TinyLancePlatformIO(implicit val p: Parameters) extends Bundle {
  val pins = new Bundle {
    val gpio = new GPIOPins(() => PinGen(), p(PeripheryGPIOKey)(0))
    val seg7 = new Seg7LEDPins(() => PinGen())
    val aon  = new MockAONWrapperPins()
  }
}

//-------------------------------------------------------------------------
// TinyLancePlatform
//-------------------------------------------------------------------------

class TinyLancePlatform(implicit val p: Parameters) extends Module {
  val sys = Module(LazyModule(new TinyLanceSystem).module)
  val io = new TinyLancePlatformIO

  // This needs to be de-asserted synchronously to the coreClk.
  val async_corerst = sys.aon.rsts.corerst
  // Add in debug-controlled reset.
  sys.reset := ResetCatchAndSync(clock, async_corerst, 20)

  //-----------------------------------------------------------------------
  // Check for unsupported rocket-chip connections
  //-----------------------------------------------------------------------

  require (p(NExtTopInterrupts) == 0, "No Top-level interrupts supported");

  //-----------------------------------------------------------------------
  // Build GPIO Pin Mux
  //-----------------------------------------------------------------------
  // Pin Mux for UART
  // First convert the System outputs into "IOF" using the respective *GPIOPort
  // converters.

  val sys_uart = sys.uart
  val uart_pins = sys.outer.uartParams.map { c => Wire(new UARTPins(() => PinGen()))}
  (uart_pins zip  sys_uart) map {case (p, r) => UARTPinsFromPort(p, r, clock = clock, reset = reset, syncStages = 0)}

  //-----------------------------------------------------------------------
  // Default Pin connections before attaching pinmux

  for (iof_0 <- sys.gpio(0).iof_0.get) {
    iof_0.default()
  }

  for (iof_1 <- sys.gpio(0).iof_1.get) {
    iof_1.default()
  }

  //-----------------------------------------------------------------------

  val iof_0 = sys.gpio(0).iof_0.get
  val iof_1 = sys.gpio(0).iof_1.get

  // UART0
  BasePinToIOF(uart_pins(0).rxd, iof_0(16))
  BasePinToIOF(uart_pins(0).txd, iof_0(17))

  //-----------------------------------------------------------------------
  // Drive actual Pads
  //-----------------------------------------------------------------------

  // Result of Pin Mux
  GPIOPinsFromPort(io.pins.gpio, sys.gpio(0))

  // Dedicated Seg 7 LED Pads
  Seg7LEDPinsFromPort(io.pins.seg7, sys.seg7Led(0), clock = sys.clock, reset = sys.reset, syncStages = 0)

  // AON Pads -- direct connection is OK because
  // EnhancedPin is hard-coded in MockAONPads
  // and thus there is no .fromPort method.
  io.pins.aon <> sys.aon.pins
}
