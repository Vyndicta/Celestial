# Celestial Top Module

## Overview

The Celestial Top Module serves as the primary interface between the accelerator and the rocket core. It is responsible for managing communication, controlling the simulation iterations, and handling data packets that dictate the operations of the accelerator.

## Communication with the Rocket Core

The communication between the Celestial Top Module and the rocket core is established through the transmission of 64-bit packets. These packets are sent to a `data_in` register within the top module. The structure of these packets is crucial for ensuring that commands are executed correctly and efficiently.

## Packet Structure

The data packets consist of the following components:

- **Command**: Specifies the action that the accelerator is expected to perform.
- **Lock Key**: Ensures that the request is authorized, providing a layer of security against unauthorized commands.
- **Data**: Contains any additional values relevant to the command being executed.

## Supported Commands

The Celestial Top Module supports a variety of commands that allow for flexible control of the accelerator. These commands enable the execution of tasks such as starting or stopping simulations, adjusting parameters, and retrieving status information.

## Conclusion

The Celestial Top Module is a critical component of the accelerator, facilitating communication and control. Its design ensures that the accelerator operates efficiently and securely, allowing for accurate simulations of celestial dynamics.