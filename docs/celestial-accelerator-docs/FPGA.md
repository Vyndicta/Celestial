---
layout: default
title: FPGA implementation
---

<style>
table {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 1em;
}

th, td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

tr:nth-child(even) {
  background-color: #f9f9f9;
}

th {
  background-color: #f2f2f2;
}
</style>

# Results

The accelerator was implemented on a Nexys Video Artix-7 FPGA, with 20 body processing units (BPUs) alongside a Rocket Core. The implementation's performance and resource utilization were compared against a baseline version of the Rocket Core without the accelerator.

## Performance and Resource Utilization

In terms of LUT utilization:

| Version             | Effective Frequency (MHz) | Total LUT Utilization | Logic LUT Utilization |
| ------------------- | ------------------------- | --------------------- | --------------------- |
| Baseline            | 75.0                      | 42,852                | 39,707                |
| With Accelerator    | 16.7                      | 102,810               | 99,661                |



### Analysis

- The introduction of the accelerator leads to a higher increase in LUT utilization than expected. This is because the ^-3/2 module seems to have one slow step, which forces heavy optimization during the PnR flow. This leads to extremely high utilization, which could probably be greatly reduced with some investigation.
- The maximum operating frequency is reduced by a factor of approximately 4.4x (from 75.0 MHz to 16.7 MHz). The critical path is in the buffer register of the fast negative three-half algorithm ($x^{-3/2}$). Once again, some investigation would likely be able to increase drastically this frequency.
- Despite this frequency reduction, the accelerator provides a **626% speed-up** for a 2-body simulation at iso frequency. This significant performance gain compensates for the lower clock speed, meaning that the accelerator outperforms a pure software implementation, even in this worst-case scenario (2 bodies).

## Sub-module Utilization

The LUT utilization for the primary sub-modules within the BPU is detailed below:

| Module type              | Min total LUTs | Max total LUTs |
| ------------------------ | -------------- | -------------- |
| BPU                      | 588            | 1,674          |
| NegThreeHalfExpTop       | 3              | 3              |
| NegThreeHalfExpInitial   | 62             | 63             |
| NegThreeHalfExpRefine    | 1,870          | 2,706          |
| F32Multiplier            | 185            | 188            |

*Note: The synthesis report did not explicitly list the add/subtract module, suggesting it may have been embedded directly into the BPU logic by the Chisel compiler.*

### Analysis

- The `NegThreeHalfExpRefine` module shows high and widely varying LUT utilization. This is likely because it lies on the critical path and is therefore subject to heavy optimization during synthesis.
- The refining module (`NegThreeHalfExpRefine`) consumes approximately 10 times more LUTs than the multiplier module. This makes the current module sharing strategy useless, but is due to the high optimization process. Finding the problematic step in this module and diving it in more steps would fix this issue.

## Conclusion and Future Work

The FPGA implementation successfully demonstrates that the Celestial accelerator provides a substantial performance improvement over a software-only approach, even with a notable decrease in clock frequency.

Key areas for future optimization include:
1.  **Pipelining the $x^{-3/2}$ module:** Further pipelining the `NegThreeHalfExpRefine` stage could significantly improve the maximum operating frequency.
2.  **Optimizing BPU activity:** During the velocity update phase, the BPU broadcasting its position is idle. Eliminating this idle state by broadcasting and computing at once could nearly double the performance in the worst case scenarios.

# To implement a chipyard design on an FPGA:
# Requirments

Chipyard \
Vivado \
Add the board to vivado (https://digilent.com/reference/programmable-logic/guides/installing-vivado-and-sdk?redirect=1#installing_digilent_board_files)  \
Share the USB port to the VM  \
Install digilent Adept runtime and utilities  


# Start

Plug the FPGA using the PROG port  \
Check if the device is seen in the VM using djtgcfg enum. If it isn't recognised, you might have some diver conflicts with Linux to troubleshoot first.  

## Sending a code 

First (which might take 30 minutes or more):  

```
chipyard-start
cd fpga/
make SUB_PROJECT=nexysvideo bitstream 
```

Then, send the binary to the FPGA. To do so, start one terminal, and call:
```
hw_server 
```

Then, open another terminal and call:
```
vivado -mode tcl
open_hw_manager
connect_hw_server
open_hw_target
get_hw_devices
```

get_hw_devices should return something, otherwise the device is not detected. If it appears, you may continue with: 

```
set_property PROGRAM.FILE /home/elech505/chipyard/fpga/generated-src/chipyard.fpga.nexysvideo.NexysVideoHarness.RocketNexysVideoConfig/obj/NexysVideoHarness.bit [get_hw_devices xc7a200t_0]
program_hw_devices [get_hw_devices xc7a200t_0]
```
If the device doesn't appear, it can be an issue with: the USB port sharing, the drivers which are not installed, linux not attributing the right driver, ...

You should see some LEDs blinking on the FPGA once the binary is sent.


To send a custom binary, e.g. the binary for the Celestial accelerator: 
```
set_property PROGRAM.FILE /home/elech505/chipyard/fpga/generated-src/chipyard.fpga.nexysvideo.NexysVideoHarness.CelestialNexysVideoConfig/obj/NexysVideoHarness.bit [get_hw_devices xc7a200t_0]
program_hw_devices [get_hw_devices xc7a200t_0]
```

# Sending a C code
First, plug the FPGA with the UART port. To check that it is seen, you may use ls /dev/ttyUSB* \
Then : 
```
chipyard-start
cd generators/testchipip/uart_tsi

./uart_tsi +tty=/dev/ttyUSB0 /home/elech505/chipyard/tests/build/hello.riscv
./uart_tsi +tty=/dev/ttyUSB0 +selfcheck /home/elech505/chipyard/tests/build/hello.riscv +baudrate=115200
```