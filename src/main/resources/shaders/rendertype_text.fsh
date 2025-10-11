#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

//%IMPORTS%

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

flat in int animationType;

flat in int frames;
flat in int fps;
flat in float frameheight;

out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);
    vec4 color = texColor * vertexColor * ColorModulator;

    vec2 centerUV = gl_FragCoord.xy / ScreenSize - 0.5;
    float ratio = ScreenSize.y / ScreenSize.x;

    if (animationType != 0) {
        switch (animationType) {
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

    if (color.a < 0.1) {
        discard;
    }

    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
