#define RISCV 1 // 1 for production, 0 to disable RISC-V specific code when testing on another, faster, platform.

#define NUM_BODIES 3

#if RISCV
#include "mmio.h"
#endif

#include <stdio.h>
#include <stdint.h>

/*
Simulation with and withhout using the accelerator.
2 bodies : 
No accelerator : 134 setup cycles, 114056 run cycles.
With accelerator : 1098 setup cycles, 18802 run cycles.
88.56 % speedup
4 bodies : 
No accelerator : 743 setup cycles, 594328 run cycles.
With accelerator : 1098 setup cycles, 34 948 run cycles.
464.8 % speedup
6 bodies : 
No accelerator : 3192 setup cycles, 1432360 run cycles.
With accelerator : 1440 setup cycles, 51 692 run cycles.
1120% speedup
8 bodies : 
No accelerator : 4634 setup cycles, 2618545 run cycles.
With accelerator : 1706 setup cycles, 68 436 run cycles.
2048.8 % speedup
*/

#pragma region MMIO registers

#define CELESTIAL_DIN       0x4000
#define CELESTIAL_LOCKED    0x4010
#define CELESTIAL_DOUT         0x4020
#define CELESTIAL_ITERATION    0x4030

#pragma endregion

#pragma region Accelerator command codes

#define CMD_IDLE                0
#define CMD_LOCK                1
#define CMD_UNLOCK              2
#define CMD_SET_X               3
#define CMD_SET_Y               4
#define CMD_SET_Z               5
#define CMD_SET_MASS            6
#define CMD_SET_SIZE            7
#define CMD_SET_DT              8
#define CMD_FORWARD_POSITION    9
#define CMD_FORWARD_VELOCITY    10
#define CMD_STOP_ON_COLLISION   11
#define CMD_START_SIMULATION    12
#define CMD_STOP_SIMULATION     13
#define CMD_SET_MAX_ITERATIONS  14
#define CMD_SET_ACTIVE_BPES     15
#define CMD_KEEP_ALIVE          16
#define CMD_SET_TARGET          17
#define CMD_OUTPUT_X            18
#define CMD_OUTPUT_Y            19
#define CMD_OUTPUT_Z            20
#define CMD_OUTPUT_DX           21
#define CMD_OUTPUT_DY           22
#define CMD_OUTPUT_DZ           23
#define CMD_OUTPUT_COLLISION_ID 24

#pragma endregion

#pragma region Structs

struct CelestialBody
{
    float x;
    float y;
    float z;
    float vx;
    float vy;
    float vz;
    float mass;
    float size;
};

#pragma endregion

#pragma region Utilities

// Type-safe float to uint32_t punning
uint32_t floatToBits(float f) {
    union {
        float f;
        uint32_t u;
    } pun;
    pun.f = f;
    return pun.u;
}

float bitsToFloat(uint32_t bits)
{
    union {
        float f;
        uint32_t u;
    } pun;
    pun.u = bits;
    return pun.f;
}

float abs(float x) {
    return (x < 0) ? -x : x;
}

float fastInvSqrt(float x, int numRefines)
{
    float x2 = x * 0.5F;
    float y = x;

    union {
        float f;
        uint32_t i;
    } conv;

    conv.f = y;
    conv.i = 0x5f3759df - (conv.i >> 1); // Magic constant approximation
    y = conv.f;

    const float threeHalfs = 1.5F;
    y = y * (threeHalfs - (x2 * y * y)); // First iteration

    for (int j = 0; j < numRefines; j++) {
        y = y * (threeHalfs - (x2 * y * y)); // Newton-Raphson refinement
    }

    return y;
}

void printPosition(struct CelestialBody *earth) {
    printf("Position (float bits): (%x, %x, %x)\n",
        floatToBits(earth->x),
        floatToBits(earth->y),
        floatToBits(earth->z));
}

