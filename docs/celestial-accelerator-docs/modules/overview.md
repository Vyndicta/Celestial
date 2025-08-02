# Overview

The accelerator is built with the following modules : 

![The celestial accelerator](../assets/image.png)


The top module receives the commands sent by the rocket core, and orchestrates the other modules to execute them. More detail is provided at [this page](celestial-top-module.md).

The switch module connects the top module and object processing units accordingly (see [this page](switch-module.md)). The object processing unit compute the velocity and position update of a given celestial object, see [this page](object-processing-unit.md).
