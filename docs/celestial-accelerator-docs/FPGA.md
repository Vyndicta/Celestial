---
layout: default
title: FPGA implementation
---

# Requirments

Chipyard \
Vivado \
Add the board to vivado (https://digilent.com/reference/programmable-logic/guides/installing-vivado-and-sdk?redirect=1#installing_digilent_board_files)  \
Share the USB port to the VM  \
Install digilent Adept runtime and utilities  \

# First setup

Plug the FPGA using the PROG port  \
Check if the device is seen in the VM using djtgcfg enum. If it isn't recognised, you might have some diver conflicts with Linux to troubleshoot first.  \

## Sending a code 

First (which might take 30 minutes or more):  \

```
chipyard-start
cd fpga/
make SUB_PROJECT=nexysvideo bitstream 
```

Then, send the binary to the FPGA. To do so, start one terminal, and call: \
hw_server \

Then, open another terminal and  \
```
vivado -mode tcl
open_hw_manager
connect_hw_server
open_hw_target
get_hw_devices
```

get_hw_devices should return something, otherwise the device is not detected. If it appears, you may continue with: \ 

```
set_property PROGRAM.FILE /home/elech505/chipyard/fpga/generated-src/chipyard.fpga.nexysvideo.NexysVideoHarness.RocketNexysVideoConfig/obj/NexysVideoHarness.bit [get_hw_devices xc7a200t_0]
program_hw_devices [get_hw_devices xc7a200t_0]
```
If the device doesn't appear, it can be an issue with: the USB port sharing, the drivers which are not installed, linux not attributing the right driver, ... \ 
You should see some LEDs blinking on the FPGA once the binary is sent. \


To send the Celestial binary: \ 
```
set_property PROGRAM.FILE /home/elech505/chipyard/fpga/generated-src/chipyard.fpga.nexysvideo.NexysVideoHarness.CelestialNexysVideoConfig/obj/NexysVideoHarness.bit [get_hw_devices xc7a200t_0]
program_hw_devices [get_hw_devices xc7a200t_0]
```

# Sending a C code
First, plug the FPGA with the UART port. To check that it is seen, you may use ls /dev/ttyUSB* \ 
Then : \ 
```
chipyard-start
cd generators/testchipip/uart_tsi

./uart_tsi +tty=/dev/ttyUSB0 /home/elech505/chipyard/tests/build/hello.riscv
./uart_tsi +tty=/dev/ttyUSB0 +selfcheck /home/elech505/chipyard/tests/build/hello.riscv +baudrate=115200
```