int checkPositionError(float startPos, float currentPos, const char *axis) {
    float delta = abs(currentPos - startPos);
    // More tolereance than for 2 bodies, as the moon will alter earth's position
    if (delta > 0.085f * abs(startPos)) {
        printf("Error: %s position out of bounds: %f\n", axis, delta);
        printf("Start position: %x, Current position: %x\n", floatToBits(startPos), floatToBits(currentPos));
        return 1;
    }
    return 0;
}


int checkPositionErrorCustomTol(float startPos, float currentPos, const char *axis, float tolerance) {
    float delta = abs(currentPos - startPos);
    // More tolereance than for 2 bodies, as the moon will alter earth's position
    if (delta > tolerance * abs(startPos)) {
        printf("Error: %s position out of bounds: %f\n", axis, delta);
        printf("Start position: %x, Current position: %x\n", floatToBits(startPos), floatToBits(currentPos));
        return 1;
    }
    return 0;
}

float earthMoonDistance(struct CelestialBody *earth, struct CelestialBody *moon) {
    float dx = moon->x - earth->x;
    float dy = moon->y - earth->y;
    float dz = moon->z - earth->z;
    return 1/fastInvSqrt(dx * dx + dy * dy + dz * dz, 2);
}


static inline uint64_t readCycle() {
    #if RISCV
    uint64_t cycle;
    asm volatile ("rdcycle %0" : "=r"(cycle));
    return cycle;
    #else
    return 0;
    #endif
}

#pragma endregion

#if RISCV
#pragma region accelerator functions

#pragma region MMIO functions

void sendPacket(uint32_t data, uint8_t cmd, uint32_t lock)
{
    // Mask lock to ensure it's only 27 bits
    lock = lock & 0x07FFFFFF;
    
    // Combine into a single 64-bit value
    uint64_t packet = ((uint64_t)cmd << 59) | ((uint64_t)lock << 32) | data;
    // uint32_t high = (uint32_t)(packet >> 32);
    // uint32_t low = (uint32_t)(packet & 0xFFFFFFFF);
    // printf("Sending packet: 0x%08X%08X\n", high, low);
    // printf("Command: %d\n", cmd);

    // Write the packet to the accelerator
    reg_write64(CELESTIAL_DIN, packet);
}

uint32_t read_dOut()
{
    uint32_t data = reg_read32(CELESTIAL_DOUT);
    // printf("Reading dOut: 0x%08X\n", data);
    return data;
}

#pragma endregion

#pragma region Lock-related

int lockAccelerator(uint32_t lock)
{
    // If device is already locked, return false
    if (reg_read8(CELESTIAL_LOCKED) != 0x0) {
        return 1;
    }
    // Write the lock value to the accelerator
    uint32_t data = 0x0;
    uint8_t cmd = 0x1;
    sendPacket(data, cmd, lock);
    return 0;
}

void unlockAccelerator(uint32_t lock)
{
    // Unlock the accelerator
    uint32_t data = 0x0;
    uint8_t cmd = CMD_UNLOCK;
    sendPacket(data, cmd, lock);
}

#pragma endregion

# pragma region Simulation control

void sendIdle(uint32_t lock)
{
    // Send the idle command to the accelerator
    uint32_t data = 0x0;
    uint8_t cmd = CMD_IDLE;
    sendPacket(data, cmd, lock);
}

// A function to send either X,Y or Z
// Type is 0 for X, 1 for Y, and 2 for Z
void sendOnePos(int type, float coo, uint32_t lock)
{
    uint32_t data = floatToBits(coo);
    uint8_t cmd = CMD_SET_X + type; // Small hack to be faster
    sendPacket(data, cmd, lock);
}

void setMass(float mass, uint32_t lock)
{
    // Send the mass to the accelerator
    uint32_t data = floatToBits(mass);
    uint8_t cmd = CMD_SET_MASS;
    sendPacket(data, cmd, lock);
}

