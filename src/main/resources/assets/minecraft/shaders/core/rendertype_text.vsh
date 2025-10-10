#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

const vec3 COLOR_TOP_RIGHT = vec3(254.0 / 255.0, 1.0, 1.0); // ~#FEFFFF
const vec3 COLOR_TOP_LEFT  = vec3(253.0 / 255.0, 1.0, 1.0); // ~#FDFFFF
const vec2[4] corners = vec2[4](vec2(0), vec2(0, 1), vec2(1), vec2(1, 0));

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;

uniform vec2 ScreenSize;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

flat out int animationType;
flat out int frames;
flat out int fps;
flat out float frameheight;

void main() {
    texCoord0 = UV0;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);

    int vert = gl_VertexID % 4;
    vec2 coord = corners[vert];
    vec4 col = round(texture(Sampler0, UV0) * 255);

    animationType = 0;
    if (col.a == 251 && Position.z == 0) {
        animationType = int(col.b);
        gl_Position.xy = vec2(coord * 2 - 1) * vec2(1, -1);
        gl_Position.zw = vec2(-1, 1);
        vertexColor = Color;
    }

    //vanilla
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);

    // animated emoji
    frames = fps = 0;
    if (animationType != 0) {
        texCoord0 = vec2(UV0 - coord * 64 / 256);
    }
    else {
        if (col.a == 252 && Position.z == 0) {
            frames = int(col.r);
            fps = int(col.g);
            frameheight = col.b;
        }
        texCoord0 = UV0;
    }
}
