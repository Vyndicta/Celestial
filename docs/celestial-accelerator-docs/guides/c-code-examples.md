# C code examples

This page provides a few examples of how to use the accelerator, using C codes.

## Locking the accelerator

This is the first step to using the accelerator. It ensures that another user can't temper with it, and can be done with the following code:

```c
#include "mmio.h"
#include <stdio.h>
#include <stdint.h>

#define CELESTIAL_DIN 0x4000
#define CELESTIAL_LOCKED 0x4010

#define CMD_IDLE 0
#define CMD_LOCK 1

void sendPacket(uint32_t data, uint8_t cmd, uint32_t lock)
{
    // Mask packet to ensure it's only 27 bits
    lock = lock & 0x07FFFFFF;
    uint64_t packet = ((uint64_t)cmd << 59) | ((uint64_t)lock << 32) | data;
    reg_write64(CELESTIAL_DIN, packet);
}

int lockAccelerator(uint32_t lock)
{
    // If device is already locked, return false
    if (reg_read8(CELESTIAL_LOCKED) != 0x0) {
        return 1;
    }
    uint32_t data = 0x0;
    uint8_t cmd = 0x1;
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
    return 0;
}
```

To send a planet's position:


```c
#include "mmio.h"
#include <stdio.h>
#include <stdint.h>

#define CELESTIAL_DIN       0x4000
#define CELESTIAL_LOCKED    0x4010

#define CMD_SET_X               3
#define CMD_SET_Y               4
#define CMD_SET_Z               5
#define CMD_SET_MASS            6
#define CMD_SET_SIZE            7
#define CMD_SET_DT              8
#define CMD_FORWARD_POSITION    9
#define CMD_FORWARD_VELOCITY    10

void sendPacket(uint32_t data, uint8_t cmd, uint32_t lock)
{
// Mask packet to ensure it's only 27 bits
lock = lock & 0x07FFFFFF;
uint64_t packet = ((uint64_t)cmd << 59) | ((uint64_t)lock << 32) | data;
reg_write64(CELESTIAL_DIN, packet);
}

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
uint8_t cmd = CMD_SET_X + type; // Small hack for lighter code
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

void sendVelocity(float vx, float vy, float vz, int targetBPE, uint32_t lock)
{
// Send the velocity to the accelerator.
sendOnePos(0, vx, lock);
sendOnePos(1, vy, lock);
sendOnePos(2, vz, lock);
// Send the target BPE
uint32_t data = targetBPE;
uint8_t cmd = CMD_FORWARD_VELOCITY;
sendPacket(data, cmd, lock);
}

void sendPosition(float x, float y, float z, int targetBPE, uint32_t lock)
{
sendOnePos(0, x, lock);
sendOnePos(1, y, lock);
sendOnePos(2, z, lock);

uint32_t data = targetBPE;
uint8_t cmd = CMD_FORWARD_POSITION;
sendPacket(data, cmd, lock);
}

int main(void)
{
uint32_t lock = 0x12345;
// Assuming the previous code has locked the accelerator
// In practice, this requires scaling as the range of the fastNegThreeHalf module is limited
float mass = 5.972e24f; // Mass of Earth in kg
float size = 1.0f; // Size of Earth
float x = -9.34039169997118860e+07f * 1e3f; // X position of Earth
float y = -1.18811312084356889e+08f * 1e3f; // Y position of Earth
float z = 7.94186043863161467e+03f * 1e3f; // Z position of Earth
float vx = 2.29385493455156606e+01f * 1e3f; // X velocity of Earth
float vy = -1.85184623747619383e+01f * 1e3f; // Y velocity of Earth
float vz = 1.33196768834853430e-03f * 1e3f; // Z velocity of Earth
setMass(mass, lock);
setSize(size, lock);
sendPosition(x, y, z, 1, lock); // Send position to target BPE 1
sendVelocity(vx, vy, vz, 1, lock); // Send velocity to target BPE 1
return 0;
}
```

And to run the simulation:


