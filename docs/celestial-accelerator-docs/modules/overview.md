# Overview

The accelerator is built with the following modules : 

![The celestial accelerator](../assets/image.png)


The top module receives the commands sent by the rocket core, and orchestrates the other modules to execute them. More detail is provided at [this page](celestial-top-module.md).

The switch module connects the top module and object processing units accordingly (see [this page](switch-module.md)). The object processing unit compute the velocity and position update of a given celestial object, see [this page](object-processing-unit.md).

To read the documentation, it is recommended to start with the velocity update flow, as optimizing this processs dictated most of the architecture. Then, the C codes can be read to understand how to use the accelerator, and at last each module can be understood individually. The accelerator includes some basic security features, and an analysis of the performance speedup provided by the accelerator was made. The accelerator was implemented on an FPGA, as described at [this page](../FPGA.md).

## Table of Contents

- [Overview](#overview)
- [Modules](#modules)
  - [Celestial Top Module](modules/celestial-top-module.md)
  - [Object Processing Unit](modules/object-processing-unit.md)
  - [Fast Negative Three-Half Exponent](modules/fast-negative-three-half.md)
  - [Switch Module](modules/switch-module.md)
  - [Arithmetic Units](modules/arithmetic-units.md)
- [Performance Analysis](performance/performance-analysis.md)
- [Security Risks](security/security-risks.md)
- [Guides](#guides)
  - [Getting Started](guides/getting-started.md)
  - [C Code Examples](guides/c-code-examples.md)
- [Assets](assets/images)