void setSize(float size, uint32_t lock)
{
    // Send the size to the accelerator
    uint32_t data = floatToBits(size);
    uint8_t cmd = CMD_SET_SIZE;
    sendPacket(data, cmd, lock);
}

void sendPosition(float x, float y, float z, int targetBPE, uint32_t lock)
{
    // Send the position to the accelerator. Also forwards the size and mass if previously set
    sendOnePos(0, x, lock);
    sendOnePos(1, y, lock);
    sendOnePos(2, z, lock);
    // Send the target BPE
    uint32_t data = targetBPE;
    uint8_t cmd = CMD_FORWARD_POSITION;
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

void setTimeStep(float dt, uint32_t lock)
{
    // Send the time step to the accelerator
    uint32_t data = floatToBits(dt);
    uint8_t cmd = CMD_SET_DT;
    sendPacket(data, cmd, lock);
}

void setMaxIterations(uint32_t maxIterations, uint32_t lock)
{
    // Send the maximum iterations to the accelerator
    uint32_t data = maxIterations;
    uint8_t cmd = CMD_SET_MAX_ITERATIONS;
    sendPacket(data, cmd, lock);
}
void setActiveBPEs(uint32_t activeBPEs, uint32_t lock)
{
    // Send the active BPEs to the accelerator
    uint32_t data = activeBPEs;
    uint8_t cmd = CMD_SET_ACTIVE_BPES;
    sendPacket(data, cmd, lock);
}
void setStopOnCollision(uint32_t stopOnCollision, uint32_t lock)
{
    // Send the stop on collision flag to the accelerator
    uint32_t data = stopOnCollision;
    uint8_t cmd = CMD_STOP_ON_COLLISION;
    sendPacket(data, cmd, lock);
}

void startimulation(uint32_t lock)
{
    // Start the simulation
    uint32_t data = 0x0;
    uint8_t cmd = CMD_START_SIMULATION;
    sendPacket(data, cmd, lock);
}

void sendKeepAlive( uint32_t lock)
{
    // Send the keep alive flag to the accelerator
    uint32_t data = 0x0;
    uint8_t cmd = CMD_KEEP_ALIVE;
    sendPacket(data, cmd, lock);
}

#pragma endregion

#pragma region Output functions

void setTarget(uint32_t target, uint32_t lock)
{
    // Send the target to the accelerator
    uint32_t data = target;
    uint8_t cmd = CMD_SET_TARGET;
    sendPacket(data, cmd, lock);
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

uint32_t getIteration(uint32_t lock)
{
    uint32_t iteration = reg_read32(CELESTIAL_ITERATION);
    return iteration;
}

#pragma endregion

#pragma endregion
#endif 

#pragma region Planets / moons / stars

void setSun(struct CelestialBody *sun)
{
    sun->x = 0.0f;
    sun->y = 0.0f;
    sun->z = 0.0f;
    sun->vx = 0.0f;
    sun->vy = 0.0f;
    sun->vz = 0.0f;
    sun->mass = 1.989e30f;
    sun->size = 1.0f;
}

void setEarth(struct CelestialBody *earth)
{
    earth->x = -9.34039169997118860e+07f * 1e3f;
    earth->y = -1.18811312084356889e+08f * 1e3f;
    earth->z = 7.94186043863161467e+03f * 1e3f;

    earth->vx = 2.29385493455156606e+01f * 1e3f;
    earth->vy = -1.85184623747619383e+01f * 1e3f;
    earth->vz = 1.33196768834853430e-03f * 1e3f;

    earth->mass = 5.972e24f;
    earth->size = 6371.0f;
}

void setMoon(struct CelestialBody *moon)
{
    moon->x = -9.36559689590353370e+07f * 1e3f;
    moon->y = -1.19127336121712506e+08f * 1e3f;
    moon->z = -2.17642638604252134e+04f * 1e3f;
    moon->vx = 2.37093629959455896e+01f * 1e3f;
    moon->vy = -1.91124440864929994e+01f * 1e3f;
    moon->vz = -4.60429620468332246e-02f * 1e3f;

    moon->mass = 7.34767309e22f;
    moon->size = 1737.4f;
}

void setVenus(struct CelestialBody *venus)
{
    venus->x = -1.25794823898377996e+07f * 1e3f; // Distance from the Sun in meters
    venus->y = -1.07962059001944438e+08f * 1e3f; // Distance from the Sun in meters
    venus->z = -7.57368338207921246e+05f * 1e3f; // Distance from the Sun in meters
    venus->vx = 3.45628783498460805e+01f * 1e3f; // Orbital speed of the Venus in km/s
    venus->vy = -4.19309824420221489e+00f * 1e3f; // Orbital speed of the Venus in km/s
    venus->vz = -2.05133668909497757e+00f * 1e3f; // Orbital speed of the Venus in km/s

    venus->mass = 4.8675e24f;
    venus->size = 6051.8f;
}

void setRandomPlanet(struct CelestialBody *planet)
{
    planet->x = (float)(rand() % 1000000) / 1000000.0f * 1e8f;
    planet->y = (float)(rand() % 1000000) / 1000000.0f * 1e8f;
    planet->z = (float)(rand() % 1000000) / 1000000.0f * 1e8f;
    planet->vx = (float)(rand() % 1000000) / 1000000.0f * 1e8f;
    planet->vy = (float)(rand() % 1000000) / 1000000.0f * 1e8f;
    planet->vz = (float)(rand() % 1000000) / 1000000.0f * 1e8f;
    planet->mass = (float)(rand() % 1000000) / 1000000.0f * 1e8f;
    planet->size = 1.0f;
}

#pragma endregion

#pragma region Simulation without acceleration

void updatePosition(struct CelestialBody *body, float dt)
{
    body->x += body->vx * dt;
    body->y += body->vy * dt;
    body->z += body->vz * dt;
}

void updateVelocity(struct CelestialBody *target, struct CelestialBody *source, float dt)
{
    float G = 6.67430e-11f;
    float dx = source->x - target->x;
    float dy = source->y - target->y;
    float dz = source->z - target->z;

    float distSq = dx * dx + dy * dy + dz * dz;
    float invDist = fastInvSqrt(distSq, 3);
    float invDistCube = invDist * invDist * invDist;

    float acc_multiplier = G * source->mass * invDistCube * dt;

    target->vx += acc_multiplier * dx;
    target->vy += acc_multiplier * dy;
    target->vz += acc_multiplier * dz;
}

void runSimulationNoAcc(struct CelestialBody *bodies, float dt, int numIterations, int numBodies)
{
    for (int i = 0; i < numIterations; i++)
    {
        for (int j = 0; j < numBodies; j++)
        {
            updatePosition(&bodies[j], dt);
        }

        for (int j = 0; j < numBodies; j++)
        {
            for (int k = 0; k < numBodies; k++)
            {
                if (j != k)
                {
                    updateVelocity(&bodies[j], &bodies[k], dt);
                }
            }
        }
    }
}

void simulateNoAcc(struct CelestialBody *bodies)
{
    uint64_t start_cycles = readCycle();

    setSun(&bodies[0]);
    setEarth(&bodies[1]);
    #if NUM_BODIES > 2
    setMoon(&bodies[2]);
    setVenus(&bodies[3]);
    #endif
    #if NUM_BODIES > 4
    // We don't care about the other planets, only to measure the clock cycles
    for (int i = 4; i < NUM_BODIES; i++) {
        setRandomPlanet(&bodies[i]);
    }
    #endif

    float dt = 60.0f * 60.0f * 24.0f; 
    int numIterations = 365;
    uint64_t end_cycles = readCycle();
    printf("Total setup clock cycles - no acc: %lu\n", (end_cycles - start_cycles));

    float startPosX = bodies[1].x;
    float startPosY = bodies[1].y;
    float startPosZ = bodies[1].z;

    printf("Initial positions:\n");
    printPosition(&bodies[1]);
    #if NUM_BODIES > 2
    float initearthMoonDistance = earthMoonDistance(&bodies[1], &bodies[2]);
    printf("Earth-Moon distance: %f\n", initearthMoonDistance);
    #endif
    

    start_cycles = readCycle();
    runSimulationNoAcc(bodies, dt, numIterations, NUM_BODIES);
    end_cycles = readCycle();

    printf("Final positions:\n");
    printPosition(&bodies[1]);
    #if NUM_BODIES > 2
    float finalearthMoonDistance = earthMoonDistance(&bodies[1], &bodies[2]);
    printf("Earth-Moon distance: %f\n", finalearthMoonDistance);
    #endif

    // Check that the Earth position is within more or less the same position, and that the moon-earth distance is the more or less the same
    if (checkPositionError(startPosX, bodies[1].x, "X") ||
        checkPositionError(startPosY, bodies[1].y, "Y") ||
        checkPositionError(startPosZ, bodies[1].z, "Z") 
#if NUM_BODIES > 2
    || checkPositionError(finalearthMoonDistance, initearthMoonDistance, "Earth-Moon distance")
#endif
    )
    {
        printf("Simulation failed.\n");
    }

    printf("Total run clock cycles - no acc: %lu\n", (end_cycles - start_cycles));
}

#pragma endregion


#if RISCV
#pragma region Simulation with acceleration

float scaledMass(float mass)
{
    float G = 6.67430e-11f;
    return mass * G * 1e-18f;
}

float scaledSize(float size)
{
    return size * 1e-6f;
}
float scaledDistance(float distance)
{
    return distance * 1e-6f;
}

void setupCelestialBody(struct CelestialBody *body, uint32_t targetBPE, uint32_t lock)
{
    // Set the mass and size
    setMass(scaledMass(body->mass), lock);
    setSize(scaledSize(body->size), lock);

    // Send the position
    sendPosition(scaledDistance(body->x), scaledDistance(body->y), scaledDistance(body->z), targetBPE, lock);

    // Send the velocity
    sendVelocity(scaledDistance(body->vx), scaledDistance(body->vy), scaledDistance(body->vz), targetBPE, lock);
}

void runSimlationAcc(uint32_t lock, int maxWait)
{
    startimulation(lock);
    sendIdle(lock); // Otherwise the accelerator is stuck on startSimulation and will start it again in case it finishes before the C code notices
    for (int i = 0; i < NUM_BODIES-1; i++) { // Ensure we don't check if the iteration is 0 before the first iteration 
        // is completed
        sendIdle(lock);
    }

    int currentWait = 0;
    while (currentWait < maxWait) {       
        if (getIteration(lock) == 0) {
            // Simulation finished
            break;
        }
        currentWait++;
        sendKeepAlive(lock);
        sendIdle(lock);
    }   
}

void simulateAcc(struct CelestialBody *bodiesWithoutAcc)
{
    // bodiesWithoutAcc are used to compare the results with the accelerator
    struct CelestialBody bodies[NUM_BODIES];
    setSun(&bodies[0]); 
    setEarth(&bodies[1]);
#if NUM_BODIES > 2
    setMoon(&bodies[2]);
    setVenus(&bodies[3]);
#endif
#if NUM_BODIES > 4
    // We don't care about the other planets, only to measure the clock cycles
    for (int i = 4; i < NUM_BODIES; i++) {
        setRandomPlanet(&bodies[i]);
    }
#endif

    float dt = 60.0f * 60.0f * 24.0f;
    int numIterations = 365;
    // int numIterations = 30;
    int maxWait = numIterations * 10000;
    uint32_t lock = 0x12345; 
    uint32_t activeBPEs = NUM_BODIES;

    uint64_t start_cycles = readCycle();
    // Lock the accelerator
    lockAccelerator(lock);
    // Set the time step
    setTimeStep(dt, lock);
    // Set the maximum iterations
    setMaxIterations(numIterations, lock);
    // Set the active BPEs
    setActiveBPEs(activeBPEs, lock);
    // Send the planets to the accelerator
    for (int i = 0; i < NUM_BODIES; i++) {
        setupCelestialBody(&bodies[i], i, lock);
    }

    uint64_t end_cycles = readCycle();
    printf("Time to setup the accelerator: %lu\n", (end_cycles - start_cycles));
    
    printf("Start positions in bits :\n");
    setTarget(1, lock);

    int errorSum = 0;
    
    float posEarthX = getX(lock);
    float posEarthY = getY(lock);
    float posEarthZ = getZ(lock);
    printf("Earth: (%x, %x, %x)\n",
        floatToBits(posEarthX),
        floatToBits(posEarthY),
        floatToBits(posEarthZ));

#if NUM_BODIES > 2
    // Set the target to the Moon
    setTarget(2, lock);
    float posMoonX = getX(lock);
    float posMoonY = getY(lock);
    float posMoonZ = getZ(lock);
    printf("Moon: (%x, %x, %x)\n",
        floatToBits(posMoonX),
        floatToBits(posMoonY),
        floatToBits(posMoonZ));
#endif

#if NUM_BODIES > 3
    // Set the target to the Venus
    setTarget(3, lock);
    float posVenusX = getX(lock);
    float posVenusY = getY(lock);
    float posVenusZ = getZ(lock);
    printf("Venus: (%x, %x, %x)\n",
        floatToBits(posVenusX),
        floatToBits(posVenusY),
        floatToBits(posVenusZ));
#endif

    start_cycles = readCycle();
    runSimlationAcc(lock, maxWait);
    end_cycles = readCycle();
    printf("Approximate time to run the simulation - use GKTwave for more precision: %lu\n", (end_cycles - start_cycles));

#if NUM_BODIES < 5 // Don't check if used random planets

    errorSum += checkPositionError(posEarthX, scaledDistance(bodiesWithoutAcc[1].x), "Earth X - acc vs no acc");
    errorSum += checkPositionError(posEarthY, scaledDistance(bodiesWithoutAcc[1].y), "Earth Y - acc vs no acc");
    errorSum += checkPositionError(posEarthZ, scaledDistance(bodiesWithoutAcc[1].z), "Earth Z - acc vs no acc");
    #if NUM_BODIES > 2
    errorSum += checkPositionError(posMoonX, scaledDistance(bodiesWithoutAcc[2].x), "Moon X - acc vs no acc");
    errorSum += checkPositionError(posMoonY, scaledDistance(bodiesWithoutAcc[2].y), "Moon Y - acc vs no acc");
     // Slightly more tolerance as low number at the start, thus variation is higher in %age
    errorSum += checkPositionErrorCustomTol(posMoonZ, scaledDistance(bodiesWithoutAcc[2].z), "Moon Z - acc vs no acc", 0.3f);
    #endif
    if (errorSum > 0)
    {
        printf("Simulation failed.\n");
    }
    else
    {
        printf("Simulation with accelerator succeeded !\n");
    }

#endif

    // Print the final positions
    printf("Final positions in bits:\n");
    setTarget(1, lock);
    posEarthX = getX(lock);
    posEarthY = getY(lock);
    posEarthZ = getZ(lock);
    printf("Earth: (%x, %x, %x)\n",
        floatToBits(posEarthX),
        floatToBits(posEarthY),
        floatToBits(posEarthZ));

}
#pragma endregion
#endif

#pragma region Main simulation

int main(void)
{
    printf("Starting\n");
    struct CelestialBody bodies[NUM_BODIES];
    simulateNoAcc(bodies);

    # if RISCV
    simulateAcc(bodies);
    # endif
    printf("Done\n");
    return 0;
}

#pragma endregion
