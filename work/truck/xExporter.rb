=begin
 DirectX Exporter from Google SketchUp 
 Copyright (C) 2008 edecadoudal@yahoo.com
7 jun 2008 Version 0.5b:
 . fixed Export for Blender (rotation matrix wasn't taken into account, modified the counterclockwise() proc)
6 jun 2008 Version 0.5:
 . Added Export for Blender (add a rotation matrix thus the meshes are right oriented)
12 mar 2008 Version 0.4 :
 . Added Export Textured faces only
 . Light presentation : http://edecadoudal.googlepages.com/sketchupwithdirectx
29 feb 2008 Version 0.3 :
 . Now Supports Back Faces (BUG: sometimes it is necessary to explode all ...?)
 . speed export improvement
27 feb 2008 Version 0.2 :
 . Now Supports Groups and Components
26 feb 2008 Version 0.1 : published on http://creators.xna.com
 . Support Textures : created in the same folder
 . Supports front faces only : no Group or Components 
Fixed bugs:
  . Color only texture are not rendered
Known bugs:
 . Some models may generate an error when loaded in DirectX Viewer: don't know why
 . Some faces are rendered black (something to do with normals) : try to explode the model in sketchup to fix. 


Thank you to jim foltz for its obj exporter that helps me learning a bit of ruby and SketchUp API: http://sketchuptips.blogspot.com/2007/01/wavefront-obj-exporter.html
Thanks to Dale Martens (Whaat)  for textures SU API in its indigo exporter http://www.indigorenderer.com/joomla/index.php?option=com_docman&task=cat_view&gid=38&Itemid=62
 
 Install : Copy script to PLUGINS folder in SketchUp folder, run SketchUp, go to Plugins > Export DirectX...
 Try loading your model in DirectX Viewer
 
Feed Back appreciated
 
   
=end
$export_textured_only = false
$export_rotated = false

# add item if menu is not already loaded
if( $directxExport_loaded != true ) then
	main_menu = UI.menu("Plugins").add_submenu("DirectX")
	main_menu.add_item("Export Model...") { export_directx_all }
	main_menu.add_item("Export textured faces ...") { export_directx_textured }
	main_menu.add_item("Export Blender...") { export_directx_rotated}
	$directxExport_loaded = true
end

#Collect objects and explode group or components, apply transformation
def get_all_model_faces(entities, trans)
	if entities.class == Sketchup::ComponentInstance
		entity_list=entities.definition.entities 
		
	elsif entities.class == Sketchup::Group
		entity_list=entities.entities 
		
	else
		entity_list=entities
	end
	
	
	# for each element
	for e in entity_list
	    # if the element is a Group or a Component
		if (e.layer.visible? and (e.class == Sketchup::Group or e.class == Sketchup::ComponentInstance)) then
			#check for material to set it to the children on next recusive call
			$components += 1 if entities.class == Sketchup::ComponentInstance
			$groups +=1 if entities.class == Sketchup::Group
			if e.material != nil
				$parent_mat.push(e.material)
				#print e.material.to_s
			else
				$parent_mat.push($parent_mat.last)
			end
			# recurs call on the group or component
			get_all_model_faces(e, trans*e.transformation)
			$parent_mat.pop
		end
		# if the object is a simple Face
		if( e.class == Sketchup::Face) # and e.layer.visible?) then ## COMMENTED
			# check for material
			if e.material == nil
				mat=$parent_mat.last				
			else
				mat = e.material
			end
			# check for Back face material 
			if e.back_material == nil
				back_mat=$parent_mat.last				
			else
				back_mat = e.back_material
			end
			
			if $export_textured_only==false then
				# add object to the global collection
				$face_trans_collection.push([e,trans,mat,back_mat])
				#print e.to_s + "#" + trans.to_s + "\n"
				$faces += 1
			else 
			        if mat != nil then
					$face_trans_collection.push([e,trans,mat,nil])
					#print e.to_s + "#" + trans.to_s + "\n"
					$faces += 1
				end
			        if back_mat !=nil then
					$face_trans_collection.push([e,trans,nil, back_mat])
					#print e.to_s + "#" + trans.to_s + "\n"
					$faces += 1
				end
			end
				
		end
	end
end

def export_directx_textured()
	$export_textured_only =true
	$export_rotated =false
	export_directx3()
end
def export_directx_all()
	$export_textured_only =false
	$export_rotated =false
	export_directx3()
end
def export_directx_rotated()
	$export_textured_only =false
	$export_rotated =true
	export_directx3()
end

# Main entry point
def export_directx3() 
	
	# display ruby panel for messages
	Sketchup.send_action "showRubyPanel:"
	# call Save Dialog function
	filename = get_filename
	if filename == nil then # exit if cancel was choosen
		return
	end 
	
	# array to store material
	$all_materials = []
	$faces = 0
	$groups = 0
	$components = 0

	meshes = []
	faces = []
	# get the active sketchup model
	for entity in Sketchup.active_model.entities
		entity_name = "mesh_" + meshes.length.to_s
		# get the array of entities
		if entity.class == Sketchup::ComponentInstance and entity.layer.visible?
			entity_list=entity.definition.entities 
			$components += 1 
			entity_name=entity.name if entity.name.to_s != ""
			if entity.name.to_s != ""
				entity_name=entity.name 
			elsif entity.definition.name.to_s != ""
				entity_name=entity.definition.name
			end
			txt = export_directx(entity_list, entity_name, filename, entity.transformation, entity.material)
			meshes.push(txt) if txt != "" 
		elsif entity.class == Sketchup::Group and entity.layer.visible?
			entity_list=entity.entities 
			$groups +=1 
			entity_name=entity.name if entity.name.to_s != ""
			print "group:" + entity_name + "\n"
			txt = export_directx(entity_list, entity_name, filename, entity.transformation, entity.material)
			meshes.push(txt) if txt != "" 
		else
			if entity.layer.visible?
				faces.push(entity) 
				$faces += 1 
			end
		end
	end
	if faces.length > 0
		entity_name = "mesh_" + meshes.length.to_s
		txt = export_directx(faces, entity_name, filename, Geom::Transformation.new, nil)
		meshes.push(txt) if txt != "" 
	end
	
	print "#{$groups} group(s), #{$components} component(s), #{$faces} faces."
	print "Saving to : #{filename} \n"
	
	fout = create_file(filename)
	write_string(fout, directx_header())
	write_string(fout, directx_materials($all_materials, filename))
	
	if $export_rotated
		write_string(fout,  directx_rotation())
	end
	
	for m in meshes
		write_string(fout, m)
	end
	
	if $export_rotated
		write_string(fout,  "}")
	end
	close_file(fout)
	print "end.\n"
	
end

def export_directx(entities, meshName, filename, transform, mat_def) 
	# get the active sketchup model
	#model = Sketchup.active_model
	# get a new writer for textures export
	texturewriter = Sketchup.create_texture_writer

	#global array that stores each individual text mesh
	meshes = []
	vertex_collection = []
	normal_objects = []
	normal_collection = []
	normal_faces = []
	texture_coords = []
	face_collection = []
	material_collection = []
	material_face = []
	
	# global push/pop array for children material propagation
	$parent_mat = []
	# global collection that stores a triplet [face, transformation, material]
	$face_trans_collection = []

	# add a default material when nothing special is specified for a face
	material_collection.push("{ Default_Material }")

	progress=0
	
	#get model entities 
	#entities = model.entities
	
	$parent_mat.push(mat_def)
	print "get faces\n"
	# collect faces
	#get_all_model_faces(entities, Geom::Transformation.new)
	get_all_model_faces(entities, transform)
	print "face_trans_collection=#{$face_trans_collection.length}\n"
	
	startindex = 0
	# for all Faces
	for ft in $face_trans_collection
		entity = ft[0]
		trans = ft[1]
		mat = ft[2]
		back_mat = ft[3]
		mats = []
		mats.push(mat)
		mats.push(back_mat)
		progress+=1
		print "#{progress} / #{$faces} faces.." if progress % 1000 == 0

		if(entity.class == Sketchup::Face) then
			# Get a the MeshPolygon from the Face
			#0 (Include PolygonMeshPoints), 1 (Include PolygonMeshUVQFront), 2 (Include PolygonMeshUVQBack), 4 (Include PolygonMeshNormals). 
			mesh = entity.mesh 1 | 4 | 2
			# apply transformation when the object was in a group or in a component
			mesh.transform! trans
			# For all points
			#print "mesh\n"
			for p in (1..mesh.count_points)
				# get the 3D point
				pos = mesh.point_at(p).to_a
				# get the normal at this point
				norm = mesh.normal_at(p)
				normal_objects.push(norm)
				norm = norm.to_a
				# default texture size
				texsize = Geom::Point3d.new(1,1,1)
				
				if !$export_textured_only then 
					# if the material was obtained from the group or component
					if mat!=nil and mat.texture!=nil and entity.material==nil then
						# get the texture size
						texsize = Geom::Point3d.new(mat.texture.width, mat.texture.height, 1)
					end
					# change to left hand counter clockwise
					pos = counterclockwise(pos)
					v = "#{"%.4f" %(pos[0])};#{"%.4f" %(pos[1])};#{"%.4f" %(pos[2])};"
					vertex_collection.push(v)
					# change to left hand counter clockwise
					norm = counterclockwise(norm)
					n = "#{"%.4f" %(norm[0])};#{"%.4f" %(norm[1])};#{"%.4f" %(norm[2])};"
					normal_collection.push(n)
					# get the mesh texture coords (texture deformation: texture can only be streshed, rotated, translated (no parallel deformation)
					uv = [mesh.uv_at(p,true).x/texsize.x, mesh.uv_at(p,true).y/texsize.y, mesh.uv_at(p,true).z/texsize.z]
					u = "#{"%.4f" %(uv[0]+1)},#{"%.4f" %(-uv[1])};"
					texture_coords.push(u)
				
					# Back Face
					texsize = Geom::Point3d.new(1,1,1)
					# if the material was obtained from the group or component
					if back_mat!=nil and back_mat.texture!=nil and entity.back_material==nil then
						# get the texture size
						texsize = Geom::Point3d.new(back_mat.texture.width, back_mat.texture.height, 1)
					end
					v = "#{"%.4f" %(pos[0])};#{"%.4f" %(pos[1])};#{"%.4f" %(pos[2])};"
					vertex_collection.push(v)
					n = "#{"%.4f" %(-norm[0])};#{"%.4f" %(-norm[1])};#{"%.4f" %(-norm[2])};"
					normal_collection.push(n)

					# get the mesh texture coords (texture deformation: texture can only be streshed, rotated, translated (no parallel deformation)
					uv = [mesh.uv_at(p,false).x/texsize.x, mesh.uv_at(p,false).y/texsize.y, mesh.uv_at(p,false).z/texsize.z]
					#uv = counterclockwise(uv)
					u = "#{"%.4f" %(uv[0]+1)},#{"%.4f" %(-uv[1])};"
					texture_coords.push(u)
				else
					# if the material was obtained from the group or component
					if mat!=nil and mat.texture!=nil and entity.material==nil then
						# get the texture size
						texsize = Geom::Point3d.new(mat.texture.width, mat.texture.height, 1)
					end
					if mat!=nil then
						front=true
					else
						front=false
					end
					# change to left hand counter clockwise
					pos = counterclockwise(pos)
					v = "#{"%.4f" %(pos[0])};#{"%.4f" %(pos[1])};#{"%.4f" %(pos[2])};"
					vertex_collection.push(v)
					# change to left hand counter clockwise
					norm = counterclockwise(norm)
					if front then
						n = "#{"%.4f" %(norm[0])};#{"%.4f" %(norm[1])};#{"%.4f" %(norm[2])};"
					else
						n = "#{"%.4f" %(-norm[0])};#{"%.4f" %(-norm[1])};#{"%.4f" %(-norm[2])};"
					end
					normal_collection.push(n)
					# get the mesh texture coords (texture deformation: texture can only be streshed, rotated, translated (no parallel deformation)
					uv = [mesh.uv_at(p,front).x/texsize.x, mesh.uv_at(p,front).y/texsize.y, mesh.uv_at(p,front).z/texsize.z]
					u = "#{"%.4f" %(uv[0]+1)},#{"%.4f" %(-uv[1])};"
					texture_coords.push(u)
				end
			end
			
			# for each polygon
			for poly in mesh.polygons
				v1 = (poly[0]>=0 ? poly[0] : -poly[0])+startindex
				v2 = (poly[1]>=0 ? poly[1] : -poly[1])+startindex
				v3 = (poly[2]>=0 ? poly[2] : -poly[2])+startindex

				if !$export_textured_only then 
					f = "3;#{(v3-1)*2},#{(v2-1)*2},#{(v1-1)*2}"
					face_collection.push(f)
					# Back Face
					f = "3;#{(v1-1)*2+1},#{(v2-1)*2+1},#{(v3-1)*2+1}"
					face_collection.push(f)
				else
					if ($export_textured_only and mat != nil)  then
						f = "3;#{(v3-1)},#{(v2-1)},#{(v1-1)}"
						face_collection.push(f)
					end
					if ($export_textured_only and back_mat != nil)  then
						# Back Face
						f = "3;#{(v1-1)},#{(v2-1)},#{(v3-1)}"
						face_collection.push(f)
					end
				end
				
				# get the material 
				for material in mats
					mat_index = 0
					#material = mat
					if material then
						front = true
						if (material==back_mat) then
							front=false
						end
						# add the material to the global list if this is a new one
						if  !($all_materials.index(material)) then
							$all_materials.push(material)
							if material.texture then
								# build a filename based on the targetfilename and the texture name
								f = filename + File.basename(material.texture.filename)
								# load the texture of the entity into the texture writer object
								texturewriter.load entity, front  # False for BackFaces
								# serialize the texture to disk
								texturewriter.write entity, front, f
								print "<material=" + f + ">"
							end
							#m = material.name.gsub(/[^a-zA-Z0-9]/, "_")
							#material_collection.push("{ " + m + " }")
						#else
						#	if material_collection.index
						end
						
						m = "{ " + material.name.gsub(/[^a-zA-Z0-9]/, "_") + " }"
						mat_index = material_collection.index(m)
						if mat_index == nil
							material_collection.push(m)
						end
						# get the index for the material
						#mat_index = $all_materials.index(material)+1
						mat_index = material_collection.index(m)
					end 
					if !$export_textured_only or ($export_textured_only and material != nil) then 
						# add the index of the material in the list
						material_face.push(mat_index.to_s)
					end
				end
			end
			startindex = startindex + mesh.count_points
		end
	end
print "******\n" + meshName.to_s + "\n*******"
	text = ""
if vertex_collection.length > 0 then	
	text+= "Mesh #{(meshName.gsub(/[^a-zA-Z0-9]/, "_"))}{\n"
	text+= " #{vertex_collection.length};\n"
	stxt = vertex_collection.to_a.join(",\n ")
	text+= " #{stxt};\n"
	text+= " #{face_collection.length};\n"
	stxt = face_collection.to_a.join(",\n ")
	text+= " #{stxt};;\n"
	text+= "  MeshMaterialList {\n"
	text+= "  #{material_collection.length};\n"
	text+= "  #{material_face.length};\n"
	stxt = material_face.to_a.join(",\n  ")
	text+= "  #{stxt};\n"
	stxt = material_collection.to_a.join("\n  ")
	text+= "  #{stxt}\n"
	text+= "  }\n"
	text+= "  MeshTextureCoords {\n"
	text+= "  #{texture_coords.length};\n"
	stxt = texture_coords.to_a.join("\n  ")
	text+= "  #{stxt};\n"
	text+= "  }\n"
	text+= "  MeshNormals {\n"
	text+= "  #{normal_collection.length};\n  "
	stxt = normal_collection.to_a.join("\n")
	text+= "  #{stxt};\n"
	text+= "  #{face_collection.length};\n"
	stxt = face_collection.to_a.join(";\n  ")
	text+= "  #{stxt};;\n"
	
	text+= "  }\n"
	text+= " }\n"
else
	text = ""
end
	
end

def get_filename
	model = Sketchup.active_model
	model_filename = File.basename(model.path)
	if model_filename != ""
		model_name = model_filename.split(".")[0]
	else
		model_name = "Untitled"
	end
	model_name += "-T" if $export_textured_only
	model_name += ".x"
	my_str = UI.savepanel("Export as", "", model_name)
end

#convert a clockwise vector in a counterclockwise vector
def counterclockwise(v)
	v2 = Array.new
	if !$export_rotated
		v2.push(-v.to_a[1])
		v2.push(v.to_a[2])
		v2.push(v.to_a[0])
	else
		v2.push(v.to_a[0])
		v2.push(v.to_a[1])
		v2.push(v.to_a[2])
	end
	my_a = v2
end

def directx_header()
	text = "xof 0303txt 0032 
// SketchUp 6 -> DirectX (c)2008 edecadoudal, supports: faces, normals and textures 
Material Default_Material{ 
1.0;1.0;1.0;1.0;;
3.2;
0.000000;0.000000;0.000000;;
0.000000;0.000000;0.000000;;
} 
"
end

def directx_rotation()
	text = "Frame {
 FrameTransformMatrix
    {
        1.000000,0.000000,0.000000,0.000000,
        0.000000,-1.000000,0.000000,0.000000,
        0.000000,0.000000,1.000000,0.000000,
        0.000000,0.000000,0.000000,1.000000;;
    }
"
end


def directx_materials(all_materials, filename)
	text = ""
	all_materials.each{|mat|
		# replace '[' and ']' in name by a '_'
		n = mat.name.gsub(/[^a-zA-Z0-9]/, "_")
		mat_string = "Material " + n + "{ \n"
		 # faceColor 
		mat_string += (mat.color.red/255.0).to_s + ";"  +(mat.color.green/255.0).to_s + ";" + (mat.color.blue/255.0).to_s + ";"
		# Alpha
		mat_string += mat.alpha.to_s + ";;\n"  
		#power
		mat_string += "3.2;\n" 
		# specularColor	ColorRGB 		
		mat_string += "0.000000;0.000000;0.000000;;\n" 
		# emissiveColor 	ColorRGB 
		mat_string += "0.000000;0.000000;0.000000;;\n" 
		if mat.texture && mat.texture.filename != "" then
			mat_string += "   TextureFilename { "
			mat_string += "\"" + File.basename(filename) + File.basename(mat.texture.filename) + "\";"
			mat_string += "   } \n"
		end 
		mat_string += "} \n"
		text += mat_string
	}
	text+=""
end


def write_file(filename, text)
   fout = File.open(filename, "w")
   fout.puts text
   fout.close
end

def create_file(filename)
	fout = File.open(filename, "w")
end

def write_string(fout, text)
	fout.puts text
end

def close_file(fout)
	fout.close
end
