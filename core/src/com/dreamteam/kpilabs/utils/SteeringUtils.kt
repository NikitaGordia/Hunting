package com.dreamteam.kpilabs.utils

fun computeSteeringForce(
        position: Vec,
        desirePosition: Vec,
        velocity: Vec,
        maxSpeed: Float,
        maxForce: Float
): Vec {
    var desired = (desirePosition - position)
    desired = desired.norm() * maxSpeed

    return (desired - velocity).limit(maxForce)
}