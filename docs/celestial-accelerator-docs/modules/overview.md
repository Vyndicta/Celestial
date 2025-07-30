# Overview

The accelerator is built with the following modules : 



The top module receives the commands sent by the rocket core, and orchestrates the other modules to execute them. More detail is provided at [this page](https://github.com/Vyndicta/Celestial/tree/main/CelestialAccelerator/celesital-top-module).

The switch module connects the top module and object processing units accordingly (see [this page](https://github.com/Vyndicta/Celestial/tree/main/CelestialAccelerator/switch-module) ). The object processing unit compute the velocity and position update of a given celestial object, see this page](https://github.com/Vyndicta/Celestial/tree/main/CelestialAccelerator/object-processing-unit).