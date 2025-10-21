#define NOISE_CUTOFF 0.66
#define SPEED 10.0

float rand(vec2 n) {
    return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 ip = floor(p);
    vec2 u = fract(p);
    u = u * u * (3.0 - 2.0 * u);

    float res = mix(
    mix(rand(ip), rand(ip + vec2(1.0,0.0)), u.x),
    mix(rand(ip + vec2(0.0,1.0)), rand(ip + vec2(1.0,1.0)), u.x),
    u.y
    );
    return res * res;
}

float samplePoint(vec2 p) {
    float n = noise(p);
    n *= n * n * n;
    n = step(NOISE_CUTOFF, n);
    return n;
}

float layer(vec2 fragCoord, float scale, float iTime) {
    float n = 0.0;
    vec2 squared = floor((fragCoord + vec2(0.0, SPEED * iTime)) / scale);
    n += samplePoint(squared);
    n += 0.5 * samplePoint(squared - vec2(0.0, 1.0));
    n += 0.25 * samplePoint(squared - vec2(0.0, 2.0));
    n += 0.125 * samplePoint(squared - vec2(0.0, 3.0));
    return n;
}

mat2 rotate2d(float _angle) {
    float c = cos(_angle);
    float s = sin(_angle);
    return mat2(c, -s, s, c);
}

vec3 decodeSRGB(vec3 screenRGB) {
    vec3 a = screenRGB / 12.92;
    vec3 b = pow((screenRGB + 0.055) / 1.055, vec3(2.4));
    vec3 c = step(vec3(0.04045), screenRGB);
    return mix(a, b, c);
}
