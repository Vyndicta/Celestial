#include "mmio.h"
#include <stdio.h>
#include <stdint.h>

#define CELESTIAL_DIN       0x4000
#define CELESTIAL_LOCKED    0x4010


#define CMD_IDLE                0
#define CMD_LOCK                1

void sendPacket(uint32_t data, uint8_t cmd, uint32_t lock)
{
    // Mask lock to ensure it's only 27 bits
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