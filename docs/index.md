---
layout: default
title: Main page
---

# Celestial Accelerator

This website documents the Celestial accelerator, an accelerator for [n-body](https://en.wikipedia.org/wiki/N-body_simulation) simulation. This hardware accelerator uses multiple processing elements and an optimized velocity update flow, so that the computation time scales in n, instead of n² as a software implementation would. The accelerator was built using Chipyard and Chisel. Example C codes to use the accelerator are provided [here](https://github.com/Vyndicta/Celestial/tree/main/CelestialAccelerator/C_Codes), and an overview of the accelerator is provided [here](celestial-accelerator-docs/modules/overview).

The main features are:
    - optimized velocity update flow, using a custom algorithm similar to the fast inverse square root algorithm, to decrease the number of cycles as much as possible.
    - The arithmetic modules are shared to decrease the ressource utilization
    - Parametrisation, as the number of body processing units is left as a Chisel parameter
    - Basic security features, to ensure privacy and avoid data tempering