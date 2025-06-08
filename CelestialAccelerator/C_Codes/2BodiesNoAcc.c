#include <stdio.h>
#include <stdint.h>
#define RISCV FALSE

/*
Simulation with 2 bodies (Sun and Earth), without using the accelerator.
This program takes 102249 cycles.
*/

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

float abs(float x) {
    return (x < 0) ? -x : x;
}

// Type-safe float to uint32_t punning
uint32_t floatToBits(float f) {
    union {
        float f;
        uint32_t u;
    } pun;
    pun.f = f;
    return pun.u;
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

    const float threehalfs = 1.5F;
    y = y * (threehalfs - (x2 * y * y)); // First iteration

    for (int j = 0; j < numRefines; j++) {
        y = y * (threehalfs - (x2 * y * y)); // Newton-Raphson refinement
    }

    return y;
}


static inline uint64_t read_cycle() {
    #if RISCV
    uint64_t cycle;
    asm volatile ("rdcycle %0" : "=r"(cycle));
    return cycle;
    #else
    return 0;
    #endif
}

#pragma endregion

#pragma region Planets / moons

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

#pragma endregion

#pragma region Simulation

void UpdatePosition(struct CelestialBody *body, float dt)
{
    body->x += body->vx * dt;
    body->y += body->vy * dt;
    body->z += body->vz * dt;
}

void UpdateVelocity(struct CelestialBody *target, struct CelestialBody *source, float dt)
{
    float G = 6.67430e-11f;
    float dx = source->x - target->x;
    float dy = source->y - target->y;
    float dz = source->z - target->z;

    float distSq = dx * dx + dy * dy + dz * dz;
    float invDist = fastInvSqrt(distSq, 2);
    float invDistCube = invDist * invDist * invDist;

    float acc_multiplier = G * source->mass * invDistCube * dt;

    target->vx += acc_multiplier * dx;
    target->vy += acc_multiplier * dy;
    target->vz += acc_multiplier * dz;
}

void RunSimulation(struct CelestialBody *bodies, float dt, int numIterations, int numBodies)
{
    for (int i = 0; i < numIterations; i++)
    {
        for (int j = 0; j < numBodies; j++)
        {
            UpdatePosition(&bodies[j], dt);
        }

        for (int j = 0; j < numBodies; j++)
        {
            for (int k = 0; k < numBodies; k++)
            {
                if (j != k)
                {
                    UpdateVelocity(&bodies[j], &bodies[k], dt);
                }
            }
        }
    }
}

#pragma endregion

#pragma region Main Simulation

void PrintPosition(struct CelestialBody *earth) {
    printf("Position (float bits): (%x, %x, %x)\n",
        floatToBits(earth->x),
        floatToBits(earth->y),
        floatToBits(earth->z));
}

int CheckPositionError(float startPos, float currentPos, const char *axis) {
    float delta = abs(currentPos - startPos);
    if (delta > 0.005f * abs(startPos)) {
        printf("Error: %s position out of bounds: %f\n", axis, delta);
        return 1;
    }
    return 0;
}

int main(void)
{
    struct CelestialBody bodies[2];
    setSun(&bodies[0]);
    setEarth(&bodies[1]);

    float dt = 60.0f * 60.0f * 24.0f; 
    int numIterations = 365;

    float startPosX = bodies[1].x;
    float startPosY = bodies[1].y;
    float startPosZ = bodies[1].z;

    printf("Initial positions:\n");
    PrintPosition(&bodies[1]);

    uint64_t start_cycles = read_cycle();
    RunSimulation(bodies, dt, numIterations, 2);
    uint64_t end_cycles = read_cycle();

    printf("Final positions:\n");
    PrintPosition(&bodies[1]);

    if (CheckPositionError(startPosX, bodies[1].x, "X") ||
        CheckPositionError(startPosY, bodies[1].y, "Y") ||
        CheckPositionError(startPosZ, bodies[1].z, "Z")) {
        printf("Simulation failed.\n");
        return 1;
    }

    printf("Total clock cycles: %lu\n", (end_cycles - start_cycles));

    return 0;
}

#pragma endregion
