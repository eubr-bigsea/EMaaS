package PointDependencies;

import java.io.IOException;

import java.io.Serializable;

import org.apache.commons.lang.NullArgumentException;

public class FieldsInputsMatchUp implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String geometry;
	private String name;
	private String indexOfID;
	private String id;
	private static final int NUMBER_ATTRIBUTES = 4;
	
	public FieldsInputsMatchUp(String params) throws IOException {
		
		String[] attributesArray = params.split(",");
		if (attributesArray.length != NUMBER_ATTRIBUTES) {
			System.err.println("Usage: <geometry,name,indexOfID,id>");
			throw new IOException( "The input file should have 4 attributes not null. The fields should be separated by comma.");
		}
		
		initializeAttributes(attributesArray[0], attributesArray[1], attributesArray[2], attributesArray[3]);

	}
	
	public FieldsInputsMatchUp(String geometry, String name, String indexOfID, String id) {
		initializeAttributes(geometry, name, indexOfID, id);
	}
	
	private void initializeAttributes(String geometry, String name, String indexOfID, String id) {
		if (geometry == null || name == null || indexOfID == null || id == null) {
			throw new NullArgumentException(name);
		}
		this.geometry = geometry;
		this.name = name;
		this.indexOfID = indexOfID;
		this.id = id;
	}
	
	public String getGeometry() {
		return geometry;
	}
	public void setGeometry(String geometry) {
		this.geometry = geometry;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getIndexOfID() {
		return indexOfID;
	}
	public void setIndexOfID(String indexOfID) {
		this.indexOfID = indexOfID;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	
	
}
