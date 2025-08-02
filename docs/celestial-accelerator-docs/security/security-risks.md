# Security risks

This document outlines the security considerations for the Celestial Accelerator, including a threat model and implemented security features.

## Threat model

### Scope and assets

The key assets protected by the Celestial Accelerator security model include:

- Confidential simulation data (positions and velocities of celestial bodies)
- Accelerator state (internal registers and configuration)
- Execution integrity of the simulation logic
- Simulation parameters (gravitational constant-scaled mass and time step)

The scope of this threat model is limited to attacks originating from other software running on the same SoC. Other threats, such as reading the bus to steal data, would require advanced cryptographic modules as countermeasures, which is outside the scope of this project.

### Trust boundaries

Trust is explicitly placed in the software component that successfully locks the accelerator. The following boundaries are identified:

- Between untrusted software and the MMIO interface
- Between multiple software processes potentially trying to access the accelerator simultaneously

The root of trust for this system is assumed to be:

- A secure boot process that ensures only signed and verified firmware and kernel code are executed
- The kernel-level MMIO driver that is responsible for managing access to the accelerator

Any compromise of the root of trust would invalidate all other protections.

### Threat actors and attack surfaces

This model considers software-only attackers without physical access. The primary threat actors considered are:

- Malicious software running on the same host CPU
- Software attempting to read simulation results without authorization
- Software attempting to corrupt simulation results or initial parameters

Key attack surfaces include:

- The MMIO command interface
- Shared output registers
- Inactivity timeout behavior
- Locking mechanism and key management

### Attack surface analysis

The most critical vulnerabilities identified are:

- **Race conditions**: Competing software may try to access or alter the MMIO interface mid-simulation
- **Data leakage**: The output registers may be read by unauthorized processes
- **Denial of service**: An attacker could continuously send invalid commands, preventing useful access
- **Brute force attack**: The 27-bit long lock key could be brute-forced, especially if the accelerator is kept locked during a long acceleration

### Threat enumeration (STRIDE Model)

Based on the STRIDE model:

- **Spoofing**: An unauthorized process may impersonate a legitimate one by guessing or reusing a lock key
- **Tampering**: Malicious MMIO commands could alter initial conditions or running simulations
- **Repudiation**: Any software with the lock's key can request to output the data
- **Information disclosure**: Simulation data may leak if bit flip masks are predictable or reused
- **Denial of Service**: Spamming invalid commands could prevent legitimate ones from being received, or a malicious software could lock the accelerator and keep it locked with minimal activity
- **Elevation of privilege**: If a lower-privilege process accesses MMIO without proper enforcement, it might impact simulation results, which could affect how higher-privileged software behaves

## Security features

The Celestial Accelerator incorporates the following security features to mitigate the identified threats:

### Locking mechanism

To start using the accelerator, a program must first send a 27-bit lock value. All subsequent commands require the same lock value to be valid, ensuring that another program can't use the accelerator while the first one is still using it.

To prevent permanent denial of service if a program crashes after locking the accelerator, an automatic timeout feature unlocks the accelerator after an arbitrary number of cycles without receiving valid commands.

### State zeroing

When the lock is released (either manually or through timeout), internal data and registers are cleared to prevent information reuse. This ensures that only a program holding the initial lock key can access the data that was sent.

### Bit flipping protection

To protect against unauthorized data access, every command that outputs data takes a bit flip mask as input. This bit flip mask is known only to the program that already holds the simulation data. By using a randomly generated bit flip mask for each output request, only the authorized program can correctly interpret the output values.

## Future security enhancements

Potential security enhancements for future versions include:

- Rate limiting and invalid command detection with alarms
- More sophisticated key management
- Encryption of data in transit
- Additional countermeasures for threats outside the current scope of analysis

## References

- For implementation details of the locking mechanism, see [SetLock.c](/CelestialAccelerator/C_Codes/SetLock.c)
- For information on how to securely use the accelerator, see the [Getting Started Guide](../guides/getting-started.md)
