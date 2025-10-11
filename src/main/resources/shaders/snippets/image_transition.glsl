void fragment () {
    float mask = texture(Sampler0, texCoord0).r;
    color = vec4(vec3(0.), step(mask, vertexColor.a));
}