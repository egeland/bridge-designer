class CabinternalModel {
    class TriangleStrip {
        TriangleStrip(float [] c, Texture t, float [] p, float [] n, float [] tc) {
            color = c;
            texture = t;
            points = p;
            normals = n;
            texCoords = tc;
        }
        float [] color;
        Texture texture;
        float [] points;
        float [] normals;
        float [] texCoords;
    };
    float [] Color_default = { 0.7f, 0.7f, 0.7f, 1f };
    float [] Color_Burnt_Umber1 = { 0.0275f, 0.0118f, 0.0078f , 1f};
    float [] Color_Metal_Corrogated_Shiny = { 0.7569f, 0.7569f, 0.7569f , 1f};
    Texture Texture_Metal_Corrogated_Shiny = WPBDApp.getApplication().getTextureResource("Metal_Corrogated_Shiny.jpg", true, TextureIO.JPG);
    float [] Color_Metal_Aluminum_Anodized = { 0.7922f, 0.8196f, 0.8588f , 1f};
    Texture Texture_Metal_Aluminum_Anodized = WPBDApp.getApplication().getTextureResource("Metal_Aluminum_Anodized.jpg", true, TextureIO.JPG);
    float [] Color_Metal_Steel_Textured = { 0.4157f, 0.4314f, 0.4824f , 1f};
    Texture Texture_Metal_Steel_Textured = WPBDApp.getApplication().getTextureResource("Metal_Steel_Textured.jpg", true, TextureIO.JPG);
    float [] Color_0135_DarkGray = { 0.3176f, 0.3176f, 0.3176f , 1f};
    float [] Color_0128_White = { 1.0000f, 1.0000f, 1.0000f , 1f};
    float [] Color_0020_Red = { 1.0000f, 0.0000f, 0.0000f , 1f};
    float [] Color_0039_DarkOrange = { 1.0000f, 0.5490f, 0.0000f , 1f};
    float [] Color_Translucent_Glass_Sky_Reflection = { 0.4941f, 0.5412f, 0.7176f , 1f};
    Texture Texture_Translucent_Glass_Sky_Reflection = WPBDApp.getApplication().getTextureResource("Translucent_Glass_Sky_Reflection_.jpg", true, TextureIO.JPG);
    float [] Color_0137_Black = { 0.0000f, 0.0000f, 0.0000f , 1f};
    float [] Color_Color_003 = { 0.6667f, 0.6667f, 0.6667f , 1f};
    float [] Color_Color_000 = { 1.0000f, 1.0000f, 1.0000f , 1f};
    float [] Color_Color_006 = { 0.3373f, 0.3373f, 0.3373f , 1f};
    float [] Color_Color_005 = { 0.4471f, 0.4471f, 0.4471f , 1f};
    float [] Color_0131_Silver = { 0.7529f, 0.7529f, 0.7529f , 1f};
    float [] Color_0036_BurlyWood = { 0.8706f, 0.7216f, 0.5294f , 1f};
    TriangleStrip [] strips = {
        // front face
        new TriangleStrip(Color_Metal_Corrogated_Shiny, Texture_Metal_Corrogated_Shiny, 
            new float [] { // points
                 -0.79922f,  3.27394f, -1.30200f,
                 -0.79922f,  1.12394f,  1.29800f,
                 -0.79922f,  1.12394f, -1.30200f,
                 -0.79922f,  1.12394f,  1.29800f,
                 -0.79922f,  3.27394f, -1.30200f,
                 -0.79922f,  3.27394f,  1.29800f,
            },
            new float [] { // normals
                  1.00000f, -0.00000f,  0.00000f,
                  1.00000f, -0.00000f,  0.00000f,
                  1.00000f, -0.00000f,  0.00000f,
                  1.00000f, -0.00000f,  0.00000f,
                  1.00000f, -0.00000f,  0.00000f,
                  1.00000f, -0.00000f,  0.00000f,
            },
            new float [] { // texcoords
                 32.22387f,-12.81492f,
                 11.06245f, 12.77563f,
                 11.06245f,-12.81492f,
                 11.06245f, 12.77563f,
                 32.22387f,-12.81492f,
                 32.22387f, 12.77563f,
            }),
    };
    public void display(GL2 gl) {
        for (int i = 0; i < strips.length; i++) {
            TriangleStrip strip = strips[i];
            if (strip.color != null) {
                gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE, strip.color, 0);
            }
            gl.glBegin(GL2.GL_TRIANGLES);
            for (int j = 0; j < strip.points.length; j += 3) {
                gl.glNormal3fv(strip.normals, j);
                gl.glVertex3fv(strip.points, j);
            }
            gl.glEnd();
        }
    }
};
