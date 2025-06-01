# C code examples

This document provides practical C code examples demonstrating how to interact with the celestial accelerator. These examples are based on the actual implementation and show the complete workflow from initialization to simulation execution.

## Command definitions

```c
#define CELESTIAL_DIN       0x4000
#define CELESTIAL_LOCKED    0x4010
#define CELESTIAL_DOUT      0x4020
#define CELESTIAL_ITERATION 0x4030

// Command codes
#define CMD_IDLE                0
#define CMD_LOCK                1
#define CMD_SET_X               3
#define CMD_SET_Y               4
#define CMD_SET_Z               5
#define CMD_SET_MASS            6
#define CMD_SET_SIZE            7
#define CMD_SET_DT              8
#define CMD_FORWARD_POSITION    9
#define CMD_FORWARD_VELOCITY    10
#define CMD_START_SIMULATION    12
#define CMD_STOP_SIMULATION     13
#define CMD_SET_ACTIVE_BPES     15
#define CMD_KEEP_ALIVE          16
#define CMD_SET_TARGET          17
#define CMD_OUTPUT_X            18
#define CMD_OUTPUT_Y            19
#define CMD_OUTPUT_Z            20
#define CMD_OUTPUT_DX           21
#define CMD_OUTPUT_DY           22
#define CMD_OUTPUT_DZ           23
```

## Packet communication

```c
void sendPacket(uint32_t data, uint8_t cmd, uint32_t lock)
{
    // Mask lock to ensure it's only 27 bits
    lock = lock & 0x07FFFFFF;
    uint64_t packet = ((uint64_t)cmd << 59) | ((uint64_t)lock << 32) | data;
    reg_write64(CELESTIAL_DIN, packet);
}
```

## Example 1: Locking the accelerator

```c
#include "mmio.h"
#include <stdio.h>
#include <stdint.h>

int lockAccelerator(uint32_t lock)
{
    // If device is already locked, return false
    if (reg_read8(CELESTIAL_LOCKED) != 0x0) {
        return 1;
    }
    uint32_t data = 0x0;
    uint8_t cmd = CMD_LOCK;
    sendPacket(data, cmd, lock);
    return 0;
}

int main(void)
{
    // Wait for the accelerator to be ready
    while (reg_read8(CELESTIAL_LOCKED) != 0x0) {
        // Wait for the accelerator to be ready
    }
    
    uint32_t lock = 0x12345;
    int lockStatus = lockAccelerator(lock);
    if (lockStatus == 1) {
        printf("Failed to lock accelerator\n");
        return -1;
    }
    
    printf("Successfully locked accelerator\n");
    return 0;
}
```

## Example 2: Sending celestial body data

```c
#include "mmio.h"
#include <stdio.h>
#include <stdint.h>

uint32_t floatToBits(float f)
{
    // Convert float to bits
    uint32_t bits;
    memcpy(&bits, &f, sizeof(float));
    return bits;
}

void sendOnePos(int type, float coo, uint32_t lock)
{
    uint32_t data = floatToBits(coo);
    uint8_t cmd = CMD_SET_X + type; // X=3, Y=4, Z=5
    sendPacket(data, cmd, lock);
}

void setMass(float mass, uint32_t lock)
{
    uint32_t data = floatToBits(mass);
    uint8_t cmd = CMD_SET_MASS;
    sendPacket(data, cmd, lock);
}

void setSize(float size, uint32_t lock)
{
    uint32_t data = floatToBits(size);
    uint8_t cmd = CMD_SET_SIZE;
    sendPacket(data, cmd, lock);
}

void sendPosition(float x, float y, float z, int targetBPE, uint32_t lock)
{
    sendOnePos(0, x, lock);     // X coordinate
    sendOnePos(1, y, lock);     // Y coordinate
    sendOnePos(2, z, lock);     // Z coordinate

    uint32_t data = targetBPE;
    uint8_t cmd = CMD_FORWARD_POSITION;
    sendPacket(data, cmd, lock);
}

void sendVelocity(float vx, float vy, float vz, int targetBPE, uint32_t lock)
{
    sendOnePos(0, vx, lock);    // X velocity
    sendOnePos(1, vy, lock);    // Y velocity
    sendOnePos(2, vz, lock);    // Z velocity
    
    uint32_t data = targetBPE;
    uint8_t cmd = CMD_FORWARD_VELOCITY;
    sendPacket(data, cmd, lock);
}

int main(void)
{
    uint32_t lock = 0x12345;
    
    // Earth data (scaled for the accelerator's range)
    float mass = 5.972e24f;                          // Mass of Earth in kg
    float size = 1.0f;                               // Size of Earth
    float x = -9.34039169997118860e+07f * 1e3f;     // X position
    float y = -1.18811312084356889e+08f * 1e3f;     // Y position
    float z = 7.94186043863161467e+03f * 1e3f;      // Z position
    float vx = 2.29385493455156606e+01f * 1e3f;     // X velocity
    float vy = -1.85184623747619383e+01f * 1e3f;    // Y velocity
    float vz = 1.33196768834853430e-03f * 1e3f;     // Z velocity
    
    setMass(mass, lock);
    setSize(size, lock);
    sendPosition(x, y, z, 1, lock);  // Send to BPE 1
    sendVelocity(vx, vy, vz, 1, lock); // Send to BPE 1
    
    return 0;
}
```

