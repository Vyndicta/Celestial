# Overview

The accelerator is built with the following modules : 



The top module receives the commands sent by the rocket core, and orchestrates the other modules to execute them. More detail is provided at [this page](celestial-accelerator-docs/modules/overview/celesital-top-module).

The switch module connects the top module and object processing units accordingly (see [this page](celestial-accelerator-docs/modules/overview/switch-module) ). The object processing unit compute the velocity and position update of a given celestial object, see this page](celestial-accelerator-docs/modules/overview/object-processing-unit).

To read the documentation, it is recommended to start with the velocity update flow, as optimizing this processs dictated most of the architecture. Then, the C codes can be read to understand how to use the accelerator, and at last each module can be understood individually. The accelerator includes some basic security features, and an analysis of the performance speedup provided by the accelerator was made. The accelerator was implemented on an FPGA, as described at  [this page](celestial-accelerator-docs/FPGA).