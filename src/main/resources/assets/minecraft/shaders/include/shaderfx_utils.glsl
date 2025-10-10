#define PI 3.14159265

mat2 mat2_rotate_z(float radians) {
    float s = sin(radians);
    float c = cos(radians);
    return mat2(
        c, -s,
        s,  c
    );
}

int hash(int x) {
    x ^= (x << 13);
    x ^= (x >> 17);
    x ^= (x << 5);
    x ^= (x >> 9);
    return x;
}





