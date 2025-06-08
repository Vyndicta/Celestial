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
    // Mask lock to ensure it's only 27 bits
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