## Example 3: Running a simulation

```c
#include "mmio.h"
#include <stdio.h>
#include <stdint.h>

float bitsToFloat(uint32_t bits)
{
    float f;
    memcpy(&f, &bits, sizeof(float));
    return f;
}

uint32_t read_dOut()
{
    return reg_read32(CELESTIAL_DOUT);
}

uint32_t getIteration(uint32_t lock)
{
    return reg_read32(CELESTIAL_ITERATION);
}

void setTimeStep(float dt, uint32_t lock)
{
    uint32_t data = floatToBits(dt);
    uint8_t cmd = CMD_SET_DT;
    sendPacket(data, cmd, lock);
}

void setActiveBPEs(uint32_t activeBPEs, uint32_t lock)
{
    uint32_t data = activeBPEs;
    uint8_t cmd = CMD_SET_ACTIVE_BPES;
    sendPacket(data, cmd, lock);
}

void startSimulation(uint32_t lock)
{
    uint32_t data = 0x0;
    uint8_t cmd = CMD_START_SIMULATION;
    sendPacket(data, cmd, lock);
}

void sendKeepAlive(uint32_t lock)
{
    uint32_t data = 0x0;
    uint8_t cmd = CMD_KEEP_ALIVE;
    sendPacket(data, cmd, lock);
}

void sendIdle(uint32_t lock)
{
    uint32_t data = 0x0;
    uint8_t cmd = CMD_IDLE;
    sendPacket(data, cmd, lock);
}

void runSimulationAcc(uint32_t lock, int maxWait)
{
    startSimulation(lock);
    sendIdle(lock); // Prevent accelerator from restarting
    
    int currentWait = 0;
    uint32_t lastIteration = 0;
    
    while (currentWait < maxWait) {
        currentWait++;
        uint32_t currentIteration = getIteration(lock);
        
        if (currentIteration != lastIteration) {
            lastIteration = currentIteration;
        }
        
        // Check if simulation completed (iteration reset to 0)
        if (currentIteration == 0 && lastIteration != 0) {
            break;
        }
        
        lastIteration = currentIteration;
        sendKeepAlive(lock);
        sendIdle(lock);
    }
}

int main(void)
{
    uint32_t lock = 0x12345;
    
    // Simulation parameters for one year with daily time steps
    float dt = 60.0f * 60.0f * 24.0f;    // One day in seconds
    int numIterations = 365;              // One year
    int maxWait = numIterations * 10;     // Safety timeout
    uint32_t activeBPEs = 2;              // Two bodies
    
    setTimeStep(dt, lock);
    setActiveBPEs(activeBPEs, lock);
    
    printf("Starting simulation...\n");
    runSimulationAcc(lock, maxWait);
    printf("Simulation completed\n");
    
    return 0;
}
```

## Example 4: Retrieving simulation results

```c
void setTarget(uint32_t target, uint32_t lock)
{
    uint32_t data = target;
    uint8_t cmd = CMD_SET_TARGET;
    sendPacket(data, cmd, lock);
}

float getX(uint32_t lock)
{
    uint32_t data = rand(); // Random bit flip mask for security
    uint8_t cmd = CMD_OUTPUT_X;
    sendPacket(data, cmd, lock);
    uint32_t output = read_dOut();
    output ^= data; // Unflip the bits
    return bitsToFloat(output);
}

float getY(uint32_t lock)
{
    uint32_t data = rand();
    uint8_t cmd = CMD_OUTPUT_Y;
    sendPacket(data, cmd, lock);
    uint32_t output = read_dOut();
    output ^= data;
    return bitsToFloat(output);
}

float getZ(uint32_t lock)
{
    uint32_t data = rand();
    uint8_t cmd = CMD_OUTPUT_Z;
    sendPacket(data, cmd, lock);
    uint32_t output = read_dOut();
    output ^= data;
    return bitsToFloat(output);
}

int main(void)
{
    uint32_t lock = 0x12345;
    
    // Get Earth's position after simulation
    setTarget(1, lock); // Target BPE 1 (Earth)
    
    float posEarthX = getX(lock);
    float posEarthY = getY(lock);
    float posEarthZ = getZ(lock);
    
    printf("Earth position: (%x, %x, %x)\n",
           floatToBits(posEarthX),
           floatToBits(posEarthY),
           floatToBits(posEarthZ));
    
    return 0;
}
```

## Security features

The accelerator implements several security measures:

1. **Lock mechanism**: Prevents unauthorized access
2. **Bit flipping**: Data is XORed with random masks to prevent eavesdropping
3. **Keep alive**: Ensures continued authorization during long operations

## Notes

- The lock key (0x12345 in examples) should be unique per session
- All floating-point values must be converted to 32-bit representations
- The accelerator supports a configurable number of body processing elements (BPEs)
- Position and velocity data should be scaled appropriately for the accelerator's computational range