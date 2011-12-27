
require 'sketchup.rb'

def export_main

  # set scale factor taking inches to desired units
  $scale = 0.0254
  
  model = Sketchup.active_model
  model_filename = File.basename(model.path).capitalize
  model_name = model_filename.split(".")[0].capitalize
  if model_filename == "" 
    model_filename = "model"
  end
  if model.selection.empty?
    entities = model.entities
  else
    entities = model.selection
  end
  
  if entities.length > 0
    # set up for undo
    model.start_operation("Export Java", true)
    filename_prefix = File.basename(model.path).split(".")[0];
    output_path = UI.savepanel("Java file location",  "." ,  filename_prefix + ".java")
    $output_file = File.new(output_path, "w" ) 
    export_preface(model_name)
    export_materials(model.materials)
    export_preentities(model_name)
    export_entities(entities, Geom::Transformation.new())
    export_postentities(model_name)
    export_epilog(model_name)
    $output_file.close
    model.commit_operation
  end
end

def javify_id(s, prefix)
  s = s.gsub(/ /, '_')
  s = s.gsub(/\W/, '')
  return prefix + s
end

def export_preface(name)
  model_class_name = name + "Model"
  $output_file.puts("class " +  model_class_name + " {\n" + <<-EOS)
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
EOS
end

def export_preentities(name)
  $output_file.puts("    TriangleStrip [] strips = {")
end

def export_postentities(name)
  $output_file.puts("    };")
end

def export_epilog(name)
  model_class_name = name + "Model"
  $output_file.puts(<<-EOS)
    public void display(GL gl) {
        for (int i = 0; i < strips.length; i++) {
            TriangleStrip strip = strips[i];
            if (strip.color != null) {
                gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE, strip.color, 0);
            }
            gl.glBegin(GL.GL_TRIANGLES);
            for (int j = 0; j < strip.points.length; j += 3) {
                gl.glNormal3fv(strip.normals, j);
                gl.glVertex3fv(strip.points, j);
            }
            gl.glEnd();
        }
    }
};
EOS
end

def export_materials(materials)
  fmt = "%.4f"
  materials.each do |material|
    r = fmt % (material.color.red / 255.0)
    g = fmt % (material.color.green / 255.0)
    b = fmt % (material.color.blue / 255.0)
    texture = material.texture
    color_name = javify_id(material.name, "Color_")
    $output_file.puts("    float [] " + color_name + " = { " + r + "f, " + g + "f, " + b + "f , 1f};")
    if texture and texture.filename
      texture_name = javify_id(material.name, "Texture_")
      $output_file.puts("    Texture " + texture_name + " = WPBDApp.getApplication().getTextureResource(\"" + texture.filename  + "\", true, TextureIO.JPG);" )
    end
  end  
end

def export_entities(entities, xform)
  entities.each do |entity|
    if  entity.typename == "Face"
      write_face(entity, xform)
    elsif entity.typename == "Group"
      $output_file.puts("        // Group: " + entity.name)
      export_entities(entity.entities, xform * entity.transformation)
    elsif entity.typename == "ComponentInstance"
      $output_file.puts("        // Component instance: " + entity.name)
      export_entities(entity.definition.entities, xform * entity.transformation)
    end
  end
end

def point_string(p, scale, sep, sfx)
  x = p.x.to_f * scale
  y = p.y.to_f * scale
  z = p.z.to_f * scale
  fmt = "%9.5f"
  fmt % x + sep + fmt % y + sep + fmt % z + sfx
end

def uv_string(p, sep, sfx)
  x = p.x.to_f
  y = p.y.to_f
  fmt = "%9.5f"
  fmt % x + sep + fmt % y + sfx
end

def coord_string(mesh, polygon, i, sep, sfx)
  point_string(mesh.point_at(polygon[i].abs), $scale, sep, sfx)
end

def normal_string(mesh, polygon, i, sep, sfx)
  point_string(mesh.normal_at(polygon[i].abs), 1, sep, sfx)
end

def texcoord_string(mesh, polygon, i, sep, sfx)
  uv_string(mesh.uv_at(polygon[i].abs, true), sep, sfx)
end

def put_coord(mesh, polygon, i)
  $output_file.puts("                " + coord_string(mesh, polygon, i, "f,", "f") + ",\n")
end

def put_normal(mesh, polygon, i)
  $output_file.puts("                " + normal_string(mesh, polygon, i, "f,", "f") + ",\n")
end

def put_texcoord(mesh, polygon, i)
  $output_file.puts("                " + texcoord_string(mesh, polygon, i, "f,", "f") + ",\n")
end

def write_face(face, xform)
  mesh = face.mesh(0 + 1 + 4)
  mesh.transform! xform
  if face.material
    material = face.material
    if material.name 
      color_name = javify_id(material.name, "Color_") 
      texture_name = material.texture ? javify_id(material.name, "Texture_") : "null"
    else
      color_name = "Color_default"
      texture_name = "null"
    end
  else
    color_name = "Color_default"
    texture_name = "null"
  end
    
  $output_file.puts("        new TriangleStrip(" + 
    color_name + ", " +
    texture_name + ", ")

  # points
  $output_file.puts("            new float [] { // points")
  mesh.polygons.each do |polygon|
    for i in 0 .. polygon.length - 1 do
      put_coord(mesh, polygon, i)
    end
  end
  $output_file.puts("            },")
  
  # normals
  $output_file.puts("            new float [] { // normals")
  mesh.polygons.each do |polygon|
    for i in 0 .. polygon.length - 1 do
      put_normal(mesh, polygon, i)
    end
  end
  $output_file.puts("            },")

  # texture coordinates
  if texture_name != "null"
    $output_file.puts("            new float [] { // texcoords")
    mesh.polygons.each do |polygon|
      for i in 0 .. polygon.length - 1 do
        put_texcoord(mesh, polygon, i)
      end
    end
    $output_file.puts("            }),")
  else
    $output_file.puts("            null),")
  end
end

if( not file_loaded?("skp_to_java.rb") )
   add_separator_to_menu("Tools")
   UI.menu("Tools").add_item("Export Java Code") { export_main }
end

file_loaded("skp_to_java.rb")
