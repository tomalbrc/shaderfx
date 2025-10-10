void main() {
    color = vec4(0, 0, 0, clamp(length(centerUV * vec2(0.8, 0.5 / (1 - vertexColor.a))) - 0.6, 0, 1));
}