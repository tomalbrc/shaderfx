#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform vec2 ScreenSize;
uniform float GameTime;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform int FogShape;

//%IMPORTS%

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

flat in int effectId;

flat in int frames;
flat in int fps;
flat in float frameheight;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);
    vec4 color = texColor * vertexColor * ColorModulator;

    vec2 centerUV = gl_FragCoord.xy / ScreenSize - 0.5;
    float ratio = ScreenSize.y / ScreenSize.x;

    if (effectId != 0) {
        switch (effectId) {
//%CASES%
        }
    }

    // animated emoji
    if (frames > 1) {
        int frameI = int(mod(floor(GameTime*1000.0*fps), frames-1));
        float framePart = 1.0 / float(frames);

        float ty = texCoord0.y*256.0;
        if (ty > frameheight)
            discard;

        color = texture(Sampler0, texCoord0  + vec2(0, frameheight/256*(frameI+1))) * vertexColor * ColorModulator;
    }

    if (effectId == 0 && color.a < 0.1) {
        discard;
    }

    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
