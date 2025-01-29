package ai.aitia.demo.car_common.dto;

import java.io.Serializable;

public class RegisteredSystemResponseDTO implements Serializable {

	//=================================================================================================
	// members

	private static final long serialVersionUID = -8371510478751740542L;
	
	private int id;
	private String name;
	private String endpoint;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	public RegisteredSystemResponseDTO() {}
	
	//-------------------------------------------------------------------------------------------------
	public RegisteredSystemResponseDTO(final int id, final String name, final String endpoint) {
		this.id = id;
		this.name = name;
		this.endpoint = endpoint;
	}

	//-------------------------------------------------------------------------------------------------
	public int getId() { return id; }
	public String getName() { return name; }
	public String getEndpoint() { return endpoint; }

	//-------------------------------------------------------------------------------------------------
	public void setId(final int id) {this.id = id; }
	public void setName(final String name) { this.name = name; }
	public void setEndpoint(final String endpoint) { this.endpoint = endpoint; }	
}