```c
#include "mmio.h"
#include <stdio.h>
#include <stdint.h>

#define CELESTIAL_DIN       0x4000
#define CELESTIAL_LOCKED    0x4010
#define CELESTIAL_DOUT         0x4020
#define CELESTIAL_ITERATION    0x4030

#define CMD_IDLE                0
#define CMD_SET_DT              8
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

void sendPacket(uint32_t data, uint8_t cmd, uint32_t lock)
{
	// Mask packet to ensure it's only 27 bits
	lock = lock & 0x07FFFFFF;
	uint64_t packet = ((uint64_t)cmd << 59) | ((uint64_t)lock << 32) | data;
	reg_write64(CELESTIAL_DIN, packet);
}

float getX(uint32_t lock)
{
	// Get the X position from the accelerator
	uint32_t data = rand(); // Generate a random bit flip mask
	uint8_t cmd = CMD_OUTPUT_X;
	sendPacket(data, cmd, lock);
	uint32_t output = read_dOut();
	// Unflip the bits
	output ^= data;
	float x = bitsToFloat(output);
	return x;    
}

float getY(uint32_t lock)
{
	// Get the Y position from the accelerator
	uint32_t data = rand();
	uint8_t cmd = CMD_OUTPUT_Y;
	sendPacket(data, cmd, lock);
	uint32_t output = read_dOut();
	output ^= data;
	float y = bitsToFloat(output);
	return y;    
}

float getZ(uint32_t lock)
{
	// Get the Z position from the accelerator
	uint32_t data = rand();
	uint8_t cmd = CMD_OUTPUT_Z;
	sendPacket(data, cmd, lock);
	uint32_t output = read_dOut();
	output ^= data;
	float z = bitsToFloat(output);
	return z;    
}

float getDX(uint32_t lock)
{
	// Get the DX position from the accelerator
	uint32_t data = 0x0;
	uint8_t cmd = CMD_OUTPUT_DX;
	sendPacket(data, cmd, lock);
	uint32_t output = read_dOut();
	output ^= data;
	float dx = bitsToFloat(output);
	return dx;    
}

float getDY(uint32_t lock)
{
	// Get the DY position from the accelerator
	uint32_t data = rand();
	uint8_t cmd = CMD_OUTPUT_DY;
	sendPacket(data, cmd, lock);
	uint32_t output = read_dOut();
	output ^= data;
	float dy = bitsToFloat(output);
	return dy;    
}

float getDZ(uint32_t lock)
{
	// Get the DZ position from the accelerator
	uint32_t data = rand();
	uint8_t cmd = CMD_OUTPUT_DZ;
	sendPacket(data, cmd, lock);
	uint32_t output = read_dOut();
	output ^= data;
	float dz = bitsToFloat(output);
	return dz;    
}

void setTarget(uint32_t target, uint32_t lock)
{
	// Send the target to the accelerator
	uint32_t data = target;
	uint8_t cmd = CMD_SET_TARGET;
	sendPacket(data, cmd, lock);
}

void setTimeStep(float dt, uint32_t lock)
{
	// Send the time step to the accelerator
	uint32_t data = floatToBits(dt);
	uint8_t cmd = CMD_SET_DT;
	sendPacket(data, cmd, lock);
}

void sendIdle(uint32_t lock)
{
	// Send the idle command to the accelerator
	uint32_t data = 0x0;
	uint8_t cmd = CMD_IDLE;
	sendPacket(data, cmd, lock);
}

void setActiveBPEs(uint32_t activeBPEs, uint32_t lock)
{
	// Send the active BPEs to the accelerator
	uint32_t data = activeBPEs;
	uint8_t cmd = CMD_SET_ACTIVE_BPES;
	sendPacket(data, cmd, lock);
}

void runSimlationAcc(uint32_t lock, int maxWait)
{
	startimulation(lock);
	sendIdle(lock); // Otherwise the accelerator is stuck on startSimulation and will start it again in case it finishes before the C code notices
	int currentWait = 0;
	uint32_t lastIteration = 0;
	
	while (currentWait < maxWait) {
		currentWait++;
		uint32_t currentIteration = getIteration(lock);
		if (currentIteration != lastIteration) {
			lastIteration = currentIteration;
		}
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
	// Assuming the previous codes have locked the accelerator and sent 2 planets / stars
	
	float dt = 60.0f * 60.0f * 24.0f;
	int numIterations = 365;
	int maxWait = numIterations * 10;
	uint32_t activeBPEs = 2;
	
	setTimeStep(dt, lock);
	setMaxIterations(numIterations, lock);
	setActiveBPEs(2, lock);
	runSimlationAcc(lock, maxWait);
	
	setTarget(1, lock);
	int errorSum = 0;
	
	float posEarthX = getX(lock);
	float posEarthY = getY(lock);
	float posEarthZ = getZ(lock);
	printf("Earth: (%x, %x, %x)\n",
	floatToBits(posEarthX),
	floatToBits(posEarthY),
	floatToBits(posEarthZ));
	
	return 0;
}
```
