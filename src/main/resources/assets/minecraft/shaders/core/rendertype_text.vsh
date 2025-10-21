#version 150
#moj_import <fog.glsl>

const vec2[4] corners = vec2[4](vec2(0), vec2(0, 1), vec2(1), vec2(1, 0));

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

uniform float GameTime;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

uniform vec2 ScreenSize;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

// effect
flat out int effectId;
// animated emoji
flat out int frames;
flat out int fps;
flat out float frameheight;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);

    vertexDistance = fog_distance(Position, FogShape);

    int vert = gl_VertexID % 4;
    vec2 coord = corners[vert];
    vec4 col = round(texture(Sampler0, UV0) * 255);

    if (col.a == 251 && Position.z == 2400.12) { // screenspace
        effectId = int(col.b);
        gl_Position.xy = vec2(coord * 2 - 1) * vec2(1, -1);
        gl_Position.zw = vec2(-1, 1);
        vertexColor = Color;
        texCoord0 = vec2(UV0 - coord * 64 / 256);
    } else if ((col.a == 251 || col.a == 253) && Position.z == 2400.0) {
        gl_Position = vec4(2,2,2,1); // hide shadow
    } else if (col.a == 253) { // normal/in-world effect
        effectId = int(col.b);
        vertexColor = Color;
        texCoord0 = UV0;
    } else {
        effectId = 0;
        vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
        texCoord0 = UV0;
    }

    // animated emoji
    frames = fps = 0;
    if (col.a == 252 && Position.z == 0) {
        frames = int(col.r);
        fps = int(col.g);
        frameheight = col.b;
    }
}
