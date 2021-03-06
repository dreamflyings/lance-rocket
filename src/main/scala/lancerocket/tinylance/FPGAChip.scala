// See LICENSE.SiFive for license details.
// See LICENSE.HORIE_Tetsuya for license details.
package lancerocket.tinylance

import Chisel._
import chisel3.core.{attach}
import chisel3.experimental.{withClockAndReset}

import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy.{LazyModule}

import sifive.blocks.devices.gpio._

import sifive.fpgashells.ip.xilinx.{IBUFG, IOBUF, PULLUP, PowerOnResetFPGAOnly, STARTUPE2}

//-------------------------------------------------------------------------
// TinyLanceChip
//-------------------------------------------------------------------------

class TinyLanceChip(implicit override val p: Parameters) extends TinyLanceShell {

  //-----------------------------------------------------------------------
  // Clock divider
  //-----------------------------------------------------------------------
  val slow_clock = Wire(Bool())

  // Divide clock by 256, used to generate 32.768 kHz clock for AON block
  withClockAndReset(clock_8MHz, ~mmcm_locked) {
    val clockToggleReg = RegInit(false.B)
    val (_, slowTick) = Counter(true.B, 256)
    when (slowTick) {clockToggleReg := ~clockToggleReg}
    slow_clock := clockToggleReg
  }

  //-----------------------------------------------------------------------
  // DUT
  //-----------------------------------------------------------------------

  withClockAndReset(clock_32MHz, ~ck_rst) {
    val dut = Module(new TinyLancePlatform)

    // UART
    IOBUF(uart_txd_in, dut.io.pins.gpio.pins(16))
    IOBUF(uart_rxd_out, dut.io.pins.gpio.pins(17))
    val iobuf_uart_cts = Module(new IOBUF())
    iobuf_uart_cts.io.I := false.B
    iobuf_uart_cts.io.T := false.B
    attach(uart_cts, iobuf_uart_cts.io.IO)

    // Only 19 out of 20 shield pins connected to GPIO pins
    // Shield pin A5 (pin 14) left unconnected
    // The buttons are connected to some extra GPIO pins not connected on the
    // HiFive1 board
    IOBUF(btn_0, dut.io.pins.gpio.pins(15))
    IOBUF(btn_1, dut.io.pins.gpio.pins(30))
    IOBUF(btn_2, dut.io.pins.gpio.pins(31))

    val iobuf_btn_3 = Module(new IOBUF())
    iobuf_btn_3.io.I := ~dut.io.pins.aon.pmu.dwakeup_n.o.oval
    iobuf_btn_3.io.T := ~dut.io.pins.aon.pmu.dwakeup_n.o.oe
    attach(btn_3, iobuf_btn_3.io.IO)
    dut.io.pins.aon.pmu.dwakeup_n.i.ival := ~iobuf_btn_3.io.O & dut.io.pins.aon.pmu.dwakeup_n.o.ie

    // Use the LEDs for some more useful debugging things
    IOBUF(led_0, ck_rst)
//    IOBUF(led_1, SRST_n)
    IOBUF(led_2, dut.io.pins.aon.pmu.dwakeup_n.i.ival)
    IOBUF(led_3, dut.io.pins.gpio.pins(14))

    IOBUF(led_4, dut.io.pins.gpio.pins(4))
    IOBUF(led_5, dut.io.pins.gpio.pins(5))
    IOBUF(led_6, dut.io.pins.gpio.pins(6))
    IOBUF(led_7, dut.io.pins.gpio.pins(7))
    IOBUF(led_8, dut.io.pins.gpio.pins(8))
    IOBUF(led_9, dut.io.pins.gpio.pins(9))
    IOBUF(led_10, dut.io.pins.gpio.pins(10))
    IOBUF(led_11, dut.io.pins.gpio.pins(11))
    IOBUF(led_12, dut.io.pins.gpio.pins(26))
    IOBUF(led_13, dut.io.pins.gpio.pins(27))
    IOBUF(led_14, dut.io.pins.gpio.pins(28))
    IOBUF(led_15, dut.io.pins.gpio.pins(29))

    // Seg 7 LED
    IOBUF(seg7_ca, dut.io.pins.seg7.cathodes(0))
    IOBUF(seg7_cb, dut.io.pins.seg7.cathodes(1))
    IOBUF(seg7_cc, dut.io.pins.seg7.cathodes(2))
    IOBUF(seg7_cd, dut.io.pins.seg7.cathodes(3))
    IOBUF(seg7_ce, dut.io.pins.seg7.cathodes(4))
    IOBUF(seg7_cf, dut.io.pins.seg7.cathodes(5))
    IOBUF(seg7_cg, dut.io.pins.seg7.cathodes(6))

    IOBUF(seg7_dp, dut.io.pins.seg7.decimalPoint)

    IOBUF(seg7_an_0, dut.io.pins.seg7.anodes(0))
    IOBUF(seg7_an_1, dut.io.pins.seg7.anodes(1))
    IOBUF(seg7_an_2, dut.io.pins.seg7.anodes(2))
    IOBUF(seg7_an_3, dut.io.pins.seg7.anodes(3))
    IOBUF(seg7_an_4, dut.io.pins.seg7.anodes(4))
    IOBUF(seg7_an_5, dut.io.pins.seg7.anodes(5))
    IOBUF(seg7_an_6, dut.io.pins.seg7.anodes(6))
    IOBUF(seg7_an_7, dut.io.pins.seg7.anodes(7))

    //---------------------------------------------------------------------
    // Unconnected inputs
    //---------------------------------------------------------------------

    dut.io.pins.aon.erst_n.i.ival       := ~reset_periph
    dut.io.pins.aon.lfextclk.i.ival     := slow_clock
    dut.io.pins.aon.pmu.vddpaden.i.ival := 1.U
  }
}
