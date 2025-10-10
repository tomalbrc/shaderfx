void main() {
    ivec2 grid = ivec2(gl_FragCoord.xy / 32) * 32;
    color = (abs(hash(grid.x ^ hash(grid.y)) % 0x100) + 10 < int(vertexColor.a * (length(grid / ScreenSize.xy - 0.5) * 2 + 1) * 0x100))
    ? vec4(vertexColor.rgb, 1)
    : vec4(0);
}